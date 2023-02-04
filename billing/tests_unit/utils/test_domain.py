# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest

from brest.core.exception import SnoutException

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.utils.domain import get_domain


@pytest.mark.smoke
class TestCaseGetDomain(TestCaseApiAppBase):

    @pytest.mark.parametrize(
        'hostname, domain_level, res_domain',
        [
            ('admin.balance.yandex.ru', 2, '.yandex.ru'),
            ('admin.balance.yandex.com', 2, '.yandex.com'),
            ('admin.balance.yandex.com.tr', 2, '.yandex.com.tr'),
            ('admin.balance.yandex.ru', 3, '.balance.yandex.ru'),
            ('admin.balance.yandex.com', 3, '.balance.yandex.com'),
            ('admin.balance.yandex.com.tr', 3, '.balance.yandex.com.tr'),
            ('admin.balance.yandex-team.ru', 2, '.yandex-team.ru'),
            ('admin.balance.yandex-team.com.tr', 2, '.yandex-team.com.tr'),
            ('balance.yandex.ru', 2, '.yandex.ru'),
            ('balance.yandex.com', 2, '.yandex.com'),
            ('balance.yandex.com.tr', 2, '.yandex.com.tr'),
            ('balance.yandex.ru', 3, '.balance.yandex.ru'),
            ('balance.yandex.com', 3, '.balance.yandex.com'),
            ('balance.yandex.com.tr', 3, '.balance.yandex.com.tr'),
            ('balance.yandex.com.tr', 1, '.yandex.com.tr'),  # минимум можно 2
        ],
    )
    def test_domain(self, hostname, domain_level, res_domain):
        assert get_domain(hostname, domain_level) == res_domain

    @pytest.mark.parametrize(
        'hostname',
        [
            'balance.yandex.aa',
            'balance.yandex.ru.aa',
            'balance.yandex.tru',
            'balance.paysys.badsiteru',
        ],
    )
    def test_unknown_domain(self, hostname):
        with pytest.raises(SnoutException) as exc_info:
            get_domain(hostname, 2)
        assert exc_info.value.description == 'Unknown first level domain.'
