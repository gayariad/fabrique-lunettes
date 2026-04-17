package com.lunettes;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bernard_flou.Fabricateur.TypeLunette;

import java.util.HashMap;
import java.util.Map;

public class ServeurCallback implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(ServeurCallback.class);
    private final Usine usine;
    private final MqttClient mqttClient;

    public ServeurCallback(Usine usine, MqttClient mqttClient) {
        this.usine = usine;
        this.mqttClient = mqttClient;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        log.info("Message recu sur : {}", topic);
        if (topic.startsWith("orders/")) {
            traiterCommande(topic, message);
        } else if (topic.startsWith("serials/")) {
            traiterVerification(topic);
        }
    }

    private void traiterCommande(String topic, MqttMessage message) {
        String[] parts = topic.split("/");
        if (parts.length != 2) return;
        String uuid = parts[1];
        // TODO: deserialiser + valider + produire
        log.info("Commande recue : {}", uuid);
    }

    private void traiterVerification(String topic) {
        // TODO: implementer
    }

    private void publier(String topic, String contenu) {
        try {
            mqttClient.publish(topic, new MqttMessage(contenu.getBytes()));
        } catch (MqttException e) {
            log.error("Echec publication sur {} : {}", topic, e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("Connexion MQTT perdue : {}", cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}

    Map<TypeLunette, Integer> deserialiserCommande(String contenu) {
        if (contenu == null || contenu.isBlank()) return null;
        Map<TypeLunette, Integer> commande = new HashMap<>();
        for (String part : contenu.split(";")) {
            String[] kv = part.split(":");
            if (kv.length != 2) return null;
            try {
                commande.put(TypeLunette.valueOf(kv[0].trim()), Integer.parseInt(kv[1].trim()));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return commande;
    }
}
