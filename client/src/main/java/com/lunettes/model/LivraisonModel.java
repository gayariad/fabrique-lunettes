package com.lunettes.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modele de la livraison recue depuis le backend.
 * Sait lire le payload brut et le transformer en donnees exploitables par le controller.
 */
public class LivraisonModel {

    private static final Logger log = LoggerFactory.getLogger(LivraisonModel.class);

    /**
     * Represente une lunette avec son type et son numero de serie.
     *
     * @param type   type de la lunette (ex: {@code "CLAUDE"})
     * @param serial numero de serie unique de la lunette
     */
    public record Lunette(String type, String serial) {}

    private final List<Lunette> lunettes = new ArrayList<>();

    /**
     * Lit le payload {@code "TYPE:serial;TYPE:serial;..."} recu du backend et remplit la liste.
     * Les segments malformes sont ignores avec un log d'avertissement.
     *
     * @param payload chaine brute recue sur le topic MQTT de livraison
     */
    public void deserialiser(String payload) {
        lunettes.clear();
        for (String partie : payload.split(";")) {
            String[] kv = partie.split(":");
            // chaque segment doit avoir exactement deux parties : type et serial
            if (kv.length < 2) {
                log.warn("Segment ignore dans la livraison (format invalide) : '{}'", partie);
                continue;
            }
            lunettes.add(new Lunette(kv[0], kv[1]));
        }
    }

    /**
     * Regroupe les lunettes par type pour l'affichage.
     * Ex: {@code [CLAUDE:s1, CLAUDE:s2, BANANA:s3] -> {CLAUDE=[s1,s2], BANANA=[s3]}}
     *
     * @return map non modifiable (type -> liste de serials), ordre d'insertion preserve
     */
    public Map<String, List<String>> grouperParType() {
        Map<String, List<String>> groupes = new LinkedHashMap<>();
        for (Lunette lunette : lunettes) {
            groupes.computeIfAbsent(lunette.type(), k -> new ArrayList<>()).add(lunette.serial());
        }
        return Collections.unmodifiableMap(groupes);
    }

    /**
     * @return le nombre total de lunettes recues dans cette livraison
     */
    public int getNombreLunettes() {
        return lunettes.size();
    }
}
