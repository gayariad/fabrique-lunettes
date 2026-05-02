package com.lunettes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import bernard_flou.Fabricateur.TypeLunette;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de ServeurCallback")
public class ServeurCallbackTest {

    @Mock
    private MqttClient mqttClientMock;

    @Mock
    private Usine usineMock;

    private ServeurCallback callback;

    @BeforeEach
    void setUp() {
        callback = new ServeurCallback(usineMock, mqttClientMock);
    }

    //deserialiserCommande

    @Test
    @DisplayName("deserialiserCommande : cas nominal avec un seul type")
    void deserialiserCommande_casNominal_unType() {
        Map<TypeLunette, Integer> result = callback.deserialiserCommande("CLAUDE:2");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2, result.get(TypeLunette.CLAUDE));
    }

    @Test
    @DisplayName("deserialiserCommande : cas nominal avec plusieurs types")
    void deserialiserCommande_casNominal_plusieursTypes() {
        Map<TypeLunette, Integer> result = callback.deserialiserCommande("CLAUDE:2;BANANA:3");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.get(TypeLunette.CLAUDE));
        assertEquals(3, result.get(TypeLunette.BANANA));
    }

    @Test
    @DisplayName("deserialiserCommande : type inconnu -> retourne null")
    void deserialiserCommande_typeInconnu_retourneNull() {
        assertNull(callback.deserialiserCommande("INCONNU:2"));
    }

    @Test
    @DisplayName("deserialiserCommande : segment sans ':' -> retourne null")
    void deserialiserCommande_formatSansSeparateur_retourneNull() {
        assertNull(callback.deserialiserCommande("CLAUDE"));
    }

    @Test
    @DisplayName("deserialiserCommande : quantite non numerique -> retourne null")
    void deserialiserCommande_quantiteNonNumerique_retourneNull() {
        assertNull(callback.deserialiserCommande("CLAUDE:abc"));
    }

    @Test
    @DisplayName("deserialiserCommande : chaine vide -> retourne null")
    void deserialiserCommande_chaineVide_retourneNull() {
        assertNull(callback.deserialiserCommande(""));
    }

    //validerCommande

    @Test
    @DisplayName("validerCommande : commande normale -> null (pas d'erreur)")
    void validerCommande_commandeNormale_pasErreur() {
        Map<TypeLunette, Integer> commande = Map.of(TypeLunette.CLAUDE, 2, TypeLunette.BANANA, 3);
        assertNull(callback.validerCommande(commande));
    }

    @Test
    @DisplayName("validerCommande : toutes les quantites a 0 -> erreur quantite totale")
    void validerCommande_toutesQuantitesZero_erreur() {
        Map<TypeLunette, Integer> commande = Map.of(TypeLunette.CLAUDE, 0);
        assertNotNull(callback.validerCommande(commande));
    }

    @Test
    @DisplayName("validerCommande : quantite = 10 (hors limite) -> erreur")
    void validerCommande_quantiteEgaleDixHorsLimite_erreur() {
        Map<TypeLunette, Integer> commande = Map.of(TypeLunette.CLAUDE, 10);
        assertNotNull(callback.validerCommande(commande));
    }

    @Test
    @DisplayName("validerCommande : quantite = 9 (limite valide) -> pas d erreur")
    void validerCommande_quantiteNeuf_pasErreur() {
        Map<TypeLunette, Integer> commande = Map.of(TypeLunette.CLAUDE, 9);
        assertNull(callback.validerCommande(commande));
    }

    @Test
    @DisplayName("validerCommande : quantite negative -> erreur")
    void validerCommande_quantiteNegative_erreur() {
        Map<TypeLunette, Integer> commande = Map.of(TypeLunette.CLAUDE, -1);
        assertNotNull(callback.validerCommande(commande));
    }

    @Test
    @DisplayName("validerCommande : commande vide (map vide) -> erreur quantite totale")
    void validerCommande_commandeVide_erreur() {
        assertNotNull(callback.validerCommande(Map.of()));
    }

    //messageArrived â€” commande valide

    @Test
    @DisplayName("messageArrived orders/{uuid} : publie validated puis delivery si production OK")
    void messageArrived_commandeValide_publieValidatedEtDelivery() throws Exception {
        when(usineMock.produire(any())).thenReturn(java.util.List.of());
        MqttMessage msg = new MqttMessage("CLAUDE:1".getBytes());
        callback.messageArrived("orders/test-uuid", msg);
        verify(mqttClientMock).publish(eq("orders/test-uuid/validated"), any(MqttMessage.class));
        verify(mqttClientMock).publish(eq("orders/test-uuid/delivery"), any(MqttMessage.class));
    }

    @Test
    @DisplayName("messageArrived orders/{uuid} : publie cancelled si commande invalide (format)")
    void messageArrived_commandeInvalide_publieCancelled() throws Exception {
        MqttMessage msg = new MqttMessage("FORMAT_INVALIDE".getBytes());
        callback.messageArrived("orders/test-uuid", msg);
        verify(mqttClientMock).publish(eq("orders/test-uuid/cancelled"), any(MqttMessage.class));
        verify(mqttClientMock, never()).publish(eq("orders/test-uuid/validated"), any(MqttMessage.class));
    }

    @Test
    @DisplayName("messageArrived orders/{uuid} : publie cancelled si quantite invalide (> 9)")
    void messageArrived_quantiteHorsLimite_publieCancelled() throws Exception {
        MqttMessage msg = new MqttMessage("CLAUDE:10".getBytes());
        callback.messageArrived("orders/test-uuid", msg);
        verify(mqttClientMock).publish(eq("orders/test-uuid/cancelled"), any(MqttMessage.class));
        verify(mqttClientMock, never()).publish(eq("orders/test-uuid/validated"), any(MqttMessage.class));
    }

    @Test
    @DisplayName("messageArrived orders/{uuid} : publie cancelled si quantite totale = 0")
    void messageArrived_quantiteTotaleZero_publieCancelled() throws Exception {
        MqttMessage msg = new MqttMessage("CLAUDE:0".getBytes());
        callback.messageArrived("orders/test-uuid", msg);
        verify(mqttClientMock).publish(eq("orders/test-uuid/cancelled"), any(MqttMessage.class));
        verify(mqttClientMock, never()).publish(eq("orders/test-uuid/validated"), any(MqttMessage.class));
    }

    @Test
    @DisplayName("messageArrived orders/{uuid} : publie error si l usine plante")
    void messageArrived_usinePlante_publieError() throws Exception {
        when(usineMock.produire(any())).thenThrow(new RuntimeException("Panne machine"));
        MqttMessage msg = new MqttMessage("CLAUDE:1".getBytes());
        callback.messageArrived("orders/test-uuid", msg);
        verify(mqttClientMock).publish(eq("orders/test-uuid/validated"), any(MqttMessage.class));
        verify(mqttClientMock).publish(eq("orders/test-uuid/error"), any(MqttMessage.class));
        verify(mqttClientMock, never()).publish(eq("orders/test-uuid/delivery"), any(MqttMessage.class));
    }

    @Test
    @DisplayName("messageArrived orders/{uuid}/validated : topic a 3 segments ignore")
    void messageArrived_topicTroisSegments_ignore() throws Exception {
        MqttMessage msg = new MqttMessage("CLAUDE:1".getBytes());
        callback.messageArrived("orders/test-uuid/validated", msg);
        verify(mqttClientMock, never()).publish(any(), any(MqttMessage.class));
    }
}
