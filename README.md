# Room & Event Booking (RBM)

Application web de **location et de gestion de salles** (séminaires, formations, mariages, événements d'entreprise).
Les clients consultent les salles, vérifient leur disponibilité et demandent une réservation ; l'administrateur gère les salles, valide les demandes et suit l'activité.
Le cœur fonctionnel est la **détection des conflits de réservation** : une salle ne peut pas être réservée deux fois sur des créneaux qui se chevauchent.

> Projet de la formation **FullStack Spring Boot & Angular** — Étudiante : **Majda Soussia** — Enseignante : **Dr. Hanen Harbaoui**.

---

## Démo en ligne

| Élément | Adresse |
|---|---|
| Application (Angular, Vercel) | https://rbm-booking-platform.vercel.app |
| API (Spring Boot, Render) | https://rbm-booking-platform.onrender.com |

- L'hébergement est **gratuit** : après 15 minutes sans trafic, l'API se met en veille et la **première requête peut prendre environ une minute**. Ouvrez l'application, patientez, puis rechargez.
- Comptes de démonstration : l'administrateur est `admin@rbm.com`. **Les mots de passe ne figurent pas dans ce dépôt** : ils sont communiqués séparément.
- Les photos téléversées sur la version en ligne sont **perdues quand l'API redémarre** (stockage éphémère de l'hébergeur gratuit).

---

## Fonctionnalités

### Client
- Consulter les salles avec photos, tarifs, capacité, équipements ; filtrer par capacité et équipements.
- Rechercher les salles libres sur un créneau.
- Créer une demande de réservation (prix total affiché avant l'envoi, calculé définitivement par le serveur).
- Consulter et annuler **ses** réservations (avant leur début).
- Inviter des participants par email depuis une réservation confirmée.
- Voir le calendrier : les créneaux occupés par les autres clients apparaissent sans titre ni nom.
- Créer un compte avec **vérification de l'adresse email**, modifier son profil et son mot de passe.

### Administrateur
- CRUD des salles (tarif et photo compris), des équipements et des utilisateurs.
- Voir toutes les réservations, **confirmer ou refuser** les demandes en attente.
- Calendrier global avec tous les détails, coloré par statut.
- Tableau de bord avec compteurs.

### Règles de gestion (couvertes par des tests unitaires)
1. `dateFin` strictement postérieure à `dateDebut` ; `dateDebut` non passée.
2. Pas de chevauchement entre deux réservations **confirmées** d'une même salle (`nouveauDebut < existantFin` et `nouveauFin > existantDebut` ; un créneau qui commence quand un autre se termine n'est pas un conflit).
3. `nbParticipants` ≤ capacité de la salle.
4. Une salle désactivée ne peut pas être réservée.
5. Workflow : `EN_ATTENTE` → `CONFIRMEE` ou `REFUSEE` ; annulation possible (`ANNULEE`) ; la confirmation revérifie l'absence de conflit.
6. Une salle ayant des réservations futures confirmées ne peut pas être supprimée (seulement désactivée).
7. Créneaux par pas de 30 minutes, durée minimale 1 heure ; `prixTotal = durée × tarifHoraire`, enregistré dans la réservation.

---

## Architecture

```
┌──────────────┐   HTTPS    ┌─────────────────────┐   JDBC (TLS)   ┌───────────────┐
│  Navigateur  │──────────▶│  Angular (Vercel)    │               │               │
└──────────────┘            │  /api, /uploads ─────┼──────────────▶│ Spring Boot   │──────▶ MySQL
                            │  (redirections)      │               │ (Render)      │
                            └─────────────────────┘               └──────┬────────┘
                                                                          │ HTTPS
                                                                          ▼
                                                                  Brevo (emails)
```

