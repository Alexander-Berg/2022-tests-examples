package ru.auto.ara.data.preferences;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;

import ru.auto.ara.RobolectricTest;

/**
 * Created by aleien on 13.01.17.
 */
public class DefaultPreferencesTest extends RobolectricTest {

    @Test
    public void setSessionTimestamp_correctTimestamp_savedTimestamp() {
        long currentDate = Calendar.getInstance().getTimeInMillis();
        DefaultPreferences.INSTANCE.setSessionTimestamp(currentDate);
        assertEquals(currentDate, DefaultPreferences.INSTANCE.getSessionTimestamp());
    }

    @Test
    public void setNewSessionTimestamp_correctTimestamp_newSavedTimestamp() {
        long currentDate = Calendar.getInstance().getTimeInMillis();
        DefaultPreferences.INSTANCE.setSessionTimestamp(currentDate + 10);
        DefaultPreferences.INSTANCE.setSessionTimestamp(currentDate);
        assertEquals(currentDate, DefaultPreferences.INSTANCE.getSessionTimestamp());
    }
}
