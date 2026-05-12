package com.lunettes.controller;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.lunettes.model.CommandeModel;
import com.lunettes.model.Produit;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class CommandeController {

    private static final Logger log = LoggerFactory.getLogger(CommandeController.class);

    @FXML private Button btnValider;
    @FXML private Button btnRetour;

    // Labels et ComboBox injectés directement par fx:id — plus d'accès par index fragile
    @FXML private Label nomLabel0;  @FXML private Label prixLabel0;
    @FXML private Label descLabel0; @FXML private Label badgeLabel0; @FXML private ComboBox<String> combo0;

    @FXML private Label nomLabel1;  @FXML private Label prixLabel1;
    @FXML private Label descLabel1; @FXML private Label badgeLabel1; @FXML private ComboBox<String> combo1;

    @FXML private Label nomLabel2;  @FXML private Label prixLabel2;
    @FXML private Label descLabel2; @FXML private Label badgeLabel2; @FXML private ComboBox<String> combo2;

    @FXML private Label nomLabel3;  @FXML private Label prixLabel3;
    @FXML private Label descLabel3; @FXML private Label badgeLabel3; @FXML private ComboBox<String> combo3;

    private List<Produit> produits = lireJson("/products.json");
    private MqttClient mqttClient;

    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    @FXML
    public void initialize() {
        Label[] noms   = { nomLabel0,  nomLabel1,  nomLabel2,  nomLabel3  };
        Label[] prix   = { prixLabel0, prixLabel1, prixLabel2, prixLabel3 };
        Label[] descs  = { descLabel0, descLabel1, descLabel2, descLabel3 };
        Label[] badges = { badgeLabel0,badgeLabel1,badgeLabel2,badgeLabel3};

        for (int i = 0; i < produits.size(); i++) {
            Produit p = produits.get(i);
            noms[i].setText(p.name());
            prix[i].setText(String.format("%.2f €", p.price()));
            descs[i].setText(p.description());
            String badge = p.badge();
            if (badge != null && !badge.isBlank()) {
                badges[i].setText(badge);
                badges[i].setVisible(true);
            } else {
                badges[i].setVisible(false);
                badges[i].setManaged(false);
            }
        }
    }

    private List<Produit> lireJson(String jsonPath) {
        try {
            Reader reader = new InputStreamReader(getClass().getResourceAsStream(jsonPath));
            Produit[] tableau = new Gson().fromJson(reader, Produit[].class);
            return List.of(tableau);
        } catch (Exception e) {
            log.error("Erreur lecture JSON {} : {}", jsonPath, e.getMessage());
            return List.of();
        }
    }

    @FXML
    private void onRetour() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/accueil.fxml"));
        Scene scene = new Scene(loader.load());
        AccueilController ctrl = loader.getController();
        ctrl.setMqttClient(mqttClient);
        Stage stage = (Stage) btnRetour.getScene().getWindow();
        stage.setScene(scene);
    }

    @FXML
    private void onValider() {
        @SuppressWarnings("unchecked")
        ComboBox<String>[] combos = new ComboBox[]{ combo0, combo1, combo2, combo3 };

        CommandeModel commande = new CommandeModel();
        for (int i = 0; i < produits.size(); i++) {
            String valeur = combos[i].getValue() != null ? combos[i].getValue() : "0";
            commande.ajouterLigne(produits.get(i).id(), Integer.parseInt(valeur));
        }

        if (commande.estVide()) {
            afficherErreur("Commande vide", "Veuillez sélectionner au moins un produit avant de valider.");
            return;
        }

        String uuid = UUID.randomUUID().toString();
        String message = commande.serialiser();

        try {
            log.info("Envoi de la commande {} : {}", uuid, message);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fabrication.fxml"));
            Scene scene = new Scene(loader.load());

            FabricationController fabricationController = loader.getController();
            fabricationController.setMqttClient(mqttClient);
            fabricationController.setUuid(uuid);
            fabricationController.demarrer();

            mqttClient.publish("orders/" + uuid, new MqttMessage(message.getBytes()));

            Stage stage = (Stage) btnValider.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la commande : {}", e.getMessage());
            afficherErreur("Erreur réseau", "Impossible d'envoyer la commande.\nVérifiez que le serveur est bien démarré.");
        }
    }

    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
