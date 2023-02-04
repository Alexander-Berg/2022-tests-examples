# coding: utf-8

import pytest

import itertools
import uuid

from django.conf import settings

from at.aux_ import models as aux_models


FAKE_USERS = [
    {'login': 'deadman', 'person_id': 8756, 'user_type': 'profile', 'status':'dismissed'},
    {'login': 'this-is-login', 'person_id': 12345, 'user_type': 'profile'},
    {'login': 'login-from-bb', 'person_id': 2345, 'user_type': 'profile'},
    {'login': 'theigel', 'person_id': 11248, 'user_type': 'profile'},
    {'login': 'someclub', 'person_id': 4611686018427389188, 'user_type': 'community', 'community_type': 'OPENED_COMMUNITY'},
    {'login': 'testclub', 'person_id': 4611686018427389999, 'user_type': 'community', 'community_type': 'OPENED_COMMUNITY'},
    {'login': 'closedclub', 'person_id': 4611686018427389189, 'user_type': 'community', 'community_type': 'CLOSED_COMMUNITY'},
    {'login': 'kukutz', 'person_id': 11278, 'user_type': 'profile'},
]


@pytest.fixture
def person_builder(db):
    def builder(**kwargs):
        params = get_person_default_params()
        params.update(kwargs)
        try:
            person = aux_models.Person.objects.get(person_id=params['person_id'])
        except aux_models.Person.DoesNotExist:
            person = aux_models.Person.objects.create(**params)
        return person
    return builder


@pytest.fixture
def person_builder_bulk(db):
    def builder(**kwargs):
        count = kwargs.pop('_count', 1)
        models = []
        for _ in range(count):
            params = get_person_default_params()
            params.update(kwargs)
            models.append(aux_models.Person(**params))
        persons = aux_models.Person.objects.bulk_create(models)
        return persons
    return builder


person_uid_gen = itertools.count(10000000, 20000000)


def get_person_default_params():
    uid = next(person_uid_gen)
    login = 'person_' + str(uid)
    first_name_ru = 'Иван' + str(uuid.uuid4())[0]
    last_name_ru = 'Петров' + str(uuid.uuid4())[0]
    first_name_en = 'Ivan' + str(uuid.uuid4())[0]
    last_name_en = 'Petrov' + str(uuid.uuid4())[0]
    return {
        'person_id': uid,
        'login': login,
        'title': ' '.join([first_name_ru, last_name_ru]),
        'title_eng': ' '.join([first_name_en, last_name_en]),
        'user_type': aux_models.Person.PROFILE,
        'email': login + '@yandex-team.ru',
        'status': 'normal',
        'community_type': 'NONE',
        'has_access': True
    }


@pytest.fixture
def person(person_builder):
    return person_builder()


@pytest.fixture
def friend_builder(db):
    def builder(**kwargs):
        params = {}
        params.update(kwargs)
        if 'who' not in params:
            params['person_id'] = person_builder()
        else:
            params['person_id'] = params.pop('who')
        if 'whom' not in params:
            params['uid'] = person_builder()
        else:
            params['uid'] = params.pop('whom')
        friend = aux_models.Friend.objects.create(**params)
        return friend
    return builder


@pytest.fixture
def test_person(person_builder):
    return person_builder(
        person_id=settings.YAUTH_TEST_USER['uid'],
        login=settings.YAUTH_TEST_USER['login'],
    )
