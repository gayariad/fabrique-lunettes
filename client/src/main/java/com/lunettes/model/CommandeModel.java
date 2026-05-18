package com.lunettes.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Modele d'une commande de lunettes.
 * Stocke les produits selectionnes et sait les serialiser pour l'envoi MQTT.
 */
public class CommandeModel {

    private final Map<String, Integer> lignes = new HashMap<>();

    /**
     * Ajoute un produit a la commande avec la quantite voulue.
     * Une quantite de 0 ou moins est ignoree silencieusement.
     *
     * @param idProduit identifiant du produit (ex: {@code "claude"}), converti en majuscules
     * @param quantite  nombre d'unites demandees (doit etre > 0 pour etre pris en compte)
     */
    public void ajouterLigne(String idProduit, int quantite) {
        if (quantite > 0) {
            lignes.put(idProduit.toUpperCase(), quantite);
        }
    }

    /**
     * @return {@code true} si aucun produit n'a ete ajoute a la commande
     */
    public boolean estVide() {
        return lignes.isEmpty();
    }

    /**
     * @return une vue non modifiable des lignes de la commande (cle = id produit, valeur = quantite)
     */
    public Map<String, Integer> getLignes() {
        return Collections.unmodifiableMap(lignes);
    }

    /**
     * Serialise la commande au format {@code PRODUIT:quantite;PRODUIT2:quantite2}.
     *
     * @return chaine a publier sur le topic MQTT, vide si la commande est vide
     */
    public String serialiser() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : lignes.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }
}
