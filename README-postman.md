# ws-events — Utilisation avec Postman

## Prérequis

- Serveur démarré : `mvn spring-boot:run` → http://localhost:8080
- Postman installé (v9+)

---

## 1. Variable d'environnement

Dans Postman, créer un environnement **ws-events Local** avec deux variables :

| Variable  | Initial Value         |
|-----------|-----------------------|
| `baseUrl` | `http://localhost:8080` |
| `token`   | *(vide)*              |

Activer l'environnement (menu déroulant en haut à droite).

---

## 2. Obtenir un token JWT

**POST** `{{baseUrl}}/api/auth/token`

- **Body** → raw → JSON :
```json
{ "username": "user1" }
```

Utilisateurs disponibles : `user1`, `user2`, `admin`.

Dans l'onglet **Tests**, coller ce script pour stocker le access_token automatiquement :
```javascript
pm.environment.set("access_token", pm.response.json().token);
```

Réponse attendue (200) :
```json
{
  "access_token": "eyJhbGci...",
  "username": "user1",
  "roles": ["USER"],
  "expirationMs": 3600000
}
```

---

## 3. Publier un événement

**POST** `{{baseUrl}}/api/events`

- **Headers** :

| Key           | Value              |
|---------------|--------------------|
| Authorization | `Bearer {{access_token}}` |
| Content-Type  | `application/json` |

- **Body** → raw → JSON :
```json
{ "payload": "mon-evenement" }
```

Réponse attendue (201) :
```json
{
  "id": 1,
  "payload": "mon-evenement",
  "createdAt": "2024-05-01T10:30:00.000Z",
  "owner": "user1"
}
```

---

## 4. Connexion WebSocket

Dans Postman : **New → WebSocket**, puis saisir l'URL :

```
ws://localhost:8080/ws/events?access_token={{token}}
```

Cliquer **Connect** — les événements publiés via POST apparaissent en temps réel.

Pour rejouer les événements manqués depuis un ID donné :
```
ws://localhost:8080/ws/events?access_token={{token}}&lastId=5
```

---

## Codes d'erreur

| Code | Cause                        |
|------|------------------------------|
| 401  | Token absent ou invalide     |
| 400  | Payload vide ou null         |
| 500  | Erreur interne               |
