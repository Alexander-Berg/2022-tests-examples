import copy

from mock import mock, Mock
import pytest

from django.utils.encoding import force_text

from infra.cauth.server.common.models import Group, SkippedStaffInconsistencies, User
from infra.cauth.server.master.importers.staff.retrievers import fetch_staff
from infra.cauth.server.master.importers.staff.suites import StaffDatabaseSuite
from infra.cauth.server.master.importers.tasks import run_import

pytestmark = pytest.mark.django_db


@pytest.yield_fixture
def mock_registry():
    mocked_sg_obj = Mock()
    mocked_sg_obj.name = 'staff'
    mocked_sg_obj.suites = {'database': StaffDatabaseSuite}

    with mock.patch('infra.cauth.server.master.importers.registry.SuiteGroup.registry') as registry_mock:
        registry_mock.__getitem__ = Mock(return_value=mocked_sg_obj)
        yield mocked_sg_obj


@pytest.yield_fixture
def mock_staff_fetcher(mock_registry):
    mock_registry.fetcher = fetch_staff
    yield


def mock_fetch_staff(user_count, group_count, memberships_count):
    persons = [
        {
            'uid': uid,
            'first_name': 'First',
            'last_name': 'Last',
            'gid': 20000 + uid,
            'is_fired': False,
            'is_robot': False,
            'join_date': '2021-01-01',
            'login': 'login_{}'.format(uid),
            'name': 'User {}'.format(uid),
            'pub_keys': [],
            'shell': '/bin/bash',
        } for uid in range(1, user_count + 1)
    ]
    logins = [p['login'] for p in persons]
    members_count = memberships_count // group_count
    members = logins[:members_count]

    descendant_groups = [
        {
            'ancestors': [20001],
            'gid': 20001 + gid,
            'members': members,
            'name': 'dpt_{}'.format(gid),
            'parent_gid': 20001,
            'service_id': None,
            'staff_id': gid,
            'type': 'dpt',
        } for gid in range(1, group_count)
    ]

    result = {
        'persons': persons,
        'groups': [
            {
                'ancestors': [],
                'gid': 20001,
                'members': [],
                'name': 'dpt_yandex',
                'parent_gid': None,
                'service_id': None,
                'staff_id': 0,
                'type': 'dpt',
            },
        ] + descendant_groups
    }
    return result


def _check_asserts(client, result, group_count=0, inconsistencies=None, expected_result=None):
    inconsistencies = inconsistencies or []
    if expected_result:
        assert result._state == 'FAILURE'
        assert str(result._result) == expected_result
    else:
        assert result._state == 'SUCCESS'
    assert Group.query.count() == group_count
    assert (
        SkippedStaffInconsistencies.query
        .with_entities(SkippedStaffInconsistencies.error)
        .all()
    ) == [(inconsistency,) for inconsistency in inconsistencies]
    response = client.get('/monitorings/skipped-inconsistencies/')
    if inconsistencies:
        assert response.status_code == 500
        assert force_text(response.content) == ' | '.join(inconsistencies)
    else:
        assert response.status_code == 200
        assert response.content == b'ok'


def test_import_staff(mock_registry, client):
    # Импорт пустых групп
    mock_registry.fetcher.return_value = mock_fetch_staff(0, 2000, 0)
    result = run_import.apply(args=['staff'])
    assert result._state == 'SUCCESS'
    assert Group.query.count() == 2000

    # Лимиты
    mock_registry.fetcher.return_value = mock_fetch_staff(2000, 2000, 14000)
    result = run_import.apply(args=['staff'])
    assert result._state == 'SUCCESS'

    mock_registry.fetcher.return_value = mock_fetch_staff(6000, 2000, 36000)
    result = run_import.apply(args=['staff'])
    assert result._state == 'FAILURE'
    assert str(result._result) == 'add is off the limits (4000 > 3000)'

    mock_registry.fetcher.return_value = mock_fetch_staff(5000, 2000, 36000)
    result = run_import.apply(args=['staff'])
    assert result._state == 'FAILURE'
    assert str(result._result) == 'add is off the limits (22000 > 20000)'


