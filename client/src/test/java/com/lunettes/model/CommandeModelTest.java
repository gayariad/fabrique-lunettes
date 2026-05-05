package com.lunettes.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tests de CommandeModel")
public class CommandeModelTest {

    private CommandeModel commande;

    @BeforeEach
    void setUp() {
        commande = new CommandeModel();
    }

    //estVide

    @Test
    @DisplayName("estVide : nouvelle commande sans lignes -> true")
    void estVide_sansLignes_retourneTrue() {
        assertTrue(commande.estVide());
    }

    @Test
    @DisplayName("estVide : apres ajout d'une ligne valide -> false")
    void estVide_apresAjout_retourneFalse() {
        commande.ajouterLigne("CLAUDE", 2);
        assertFalse(commande.estVide());
    }

    //ajouterLigne

    @Test
    @DisplayName("ajouterLigne : quantite 0 -> ligne ignoree, commande reste vide")
    void ajouterLigne_quantiteZero_ignore() {
        commande.ajouterLigne("CLAUDE", 0);
        assertTrue(commande.estVide(), "une quantite de 0 ne doit pas ajouter de ligne");
    }

    @Test
    @DisplayName("ajouterLigne : id en minuscules -> stocke en majuscules")
    void ajouterLigne_minuscules_stockeEnMajuscules() {
        commande.ajouterLigne("claude", 1);
        assertTrue(commande.getLignes().containsKey("CLAUDE"));
    }

    @Test
    @DisplayName("ajouterLigne : plusieurs lignes -> toutes presentes dans la map")
    void ajouterLigne_plusieursLignes_toutesPresentes() {
        commande.ajouterLigne("CLAUDE", 2);
        commande.ajouterLigne("BANANA", 3);

        assertEquals(2, commande.getLignes().size());
        assertEquals(2, commande.getLignes().get("CLAUDE"));
        assertEquals(3, commande.getLignes().get("BANANA"));
    }

    @Test
    @DisplayName("ajouterLigne : meme id deux fois -> la deuxieme valeur ecrase la premiere")
    void ajouterLigne_memeIdDeuxFois_derniereValeurGagne() {
        commande.ajouterLigne("CLAUDE", 1);
        commande.ajouterLigne("CLAUDE", 5);

        assertEquals(1, commande.getLignes().size());
        assertEquals(5, commande.getLignes().get("CLAUDE"));
    }

    //serialiser

    @Test
    @DisplayName("serialiser : commande avec un seul produit -> 'TYPE:quantite'")
    void serialiser_unProduit_formatCorrect() {
        commande.ajouterLigne("CLAUDE", 2);

        assertEquals("CLAUDE:2", commande.serialiser());
    }

    @Test
    @DisplayName("serialiser : plusieurs produits -> segments separes par ';'")
    void serialiser_plusieursProduits_separesParPointVirgule() {
        commande.ajouterLigne("CLAUDE", 2);
        commande.ajouterLigne("BANANA", 1);

        String resultat = commande.serialiser();

        // on verifie que les deux segments sont presents
        assertTrue(resultat.contains("CLAUDE:2"), "doit contenir CLAUDE:2");
        assertTrue(resultat.contains("BANANA:1"), "doit contenir BANANA:1");
        assertTrue(resultat.contains(";"), "les segments doivent etre separes par ';'");
    }

    @Test
    @DisplayName("serialiser : commande vide -> chaine vide")
    void serialiser_commandeVide_chaineVide() {
        assertEquals("", commande.serialiser());
    }

    @Test
    @DisplayName("serialiser : pas de ';' en debut ou fin de chaine")
    void serialiser_pasDeSemicolonEnDebutOuFin() {
        commande.ajouterLigne("CLAUDE", 2);
        commande.ajouterLigne("BANANA", 1);

        String resultat = commande.serialiser();

        assertFalse(resultat.startsWith(";"), "ne doit pas commencer par ';'");
        assertFalse(resultat.endsWith(";"), "ne doit pas se terminer par ';'");
    }

    //getLignes — immutabilite

    @Test
    @DisplayName("getLignes : retourne une map non modifiable")
    void getLignes_retourneMapNonModifiable() {
        commande.ajouterLigne("CLAUDE", 1);

        assertThrows(UnsupportedOperationException.class, () -> {
            commande.getLignes().put("BANANA", 2);
        }, "getLignes() doit retourner une map non modifiable");
    }
}
