# -*- coding: utf-8 -*-
import pytest
import datetime

from balance import constants as cst

from medium.medium_logic import Logic

from tests import object_builder as ob


NOW = datetime.datetime.now()


@pytest.fixture()
def logic():
    return Logic()


@pytest.mark.promo_code
def test_import_promocodes(logic):
    start_dt = NOW
    end_dt = NOW + datetime.timedelta(days=10)
    params = {
        'StartDt': start_dt,
        'EndDt': end_dt,
        'CalcClassName': 'LegacyPromoCodeGroup',
        'CalcParams': {
                    "service_ids": [7],
                    "middle_dt": start_dt + datetime.timedelta(days=5),
                    "multicurrency_bonuses": {"RUB": {"bonus1": 15000, "bonus2": 15000}},
                    "discount_pct": u"0",
                    "bonus1": u"500",
                    "bonus2": u"500"
                },
        'Promocodes': [{'code': str(ob.get_big_number()), 'client_id': None} for _i in xrange(3)],
        'EventName': 'Test Event',
        'FirmId': cst.FirmId.YANDEX_OOO,
        'ReservationDays': 30,
        'TicketId': None,
        'NewClientsOnly': True,
        'ValidUntilPaid': True,
        'IsGlobalUnique': False,
        'NeedUniqueUrls': True,
        'SkipReservationCheck': False,
        'MinimalAmounts': {'FISH': '0.3'},
        'ServiceIds': [cst.ServiceId.MARKET, cst.ServiceId.AUTORU]
    }
    code, res = logic.ImportPromoCodes([params])
    assert code == 0
    assert res == 'SUCCESS'
