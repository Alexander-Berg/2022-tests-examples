from typing import Union, Optional

import sys

sys.path.append("schema")
sys.path.append("../schema")

import pytest
from rest_api import Resources
from rest_api.calculator import StatCalculator
from vsml_common.proto import ProtoSerializable
from rest_api.region_api import RegionTree
from rest_api.catalog import Catalog

import schema.auto.api.response_model_pb2 as proto_response
import schema.auto.api.stats_model_pb2 as proto

from rest_api.model import ResponseStatus, StatsMarkResponseV3, StatsModelResponseV3, StatsMark, StatsMarkModel, \
    ModelPhotos, Price, DeprecatedPrice, AgeWithPrice, StatsModel, Model, TechParams, MostPopularTechParam, \
    DurationOfSale, DataSource

datasource_mapping = {1: DataSource.MARK,
                      2: DataSource.MODEL,
                      3: DataSource.GENERATION,
                      4: DataSource.CONFIGURATION,
                      5: DataSource.TECH_PARAM,
                      6: DataSource.COMPLECTATION}

response_status_mapping = {
    0: ResponseStatus.SUCCESS,
    1: ResponseStatus.ERROR
}


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


def _parse_mark(response: proto.MarkStats,
                status: proto_response.ResponseStatus) -> StatsMarkResponseV3:
    models = []
    for p_model in response.models:
        p_photo = p_model.photo
        photo = None
        if p_photo.ByteSize():
            photo = ModelPhotos(sizes=ModelPhotos.ModelPhotosSizes(
                cattouch=p_photo.sizes['cattouch'],
                cattouchret=p_photo.sizes['cattouchret']
            ))

        price = _parse_price(p_model.price)
        deprecation = _parse_deprecation(p_model.deprecation)

        model = StatsMarkModel(
            model=p_model.model,
            photo=photo,
            year_from=p_model.year_from,
            year_to=p_model.year_to,
            price=price,
            deprecation=deprecation
        )
        models.append(model)

    return StatsMarkResponseV3(status=response_status_mapping[status],
                               stats=StatsMark(
                                   mark=StatsMark.StatsMarkInner(
                                       models=models
                                   )
                               ))


def _parse_price(p_price: proto.PriceBlock) -> Optional[Price]:
    if p_price.ByteSize():
        return Price(
            data_source=datasource_mapping[p_price.data_source],
            min_price=p_price.min_price,
            max_price=p_price.max_price,
            average_price=p_price.average_price,
            offers_count=p_price.offers_count
        )


def _parse_deprecation(p_deprecation: proto.DeprecationPriceBlock) -> Optional[DeprecatedPrice]:
    if p_deprecation.ByteSize():
        price_percentage_diff = []
        for ic, age_diff in enumerate(p_deprecation.price_percentage_diff):
            diff = age_diff.price_percentage_diff
            if ic == 0 and diff == 0:
                diff = None
            price_percentage_diff.append(AgeWithPrice(
                age=age_diff.age,
                price=age_diff.price,
                price_percentage_diff=diff
            ))
        return DeprecatedPrice(
            data_source=datasource_mapping[p_deprecation.data_source],
            avg_in_percentage=p_deprecation.avg_in_percentage,
            price_percentage_diff=price_percentage_diff  # or None
        )


def _parse_techparam(p_tech_params: proto.TechParamsBlock) -> Optional[TechParams]:
    if p_tech_params.ByteSize():
        p_most_popular = p_tech_params.most_popular_tech_param
        most_popular_tech_param = None
        if p_most_popular.ByteSize():
            most_popular_tech_param = MostPopularTechParam(
                mark=p_most_popular.mark,
                mark_name=p_most_popular.mark_name,
                model=p_most_popular.model,
                model_name=p_most_popular.model_name,
                super_gen_id=str(p_most_popular.super_gen_id),
                super_gen_name=p_most_popular.super_gen_name,
                configuration_id=str(p_most_popular.configuration_id),
                tech_param_id=str(p_most_popular.tech_param_id),
                body_type=p_most_popular.body_type,
                engine_type=p_most_popular.engine_type,
                displacement=p_most_popular.displacement,
                transmission=p_most_popular.transmission,
                horse_power=p_most_popular.horse_power
            )

        displacement_segments = p_tech_params.displacement_segments
        if displacement_segments:
            displacement_segments = {str(k): v for k, v in displacement_segments.items()}
        return TechParams(
            data_source=datasource_mapping[p_tech_params.data_source],
            most_popular_tech_param=most_popular_tech_param,
            displacement_segments=displacement_segments,
            engine_type_segments=p_tech_params.engine_type_segments,
            transmission_segments=p_tech_params.transmission_segments,
            gear_type_segments=p_tech_params.gear_type_segments
        )


