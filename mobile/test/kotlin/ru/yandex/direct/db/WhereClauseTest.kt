// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhereClauseTest {
    @Test
    fun builder_where_worksInSimpleCase() {
        val where = WhereClause.builder()
                .where("A").isEqualTo("a")
                .build();
        assertThat(where.query).isEqualTo("A = ?")
        assertThat(where.arguments).isEqualTo(arrayOf("a"))
    }

    @Test
    fun builder_and_correctlyWorksWithTwoValues() {
        val where = WhereClause.builder()
                .where("A").isEqualTo("a")
                .and("B").isEqualTo("b")
                .build();
        assertThat(where.query).isEqualTo("(A = ?) AND (B = ?)")
        assertThat(where.arguments).isEqualTo(arrayOf("a", "b"))
    }

    @Test
    fun builder_and_correctlyWorksWithManyValues() {
        val where = WhereClause.builder()
                .where("A").isEqualTo("a")
                .and("B").isEqualTo("b")
                .and("C").isEqualTo("c")
                .build();
        assertThat(where.query).isEqualTo("((A = ?) AND (B = ?)) AND (C = ?)")
        assertThat(where.arguments).isEqualTo(arrayOf("a", "b", "c"))
    }

    @Test
    fun builder_shouldUseCorrectSyntaxForNull() {
        val where = WhereClause.builder()
                .where("N").isEqualTo(null)
                .build();
        assertThat(where.query).isEqualTo("N IS NULL")
        assertThat(where.arguments).isEqualTo(emptyArray<String>())
    }

    @Test
    fun builder_shouldUseCorrectArgumentsWithNulls() {
        val where = WhereClause.builder()
                .where("A").isEqualTo("a")
                .and("N").isEqualTo(null)
                .and("B").isEqualTo("b")
                .build();
        assertThat(where.query).isEqualTo("((A = ?) AND (N IS NULL)) AND (B = ?)")
        assertThat(where.arguments).isEqualTo(arrayOf("a", "b"))
    }
}