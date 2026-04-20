package com.sai.decisiongraveyard.util;

import com.sai.decisiongraveyard.model.Decision;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class DateUtils {

    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private DateUtils() {
    }

    public static String formatDateTime(long millis) {
        return DATE_TIME_FORMAT.format(new Date(millis));
    }

    public static String formatDate(long millis) {
        return DATE_FORMAT.format(new Date(millis));
    }

    public static String getCategoryDisplayName(String category) {
        if (category == null) return "PERSONAL";
        switch (category.toLowerCase()) {
            case "money":
                return "MONEY";
            case "health":
                return "HEALTH";
            case "study":
                return "STUDY";
            case "personal":
                return "PERSONAL";
            case "work":
                return "WORK";
            case "relationship":
                return "LOVE";
            default:
                return category.toUpperCase();
        }
    }

    public static int getHourOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public static long buildNineAmTimestamp(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static String buildDecisionTimeline(Decision decision) {
        return "Logged " + formatDateTime(decision.getDecisionTime())
                + " • Review " + formatDateTime(decision.getEvaluationTime());
    }

    public static String buildDetailMeta(Decision decision) {
        return "Category: " + getCategoryDisplayName(decision.getCategory())
                + "\nLogged: " + formatDateTime(decision.getDecisionTime())
                + "\nReview date: " + formatDateTime(decision.getEvaluationTime())
                + "\nDecision hour: " + getHourLabel(decision.getDecisionHour());
    }

    public static String getHourLabel(int hourOfDay) {
        if (hourOfDay >= 23 || hourOfDay < 5) {
            return "Late night";
        }
        if (hourOfDay < 12) {
            return "Morning";
        }
        if (hourOfDay < 17) {
            return "Afternoon";
        }
        return "Evening";
    }

    public static String toCsvValue(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    public static String formatDetailedCountdown(long remainingMillis) {
        if (remainingMillis <= 0) {
            return "0s";
        }

        long days = TimeUnit.MILLISECONDS.toDays(remainingMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(remainingMillis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m");
        }
        if (days == 0 && hours == 0 && minutes == 0) {
            sb.append(seconds).append("s");
        }
        return sb.toString().trim();
    }

    public static String formatCountdown(long millisRemaining) {
        long seconds = millisRemaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d";
        } else if (hours > 0) {
            return hours + "h";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return "<1m";
        }
    }

    public static String getRelativeTime(long millis) {
        long diff = System.currentTimeMillis() - millis;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 30) {
            return formatDate(millis);
        } else if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " min" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "just now";
        }
    }
}
