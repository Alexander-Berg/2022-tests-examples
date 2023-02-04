# -*- coding: utf-8 -*-

import pytest
from tests import object_builder as ob
from balance.constants import FirmId


@pytest.fixture
def manager(session):
    return ob.SingleManagerBuilder().build(session).obj


@pytest.fixture
def existing_passport(session):
    return ob.PassportBuilder().build(session).obj


@pytest.fixture
def not_existing_passport_id():
    return str(-ob.get_big_number())


def test_get_managers_info(xmlrpcserver, session, manager, existing_passport, not_existing_passport_id):
    manager.passport_id = existing_passport.passport_id
    manager.firm_id = FirmId.YANDEX_OOO
    session.flush()

    method_result = xmlrpcserver.GetManagersInfo([existing_passport.passport_id, not_existing_passport_id])
    firm_id_from_method = method_result[str(existing_passport.passport_id)]['firm_id']

    # Проверяем, что возвращаются данные только по 2м переданным паспортам
    assert len(method_result) == 2

    # Проверяем, что для несуществующего паспорта возвращается None
    assert method_result[not_existing_passport_id] is None

    # Проверяем, что для менеджерского паспорта возвращается правильное значение фирмы
    assert manager.firm_id == firm_id_from_method
