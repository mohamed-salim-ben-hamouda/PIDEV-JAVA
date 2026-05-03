package com.pidev.models;

import java.util.EnumSet;

public final class Calendar {
    private Calendar() {
    }

    public enum CalendarStatus {
        PUBLISHED("#3559e0", "-fx-background-color: #dbeafe;", "-fx-text-fill: #1d4ed8;"),
        IN_PROGRESS("#f59e0b", "-fx-background-color: #fef3c7;", "-fx-text-fill: #b45309;"),
        SUBMITTED("#16a34a", "-fx-background-color: #dcfce7;", "-fx-text-fill: #15803d;"),
        EVALUATED("#06b6d4", "-fx-background-color: #cffafe;", "-fx-text-fill: #0e7490;"),
        NOT_EVALUATED("#ef4444", "-fx-background-color: #fee2e2;", "-fx-text-fill: #b91c1c;"),
        DEADLINE("#dc2626", "-fx-background-color: #fee2e2;", "-fx-text-fill: #b91c1c;");

        private final String color;
        private final String badgeStyle;
        private final String textStyle;

        CalendarStatus(String color, String badgeStyle, String textStyle) {
            this.color = color;
            this.badgeStyle = badgeStyle;
            this.textStyle = textStyle;
        }

        public String color() {
            return color;
        }

        public String badgeStyle() {
            return badgeStyle;
        }

        public String textStyle() {
            return textStyle;
        }
    }

    public record DayStatusCounts(
            int published,
            int inProgress,
            int submitted,
            int evaluated,
            int notEvaluated,
            EnumSet<CalendarStatus> visibleStatuses
    ) {
    }

    public record DayCalendarEntry(String title, String groupName, CalendarStatus status, int count) {
    }

    public static final class DayCalendarEntryAccumulator {
        private final String title;
        private final String groupName;
        private final CalendarStatus status;
        private int count;

        public DayCalendarEntryAccumulator(String title, String groupName, CalendarStatus status, int count) {
            this.title = title;
            this.groupName = groupName;
            this.status = status;
            this.count = count;
        }

        public String title() {
            return title;
        }

        public CalendarStatus status() {
            return status;
        }

        public String groupName() {
            return groupName;
        }

        public int count() {
            return count;
        }

        public void increment() {
            count++;
        }
    }
}
