# -*- coding: utf-8 -*-

import random
import pytest

from butils.passport import INTRA_MIN, INTRA_MAX
from tests import object_builder as ob


def get_domain_uid_value():
    return random.randint(INTRA_MIN(), INTRA_MAX())


@pytest.fixture
def passport(session):
    return ob.PassportBuilder().build(session).obj


@pytest.fixture
def domain_passport(session):
    return ob.PassportBuilder(passport_id=get_domain_uid_value()).build(session).obj


# менеджеры продаж (manager_type=1, is_sales=1)
@pytest.fixture
@pytest.mark.usefixtures("session", "domain_passport")
def some_manager(session, domain_passport):
    return ob.SingleManagerBuilder(manager_type=1, is_sales=1,
                                   domain_passport_id=domain_passport.passport_id,
                                   domain_login=domain_passport.login).build(session).obj

# менеджеры партнерки (manager_type=3, is_sales=1)
@pytest.fixture
@pytest.mark.usefixtures("session", "domain_passport")
def some_partner_manager(session, domain_passport):
    return ob.SingleManagerBuilder(manager_type=3, is_sales=1,
                                   domain_passport_id=domain_passport.passport_id,
                                   domain_login=domain_passport.login).build(session).obj


@pytest.fixture
@pytest.mark.usefixtures("session", "passport")
def manager_wo_passport(session, passport):
    return ob.SingleManagerBuilder(manager_type=1, is_sales=1,
                                   passport_id=passport.passport_id).build(session).obj


@pytest.fixture
def person(session, client):
    return ob.PersonBuilder(client=client, name='Name').build(session).obj


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def manager(session):
    return ob.SingleManagerBuilder().build(session).obj


@pytest.fixture
def tariff_group(session):
    return ob.TariffGroupBuilder().build(session).obj


@pytest.fixture
def tariff(session, tariff_group):
    return ob.TariffBuilder(tariff_group=tariff_group).build(session).obj
