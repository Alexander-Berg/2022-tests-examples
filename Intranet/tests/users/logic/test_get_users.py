import pytest

from django.conf import settings

from intranet.audit.src.users.logic.user import _get_users_objects, prepare_blackbox_response, _get_user_param
from intranet.audit.src.users.models import User, StatedPerson


def test_get_users_from_blackbox_result_success():
    response = {
        'users': [
            {'id': '123', 'login': 'test',
             'dbfields': {'userinfo.firstname.uid': 'some name',
                          'userinfo.lastname.uid': 'some last name',
                          },
             },
            {'id': '1234', 'login': 'test1',
             'dbfields': {'userinfo.firstname.uid': 'some other name',
                          'userinfo.lastname.uid': 'some other last name',
                          },
             },
        ]
    }
    prepared_response = prepare_blackbox_response(response)
    users = _get_users_objects(*prepared_response, model=User)
    assert len(users) == 2
    assert users[0].uid == '123'
    assert users[0].login == 'test'
    assert users[0].first_name == 'some name'
    assert users[1].login == 'test1'
    assert users[1].last_name == 'some other last name'


def test_get_users_from_staff_success():
    staff_response = [
        {'uid': '123', 'login': 'test',
         'name': {
             'first': {
                 'ru': 'some name',
                 'en': 'eng name'
             },
             'last': {
                 'ru': 'some last name',
                 'en': 'eng last name'
             },
         }},
        {'uid': '1234', 'login': 'test1',
         'name': {
             'first': {
                 'ru': 'some other name',
                 'en': 'eng other name'
             },
             'last': {
                 'ru': 'some other last name',
                 'en': 'eng other last name'
             },
         }},
    ]
    users = _get_users_objects(*staff_response, model=User, value_map=settings.STAFF_VALUE_MAP)
    assert len(users) == 2
    assert users[0].uid == '123'
    assert users[0].login == 'test'
    assert users[0].first_name == 'some name'
    assert users[1].login == 'test1'
    assert users[1].last_name == 'some other last name'


def test_get_stated_person_from_staff_success():
    staff_response = [
        {'uid': '123', 'login': 'test',
         'name': {
             'first': {
                 'ru': 'some name',
                 'en': 'eng name'
             },
             'last': {
                 'ru': 'some last name',
                 'en': 'eng last name'
             },
         },
         'official': {
             'position': {
                 'ru': 'ru test position',
                 'en': 'en test position',
             }
         },
         'department_group': {
             'name': 'group name',
             'url': 'group slug',
         },
         },
        {'uid': '1234', 'login': 'test1',
         'name': {
             'first': {
                 'ru': 'some other name',
                 'en': 'eng other name'
             },
             'last': {
                 'ru': 'some other last name',
                 'en': 'eng other last name'
             },
         },
         'official': {
             'position': {
                 'ru': 'ru test position other',
                 'en': 'en test position other',
             }
         },
         'department_group': {
             'name': 'group name other',
             'url': 'group slug other',
         },
         },
    ]
    users = _get_users_objects(*staff_response,
                               attributes=settings.STAFF_STATED_PERSON_ATTRIBUTES,
                               value_map=settings.STAFF_STATED_PERSON_VALUE_MAP,
                               model=StatedPerson,
                               )
    assert len(users) == 2
    assert users[0].uid == '123'
    assert users[0].login == 'test'
    assert users[0].first_name == 'some name'
    assert users[0].position == 'ru test position'
    assert users[0].department == 'group name'
    assert users[0].department_slug == 'group slug'

    assert users[1].login == 'test1'
    assert users[1].last_name == 'some other last name'
    assert users[1].position == 'ru test position other'
    assert users[1].department == 'group name other'
    assert users[1].department_slug == 'group slug other'


def test_get_one(dummy_yauser):
    users = _get_users_objects(dummy_yauser, model=User)
    assert len(users) == 1
    assert users[0].login == dummy_yauser.login
    assert users[0].last_name == dummy_yauser.last_name


def test_get_many(dummy_yauser, dummy_yauser2):
    users = _get_users_objects(dummy_yauser, dummy_yauser2, model=User)
    assert len(users) == 2
    assert users[0].login == dummy_yauser.login
    assert users[0].uid == dummy_yauser.uid
    assert users[0].last_name == dummy_yauser.last_name
    assert users[0].first_name == dummy_yauser.first_name
    assert users[1].login == dummy_yauser2.login
    assert users[1].uid == dummy_yauser2.uid
    assert users[1].last_name == dummy_yauser2.last_name
    assert users[1].first_name == dummy_yauser2.first_name


def test_with_right_parameters(dummy_yauser, dummy_yauser2):
    users = _get_users_objects(dummy_yauser, dummy_yauser2,
                               model=User,
                               last_name='privet')
    assert len(users) == 2
    assert users[0].last_name == 'privet'
    assert users[1].last_name == 'privet'
    assert users[0].login == dummy_yauser.login
    assert users[1].login == dummy_yauser2.login


def test_with_wrong_parameters(dummy_yauser, dummy_yauser2):
    with pytest.raises(TypeError):
        _get_users_objects(dummy_yauser, dummy_yauser2,
                           some_very_fake_field='privet',
                           model=User,
                           )


def test__get_user_param_success_dict():
    user_data = {'data': {'uid': {'value': {'id': '1234'}}}}
    result = _get_user_param(user_data, ('data', 'uid', 'value', 'id'))
    assert result == '1234'


def test__get_user_param_success_yauser(dummy_yauser):
    result = _get_user_param(dummy_yauser, ('uid',))
    assert result == '1'


def test__get_user_param_fail_dict():
    user_data = {'data': {'uid': {'value': {'id': 1234}}}}
    with pytest.raises(KeyError):
        _get_user_param(user_data, ('uid', 'value',))


def test__get_user_param_fail_yauser(dummy_yauser):
    with pytest.raises(AttributeError):
        _get_user_param(dummy_yauser, ('uid', 'value',))
