import sys

sys.path.append("schema")
sys.path.append("../schema")

import pytest
import numpy as np
from rest_api import Resources
from rest_api.calculator import StatCalculator
from rest_api.region_api import RegionTree
from rest_api.catalog import Catalog

from rest_api.model import StatsMarkResponseV3, StatsModelResponseV3, DataSource


@pytest.fixture(scope='module')
def resources():
    resources = Resources()
    region_tree = RegionTree('data/geobase_export.json')
    resources.region_tree = region_tree
    catalog = Catalog('data/catalog.json')
    resources.catalog = catalog
    calculator = StatCalculator('data/auto_stats.tsv', resources)
    resources.calculator = calculator
    return resources


def _test_query_v3(resources,
                   expected_level: DataSource,
                   rid: int,
                   mark: str,
                   model: str = None,
                   super_gen: str = None,
                   configuration_id: str = None,
                   tech_param_id: str = None):
    response: StatsMarkResponseV3 = resources.calculator.stat_v3(rid=rid,
                                                                 mark=mark,
                                                                 model=model,
                                                                 super_gen=super_gen,
                                                                 configuration_id=configuration_id,
                                                                 tech_param_id=tech_param_id)
    if expected_level == DataSource.MARK:
        assert isinstance(response, StatsMarkResponseV3)
    else:
        if expected_level in {DataSource.TECH_PARAM, DataSource.COMPLECTATION}:
            expected_conf_level = DataSource.CONFIGURATION
        else:
            expected_conf_level = expected_level
        assert isinstance(response, StatsModelResponseV3)
        response_model = response.stats.model
        if response_model.price:
            assert response_model.price.data_source == expected_level
        if response_model.deprecation:
            assert response_model.deprecation.data_source == expected_level
        if response_model.duration_of_sale:
            assert response_model.duration_of_sale.data_source == expected_level
        if response_model.tech_params:
            assert response_model.tech_params.data_source == expected_conf_level


def _filter_region(rid, calculator: StatCalculator):
    if rid == 10000:
        return np.ones_like(calculator._region_code, dtype=bool)
    return (calculator._region_code == rid) | (calculator._country_code == rid)


def _test_all_models_v3(resources, rid, mark_id):
    mark = resources.catalog.tree.get(mark_id)
    model_id = None
    try:
        _test_query_v3(resources, rid=rid, mark=mark_id, model='N/A', expected_level=DataSource.MODEL)
        for model_id in mark.models:
            _test_query_v3(resources, rid=rid, mark=mark_id, model=model_id, expected_level=DataSource.MODEL)
    except AssertionError:
        print(rid, mark_id, model_id)
        raise


def _test_all_supergen_v3(resources, rid, mark_id):
    mark = resources.catalog.tree.get(mark_id)
    model_id = None
    supergen_id = None
    try:
        model_id = 'N/A'
        supergen_id = 'N/A'
        _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                       super_gen=supergen_id, expected_level=DataSource.MODEL)

        filtered_region = _filter_region(rid, resources.calculator)

        for model_id in mark.models:
            model = mark.models[model_id]
            _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                           super_gen=supergen_id, expected_level=DataSource.MODEL)

            for supergen_id in model.supergens:
                extended_model_id = mark_id + '__' + model_id
                extended_supergen_id = extended_model_id + '__' + supergen_id
                if np.any(filtered_region &
                          (resources.calculator._super_generation ==
                           resources.calculator._maps['super_generation'].get(extended_supergen_id, -1))):
                    expected_level = DataSource.GENERATION
                else:
                    expected_level = DataSource.MODEL

                _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id, super_gen=supergen_id,
                               expected_level=expected_level)
    except:
        print(rid, mark_id, model_id, supergen_id)
        raise


