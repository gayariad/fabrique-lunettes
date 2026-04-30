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
import javafx.stage.Stage;

public class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    // le client MQTT est gardé ici pour pouvoir le fermer proprement quand l'appli se ferme
    private MqttClient mqttClient;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // on lit l'adresse du broker depuis config.properties (B3 : ne pas mettre l'URL en dur)
        String brokerUrl = lireConfig("broker.url");
        if (brokerUrl == null) {
            log.error("URL du broker introuvable dans config.properties — arrêt.");
            primaryStage.close();
            return;
        }

        // on crée le client MQTT une seule fois et on le passe à chaque écran
        mqttClient = creerEtConnecterClient(brokerUrl);
        if (mqttClient == null) {
            log.error("Impossible de se connecter au broker MQTT — arrêt de l'application.");
            primaryStage.close();
            return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/accueil.fxml"));
        Scene scene = new Scene(loader.load());

        AccueilController accueilController = loader.getController();
        accueilController.setMqttClient(mqttClient);

        primaryStage.setTitle("La Fabrique de Lunettes");
        primaryStage.setScene(scene);
        primaryStage.show();

        // quand l'utilisateur ferme la fenêtre, on déconnecte proprement le client MQTT
        primaryStage.setOnCloseRequest(event -> fermerProprement());
    }

    // crée un client MQTT et le connecte au broker avec des options adaptées
    // retourne null si la connexion échoue
    private MqttClient creerEtConnecterClient(String brokerUrl) {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            // reconnexion automatique si la connexion est perdue (réseau coupé, broker redémarré...)
            options.setAutomaticReconnect(true);
            // cleanSession=true : on repart d'un état propre à chaque démarrage
            options.setCleanSession(true);
            // timeout de connexion : 10 secondes avant d'abandonner
            options.setConnectionTimeout(10);
            // keepalive : le client envoie un ping toutes les 30s pour garder la connexion active
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

    // ferme la connexion MQTT proprement quand l'application se ferme
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

    // lit une valeur dans config.properties embarqué dans le JAR
    // retourne null si le fichier est absent ou si la clé n'existe pas
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
