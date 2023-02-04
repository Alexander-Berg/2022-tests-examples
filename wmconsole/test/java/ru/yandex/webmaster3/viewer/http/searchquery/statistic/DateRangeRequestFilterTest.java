package ru.yandex.webmaster3.viewer.http.searchquery.statistic;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.http.ActionResponse;
import ru.yandex.webmaster3.core.http.WebmasterErrorResponse;

/**
 * @author aherman
 */
@SuppressWarnings("Duplicates")
public class DateRangeRequestFilterTest {
    @Test
    public void testFutureDatesFrom() throws Exception {
        TestRequest request = new TestRequest();
        request.setDateFrom(DateTime.now().plusDays(1));
        request.setDateTo(DateTime.now().minusDays(1));

        DateRangeRequestFilter filter = new DateRangeRequestFilter();
        ActionResponse.ErrorResponse error = filter.beforeRequest(null, request);
        Assert.assertNotNull(error);
        Assert.assertTrue(error instanceof WebmasterErrorResponse.IllegalParameterValueResponse);
    }

    @Test
    public void testFutureDatesTo() throws Exception {
        TestRequest request = new TestRequest();
        DateTime now = DateTime.now();
        request.setDateFrom(now.minusDays(1));
        request.setDateTo(now.plusDays(1));

        DateRangeRequestFilter filter = new DateRangeRequestFilter();
        ActionResponse.ErrorResponse error = filter.beforeRequest(null, request);
        Assert.assertNotNull(error);
        Assert.assertTrue(error instanceof WebmasterErrorResponse.IllegalParameterValueResponse);
    }

    @Test
    public void testFutureDatesFromTo() throws Exception {
        TestRequest request = new TestRequest();
        DateTime now = DateTime.now();
        request.setDateFrom(now.plusDays(1));
        request.setDateTo(now.plusDays(1));

        DateRangeRequestFilter filter = new DateRangeRequestFilter();
        ActionResponse.ErrorResponse error = filter.beforeRequest(null, request);
        Assert.assertNotNull(error);
        Assert.assertTrue(error instanceof WebmasterErrorResponse.IllegalParameterValueResponse);
    }

    @Test
    public void testLongRange() throws Exception {
        TestRequest request = new TestRequest();
        DateTime now = DateTime.now();
        request.setDateFrom(now.minusMonths(DateRangeRequestFilter.MAXIMUM_RANGE_IN_MONTH + 2));
        request.setDateTo(now.minusDays(1));

        DateRangeRequestFilter filter = new DateRangeRequestFilter();
        ActionResponse.ErrorResponse error = filter.beforeRequest(null, request);
        Assert.assertNotNull(error);
        Assert.assertTrue(error instanceof WebmasterErrorResponse.IllegalParameterValueResponse);
    }

    @Test
    public void testNormalRange() throws Exception {
        TestRequest request = new TestRequest();
        DateTime now = DateTime.now();
        request.setDateFrom(now.minusMonths(DateRangeRequestFilter.MAXIMUM_RANGE_IN_MONTH));
        request.setDateTo(now.minusDays(1));

        DateRangeRequestFilter filter = new DateRangeRequestFilter();
        ActionResponse.ErrorResponse error = filter.beforeRequest(null, request);
        Assert.assertNull(error);
    }

    @Test
    public void testNormalRange1() throws Exception {
        TestRequest request = new TestRequest();
        DateTime now = DateTime.now();
        request.setDateFrom(now.minusDays(2));
        request.setDateTo(now.minusDays(1));

        DateRangeRequestFilter filter = new DateRangeRequestFilter();
        ActionResponse.ErrorResponse error = filter.beforeRequest(null, request);
        Assert.assertNull(error);
    }

    private static class TestRequest implements DateRangeAware {
        private DateTime dateFrom;
        private DateTime dateTo;

        @Override
        public void setDateFrom(DateTime dateFrom) {
            this.dateFrom = dateFrom;
        }

        @Override
        public DateTime getDateFrom() {
            return dateFrom;
        }

        @Override
        public void setDateTo(DateTime dateTo) {
            this.dateTo = dateTo;
        }

        @Override
        public DateTime getDateTo() {
            return dateTo;
        }
    }
}