def _make_group(group_id, parent_id=20001):
    return {
        'id': group_id,
        'type': 'department',
        'url': 'dpt_{}'.format(group_id),
        'department': {'id': 99999999},
        'ancestors': [],
        'parent': {
            'id': parent_id,
            'department': {'id': 99999999},
        },
    }


def _run_import_staff(profiles_fetched, groups_fetched):
    def _fetcher(entity, params, logger):
        # some transformations functions somewhere deep inside import task
        # modify arguments, so we have to copy them
        return {
            'groups': copy.deepcopy(groups_fetched),
            'persons': profiles_fetched,
        }[entity]
    with mock.patch('infra.cauth.server.master.importers.staff.retrievers._fetch_entity', _fetcher):
        return run_import.apply(args=['staff'])


@pytest.mark.skip()
def test_skip_profile_inconsistencies(mock_staff_fetcher, client):
    CONSISTENT_GROUP_ID = 20002
    INCONSISTENT_GROUP_ID = 20003

    def _create_groups_fetched(group_ids):
        return [{
            'id': 20001,
            'type': 'department',
            'url': 'yandex',
            'department': {'id': 99999999},
            'ancestors': [],
        }] + [
            _make_group(group_id)
            for group_id in group_ids
        ]

    def _create_profiles_fetched(invalid_profile_ids=None, valid_profile_ids=None):
        invalid_profile_ids = invalid_profile_ids or []
        valid_profile_ids = valid_profile_ids or []
        return [
            {
                'id': profile_id,
                'login': 'user_{}'.format(profile_id),
                'official': {
                    'is_dismissed': False,
                    'is_robot': True,
                    'join_at': '2022-01-01',
                },
                'name': {
                    'first': {'en': ''},
                    'last': {'en': ''},
                },
                'environment': {'shell': '/bin/bash'},
                'department_group': {
                    'id': group_id,
                    'department': {'id': 99999999}
                },
                'groups': [],
                'keys': [],
            }
            for profile_id, group_id in (
                [(id, INCONSISTENT_GROUP_ID) for id in invalid_profile_ids] +
                [(id, CONSISTENT_GROUP_ID) for id in valid_profile_ids]
            )
        ]

    consistent_groups_fetched = _create_groups_fetched([CONSISTENT_GROUP_ID])

    add_single_group_result = _run_import_staff(
        profiles_fetched=[],
        groups_fetched=consistent_groups_fetched,
    )
    _check_asserts(
        client,
        add_single_group_result,
        group_count=2,  # root and consistent groups
    )
    assert User.query.count() == 0

    inconsistent_profile_ids = [1, 2]

    run_into_limit_result = _run_import_staff(
        profiles_fetched=_create_profiles_fetched(inconsistent_profile_ids, [3, 4]),
        groups_fetched=consistent_groups_fetched,
    )
    _check_asserts(
        client,
        run_into_limit_result,
        group_count=2,
        expected_result=(
            "Sanity checks failed during skipping staff inconsistencies: '{sanity_check}'. Inconsistencies are: {inconsistency_1}, {inconsistency_2}".format(
                sanity_check='Too much profiles to skip: 2/4 > 0.1;',  # 3, 4 are valid profiles, 4 is total count
                inconsistency_1='Group with id=20003 not found (referenced in <Person: 1, user_1>)',
                inconsistency_2='Group with id=20003 not found (referenced in <Person: 2, user_2>)',
            )
        ),
    )
    assert User.query.count() == 0

    consistent_profile_ids = [3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]
    all_profiles_fetched = _create_profiles_fetched(inconsistent_profile_ids, consistent_profile_ids)

    skip_invalids_result = _run_import_staff(
        profiles_fetched=all_profiles_fetched,
        groups_fetched=consistent_groups_fetched,
    )
    _check_asserts(
        client,
        skip_invalids_result,
        group_count=2,
        inconsistencies=[
            'Group with id=20003 not found (referenced in <Person: 1, user_1>)',
            'Group with id=20003 not found (referenced in <Person: 2, user_2>)',
        ],
    )
    assert User.query.count() == 18

    invalids_are_added_result = _run_import_staff(
        profiles_fetched=all_profiles_fetched,
        groups_fetched=_create_groups_fetched([CONSISTENT_GROUP_ID, INCONSISTENT_GROUP_ID]),
    )
    _check_asserts(
        client,
        invalids_are_added_result,
        group_count=3,
    )
    assert User.query.count() == 20


