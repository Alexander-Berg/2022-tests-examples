import pytest
from unittest.mock import patch

from django.urls import reverse

from ok.flows.executor import (
    get_department_chain,
    get_department_chain_of_heads,
    get_departments_chain_with_heads,
    get_duty_by_service_slug,
    Department,
)
from tests.factories import FlowFactory

pytestmark = pytest.mark.django_db

STAFF_API_MOCK = [
    Department(id=1, url='dep1', name={'en': 'dep_1'}, head='chief1', is_direction=False),
    Department(id=2, url='dep2', name={'en': 'dep_2'}, head='chief2', is_direction=False),
    Department(id=3, url='dep3', name={'en': 'dep_3'}, head='chief1', is_direction=False),
    Department(id=4, url='dep4', name={'en': 'dep_4'}, head='chief3', is_direction=False),
]


STAFF_API_PERSON_DEP_MOCK = {
    'department_group': {
        'ancestors': [
            {
                'department': {
                    'url': 'yandex',
                },
            },
            {
                'department': {
                    'url': 'yandex_mega_dep',
                },
            }
        ],
        'department': {
            'url': 'yandex_small_dep',
        },
    }
}


DUTY_DATA = [
    {'person': {'login': 'dmitrypro'},
      'start_datetime': '2021-05-23T20:25:00+03:00',
      'end_datetime': '2021-05-28T20:25:00+03:00'},
    {'person': {'login': 'zivot'},
      'start_datetime': '2021-05-25T00:00:00+03:00',
      'end_datetime': '2021-05-27T00:00:00+03:00'},
    {'person': None,
      'start_datetime': '2021-05-25T00:00:00+03:00',
      'end_datetime': '2021-05-27T00:00:00+03:00'},
    {'person': {'login': 'shigarus'},
      'start_datetime': '2021-05-25T00:00:00+03:00',
      'end_datetime': '2021-05-27T00:00:00+03:00'}]


@patch('ok.flows.flow_functions._get_departments_from_api', return_value=STAFF_API_MOCK)
def test_skip_duplicates(mocked):
    res = get_department_chain_of_heads(url='dep1', skip_duplicates=True)
    assert res == ['chief2', 'chief1', 'chief3']


@patch('ok.flows.executor.person_repo.get_one', return_value=STAFF_API_PERSON_DEP_MOCK)
def test_get_department_chain(mocked):
    res = get_department_chain('login')
    assert res == ['yandex', 'yandex_mega_dep', 'yandex_small_dep']


@patch('ok.utils.abc_repo.AbcWorker._iter_abc', lambda x, y, z: [(yield val) for val in DUTY_DATA])
def test_get_duty():
    res = get_duty_by_service_slug('service')
    assert set(res) == {'dmitrypro', 'zivot', 'shigarus'}


@patch('ok.flows.flow_functions._get_departments_from_api', return_value=STAFF_API_MOCK)
@pytest.mark.parametrize(
    'query,res_position',
    [
        ({'until_id': 3}, 3),
        ({'until_url': 'dep2'}, 2),
        ({'until_name': 'dep_3'}, 3),
        ({'until_id': 1}, 1),
        ({'until_id': 2, 'include_until': False}, 1),
        ({}, 4),
    ]
)
def test_get_departments_chain_until(mocked, query, res_position):
    res = get_departments_chain_with_heads(url='dep1', **query)
    assert res == STAFF_API_MOCK[:res_position]


def test_execute_flow(client):
    flow1 = FlowFactory(
        name='flow1',
        code='add_approver("flow1_approver1")\n'
             'add_approver("flow1_approver2")\n'
    )
    url = reverse('private_api:flows:test_flow', args=(flow1.pk,))
    resp_content = client.get(url).json()

    assert resp_content['stages'] == [{'approver': 'flow1_approver1'}, {'approver': 'flow1_approver2'}]

    flow2 = FlowFactory(
        name='flow2',
        code=('add_approver("flow2_approver1")\n'
              'add_approver("flow2_approver2")\n'
              'flow1_result = execute_flow("flow1")[\'stages\']\n'
              'f1_approvers = [approver["approver"] for approver in flow1_result]\n'
              'for f1_approver in f1_approvers:\n'
              '    add_approver(f1_approver)\n'
              ),
    )
    url = reverse('private_api:flows:test_flow', args=(flow2.pk,))
    resp_content = client.get(url).json()

    assert resp_content['stages'] == [
        {'approver': 'flow2_approver1'},
        {'approver': 'flow2_approver2'},
        {'approver': 'flow1_approver1'},
        {'approver': 'flow1_approver2'},
    ]


