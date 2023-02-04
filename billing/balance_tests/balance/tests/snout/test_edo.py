# -*- coding: utf-8 -*-
import pytest

import btestlib.reporter as reporter
from balance.balance_steps.other_steps import UserSteps
from balance.features import Features
from balance.snout_steps import api_steps as steps
from balance.tests.conftest import get_free_user
from btestlib.data.snout_constants import Handles

pytestmark = [reporter.feature(Features.UI, Features.EDO)]


def test_edo_types_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/edo/types
    """
    client_id, person_id, _, _ = steps.create_edo_person()
    steps.pull_handle_and_check_result(Handles.EDO_TYPES, client_id)


def test_edo_person_actual_offers_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/edo/person/actual-offers?client_id=XXX
    """
    client_id, person_id, _, _ = steps.create_edo_person()
    steps.pull_handle_and_check_result(Handles.EDO_PERSON_ACTUAL_OFFERS, client_id)


@pytest.mark.smoke
def test_edo_person_contracts_handle(get_free_user):
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/edo/person/contracts?client_id=XXX
    """
    user = get_free_user()
    UserSteps.set_role(user, role_id=0)
    client_id, _, _ = steps.create_edo_collateral()
    steps.pull_handle_and_check_result(Handles.EDO_PERSON_CONTRACTS, client_id, user=user)


def test_edo_person_offers_history_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/edo/person/offers-history?person_id=XXX&firm_kpp=YYY&firm_inn=ZZZ
    """
    _, person_id, inn, kpp = steps.create_edo_person()
    steps.pull_handle_and_check_result(Handles.EDO_PERSON_OFFERS_HISTORY, person_id, {'firm_kpp': kpp, 'firm_inn': inn})
