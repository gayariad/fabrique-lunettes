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

import com.lunettes.controller.AccueilController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private MqttClient mqttClient;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        String brokerUrl = lireConfig("broker.url");
        if (brokerUrl == null) {
            log.error("URL du broker introuvable dans config.properties — arrêt.");
            afficherErreurFatale(
                "Configuration manquante",
                "Le fichier config.properties est absent ou ne contient pas 'broker.url'.\n" +
                "L'application ne peut pas démarrer."
            );
            return;
        }

        mqttClient = creerEtConnecterClient(brokerUrl);
        if (mqttClient == null) {
            log.error("Impossible de se connecter au broker MQTT — arrêt de l'application.");
            afficherErreurFatale(
                "Connexion impossible",
                "Impossible de se connecter au broker MQTT :\n" + brokerUrl + "\n\n" +
                "Vérifiez que le backend est démarré et que l'adresse dans config.properties est correcte."
            );
            return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/accueil.fxml"));
        Scene scene = new Scene(loader.load());

        AccueilController accueilController = loader.getController();
        accueilController.setMqttClient(mqttClient);

        primaryStage.setTitle("La Fabrique de Lunettes");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> fermerProprement());
    }

    private MqttClient creerEtConnecterClient(String brokerUrl) {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);

            MqttClient client = new MqttClient(brokerUrl, UUID.randomUUID().toString());
            client.connect(options);
            log.info("Client MQTT connecté au broker {} avec l'id {}", brokerUrl, client.getClientId());
            return client;
        } catch (MqttException e) {
            log.error("Erreur de connexion MQTT : {}", e.getMessage());
            return null;
        }
    }

    private void fermerProprement() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                log.info("Client MQTT déconnecté proprement.");
            } catch (MqttException e) {
                log.warn("Erreur lors de la déconnexion MQTT : {}", e.getMessage());
            }
        }
    }

    // Affiche une Alert bloquante puis quitte — utilisé quand l'app ne peut pas démarrer.
    private void afficherErreurFatale(String titre, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        javafx.application.Platform.exit();
    }

    private String lireConfig(String cle) {
        Properties proprietes = new Properties();
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
