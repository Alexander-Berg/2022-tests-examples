package ru.yandex.solomon.expression.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.runners.Parameterized;

import ru.yandex.solomon.expression.version.SelVersion;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public abstract class VersionedSelTestBase {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> versions() {
        return Arrays.stream(SelVersion.values()).map(ver -> new Object[]{ver}).collect(Collectors.toList());
    }

    protected final SelVersion version;

    // Force descendants to have constructor accepting version
    protected VersionedSelTestBase(SelVersion version) {
        this.version = version;
    }

}
