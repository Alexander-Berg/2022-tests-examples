import pytest

from prediction_api.common.micromodel import (
    Configuration, SuperGeneration, MarkInfo, ModelInfo, TechParam
)


@pytest.fixture(scope="session")
def valid_mark_info():
    mark = MarkInfo()
    mark.name_ = "RENAULT"
    mark.is_popular = True
    mark.vendor_id = 5
    mark.country_id = 124
    return mark


@pytest.fixture(scope="session")
def valid_model_info():
    model = ModelInfo()
    model.name_ = "CAPTUR"
    model.is_popular = False
    return model


@pytest.fixture(scope="session")
def valid_supergen(valid_mark_info, valid_model_info):
    supergen = SuperGeneration()
    supergen.parent_mark = valid_mark_info
    supergen.parent_model = valid_model_info
    supergen.id_ = 20037120
    supergen.name_ = "I"
    supergen.year_from = 2012
    supergen.year_to = 2017
    supergen.segment = "MEDIUM"
    supergen.group = "CITY"
    supergen.is_restyle = False
    return supergen


@pytest.fixture(scope="session")
def valid_techparam():
    techparam = TechParam()
    techparam.id_= 20037126
    techparam.tech_params = {
          "engineType": "GASOLINE",
          "displacement": 1197,
          "gearType": "FORWARD_CONTROL",
          "transmission": "ROBOT",
          "power": 120,
          "powerKvt": "85.0",
          "yearStart": 2013,
          "yearStop": 2017,
          "humanName": "1.2 AMT (120 л.с.)",
          "cylindersOrder": "IN-LINE",
          "cylindersValue": 4,
          "valves": 4,
          "diameter": "72.2x73.1",
          "compression": 9.8,
          "petrolType": "95 RON",
          "emissionEuroClass": "Euro 5",
          "gearValue": 6,
          "maxSpeed": 192,
          "acceleration": 10.9,
          "consumptionMixed": 5.4,
          "consumptionCity": 6.6,
          "consumptionHiway": 4.7,
          "weight": 1180,
          "fullWeight": 1726,
          "wheelSize": "205/60/R16, 205/55/R17",
          "engineOrder": "Front transverse engine",
          "powerRpm": [
            4900
          ],
          "engineFeeding": "Direct fuel injection",
          "feeding": "Turbocharger",
          "moment": 190.0,
          "momentRpm": [
            2000
          ],
          "clearance": [
            170
          ],
          "backSuspension": "Semi-independent, coil suspension",
          "backBrake": "Drum",
          "fuelEmission": 125
        }
    return techparam


@pytest.fixture(scope="session")
def valid_configuration(valid_techparam, valid_supergen):
    configuration = Configuration()
    configuration.id_ = 20037123
    configuration.doors_count = 5
    configuration.body_type = "ALLROAD_5_DOORS"
    configuration.steering_wheel = "LEFT"
    configuration.techparams = {valid_techparam.id_: valid_techparam}
    configuration.parent_supergen = valid_supergen
    return configuration


@pytest.fixture(scope="session")
def configuration_json():
    with open("tests_data/configuration_20037123.json") as f:
        return f.read()


def test_from_catalog_json_configuration(valid_configuration, configuration_json):
    conf = Configuration.from_catalog_json(configuration_json)
    assert conf == valid_configuration


def test_from_catalog_json_supergen(valid_supergen, configuration_json):
    conf = Configuration.from_catalog_json(configuration_json)
    assert conf.parent_supergen == valid_supergen


def test_from_catalog_json_techparam(valid_techparam, configuration_json):
    conf = Configuration.from_catalog_json(configuration_json)
    assert conf.techparams[valid_techparam.id_] == valid_techparam


def test_from_catalog_json_mark(valid_mark_info, configuration_json):
    conf = Configuration.from_catalog_json(configuration_json)
    assert conf.parent_supergen.parent_mark == valid_mark_info


def test_from_catalog_json_model(valid_model_info, configuration_json):
    conf = Configuration.from_catalog_json(configuration_json)
    assert conf.parent_supergen.parent_model == valid_model_info
