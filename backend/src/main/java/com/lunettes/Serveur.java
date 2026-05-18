package com.lunettes;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serveur MQTT de la fabrique de lunettes.
 * Se connecte au broker, s'abonne aux topics de commandes et de verification,
 * et delegue le traitement a {@link ServeurCallback}.
 */
public class Serveur {

    private static final Logger log = LoggerFactory.getLogger(Serveur.class);

    private final Usine usine;
    private final MqttClient mqttClient;

    /**
     * @param usine      l'usine qui fabrique les lunettes
     * @param mqttClient le client MQTT pre-cree (non encore connecte)
     */
    public Serveur(Usine usine, MqttClient mqttClient) {
        this.usine = usine;
        this.mqttClient = mqttClient;
    }

    /**
     * Connecte le client au broker MQTT et s'abonne aux topics {@code orders/+} et {@code serials/+/check}.
     * En cas d'echec, l'erreur remonte a {@link App} qui decide quoi faire.
     *
     * @param options options de connexion MQTT (timeout, reconnexion, etc.)
     * @throws MqttException si la connexion ou l'abonnement echoue
     */
    public void demarrer(MqttConnectOptions options) throws MqttException {
        // le callback reçoit et traite tous les messages MQTT entrants
        ServeurCallback callback = new ServeurCallback(usine, mqttClient);
        mqttClient.setCallback(callback);

        mqttClient.connect(options);

        // orders/+ : on reçoit uniquement les messages de niveau "orders/{uuid}"
        // le "+" remplace exactement un segment, on évite orders/# qui recevrait aussi nos propres publications (orders/{uuid}/validated, /delivery, etc.)
        mqttClient.subscribe("orders/+");
        // serials/+/check : on reçoit les demandes de vérification de numéro de série
        mqttClient.subscribe("serials/+/check");

        log.info("Connecté au broker, abonné à orders/+ et serials/+/check");
    }
}
