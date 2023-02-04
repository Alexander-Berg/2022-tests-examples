# -*- coding: utf-8 -*-
import mock
import pytest
import contextlib
import datetime
import sqlalchemy as sa
import hamcrest as hm
from datetime import datetime, timedelta
from decimal import Decimal as D

from balance import constants as cst, mapper
from cluster_tools.yt_import_edo import ImportEdo, _get_firm_exports
from tests import object_builder as ob

pytestmark = [
    pytest.mark.cashback,
]

cst.ServiceId.MARKET = cst.ServiceId.MARKET


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture(name='order')
def create_order(session, client, service_id=cst.ServiceId.DIRECT,
                 product_id=cst.DIRECT_PRODUCT_RUB_ID, **kw):
    return ob.OrderBuilder.construct(
        session,
        client=client,
        service_id=service_id,
        product_id=product_id,
        **kw
    )


def create_request(session, client, orders):
    return ob.RequestBuilder.construct(
        session,
        basket=ob.BasketBuilder(
            client=client,
            rows=[
                ob.BasketItemBuilder(order=o, quantity=qty)
                for o, qty in orders
            ],
        ),
    )


@pytest.fixture(name='invoice')
def create_invoice(session, client, person=None, request_=None, **kwargs):
    request_ = request_ or create_request(session, client, [(create_order(session, client), D('1'))])
    return ob.InvoiceBuilder.construct(
        session,
        request=request_,
        person=person,
        **kwargs
    )


@contextlib.contextmanager
def yt_client_mock(data):
    mock_path = 'yt.wrapper.YtClient'

    with mock.patch(mock_path) as m:
        m.return_value.run_map_reduce.return_value = None
        m.return_value.read_table.return_value = data
        yield m


