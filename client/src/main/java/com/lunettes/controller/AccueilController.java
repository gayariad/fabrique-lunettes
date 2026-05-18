package com.lunettes.controller;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 * Contrôleur de l'écran d'accueil.
 * Permet à l'utilisateur de naviguer vers la page de commande ou de vérification.
 */
public class AccueilController {

    private static final Logger log = LoggerFactory.getLogger(AccueilController.class);

    @FXML private Button boutonCommander;
    @FXML private Button boutonVerifier;

    private MqttClient mqttClient;

    /**
     * Transmet le client MQTT partagé à ce contrôleur.
     *
     * @param mqttClient le client MQTT connecté, créé dans {@link com.lunettes.App}
     */
    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    /**
     * Navigue vers la page de commande.
     *
     * @throws Exception si le chargement du fichier FXML échoue
     */
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

    /**
     * Navigue vers la page de verification de numero de serie.
     *
     * @throws Exception si le chargement du fichier FXML echoue
     */
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
