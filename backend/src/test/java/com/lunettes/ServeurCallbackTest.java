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

    @Mock private MqttClient mqttClientMock;
    @Mock private Usine usineMock;
    private ServeurCallback callback;

    @BeforeEach
    void setUp() {
        callback = new ServeurCallback(usineMock, mqttClientMock);
    }

    @Test
    @DisplayName("deserialiserCommande : cas nominal un type")
    void deserialiserCommande_casNominal_unType() {
        Map<TypeLunette, Integer> result = callback.deserialiserCommande("CLAUDE:2");
        assertNotNull(result);
        assertEquals(2, result.get(TypeLunette.CLAUDE));
    }

    @Test
    @DisplayName("deserialiserCommande : type inconnu -> null")
    void deserialiserCommande_typeInconnu_retourneNull() {
        assertNull(callback.deserialiserCommande("INCONNU:2"));
    }

    @Test
    @DisplayName("deserialiserCommande : chaine vide -> null")
    void deserialiserCommande_chaineVide_retourneNull() {
        assertNull(callback.deserialiserCommande(""));
    }
}
