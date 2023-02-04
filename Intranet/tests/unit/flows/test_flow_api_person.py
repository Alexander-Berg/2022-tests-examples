import pytest
from copy import deepcopy
from unittest.mock import patch, Mock

from ok.flows.flow_api import Department, Person


def test_person_magic():
    assert Person('ya') == Person('ya')
    assert Person('ya') != 'ya'
    assert Person('ya') != Person('xxx')

    assert repr(Person('ya')) == 'Person<ya>'

    assert hash(Person('ya')) == hash('ya')

    assert str(Person('ya')) == 'ya'


_person_mock_data = {
    'chiefs': [
        {'login': 'mantsu'},
        {'login': 'banff'},
        {'login': 'a-zhestkov'},
        {'login': 'nadyag'},
    ],
    'chief': {'login': 'mantsu'},
    'official': {
        'affiliation': 'external',
        'is_trainee': False,
    },
    'department_group': {'url': 'outstaff_2289_9345'},
    'hr_partners': [{'login': 'burlutskayao'}],
}


@patch('ok.flows.flow_api.Person._get_data', return_value=deepcopy(_person_mock_data))
def test_department(mocked):
    assert Person('ya').department == Department('outstaff_2289_9345')


@patch('ok.flows.flow_api.Person._get_data', return_value=deepcopy(_person_mock_data))
def test_hr_partners(mocked):
    assert Person('ya').hr_partners == [Person('burlutskayao')]


@patch('ok.flows.flow_api.Person._get_data', return_value=deepcopy(_person_mock_data))
def test_head(mocked):
    assert Person('ya').head == Person('mantsu')


@patch('ok.flows.flow_api.Person._get_data', return_value=deepcopy(_person_mock_data))
def test_department_heads_chain(mocked):
    assert Person('ya').heads_chain == [
        Person('mantsu'),
        Person('banff'),
        Person('a-zhestkov'),
        Person('nadyag'),
    ]


@patch('ok.flows.flow_api.Person._get_data', return_value=deepcopy(_person_mock_data))
def test_is_external(mocked):
    mock_method = Mock(return_value=[])
    with patch('ok.flows.flow_api.Department.get_ancestors', mock_method):
        assert Person('ya').department_chain == []
        mock_method.assert_called_once_with(True)


@patch('ok.flows.flow_api.Person._get_data', return_value=deepcopy(_person_mock_data))
def test_is_external(mocked):
    assert Person('ya').is_external == True


@patch('ok.flows.flow_api.Person._get_data', return_value=deepcopy(_person_mock_data))
def test_is_intern(mocked):
    assert Person('ya').is_intern == False
