# ids-sms-routing

Simulation de routage de messages SMS sur un réseau d'antennes distribué.
Chaque antenne et chaque téléphone est un processus JVM indépendant.
La communication passe entièrement par RabbitMQ (exchanges fanout + queues).

## Prérequis

- Java 11+
- Docker

## Compilation

```bash
javac -cp "lib/*" -d bin src/*.java
```

## Lancer RabbitMQ

```bash
./start-rmq
```

## Lancer les antennes

Chaque antenne est un processus indépendant à lancer dans un terminal séparé.

```bash
java -cp bin:"lib/*" AntenneImplementation <id> <fichier_config>
```

Exemple avec 3 antennes en ligne (`test/graphe-simple-ligne`) :

```bash
# Terminal 1
java -cp bin:"lib/*" AntenneImplementation 0 test/graphe-simple-ligne

# Terminal 2
java -cp bin:"lib/*" AntenneImplementation 1 test/graphe-simple-ligne

# Terminal 3
java -cp bin:"lib/*" AntenneImplementation 2 test/graphe-simple-ligne
```

Prompt antenne (envoi d'un message vers l'antenne 2) :
```
antenne> 2 hello
```

Commandes disponibles : `quit` pour arrêter le processus.

## Lancer des téléphones

Les téléphones nécessitent un fichier de config avec une section téléphones
(ex. `test/graphe-simple-ligne-utilisateurs`).

```bash
java -cp bin:"lib/*" Phone test/graphe-simple-ligne-utilisateurs
```

Le programme affiche la liste des téléphones disponibles, demande d'en choisir un,
puis ouvre un prompt d'envoi.

Prompt téléphone (envoi du message "bonjour" au téléphone 2) :
```
phone> bonjour 2
```

Commandes disponibles : `quit` pour arrêter le processus.

## Format du fichier de configuration

```
N              # nombre d'antennes
x0 y0 r0       # position (x, y) et rayon r de l'antenne 0
x1 y1 r1
...
M              # (optionnel) nombre de téléphones
px0 py0        # position du téléphone 0
px1 py1
...
```

Deux antennes sont voisines si la distance entre leurs centres est ≤ r1 + r2.
Un téléphone se connecte à l'antenne la plus proche dans son rayon de couverture.

## Fichiers de test disponibles

| Fichier | Description |
|---|---|
| `test/graphe-simple-ligne` | 3 antennes en ligne, sans téléphones |
| `test/graphe-simple-ligne-utilisateurs` | 3 antennes en ligne, 3 téléphones |
| `test/graphe-connecte` | 4 antennes formant un carré connecté |
| `test/graphe-mauvais` | Fichier malformé (test de la gestion d'erreur) |

## Arrêter RabbitMQ

```bash
./stop-rmq
```
