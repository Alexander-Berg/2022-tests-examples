// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.domain.statistics;

import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ReportRowTestJava {
    /**
     * This method should be placed in Java file, because Kotlin does not allow generic types without parameters.
     */
    @Test
    public void put_throws_ifWrongTypeOfParameters() {
        ReportMetricsColumn column = ReportColumn.CLICKS;
        //noinspection unchecked: in this test method is invoked with wrong parameter type
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> row().put(column, 13.37));
    }

    private ReportRow row() {
        return new ReportRow(UUID.randomUUID());
    }
}