def test_skip_group_inconsistencies(mock_staff_fetcher, client):
    ROOT_GROUP_ID = 1

    def _run_import_staff_given_groups_parentships(groups_parents_ids):
        return _run_import_staff(
            profiles_fetched=[],
            groups_fetched=[{
                'id': 20000 + ROOT_GROUP_ID,
                'type': 'department',
                'url': 'yandex',
                'department': {'id': 99999999},
                'ancestors': [],
            }] + [
                _make_group(20000 + group_id, 20000 + parent_id)
                for group_id, parent_id in groups_parents_ids
            ]
        )

    sync_single_group_result = _run_import_staff_given_groups_parentships([
        (5, ROOT_GROUP_ID),
    ])
    _check_asserts(
        client,
        sync_single_group_result,
        group_count=2
    )

    LATE_PARENT_GROUP_ID = 2
    inconsistent_parentships = [
        (3, LATE_PARENT_GROUP_ID),
        (4, LATE_PARENT_GROUP_ID),
    ]

    run_into_limit_result = _run_import_staff_given_groups_parentships(
        [(5, ROOT_GROUP_ID), (6, ROOT_GROUP_ID)] +
        inconsistent_parentships
    )
    _check_asserts(
        client,
        run_into_limit_result,
        group_count=2,
        expected_result=(
            "Sanity checks failed during skipping staff inconsistencies: '{sanity_check}'. Inconsistencies are: {inconsistency_1}, {inconsistency_2}".format(
                sanity_check='Too much groups to skip: 2/5 > 0.1;',
                inconsistency_1='Group with id=20002 not found (referenced in <Group: 20003, department, dpt_20003>)',
                inconsistency_2='Group with id=20002 not found (referenced in <Group: 20004, department, dpt_20004>)',
            )
        ),
    )

    consistent_parentships = [
        (x, ROOT_GROUP_ID)
        for x in [5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21]
    ]

    invalids_are_skipped_result = _run_import_staff_given_groups_parentships(
        consistent_parentships +
        inconsistent_parentships
    )
    _check_asserts(
        client,
        invalids_are_skipped_result,
        group_count=18,
        inconsistencies=[
            'Group with id=20002 not found (referenced in <Group: 20003, department, dpt_20003>)',
            'Group with id=20002 not found (referenced in <Group: 20004, department, dpt_20004>)',
        ],
    )

    invalids_are_added_result = _run_import_staff_given_groups_parentships(
        consistent_parentships +
        inconsistent_parentships +
        [(LATE_PARENT_GROUP_ID, ROOT_GROUP_ID)]
    )
    _check_asserts(
        client,
        invalids_are_added_result,
        group_count=21,
    )


def test_skip_incorrect_staff_format(mock_staff_fetcher, client):
    fetch_incorrect_data_result = _run_import_staff(
        profiles_fetched=[],
        groups_fetched=[{
            'id': 20001,
            'type': 'department',
            'url': 'yandex',
            'ancestors': 'some_invalid_type',
        }],
    )
    expected_result = (
        "Sanity checks failed during skipping staff inconsistencies: "
        "'Too much groups to skip: 1/1 > 0.1;'. Inconsistencies are: "
        """yandex: {"ancestors": ["Invalid type."], "department": ["Missing data for required field."]}"""
    )

    _check_asserts(
        client, fetch_incorrect_data_result,
        expected_result=expected_result,
    )
