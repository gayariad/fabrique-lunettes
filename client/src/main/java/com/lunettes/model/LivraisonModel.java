package com.lunettes.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// le Model de la livraison : sait lire le payload brut du backend
// et transformer ca en donnees propres que le controller peut afficher
// le controller ne fait plus de parsing de texte, il appelle juste ce model
public class LivraisonModel {

    private static final Logger log = LoggerFactory.getLogger(LivraisonModel.class);

    // represente une lunette avec son type et son numero de serie
    public record Lunette(String type, String serial) {}

    // liste de toutes les lunettes recues
    private final List<Lunette> lunettes = new ArrayList<>();

    // lit le payload "TYPE:serial;TYPE:serial;..." et remplit la liste
    // les segments malformes sont ignores avec un log d'avertissement
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

    // regroupe les lunettes par type pour l'affichage
    // ex: [CLAUDE:s1, CLAUDE:s2, BANANA:s3] -> {CLAUDE=[s1,s2], BANANA=[s3]}
    // LinkedHashMap preserve l'ordre d'insertion pour que l'affichage soit stable
    public Map<String, List<String>> grouperParType() {
        Map<String, List<String>> groupes = new LinkedHashMap<>();
        for (Lunette lunette : lunettes) {
            groupes.computeIfAbsent(lunette.type(), k -> new ArrayList<>()).add(lunette.serial());
        }
        return Collections.unmodifiableMap(groupes);
    }

    // retourne le nombre total de lunettes recues
    public int getNombreLunettes() {
        return lunettes.size();
    }
}
