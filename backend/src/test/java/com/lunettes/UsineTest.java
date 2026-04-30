package com.lunettes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de Usine")
public class UsineTest {

    @Mock private Fabricateur fabricateurMock;
    private Usine usine;

    @BeforeEach
    void setUp() {
        when(fabricateurMock.getCapacity()).thenReturn(5);
        usine = new Usine(fabricateurMock);
    }

    @Test
    @DisplayName("produire : commande inferieure a la capacite -> un seul lot")
    void produire_commandePlusPetiteQueCapacite_unSeulLot() throws Exception {
        Lunette lunetteMock = mock(Lunette.class);
        when(fabricateurMock.fabriquer(any(TypeLunette.class))).thenReturn(lunetteMock);
        List<Lunette> resultat = usine.produire(Map.of(TypeLunette.CLAUDE, 2));
        assertEquals(2, resultat.size());
        verify(fabricateurMock, times(1)).configurer(any(TypeLunette[].class));
    }

    @Test
    @DisplayName("produire : commande vide -> liste vide")
    void produire_commandeVide_retourneListeVide() throws Exception {
        assertTrue(usine.produire(Map.of()).isEmpty());
        verify(fabricateurMock, never()).configurer(any(TypeLunette[].class));
    }
}
