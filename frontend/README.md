# AQAR Immobilier

Interface React, TypeScript et Vite pour découvrir et gérer les annonces, visites, favoris et offres de l’agence.

## Développement local

Prérequis : Node.js 20.19+ ou 22+, npm et les services d’une pile de test. Depuis `frontend/` dans PowerShell :

```powershell
npm.cmd ci
Copy-Item .env.example .env.local
npm.cmd run dev
```

Ouvrir <http://localhost:5173>. Utiliser des bases jetables pour les tests d’écriture. Les cibles `AQAR_PROXY_*_URL` servent uniquement au proxy Vite local et à l’aperçu. Renseigner des origines HTTP(S) de bouclage sans chemin, identifiants ni secrets. Sans valeur, les ports locaux sont les suivants.

| API | Port | Variable du proxy local |
| --- | ---: | --- |
| Identité | 8085 | `AQAR_PROXY_IDENTITY_URL` |
| Annonces | 8081 | `AQAR_PROXY_ANNOUNCEMENTS_URL` |
| Favoris | 8083 | `AQAR_PROXY_FAVOURITES_URL` |
| Visites | 8084 | `AQAR_PROXY_VISITS_URL` |
| Offres | 8082 | `AQAR_PROXY_OFFERS_URL` |

Le proxy relaie les requêtes de même origine sur `/backend/<service>/api/…`. Conserver `http://localhost:5173` pour les piles locales dont les règles CORS l’exigent. Garder les clés JWT, mots de passe et paramètres JDBC dans l’environnement des services concernés.

```powershell
npm.cmd run typecheck
npm.cmd run build
npm.cmd run preview
```

Le build TypeScript précède Vite. Aucun script `test` ou `lint` n’est fourni dans ce projet. Le menu **Services** indique si les routes répondent. Un 401/403 sur une sonde sans jeton indique une route accessible et protégée.

## Compatibilité et activation

- **Identité.** L’inscription publique accepte Acheteur et Agent. Le backend refuse l’inscription Admin. La connexion valide le profil via `GET /api/users/me` et compare l’identifiant et le rôle au JWT avant d’ouvrir un espace.
- **Annonces.** Le catalogue et les fiches sont publics. Les filtres du catalogue sont appliqués sur les données renvoyées par l’API. Les permissions d’écriture et de propriété sont contrôlées par le serveur.
- **Favoris.** Activer `VITE_FAVOURITES_ENABLED=true` uniquement avec la version qui applique le rôle BUYER et la correspondance entre sujet JWT et client. La version de `main` au moment de la préparation ne fournit pas ces contrôles. L’interface laisse les favoris désactivés tant que cette version sécurisée n’est pas déployée.
- **Visites.** Activer `VITE_VISITS_ENABLED=true` uniquement avec la version qui contrôle le JWT, l’identité et la propriété des annonces. La correction du worktree sécurité n’est pas intégrée à `main`. Les routes et le champ `clientId` restent compatibles. L’interface cale ses actions sur les rôles et transitions sécurisés. Elle traite les conflits 409 et l’indisponibilité 503, par exemple si la propriété ne peut pas être vérifiée.
- **Offres.** Activer `VITE_OFFER_DECISIONS_ENABLED=true` avec une version qui propose les routes `PUT /api/offers/{id}/accept` et `PUT /api/offers/{id}/reject`. Les boutons Agent et Admin sont proposés pour les offres en attente. Les refus d’accès sont traités en 401/403. Une offre disparue est traitée en 404, et la liste est rechargée après un échec de décision. Sur la version actuelle de `main`, une seconde décision sur une offre traitée renvoie 500. La correction 409 existe dans le worktree sécurité.
- **Dépendance interservice.** Le service offres de `main` consulte `http://localhost:8081/api/announcements/{id}` pour vérifier le propriétaire. Un test local doit faire pointer cette adresse sur les annonces de la même pile. Le déploiement doit fournir un routage serveur approprié. Le proxy navigateur ne modifie pas cette adresse.

## Hébergement de l’artefact de production

Le dossier `dist/` est statique. Configurer le serveur d’hébergement ou la passerelle pour servir la SPA et acheminer `/backend/{identity,announcements,favourites,visits,offers}/api/…` vers les API correspondantes par chemin, en supprimant le préfixe `/backend/<service>` et en transmettant `Authorization`. Les noms de machines et les ports de production appartiennent à cette passerelle. Les requêtes API en erreur doivent garder leur statut HTTP. La réécriture vers `index.html` doit s’appliquer aux routes de la SPA.

Les variables publiques `VITE_*_ENABLED` se définissent à la compilation via la CI ou un fichier local ignoré. Elles indiquent une capacité déployée et ne constituent pas un contrôle serveur. Le build ne conserve pas le JWT dans le stockage du navigateur. Un rechargement de la version de production exige une nouvelle connexion avec le contrat actuel. Pour une session persistante en production, prévoir un contrat de session serveur ou BFF sous cookie HttpOnly et une stratégie de protection CSRF appropriée avant le déploiement utilisateur.

En développement, seul le JWT d’accès est conservé dans `sessionStorage` pour restaurer F5. Il reste lisible en cas de XSS. Le jeton de renouvellement reste en mémoire et n’est pas persisté. La restauration recharge le profil réel et efface le jeton en cas d’expiration ou de refus. La déconnexion révoque le jeton de renouvellement lorsqu’il est encore disponible en mémoire. Le JWT d’accès reste valide côté serveur jusqu’à son expiration normale.

## Photos locales

Images enregistrées localement dans ce navigateur uniquement. Elles ne sont pas stockées sur le serveur.

En développement, renseigner un nouvel identifiant `VITE_LOCAL_IMAGE_ENV` pour chaque nouvelle base d’annonces jetable. La galerie associe cet espace, l’identifiant d’annonce et l’identifiant agent. Elle accepte jusqu’à cinq fichiers JPG, PNG ou WebP, chacun de 5 Mo maximum, et les enregistre sous forme de Blob après validation de l’annonce par l’API. En cas de quota dépassé, la galerie affiche une erreur et permet de réessayer. Pour supprimer les images locales de tous les environnements, supprimer la base IndexedDB `maison-adresse-images` dans les outils de développement du navigateur. L’illustration d’accueil est une photographie Unsplash.
