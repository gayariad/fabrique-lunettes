package com.lunettes.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lunettes.model.LivraisonModel;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class FabricationController {

    private static final Logger log = LoggerFactory.getLogger(FabricationController.class);

    // si aucun "validated" n'arrive dans ce délai, on considère que le backend est absent
    private static final int TIMEOUT_SECONDES = 30;

    @FXML private Label lblStatut;
    @FXML private VBox conteneurResultats;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button btnRetour;

    private MqttClient mqttClient;
    private String uuid;

    private final LivraisonModel livraisonModel = new LivraisonModel();

    // scheduler qui gère le timeout, on démarre un timer, et si "validated" arrive on l'annule
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> tacheTimeout;

    // flag pour éviter d'afficher une erreur de timeout si la commande a déjà abouti
    private final AtomicBoolean commandeTerminee = new AtomicBoolean(false);

    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    // s'abonne aux topics de la commande et démarre le timer de timeout
    public void demarrer() {
        try {
            // on démarre le timer AVANT de s'abonner pour ne pas rater la fenêtre
            // si le backend répond très vite, "validated" peut arriver avant qu'on annule le timer
            // mais AtomicBoolean commandeTerminee évite d'afficher deux messages
            demarrerTimeout();

            mqttClient.subscribe("orders/" + uuid + "/validated", (topic, msg) -> {
                log.info("Commande {} validée par le serveur", uuid);
                // on annule le timeout le backend a bien reçu notre commande
                annulerTimeout();
                Platform.runLater(() -> lblStatut.setText("Commande validée, fabrication en cours..."));
            });

            mqttClient.subscribe("orders/" + uuid + "/status", (topic, msg) -> {
                // messages de statut intermédiaires envoyés par le backend pendant la fabrication
                String statut = msg.toString();
                log.info("Statut de la commande {} : {}", uuid, statut);
                Platform.runLater(() -> {
                    if ("processing".equals(statut)) {
                        lblStatut.setText("Fabrication en cours...");
                    } else if ("processed".equals(statut)) {
                        lblStatut.setText("Fabrication terminée, livraison en route...");
                    }
                });
            });

            mqttClient.subscribe("orders/" + uuid + "/delivery", (topic, msg) -> {
                String contenu = msg.toString();
                log.info("Livraison reçue pour la commande {}", uuid);
                livraisonModel.deserialiser(contenu);
                terminer(() -> {
                    lblStatut.setText("Commande livrée ! (" + livraisonModel.getNombreLunettes() + " lunettes)");
                    progressIndicator.setVisible(false);
                    afficherLivraison();
                    btnRetour.setVisible(true);
                });
            });

            mqttClient.subscribe("orders/" + uuid + "/error", (topic, msg) -> {
                log.warn("Erreur de production pour la commande {} : {}", uuid, msg.toString());
                terminer(() -> {
                    progressIndicator.setVisible(false);
                    afficherErreur("Le serveur n'a pas pu fabriquer les lunettes. Réessayez plus tard.");
                    btnRetour.setVisible(true);
                });
            });

            mqttClient.subscribe("orders/" + uuid + "/cancelled", (topic, msg) -> {
                String raison = msg.toString();
                log.warn("Commande {} annulée : {}", uuid, raison);
                terminer(() -> {
                    progressIndicator.setVisible(false);
                    afficherErreur("Commande refusée : " + (raison.isBlank() ? "format invalide" : raison));
                    btnRetour.setVisible(true);
                });
            });

            log.info("Abonné aux topics de la commande {}", uuid);

        } catch (MqttException e) {
            log.error("Erreur abonnement commande {} : {}", uuid, e.getMessage());
            annulerTimeout();
            afficherErreur("Impossible de suivre la commande. Vérifiez la connexion réseau.");
        }
    }

    // démarre le timer de timeout
    // si TIMEOUT_SECONDES s'écoulent sans réponse du backend, on affiche un message à l'utilisateur
    private void demarrerTimeout() {
        tacheTimeout = scheduler.schedule(() -> {
            if (commandeTerminee.get()) return;
            log.warn("Timeout : aucune réponse du backend pour la commande {} après {}s", uuid, TIMEOUT_SECONDES);
            seDesabonner();
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                afficherErreur("Aucune usine disponible. Vérifiez que le backend est démarré et réessayez.");
                btnRetour.setVisible(true);
            });
        }, TIMEOUT_SECONDES, TimeUnit.SECONDS);
    }

    // annule le timer de timeout (appelé quand "validated" arrive)
    private void annulerTimeout() {
        if (tacheTimeout != null && !tacheTimeout.isDone()) {
            tacheTimeout.cancel(false);
            log.info("Timeout annulé pour la commande {}", uuid);
        }
    }

    // marque la commande comme terminée, annule le timeout, se désabonne, puis met à jour l'UI
    private void terminer(Runnable miseAJourUI) {
        if (commandeTerminee.getAndSet(true)) return; // on ne termine qu'une seule fois
        annulerTimeout();
        seDesabonner();
        Platform.runLater(miseAJourUI);
    }

    // se désabonne de tous les topics de cette commande
    private void seDesabonner() {
        try {
            mqttClient.unsubscribe("orders/" + uuid + "/validated");
            mqttClient.unsubscribe("orders/" + uuid + "/status");
            mqttClient.unsubscribe("orders/" + uuid + "/delivery");
            mqttClient.unsubscribe("orders/" + uuid + "/error");
            mqttClient.unsubscribe("orders/" + uuid + "/cancelled");
            // on arrête le scheduler proprement pour libérer le thread
            scheduler.shutdownNow();
            log.info("Désabonné des topics de la commande {}", uuid);
        } catch (MqttException e) {
            log.warn("Erreur lors du désabonnement de la commande {} : {}", uuid, e.getMessage());
        }
    }

    @FXML
    private void onRetour() throws Exception {
        // si l'utilisateur quitte avant la fin, on nettoie quand même
        if (!commandeTerminee.get()) {
            annulerTimeout();
            seDesabonner();
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/accueil.fxml"));
        Scene scene = new Scene(loader.load());
        AccueilController ctrl = loader.getController();
        ctrl.setMqttClient(mqttClient);
        Stage stage = (Stage) btnRetour.getScene().getWindow();
        stage.setScene(scene);
    }

    // affiche les lunettes livrées regroupées par type
    private void afficherLivraison() {
        Map<String, List<String>> groupes = livraisonModel.grouperParType();
        for (Map.Entry<String, List<String>> entry : groupes.entrySet()) {
            Label titreType = new Label(entry.getKey() + " (" + entry.getValue().size() + ")");
            titreType.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: black;");
            conteneurResultats.getChildren().add(titreType);
            for (String serial : entry.getValue()) {
                Label lblSerial = new Label("  • " + serial);
                lblSerial.setStyle("-fx-font-size: 13px; -fx-text-fill: black;");
                conteneurResultats.getChildren().add(lblSerial);
            }
        }
    }

    // affiche un message d'erreur dans le label de statut en rouge
    private void afficherErreur(String message) {
        lblStatut.setText("Erreur : " + message);
        lblStatut.setStyle("-fx-text-fill: red;");
    }
}
