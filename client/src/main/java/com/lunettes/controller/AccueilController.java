package com.lunettes.controller;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class AccueilController {

    private static final Logger log = LoggerFactory.getLogger(AccueilController.class);

    @FXML private Button boutonCommander;
    @FXML private Button boutonVerifier;

    // le client MQTT est cree dans App.java et passe ici via setMqttClient()
    private MqttClient mqttClient;

    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    @FXML
    private void onCommander() throws Exception {
        log.info("Navigation vers la page de commande");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/commande.fxml"));
        Scene scene = new Scene(loader.load());

        // on transmet le client MQTT au controller suivant
        CommandeController controller = loader.getController();
        controller.setMqttClient(mqttClient);

        Stage stage = (Stage) boutonCommander.getScene().getWindow();
        stage.setScene(scene);
    }

    @FXML
    private void onVerifier() throws Exception {
        log.info("Navigation vers la page de verification");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/verification.fxml"));
        Scene scene = new Scene(loader.load());

        VerificationController controller = loader.getController();
        controller.setMqttClient(mqttClient);

        Stage stage = (Stage) boutonVerifier.getScene().getWindow();
        stage.setScene(scene);
    }
}
