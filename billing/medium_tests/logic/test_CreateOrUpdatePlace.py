# -*- coding: utf-8 -*-

import pytest

from balance import exc
from medium.medium_logic import Logic
from tests import object_builder as ob


DUMMY_CLIENT_ID = 921000921
DUMMY_PASSPORT_ID = 9219210000921921
DUMMY_SEARCH_ID = 921921921
DUMMY_URL = 'dummy.url'


@pytest.fixture
def passport(session):
    yield ob.PassportBuilder(passport_id=DUMMY_PASSPORT_ID).build(session).obj


@pytest.fixture
def client(session):
    another_client = ob.ClientBuilder(id=DUMMY_CLIENT_ID+1).build(session).obj
    yield ob.ClientBuilder(id=DUMMY_CLIENT_ID).build(session).obj


@pytest.fixture
def place(session, client):
    yield ob.PlaceBuilder.construct(session, client=client, type=20, search_id=DUMMY_SEARCH_ID)


def test_change_client_id_where_place_type_is_20(passport, place):
    params = {
        'ClientID': DUMMY_CLIENT_ID + 1,
        'Type':     place.type,
        'SearchID': place.search_id
    }
    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        Logic().CreateOrUpdatePlace(passport.passport_id, params)
    assert exc_info.value.msg == 'Invalid parameter for function: ClientID cannot be changed'


def test_create_place_without_search_id(passport, place):
    params = {
        'ClientID': DUMMY_CLIENT_ID + 1,
        'Type':     place.type
    }
    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        Logic().CreateOrUpdatePlace(passport.passport_id, params)
    assert exc_info.value.msg == 'Invalid parameter for function: SearchID not supplied'


def test_update_place_url(passport, place):
    params = {
        'ClientID': DUMMY_CLIENT_ID,
        'URL':      DUMMY_URL,
        'Type':     place.type,
        'SearchID': place.search_id
    }
    Logic().CreateOrUpdatePlace(passport.passport_id, params)
    assert place.url == DUMMY_URL
