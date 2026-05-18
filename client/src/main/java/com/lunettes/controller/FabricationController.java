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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.lunettes.model.LivraisonModel;
import com.lunettes.model.Produit;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controleur de l'ecran de suivi de fabrication.
 * S'abonne aux topics MQTT de la commande en cours et met a jour l'interface
 * selon les statuts recus (validation, fabrication, livraison, erreur).
 */
public class FabricationController {

    private static final Logger log = LoggerFactory.getLogger(FabricationController.class);
    private static final int TIMEOUT_SECONDES = 30;

    @FXML private Label lblStatut;
    @FXML private VBox conteneurResultats;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button btnRetour;

    private MqttClient mqttClient;
    private String uuid;
    private final Map<String, String> nomParId = chargerNoms();

    private final LivraisonModel livraisonModel = new LivraisonModel();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> tacheTimeout;
    private final AtomicBoolean commandeTerminee = new AtomicBoolean(false);

    /**
     * Transmet le client MQTT partage a ce controleur.
     *
     * @param mqttClient le client MQTT connecte
     */
    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    /**
     * Definit l'identifiant unique de la commande a suivre.
     *
     * @param uuid UUID de la commande, utilise pour construire les topics MQTT
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * S'abonne aux topics MQTT de la commande et demarre le timeout de securite.
     * Doit etre appele apres {@link #setMqttClient} et {@link #setUuid}.
     */
    public void demarrer() {
        try {
            demarrerTimeout();

            mqttClient.subscribe("orders/" + uuid + "/validated", (topic, msg) -> {
                log.info("Commande {} validée par le serveur", uuid);
                annulerTimeout();
                Platform.runLater(() -> lblStatut.setText("Commande validée, fabrication en cours..."));
            });

            mqttClient.subscribe("orders/" + uuid + "/status", (topic, msg) -> {
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

    /**
     * Se desabonne des topics MQTT et arrete le scheduler.
     * Doit etre appele quand l'utilisateur ferme la fenetre, pour eviter que
     * le scheduler ne retarde l'arret de la JVM.
     */
    public void nettoyer() {
        if (!commandeTerminee.get()) {
            annulerTimeout();
            seDesabonner();
        }
    }

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

    private void annulerTimeout() {
        if (tacheTimeout != null && !tacheTimeout.isDone()) {
            tacheTimeout.cancel(false);
            log.info("Timeout annulé pour la commande {}", uuid);
        }
    }

    private void terminer(Runnable miseAJourUI) {
        if (commandeTerminee.getAndSet(true)) return;
        annulerTimeout();
        seDesabonner();
        Platform.runLater(miseAJourUI);
    }

    private void seDesabonner() {
        try {
            mqttClient.unsubscribe("orders/" + uuid + "/validated");
            mqttClient.unsubscribe("orders/" + uuid + "/status");
            mqttClient.unsubscribe("orders/" + uuid + "/delivery");
            mqttClient.unsubscribe("orders/" + uuid + "/error");
            mqttClient.unsubscribe("orders/" + uuid + "/cancelled");
            scheduler.shutdownNow();
            log.info("Désabonné des topics de la commande {}", uuid);
        } catch (MqttException e) {
            log.warn("Erreur lors du désabonnement de la commande {} : {}", uuid, e.getMessage());
        }
    }

    @FXML
    private void onRetour() throws Exception {
        nettoyer();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/accueil.fxml"));
        Scene scene = new Scene(loader.load());
        AccueilController ctrl = loader.getController();
        ctrl.setMqttClient(mqttClient);
        Stage stage = (Stage) btnRetour.getScene().getWindow();
        stage.setScene(scene);
    }

    private void afficherLivraison() {
        Map<String, List<String>> groupes = livraisonModel.grouperParType();
        for (Map.Entry<String, List<String>> entry : groupes.entrySet()) {
            String id = entry.getKey();
            String nom = nomParId.getOrDefault(id.toLowerCase(), id);
            Label titreType = new Label(nom + " (" + entry.getValue().size() + ")");
            titreType.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: black;");
            conteneurResultats.getChildren().add(titreType);
            for (String serial : entry.getValue()) {
                Label lblSerial = new Label("  • " + serial);
                lblSerial.setStyle("-fx-font-size: 13px; -fx-text-fill: black;");
                conteneurResultats.getChildren().add(lblSerial);
            }
        }
    }

    private java.util.Map<String, String> chargerNoms() {
        try {
            Reader reader = new InputStreamReader(getClass().getResourceAsStream("/products.json"));
            Produit[] produits = new Gson().fromJson(reader, Produit[].class);
            return Arrays.stream(produits)
                .collect(Collectors.toMap(p -> p.id().toLowerCase(), Produit::name));
        } catch (Exception e) {
            log.warn("Impossible de charger products.json pour la résolution des noms : {}", e.getMessage());
            return Map.of();
        }
    }

    private void afficherErreur(String message) {
        lblStatut.setText("Erreur : " + message);
        lblStatut.setStyle("-fx-text-fill: red;");
    }
}
