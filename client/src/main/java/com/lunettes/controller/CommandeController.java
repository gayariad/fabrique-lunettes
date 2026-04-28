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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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

    @FXML
    public void initialize() {
        for (int i = 0; i < produits.size(); i++) {
            VBox carte = (VBox) conteneurCartes.getChildren().get(i);
            Label nomLabel  = (Label) carte.getChildren().get(1);
            Label prixLabel = (Label) carte.getChildren().get(2);
            Label descLabel = (Label) carte.getChildren().get(3);
            nomLabel.setText(produits.get(i).name());
            prixLabel.setText(produits.get(i).price() + " EUR");
            descLabel.setText(produits.get(i).description());
        }
    }

    private List<Produit> lireJson(String path) {
        try {
            Reader reader = new InputStreamReader(getClass().getResourceAsStream(path));
            return List.of(new Gson().fromJson(reader, Produit[].class));
        } catch (Exception e) {
            log.error("Erreur lecture JSON {} : {}", path, e.getMessage());
            return List.of();
        }
    }

    @FXML
    private void onRetour() throws Exception {
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/accueil.fxml"));
        javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
        AccueilController ctrl = loader.getController();
        ctrl.setMqttClient(mqttClient);
        javafx.stage.Stage stage = (javafx.stage.Stage) btnRetour.getScene().getWindow();
        stage.setScene(scene);
    }

    @FXML
    private void onValider() {
        CommandeModel commande = new CommandeModel();
        for (int i = 0; i < conteneurCartes.getChildren().size(); i++) {
            VBox carte = (VBox) conteneurCartes.getChildren().get(i);
            ComboBox<String> combo = (ComboBox<String>) carte.getChildren().get(4);
            String valeur = combo.getValue() != null ? combo.getValue() : "0";
            commande.ajouterLigne(produits.get(i).id(), Integer.parseInt(valeur));
        }
        if (commande.estVide()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Commande vide");
            alert.setContentText("Selectionnez au moins un produit.");
            alert.showAndWait();
            return;
        }
        String uuid = UUID.randomUUID().toString();
        String message = commande.serialiser();
        try {
            mqttClient.publish("orders/" + uuid, new MqttMessage(message.getBytes()));
            log.info("Commande {} envoyee : {}", uuid, message);
            // TODO : naviguer vers FabricationController
        } catch (Exception e) {
            log.error("Erreur envoi commande : {}", e.getMessage());
        }
    }
}
