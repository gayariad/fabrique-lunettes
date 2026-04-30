package com.lunettes.controller;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class VerificationController {

    private static final Logger log = LoggerFactory.getLogger(VerificationController.class);

    @FXML private TextField txtSerial;
    @FXML private Button btnVerifier;
    @FXML private Label lblResultat;
    @FXML private Button btnRetour;

    private MqttClient mqttClient;
    // on garde le dernier serial vérifié pour se désabonner avant d'en vérifier un nouveau
    private String dernierSerial = null;

    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    @FXML
    private void onVerifier() {
        String serial = txtSerial.getText().trim();
        if (serial.isEmpty()) {
            lblResultat.setText("Veuillez saisir un numéro de série.");
            return;
        }

        try {
            // si l'utilisateur vérifie un serial différent du précédent, on se désabonne d'abord
            // sinon la réponse de l'ancienne vérification pourrait s'afficher par-dessus
            if (dernierSerial != null && !dernierSerial.equals(serial)) {
                mqttClient.unsubscribe("serials/" + dernierSerial);
                log.info("Désabonnement de serials/{}", dernierSerial);
            }

            // on s'abonne avec un listener dédié à ce serial, pas de setCallback() global
            // comme ça on ne perturbe pas les autres écrans s'ils sont actifs
            mqttClient.subscribe("serials/" + serial, (topic, msg) -> {
                String contenu = msg.toString();
                log.info("Résultat vérification serial {} : {}", serial, contenu);
                Platform.runLater(() -> {
                    if ("invalid".equals(contenu)) {
                        lblResultat.setText("Numéro de série invalide.");
                    } else {
                        lblResultat.setText("Série valide — Type : " + contenu);
                    }
                });
            });

            // on envoie la demande de vérification au backend (payload vide)
            mqttClient.publish("serials/" + serial + "/check", new MqttMessage());
            dernierSerial = serial;
            log.info("Vérification demandée pour le serial {}", serial);

        } catch (MqttException e) {
            log.error("Erreur MQTT lors de la vérification : {}", e.getMessage());
            lblResultat.setText("Erreur réseau. Vérifiez la connexion.");
        }
    }

    @FXML
    private void onRetour() throws Exception {
        // on se désabonne du dernier serial avant de quitter l'écran
        if (dernierSerial != null) {
            try {
                mqttClient.unsubscribe("serials/" + dernierSerial);
            } catch (MqttException e) {
                log.warn("Erreur désabonnement lors du retour : {}", e.getMessage());
            }
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/accueil.fxml"));
        Scene scene = new Scene(loader.load());
        AccueilController ctrl = loader.getController();
        ctrl.setMqttClient(mqttClient);
        Stage stage = (Stage) btnRetour.getScene().getWindow();
        stage.setScene(scene);
    }
}