def _parse_duration(p_duration: proto.DurationOfSaleBlock) -> Optional[DurationOfSale]:
    if p_duration.ByteSize():
        return DurationOfSale(
            data_source=datasource_mapping[p_duration.data_source],
            avg=p_duration.avg,
            vas=p_duration.vas,
            cert=p_duration.cert
        )


def _parse_model(response: proto.ModelStats,
                 status: proto_response.ResponseStatus) -> StatsModelResponseV3:
    price = _parse_price(response.price)
    deprecation = _parse_deprecation(response.deprecation)
    tech_params = _parse_techparam(response.tech_params)
    duration_of_sale = _parse_duration(response.duration_of_sale)
    model = Model(
        price=price,
        deprecation=deprecation,
        tech_params=tech_params,
        duration_of_sale=duration_of_sale
    )

    return StatsModelResponseV3(status=response_status_mapping[status],
                                stats=StatsModel(
                                    model=model
                                ))


def parse_response_from_protobuf(
        response: proto_response.StatsSummaryResponse
) -> Optional[Union[StatsMarkResponseV3, StatsModelResponseV3]]:
    mark = response.stats.mark
    model = response.stats.model
    status = response.status
    if response.stats.WhichOneof('data') == 'mark':
        return _parse_mark(mark, status)
    elif response.stats.WhichOneof('data') == 'model':
        return _parse_model(model, status)


def serde(response: ProtoSerializable):
    serialized = response.to_proto().SerializeToString()
    proto_deserialized = proto_response.StatsSummaryResponse()
    proto_deserialized.ParseFromString(serialized)
    return parse_response_from_protobuf(proto_deserialized)


def _test_query_v3(resources,
                   rid: int,
                   mark: str,
                   model: str = None,
                   super_gen: str = None,
                   configuration_id: str = None,
                   tech_param_id: str = None):
    expected_response = resources.calculator.stat_v3(rid=rid,
                                                     mark=mark,
                                                     model=model,
                                                     super_gen=super_gen,
                                                     configuration_id=configuration_id,
                                                     tech_param_id=tech_param_id)

    deserialized_response = serde(expected_response)
    assert deserialized_response == expected_response


def _test_all_models_v3(resources, rid, mark):
    mark = resources.catalog.tree.get(mark)
    if not mark:
        return True
    for model_id in mark.models:
        _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id)

    # these tests does not match catalog tree, thus should accept properly only mark
    # (all other params are just silently dropped)
    _test_query_v3(resources, rid=rid, mark=mark.mark, model='does-not-exist')
    _test_query_v3(resources, rid=rid, mark=mark.mark, model='3')
    _test_query_v3(resources, rid=rid, mark=mark.mark, model='3',
                   super_gen='7754738')
    _test_query_v3(resources, rid=rid, mark=mark.mark, model='3',
                   super_gen='7754738', configuration_id='7754744')
    _test_query_v3(resources, rid=rid, mark=mark.mark, model='3',
                   super_gen='7754738', configuration_id='7754744', tech_param_id='7754757')


def _test_all_supergen_v3(resources, rid, mark):
    mark = resources.catalog.tree.get(mark)
    if not mark:
        return True
    for model_id in mark.models:
        model = mark.models[model_id]
        _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                       super_gen='does-not-exist')
        _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                       super_gen='7754738')
        _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                       super_gen='7754738', configuration_id='7754744')
        _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                       super_gen='7754738', configuration_id='7754744', tech_param_id='7754757')

        for supergen_id in model.supergens:
            _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id, super_gen=supergen_id)


