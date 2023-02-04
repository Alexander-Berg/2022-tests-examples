package ru.yandex.solomon.coremon.stockpile;

import ru.yandex.solomon.coremon.CoremonShardQuota;
import ru.yandex.solomon.coremon.meta.CoremonMetric;
import ru.yandex.solomon.coremon.meta.TestMetricsCollection;


/**
 * @author Sergey Polovko
 */
public class TestResolveHelper extends CoremonShardStockpileResolveHelper {

    public TestResolveHelper(CoremonMetric... metrics) {
        super(new TestMetricsCollection<>(metrics), CoremonShardQuota.DEFAULT);
    }
}
