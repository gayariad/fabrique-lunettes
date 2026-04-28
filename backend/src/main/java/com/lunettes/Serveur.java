package com.lunettes;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Serveur {

    private static final Logger log = LoggerFactory.getLogger(Serveur.class);

    private final Usine usine;
    // final : on reçoit le client dans le constructeur et on ne le change plus jamais
    private final MqttClient mqttClient;

    public Serveur(Usine usine, MqttClient mqttClient) {
        this.usine = usine;
        this.mqttClient = mqttClient;
    }

    // connecte au broker avec les options données et s'abonne aux topics
    // si ça plante, on laisse remonter l'erreur à App.java qui décidera quoi faire
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
