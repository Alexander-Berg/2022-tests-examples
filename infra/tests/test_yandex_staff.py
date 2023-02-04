import mock
from sepelib.yandex.staff import get_group_ids, get_group_member_logins


def test_staff_client(staff_client_mock):
    resp = mock.Mock()
    resp.content = '{}'
    with mock.patch('requests.Session.get', return_value=resp):
        staff_client_mock.list_persons({'some': 'spec', 'for': 'test'})


def test_get_group_ids(staff_client_mock):
    resp = mock.Mock()
    r = '{"result": [{"group": {"type":"department","ancestors":[{"id":"g1"}],"id":"g2"}}, {"group": {"type": "service", "id": "g3"}}]}'
    resp.content = r
    with mock.patch('requests.Session.get', return_value=resp):
        r = get_group_ids(staff_client_mock, 'login')
        assert r == ["g1", "g2", "g3"]


def test_get_group_member_logins(staff_client_mock):
    resp = mock.Mock()
    resp.content = '{"result": [{"person":{"login":"user1"}}, {"person":{"login":"user2"}}]}'
    with mock.patch('requests.Session.get', return_value=resp):
        r = get_group_member_logins(staff_client_mock, 'group_id')
        assert r == ["user1", "user2"]
