package com.pidev.utils;

import com.pidev.models.Calendar.DayCalendarEntry;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class ChallengeCalendarUtil {
    private static final String DAY_CARD_STYLE = "-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 18; -fx-background-radius: 18; -fx-padding: 12;";
    private static final String DAY_NUMBER_STYLE = "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #0f172a;";
    private static final String TODAY_BADGE_STYLE = "-fx-background-color: #3559e0; -fx-background-radius: 999; -fx-padding: 4 10 4 10; -fx-text-fill: white; -fx-font-size: 10; -fx-font-weight: bold;";
    private static final String DAY_ENTRY_STYLE = "-fx-background-radius: 10; -fx-padding: 6 8 6 8;";
    private static final DateTimeFormatter WEEK_RANGE_FORMATTER = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter WEEK_RANGE_END_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private ChallengeCalendarUtil() {
    }

    public static LocalDate renderWeek(GridPane calendarGrid,
                                       Label monthLabel,
                                       LocalDate weekStart,
                                       Function<LocalDate, List<DayCalendarEntry>> entriesProvider) {
        if (calendarGrid == null || monthLabel == null || weekStart == null || entriesProvider == null) {
            return weekStart;
        }

        LocalDate normalizedWeekStart = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        monthLabel.setText(formatWeekLabel(normalizedWeekStart));
        calendarGrid.getChildren().removeIf(node -> {
            Integer rowIndex = GridPane.getRowIndex(node);
            return (rowIndex == null ? 0 : rowIndex) > 0;
        });

        for (int column = 0; column < 7; column++) {
            LocalDate date = normalizedWeekStart.plusDays(column);
            VBox dayCell = createDayCell(date, entriesProvider.apply(date));
            calendarGrid.add(dayCell, column, 1);
        }

        return normalizedWeekStart;
    }

    private static String formatWeekLabel(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        if (weekStart.getMonth() == weekEnd.getMonth() && weekStart.getYear() == weekEnd.getYear()) {
            return weekStart.format(WEEK_RANGE_FORMATTER) + " - "
                    + weekEnd.format(DateTimeFormatter.ofPattern("d, yyyy", Locale.ENGLISH));
        }
        return weekStart.format(WEEK_RANGE_FORMATTER) + " - " + weekEnd.format(WEEK_RANGE_END_FORMATTER);
    }

    private static VBox createDayCell(LocalDate date, List<DayCalendarEntry> entries) {
        VBox dayBox = new VBox(8);
        dayBox.setPrefHeight(145);
        dayBox.setMaxWidth(Double.MAX_VALUE);
        dayBox.setStyle(DAY_CARD_STYLE);

        HBox header = new HBox(8);
        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.setStyle(DAY_NUMBER_STYLE);
        header.getChildren().add(dayNumber);

        if (LocalDate.now().equals(date)) {
            Label todayLabel = new Label("Today");
            todayLabel.setStyle(TODAY_BADGE_STYLE);
            header.getChildren().add(todayLabel);
        }

        VBox entriesBox = new VBox(6);
        if (entries != null) {
            for (DayCalendarEntry entry : entries) {
                entriesBox.getChildren().add(createEntryLabel(entry));
            }
        }

        dayBox.getChildren().addAll(header, entriesBox);
        GridPane.setHgrow(dayBox, Priority.ALWAYS);
        return dayBox;
    }

    private static VBox createEntryLabel(DayCalendarEntry entry) {
        String suffix = entry.count() > 1 ? " (" + entry.count() + ")" : "";
        VBox entryBox = new VBox(2);
        entryBox.setMaxWidth(Double.MAX_VALUE);
        entryBox.setStyle(DAY_ENTRY_STYLE + entry.status().badgeStyle());

        Label titleLabel = new Label(safeTitle(entry.title()) + suffix);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setWrapText(true);
        titleLabel.setTextOverrun(OverrunStyle.CLIP);
        titleLabel.setMinHeight(Region.USE_PREF_SIZE);
        titleLabel.setStyle("-fx-font-size: 11; " + entry.status().textStyle());
        entryBox.getChildren().add(titleLabel);

        if (shouldShowGroupName(entry)) {
            Label groupLabel = new Label("Group : " + entry.groupName());
            groupLabel.setMaxWidth(Double.MAX_VALUE);
            groupLabel.setWrapText(true);
            groupLabel.setTextOverrun(OverrunStyle.CLIP);
            groupLabel.setMinHeight(Region.USE_PREF_SIZE);
            groupLabel.setStyle("-fx-font-size: 10; " + entry.status().textStyle());
            entryBox.getChildren().add(groupLabel);
        }

        return entryBox;
    }

    private static String safeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Challenge";
        }
        return title.trim();
    }

    private static boolean shouldShowGroupName(DayCalendarEntry entry) {
        return (entry.status() == com.pidev.models.Calendar.CalendarStatus.IN_PROGRESS
                || entry.status() == com.pidev.models.Calendar.CalendarStatus.SUBMITTED
                || entry.status() == com.pidev.models.Calendar.CalendarStatus.EVALUATED)
                && entry.groupName() != null
                && !entry.groupName().isBlank();
    }
}
