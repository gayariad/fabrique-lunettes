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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controleur de l'ecran de verification de numero de serie.
 * Envoie une demande de verification via MQTT et affiche le resultat recu.
 */
public class VerificationController {

    private static final Logger log = LoggerFactory.getLogger(VerificationController.class);

    @FXML private TextField txtSerial;
    @FXML private Button btnVerifier;
    @FXML private Label lblResultat;
    @FXML private Button btnRetour;

    private MqttClient mqttClient;
    private String dernierSerial = null;

    /**
     * Transmet le client MQTT partage a ce controleur.
     *
     * @param mqttClient le client MQTT connecte
     */
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
            if (dernierSerial != null && !dernierSerial.equals(serial)) {
                mqttClient.unsubscribe("serials/" + dernierSerial);
                log.info("Désabonnement de serials/{}", dernierSerial);
            }

            // Feedback immédiat : l'utilisateur sait que la requête est partie
            lblResultat.setText("Vérification en cours...");
            btnVerifier.setDisable(true);

            mqttClient.subscribe("serials/" + serial, (topic, msg) -> {
                String contenu = msg.toString();
                log.info("Résultat vérification serial {} : {}", serial, contenu);
                Platform.runLater(() -> {
                    if ("invalid".equals(contenu)) {
                        lblResultat.setText("Numéro de série invalide.");
                    } else {
                        lblResultat.setText("Série valide — Type : " + contenu);
                    }
                    btnVerifier.setDisable(false);
                });
            });

            mqttClient.publish("serials/" + serial + "/check", new MqttMessage());
            dernierSerial = serial;
            log.info("Vérification demandée pour le serial {}", serial);

        } catch (MqttException e) {
            log.error("Erreur MQTT lors de la vérification : {}", e.getMessage());
            lblResultat.setText("Erreur réseau. Vérifiez la connexion.");
            btnVerifier.setDisable(false);
        }
    }

    @FXML
    private void onRetour() throws Exception {
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
