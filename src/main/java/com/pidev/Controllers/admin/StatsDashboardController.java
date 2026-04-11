package com.pidev.Controllers.admin;

import com.pidev.Services.ServiceHackathon;
import com.pidev.Services.ServiceSponsor;
import com.pidev.Services.ServiceSponsorHackathon;
import com.pidev.models.Hackathon;
import com.pidev.models.Sponsor;
import com.pidev.models.SponsorHackathon;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class StatsDashboardController implements Initializable {

    @FXML private Label totalHackathonsLabel, totalSponsorsLabel, totalAssignmentsLabel;
    @FXML private PieChart themePieChart;
    @FXML private BarChart<String, Number> sponsorBarChart;
    @FXML private LineChart<String, Number> activityLineChart;

    private ServiceHackathon serviceH = new ServiceHackathon();
    private ServiceSponsor serviceS = new ServiceSponsor();
    private ServiceSponsorHackathon serviceSH = new ServiceSponsorHackathon();

    /**
     * Initialise le tableau de bord des statistiques en chargeant tous les graphiques et compteurs.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadSummaryStats();
        loadThemePieChart();
        loadSponsorBarChart();
        loadActivityLineChart();
    }

    /**
     * Charge les compteurs globaux (Total Hackathons, Sponsors et Affectations).
     */
    private void loadSummaryStats() {
        List<Hackathon> hackathons = serviceH.getAll();
        List<Sponsor> sponsors = serviceS.getAll();
        List<SponsorHackathon> relations = serviceSH.getAll();

        totalHackathonsLabel.setText(String.valueOf(hackathons.size()));
        totalSponsorsLabel.setText(String.valueOf(sponsors.size()));
        totalAssignmentsLabel.setText(String.valueOf(relations.size()));
    }

    /**
     * Génère un diagramme circulaire (Pie Chart) montrant la répartition des hackathons par thème.
     */
    private void loadThemePieChart() {
        Map<String, Long> themeCounts = serviceH.getAll().stream()
                .collect(Collectors.groupingBy(Hackathon::getTheme, Collectors.counting()));

        themePieChart.getData().clear();
        themeCounts.forEach((theme, count) -> {
            themePieChart.getData().add(new PieChart.Data(theme, count));
        });
    }

    /**
     * Génère un diagramme en barres (Bar Chart) montrant le nombre de sponsors par hackathon.
     */
    private void loadSponsorBarChart() {
        List<SponsorHackathon> relations = serviceSH.getAll();
        Map<String, Long> sponsorsPerHackathon = relations.stream()
                .collect(Collectors.groupingBy(sh -> sh.getHackathon().getTitle(), Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre de Sponsors");

        sponsorsPerHackathon.forEach((title, count) -> {
            series.getData().add(new XYChart.Data<>(title, count));
        });

        sponsorBarChart.getData().clear();
        sponsorBarChart.getData().add(series);
    }

    /**
     * Génère un graphique linéaire (Line Chart) montrant l'activité des lancements de hackathons par date.
     */
    private void loadActivityLineChart() {
        List<Hackathon> hackathons = serviceH.getAll();
        
        // Grouper les hackathons par date de début (formatée en Mois Jour)
        Map<String, Long> activityByDate = hackathons.stream()
                .sorted(Comparator.comparing(Hackathon::getStartAt))
                .collect(Collectors.groupingBy(
                        h -> h.getStartAt().format(DateTimeFormatter.ofPattern("MMM dd")),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Hackathons Débutant");

        activityByDate.forEach((date, count) -> {
            series.getData().add(new XYChart.Data<>(date, count));
        });

        activityLineChart.getData().clear();
        activityLineChart.getData().add(series);
    }
}
