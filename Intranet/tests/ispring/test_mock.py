import uuid
from xml.etree.ElementTree import tostring

from intranet.hrdb_ext.src.ispring.mock.resources import MockedUser, MockedDepartment


def test_mocked_user_create():
    user = MockedUser.generate()
    assert isinstance(user, MockedUser)
    assert user.user_id
    assert user.user_id == user.uid()

    length = len(str(uuid.uuid4()))
    assert len(user.user_id) == length
    assert len(user.login) == length
    assert len(user.first_name) == length
    assert len(user.last_name) == length
    assert len(user.department_id) == length
    assert len({user.user_id, user.login, user.first_name, user.last_name, user.department_id}) == 5


def test_mocked_user_update():
    user = MockedUser.generate()
    assert all([user.user_id, user.login, user.first_name, user.last_name, user.department_id])

    user.update({
        'login': '1',
        'first_name': '2',
        'last_name': '3',
        'department_id': '4',
    })
    assert user.login == '1'
    assert user.first_name == '2'
    assert user.last_name == '3'
    assert user.department_id == '4'


def test_mocked_user_serialization():
    user = MockedUser.generate()
    assert all([user.user_id, user.login, user.first_name, user.last_name, user.department_id])

    xml = user.serialize()
    assert xml.tag == user.response_key
    assert xml.find('userId').text == user.user_id
    assert xml.find('departmentId').text == user.department_id

    text = str(tostring(xml))
    assert user.first_name in text
    assert user.last_name in text
    assert user.login in text


def test_mock_department_serialization():
    dep = MockedDepartment.generate()
    assert all([dep.department_id, dep.name, dep.code, dep.parent_id])

    xml = dep.serialize()
    assert xml.tag == dep.response_key
    assert xml.find('departmentId').text == dep.department_id
    assert xml.find('parentDepartmentId').text == dep.parent_id
    assert xml.find('name').text == dep.name
    assert xml.find('code').text == dep.code
