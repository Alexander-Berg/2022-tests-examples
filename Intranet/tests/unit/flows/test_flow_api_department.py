import pytest
from copy import deepcopy
from unittest.mock import patch

from ok.flows.flow_api import Department, Person


def test_magic():
    assert Department('ya') == Department('ya')
    assert Department('ya') != 'ya'
    assert Department('ya') != Department('xxx')

    assert repr(Department('ya')) == 'Department<ya>'

    assert hash(Department('ya')) == hash('ya')

    assert str(Department('ya')) == 'ya'


_department_mock_data = {
    'parent': {'url': 'yandex_edu_analytics_bo'},
    'department': {'heads': [{'person': {'login': 'evgpopkova'}, 'role': 'chief'}]},
    'ancestors': [
        {'department': {
            'url': 'yandex',
            'heads': [
                {'person': {'login': 'volozh'}, 'role': 'general_director'},
                {'person': {'login': 'tigran'}, 'role': 'chief'},
            ]
        }},
        {'department': {
                'url': 'yandex_edu',
                'heads': [
                    {'person': {'login': 'bunina'}, 'role': 'chief'},
                    {'person': {'login': 'kettu'}, 'role': 'hr_partner'},
                    {'person': {'login': 'uttke'}, 'role': 'hr_partner'},
                ]
        }},
        {'department': {
            'url': 'yandex_edu_analytics',
            'heads': [
                {'person': {'login': 'kristinapogosyan'}, 'role': 'chief'}
            ]
        }},
        {'department': {
            'url': 'yandex_edu_analytics_bo',
            'heads': [
                {'person': {'login': 'angelina-iln'}, 'role': 'chief'},
                {'person': {'login': 'mguryanova'}, 'role': 'deputy'},
            ]
        }}
    ],
}


@patch('ok.flows.flow_api.Department._get_data', return_value=deepcopy(_department_mock_data))
def test_parent(mocked):
    assert Department('ya').parent == Department('yandex_edu_analytics_bo')


@patch('ok.flows.flow_api.Department._get_data', return_value=deepcopy(_department_mock_data))
def test_get_ancestors(mocked):
    ya = Department('ya')
    assert ya.get_ancestors() == [
        ya,
        Department('yandex_edu_analytics_bo'),
        Department('yandex_edu_analytics'),
        Department('yandex_edu'),
        Department('yandex'),
    ]


@patch('ok.flows.flow_api.Department._get_data', return_value=deepcopy(_department_mock_data))
def test_head(mocked):
    assert Department('ya').head == Person('evgpopkova')


@patch('ok.flows.flow_api.Department._get_role_owners', return_value=iter(()))
def test_no_head(mocked):
    assert Department('ya').head == None


@patch('ok.flows.flow_api.Department._get_role_owners', return_value=iter(()))
def test_no_heads_chain(mocked):
    assert Department('ya').heads_chain == []


@patch('ok.flows.flow_api.Department._get_data', return_value=deepcopy(_department_mock_data))
def test_hr_partners(mocked):
    assert Department('ya').hr_partners == [Person('kettu'), Person('uttke')]


@patch('ok.flows.flow_api.Department._get_data', return_value=deepcopy(_department_mock_data))
def test_heads_chain(mocked):
    assert Department('ya').heads_chain == [
        Person('evgpopkova'),
        Person('angelina-iln'),
        Person('kristinapogosyan'),
        Person('bunina'),
        Person('tigran'),
    ]


