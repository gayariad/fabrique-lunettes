# Fabrique de Lunettes

Projet étudiant — Application de commande et fabrication de lunettes en temps réel.

Le client (interface graphique) permet de passer des commandes de lunettes. Ces commandes sont transmises via **MQTT** à un backend Java qui simule la fabrication et renvoie les résultats au client.

```
Client (JavaFX)  <--MQTT-->  Broker (Mosquitto)  <--MQTT-->  Backend (Java)
```

---

## Prérequis

- **[Mosquitto](https://mosquitto.org/download/)** — broker MQTT à installer et démarrer avant tout
- **Java 17+** — uniquement pour le backend

Le client inclut Java, aucune installation supplémentaire requise.

---

## Lancement

### 1. Démarrer Mosquitto

**Windows :**
```
"C:\Program Files\mosquitto\mosquitto.exe" -v
```

**Linux/macOS :**
```
mosquitto -v
```

> Si Mosquitto tourne déjà en service Windows, cette étape n'est pas nécessaire.

---

### 2. Lancer le backend

Téléchargez `backend-runnable.jar` depuis la dernière [Release GitHub](../../releases/latest), puis :

```
java -jar backend-runnable.jar
```

---

### 3. Lancer le client

Téléchargez le fichier correspondant à votre OS depuis la dernière [Release GitHub](../../releases/latest) :

| OS | Fichier | Instructions |
|----|---------|--------------|
| Windows | `FabriqueLunettes-win.zip` | Dézipper et lancer `FabriqueLunettes\FabriqueLunettes.exe` |
| Linux | `FabriqueLunettes-linux.zip` | Dézipper et lancer `FabriqueLunettes/bin/FabriqueLunettes` |
| macOS | `FabriqueLunettes-mac.dmg` | Ouvrir le DMG et glisser l'app dans Applications |

Vous pouvez ouvrir **plusieurs fenêtres client** en même temps pour simuler plusieurs utilisateurs.

---

## Utilisation

1. Cliquez sur **Commander** depuis l'accueil
2. Choisissez les quantités par modèle et validez
3. L'écran de fabrication affiche le statut en temps réel
4. Les lunettes livrées apparaissent avec leur numéro de série
5. Vous pouvez vérifier un numéro de série depuis l'accueil

---

## Modèles disponibles

| Modèle | Prix |
|--------|------|
| Bananaaaa | 89,99 € |
| BlaBlaBla | 74,99 € |
| Miaousse | 129,99 € |
| Claude | 99,99 € |
