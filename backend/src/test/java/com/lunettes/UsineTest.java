package com.lunettes;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de Usine")
public class UsineTest {

    @Mock
    private Fabricateur fabricateurMock;

    private Usine usine;

    @BeforeEach
    void setUp() {
        // getCapacity() est appelé dans le constructeur pour le log, on le configure ici
        when(fabricateurMock.getCapacity()).thenReturn(5);
        usine = new Usine(fabricateurMock);
    }

    //produire (cas de base)

    @Test
    @DisplayName("produire : commande inférieure à la capacité -> un seul lot")
    void produire_commandePlusPetiteQueCapacite_unSeulLot() throws Exception {
        Lunette lunetteMock = mock(Lunette.class);
        when(fabricateurMock.fabriquer(any(TypeLunette.class))).thenReturn(lunetteMock);

        // capacité = 5, commande = 2 -> tout passe en un seul lot
        Map<TypeLunette, Integer> commande = Map.of(TypeLunette.CLAUDE, 2);
        List<Lunette> resultat = usine.produire(commande);

        assertEquals(2, resultat.size());
        verify(fabricateurMock, times(1)).configurer(any(TypeLunette[].class));
    }

    @Test
    @DisplayName("produire : commande égale à la capacité -> un seul lot exact")
    void produire_commandeEgaleCapacite_unSeulLot() throws Exception {
        Lunette lunetteMock = mock(Lunette.class);
        when(fabricateurMock.fabriquer(any(TypeLunette.class))).thenReturn(lunetteMock);

        Map<TypeLunette, Integer> commande = Map.of(TypeLunette.CLAUDE, 5);
        List<Lunette> resultat = usine.produire(commande);

        assertEquals(5, resultat.size());
        verify(fabricateurMock, times(1)).configurer(any(TypeLunette[].class));
    }

    @Test
    @DisplayName("produire : commande supérieure à la capacité -> plusieurs lots")
    void produire_commandePlusGrandeQueCapacite_plusieursLots() throws Exception {
        // capacité = 5, commande = 7 -> 2 lots : [5, 2]
        Lunette lunetteMock = mock(Lunette.class);
        when(fabricateurMock.fabriquer(any(TypeLunette.class))).thenReturn(lunetteMock);

        Map<TypeLunette, Integer> commande = Map.of(TypeLunette.CLAUDE, 7);
        List<Lunette> resultat = usine.produire(commande);

        assertEquals(7, resultat.size());
        // 2 appels à configurer : lot[5] + lot[2]
        verify(fabricateurMock, times(2)).configurer(any(TypeLunette[].class));
    }

    @Test
    @DisplayName("produire : commande avec plusieurs types de lunettes")
    void produire_commandePlusieursTypes() throws Exception {
        Lunette lunetteMock = mock(Lunette.class);
        when(fabricateurMock.fabriquer(any(TypeLunette.class))).thenReturn(lunetteMock);

        // CLAUDE:2 + BANANA:3 = 5 lunettes en tout, tient dans un seul lot (capacité=5)
        Map<TypeLunette, Integer> commande = Map.of(
            TypeLunette.CLAUDE, 2,
            TypeLunette.BANANA, 3
        );
        List<Lunette> resultat = usine.produire(commande);

        assertEquals(5, resultat.size());
        verify(fabricateurMock, times(1)).configurer(any(TypeLunette[].class));
    }

    @Test
    @DisplayName("produire : commande vide -> retourne une liste vide sans appeler le fabricateur")
    void produire_commandeVide_retourneListeVide() throws Exception {
        Map<TypeLunette, Integer> commande = Map.of();
        List<Lunette> resultat = usine.produire(commande);

        assertTrue(resultat.isEmpty());
        // si rien à fabriquer, configurer ne doit jamais être appelé
        verify(fabricateurMock, never()).configurer(any(TypeLunette[].class));
    }

    @Test
    @DisplayName("produire : deux commandes en parallèle -> résultats corrects pour chacune")
    void produire_deuxCommandesEnParallele_resultatsCorrects() throws Exception {
        Lunette lunetteMock = mock(Lunette.class);
        when(fabricateurMock.fabriquer(any(TypeLunette.class))).thenReturn(lunetteMock);

        // on lance deux commandes en même temps depuis deux threads différents l'usine doit renvoyer le bon nombre de lunettes à chacune
        var futur1 = java.util.concurrent.CompletableFuture.supplyAsync(() ->
            usine.produire(Map.of(TypeLunette.CLAUDE, 2))
        );
        var futur2 = java.util.concurrent.CompletableFuture.supplyAsync(() ->
            usine.produire(Map.of(TypeLunette.BANANA, 3))
        );

        List<Lunette> resultat1 = futur1.get();
        List<Lunette> resultat2 = futur2.get();

        // chaque commande doit avoir reçu exactement le bon nombre de lunettes
        assertEquals(2, resultat1.size());
        assertEquals(3, resultat2.size());
    }
}
