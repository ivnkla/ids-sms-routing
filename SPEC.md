Projet IDS


Specs :

Dans un premier temps ANTENNE=GENS (ensuite on verra le cas où les gens se déplacent)

- Il y a N antennes (Nodes) 
- Au lancement, chaque antenne connaît sa position (x, y), son rayon r, la position des autres antennes (x, y) et leur rayon respectif.
- Génère une matrice d’adjacence représentant la topologie des antennes
- Créer les queues entre chaques nodes en fonction de la matrice d’adjacence
- A noter que : pour un lien A—B, chaque nœud déclare les mêmes queues (rabbitmq) “ab” et ”ba”. L’unique différence réside dans l’appel de la méthode consume et publish.
- En termes de classe on aurait un truc comme ca 
  - Noeud/Antenne
      Int x
      Int y
      Int rayon    
      matrice(int, int)
    
Pour calculer le routage, on a trouvé deux méthodes : 
  - Utiliser l'algo de spanning tree pour supprimer les cycles dans le réseau. Ceci va créer une matrice de routage commune à tous les noeuds
  - Sinon, on broadcast le message.
Dans les deux cas, on a un protocole à gérer.


Exemple de lancement de programme :

Lancement du programme (le scripter pour aller plus vite)
./antenne <x_antenne> <y_antenne> <rayon_antenne> [<x_antenne_voisine> <y_antenne_voisine> <rayon_antenne_voisin> … ]
 
Autre idée : on donne la matrice au lancement calculée par un  programme intermédiaire.
./antenne 0 matrice-adj
./antenne <n> <matrice-adj>

Rappel TP
./noeud 1 in=0 out=2