@pytest.fixture
def base_data():
    return [
        {"STATUS": "REJECTED_BY_ME", "ORG_ID": 64552, "DEFAULT_FLAG": "Y", "LAST_UPDATE_DATE": "2020-08-16 21:52:16",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2020-08-12 18:20:33", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2ANGAAM9RxAAG", "INV_ORAROWID": "ABVES7AORAAJv3ZAAQ", "ENABLED_FLAG": "N",
         "PERSON_INN": "771887475106", "ACTIVE_END_DATE": "2020-08-16 22:03:29", "BLOCKED": "N"},
        {"STATUS": "INVITED_BY_ME", "ORG_ID": 64552, "DEFAULT_FLAG": "Y", "LAST_UPDATE_DATE": "2020-08-16 21:53:52",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2020-08-16 21:53:52", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2ANGAAM9RxAAG", "INV_ORAROWID": "ABVES7AP8AAAtHiAAN", "ENABLED_FLAG": "N",
         "PERSON_INN": "771887475106", "ACTIVE_END_DATE": "2020-08-17 20:03:50", "BLOCKED": "N"},
        {"STATUS": "INVITED_BY_ME", "ORG_ID": 121, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-05-18 17:08:53",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2021-05-13 18:14:39", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AC/AABOP9AAI", "INV_ORAROWID": "ABVES7APrAAAraAAAK", "ENABLED_FLAG": "N",
         "PERSON_INN": "781105545934", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "ERROR", "ORG_ID": 64554, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-05-27 18:14:56",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-05-25 18:08:16", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2ADQAAGzeBAAF", "INV_ORAROWID": "ABVES7AEKAAC35CAAT", "ENABLED_FLAG": "N",
         "PERSON_INN": "616300980347", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "REJECTED_BY_ME", "ORG_ID": 64552, "DEFAULT_FLAG": "Y", "LAST_UPDATE_DATE": "2021-06-14 18:24:36",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2021-06-12 18:41:46", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2ANXAAP41hAAG", "INV_ORAROWID": "ABVES7ANsAABgCFAAY", "ENABLED_FLAG": "N",
         "PERSON_INN": "230217956389", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "NOT_FOUND", "ORG_ID": 111012, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-07-25 19:38:45",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-07-23 19:28:25", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2ALzAAA3i8AAR", "INV_ORAROWID": "ABVES7AL6AACa7oAAQ", "ENABLED_FLAG": "N",
         "PERSON_INN": "563604859561", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "NOT_FOUND", "ORG_ID": 111012, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-07-25 19:38:45",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-07-23 19:28:25", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AMIAAA7EiAAU", "INV_ORAROWID": "ABVES7AL6AACa7oAAO", "ENABLED_FLAG": "N",
         "PERSON_INN": "170108522296", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "INVITED_BY_ME", "ORG_ID": 150405, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-12-31 13:23:33",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2021-07-23 19:15:26", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AOQAAClZ1AAf", "INV_ORAROWID": "ABVES7AMJAACzUAAAF", "ENABLED_FLAG": "Y",
         "PERSON_INN": "482425847409", "ACTIVE_END_DATE": "2021-09-08 18:14:31", "BLOCKED": "N"},
        {"STATUS": "NOT_FOUND", "ORG_ID": 150405, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-12-31 13:26:24",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-07-23 19:13:41", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AOQAAClZ1AAf", "INV_ORAROWID": "ABVES7AM7AAESY0AAA", "ENABLED_FLAG": "Y",
         "PERSON_INN": "482425847409", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "REJECTED_BY_ME", "ORG_ID": 150405, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-12-31 13:38:16",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2021-09-08 18:14:31", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AOQAAClZ1AAf", "INV_ORAROWID": "ABVES7AKzAAHiRSAAD", "ENABLED_FLAG": "Y",
         "PERSON_INN": "482425847409", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "INVITED_BY_ME", "ORG_ID": 64552, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-12-31 14:21:39",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2021-06-29 18:22:09", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AJ6AABbmaAAA", "INV_ORAROWID": "ABVES7AD9AAAeSIAAS", "ENABLED_FLAG": "Y",
         "PERSON_INN": "663404679099", "ACTIVE_END_DATE": "2021-10-17 18:29:49", "BLOCKED": "N"},
        {"STATUS": "FRIENDS", "ORG_ID": 121, "DEFAULT_FLAG": "Y", "LAST_UPDATE_DATE": "2021-12-31 17:28:25",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2018-11-27 00:00:00", "PERSON_KPP": "526001001",
         "ORG_ORAROWID": "ABVES2ALgAAC0cxAAg", "INV_ORAROWID": "ABVES7AL+AAB4NBAAJ", "ENABLED_FLAG": "S",
         "PERSON_INN": "5260441824", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "FRIENDS", "ORG_ID": 121, "DEFAULT_FLAG": "Y", "LAST_UPDATE_DATE": "2021-12-31 18:10:39",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2019-01-25 00:00:00", "PERSON_KPP": "710501001",
         "ORG_ORAROWID": "ABVES2ALRAACzbgAAJ", "INV_ORAROWID": "ABVES7AL+AAB4NBAAI", "ENABLED_FLAG": "Y",
         "PERSON_INN": "7105519780", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "INVITED_BY_ME", "ORG_ID": 64554, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-12-31 20:08:21",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2021-12-01 20:25:46", "PERSON_KPP": "770401001",
         "ORG_ORAROWID": "ABVES2AOeAAGBrSAAN", "INV_ORAROWID": "ABVES7AD9AAC2r9AAG", "ENABLED_FLAG": "Y",
         "PERSON_INN": "9704073805", "ACTIVE_END_DATE": "2021-12-08 20:08:33", "BLOCKED": "N"},
        {"STATUS": "INVITED_BY_ME", "ORG_ID": 64554, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-12-31 20:08:21",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-11-30 20:10:43", "PERSON_KPP": "770401001",
         "ORG_ORAROWID": "ABVES2AOeAAGBrSAAN", "INV_ORAROWID": "ABVES7AEjAAEEJ2AAA", "ENABLED_FLAG": "Y",
         "PERSON_INN": "9704073805", "ACTIVE_END_DATE": "2021-12-08 20:08:33", "BLOCKED": "N"},
        {"STATUS": "INVITED_BY_ME", "ORG_ID": 64554, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-12-31 20:36:34",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2021-12-31 20:36:34", "PERSON_KPP": "770401001",
         "ORG_ORAROWID": "ABVES2AOeAAGBrSAAN", "INV_ORAROWID": "ABVES7ANqAADBvfAAE", "ENABLED_FLAG": "Y",
         "PERSON_INN": "9704073805", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "REJECTS_ME", "ORG_ID": 64554, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-12-31 20:37:27",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2021-08-17 18:08:32", "PERSON_KPP": "695001001",
         "ORG_ORAROWID": "ABVES2AGZAAEk6wAAW", "INV_ORAROWID": "ABVES7AOvAAAKuIAAJ", "ENABLED_FLAG": "Y",
         "PERSON_INN": "6950230363", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "FRIENDS", "ORG_ID": 64554, "DEFAULT_FLAG": "Y", "LAST_UPDATE_DATE": "2021-12-31 20:54:02",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-01-01 00:00:00", "PERSON_KPP": "770401001",
         "ORG_ORAROWID": "ABVES2AOeAAGBrSAAN", "INV_ORAROWID": "ABVES7ACaAAGHM5AAR", "ENABLED_FLAG": "Y",
         "PERSON_INN": "9704073805", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "REJECTS_ME", "ORG_ID": 64554, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-12-31 21:14:00",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2020-08-25 18:05:45", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AIRAAC8J1AAF", "INV_ORAROWID": "ABVES7AFTAACHvhAAK", "ENABLED_FLAG": "Y",
         "PERSON_INN": "332904741170", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "INVITED_BY_ME", "ORG_ID": 64554, "DEFAULT_FLAG": "Y", "LAST_UPDATE_DATE": "2021-12-31 21:19:48",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2020-11-30 18:07:49", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AIRAAC8J1AAF", "INV_ORAROWID": "ABVES7AAYAALmeqAAE", "ENABLED_FLAG": "Y",
         "PERSON_INN": "332904741170", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "REJECTS_ME", "ORG_ID": 64554, "DEFAULT_FLAG": "Y", "LAST_UPDATE_DATE": "2021-12-31 21:20:14",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-08-10 18:05:09", "PERSON_KPP": "695001001",
         "ORG_ORAROWID": "ABVES2AGZAAEk6wAAW", "INV_ORAROWID": "ABVES7AN2AABW5rAAC", "ENABLED_FLAG": "Y",
         "PERSON_INN": "6950230363", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "INVITED_BY_ME", "ORG_ID": 121, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2022-01-04 18:30:14",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2021-07-23 18:14:47", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AJCAAAt7+AAJ", "INV_ORAROWID": "ABVES7AP7AAAchnAAI", "ENABLED_FLAG": "Y",
         "PERSON_INN": "910401301180", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "NOT_FOUND", "ORG_ID": 121, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2022-01-04 18:30:14",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-07-23 18:12:57", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AJCAAAt7+AAJ", "INV_ORAROWID": "ABVES7ACpAAAqrNAAP", "ENABLED_FLAG": "Y",
         "PERSON_INN": "910401301180", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "INVITED_BY_ME", "ORG_ID": 64554, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2022-01-05 19:36:18",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-06-11 18:13:26", "PERSON_KPP": "911001001",
         "ORG_ORAROWID": "ABVES2AEVAAC3XhAAC", "INV_ORAROWID": "ABVES7AM2AAGPs3AAT", "ENABLED_FLAG": "Y",
         "PERSON_INN": "9110003924", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "NOT_FOUND", "ORG_ID": 127556, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2022-01-06 18:31:35",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-06-11 18:53:30", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2ACxAADuR8AAQ", "INV_ORAROWID": "ABVES7AOoAABzWkAAC", "ENABLED_FLAG": "Y",
         "PERSON_INN": "711101220225", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "INVITED_BY_ME", "ORG_ID": 64554, "DEFAULT_FLAG": "Y", "LAST_UPDATE_DATE": "2022-01-13 20:59:34",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2020-09-15 18:01:16", "PERSON_KPP": "344401001",
         "ORG_ORAROWID": "ABVES2ABAAAE28yAAP", "INV_ORAROWID": "ABVES7ADtAAF+TgAAF", "ENABLED_FLAG": "Y",
         "PERSON_INN": "3444409506", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "NOT_FOUND", "ORG_ID": 64552, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2022-01-16 19:02:24",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-07-23 18:40:57", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AEcAADAxBAAG", "INV_ORAROWID": "ABVES7AOfAACopfAAP", "ENABLED_FLAG": "Y",
         "PERSON_INN": "450210230420", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "INVITED_BY_ME", "ORG_ID": 64552, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2022-01-24 18:40:33",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2021-10-29 18:41:44", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AAgAALRjpAAO", "INV_ORAROWID": "ABVES7AOTAADie4AAQ", "ENABLED_FLAG": "Y",
         "PERSON_INN": "380412547842", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "NOT_FOUND", "ORG_ID": 64552, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2022-01-24 18:40:33",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-10-27 21:46:56", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AAgAALRjpAAO", "INV_ORAROWID": "ABVES7ADLAAIS+YAAO", "ENABLED_FLAG": "Y",
         "PERSON_INN": "380412547842", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
    ]


@pytest.fixture
def new_data():
    return [
        {"STATUS": "ERROR", "ORG_ID": 64554, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-05-27 18:14:56",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-05-25 18:08:16", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2ADQAAGzeBAAF", "INV_ORAROWID": "ABVES7AEKAAC35CAAT", "ENABLED_FLAG": "N",
         "PERSON_INN": "616300980347", "ACTIVE_END_DATE": "2021-09-08 18:14:55", "BLOCKED": "N"},
        {"STATUS": "NOT_FOUND", "ORG_ID": 111012, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-07-25 19:38:45",
         "OPERATORCODE": "2BM", "ACTIVE_START_DATE": "2021-07-23 19:28:25", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2ALzAAA3i8AAR", "INV_ORAROWID": "ABVES7AL6AACa7oAAQ", "ENABLED_FLAG": "N",
         "PERSON_INN": "563604859561", "ACTIVE_END_DATE": None, "BLOCKED": "N"},
        {"STATUS": "REJECTED_BY_ME", "ORG_ID": 150405, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2021-12-31 13:38:16",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2021-09-08 18:14:31", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AOQAAClZ1AAf", "INV_ORAROWID": "ABVES7AKzAAHiRSAAD", "ENABLED_FLAG": "N",
         "PERSON_INN": "482425847409", "ACTIVE_END_DATE": "2021-09-08 18:14:55", "BLOCKED": "N"},
        {"STATUS": "INVITED_BY_ME", "ORG_ID": 121, "DEFAULT_FLAG": "N", "LAST_UPDATE_DATE": "2022-01-04 18:30:14",
         "OPERATORCODE": "2BE", "ACTIVE_START_DATE": "2021-07-23 18:14:47", "PERSON_KPP": None,
         "ORG_ORAROWID": "ABVES2AJCAAAt7+AAJ", "INV_ORAROWID": "ABVES7AP7AAAchnAAI", "ENABLED_FLAG": "Y",
         "PERSON_INN": "910401301180", "ACTIVE_END_DATE": "2021-09-08 18:14:55", "BLOCKED": "N"},
    ]



@pytest.fixture(autouse=True)
def yt_table_path_mock():
    mock_path = 'yt.wrapper.TablePath'

    def _get_path(table, start_index=None):
        return table, start_index

    with mock.patch(mock_path, _get_path) as m:
        yield m


@pytest.fixture(autouse=True)
def yt_helpers_mock():
    mock_path = 'balance.utils.yt_helpers.get_token'
    with mock.patch(mock_path) as m:
        yield m


def _get_firm_export(session, org_id):
    return _get_firm_exports(session, [org_id])[0]


def test_import_insert(session, client, base_data):
    with yt_client_mock(base_data):
        ImportEdo(session).import_from_yt()

    session.expire_all()
    offers = session.query(mapper.EdoOffer).all()
    hm.assert_that(
        offers,
        hm.contains_inanyorder(*[
            hm.has_properties(
                person_inn=row["PERSON_INN"],
                person_kpp=row["PERSON_KPP"],
                status=row["STATUS"],
                blocked=(row["BLOCKED"] == 'Y'),
                default_flag=(row["DEFAULT_FLAG"] == 'Y'),
                firm_id=_get_firm_export(session, row["ORG_ID"]).firm_id,
                org_orarowid=row["ORG_ORAROWID"],
                inv_orarowid=row["INV_ORAROWID"],
            )
            for row in base_data
        ]),
    )


def test_import_update(session, client, base_data, new_data):
    with yt_client_mock(base_data):
        ImportEdo(session).import_from_yt()
    session.expire_all()
    offers_len = session.query(mapper.EdoOffer).count()

    with yt_client_mock(new_data):
        ImportEdo(session).import_from_yt()

    session.expire_all()
    offers = session.query(mapper.EdoOffer).all()

    assert len(offers) == offers_len

    new_rows_ids = [(row["ORG_ORAROWID"], row["INV_ORAROWID"]) for row in new_data]
    hm.assert_that(
        offers,
        hm.contains_inanyorder(
            *([
                hm.has_properties(
                    person_inn=row["PERSON_INN"],
                    person_kpp=row["PERSON_KPP"],
                    status=row["STATUS"],
                    blocked=(row["BLOCKED"] == 'Y'),
                    default_flag=(row["DEFAULT_FLAG"] == 'Y'),
                    firm_id=_get_firm_export(session, row["ORG_ID"]).firm_id,
                    org_orarowid=row["ORG_ORAROWID"],
                    inv_orarowid=row["INV_ORAROWID"],
                )
                for row in base_data if (row["ORG_ORAROWID"], row["INV_ORAROWID"]) not in new_rows_ids
            ] + [
                hm.has_properties(
                    person_inn=row["PERSON_INN"],
                    person_kpp=row["PERSON_KPP"],
                    status=row["STATUS"],
                    blocked=(row["BLOCKED"] == 'Y'),
                    default_flag=(row["DEFAULT_FLAG"] == 'Y'),
                    firm_id=_get_firm_export(session, row["ORG_ID"]).firm_id,
                    org_orarowid=row["ORG_ORAROWID"],
                    inv_orarowid=row["INV_ORAROWID"],
                )
                for row in new_data
            ])
        ),
    )


def test_config_update(session, client, base_data):
    session.config.set('EDO_UPDATE_FROM_DT', datetime(2019, 1, 1),  column_name='value_dt', can_create=True)
    session.config.set('EDO_UPDATE_TO_DT',   datetime(2022, 1, 31), column_name='value_dt', can_create=True)

    session.flush()

    old_update_from_dt, old_update_to_dt = session.config.get("EDO_UPDATE_FROM_DT"), session.config.get("EDO_UPDATE_TO_DT")
    with yt_client_mock(base_data):
        ImportEdo(session).import_from_yt()

    session.expire_all()
    new_update_from_dt, new_update_to_dt = session.config.get("EDO_UPDATE_FROM_DT"), session.config.get("EDO_UPDATE_TO_DT")

    assert new_update_to_dt is None
    assert new_update_from_dt == old_update_to_dt - timedelta(hours=3)
