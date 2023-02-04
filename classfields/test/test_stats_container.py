import pytest
import os
import sys

parent_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.append(parent_dir)
from api.config import YDB_ENDPOINT, YDB_DATABASE, YDB_TOKEN


@pytest.fixture
def ydb_helper_test():
    from vsml_common.data_helpers.ydb import YdbHelper
    ydb_helper = YdbHelper(
        endpoint=YDB_ENDPOINT,
        database=YDB_DATABASE,
        auth_token=YDB_TOKEN
    )
    return ydb_helper


@pytest.fixture
def new_stat_container_test(ydb_helper_test, mocker):
    import api.data_processing
    paths = {
        "building": "/ru-prestable/verticals/testing/common/vsml-features-storage/realty_building",
        "city_quadkey": "/ru-prestable/verticals/testing/common/vsml-features-storage/realty_city_quadkey",
        "region_quadkey": "/ru-prestable/verticals/testing/common/vsml-features-storage/realty_region_quadkey"
    }
    return api.data_processing.StatContainer(
        custom_stat_manager=api.data_processing.YdbStatManager(ydb_helper_test, paths=paths)
    )


@pytest.fixture
def region_quadkey_stat_test():
    return {
        'orgvisit_avg_duration_region': 1,
        'orgvisit_cnt_region': 2,
        'orgvisit_median_duration_region': 3,
        'orgvisit_unique_permalink_cnt_region': 4
    }


@pytest.fixture
def city_quadkey_stat_test():
    return {
        'orgvisit_avg_duration_city': 5,
        'orgvisit_cnt_city': 6,
        'orgvisit_median_duration_city': 7,
        'orgvisit_unique_permalink_cnt_city': 8
    }


@pytest.fixture
def unified_address_stat_test():
    return {
        'building_sell_apartment_min': 9,
        'building_sell_apartment_25': 10,
        'building_sell_apartment_50': 11,
        'building_sell_apartment_75': 12,
        'building_sell_apartment_max': 13,
        'building_sell_apartment_cnt': 14
    }


@pytest.fixture
def unified_address_stat_proto_test():
    from base64 import b64decode
    return b64decode(
        b'GhsNAAAQQRUAACBBHQAAMEElAABAQS0AAFBBMA4='
    )


def test_stat_container_test_init(stat_container_test):
    assert stat_container_test is not None


def test_new_stat_container_test_init(new_stat_container_test):
    assert new_stat_container_test is not None


def test_stat_container_test_enrich_factors(stat_container_test, unified_address_stat_test, city_quadkey_stat_test,
                                            region_quadkey_stat_test, mocker):
    region_quadkey = "1" * 10
    city_quadkey = "1" * 15
    unified_address = "test_address"
    offer_factors = {
        "region_quadkey": region_quadkey,
        "city_quadkey": city_quadkey,
        "unified_address": unified_address,
        "model_name": None,
    }

    mocker.patch.object(stat_container_test.custom_stat_manager, "region_stat",
                        {region_quadkey: region_quadkey_stat_test})
    mocker.patch.object(stat_container_test.custom_stat_manager, "city_stat", {city_quadkey: city_quadkey_stat_test})
    mocker.patch.object(stat_container_test.custom_stat_manager, "building_stat",
                        {unified_address: unified_address_stat_test})

    result_factors = stat_container_test.enrich_factors(offer_factors)

    correct_factors = {
        **offer_factors,
        **region_quadkey_stat_test,
        **city_quadkey_stat_test,
        **unified_address_stat_test
    }
    assert len(result_factors) == len(correct_factors)
    for key in result_factors:
        assert result_factors[key] == correct_factors[key]


def test_new_stat_container_test_connected_to_ydb(new_stat_container_test):
    ydb_helper = new_stat_container_test.custom_stat_manager.ydb_helper
    assert ydb_helper is not None
    assert ydb_helper.driver is not None
    session = ydb_helper.driver.table_client.session().create()
    assert session.session_id is not None


def test_new_stat_container_test_enrich_factors_by_address(new_stat_container_test, unified_address_stat_test,
                                                           unified_address_stat_proto_test, mocker):
    region_quadkey = "1" * 10
    city_quadkey = "1" * 15
    unified_address = "test_address"
    offer_factors = {
        "region_quadkey": region_quadkey,
        "city_quadkey": city_quadkey,
        "unified_address": unified_address,
        "model_name": None
    }

    def mock_ydb_response(*args, **kwargs):
        class TempElem:
            Value = unified_address_stat_proto_test

        class Temp:
            rows = [TempElem()]

        return [Temp()]

    mocker.patch.object(new_stat_container_test.custom_stat_manager.ydb_helper, "execute", mock_ydb_response)

    result_factors = new_stat_container_test.enrich_factors(offer_factors)
    correct_factors = {
        **offer_factors,
        **unified_address_stat_test
    }
    for key in correct_factors:
        assert result_factors[key] == correct_factors[key]


def test_new_stat_container_fake_protobuf_in_ydb(new_stat_container_test):
    region_quadkey = "1" * 10
    city_quadkey = "1" * 15
    unified_address = "test_address"
    history_factors = new_stat_container_test.custom_stat_manager.get_factors(unified_address, city_quadkey,
                                                                              region_quadkey)
    assert isinstance(history_factors, dict)
    assert len(history_factors) == 0


def test_new_stat_container_real_protobuf_in_ydb(new_stat_container_test):
    region_quadkey = "1" * 10
    city_quadkey = "002202222100232"
    unified_address = "test_address"
    history_factors = new_stat_container_test.custom_stat_manager.get_factors(unified_address, city_quadkey,
                                                                              region_quadkey)
    assert isinstance(history_factors, dict)
    assert len(history_factors) > 0
