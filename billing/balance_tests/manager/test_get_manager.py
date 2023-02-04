import random
import pytest

from tests import object_builder as ob
from balance.mapper.clients import Manager
from butils.passport import INTRA_MIN, INTRA_MAX
from balance import exc


def get_domain_uid_value():
    return random.randint(INTRA_MIN(), INTRA_MAX())


@pytest.fixture
def manager(session):
    return ob.SingleManagerBuilder().build(session).obj


@pytest.fixture
def passport(session):
    return ob.PassportBuilder().build(session).obj


@pytest.fixture
def domain_passport(session):
    return ob.PassportBuilder(passport_id=get_domain_uid_value()).build(session).obj


def test_get_manager_domain_uid(session, domain_passport, manager):
    manager.domain_login = domain_passport.login
    session.flush()
    result = Manager.get(session, uid=domain_passport.passport_id)
    assert result == manager


def test_get_manager_hidden(session, domain_passport, manager):
    manager.domain_login = domain_passport.login
    manager.hidden = 1
    session.flush()
    result = Manager.get(session, uid=domain_passport.passport_id)
    assert result is None


def test_get_manager_external(session, passport, manager):
    manager.passport_id = passport.passport_id
    session.flush()
    result = Manager.get(session, uid=manager.passport_id)
    assert result == manager


def test_get_manager_not_found(session):
    non_existing_uid = ob.get_big_number()
    with pytest.raises(exc.NOT_FOUND) as exc_info:
        Manager.get(session, uid=non_existing_uid, strict=1)
    assert exc_info.value.msg == 'Manager for uid {0} was not found'.format(non_existing_uid)
