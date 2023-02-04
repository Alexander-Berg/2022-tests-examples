package ru.yandex.webmaster3.core.turbo.model.feed;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.turbo.model.error.TurboRawError;
import ru.yandex.webmaster3.core.turbo.model.error.TurboSeverity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Oleg Bazdyrev on 12/10/2018.
 */
public class TurboFeedStatisticsTest {

    @Test
    public void testGetState() throws Exception {
        List<TurboRawError> rawErrors = new ArrayList<>();
        TurboFeedStatistics tfs = new TurboFeedStatistics(TurboFeedType.RSS, "http://some.feed/url", true,
                Collections.emptyList(), DateTime.now(), DateTime.now(),
                rawErrors, new TurboFeedItemStatistics(100, 0, 0, 100), null);

        Assert.assertEquals(TurboCrawlState.OK, tfs.getState());

        tfs = new TurboFeedStatistics(TurboFeedType.RSS, "http://some.feed/url", true,
                Collections.emptyList(), DateTime.now(), DateTime.now(),
                rawErrors, new TurboFeedItemStatistics(100, 100, 0, 0), null);

        Assert.assertEquals(TurboCrawlState.ERROR, tfs.getState());

        rawErrors = Arrays.asList(
            new TurboRawError("SomeUnknownError", 0, 0, null, TurboSeverity.ERROR, null)
        );
        tfs = new TurboFeedStatistics(TurboFeedType.RSS, "http://some.feed/url", true,
                Collections.emptyList(), DateTime.now(), DateTime.now(),
                rawErrors, new TurboFeedItemStatistics(100, 0, 0, 100), null);

        Assert.assertEquals(TurboCrawlState.OK, tfs.getState());

        rawErrors = Arrays.asList(
                new TurboRawError("Parser.Goal.UnknownType", 0, 0, null, TurboSeverity.WARNING, null)
        );
        tfs = new TurboFeedStatistics(TurboFeedType.RSS, "http://some.feed/url", true,
                Collections.emptyList(), DateTime.now(), DateTime.now(),
                rawErrors, new TurboFeedItemStatistics(100, 0, 0, 100), null);

        Assert.assertEquals(TurboCrawlState.WARNING, tfs.getState());

        rawErrors = Arrays.asList(
                new TurboRawError("Skip.Images", 0, 0, null, TurboSeverity.WARNING, null)
        );
        tfs = new TurboFeedStatistics(TurboFeedType.RSS, "http://some.feed/url", true,
                Collections.emptyList(), DateTime.now(), DateTime.now(),
                rawErrors, new TurboFeedItemStatistics(100, 0, 0, 100), null);

        Assert.assertEquals(TurboCrawlState.OK, tfs.getState());

        tfs = new TurboFeedStatistics(TurboFeedType.RSS, "http://some.feed/url", false,
                Collections.emptyList(), DateTime.now(), DateTime.now(),
                rawErrors, new TurboFeedItemStatistics(100, 0, 0, 100), null);

        Assert.assertEquals(TurboCrawlState.WARNING, tfs.getState());

        rawErrors = Arrays.asList(
                new TurboRawError("Skip.Images", 0, 0, null, TurboSeverity.WARNING, null),
                new TurboRawError("Skip.Images", 0, 0, null, TurboSeverity.WARNING, null),
                new TurboRawError("Skip.Images", 0, 0, null, TurboSeverity.WARNING, null),
                new TurboRawError("Skip.Images", 0, 0, null, TurboSeverity.WARNING, null),
                new TurboRawError("Skip.Images", 0, 0, null, TurboSeverity.WARNING, null),
                new TurboRawError("Skip.Images", 0, 0, null, TurboSeverity.WARNING, null)
        );
        tfs = new TurboFeedStatistics(TurboFeedType.RSS, "http://some.feed/url", true,
                Collections.emptyList(), DateTime.now(), DateTime.now(),
                rawErrors, new TurboFeedItemStatistics(100, 0, 0, 100), null);

        Assert.assertEquals(TurboCrawlState.WARNING, tfs.getState());

        rawErrors = Arrays.asList(
                new TurboRawError("Skip.Images", 0, 0, null, TurboSeverity.WARNING, null),
                new TurboRawError("Skip.Images", 0, 0, null, TurboSeverity.WARNING, null),
                new TurboRawError("Parser.Goal.UnknownType", 0, 0, null, TurboSeverity.WARNING, null),
                new TurboRawError("Skip.Images", 0, 0, null, TurboSeverity.WARNING, null)
        );
        tfs = new TurboFeedStatistics(TurboFeedType.RSS, "http://some.feed/url", true,
                Collections.emptyList(), DateTime.now(), DateTime.now(),
                rawErrors, new TurboFeedItemStatistics(100, 0, 0, 100), null);

        Assert.assertEquals(TurboCrawlState.WARNING, tfs.getState());
    }

}
