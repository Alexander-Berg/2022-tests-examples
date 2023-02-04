# -*- coding: utf-8 -*-
import itertools
import random
from typing import Tuple, Dict, List
from unittest import mock

import pytest
from django.test.utils import override_settings

from idm.core.models import NetworkMacro
from idm.sync.racktables import sync_macros
from idm.tests.utils import random_slug

# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


def generate_network_macro() -> Tuple['str', Dict[str, List[Dict[str, str]]]]:
    return random_slug(), {'owners': [{'type': random.choice(['user', 'service', 'department']), 'name': random_slug()}]}


@pytest.fixture
def get_macros_mock() -> mock.MagicMock:
    with mock.patch('idm.integration.racktables.get_macros') as _mock:
        yield _mock


def test_sync__fetch_error(get_macros_mock):
    assert NetworkMacro.objects.count() == 0
    get_macros_mock.side_effect = Exception
    with pytest.raises(Exception):
        sync_macros()
    get_macros_mock.assert_called_once()
    assert NetworkMacro.objects.count() == 0


def test_sync__add_remove_restore(get_macros_mock):
    to_remove = generate_network_macro()
    to_restore = generate_network_macro()
    to_add = generate_network_macro()
    for macro, _ in (to_remove, to_restore):
        NetworkMacro.objects.create(slug=macro, is_active=macro in to_restore)

    get_macros_mock.return_value = dict([to_restore, to_add])
    sync_macros()

    for macro, _ in (to_add, to_restore):
        assert NetworkMacro.objects.get(slug=macro).is_active is True

    assert NetworkMacro.objects.get(slug=to_remove[0]).is_active is False


# TODO: убрать xfail после IDM-7412: Сделать мониторинги на время последнего успешного завершения тасок
@pytest.mark.parametrize('force', [True, False])
def test_sync_remove_threshold_exceed(get_macros_mock, force: bool):
    to_be_removed = [generate_network_macro(), generate_network_macro()]
    for macro, _ in to_be_removed:
        NetworkMacro.objects.create(slug=macro)

    get_macros_mock.return_value = {}
    with override_settings(IDM_RACKTABLES_REMOVED_MACROS_THRESHOLD=1):
        if force:
            sync_macros(force=force)
            assert NetworkMacro.objects.active().count() == 0
            assert NetworkMacro.objects.inactive().count() == len(to_be_removed)
        else:
            pytest.xfail()
            with pytest.raises(RuntimeError):
                sync_macros(force=force)
            assert NetworkMacro.objects.active().count() == len(to_be_removed)
        get_macros_mock.assert_called_once()