- **Backend** : Spring Boot (Java 17), architecture en couches *Controller → Service → Repository → DTO*, Spring Security + **JWT**, mots de passe **BCrypt**, gestion globale des exceptions (`@RestControllerAdvice`).
- **Frontend** : Angular (template Argon Dashboard adapté), routing par rôle, **Guards** (authentification, rôle), **Interceptor** (jeton JWT, erreurs 401/403), calendrier FullCalendar.
- **Base de données** : MySQL.
- **Conteneurisation** : `Dockerfile` multi-étapes (build Angular → intégration dans Spring Boot → `rbm.jar`), `docker-compose.yml`.
- **CI/CD** : Jenkins (build, tests, SonarQube, Nexus, Docker, déploiement).

### Structure du dépôt

```
rbm/
├── backend/            # API Spring Boot (+ Dockerfile pour l'hébergement de l'API seule)
├── frontend/           # Application Angular (+ vercel.json)
├── jenkins/            # Jenkins en Docker (Node 16, client Docker)
├── docs/               # Captures d'écran, preuves de tests, collection Postman
├── Dockerfile          # Image complète : Angular + Spring Boot
├── docker-compose.yml  # MySQL + application (+ Caddy en profil « prod », MailHog en profil « dev »)
├── Caddyfile           # HTTPS automatique pour un déploiement sur serveur
└── Jenkinsfile         # Pipeline CI/CD
```

---

## API (aperçu)

| Méthode | Route | Accès |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | Public |
| POST | `/api/auth/verify-email`, `/api/auth/resend-verification` | Public |
| GET | `/api/auth/me` | Connecté |
| GET | `/api/salles`, `/api/salles/disponibles?debut=&fin=&capacite=` | Connecté |
| POST/PUT/DELETE | `/api/salles`, `POST /api/salles/{id}/photo` | Admin |
| CRUD | `/api/equipements`, `/api/users` | Admin (lecture des équipements : connecté) |
| POST | `/api/reservations` | Connecté |
| GET | `/api/reservations/mes`, `/api/reservations/calendrier?debut=&fin=` | Connecté |
| GET | `/api/reservations` | Admin |
| PUT | `/api/reservations/{id}/confirmer`, `/refuser` | Admin |
| PUT | `/api/reservations/{id}/annuler` | Propriétaire ou admin |
| GET/POST | `/api/reservations/{id}/invitations` | Propriétaire ou admin |
| GET | `/api/dashboard` | Admin |

La collection Postman est dans `docs/`.

---

## Captures d'écran

Les images sont dans `docs/screenshots/`.

| | |
|---|---|
| ![Connexion](docs/screenshots/01-connexion.png) | ![Tableau de bord](docs/screenshots/02-dashboard.png) |
| ![Salles](docs/screenshots/03-salles.png) | ![Disponibilités](docs/screenshots/04-disponibilites.png) |
| ![Réservation](docs/screenshots/05-reservation.png) | ![Calendrier](docs/screenshots/06-calendrier.png) |
| ![Administration](docs/screenshots/07-admin-reservations.png) | ![Email de vérification](docs/screenshots/08-email-verification.png) |

### Intégration continue

| | |
|---|---|
| ![Pipeline Jenkins](docs/screenshots/09-jenkins-pipeline.png) | ![SonarQube](docs/screenshots/10-sonarqube.png) |
| ![Nexus](docs/screenshots/11-nexus.png) | ![Tests](docs/screenshots/12-tests.png) |

---

## Lancement en local

### Prérequis
Java 17, Node.js 16 ou 18, MySQL (par exemple XAMPP ou Docker), Docker Desktop (facultatif), Git.

### 1. Base de données
```sql
CREATE DATABASE IF NOT EXISTS rbm_db CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'rbm_user'@'localhost' IDENTIFIED BY 'un-mot-de-passe';
GRANT ALL PRIVILEGES ON rbm_db.* TO 'rbm_user'@'localhost';
```

### 2. Faux serveur mail (développement)
```bash
docker run -d --name mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
```
Les emails (vérification de compte, invitations) sont consultables sur http://localhost:8025.