def _test_all_techparams_v3(resources, rid, mark_id, seed, every=4):
    mark = resources.catalog.tree.get(mark_id)
    model_id = None
    supergen_id = None
    configuration_id = None
    techparam_id = None
    seed = seed % every
    try:
        model_id = 'N/A'
        supergen_id = 'N/A'
        configuration_id = '-1'
        techparam_id = '-1'
        _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                       super_gen=supergen_id, configuration_id=configuration_id,
                       tech_param_id=techparam_id, expected_level=DataSource.MODEL)

        filtered_region = _filter_region(rid, resources.calculator)
        ic = 0
        for model_id, model in mark.models.items():
            _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                           super_gen=supergen_id, configuration_id=configuration_id,
                           tech_param_id=techparam_id, expected_level=DataSource.MODEL)

            for supergen_id, supergen in model.supergens.items():
                extended_model_id = mark_id + '__' + model_id
                extended_supergen_id = extended_model_id + '__' + supergen_id
                for configuration_id, configuration in supergen.configurations.items():
                    for techparam_id, techparam in configuration.techparams.items():
                        ic += 1
                        if ic % every != seed:
                            continue  # we check only every third techparam to lighten tests
                        if np.any(filtered_region &
                                  (resources.calculator._tech_param_id == int(techparam_id))):
                            expected_level = DataSource.TECH_PARAM
                        elif np.any(filtered_region &
                                    (resources.calculator._configuration_id == int(configuration_id))):
                            expected_level = DataSource.CONFIGURATION
                        elif np.any(filtered_region &
                                    (resources.calculator._super_generation ==
                                     resources.calculator._maps['super_generation'].get(extended_supergen_id, -1))):
                            expected_level = DataSource.GENERATION
                        else:
                            expected_level = DataSource.MODEL

                        _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id, super_gen=supergen_id,
                                       configuration_id=configuration_id, tech_param_id=techparam_id,
                                       expected_level=expected_level)
    except:
        print(rid, mark_id, model_id, supergen_id, configuration_id, techparam_id)
        raise


def _test_all_marks_v3(resources, rid):
    for mark_id in resources.catalog.tree:
        _test_query_v3(resources, rid=rid, mark=mark_id, expected_level=DataSource.MARK)


def test_all_marks(resources: Resources):
    _test_all_marks_v3(resources, rid=10000)


def test_specific(resources: Resources):
    _test_query_v3(resources, rid=10000, mark='ACURA', model='MDX', super_gen='10382709',
                   configuration_id='20079921', tech_param_id='20388375',
                   expected_level=DataSource.CONFIGURATION)


def test_all_models_rid_earth(resources: Resources):
    for mark_id in resources.catalog.tree:
        _test_all_models_v3(resources, 10000, mark_id)


def test_all_models_rid_russia(resources: Resources):
    for mark_id in resources.catalog.tree:
        _test_all_models_v3(resources, 225, mark_id)


def test_all_models_rid_moscow(resources: Resources):
    for mark_id in resources.catalog.tree:
        _test_all_models_v3(resources, 3, mark_id)


def test_all_models_rid_siberia(resources: Resources):
    for mark_id in resources.catalog.tree:
        _test_all_models_v3(resources, 59, mark_id)


def test_all_supergen_rid_earth(resources: Resources):
    for mark_id in resources.catalog.tree:
        _test_all_supergen_v3(resources, 10000, mark_id)


def test_all_supergen_rid_russia(resources: Resources):
    for mark_id in resources.catalog.tree:
        _test_all_supergen_v3(resources, 225, mark_id)


def test_all_supergen_rid_moscow(resources: Resources):
    for mark_id in resources.catalog.tree:
        _test_all_supergen_v3(resources, 3, mark_id)


def test_all_supergen_rid_siberia(resources: Resources):
    for mark_id in resources.catalog.tree:
        _test_all_supergen_v3(resources, 59, mark_id)


def test_all_techparam_rid_earth(resources: Resources):
    for mark_id in resources.catalog.tree:
        _test_all_techparams_v3(resources, 10000, mark_id, 0)


def test_all_techparam_rid_russia(resources: Resources):
    for mark_id in resources.catalog.tree:
        _test_all_techparams_v3(resources, 225, mark_id, 1)


def test_all_techparam_rid_moscow(resources: Resources):
    for mark_id in resources.catalog.tree:
        _test_all_techparams_v3(resources, 3, mark_id, 2)


def test_all_techparam_rid_siberia(resources: Resources):
    for mark_id in resources.catalog.tree:
        _test_all_techparams_v3(resources, 59, mark_id, 3)
