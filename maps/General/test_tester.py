#coding: utf-8

import nose
import json
import copy

import yandex.maps.geocoder.tester as tester
from yandex.maps.geocoder import toponym

TOPONYM_FILE_NAME = "tests/toponyms.json"

#
# Geocoder-like object
#

class DummyResponse:
    def __init__(self, toponym):
        self._toponym = toponym
        self._meta = { "version": "", "found": "1" }

    def first(self):
        return self._toponym

    def meta_data(self):
        return self._meta


class DummyGeocoder:
    def __init__(self, toponyms_file_name):
        with open(toponyms_file_name) as toponyms:
            self._toponyms = json.load(toponyms)

    def request(self, request):
        return DummyResponse(toponym.make_toponym(self._toponyms[request]))

#
# test source data
#

_successful_locality_test = {
    "request": {"geocode": u"Россия, Санкт-Петербург"},
    "expected": {
        "geoid": "2",
        "kind": "locality",
        "address": u"Россия, Санкт-Петербург",
        "coords": [30.315868, 59.939095],
        "name": u"Санкт-Петербург",
        "boundedBy": [[30.042834, 59.744465],[30.568322, 60.090935]]
    }
}

_successful_json_based_test = {
            "expected": {
                "GeoObject": {
                    "metaDataProperty": {
                        "GeocoderMetaData": {
                            "text": u"Россия, Калининград",
                            "kind": "locality",
                            "AddressDetails": {
                                "Country": {
                                    "CountryName": u"Россия",
                                    "AdministrativeArea": {
                                        "SubAdministrativeArea": {
                                            "Locality": {
                                                "LocalityName": u"Калининград"
                                            },
                                            "SubAdministrativeAreaName": u"городской округ Калининград"
                                        },
                                        "AdministrativeAreaName": u"Калининградская область"
                                    },
                                    "AddressLine": u"Калининград",
                                    "CountryNameCode": "RU"
                                }
                            },
                            "precision": "other"
                        }
                    },
                    "boundedBy": {
                        "Envelope": {
                            "upperCorner": "20.653724 54.778809",
                            "lowerCorner": "20.293859 54.630133"
                        }
                    },
                    "Point": {
                        "pos": "20.507307 54.70739"
                    },
                    "description": "Россия",
                    "name": u"Калининград"
                }
            },
            "request": {
                "geocode": u"Россия, Калининград"
            }
}


all_ok_testset_criterion = {"criterion": "percent_success", "params": 100 }

def _make_test(src_test, update_expected=None, update_request=None):
    test = copy.deepcopy(src_test)
    if update_expected:
        test["expected"].update(update_expected)
    if update_request:
        test["request"] = update_request
    return  tester.tests.Test(test["request"], toponym.make_toponym(test["expected"]), tester.criteria.test.Criterion())

def _make_testset(tests, testset_criterion=None):
    return tester.tests.TestSet("testing", tests,
        tester.criteria.testset.Criterion(testset_criterion if testset_criterion else all_ok_testset_criterion))

#
# Test functions
#

g_geocoder = DummyGeocoder(TOPONYM_FILE_NAME)


def test_json_based_tests():
    successful_test = _make_test(_successful_json_based_test) # successful test with no modifications
    assert tester.run_testset(g_geocoder, _make_testset([successful_test])).passed() # must be ok

    failed_json_based_test = copy.deepcopy(_successful_json_based_test)
    failed_json_based_test['expected']['GeoObject']['name'] = u"Караганда"
    failed_test = _make_test(failed_json_based_test)
    assert tester.run_testset(g_geocoder, _make_testset([failed_test])).failed()


def test_fields_criteria():
    successful_test = _make_test(_successful_locality_test) # successful test with no modifications
    assert tester.run_testset(g_geocoder, _make_testset([successful_test])).passed() # must be ok

    wrong_city_json_based_test = _make_test(_successful_locality_test, update_request = {"geocode": u"Россия, Калининград"}) # other city in a request
    assert tester.run_testset(g_geocoder, _make_testset([wrong_city_json_based_test])).failed()

    successful_test = _make_test(_successful_locality_test, {"name": u"Санкт-Пётёрбург"}) # test е-ё
    assert tester.run_testset(g_geocoder, _make_testset([successful_test])).passed() # must be ok

    failed_test = _make_test(_successful_locality_test, {"geoid": "11111"})
    assert tester.run_testset(g_geocoder, _make_testset([failed_test])).failed()

    failed_test = _make_test(_successful_locality_test, {"kind": "other"})
    assert tester.run_testset(g_geocoder, _make_testset([failed_test])).failed()

    failed_test = _make_test(_successful_locality_test, {"name": "other"})
    assert tester.run_testset(g_geocoder, _make_testset([failed_test])).failed()

    failed_test = _make_test(_successful_locality_test, {"address": "other"})
    assert tester.run_testset(g_geocoder, _make_testset([failed_test])).failed()

    failed_test = _make_test(_successful_locality_test, {"coords": [0.0, 0.0]})
    assert tester.run_testset(g_geocoder, _make_testset([failed_test])).failed()

    failed_test = _make_test(_successful_locality_test, {"boundedBy": [[0.0, 0.0],[0.0, 0.0]]})
    assert tester.run_testset(g_geocoder, _make_testset([failed_test])).failed()


def test_percent_criteria():
    _100_percent_criterion = {"criterion": "percent_success", "params": 100 }
    _100_percent_successful = [_make_test(_successful_locality_test)] * 100
    assert tester.run_testset(g_geocoder, _make_testset(_100_percent_successful, _100_percent_criterion)).passed()

    _95_percent_successful = [_make_test(_successful_locality_test)] * 95 + \
                             [_make_test(_successful_locality_test, {"kind": "other"})] * 5
    assert tester.run_testset(g_geocoder, _make_testset(_95_percent_successful, _100_percent_criterion)).failed()

    _95_percent_criterion = {"criterion": "percent_success", "params": 95 }
    assert tester.run_testset(g_geocoder, _make_testset(_95_percent_successful, _95_percent_criterion)).passed()


def test_counters():
    successful_tests = 30
    failed_tests = 50
    _partly_successful = [_make_test(_successful_locality_test)] * successful_tests + \
                     [_make_test(_successful_locality_test, {"kind": "other"})] * failed_tests
    result = tester.run_testset(g_geocoder, _make_testset(_partly_successful))
    assert len(result.failed_tests()) == failed_tests
    assert len(result.passed_tests()) == successful_tests


def test_sequence_call():
    successful_test = _make_test(_successful_locality_test)
    executed_tesetset = tester.run_testset(g_geocoder, _make_testset([successful_test]))
    assert executed_tesetset.passed()
    assert tester.run_testset(g_geocoder, executed_tesetset).passed()
