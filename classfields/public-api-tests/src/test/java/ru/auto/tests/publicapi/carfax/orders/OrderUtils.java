package ru.auto.tests.publicapi.carfax.orders;

import org.apache.commons.lang3.ArrayUtils;
import ru.auto.tests.publicapi.carfax.RawReportUtils;

import java.util.Arrays;

public final class OrderUtils {
    public final static String[] IGNORED_PATHS = new String[]{"order.created", "order.id"};

    public static String[] ignoredPaths(String reportPath) {
        return ArrayUtils.addAll(
                IGNORED_PATHS,
                Arrays.stream(RawReportUtils.IGNORED_PATHS)
                        .map(p -> p.replaceAll("^report\\.", reportPath + "."))
                        .toArray(String[]::new));
    }
}
