package com.roadalert.cameroun.util

object ServiceActions {

    // ── Actions Service → UI ──────────────────────────────────

    // Accident détecté — démarrer CountdownActivity
    const val ACTION_ACCIDENT_DETECTED =
        "com.roadalert.cameroun.ACTION_ACCIDENT_DETECTED"

    // Countdown terminé — accident confirmé
    const val ACTION_ACCIDENT_CONFIRMED =
        "com.roadalert.cameroun.ACTION_ACCIDENT_CONFIRMED"

    // Countdown annulé par l'utilisateur
    const val ACTION_ACCIDENT_CANCELLED =
        "com.roadalert.cameroun.ACTION_ACCIDENT_CANCELLED"

    // État du service changé
    const val ACTION_SERVICE_STATE_CHANGED =
        "com.roadalert.cameroun.ACTION_SERVICE_STATE_CHANGED"

    // ── Actions UI → Service ──────────────────────────────────

    // Démarrer le service
    const val ACTION_START_SERVICE =
        "com.roadalert.cameroun.ACTION_START_SERVICE"

    // Arrêter le service
    const val ACTION_STOP_SERVICE =
        "com.roadalert.cameroun.ACTION_STOP_SERVICE"

    // Annuler le countdown depuis CountdownActivity
    const val ACTION_CANCEL_COUNTDOWN =
        "com.roadalert.cameroun.ACTION_CANCEL_COUNTDOWN"

    // ── Extras ────────────────────────────────────────────────

    // Confiance de la détection (0.0 à 1.0)
    const val EXTRA_CONFIDENCE =
        "com.roadalert.cameroun.EXTRA_CONFIDENCE"

    // État du service (true = actif)
    const val EXTRA_SERVICE_RUNNING =
        "com.roadalert.cameroun.EXTRA_SERVICE_RUNNING"

    // Mode de détection
    const val EXTRA_DETECTION_MODE =
        "com.roadalert.cameroun.EXTRA_DETECTION_MODE"
}