package com.lunettes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;

public class ServeurCallback implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(ServeurCallback.class);

    // quantité max autorisée par type (exclusive) qui est définie par le cahier des charges
    private static final int QUANTITE_MAX_PAR_TYPE = 10;

    private final Usine usine;
    private final MqttClient mqttClient;

    public ServeurCallback(Usine usine, MqttClient mqttClient) {
        this.usine = usine;
        this.mqttClient = mqttClient;
    }

    // appelé automatiquement par la lib MQTT à chaque message reçu
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        log.info("Message reçu sur le topic : {}", topic);
        if (topic.startsWith("orders/")) {
            traiterCommande(topic, message);
        } else if (topic.startsWith("serials/")) {
            traiterVerification(topic);
        }
    }

    // traite une commande reçue sur orders/{uuid}
    // on ignore les messages sur orders/{uuid}/validated, /delivery, etc.
    // (normalement le serveur n'est abonné qu'à orders/+, mais on double-vérifie)
    private void traiterCommande(String topic, MqttMessage message) {
        String[] parts = topic.split("/");
        // on attend exactement orders/{uuid} (2 segments), pas orders/{uuid}/delivery (3 segments)
        if (parts.length != 2) return;

        String uuid = parts[1];
        log.info("Nouvelle commande reçue : {}", uuid);

        // étape 1 : désérialiser le message en map {TypeLunette -> quantité}
        Map<TypeLunette, Integer> commande = deserialiserCommande(message.toString());
        if (commande == null) {
            String erreur = "Format de commande invalide (type inconnu ou quantité non numérique)";
            log.warn("Commande {} refusée : {}", uuid, erreur);
            publier("orders/" + uuid + "/cancelled", erreur);
            return;
        }

        // étape 2 : valider les règles métier du cahier des charges
        String erreurValidation = validerCommande(commande);
        if (erreurValidation != null) {
            log.warn("Commande {} refusée : {}", uuid, erreurValidation);
            publier("orders/" + uuid + "/cancelled", erreurValidation);
            return;
        }

        // la commande est valide, on confirme au client
        publier("orders/" + uuid + "/validated", "");
        log.info("Commande {} validée, lancement de la production", uuid);

        // statut "processing" : la fabrication démarre
        publier("orders/" + uuid + "/status", "processing");

        // étape 3 : produire les lunettes
        List<Lunette> lunettes;
        try {
            lunettes = usine.produire(commande);
        } catch (Exception e) {
            log.error("Erreur de production pour la commande {} : {}", uuid, e.getMessage());
            publier("orders/" + uuid + "/error", "Erreur de production : " + e.getMessage());
            return;
        }

        // statut "processed" : la fabrication est terminée, la livraison suit immédiatement
        publier("orders/" + uuid + "/status", "processed");

        // livraison au client
        log.info("Commande {} produite : {} lunettes livrées", uuid, lunettes.size());
        publier("orders/" + uuid + "/delivery", serialiserLivraison(lunettes));
    }

    // vérifie les règles métier définies dans le cahier des charges :
    // - la quantité totale doit être > 0
    // - chaque quantité individuelle doit être dans [0, 10[
    // retourne null si la commande est valide, sinon un message d'erreur
    String validerCommande(Map<TypeLunette, Integer> commande) {
        int totalQuantite = 0;
        for (Map.Entry<TypeLunette, Integer> entry : commande.entrySet()) {
            int qte = entry.getValue();
            // une quantité négative ou >= 10 est interdite
            if (qte < 0 || qte >= QUANTITE_MAX_PAR_TYPE) {
                return "Quantité invalide pour " + entry.getKey() + " : " + qte + " (doit être entre 0 et 9)";
            }
            totalQuantite += qte;
        }
        // une commande où tout est à 0 n'a pas de sens
        if (totalQuantite <= 0) {
            return "La quantité totale doit être supérieure à zéro";
        }
        return null;
    }

    // traite une demande de vérification de numéro de série sur serials/{serial}/check
    private void traiterVerification(String topic) {
        String[] parts = topic.split("/");
        // on attend exactement serials/{serial}/check (3 segments)
        // si le topic n'a pas ce format, on l'ignore pour éviter des erreurs ou des boucles
        if (parts.length != 3 || !"check".equals(parts[2])) {
            log.warn("Topic de vérification ignoré (format inattendu) : {}", topic);
            return;
        }

        String serial = parts[1];
        TypeLunette type = Fabricateur.validateSerial(serial);
        // si validateSerial retourne null, le serial n'est pas connu
        String reponse = (type == null) ? "invalid" : type.toString();
        log.info("Vérification du serial {} : {}", serial, reponse);
        publier("serials/" + serial, reponse);
    }

    // publie un message MQTT sur un topic donné
    // centralisé ici pour ne pas répéter le try/catch dans chaque méthode
    private void publier(String topic, String contenu) {
        try {
            mqttClient.publish(topic, new MqttMessage(contenu.getBytes()));
        } catch (MqttException e) {
            log.error("Échec de publication sur {} : {}", topic, e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        // se reconnecter automatiquement si connexion perdue setAutomaticReconnect(true) est activé
        log.warn("Connexion MQTT perdue : {}", cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // appelé quand l'envoi d'un message est confirmé - rien à faire ici
    }

    // transforme le texte "CLAUDE:2;BANANA:1" en map {CLAUDE -> 2, BANANA -> 1}
    // retourne null si le format est incorrect ou si un type de lunette est inconnu
    Map<TypeLunette, Integer> deserialiserCommande(String contenuMessage) {
        if (contenuMessage == null || contenuMessage.isBlank()) {
            log.warn("Message de commande vide ou null");
            return null;
        }

        Map<TypeLunette, Integer> commande = new HashMap<>();
        for (String part : contenuMessage.split(";")) {
            String[] kv = part.split(":");
            // chaque segment doit avoir deux parties : le type et la quantité
            if (kv.length != 2) {
                log.warn("Segment mal formé dans la commande : '{}'", part);
                return null;
            }
            try {
                TypeLunette type = TypeLunette.valueOf(kv[0].trim());
                int quantite = Integer.parseInt(kv[1].trim());
                commande.put(type, quantite);
            } catch (IllegalArgumentException e) {
                // le type de lunette n'existe pas ou la quantité n'est pas un nombre entier
                log.warn("Type ou quantité invalide dans la commande : '{}'", part);
                return null;
            }
        }

        return commande;
    }

    // transforme la liste de lunettes produites en texte "TYPE:serial;TYPE:serial"
    private String serialiserLivraison(List<Lunette> lunettes) {
        StringBuilder sb = new StringBuilder();
        for (Lunette lunette : lunettes) {
            if (sb.length() > 0) sb.append(";");
            sb.append(lunette.type).append(":").append(lunette.serial);
        }
        return sb.toString();
    }
}
