import itertools
import random
from unittest import mock

import freezegun
import pytest
from constance import config
from constance.test import override_config
from django.conf import settings
from django.utils import timezone

from idm.core.models import System
from idm.core.tasks.roles_export import ExportRoles, UPLOAD_ROLES_URL_PATH, MANAGE_SLUG_URL_PATH
from idm.tests.utils import create_user, make_role, run_commit_hooks, create_system, random_slug, raw_make_role
from idm.users.constants.user import USER_TYPES
from idm.utils import events
from idm.utils.mongo import get_mongo_db

pytestmark = pytest.mark.django_db


@pytest.fixture
def drop_events():
    db = get_mongo_db()
    db[settings.MONGO_EVENTS_COLLECTION].drop()


@pytest.fixture
def simple_system(simple_system):
    simple_system.tvm_id = '123'
    simple_system.save(update_fields=['tvm_id'])
    return simple_system


class FakeResponse:
    def __init__(self, status_code: int = 200):
        self.status_code = status_code
        self.content = b''


def generate_export_event(system_id: int):
    return {
        '_id': random_slug(),
        'system_id': system_id,
        'event': events.EventType.YT_EXPORT_REQUIRED,
    }


def test_export_roles(mongo_mock):
    systems = [
        create_system(tvm_id=str(random.randint(1, 10**8)), tvm_tirole_ids=[random.randint(1, 10**8) for _ in range(2)]),
        create_system(tvm_id=str(random.randint(1, 10**8))),
        create_system(tvm_tirole_ids=[random.randint(1, 10**8) for _ in range(2)]),
        create_system(),
        create_system(
            tvm_id=str(random.randint(1, 10**8)),
            tvm_tirole_ids=[random.randint(1, 10**8) for _ in range(2)],
            is_broken=True,
        ),
        create_system(
            tvm_id=str(random.randint(1, 10**8)), tvm_tirole_ids=[random.randint(1, 10**8) for _ in range(2)],
            is_active=False,
        ),
    ]
    applicable_systems = systems[:3]
    roles_by_systems = {
        system: raw_make_role(
            create_user(uid=random.randint(1, 10**8), type=random.choice(USER_TYPES.ALL_TYPES)),
            system,
            system.nodes.last().data,
            random.randint(0, 1) and {random_slug(): random_slug()} or None
        )
        for system in systems
        if system.is_operational()
    }

    fake_time = timezone.now()
    export_events = [generate_export_event(system.id) for system in systems]

    def get_events_by_system(_: str, system_id: int = None):
        return [event for event in export_events if not system_id or event['system_id'] == system_id]

    with mock.patch.object(ExportRoles, '_post_data', return_value=FakeResponse()) as post_data_mock, \
            mock.patch('idm.utils.events.get_events', wraps=get_events_by_system) as get_event_mock, \
            mock.patch('idm.utils.events.remove_events') as remove_events_mock,  \
            freezegun.freeze_time(fake_time):
        ExportRoles()

    def get_role_blob(system: System) -> dict:
        blob = {
            'revision': int(fake_time.timestamp()),
            'born_date': int(fake_time.timestamp()),
        }
        role = roles_by_systems[system]
        if role.user.type == USER_TYPES.TVM_APP:
            blob['tvm'] = {role.user.username: {role.node.slug_path: [role.fields_data or {}]}}
        elif role.user.type == USER_TYPES.USER:
            blob['user'] = {str(role.user.uid): {role.node.slug_path: [role.fields_data or {}]}}

        return blob

    roles_post_calls = [
        mock.call(path=UPLOAD_ROLES_URL_PATH, data={'system_slug': system.slug, 'roles': get_role_blob(system)})
        for system in applicable_systems
    ]
    post_data_mock.assert_has_calls(roles_post_calls, any_order=True)

    tvm_ids_post_calls = [
        mock.call(
            path=MANAGE_SLUG_URL_PATH,
            data={
                'system_slug': system.slug,
                'tvmid': tuple({
                    int(tvm_id)
                    for tvm_id in itertools.chain([system.tvm_id], system.tvm_tirole_ids or [])
                    if tvm_id
                }),
            }
        )
        for system in applicable_systems
    ]
    post_data_mock.assert_has_calls(tvm_ids_post_calls, any_order=True)
    assert post_data_mock.call_count == len(roles_post_calls) + len(tvm_ids_post_calls)

    get_events_calls = [mock.call(events.EventType.YT_EXPORT_REQUIRED)] + \
       [mock.call(events.EventType.YT_EXPORT_REQUIRED, system.id) for system in applicable_systems]
    get_event_mock.assert_has_calls(get_events_calls, any_order=True)
    assert get_event_mock.call_count == len(get_events_calls)

    remove_events_calls = [
        mock.call([event['_id'] for event in export_events if event['system_id'] == system.id])
        for system in applicable_systems
    ]
    remove_events_mock.assert_has_calls(remove_events_calls, any_order=True)
    assert remove_events_mock.call_count == len(remove_events_calls)


def test_export_roles__roles_post_unsuccessful(mongo_mock):
    system = create_system(tvm_id=str(random.randint(1, 10**8)))
    user_role, tvm_role = [
        raw_make_role(
            create_user(uid=random.randint(1, 10**8), type=user_type),
            system,
            system.nodes.last().data,
        ) for user_type in USER_TYPES.ALL_TYPES
    ]

    fake_time = timezone.now()
    export_events = [generate_export_event(system.id)]
    with mock.patch.object(ExportRoles, '_post_data', return_value=FakeResponse(400)) as post_data_mock, \
            mock.patch('idm.utils.events.get_events', return_value=export_events) as get_event_mock, \
            mock.patch('idm.utils.events.remove_events') as remove_events_mock, \
            freezegun.freeze_time(fake_time):
        assert ExportRoles._send_roles(system.slug) is False

    post_data_mock.assert_called_once_with(
        path=UPLOAD_ROLES_URL_PATH,
        data={
            'system_slug': system.slug,
            'roles': {
                'revision': int(fake_time.timestamp()),
                'born_date': int(fake_time.timestamp()),
                'tvm': {tvm_role.user.username: {tvm_role.node.slug_path: [tvm_role.fields_data or {}]}},
                'user': {str(user_role.user.uid): {user_role.node.slug_path: [user_role.fields_data or {}]}},
            }
        },
    )
    get_event_mock.assert_called_once_with(events.EventType.YT_EXPORT_REQUIRED, system.id)
    remove_events_mock.assert_not_called()
