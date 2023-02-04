# -*- coding: utf-8 -*-
import re

from unittest.mock import patch, Mock

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common import backpressure
from intranet.yandex_directory.src.yandex_directory.core.models import LastProcessedRevisionModel
from intranet.yandex_directory.src.yandex_directory.core.models.organization import OrganizationBillingInfoModel

from testutils import (
    TestCase,
    create_organization,
)

from hamcrest import (
    has_item,
    assert_that,
    has_items,
    equal_to,
)


class TestGolovanStatsBehaviour(TestCase):
    def setUp(self):
        # нужно из-за запуска просчета метрик в отдельных потоках
        # где не видно изменений в базе из-за запуска тестов в транзакции с её откатом в конце
        self.patchers.append(
            patch.object(LastProcessedRevisionModel, 'get_not_processed_count', return_value=1)
        )
        super(TestGolovanStatsBehaviour, self).setUp()

    def test_available_all_metric_names_match_regexp(self):
        regexp = r'^[a-zA-Z0-9_\-./@]{1,128}_([ad][vehmntx]{3}|summ|hgram|max)$'
        data = app.stats_aggregator.get_data()
        for metric in data:
            msg = 'Неправильное имя метрики для голована: "%s"' % metric[0]
            self.assertTrue(re.match(regexp, metric[0]), msg=msg)

    def test_build_closed_service_stats(self):
        mocked_backpressure = Mock()
        experiments = [
            {'is_need_to_close_service': True, 'exp_value': 1},
            {'is_need_to_close_service': False, 'exp_value': 0},
        ]
        with patch('intranet.yandex_directory.src.yandex_directory.core.monitoring.stats.backpressure', mocked_backpressure):
            for exp in experiments:
                mocked_backpressure.is_need_to_close_service.return_value = exp['is_need_to_close_service']
                mocked_backpressure.last_smoke_test_has_errors.return_value = exp['is_need_to_close_service']
                data = app.stats_aggregator.get_data()
                exp_items = [
                    ['closed_backends_summ', exp['exp_value']],
                ]
                assert_that(data, has_items(*exp_items))
