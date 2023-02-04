# -*- coding: utf-8 -*-
import pytest

from balance import mapper
from balance.constants import RegionId
from balance.core import Core
from butils.application import getApplication
from tests import object_builder as ob


@pytest.fixture(
    params=[
        'us_yt',
        'us_ytph',
    ]
)
def target_category(request):
    return request.param


PERSON_CREATION_DATA = {
    'us_yt': {
        'name': u'Test us_yt РАО «Сысоев Сазонова»',
        'phone': '+7 495 6660666',
        'email': '0^$-@rNc3G.QdG',
        'postaddress': u'Улица 25',
        'postcode': '43857',
    },
    'us_ytph': {
        'lname': 'Ivanov',
        'fname': 'Ivan',
        'phone': '+7 495 6660666',
        'email': '0^$-@rNc3G.QdG',
    },
}

EXPECTED_CREATION_ABILITY_PER_REGION = {
    RegionId.SWITZERLAND: True,
    RegionId.RUSSIA: False,
    RegionId.BELARUS: False,
    RegionId.KAZAKHSTAN: False,
    RegionId.UNITED_STATES: False,
}


@pytest.fixture()
def category_person_creation_data(target_category):
    return PERSON_CREATION_DATA[target_category]


@pytest.fixture
def manager(session):
    return ob.SingleManagerBuilder().build(session).obj


@pytest.fixture(params=[RegionId.SWITZERLAND])
def client(session, request):
    region_id = request.param
    return ob.ClientBuilder(
        region_id=region_id,
    ).build(session).obj


@pytest.fixture
def services(session):
    return tuple(
        s.id for s in (
            session.query(mapper.Service)
                .outerjoin(mapper.BalanceService)
                .filter(mapper.BalanceService.client_only == 0)
        )
    )


@pytest.mark.parametrize(
    ['client', 'presence_expected'],
    EXPECTED_CREATION_ABILITY_PER_REGION.items(),
    indirect=['client']
)
def test_available_category_presence(
    session,
    client,
    services,
    target_category,
    presence_expected,
):
    available_categories = client.get_available_person_categories(
        services,
        with_self=True,
    )
    available_categories_names = [
        person_category.category
        for person_category in available_categories
    ]

    assert (target_category in available_categories_names) == presence_expected


@pytest.mark.parametrize(
    ['client', 'presence_expected'],
    EXPECTED_CREATION_ABILITY_PER_REGION.items(),
    indirect=['client']
)
def test_in_creatable_category(
    session,
    client,
    target_category,
    presence_expected,
):
    creatable_categories = client.get_creatable_person_categories()
    creatable_categories_names = [
        person_category.category
        for person_category in creatable_categories
    ]

    assert (target_category in creatable_categories_names) == presence_expected


def test_create_person(session, manager, client, target_category, category_person_creation_data):
    person_creation_data = {
        'client_id': client.id,
        'region': RegionId.UNITED_STATES,
        'person_id': 0,
        'type': target_category,
    }
    person_creation_data.update(category_person_creation_data)

    person_id = Core(
        session,
        getApplication().dbhelper,
    ).create_or_update_person(
        session,
        operator_uid=manager.manager_code,
        client_hash=person_creation_data,
    )
    person = session.query(mapper.Person).get(person_id)

    for key, value in person_creation_data.items():
        if key != 'person_id':
            assert getattr(person, key) == value

