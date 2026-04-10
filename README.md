# ids-sms-routing

## Lancer RabbitMQ

```bash
./start-rmq
```

## Lancer les antennes

Chaque antenne est un processus indépendant à lancer dans un terminal séparé.

```bash
java -cp bin:"lib/*" AntenneImplementation <id> <fichier_config>
```

Exemple avec 3 antennes :
```bash
# Terminal 1
java -cp bin:"lib/*" AntenneImplementation 0 test/graphe-simple-ligne

# Terminal 2
java -cp bin:"lib/*" AntenneImplementation 1 test/graphe-simple-ligne

# Terminal 3
java -cp bin:"lib/*" AntenneImplementation 2 test/graphe-simple-ligne
```

Une fois lancée, chaque antenne affiche un prompt pour envoyer un message :
```
[0] Antenne prête. Format : <id_destinataire> <message>
> 2 hello
```

## Nettoyer

```bash
./stop-rmq
```
