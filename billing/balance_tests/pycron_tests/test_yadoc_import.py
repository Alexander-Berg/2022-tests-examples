# -*- coding: utf-8 -*-
import mock
import pytest
import contextlib
import datetime
import sqlalchemy as sa
import hamcrest as hm

from balance import constants as cst, mapper
from cluster_tools.yadoc_import import ImportYadocInfo
from tests import object_builder as ob


@pytest.fixture(autouse=True)
def patching():
    with mock.patch('balance.api.yadoc.YaDocApi._get_tvm_ticket', return_value='666'), \
         mock.patch('butils.application.plugins.components_cfg.get_component_cfg',
                    return_value={'Url': 'https://yadoc-test.mba.yandex-team.ru/public/api/', 'PageSize': 20}):
        yield


@contextlib.contextmanager
def yadoc_api_mock(obj, **kwargs):
    with mock.patch.object(obj, 'get_available_organizations', return_value=kwargs.get('organizations', None)), \
         mock.patch.object(obj, 'get_ap_closed_period', kwargs.get('ap_closed_period_callable', None)):
        yield obj

CLOSE_DATES = {
    121: datetime.datetime(2022, 1, 1),
    64554: None,
    145543: datetime.datetime(2022, 2, 1)
}

def get_ap_closed_period(oebs_org_id):
    return CLOSE_DATES[oebs_org_id]


def test_import_insert(session):
    import_yadoc_info = ImportYadocInfo(session)
    with yadoc_api_mock(
        import_yadoc_info._api,
        organizations=[
            {"short_code": "YARU", "org_name": 'ООО "ЯНДЕКС"',        "source_system": "OEBS", "source_entity_id": "121"},
            {"short_code": "YMAR", "org_name": 'ООО "Яндекс.Маркет"', "source_system": "OEBS", "source_entity_id": "64554"},
        ],
        ap_closed_period_callable=get_ap_closed_period
    ):
        import_yadoc_info.main()

    session.expire_all()
    yfs = session.query(mapper.YadocFirm).all()
    hm.assert_that(
        yfs,
        hm.contains_inanyorder(*[
            hm.has_properties(
                firm_id=cst.FirmId.YANDEX_OOO,
                last_closed_dt=CLOSE_DATES[121],
            ),
            hm.has_properties(
                firm_id=cst.FirmId.MARKET,
                last_closed_dt=hm.is_(CLOSE_DATES[64554]),
            )
        ]),
    )


def test_import_update(session):
    ob.YadocFirmBuilder.construct(session, firm_id=cst.FirmId.YANDEX_OOO, last_closed_dt=None)
    ob.YadocFirmBuilder.construct(session, firm_id=cst.FirmId.MARKET, last_closed_dt=datetime.datetime(2021, 12, 1))
    ob.YadocFirmBuilder.construct(session, firm_id=cst.FirmId.CLOUD, last_closed_dt=datetime.datetime(2021, 11, 1))
    import_yadoc_info = ImportYadocInfo(session)
    with yadoc_api_mock(
        import_yadoc_info._api,
        organizations=[
            {"short_code": "YARU", "org_name": 'ООО "ЯНДЕКС"',        "source_system": "OEBS", "source_entity_id": "121"},
            {"short_code": "YMAR", "org_name": 'ООО "Яндекс.Маркет"', "source_system": "OEBS", "source_entity_id": "64554"},
            {"short_code": "YACL", "org_name": 'ООО "Яндекс.Облако"', "source_system": "OEBS", "source_entity_id": "145543"},
        ],
        ap_closed_period_callable=get_ap_closed_period
    ):
        import_yadoc_info.main()

    session.expire_all()
    yfs = session.query(mapper.YadocFirm).all()
    hm.assert_that(
        yfs,
        hm.contains_inanyorder(*[
            hm.has_properties(
                firm_id=cst.FirmId.YANDEX_OOO,
                last_closed_dt=CLOSE_DATES[121],
            ),
            hm.has_properties(
                firm_id=cst.FirmId.MARKET,
                last_closed_dt=CLOSE_DATES[64554],
            ),
            hm.has_properties(
                firm_id=cst.FirmId.CLOUD,
                last_closed_dt=CLOSE_DATES[145543],
            )
        ]),
    )


def test_import_delete(session):
    ob.YadocFirmBuilder.construct(session, firm_id=cst.FirmId.YANDEX_OOO, last_closed_dt=None)
    ob.YadocFirmBuilder.construct(session, firm_id=cst.FirmId.MARKET, last_closed_dt=datetime.datetime(2021, 12, 1))
    ob.YadocFirmBuilder.construct(session, firm_id=cst.FirmId.CLOUD, last_closed_dt=datetime.datetime(2021, 11, 1))
    import_yadoc_info = ImportYadocInfo(session)
    with yadoc_api_mock(import_yadoc_info._api, organizations=[]):
        import_yadoc_info.main()

    session.expire_all()
    yfs = session.query(mapper.YadocFirm).all()
    hm.assert_that(yfs, hm.has_length(0))
