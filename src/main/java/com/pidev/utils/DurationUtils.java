package com.pidev.utils;

/**
 * Utilitaire de conversion de durée.
 * La durée est stockée en MINUTES dans la base de données.
 * Cette classe permet d'afficher et de saisir en jours / heures / minutes.
 */
public final class DurationUtils {

    private DurationUtils() {}

    /** Convertit une saisie jours/heures/minutes en minutes totales. */
    public static int toMinutes(int days, int hours, int minutes) {
        return days * 24 * 60 + hours * 60 + minutes;
    }

    /** Extrait le nombre de jours entiers d'une durée en minutes. */
    public static int toDays(int totalMinutes) {
        return totalMinutes / (24 * 60);
    }

    /** Extrait l'heure restante (0-23) d'une durée en minutes. */
    public static int toHours(int totalMinutes) {
        return (totalMinutes % (24 * 60)) / 60;
    }

    /** Extrait les minutes restantes (0-59) d'une durée en minutes. */
    public static int toMins(int totalMinutes) {
        return totalMinutes % 60;
    }

    /**
     * Formate une durée en minutes en une chaîne lisible.
     * Exemples :
     *   90    → "1h 30min"
     *   1500  → "1j 1h 0min"
     *   0     → "0min"
     */
    public static String format(int totalMinutes) {
        if (totalMinutes <= 0) return "0min";
        int d = toDays(totalMinutes);
        int h = toHours(totalMinutes);
        int m = toMins(totalMinutes);

        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("j ");
        if (h > 0 || d > 0) sb.append(h).append("h ");
        sb.append(m).append("min");
        return sb.toString().trim();
    }
}
