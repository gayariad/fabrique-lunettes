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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CommandeController {

    private static final Logger log = LoggerFactory.getLogger(CommandeController.class);

    @FXML private HBox conteneurCartes;
    @FXML private Button btnValider;
    @FXML private Button btnRetour;

    private List<Produit> produits = lireJson("/products.json");
    private MqttClient mqttClient;

    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    // on remplit chaque carte produit avec les donnees lues depuis le JSON
    @FXML
    public void initialize() {
        for (int i = 0; i < produits.size(); i++) {
            VBox carte = (VBox) conteneurCartes.getChildren().get(i);
            Label nomLabel  = (Label) carte.getChildren().get(1);
            Label prixLabel = (Label) carte.getChildren().get(2);
            Label descLabel = (Label) carte.getChildren().get(3);
            nomLabel.setText(produits.get(i).name());
            prixLabel.setText(produits.get(i).price() + " €");
            descLabel.setText(produits.get(i).description());
        }
    }

    // lit le fichier JSON et retourne la liste des produits
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
        CommandeModel commande = new CommandeModel();
        for (int i = 0; i < conteneurCartes.getChildren().size(); i++) {
            VBox carte = (VBox) conteneurCartes.getChildren().get(i);
            ComboBox<String> combo = (ComboBox<String>) carte.getChildren().get(4);
            String valeur = combo.getValue() != null ? combo.getValue() : "0";
            int quantite = Integer.parseInt(valeur);
            commande.ajouterLigne(produits.get(i).id(), quantite);
        }

        // on refuse d'envoyer une commande vide et on previent l'utilisateur
        if (commande.estVide()) {
            afficherErreur("Commande vide", "Veuillez selectionner au moins un produit avant de valider.");
            return;
        }

        String uuid = UUID.randomUUID().toString();
        // le model sait comment serialiser : "CLAUDE:2;BANANA:1"
        String message = commande.serialiser();

        try {
            log.info("Envoi de la commande {} : {}", uuid, message);

            // on prepare l'ecran de fabrication avant d'envoyer le message
            // comme ca le controller est pret a recevoir la reponse du backend
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
            afficherErreur("Erreur reseau", "Impossible d'envoyer la commande.\nVerifiez que le serveur est bien demarre.");
        }
    }

    // affiche une boite de dialogue d'erreur a l'utilisateur
    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
