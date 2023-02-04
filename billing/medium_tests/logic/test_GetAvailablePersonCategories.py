# -*- coding: utf-8 -*-

import pytest

from balance.constants import (
    RegionId,
    ServiceId,
    FirmId,
)

from tests import object_builder as ob


def test_client_russia_resident(session, xmlrpcserver):
    client = ob.ClientBuilder.construct(session, region_id=RegionId.RUSSIA)

    res = xmlrpcserver.GetAvailablePersonCategories({'ClientID': client.id})

    assert {'category': 'ur', 'region_id': RegionId.RUSSIA, 'resident': 1, 'legal_entity': 1, 'firm_id': FirmId.YANDEX_OOO} in res
    assert {'category': 'ph', 'region_id': RegionId.RUSSIA, 'resident': 1, 'legal_entity': 0, 'firm_id': FirmId.YANDEX_OOO} in res
    assert len(res) > 2


def test_client_russia_resident_by_service(session, xmlrpcserver):
    client = ob.ClientBuilder.construct(session, region_id=RegionId.RUSSIA)

    res = xmlrpcserver.GetAvailablePersonCategories({'ClientID': client.id, 'ServiceID': ServiceId.DIRECT})

    assert sorted(res) == [
        {'category': 'ph', 'region_id': RegionId.RUSSIA, 'resident': 1, 'legal_entity': 0, 'firm_id': 1},
        {'category': 'ur', 'region_id': RegionId.RUSSIA, 'resident': 1, 'legal_entity': 1, 'firm_id': 1},
    ]


def test_russia_resident_by_service_region(session, xmlrpcserver):
    res = xmlrpcserver.GetAvailablePersonCategories({'RegionID': RegionId.RUSSIA, 'ServiceID': ServiceId.DIRECT})

    assert sorted(res) == [
        {'category': 'ph', 'region_id': RegionId.RUSSIA, 'resident': 1, 'legal_entity': 0, 'firm_id': FirmId.YANDEX_OOO},
        {'category': 'ur', 'region_id': RegionId.RUSSIA, 'resident': 1, 'legal_entity': 1, 'firm_id': FirmId.YANDEX_OOO},
    ]


def test_russia_nonresident_by_service_region(session, xmlrpcserver):
    res = xmlrpcserver.GetAvailablePersonCategories({'RegionID': RegionId.ARMENIA, 'ServiceID': ServiceId.DIRECT})

    assert sorted(res) == [
        {'category': 'by_ytph', 'resident': 0, 'firm_id': FirmId.YANDEX_EU_AG, 'region_id': RegionId.SWITZERLAND, 'legal_entity': 0},
        {'category': 'yt', 'resident': 0, 'firm_id': FirmId.YANDEX_OOO, 'region_id': RegionId.RUSSIA, 'legal_entity': 1},
        {'category': 'ytph', 'resident': 0, 'firm_id': FirmId.YANDEX_OOO, 'region_id': RegionId.RUSSIA, 'legal_entity': 0},
    ]


@pytest.mark.parametrize(
    'is_agency, is_contract',
    [
        (None, 0),
        (None, 1),
        (0, None),
        (0, 0),
        (0, 1),
        (1, None),
        (1, 0),
        (1, 1),
    ]
)
def test_extra_flags(session, xmlrpcserver, is_agency, is_contract):
    params = {
        'RegionID': RegionId.RUSSIA,
        'ServiceID': ServiceId.DIRECT
    }
    if is_agency is not None:
        params['IsAgency'] = is_agency
    if is_contract is not None:
        params['IsContract'] = is_contract

    res = xmlrpcserver.GetAvailablePersonCategories(params)

    assert sorted(res) == [
        {'category': 'ph', 'region_id': RegionId.RUSSIA, 'resident': 1, 'legal_entity': 0, 'firm_id': FirmId.YANDEX_OOO},
        {'category': 'ur', 'region_id': RegionId.RUSSIA, 'resident': 1, 'legal_entity': 1, 'firm_id': FirmId.YANDEX_OOO},
    ]
