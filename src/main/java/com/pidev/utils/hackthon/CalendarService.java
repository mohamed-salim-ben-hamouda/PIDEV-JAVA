package com.pidev.utils.hackthon;

import com.pidev.models.Hackathon;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;

public class CalendarService {

    /**
     * Ouvre le navigateur pour ajouter un hackathon au calendrier Google.
     * @param h Le hackathon à ajouter
     */
    public static void addToGoogleCalendar(Hackathon h) {
        try {
            // Format requis par Google: YYYYMMDDTHHMMSS
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
            String start = h.getStartAt().format(formatter);
            String end = h.getEndAt().format(formatter);
            
            String baseUrl = "https://www.google.com/calendar/render?action=TEMPLATE";
            
            // Construction de l'URL avec encodage des paramètres
            String url = baseUrl + 
                    "&text=" + URLEncoder.encode(h.getTitle(), "UTF-8") +
                    "&dates=" + start + "/" + end +
                    "&details=" + URLEncoder.encode(h.getDescription(), "UTF-8") +
                    "&location=" + URLEncoder.encode(h.getLocation(), "UTF-8");

            // Ouverture dans le navigateur par défaut
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture du Google Calendar : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
