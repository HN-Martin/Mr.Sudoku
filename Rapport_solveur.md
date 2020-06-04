Martin BONDU 370044
## Solveur de grille de sudoku
**1 Choix de l'algorithme**

Pour réaliser ce projet de solveur de sudoku j'ai choisi une méthode très intuitive à laquelle j'ai réfléchi par moi-même (de ce fait naïve). Cette méthode trouve la solution pour une grille à unique solution ou trouve une solution pour une grille à multiple solutions. 

Voici le fonctionnement : 

1.    A partir de la grille initial de sudoku, on affecte aux cases vides l'ensemble des valeurs qu'elles peuvent prendre sans engendrer de conflits. Ainsi une case vide a au plus un ensemble de 9 valeurs, au moins 1 valeur. Si elles en a 0, c'est que la grille de départ est insolvable.
2. Si la grille est entièrement remplie de valeurs n'étant pas des ensembles, c'est qu'elle est résolue : on s'arrête. Sinon 
3. On parcourt la grille en cherchant la case vide ayant l'ensemble de valeurs possibles le plus petit qu'on appellera Cmin.
4. On affecte arbitrairement à Cmin une de ses valeures possibles puis 
5. On met à jour les ensembles de valeurs possibles des autres cases du bloc de Cmin, de la rangée de Cmin et de la colonne de Cmin.
6. On répète à partir de 2.

___

**2. Implémentation**

L'étape 1. est réalisée par la fonction `init-grid`.
L'étape 3. par `next-cell`. 
L'affectation de Cmin est faite par `set-cell`qui fait au passage la mise à jour du bloc par l'appel de `change-block`, la mise à jour de la colonne par l'appel de `change-col`et de la rangée par `change-row`.
Le tout est assemblé dans la fonction `solve`.

___
**3. Avantages et inconvénients**

 Le principal problème est que je ne dispose d'aucune preuve que cette méthode fonctionne sur toute grille ayant plus de 1 solution, ça s'est avéré fonctionner sur une vingtaine d'exemple de grilles. 
 Un autre problème est la complexité puisqu'au pire cas on a 1. parcourant 81 cases, 80 fois l'étape 6 et 5. soit 81  recherches de Cmin puis mises à jour de sa colonne/ligne/bloc. Chaque recherche faisant au pire 81 itérations, et chacune des 3 mises à jour représente 9  itérations on a en complexité pire cas : 
 81 +81*(81 + 3*9) = 8 829 itérations soit pour un sudoku de n case du O(_n2_).

Un des avantages de cette méthode est qu'elle ne trouve qu'une solution. Ainsi il est très rapide de construire des grilles aléatoires : il suffit de placer aléatoirement quelques chiffres aléatoires non en conflit sur la grille à des positions aléatoires et demander de la résoudre. On peut ensuite effacer un certain nombre de cases de cette grille résolu pour obtenir une grille à résoudre.

