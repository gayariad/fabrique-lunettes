package com.lunettes;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Serveur {

    private static final Logger log = LoggerFactory.getLogger(Serveur.class);
    private final Usine usine;
    private final MqttClient mqttClient;

    public Serveur(Usine usine, MqttClient mqttClient) {
        this.usine = usine;
        this.mqttClient = mqttClient;
    }

    public void demarrer(MqttConnectOptions options) throws MqttException {
        ServeurCallback callback = new ServeurCallback(usine, mqttClient);
        mqttClient.setCallback(callback);
        mqttClient.connect(options);
        // abonnement trop large : orders/# recoit aussi nos propres publications
        mqttClient.subscribe("orders/#");
        mqttClient.subscribe("serials/+/check");
        log.info("Serveur connecte, abonne a orders/# et serials/+/check");
    }
}