def _test_all_configuration_v3(resources, rid, mark):
    mark = resources.catalog.tree.get(mark)
    if not mark:
        return True
    for model_id, model in mark.models.items():
        for supergen_id, supergen in model.supergens.items():
            _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                           super_gen=supergen_id, configuration_id='does-not-exist')
            _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                           super_gen=supergen_id, configuration_id='7754744')
            _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                           super_gen=supergen_id, configuration_id='7754744', tech_param_id='7754757')
            for ic, (configuration_id, configuration) in enumerate(supergen.configurations.items()):
                if ic % 2 == 1: continue
                _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                               super_gen=supergen_id, configuration_id=configuration_id)


def _test_all_techparam_v3(resources, rid, mark):
    mark = resources.catalog.tree.get(mark)
    if not mark:
        return True
    for model_id, model in mark.models.items():
        for supergen_id, supergen in model.supergens.items():
            for ic, (configuration_id, configuration) in enumerate(supergen.configurations.items()):
                if ic % 2 == 0: continue
                _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                               super_gen=supergen_id, configuration_id=configuration_id, tech_param_id='does-not-exist')
                _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                               super_gen=supergen_id, configuration_id=configuration_id, tech_param_id='7754757')
                for jc, (tech_param_id, tech_param) in enumerate(configuration.techparams.items()):
                    if jc % 2 == 0: continue
                    _test_query_v3(resources, rid=rid, mark=mark.mark, model=model_id,
                                   super_gen=supergen_id, configuration_id=configuration_id,
                                   tech_param_id=tech_param_id)


def test_mark_vaz_whole_earth(resources: Resources):
    _test_query_v3(resources, rid=10000, mark='VAZ')


def test_mark_vaz_spb(resources: Resources):
    _test_query_v3(resources, rid=2, mark='VAZ')


def test_mark_vaz_small_city(resources: Resources):
    _test_query_v3(resources, rid=121713, mark='VAZ')


def test_mark_bmw_whole_earth(resources: Resources):
    _test_query_v3(resources, rid=10000, mark='BMW')


def test_mark_bmw_moscow(resources: Resources):
    _test_query_v3(resources, rid=1, mark='BMW')


def test_mark_bmw_small_city(resources: Resources):
    _test_query_v3(resources, rid=159972, mark='BMW')


def test_mark_ac_whole_earth(resources: Resources):
    _test_query_v3(resources, rid=10000, mark='AC')


def test_mark_ac_moscow(resources: Resources):
    _test_query_v3(resources, rid=1, mark='AC')


def test_mark_ac_small_city(resources: Resources):
    _test_query_v3(resources, rid=159972, mark='AC')


def test_mark_unknown_mark_whole_earth(resources: Resources):
    _test_query_v3(resources, rid=10000, mark='BLABLABLA')


def test_mark_opel_unknown_region(resources: Resources):
    _test_query_v3(resources, rid=123321123, mark='OPEL')


def test_model_vaz_whole_earth(resources: Resources):
    _test_all_models_v3(resources, rid=10000, mark='VAZ')


def test_model_vaz_spb(resources: Resources):
    _test_all_models_v3(resources, rid=2, mark='VAZ')


def test_model_vaz_small_city(resources: Resources):
    _test_all_models_v3(resources, rid=121713, mark='VAZ')


def test_model_bmw_whole_earth(resources: Resources):
    _test_all_models_v3(resources, rid=10000, mark='BMW')


def test_model_bmw_moscow(resources: Resources):
    _test_all_models_v3(resources, rid=1, mark='BMW')


def test_model_bmw_small_city(resources: Resources):
    _test_all_models_v3(resources, rid=159972, mark='BMW')


def test_model_ac_whole_earth(resources: Resources):
    _test_all_models_v3(resources, rid=10000, mark='AC')


def test_model_ac_moscow(resources: Resources):
    _test_all_models_v3(resources, rid=1, mark='AC')


def test_model_ac_small_city(resources: Resources):
    _test_all_models_v3(resources, rid=159972, mark='AC')


def test_model_unknown_mark_whole_earth(resources: Resources):
    _test_all_models_v3(resources, rid=10000, mark='BLABLABLA')


def test_model_opel_unknown_region(resources: Resources):
    _test_all_models_v3(resources, rid=123321123, mark='OPEL')


####################
# SUPERGEN
####################

def test_supergen_vaz_whole_earth(resources: Resources):
    _test_all_supergen_v3(resources, rid=10000, mark='VAZ')


def test_supergen_vaz_spb(resources: Resources):
    _test_all_supergen_v3(resources, rid=2, mark='VAZ')


