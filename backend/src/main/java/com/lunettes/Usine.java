package com.lunettes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;

public class Usine {

    private static final Logger log = LoggerFactory.getLogger(Usine.class);
    private final Fabricateur fabricateur;

    public Usine(Fabricateur fabricateur) {
        this.fabricateur = fabricateur;
        log.info("Usine dÃ©marrÃ©e, capacitÃ© : {}", fabricateur.getCapacity());
    }

    public List<Lunette> produire(final Map<TypeLunette, Integer> typesLunettes) {
        List<TypeLunette> lunettesAFabriquer = transformerMapEnListe(typesLunettes);
        if (lunettesAFabriquer.isEmpty()) {
            return List.of();
        }

        int capacite = fabricateur.getCapacity();
        List<List<TypeLunette>> lots = decouperEnLots(lunettesAFabriquer, capacite);
        List<Lunette> toutesLunettes = new ArrayList<>();

        for (List<TypeLunette> lot : lots) {
            TypeLunette[] tableau = lot.toArray(new TypeLunette[0]);
            fabricateur.configurer(tableau);
            for (TypeLunette type : tableau) {
                toutesLunettes.add(fabricateur.fabriquer(type));
            }
        }
        return toutesLunettes;
    }

    private List<TypeLunette> transformerMapEnListe(Map<TypeLunette, Integer> typesLunettes) {
        List<TypeLunette> liste = new ArrayList<>();
        for (Map.Entry<TypeLunette, Integer> entry : typesLunettes.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                liste.add(entry.getKey());
            }
        }
        return liste;
    }

    private List<List<TypeLunette>> decouperEnLots(List<TypeLunette> liste, int capacite) {
        List<List<TypeLunette>> lots = new ArrayList<>();
        for (int i = 0; i < liste.size(); i += capacite) {
            lots.add(new ArrayList<>(liste.subList(i, Math.min(i + capacite, liste.size()))));
        }
        return lots;
    }
}
