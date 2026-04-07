# RoadAlert Cameroun — Project Bible (Complete)

## 1. VISION PRODUIT
RoadAlert est un SYSTEME DE SURVIE mobile. Pas une simple app.
Il détecte automatiquement les accidents de route et alerte le réseau d'urgence complet en 30 secondes, SANS intervention humaine, même téléphone verrouillé, même si personne n'est autour, même si le téléphone est dans la poche de la victime.

### Pourquoi RoadAlert bat Apple et Google :
- Apple Crash Detection appelle le 911. Le 911 n'existe pas au Cameroun ni dans 80% du monde.
- Google Pixel détecte les accidents mais coûte 400,000 FCFA. RoadAlert fonctionne sur ITEL A56 à 25,000 FCFA.
- Ni Apple ni Google ne connaissent le groupe sanguin de la victime.
- Ni Apple ni Google n'alertent la famille avec une carte en direct.
- Ni Apple ni Google n'appellent automatiquement le SAMU local.
- Ni Apple ni Google n'ont d'alerte communautaire aux utilisateurs proches.
- Ni Apple ni Google n'ont de portail web temps réel.
- Ni Apple ni Google n'ont de reconnaissance vocale SOS mains libres.
- Ni Apple ni Google n'ont de NFC Emergency Tag.
- RoadAlert utilise 6 couches de détection vs 2 pour Apple/Google.

### Marché cible :
5 milliards de personnes vivent dans des pays sans système 911 unifié. RoadAlert est la solution universelle qui s'adapte aux numéros d'urgence locaux, aux langues locales, aux réalités locales. Potentiel : devenir l'app d'urgence par défaut d'Android dans les pays émergents.

