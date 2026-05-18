package com.lunettes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bernard_flou.Fabricateur;

/**
 * Point d'entree du backend de la fabrique de lunettes.
 * Cree l'usine, lit la configuration MQTT et demarre le serveur.
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    /**
     * Demarre le backend : cree l'usine, se connecte au broker MQTT et attend les commandes.
     *
     * @param args arguments de la ligne de commande (non utilises)
     */
    public static void main(String[] args) {
        //créer les objets métier : le fabricateur et l'usine
        Fabricateur fabricateur = new Fabricateur();
        Usine usine = new Usine(fabricateur);

        // on lit l'adresse du broker depuis le fichier de config
        String brokerUrl = lireConfig("broker.url");

        // si l'adresse est absente on ne peut pas démarrer
        if (brokerUrl == null) {
            log.error("URL du broker introuvable dans config.properties — arrêt.");
            return;
        }

        // options de connexion MQTT : reconnexion auto, timeout, keepalive
        MqttConnectOptions options = new MqttConnectOptions();
        // si la connexion est perdue, réessayer en arrière-plan
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        // on abandonne si le broker ne répond pas après 10 secondes
        options.setConnectionTimeout(10);
        // le client envoie un ping toutes les 30s pour que le broker sache qu'il est encore là
        options.setKeepAliveInterval(30);

        MqttClient mqttClient;
        try {
            mqttClient = new MqttClient(brokerUrl, UUID.randomUUID().toString());
        } catch (MqttException e) {
            log.error("Impossible de créer le client MQTT : {}", e.getMessage());
            return;
        }

        Serveur serveur = new Serveur(usine, mqttClient);
        try {
            // demarrer() reçoit les options et les utilise pour se connecter
            serveur.demarrer(options);
        } catch (MqttException e) {
            log.error("Impossible de démarrer le serveur MQTT : {}", e.getMessage());
            return;
        }

        log.info("Serveur démarré, en attente de commandes...");

        // on bloque le thread principal pour que le programme ne s'arrête pas tout seul
        // quand le processus reçoit un signal d'arrêt (Ctrl+C), on déconnecte proprement
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Arrêt du serveur en cours...");
            try {
                if (mqttClient.isConnected()) mqttClient.disconnect();
            } catch (MqttException e) {
                log.warn("Erreur lors de la déconnexion : {}", e.getMessage());
            }
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.warn("Thread principal interrompu — arrêt du serveur.");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Lit une valeur dans le fichier {@code config.properties} du classpath.
     *
     * @param cle la cle a rechercher (ex: {@code "broker.url"})
     * @return la valeur associee, ou {@code null} si le fichier est absent ou la cle inexistante
     */
    private static String lireConfig(String cle) {
        Properties proprietes = new Properties();

        // try-with-resources : le fichier est fermé automatiquement même si une erreur survient
        try (InputStream input = App.class.getResourceAsStream("/config.properties")) {
            if (input == null) {
                log.error("Fichier config.properties introuvable dans le classpath.");
                return null;
            }
            proprietes.load(input);
        } catch (IOException e) {
            log.error("Erreur lors de la lecture de config.properties : {}", e.getMessage());
            return null;
        }

        return proprietes.getProperty(cle);
    }
}
