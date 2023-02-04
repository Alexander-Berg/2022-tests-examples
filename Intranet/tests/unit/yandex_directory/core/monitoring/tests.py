# -*- coding: utf-8 -*-
from unittest.mock import patch

from testutils import (
    TestCase,
)

from hamcrest import (
    assert_that,
    equal_to,
    contains_inanyorder,
)
from intranet.yandex_directory.src.yandex_directory.core.monitoring import (
    common_metrics_data,
    get_common_metric,
)

from intranet.yandex_directory.src.yandex_directory.core.monitoring.buckets import Buckets


class TestGetCommonMetic(TestCase):
    def test_get_empty_common_metric(self):
        # если метрик нет отдадим пустой список
        with patch.dict(common_metrics_data, {}):
            assert_that(
                get_common_metric(),
                equal_to([])
            )

    def test_get_common_metric(self):
        # отдадим метрики из кэша в нужном формате
        result = {
            'func1': [
                [
                    "metric1",
                    1
                ]
            ],
            'func2': [
                [
                    "metric2",
                    2
                ]
            ]
        }
        with patch.dict(common_metrics_data, result):
            assert_that(
                get_common_metric(),
                contains_inanyorder(
                    ['metric1', 1],
                    ['metric2', 2]
                )
            )


class TestBuckets(TestCase):
    def test_buckets_counter(self):
        buckets = Buckets(0, 10, 20, 30)
        buckets.add(2)
        buckets.add(3)
        buckets.add(14)
        buckets.add(100)

        result = buckets.get()
        assert_that(
            result,
            equal_to(
                [
                    (0, 2),  # сюда попали 2 и 3
                    (10, 1), # сюда попала 14
                    (20, 0),
                    (30, 1), # а сюда 100
                ]
            )
        )
