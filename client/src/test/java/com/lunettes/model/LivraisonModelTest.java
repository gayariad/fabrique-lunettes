package com.lunettes.model;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tests de LivraisonModel")
public class LivraisonModelTest {

    private LivraisonModel livraison;

    @BeforeEach
    void setUp() {
        livraison = new LivraisonModel();
    }

    //deserialiser

    @Test
    @DisplayName("deserialiser : payload avec une lunette -> une lunette dans la liste")
    void deserialiser_uneLunette_uneEntree() {
        livraison.deserialiser("CLAUDE:CL-001");

        assertEquals(1, livraison.getNombreLunettes());
    }

    @Test
    @DisplayName("deserialiser : payload avec plusieurs lunettes -> toutes chargees")
    void deserialiser_plusieursLunettes_toutesChargees() {
        livraison.deserialiser("CLAUDE:CL-001;CLAUDE:CL-002;BANANA:BA-003");

        assertEquals(3, livraison.getNombreLunettes());
    }

    @Test
    @DisplayName("deserialiser : segment malformed ignore, les autres sont charges")
    void deserialiser_segmentMalformed_ignore() {
        livraison.deserialiser("CLAUDE:CL-001;MAUVAIS;BANANA:BA-002");

        assertEquals(2, livraison.getNombreLunettes());
    }

    @Test
    @DisplayName("deserialiser : payload vide -> aucune lunette")
    void deserialiser_payloadVide_aucuneLunette() {
        livraison.deserialiser("");

        assertEquals(0, livraison.getNombreLunettes());
    }

    @Test
    @DisplayName("deserialiser : appel deux fois -> la deuxieme efface la premiere")
    void deserialiser_deuxFois_remplaceLaDonnee() {
        livraison.deserialiser("CLAUDE:CL-001;CLAUDE:CL-002");
        livraison.deserialiser("BANANA:BA-001");

        assertEquals(1, livraison.getNombreLunettes());
    }

    //grouperParType

    @Test
    @DisplayName("grouperParType : deux lunettes du meme type -> un groupe avec deux serials")
    void grouperParType_memeType_unGroupe() {
        livraison.deserialiser("CLAUDE:CL-001;CLAUDE:CL-002");
        Map<String, List<String>> groupes = livraison.grouperParType();

        assertEquals(1, groupes.size(), "un seul type donc un seul groupe");
        assertEquals(2, groupes.get("CLAUDE").size());
        assertTrue(groupes.get("CLAUDE").contains("CL-001"));
        assertTrue(groupes.get("CLAUDE").contains("CL-002"));
    }

    @Test
    @DisplayName("grouperParType : types differents -> un groupe par type")
    void grouperParType_typesDifferents_unGroupeParType() {
        livraison.deserialiser("CLAUDE:CL-001;BANANA:BA-001;BANANA:BA-002");
        Map<String, List<String>> groupes = livraison.grouperParType();

        assertEquals(2, groupes.size());
        assertEquals(1, groupes.get("CLAUDE").size());
        assertEquals(2, groupes.get("BANANA").size());
    }

    @Test
    @DisplayName("grouperParType : retourne une map non modifiable")
    void grouperParType_retourneMapNonModifiable() {
        livraison.deserialiser("CLAUDE:CL-001");

        assertThrows(UnsupportedOperationException.class, () -> {
            livraison.grouperParType().put("HACK", List.of());
        }, "grouperParType() doit retourner une map non modifiable");
    }

    @Test
    @DisplayName("grouperParType : l'ordre des types correspond a l'ordre d'arrivee")
    void grouperParType_ordrePreserve() {
        livraison.deserialiser("BANANA:BA-001;CLAUDE:CL-001;BANANA:BA-002");
        Map<String, List<String>> groupes = livraison.grouperParType();

        List<String> cles = List.copyOf(groupes.keySet());
        assertEquals("BANANA", cles.get(0), "BANANA est arrive en premier");
        assertEquals("CLAUDE", cles.get(1), "CLAUDE est arrive en deuxieme");
    }

    //getNombreLunettes

    @Test
    @DisplayName("getNombreLunettes : sans deserialiser -> 0")
    void getNombreLunettes_sansDeserialiser_zero() {
        assertEquals(0, livraison.getNombreLunettes());
    }
}
