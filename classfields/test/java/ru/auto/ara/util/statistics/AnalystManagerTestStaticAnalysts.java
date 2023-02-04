package ru.auto.ara.util.statistics;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

/**
 * @author aleien on 14.03.17.
 */
public class AnalystManagerTestStaticAnalysts {
    private MetricaAnalyst metricaAnalyst = Mockito.mock(MetricaAnalyst.class);

    @Before
    public void setUp() {
        AnalystManager.getInstance().register(metricaAnalyst);
    }

    @Test
    public void metricaStaticLog_string_shouldLog() throws Exception {
        AnalystManager.getInstance().logEvent("Event");
        Mockito.verify(metricaAnalyst).logEvent("Event", null);
    }

    @Test
    public void metricaStaticLog_statEvent_shouldLog() throws Exception {
        AnalystManager.log(StatEvent.EVENT_SEARCH_AUTO);
        Mockito.verify(metricaAnalyst).logEvent(StatEvent.EVENT_SEARCH_AUTO, null);
    }

    @Test
    public void metricaLog_string_shouldLog() throws Exception {
        AnalystManager.getInstance().logEvent("Event");
        Mockito.verify(metricaAnalyst).logEvent("Event", null);
    }

    @Test
    public void metricaLog_statEvent_shouldLog() throws Exception {
        AnalystManager.getInstance().logEvent(StatEvent.EVENT_SEARCH_AUTO);
        Mockito.verify(metricaAnalyst).logEvent(StatEvent.EVENT_SEARCH_AUTO, null);
    }

    @Test
    public void metricaLogEvent_withParams_shouldLog() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");
        AnalystManager.getInstance().logEvent("Event", params);
        Mockito.verify(metricaAnalyst).logEvent("Event", params);
    }

    @Test
    public void metricaLogEvent_noParams_shouldLog() throws Exception {
        AnalystManager.getInstance().logEvent("Event", null);
        Mockito.verify(metricaAnalyst).logEvent("Event", null);
    }

}
