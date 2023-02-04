# -*- coding: utf-8 -*-
import itertools
import random
from typing import NamedTuple
from unittest import mock

import pytest
from django.test.utils import override_settings

from idm.core.models import ConductorGroup
from idm.sync.conductor import sync_groups
from idm.tests.utils import random_slug

pytestmark = pytest.mark.django_db


class _ConductorGroup(NamedTuple):
    external_id: int
    name: str


def generate_conductor_group(group_id: int = None, name: str = None) -> _ConductorGroup:
    return _ConductorGroup(external_id=group_id or random.randint(1, 10**6), name=name or random_slug())


@pytest.fixture
def get_conductor_groups_mock() -> mock.MagicMock:
    with mock.patch('idm.integration.conductor.get_conductor_groups') as _mock:
        yield _mock


def test_sync__fetch_error(get_conductor_groups_mock):
    assert ConductorGroup.objects.count() == 0
    get_conductor_groups_mock.side_effect = Exception
    with pytest.raises(Exception):
        sync_groups()
    
    get_conductor_groups_mock.assert_called_once()
    assert ConductorGroup.objects.count() == 0


def test_sync__add_remove_restore_rename_keep(get_conductor_groups_mock):
    keep_existing = [generate_conductor_group(), generate_conductor_group()]
    to_be_restored = [generate_conductor_group(), generate_conductor_group()]
    to_be_removed = [generate_conductor_group(), generate_conductor_group()]
    to_be_added = [generate_conductor_group()]
    to_be_renamed = [keep_existing[0], to_be_restored[0]]
    for group_data in itertools.chain(keep_existing, to_be_restored, to_be_removed):
        ConductorGroup.objects.create(
            external_id=group_data.external_id,
            name=group_data in to_be_renamed and random_slug() or group_data.name,
            is_active=group_data not in to_be_removed,
        )
    for group_data in to_be_renamed:
        group: ConductorGroup = ConductorGroup.objects.get(external_id=group_data.external_id)
        assert group.name != group_data.name

    fetched_groups = dict(itertools.chain(to_be_added, keep_existing, to_be_restored))
    get_conductor_groups_mock.return_value = fetched_groups
    sync_groups()

    get_conductor_groups_mock.assert_called_once()
    for group_data in to_be_added:
        group: ConductorGroup = ConductorGroup.objects.get(external_id=group_data.external_id)
        assert group.name == group_data.name
        assert group.is_active is True

    for group_data in itertools.chain(to_be_added, to_be_restored, keep_existing):
        group: ConductorGroup = ConductorGroup.objects.get(external_id=group_data.external_id)
        assert group.name == group_data.name
        assert group.is_active is True

    for group_data in to_be_removed:
        group: ConductorGroup = ConductorGroup.objects.get(external_id=group_data.external_id)
        assert group.name == group_data.name
        assert group.is_active is False

    for group_data in to_be_renamed:
        group: ConductorGroup = ConductorGroup.objects.get(external_id=group_data.external_id)
        assert group.name == group_data.name


# TODO: убрать xfail после IDM-7412: Сделать мониторинги на время последнего успешного завершения тасок
@pytest.mark.parametrize('force', [True, False])
def test_sync_remove_threshold_exceed(get_conductor_groups_mock, force: bool):
    to_be_removed = [generate_conductor_group(), generate_conductor_group()]
    for group_data in to_be_removed:
        ConductorGroup.objects.create(external_id=group_data.external_id, name=group_data.name)

    get_conductor_groups_mock.return_value = {}
    with override_settings(IDM_CONDUCTOR_REMOVED_GROUPS_THRESHOLD=1):
        if force:
            sync_groups(force=True)
            assert ConductorGroup.objects.active().count() == 0
            assert ConductorGroup.objects.inactive().count() == len(to_be_removed)
        else:
            pytest.xfail()
            with pytest.raises(RuntimeError):
                sync_groups(force=force)
            assert ConductorGroup.objects.active().count() == len(to_be_removed)
        get_conductor_groups_mock.assert_called_once()