## 2. EQUIPE ET CONTEXTE
- Yann : Kotlin Android natif + Firebase backend
- Collègue : Dart/Flutter — portail web + dashboard flotte
- Device test : ITEL A56 (Android 13 Go Edition, 2GB RAM, 3000mAh, écran 6.0" 720x1600)
- Professeur : Engr. Daniel MOUNE (moune.daniel@ictuniversity.edu.cm) — 50+ ans IT, expert industrie
- Cours : SE 3242 Android Application Development, ICT University Yaoundé
- Méthodologie : RUP + Agile (sprints itératifs)

### Philosophie du prof (GUIDE ABSOLU) :
"Le problème et la solution comptent. Pas le code. Pas l'IDE. Pas les outils. L'utilisateur final se fiche de combien de lignes de code tu as écrit. Il veut quelque chose qui MARCHE et qui est MEILLEUR. Pense innovation. Pense produit. Pense comme si des investisseurs allaient financer ton projet."

## 3. FEEDBACK PROFESSEUR DETAILLE (chaque point = action obligatoire)
Grade actuel : D. Chaque critique ci-dessous DOIT être résolue :

F1. TELEPHONE VERROUILLE : "Si quelqu'un a un accident et que je prends son téléphone verrouillé, je ne peux pas accéder à l'application. C'est un échec fondamental."
→ Solutions : full-screen intent countdown sur lock screen, notification persistante avec infos médicales, Quick Settings Tile SOS, Emergency SOS 5x power button, NFC Emergency Tag sur casque/coque scannable par un témoin avec SON téléphone.

F2. COORDONNEES GPS BRUTES : "Le SMS envoie des coordonnées que l'utilisateur ne comprend pas. Il voit des chiffres."
→ Solutions : Reverse geocoding (Geocoder Android + Nominatim fallback), lien Google Maps cliquable, point de référence local via Google Places API ("À 200m du Dispensaire de Messassi"), Google Plus.Code pour précision 3m.

F3. SAISIE MANUELLE NUMERO : "L'utilisateur ne devrait PAS taper son numéro de téléphone. L'application devrait le détecter."
→ Solution : TelephonyManager.getLine1Number() + SubscriptionManager pour dual SIM. Permission READ_PHONE_STATE. Fallback saisie manuelle UNIQUEMENT si API retourne null.

F4. SAISIE MANUELLE CONTACTS : "L'utilisateur ne devrait PAS taper les contacts. Il devrait choisir depuis son répertoire."
→ Solution : Intent(ACTION_PICK, ContactsContract.Contacts.CONTENT_URI) + ActivityResultLauncher. Nom + numéro importés en 1 tap. Relation via chips cliquables (Mère/Père/Conjoint/Ami/Médecin).

F5. NUMEROS URGENCE ET SMS : "Le 117, 119, 118 ne reçoivent PAS de SMS. Tes SMS vers ces numéros sont perdus."
→ Solution : APPEL vocal automatique avec message TTS complet. Intent ACTION_CALL + TextToSpeech Android. SMS uniquement pour contacts personnels.

F6. CONTACTS URGENCE CAMEROUN : "Il faut que les numéros d'urgence soient pré-intégrés."
→ Solution : 117 Police, 119 SAMU, 118 Pompiers pré-configurés, non-supprimables. Base de données hôpitaux proches via Firebase.

F7. DETECTION INSUFFISANTE : "Si le téléphone est dans la poche, sur le ventre, l'accéléromètre ne ressent presque rien. La victime saigne."
→ Solution : 6 couches de détection indépendantes avec scoring. GPS velocity, audio crash TFLite, reconnaissance vocale, Activity Recognition, inaction fallback.

F8. RECONNAISSANCE VOCALE : "Est-ce que la reconnaissance vocale peut aider à détecter un accident ?"
→ Solution : Oui — 3 modes : (A) hotword "SOS"/"aide" déclenche alerte, (B) annulation vocale du countdown, (C) détection de cris/détresse post-choc.

F9. BACKGROUND SERVICE : "WhatsApp et YouTube n'ont pas besoin d'être ouverts pour envoyer des notifications. L'app doit tourner en background invisible."
→ Solution : ForegroundService + Watchdog 15min + BOOT_COMPLETED. L'utilisateur oublie l'app après le setup.

F10. PORTAIL WEB : "Il faut fournir un portail web."
→ Solution : roadalert.cm/e/[id] — carte en direct, profil médical, timeline, photos, bouton "J'arrive", vue hôpital, vue flotte.

F11. ZONE ISOLEE : "Si l'accident arrive là où il n'y a personne autour, comment on aide ?"
→ Solution : Alerte communautaire 5km + appels automatiques urgences + portail web = les secours arrivent même sans témoin physique.

F12. UX MEDIOCRE : "L'expérience utilisateur est archi nulle. Observez les grandes applications."
→ Solution : Onboarding 55 secondes, 8 taps, zero clavier. Notification intelligente contextuelle. UI professionnelle Material Design 3.

F13. MONETISATION : "Je veux un produit qui génère vraiment de l'argent."
→ Solution : B2C freemium + B2B flottes + B2B assurances + B2B hôpitaux.

## 4. ARCHITECTURE TECHNIQUE
- Package : com.roadalert.cameroun
- Language : Kotlin (Android natif)
- Architecture : MVVM strict + Repository Pattern (Google Jetpack recommandé)
- UI existante : XML + ViewBinding (Sprints 1-5B — ne pas toucher, ça fonctionne)
- UI nouveaux écrans : Jetpack Compose là où pertinent (Sprint 6+)
- Base locale : Room (User, EmergencyContact, AccidentEvent, Hospital)
- Préférences : AppSettings (SharedPreferences) pour sensibilité, countdown, langue, son, vibration, heartbeat
- Cloud : Firebase (Realtime Database + Cloud Messaging + Hosting + Storage)
- Background : ForegroundService + ServiceWatchdogWorker (WorkManager 15min) + BOOT_COMPLETED
- Bilingue : EN défaut (res/values/) + FR (res/values-fr/) via LocaleHelper + attachBaseContext
- Audio ML : TensorFlow Lite audio classification (modèle ~2MB)
- Voix : Android SpeechRecognizer EXTRA_PREFER_OFFLINE (hotword detector basse conso)
- Géolocalisation : FusedLocationProvider + Geocoder + Google Places API + Plus.Codes
- SMS : SmsManager multi-part
- Appels : Intent ACTION_CALL + AudioManager speakerphone + TextToSpeech
- WhatsApp : Intent ACTION_SEND avec package com.whatsapp
- Photos : CameraX capture automatique 4 directions
- NFC : NdefMessage avec URI roadalert.cm/profile/[id]

## 5. SIX COUCHES DE DETECTION (architecture scoring)

### Couche 1 — Accéléromètre + Gyroscope (EXISTE DEJA)
- Magnitude G-force euclidienne sur 3 axes
- Seuils configurables : Prudent 29.4 (3G), Standard 24.5 (2.5G), Sportif 19.6 (2G)
- Gyroscope confirme orientation horizontale post-impact
- Immobilité 10s confirme que le véhicule ne bouge plus
- Score : 0-40 points
- Faiblesse : phone dans poche = choc amorti, peut manquer des accidents

### Couche 2 — GPS Velocity Crash (NOUVEAU)
- FusedLocation updates chaque seconde en mode IN_VEHICLE
- Détecte vitesse passant de 30+ km/h à 0 en moins de 3 secondes
- INDEPENDANT de la position du téléphone — que le phone soit dans poche, sac, tableau de bord, le GPS voit la même chose
- Score : 0-30 points
- Calcul : deltaV = previousSpeed - currentSpeed. Si deltaV > 30 km/h en <3s → score max
- Faiblesse : GPS peut avoir un délai de 1-2s, tunnel/parking = pas de signal

### Couche 3 — Audio Crash Detection (NOUVEAU)
- Microphone en mode écoute basse consommation
- TensorFlow Lite modèle entraîné sur sons de crash : impact métallique, verre brisé, freinage urgence, airbag, crissement pneus
- Dataset : ESC-50 (Environmental Sound Classification) + UrbanSound8K + sons crash spécifiques
- Modèle taille : ~2MB, inférence <50ms
- Score : 0-20 points
- Fonctionnement : buffer audio 3 secondes en continu, analyse par fenêtre glissante
- Faiblesse : musique forte peut masquer, micro couvert dans poche → atténué mais pas silencieux

### Couche 4 — Reconnaissance vocale SOS (NOUVEAU)
- Android SpeechRecognizer avec EXTRA_PREFER_OFFLINE = true
- Hotwords : "RoadAlert", "SOS", "aide", "help", "à l'aide", "au secours"
- Détection de cris et gémissements post-choc (classification audio TFLite)
- Score : 100 points = déclenchement IMMEDIAT (pas de scoring, alerte directe)
- Consommation : hotword detector ultra-basse conso (comme "OK Google")
- Commandes complètes quand activé :
  * "RoadAlert aide-moi" → alerte immédiate
  * "RoadAlert annuler" → arrête countdown
  * "RoadAlert appelle ma mère" → appel contact
  * "RoadAlert j'ai mal à [partie du corps]" → ajoute au profil urgence
  * "RoadAlert où suis-je ?" → TTS lit la position
  * "RoadAlert combien de temps ?" → TTS lit ETA contact "J'arrive"

### Couche 5 — Activity Recognition API (NOUVEAU)
- API Android native : DetectedActivity (IN_VEHICLE, ON_BICYCLE, ON_FOOT, STILL, RUNNING, TILTING)
- Détecte transition brutale IN_VEHICLE → STILL
- Score : 0-10 points bonus (confirme le contexte, ne déclenche pas seul)
- Aussi utilisé pour la gestion batterie adaptative

### Couche 6 — Inaction Fallback "Vous allez bien ?" (NOUVEAU)
- Se déclenche quand le score total est entre 25-49 (événement suspect mais pas certain)
- Séquence : vibration forte → écran s'allume → notification plein écran "VOUS ALLEZ BIEN ?" avec :
  * Gros bouton vert "JE VAIS BIEN" (annule)
  * Gros bouton rouge "J'AI BESOIN D'AIDE" (alerte immédiate)
  * Accepte aussi réponse vocale "je vais bien" ou "aide"
- Si aucune interaction en 45 secondes → alerte complète déclenchée
- C'est le DERNIER filet de sécurité — couvre les cas où toutes les autres couches sont faibles

### Logique de scoring :
- Score > 50 → countdown normal (15s configurable)
- Score > 80 → countdown raccourci (10s, haute confiance)
- Score = 100 (vocal SOS) → alerte immédiate, pas de countdown
- Score 25-49 → "Vous allez bien ?" avec timeout 45s
- Score < 25 → rien (activité normale)
- Contexte intelligent : si Activity Recognition = STILL depuis 2h + score < 50 → probablement faux positif, ignorer

## 6. CHAINE D'ALERTE COMPLETE (seconde par seconde)

T+0.0s : Impact détecté par une ou plusieurs couches, score > 50
T+0.5s : Écran s'allume MEME VERROUILLE (full-screen intent, FLAG_SHOW_WHEN_LOCKED, FLAG_TURN_SCREEN_ON)
T+0.5s : CountdownActivity affiche le countdown avec :
  - Gros chiffres centrés (15s par défaut)
  - Bouton ANNULER tactile large (48dp+)
  - Annulation vocale active ("RoadAlert annuler")
  - Son alarme (RingtoneManager.TYPE_ALARM, USAGE_ALARM contourne mode silencieux)
  - Vibration escalante (250ms les 10 premières secondes, triple 100ms les 5 dernières)
T+15.0s : Countdown expire. Personne n'a annulé = victime probablement inconsciente.
T+15.5s : GPS capture position précise (FusedLocation, timeout 5s, fallback dernière connue)
T+16.0s : Reverse geocoding → adresse humaine + point de référence local
T+16.5s : SMS intelligent envoyé aux 3 contacts personnels (multi-part si nécessaire)
T+17.0s : WhatsApp Intent envoyé aux contacts avec position en direct
T+18.0s : Firebase Realtime DB mise à jour → portail web actif à roadalert.cm/e/[id]
T+18.0s : FCM push notification aux contacts qui ont l'app
T+18.5s : Alerte communautaire : FCM topic "area_LAT_LON" → utilisateurs RoadAlert dans 5km reçoivent "Accident à 2.3km de vous"
T+20.0s : APPEL automatique 117 Police (Intent ACTION_CALL)
  - AudioManager.setSpeakerphoneOn(true)
  - Si réponse : message TTS complet (voir section 7)
  - Si pas de réponse après 15s sonnerie : raccrocher, passer à 119
T+25.0s : CameraX capture 4 photos (si caméra accessible) → upload Firebase Storage → portail web
T+30.0s : MediaRecorder démarre enregistrement audio ambiant 60 secondes → upload Firebase Storage
T+35.0s : APPEL automatique 119 SAMU (même format TTS + infos médicales + hôpital proche)
  - Si 117 n'avait pas répondu : 119 est le premier appel
T+40.0s : Flash LED clignote pattern SOS (... --- ...) en continu pour guider secours de nuit
T+40.0s : Haut-parleur joue son sirène toutes les 30s pendant 10 minutes (balise sonore)
T+180s : Appels terminés. Si 117+119 n'ont pas répondu → appel 118 Pompiers
T+180s : Retry cycle : re-tenter 117 après 60s, max 3 tentatives par numéro

Si batterie < 15% : mode survie → SMS seul + appel 117 seul → couper tout le reste
Si batterie < 5% : SMS seul → plus rien (économiser pour que le phone reste localisable)

## 7. MESSAGE TTS VOCAL EXACT (ce que l'opérateur 117/119 entend)

[BIP BIP BIP — 3 bips rapides]
"Alerte accident automatique RoadAlert."
[pause 1s]
"Victime : [Nom complet]."
"Groupe sanguin : [groupe]."
"Allergies : [liste ou Aucune]."
"Conditions médicales : [liste ou Aucune]."
[pause 1s]
"Localisation : [Quartier], [Ville]."
"Près de [point de référence le plus proche — ex: Dispensaire de Messassi]."
"Direction [indication — ex: sur la route vers Zoatoupsi]."
"Coordonnées : [lat] degrés nord, [lon] degrés est."
"Lien carte : roadalert point cm barre e barre [id]."
[pause 1s]
"Force de l'impact : [G] G."
"Évaluation : [probable traumatisme grave / impact modéré / impact léger]."
"Nombre de victimes détectées sur le site : [N]."
"La victime n'a pas répondu depuis [X] secondes."
[pause 3s]
"Répétition du message."
[LE MESSAGE SE REPETE UNE FOIS EN ENTIER]
[pause 2s]
"Fin du message automatique. La ligne reste ouverte pour écouter la scène."
[Micro du téléphone actif — l'opérateur écoute les bruits ambiants pendant 120 secondes]
[Si la victime est consciente, elle peut parler à l'opérateur via le haut-parleur]

Pour le SAMU (119), ajouter après "Évaluation" :
"Information médicale complémentaire : [médicaments réguliers si renseigné]."
"Hôpital le plus proche : [nom], à [distance] kilomètres."
"Deuxième hôpital : [nom], à [distance] kilomètres."

## 8. CONTACTS — ARCHITECTURE COMPLETE

### Type A : Contacts personnels (3 maximum)
- Source : répertoire natif Android via ContactPicker
- Import : Intent(ACTION_PICK, ContactsContract.Contacts.CONTENT_URI) → nom + numéro automatiques
- Relation : chips cliquables (Mère / Père / Conjoint(e) / Frère / Sœur / Ami(e) / Médecin / Autre)
- Stockage : Room table EmergencyContact (id, userId, name, phoneNumber, relation, priority, isActive)
- Priorité : Contact 1 = rouge, Contact 2 = gris, Contact 3 = gris (ordre d'alerte)
- Reçoivent : SMS + WhatsApp + Push notification + accès portail web
- Ne reçoivent PAS : appel automatique TTS (ça ferait paniquer une mère)
- Peuvent depuis le portail web : taper "J'arrive", appeler la victime, appeler le SAMU

### Type B : Contacts urgence nationaux (pré-installés, non-supprimables)
- 117 Police → APPEL automatique TTS. Pas de SMS (ne reçoit pas).
- 119 SAMU → APPEL automatique TTS avec infos médicales. Portail hôpital.
- 118 Pompiers → APPEL automatique TTS. Fallback si 117+119 ne répondent pas.
- Séquence appels : 117 d'abord (15s sonnerie) → 119 (15s) → 118 si aucun n'a répondu → retry 60s × 3

### Type C : Contacts urgence locaux (basés sur position GPS)
- Source : base de données hôpitaux Cameroun dans Firebase Realtime DB
- Champs : nom, adresse, coordonnées GPS, téléphone, type (hôpital/clinique/dispensaire), spécialités, horaires
- Cache local : Room table Hospital, synchronisé périodiquement quand internet disponible
- À chaque accident : les 3 hôpitaux les plus proches calculés par distance GPS
- Alertés via : portail web (notification push à l'interface hôpital)
- Le portail hôpital affiche : profil médical patient, G-force impact, ETA estimé, besoin sang

### Qui reçoit quoi (tableau définitif) :
Contact perso 1-3 : SMS ✓ | WhatsApp ✓ | Push ✓ | Portail ✓ | Appel ✗
Police 117 :        SMS ✗ | WhatsApp ✗ | Push ✗ | Portail ✗ | Appel TTS ✓
SAMU 119 :          SMS ✗ | WhatsApp ✗ | Push ✗ | Portail ✓ | Appel TTS ✓
Pompiers 118 :      SMS ✗ | WhatsApp ✗ | Push ✗ | Portail ✗ | Appel TTS ✓ (fallback)
3 hôpitaux proches : SMS ✗ | WhatsApp ✗ | Push ✗ | Portail ✓ | Appel ✗
Communauté 5km :    SMS ✗ | WhatsApp ✗ | Push ✓ | Portail ✗ | Appel ✗

## 9. LOCALISATION — 5 NIVEAUX DE PRECISION

Niveau 1 — GPS brut : Lat/Lon décimales. Précision 3-10m. Stocké en DB. JAMAIS montré à un humain.
Niveau 2 — Lien Maps : https://maps.google.com/?q=LAT,LON — cliquable sur tout smartphone.
Niveau 3 — Adresse : Android Geocoder.getFromLocation(lat,lon,1) → "Messassi, Yaoundé, Centre, Cameroun"
Niveau 4 — Point de référence : Google Places API Nearby Search (rayon 500m) → "À 200m du Dispensaire de Messassi, direction Zoatoupsi". Construction : distance + nom POI + direction cardinale.
Niveau 5 — Plus.Code : "6FW4+J2 Yaoundé" — carré 3×3m, fonctionne sans nom de rue, intégré Google Maps.

Fallbacks : si Geocoder échoue → Nominatim API OpenStreetMap (gratuit). Si Places API échoue → adresse Geocoder seule. Si GPS indisponible → dernière position connue + Cell ID (précision ~100-300m en ville) + mention "position approximative".

## 10. FORMAT SMS EXACT

SOS ACCIDENT RoadAlert
[Nom complet]
Sang: [groupe] | Allergie: [allergies]
Lieu: [Quartier], [Ville]
[Point de référence — ex: Près du Dispensaire de Messassi]
Carte: https://maps.google.com/?q=LAT,LON
Code: [Plus.Code] [Ville]
Impact: [G]G | [Heure HH:MM] | Aucune réponse
SAMU: 119 | Police: 117
Details: roadalert.cm/e/[id]

Multi-part SMS si >160 caractères (SmsManager divise automatiquement). Coût 2-3 SMS négligeable face à une vie.

## 11. FORMAT WHATSAPP EXACT

Intent ACTION_SEND, package "com.whatsapp", type "text/plain", EXTRA_TEXT :

🚨 ALERTE ACCIDENT — RoadAlert

Victime: [Nom complet]
Sang: [groupe] | Allergie: [allergies] | Conditions: [conditions]

📍 [Quartier], [Ville]
[Point de référence détaillé]

Impact détecté à [HH:MM] ([G]G)
Aucune réponse depuis [X] secondes

🗺️ Position en direct: roadalert.cm/e/[id]

Appelez SAMU: 119 | Police: 117

## 12. PORTAIL WEB (Flutter Web — rôle collègue)

URL structure :
- roadalert.cm/e/[id] → vue urgence (contacts/famille)
- roadalert.cm/hospital → dashboard hôpital
- roadalert.cm/fleet → dashboard flotte
- roadalert.cm/map → carte publique anonymisée
- roadalert.cm/profile/[id] → profil NFC (accessible par témoin)

### Vue urgence (contacts) — roadalert.cm/e/[id] :
- Bandeau rouge "ALERTE ACTIVE" avec nom + photo
- Carte Google Maps en direct (position mise à jour temps réel, se déplace si ambulance)
- Profil médical : groupe sanguin (gros, rouge), allergies, conditions, médicaments
- Timeline temps réel horodatée (chaque action : SMS envoyé, police appelée, etc.)
- Photos de la scène (4 directions, uploadées automatiquement)
- Audio ambiant (lecteur audio)
- Boutons d'action : "J'ARRIVE" (victime voit ETA), "APPELER SAMU", "APPELER VICTIME"
- Chat entre contacts alertés pour coordonner

### Vue hôpital — roadalert.cm/hospital :
- Dashboard alertes entrantes (carte + liste)
- Profil médical patient complet
- G-force impact → évaluation gravité probable
- ETA ambulance estimé
- Besoin sang : "[groupe] — VERIFIEZ STOCK"
- Bouton "Patient pris en charge" → met à jour portail famille

### Vue flotte — roadalert.cm/fleet :
- Carte tous véhicules temps réel
- Alerte instantanée si accident
- Historique accidents
- Statistiques conduite (vitesse moyenne, freinages brusques)
- Export rapport PDF pour assurance

### Vue carte publique — roadalert.cm/map :
- Carte anonymisée accidents temps réel (24 dernières heures)
- Points chauds par route (heatmap)
- Statistiques par zone/route
- Données ouvertes pour chercheurs et autorités

## 13. ACCES TELEPHONE VERROUILLE (6 méthodes)

M1. Détection automatique : le service tourne en background. L'alerte part SANS toucher le téléphone.
M2. Full-screen intent : countdown affiché SUR l'écran verrouillé. Flags : FLAG_SHOW_WHEN_LOCKED, FLAG_TURN_SCREEN_ON, FLAG_KEEP_SCREEN_ON. Permission USE_FULL_SCREEN_INTENT.
M3. Notification persistante : "URGENCE — Tapez pour infos médicales" visible sur lock screen. Expand = nom, sang, allergies, contacts.
M4. Quick Settings Tile SOS : TileService Android. Témoin glisse barre notifications → tape "SOS RoadAlert" → alerte manuelle sans déverrouiller.
M5. Emergency SOS 5x power : Android 12+ — 5 pressions rapides bouton power. RoadAlert enregistré comme app d'urgence via RoleManager.ROLE_EMERGENCY.
M6. NFC Emergency Tag : QR code / NFC tag sur casque ou coque. Témoin scanne avec SON téléphone → navigateur ouvre roadalert.cm/profile/[id] → profil médical + bouton "Envoyer alerte pour cette personne". Fonctionne même si téléphone victime est détruit.

## 14. ONBOARDING ZERO FRICTION (55 secondes, 8 taps, zero clavier)

Écran 1 (2s, 1 tap) : Langue — English / Français. Deux boutons. Bilingue par design.
Écran 2 (5s, 1 tap) : "RoadAlert protège votre vie sur la route. Nous configurons tout automatiquement." Bouton "Commencer".
Écran 3 (10s, 1 tap) : Permissions — UNE page avec explication visuelle pour chaque permission. Bouton "Autoriser tout". Permissions : LOCATION, PHONE_STATE, CALL_PHONE, SEND_SMS, READ_CONTACTS, RECORD_AUDIO, CAMERA, BODY_SENSORS, ACTIVITY_RECOGNITION.
Écran 4 (20s, 3 taps max) : Profil — Nom pré-rempli depuis compte Google (AccountManager). Numéro auto-détecté SIM (TelephonyManager). Groupe sanguin dropdown (A+, A-, B+, B-, AB+, AB-, O+, O-). Allergies chips cliquables (Pénicilline, Aspirine, Latex, Iode, Aucune, Autre). Conditions chips (Diabète, Asthme, Épilepsie, Hypertension, Aucune, Autre).
Écran 5 (15s, 2 taps) : Contacts — Bouton "Choisir depuis mes contacts" → ContactPicker natif → tap "Maman" → importé. Repeat pour 2ème contact. Relation chips (Mère/Père/Conjoint/Ami/Médecin).
Écran 6 (3s, 0 tap) : "Vous êtes protégé 24/7." Animation check vert. Service démarre. App se minimise. L'utilisateur ne la reverra peut-être jamais.

## 15. GESTION BATTERIE ADAPTATIVE (ITEL A56 — 3000mAh)

Activity Recognition drive le mode :
- STILL (assis, couché) : veille minimale — accéléromètre batch 5s, pas de GPS, pas de micro, pas de TFLite → ~2% batterie/jour
- ON_FOOT (marche) : mode piéton — accéléromètre normal, GPS passif (réseau), pas de micro actif → ~4%/jour
- ON_BICYCLE (vélo, moto) : mode actif — toutes couches sauf audio (vent trop fort) → ~6%/jour
- IN_VEHICLE (voiture, bus) : mode actif total — 6 couches complètes → ~8-10% pendant conduite active

Mode survie batterie :
- < 15% : couper audio TFLite + photos + enregistrement. Garder accéléromètre + GPS + vocal SOS
- < 5% pendant alerte : envoyer SMS seul + appel 117 seul → couper tout le reste
- < 5% hors alerte : notification "Batterie critique — protection limitée. Chargez votre téléphone."

## 16. VIE PRIVEE ET SECURITE

- Données en transit : HTTPS obligatoire (Firebase default)
- Données urgence sur Firebase : suppression automatique après 72 heures
- Lien portail web : expire après 72 heures, token unique non-devinable
- Pas de compte utilisateur centralisé : identification par numéro SIM uniquement
- Photos et audio : uploadés UNIQUEMENT en cas d'accident confirmé (jamais en fonctionnement normal)
- Position GPS : JAMAIS partagée en dehors d'une urgence active
- Profil médical : stocké localement Room + Firebase chiffré, accessible uniquement via lien urgence
- NFC tag : contient uniquement l'URL du profil, pas les données médicales elles-mêmes
- RGPD/données perso Cameroun : consentement explicite à l'onboarding

## 17. SCENARIOS EDGE CASES

S1. Phone dans poche (amorti) → GPS velocity + micro + inaction "Vous allez bien ?"
S2. Phone écran cassé → détection auto + annulation vocale + flash SOS + sirène
S3. Accident bus 40 passagers → mode conducteur pro + multi-victim detection (15 phones même lieu = "ACCIDENT MAJEUR")
S4. Accident nuit route non éclairée → flash LED SOS morse + sirène sonore 30s
S5. Victime coincée consciente → commandes vocales complètes mains libres
S6. Faux positif match foot → contexte : STILL + pas IN_VEHICLE + cris ≠ crash → score < 25 → rien
S7. Batterie 5% → mode survie : SMS + appel 117 uniquement
S8. Tunnel/parking souterrain → dernière position GPS + Cell ID + mention "approximative"
S9. Zone sans nom de rue → Plus.Code + point de référence local Google Places
S10. Changement téléphone → restauration profil Firebase via numéro SIM
S11. Roaming frontière → prioriser WhatsApp/push (gratuit), SMS en fallback
S12. Deux accidents même lieu 30min → alerte "risque carambolage"
S13. Utilisateur sourd → gros bouton visuel + vibration pattern spécifique, pas de son
S14. Utilisateur aveugle → commandes vocales = mode principal, TalkBack compatible
S15. 117 ne répond pas → 119 → 118 → retry 60s × 3
S16. Ligne occupée → passer au numéro suivant immédiatement
S17. Accident zone frontalière (roaming) → WhatsApp/push prioritaire, SMS fallback coûteux
S18. Grand-mère sans smartphone → SMS avec lien Maps (elle peut montrer à quelqu'un)
S19. Conducteur professionnel → mode pro : nombre passagers dans profil → "ACCIDENT BUS 40 PASSAGERS"
S20. Dashcam mode → si phone sur pare-brise : vidéo 2min avant + 1min après → upload → preuve assurance

## 18. BUSINESS MODEL

### B2C Freemium (particuliers) :
- Gratuit : détection + 1 contact + SMS + appels urgence
- Premium 1000 FCFA/mois : 5 contacts, WhatsApp, photos auto, position live, profil médical complet, NFC tag généré

### B2B Flottes (transport) :
- 50,000 FCFA/mois : dashboard web temps réel, alerte dispatching, historique, statistiques, rapport assurance PDF

### B2B Assurances :
- Commission par dossier : rapport automatique (heure, lieu, G-force, photos, vidéo dashcam). L'assureur traite en 24h vs 30 jours.

### B2B Hôpitaux :
- 25,000 FCFA/mois : dashboard alertes entrantes, profil médical patient, stock sang, ETA ambulance

## 19. INNOVATIONS UNIQUES (aucune app au monde ne fait ça)

I1. NFC Emergency Tag — profil médical accessible même téléphone détruit
I2. Commandes vocales complètes — assistant d'urgence mains libres (6 commandes)
I3. Multi-victim detection — 15 phones même lieu = "ACCIDENT MAJEUR"
I4. Flash SOS morse + sirène — balise de nuit
I5. Points de référence locaux — "À 200m du Dispensaire" au lieu de coordonnées
I6. Alerte hôpital avec besoin sang — préparation bloc AVANT arrivée patient
I7. Carte communautaire accidents — données ouvertes pour autorités
I8. "J'arrive" — contact rassure la victime en direct
I9. Dashcam mode — preuve vidéo pour assurance
I10. Protocole ouvert — potentiel standard africain des urgences mobiles
I11. Détection cris/détresse — filet de sécurité audio post-choc
I12. "Vous allez bien ?" — dernier filet par inaction (phone amorti dans poche)
I13. Description contextuelle — sans nom de rue, donne les repères que les locaux comprennent
I14. Mode conducteur pro — nombre passagers influe gravité de l'alerte
I15. Notification intelligente contextuelle — "En voiture — Protection renforcée" / "Immobile — Veille"

## 20. SPRINTS TERMINES
- Sprint 1 : Setup + Navigation + Layouts ✅
- Sprint 2 : ProfileSetupActivity + Room DB ✅
- Sprint 3 : SensorFusionEngine + Countdown (3 conditions accéléromètre) ✅
- Sprint 4 : GPS + SMS + AlertSentActivity ✅
- Sprint 5A : HistoryActivity ✅
- Sprint 5B : SettingsActivity + 3 bugs critiques + Watchdog + Son/Vibration (3475 lignes) ✅
- Language : Module multilingue EN/FR complet ✅

## 21. SPRINTS A VENIR
- Sprint 6A : UX zero friction (auto-detect SIM, contact picker natif, urgences pré-intégrées, onboarding redesign)
- Sprint 6B : SMS intelligent (reverse geocoding, lien Maps, Plus.Code, point de référence, profil médical)
- Sprint 6C : Appels automatiques urgence (ACTION_CALL 117/119/118, TTS complet, fallback, ligne ouverte 120s)
- Sprint 6D : Lock screen access (full-screen intent, Quick Settings Tile SOS, Emergency SOS 5x power, NFC tag)
- Sprint 6E : Détection multi-couches (GPS velocity, audio TFLite, vocal SOS, Activity Recognition, inaction)
- Sprint 6F : Firebase + portail web (Realtime DB, FCM push, portail Flutter Web, hosting)
- Sprint 6G : Alerte communautaire (utilisateurs proches 5km, multi-victim detection)
- Sprint 6H : Features avancées (dashcam, flash SOS, photos auto, audio ambiant, mode conducteur pro)
- Sprint 6I : Gestion batterie adaptative + notification contextuelle intelligente
- Sprint 6J : Polish + tests exhaustifs ITEL A56 + documentation + soumission

## 22. CONVENTIONS CODE
- TOUT texte UI dans strings.xml (EN défaut + FR traduction), JAMAIS hardcodé
- ViewBinding partout pour les écrans XML existants
- MVVM strict : Activity observe → ViewModel logique → Repository données
- Room pour stockage local, Firebase pour cloud
- Permissions demandées avec explication visuelle, jamais silencieusement
- Branches : feat/sprint-XX → PR → review collègue → merge → delete branch
- Commits : feat(), fix(), docs() format conventionnel
- Un build gate après chaque module (Rebuild Project, 0 erreurs)
- Fichiers Kotlin : commentaires en anglais, noms de variables/classes en anglais

## 23. FICHIERS CLES EXISTANTS
- detection/AccidentDetectionService.kt — ForegroundService permanent (à enrichir avec 6 couches)
- detection/SensorFusionEngine.kt — Algorithme 3 conditions (à étendre scoring multi-couches)
- alert/AlertManager.kt — Orchestre SMS + notifications (à enrichir : appels TTS, WhatsApp, Firebase)
- data/db/AppDatabase.kt — Room (User, EmergencyContact, AccidentEvent — à ajouter Hospital)
- util/AppSettings.kt — SharedPreferences (sensibilité, countdown, langue, son, vibration, heartbeat)
- util/LocaleHelper.kt — Gestion bilingue EN/FR
- data/worker/ServiceWatchdogWorker.kt — Relance service si tué par Android
- ui/settings/SettingsViewModel.kt — MVVM Settings (à enrichir section langue, contacts)
- RoadAlertApplication.kt — Application class (watchdog schedule, locale)
