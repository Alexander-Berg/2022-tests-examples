import pytest
import os
import sys

parent_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.append(parent_dir)


def assert_including_dicts(first, second):
    if isinstance(first, dict):
        for key, value in first.items():
            assert_including_dicts(value, second[key])
    elif isinstance(first, list):
        for i, j in zip(first, second):
            assert_including_dicts(i, j)
    else:
        assert first == second


@pytest.fixture
def offer_message_protobuf_test():
    from base64 import b64decode
    return b64decode(
        b'IAEwAlJAOhJ0ZXN0X2xvY2FsaXR5X25hbWVaChUAAMZCHQAAhEKCARV0ZXN0X2dlb2NvZGVyX2FkZHJ'
        b'lc3OwAcDEB+ABAVoMigEJCAEVzcwYQhgB2gEJCAEVzcwYQhgB4gEXEAEyAQRIAVABiAEKkAEAyAEB2g'
        b'ECNDbqARFwoMIeoAEI+AEAoAIJwALbD/IBDCVmZppBLTMzI0FAAPoBAhIA'
    )


@pytest.fixture
def offer_message_dict_test():
    return {
        'offer_type_int': 1,
        'category_type_int': 2,
        'location': {
            'locality_name': 'test_locality_name',
            'geocoder_point': {
                'latitude': 99.0,
                'longitude': 66.0
            },
            'geocoder_address': 'test_geocoder_address',
            'region_graph_id': '123456',
            'subject_federation_id': 1
        },
        'transaction': {
            'area': {'value': 38.2},
        },
        'area': {'value': 38.2},
        'apartment_info': {
            'balcony_int': 1,
            'floors': [4],
            'rooms': 1,
            'rooms_offered': 1,
            'renovation_int': 10,
            'studio': False,
            'flat_type_int': 1,
            'apartment': '46',
            'ceiling_height': 0.0
        },
        'building_info': {
            'site_id': '500000',
            'parking_type_int': 8,
            'expect_demolition': False,
            'floors_total': 9,
            'build_year': 2011,
            'building_type_int': 0,
            'building_series_id': '0',
            'building_id': '0',
            'flats_count': 0
        },
        'house_info': {
            'living_space': 19.3,
            'kitchen_space': 10.2,
            'pmg': False,
            'house_type_int': 0
        },
        'lot_info': {
            'lot_area': {
                'value': 0.0,
                'unit_int': 0
            },
            'lot_type_int': 0
        }
    }


@pytest.fixture
def landing_request_protobuf_test():
    from base64 import b64decode
    return b64decode(
            b'CiQNzcwYQjABOhcKACoAOgIICWIDCNsPcgIIAbIBALoBAEoCEgASQDIvChV0ZXN0X2dlb2N'
            b'vZGVyX2FkZHJlc3MSFgoSdGVzdF9sb2NhbGl0eV9uYW1lGAc6Cg0AAMZCFQAAhEKgAQE='
    )


@pytest.fixture
def landing_request_dict_test():
    return {
        'apartment': {
            'apartment_area': 38.2,
            'rooms_total': 1,
            'building_info': {
                'floors_total': 9,
                'built_year': 2011,
                'has_lift': True,
                'flats_count': 0,
                'expect_demolition': False,
                'building_type': 'BUILDING_TYPE_UNKNOWN',
                'site_id': '0'
            },
            'general_apartment_info': {
                'ceiling_height': 0.0,
                'renovation': 'RENOVATION_UNKNOWN'
            },
            'living_space': 0.0,
            'kitchen_space': 0.0,
            'studio': False,
            'flat_type': 'FLAT_TYPE_UNKNOWN'
        },
        'location': {
            'geocoder_address': {
                'unified_oneline': 'test_geocoder_address',
                'component': [
                    {'value': 'test_locality_name', 'region_type': 'CITY'}
                ]
            },
            'geocoder_coordinates': {
                'latitude': 99.0,
                'longitude': 66.0
            },
            'subject_federation_geoid': 1
        }
    }


@pytest.fixture
def price_features_protobuf_test():
    from base64 import b64decode
    return b64decode(
        b'ChsNAACAPxUAAABAHQAAQEAlAACAQC0AAKBAMAYSGw0AAIA/FQAAAEAdAABAQCUAAIBALQAAoEAw'
        b'BhobDQAAgD8VAAAAQB0AAEBAJQAAgEAtAACgQDAGIhsNAACAPxUAAABAHQAAQEAlAACAQC0AAKBAMAYwAg=='
    )


@pytest.fixture
def price_features_dict_test():
    return {
        'rent_apartment': {
            'min': 1.0, 'q25': 2.0, 'median': 3.0, 'q75': 4.0, 'max': 5.0, 'count': 6
        },
        'rent_room': {
            'min': 1.0, 'q25': 2.0, 'median': 3.0, 'q75': 4.0, 'max': 5.0, 'count': 6
        },
        'sell_apartment': {
            'min': 1.0, 'q25': 2.0, 'median': 3.0, 'q75': 4.0, 'max': 5.0, 'count': 6
        },
        'sell_room': {
            'min': 1.0, 'q25': 2.0, 'median': 3.0, 'q75': 4.0, 'max': 5.0, 'count': 6
        },
        'object_type': 'REGION_QUADKEY'
    }


def test_offer_message_to_dict(offer_message_protobuf_test, offer_message_dict_test):
    from api.io_helper import protobuf_to_dict
    result = protobuf_to_dict(offer_message_protobuf_test)
    assert_including_dicts(offer_message_dict_test, result)


def test_landing_request_to_dict(landing_request_protobuf_test, landing_request_dict_test):
    from api.io_helper import protobuf_to_dict
    result = protobuf_to_dict(landing_request_protobuf_test, protobuf_model="PriceLandingPredictionRequest")
    assert_including_dicts(landing_request_dict_test, result)


def test_price_featuresto_dict(price_features_protobuf_test, price_features_dict_test):
    from api.io_helper import protobuf_to_dict
    result = protobuf_to_dict(price_features_protobuf_test, protobuf_model="RealtyPricePredictionFeatures")
    assert_including_dicts(price_features_dict_test, result)
