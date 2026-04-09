package com.lunettes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;

public class Usine {

    private static final Logger log = LoggerFactory.getLogger(Usine.class);

    private final Fabricateur fabricateur;


    //File d'attente des demandes de production.
    // Principe de la mutualisation : Un thread de production tourne en permanence en arrière-plan.
    //Quand il est libre, il regarde combien de demandes attendent dans la file
    //et essaie d'en regrouper un maximum dans un seul lot (dans la limite de la capacité).
    private final LinkedBlockingQueue<Demande> fileDemandes = new LinkedBlockingQueue<>();

    public Usine(Fabricateur fabricateur) {
        this.fabricateur = fabricateur;
        // on démarre le thread de production en arrière-plan dès la création de l'usine
        Thread threadProduction = new Thread(this::boucleProduction, "thread-production");
        // daemon = true : ce thread s'arrête automatiquement quand le programme principal s'arrête
        threadProduction.setDaemon(true);
        threadProduction.start();
        log.info("Usine démarrée, capacité du fabricateur : {}", fabricateur.getCapacity());
    }

    //Point d'entrée : appelé par ServeurCallback pour chaque commande reçue.
    //On ajoute la demande dans la file et on attend que le thread de production nous renvoie le résultat via le CompletableFuture.
    // Cette méthode bloque le thread appelant jusqu'à la fin de la fabrication.
    public List<Lunette> produire(final Map<TypeLunette, Integer> typesLunettes) {
        List<TypeLunette> lunettesAFabriquer = transformerMapEnListe(typesLunettes);

        if (lunettesAFabriquer.isEmpty()) {
            return List.of();
        }

        // on crée un "ticket" pour cette commande
        CompletableFuture<List<Lunette>> futur = new CompletableFuture<>();
        fileDemandes.add(new Demande(lunettesAFabriquer, futur));
        log.info("Commande mise en file : {} lunettes à produire", lunettesAFabriquer.size());

        try {
            // on attend que le thread de production ait fini et renvoyé le résultat
            return futur.get();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'attente de la production", e);
        }
    }

    //Boucle principale du thread de production.
    //Elle tourne indéfiniment et traite les demandes une par une (ou groupées).
    private void boucleProduction() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // on attend qu'au moins une demande arrive (bloque si la file est vide)
                Demande premiere = fileDemandes.take();

                // on ramasse toutes les autres demandes déjà présentes dans la file pour essayer de les regrouper avec la première
                List<Demande> demandesGroupees = new ArrayList<>();
                demandesGroupees.add(premiere);
                fileDemandes.drainTo(demandesGroupees);

                // on traite le groupe de demandes ensemble
                traiterGroupe(demandesGroupees);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    //Fabrique toutes les lunettes d'un groupe de demandes.
    //Si le total dépasse la capacité du fabricateur, on découpe en lots.
    //Chaque commande reçoit ses lunettes via son CompletableFuture.

    private void traiterGroupe(List<Demande> demandes) {
        int capacite = fabricateur.getCapacity();

        // on met toutes les lunettes à fabriquer dans une seule liste, avec un index par commande
        List<TypeLunette> toutesLesLunettes = new ArrayList<>();
        for (Demande d : demandes) {
            toutesLesLunettes.addAll(d.lunettes);
        }

        log.info("Fabrication groupée : {} lunette(s) pour {} commande(s)", toutesLesLunettes.size(), demandes.size());

        // on découpe en lots si le total dépasse la capacité
        List<List<TypeLunette>> lots = decouperEnLots(toutesLesLunettes, capacite);
        List<Lunette> toutesLunettesProduites = new ArrayList<>();

        for (List<TypeLunette> lot : lots) {
            TypeLunette[] tableau = lot.toArray(new TypeLunette[0]);
            fabricateur.configurer(tableau);
            for (TypeLunette type : tableau) {
                toutesLunettesProduites.add(fabricateur.fabriquer(type));
            }
        }

        // on redistribue les lunettes produites à chaque commande dans l'ordre
        int index = 0;
        for (Demande demande : demandes) {
            int quantite = demande.lunettes.size();
            List<Lunette> lunettesDeCetteCommande = new ArrayList<>(
                toutesLunettesProduites.subList(index, index + quantite)
            );
            // on signale au thread appelant que sa commande est prête
            demande.futur.complete(lunettesDeCetteCommande);
            index += quantite;
        }
    }

    // transforme {CLAUDE -> 2, BANANA -> 1} en [CLAUDE, CLAUDE, BANANA]
    private List<TypeLunette> transformerMapEnListe(final Map<TypeLunette, Integer> typesLunettes) {
        List<TypeLunette> liste = new ArrayList<>();
        for (Map.Entry<TypeLunette, Integer> entry : typesLunettes.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                liste.add(entry.getKey());
            }
        }
        return liste;
    }

    // découpe une liste en sous-listes de taille maximale = capacite
    private List<List<TypeLunette>> decouperEnLots(List<TypeLunette> liste, int capacite) {
        List<List<TypeLunette>> lots = new ArrayList<>();
        for (int i = 0; i < liste.size(); i += capacite) {
            int fin = Math.min(i + capacite, liste.size());
            lots.add(new ArrayList<>(liste.subList(i, fin)));
        }
        return lots;
    }

    // représente une demande de fabrication en attente dans la file
    private static class Demande {
        final List<TypeLunette> lunettes;
        final CompletableFuture<List<Lunette>> futur;

        Demande(List<TypeLunette> lunettes, CompletableFuture<List<Lunette>> futur) {
            this.lunettes = lunettes;
            this.futur = futur;
        }
    }
}