### 3. Backend
```bash
cd backend
# Variables : DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, ADMIN_EMAIL, ADMIN_PASSWORD (voir application.yaml)
./mvnw spring-boot:run
```
L'API écoute sur http://localhost:8080. Un compte administrateur est créé au premier démarrage.

### 4. Frontend
```bash
cd frontend
npm ci --legacy-peer-deps
npx ng serve
```
Application sur http://localhost:4200. Avec `ng serve`, définir `APP_BASE_URL=http://localhost:4200` côté backend pour que le lien de l'email de vérification ouvre le bon site.

---

## Lancement avec Docker

Créer un fichier `.env` à la racine
```

```bash
docker compose up -d --build
```

L'application est servie sur http://localhost:8082 (Angular intégré dans le JAR Spring Boot). Les données MySQL et les photos sont conservées dans des volumes Docker.

Profils facultatifs :
- `docker compose --profile prod up -d` : ajoute **Caddy** (HTTPS automatique, variable `DOMAIN`) pour un serveur public.
- `docker compose --profile dev up -d` : ajoute **MailHog**.

---

## Tests

```bash
cd backend
./mvnw clean test
```

72 tests (JUnit 5, Mockito, H2) : règles de gestion des réservations, détection de conflit (chevauchement partiel, inclusion, créneaux adjacents, salle différente), authentification, vérification d'email, invitations, tableau de bord. Les tests d'API ont été réalisés avec Postman (`docs/`).

---

## CI/CD (Jenkins)

Jenkins, SonarQube et Nexus tournent dans Docker sur le poste de développement.

```bash
cd jenkins && docker compose up -d --build     # Jenkins : http://localhost:8080
docker start sonarqube nexus                    # SonarQube : :9000, Nexus : :8081
```

Pipeline (`Jenkinsfile`) :

| Étape | Rôle |
|---|---|
| Checkout | Récupération du code depuis GitHub |
| Build Maven et tests | `mvnw clean verify`, résultats JUnit |
| Analyse SonarQube | Qualité du code (jeton stocké dans les *Credentials*) |
| Publication Nexus | Dépôt du JAR (`maven-snapshots`) |
| Build Angular | `ng build --configuration production` |
| Build image Docker | Image `rbm-app` |
| Déploiement | `docker compose up -d`, puis contrôle de santé (accueil 200, API protégée 401) |

Les secrets (jeton SonarQube, compte Nexus, fichier `.env`) sont enregistrés dans les *Credentials* de Jenkins, jamais dans le dépôt.

---

## Déploiement

### Version en ligne (cloud)
- **Frontend** : Vercel. Le fichier `frontend/vercel.json` redirige `/api` et `/uploads` vers l'API : le navigateur ne parle qu'à un seul domaine.
- **API** : Render (service Docker construit depuis `backend/Dockerfile`).
- **Base de données** : MySQL compatible hébergé (TiDB Cloud), connexion chiffrée.
- **Emails** : Brevo par API HTTPS (le SMTP sortant est bloqué sur l'offre gratuite de l'hébergeur).

### Serveur VPS
Le `docker-compose.yml` fourni permet un déploiement sur un VPS Docker : MySQL avec volume persistant, application, et Caddy pour le HTTPS. Variables d'environnement dans un fichier `.env` propre au serveur.

---

## Sécurité

- Mots de passe hachés (**BCrypt**), authentification **JWT** sans session côté serveur, autorisation par rôle (`ADMIN`, `CLIENT`).
- Vérification de l'adresse email à l'inscription (jeton aléatoire, haché en base, valable 24 heures, usage unique).
- Le client ne voit et n'annule que **ses** réservations ; le calendrier masque les informations des autres clients.
- Aucune donnée secrète dans le dépôt : variables d'environnement (`.env` ignoré par Git).
- Limite connue : la déconnexion supprime le jeton du navigateur, mais le jeton reste valable jusqu'à son expiration (24 h).


## Crédits

Interface basée sur le template **Argon Dashboard Angular** de Creative Tim (licence MIT).
