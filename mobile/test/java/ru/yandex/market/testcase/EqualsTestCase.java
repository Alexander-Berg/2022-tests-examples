package ru.yandex.market.testcase;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public abstract class EqualsTestCase {

    private Collection<?> equalInstances;
    private Collection<?> notEqualInstances;

    @NonNull
    protected abstract Collection<?> getEqualInstances();

    @NonNull
    protected abstract Collection<?> getUnequalInstances();

    @Before
    public final void setUp() {
        equalInstances = getEqualInstances();
        assertThat("Expected at least 2 items in equal instances collection!",
                equalInstances.size(), greaterThan(1));
        notEqualInstances = getUnequalInstances();
        assertThat("Expected at least 2 items in unequal instances collection!",
                notEqualInstances.size(), greaterThan(1));
    }

    @Test
    public final void testEqualInstancesAreIndeedEqualToEachOther() {
        for (final Object firstObject : equalInstances) {
            for (final Object secondObject : equalInstances) {
                assertThat(firstObject, equalTo(secondObject));
            }
        }
    }

    @Test
    public final void testEqualInstancesHasSameHash() {
        final Iterator<?> iterator = equalInstances.iterator();
        final int hashCode = iterator.next().hashCode();
        while (iterator.hasNext()) {
            assertThat(iterator.next().hashCode(), equalTo(hashCode));
        }
    }

    @Test
    public final void testUnequalInstancesIndeedNotEqualToEachOther() {
        for (final Object firstObject : notEqualInstances) {
            for (final Object secondObject : notEqualInstances) {
                if (firstObject != secondObject) {
                    assertThat(firstObject, not(equalTo(secondObject)));
                }
            }
        }
    }
}
