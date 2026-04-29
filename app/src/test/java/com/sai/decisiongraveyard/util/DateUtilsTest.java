package com.sai.decisiongraveyard.util;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Calendar;

public class DateUtilsTest {

    @Test
    public void getCategoryDisplayName_returnsCorrectValues() {
        assertEquals("MONEY", DateUtils.getCategoryDisplayName("money"));
        assertEquals("HEALTH", DateUtils.getCategoryDisplayName("health"));
        assertEquals("LOVE", DateUtils.getCategoryDisplayName("relationship"));
        assertEquals("PERSONAL", DateUtils.getCategoryDisplayName(null));
        assertEquals("UNKNOWN", DateUtils.getCategoryDisplayName("unknown"));
    }

    @Test
    public void getHourLabel_returnsCorrectLabels() {
        assertEquals("Morning", DateUtils.getHourLabel(9));
        assertEquals("Afternoon", DateUtils.getHourLabel(14));
        assertEquals("Evening", DateUtils.getHourLabel(19));
        assertEquals("Late night", DateUtils.getHourLabel(2));
        assertEquals("Late night", DateUtils.getHourLabel(23));
    }

    @Test
    public void formatCountdown_returnsFormattedStrings() {
        long oneDay = 24L * 60 * 60 * 1000;
        long oneHour = 60L * 60 * 1000;
        long oneMinute = 60L * 1000;

        assertEquals("1d", DateUtils.formatCountdown(oneDay));
        assertEquals("1h", DateUtils.formatCountdown(oneHour));
        assertEquals("1m", DateUtils.formatCountdown(oneMinute));
        assertEquals("<1m", DateUtils.formatCountdown(30 * 1000));
    }
}