def test_supergen_vaz_small_city(resources: Resources):
    _test_all_supergen_v3(resources, rid=121713, mark='VAZ')


def test_supergen_bmw_whole_earth(resources: Resources):
    _test_all_supergen_v3(resources, rid=10000, mark='BMW')


def test_supergen_bmw_moscow(resources: Resources):
    _test_all_supergen_v3(resources, rid=1, mark='BMW')


def test_supergen_bmw_small_city(resources: Resources):
    _test_all_supergen_v3(resources, rid=159972, mark='BMW')


def test_supergen_ac_whole_earth(resources: Resources):
    _test_all_supergen_v3(resources, rid=10000, mark='AC')


def test_supergen_ac_moscow(resources: Resources):
    _test_all_supergen_v3(resources, rid=1, mark='AC')


def test_supergen_ac_small_city(resources: Resources):
    _test_all_supergen_v3(resources, rid=159972, mark='AC')


def test_supergen_unknown_mark_whole_earth(resources: Resources):
    _test_all_supergen_v3(resources, rid=10000, mark='BLABLABLA')


def test_supergen_opel_unknown_region(resources: Resources):
    _test_all_supergen_v3(resources, rid=123321123, mark='OPEL')


####################
# configuration
####################

def test_configuration_vaz_whole_earth(resources: Resources):
    _test_all_configuration_v3(resources, rid=10000, mark='VAZ')


def test_configuration_vaz_spb(resources: Resources):
    _test_all_configuration_v3(resources, rid=2, mark='VAZ')


def test_configuration_vaz_small_city(resources: Resources):
    _test_all_configuration_v3(resources, rid=121713, mark='VAZ')


def test_configuration_bmw_whole_earth(resources: Resources):
    _test_all_configuration_v3(resources, rid=10000, mark='BMW')


def test_configuration_bmw_moscow(resources: Resources):
    _test_all_configuration_v3(resources, rid=1, mark='BMW')


def test_configuration_bmw_small_city(resources: Resources):
    _test_all_configuration_v3(resources, rid=159972, mark='BMW')


def test_configuration_ac_whole_earth(resources: Resources):
    _test_all_configuration_v3(resources, rid=10000, mark='AC')


def test_configuration_ac_moscow(resources: Resources):
    _test_all_configuration_v3(resources, rid=1, mark='AC')


def test_configuration_ac_small_city(resources: Resources):
    _test_all_configuration_v3(resources, rid=159972, mark='AC')


def test_configuration_unknown_mark_whole_earth(resources: Resources):
    _test_all_configuration_v3(resources, rid=10000, mark='BLABLABLA')


def test_configuration_opel_unknown_region(resources: Resources):
    _test_all_configuration_v3(resources, rid=123321123, mark='OPEL')


####################
# techparam
####################

def test_techparam_vaz_whole_earth(resources: Resources):
    _test_all_techparam_v3(resources, rid=10000, mark='VAZ')


def test_techparam_vaz_spb(resources: Resources):
    _test_all_techparam_v3(resources, rid=2, mark='VAZ')


def test_techparam_vaz_small_city(resources: Resources):
    _test_all_techparam_v3(resources, rid=121713, mark='VAZ')


def test_techparam_bmw_whole_earth(resources: Resources):
    _test_all_techparam_v3(resources, rid=10000, mark='BMW')


def test_techparam_bmw_moscow(resources: Resources):
    _test_all_techparam_v3(resources, rid=1, mark='BMW')


def test_techparam_bmw_small_city(resources: Resources):
    _test_all_techparam_v3(resources, rid=159972, mark='BMW')


def test_techparam_ac_whole_earth(resources: Resources):
    _test_all_techparam_v3(resources, rid=10000, mark='AC')


def test_techparam_ac_moscow(resources: Resources):
    _test_all_techparam_v3(resources, rid=1, mark='AC')


def test_techparam_ac_small_city(resources: Resources):
    _test_all_techparam_v3(resources, rid=159972, mark='AC')


def test_techparam_unknown_mark_whole_earth(resources: Resources):
    _test_all_techparam_v3(resources, rid=10000, mark='BLABLABLA')


def test_techparam_opel_unknown_region(resources: Resources):
    _test_all_techparam_v3(resources, rid=123321123, mark='OPEL')