def test_recurse_execute_flow(client):
    flow1 = FlowFactory(
        name='flow1',
        code='add_approver("flow1_approver")\n'
             'flow2_result = execute_flow("flow2")[\'stages\']\n'
    )
    flow2 = FlowFactory(
        name='flow2',
        code=('add_approver("flow2_approver")\n'
              'flow1_result = execute_flow("flow1")[\'stages\']\n'
              ),
    )
    url = reverse('private_api:flows:test_flow', args=(flow1.pk,))

    resp_content = client.get(url).json()
    assert resp_content['stages'] == []

    flow1.refresh_from_db()
    flow2.refresh_from_db()

    assert 'FlowRecursionError' in flow1.last_error
    assert flow2.last_error is None


def test_recurse_execute_flow_with_params(client):
    flow1 = FlowFactory(
        name='flow1',
        code='add_approver("flow1_approver")\n'
             'flow2_result = execute_flow("flow2", author="someone")[\'stages\']\n'
             'extend_approvers(flow2_result)\n'
    )
    FlowFactory(
        name='flow2',
        code=('add_approver(params["author"])\n')
    )
    url = reverse('private_api:flows:test_flow', args=(flow1.pk,))

    resp_content = client.get(url).json()['stages']
    assert resp_content == [{'approver': 'flow1_approver'}, {'approver': 'someone'}]


@pytest.mark.parametrize('merge_to_right', (True, False))
def test_extend_approvers(client, merge_to_right):
    flow1 = FlowFactory(
        name='flow1',
        code='add_approver("flow1_approver")\n'
             'add_approvers_group(["flow1_group_approver1", "flow1_group_approver2"])\n'
             'flow2_result = execute_flow("flow2")[\'stages\']\n'
             'extend_approvers(flow2_result, merge_to_right={})\n'.format(merge_to_right)
    )
    FlowFactory(
        name='flow2',
        code=('add_approver("flow2_approver")\n'
              'add_approver("flow1_approver")\n'
              'add_approvers_group(['
              '"flow2_group_approver1", "flow1_approver", "flow2_group_approver2"'
              '])\n'
              ),
    )
    url = reverse('private_api:flows:test_flow', args=(flow1.pk,))

    resp_content = client.get(url).json()['stages']

    if not merge_to_right:
        assert resp_content == [
            {'approver': 'flow1_approver'},
            {'need_approvals': 1,
             'stages': [{'approver': 'flow1_group_approver1'},
                        {'approver': 'flow1_group_approver2'}]},
            {'approver': 'flow2_approver'},
            {'need_approvals': 1,
             'stages': [{'approver': 'flow2_group_approver1'},
                        {'approver': 'flow2_group_approver2'}]},
        ]
    else:
        assert resp_content == [
            {'approver': 'flow2_approver'},
            {'approver': 'flow1_approver'},
            {'need_approvals': 1,
             'stages': [{'approver': 'flow2_group_approver1'},
                        {'approver': 'flow1_approver'},
                        {'approver': 'flow2_group_approver2'}]},
            {'need_approvals': 1,
             'stages': [{'approver': 'flow1_group_approver1'},
                        {'approver': 'flow1_group_approver2'}]},
        ]


@pytest.mark.parametrize('keep_firsts', (True, False))
def test_remove_duplicates(client, keep_firsts):
    flow1 = FlowFactory(
        name='flow1',
        code='add_approver("1_approver")\n'
             'add_approver("2_approver")\n'
             'add_approver("3_approver")\n'
             'add_approver("1_approver")\n'
             'remove_duplicates(keep_firsts={})\n'.format(keep_firsts)
    )

    url = reverse('private_api:flows:test_flow', args=(flow1.pk,))

    resp_content = client.get(url).json()

    if keep_firsts:
        assert resp_content == {
            'stages':
                [
                    {'approver': '1_approver'},
                    {'approver': '2_approver'},
                    {'approver': '3_approver'},
                ]
        }
    else:
        assert resp_content == {
            'stages':
                [
                    {'approver': '2_approver'},
                    {'approver': '3_approver'},
                    {'approver': '1_approver'},
                ]
        }
