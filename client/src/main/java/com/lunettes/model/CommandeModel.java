package com.lunettes.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// le Model de la commande stocke ce que l'utilisateur a selectionne
// et sait comment transformer ca en message a envoyer au backend
// le controller ne fait plus de logique ici, il delegue tout a cette classe
public class CommandeModel {

    // cle = id du produit en majuscules (ex: "CLAUDE"), valeur = quantite choisie
    private final Map<String, Integer> lignes = new HashMap<>();

    // ajoute ou met a jour la quantite d'un produit dans la commande
    // si quantite = 0 on ignore, on ne veut pas envoyer des lignes vides
    public void ajouterLigne(String idProduit, int quantite) {
        if (quantite > 0) {
            lignes.put(idProduit.toUpperCase(), quantite);
        }
    }

    // retourne true si aucun produit n'a ete selectionne
    public boolean estVide() {
        return lignes.isEmpty();
    }

    // retourne une vue non modifiable des lignes de la commande
    public Map<String, Integer> getLignes() {
        return Collections.unmodifiableMap(lignes);
    }

    // transforme la commande en texte "CLAUDE:2;BANANA:1" pour l'envoyer via MQTT
    // StringBuilder evite de creer un objet String a chaque tour de boucle
    public String serialiser() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : lignes.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }
}
