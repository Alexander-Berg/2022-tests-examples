import json

import pytest  # noqa

import aiohttp
from aiohttp.client_reqrep import ClientResponse
from google.protobuf.json_format import MessageToDict

import ads.emily.viewer.backend.app.schema.api as api_schema
import ads.emily.viewer.backend.app.schema.proto.responses_pb2 as proto_schema
from ads.emily.viewer.backend.app import api

pytestmark = [pytest.mark.asyncio]


class ResponseTypeError(Exception):
    """ Base response type exception """


async def get_route(route, status_code, client) -> ClientResponse:
    response = await client.get(route)

    assert response.status == status_code, f"GET: {route}: status {response.status}, expeceted {status_code}\nMessage: {await response.json()}"
    return response


async def validate_route(route, schema, status_code, client) -> dict:
    try:
        response = await get_route(route, status_code, client)
        if schema is None:
            return {}
        if isinstance(schema, api_schema.msh.Schema):
            resp_json = await response.json()
            errors = schema.validate(resp_json)
            assert not errors, f"GET: {route}:\n{json.dumps(errors, sort_keys=True, indent=3)}\n{resp_json}"
            return resp_json
        resp_proto = await response.read()
        schema.ParseFromString(resp_proto)
        return MessageToDict(schema, preserving_proto_field_name=True)
    except aiohttp.client_exceptions.ContentTypeError as err:
        raise ResponseTypeError(f"GET: {route} not json serializable") from err
    raise ResponseTypeError(f"GET: {route} uncaught exception")


@pytest.mark.parametrize(
    ("route, schema, status_code"), [
        ("/api/v1/models", api_schema.ModelMapResponseSchema(), 200),
        ("/api/v1/models/meta", api_schema.ModelMetaResponseSchema(), 200),
        ("/api/v1/models/meta/metrics", api_schema.ModelMetaMetricsResponseSchema(), 200),
    ],
)
async def test_get_models(route, schema, status_code, client):
    await validate_route(route, schema, status_code, client)


@pytest.mark.parametrize(
    ("route, schema, status_code"), [
        ("/api/v1/models/mx/list", api_schema.MatrixNetMapResponseSchema(), 200),
    ],
)
async def test_get_matrixnets(route, schema, status_code, client):
    mx_response = await validate_route(route, schema, status_code, client)
    for mx_id in mx_response["data"]:
        mx = await validate_route(f"/api/v1/models/mx?matrixnet_id={mx_id}", api_schema.MatrixNetResponseSchema(), 200, client)
        for date in mx["data"]["dates"]:
            t = await (await get_route(f"/api/v1/models/mx?matrixnet_dump_name={mx['data']['name']}/{date}", 200, client)).json()
            assert t["meta"]["matrixnet_id"] == mx_id, f"Getting matrixnet by {mx['data']['name']}/{date}: got {t['meta']['matrixnet_id']}, expected {mx_id}"


@pytest.mark.parametrize(
    ("route, schema, status_code"), [
        ("/api/v1/models/lm/list", api_schema.LinearModelMapResponseSchema(), 200),
    ],
)
async def test_get_linear_models(route, schema, status_code, client):
    dump_id_map = await api.responses.storage.get_dump_ids()
    lm_response = await validate_route(route, schema, status_code, client)
    for lm_id in lm_response["data"]:
        lm = await validate_route(f"/api/v1/models/lm?linear_model_id={lm_id}", api_schema.LinearModelResponseSchema(), 200, client)
        await validate_route(f"/api/v1/models/lm/{lm_id}/matrixnets", api_schema.LinearModelMatrixnetsResponseSchema(), 200, client)
        for date in lm["data"]["dates"]:
            linear_model_id = dump_id_map.data["lm"].get(f"{lm['data']['name']}/{date}")
            assert linear_model_id == lm_id, f"Getting linear_model by {lm['data']['name']}/{date}: got {linear_model_id}, expected {lm_id}"


@pytest.mark.parametrize(
    ("route, schema, status_code"), [
        ("/api/v1/models/counters", api_schema.CounterMapResponseSchema(), 200),
    ],
)
async def test_get_counters(route, schema, status_code, client):
    c_response = await validate_route(route, schema, status_code, client)
    for c_id in c_response["data"]:
        await validate_route(f"{route}/{c_id}", api_schema.CounterResponseSchema(), 200, client)


@pytest.mark.parametrize(
    ("route, schema, status_code"), [
        ("/api/v1/tables/lm_config", api_schema.TableLinearModelConfigResponseSchema(), 200),
    ],
)
async def test_get_lm_config_table(route, schema, status_code, client):
    await validate_route(route, schema, status_code, client)


@pytest.mark.parametrize(
    ("route, schema, status_code"), [
        ("/api/v2/experiments/filters", api_schema.ExperimentsFiltersResponseSchema(), 200),
        ("/api/v2/experiments/filters?format=proto", proto_schema.ExperimentsFiltersResponse(), 200),
        ("/api/v2/experiments/bigb", api_schema.ExperimentsResponseSchema(), 200),
        ("/api/v2/experiments/bsserver", api_schema.ExperimentsResponseSchema(), 200),
        ("/api/v2/experiments/bsserver/search", api_schema.ExperimentsResponseSchema(), 200),
        ("/api/v2/experiments/bsserver/rsya", api_schema.ExperimentsResponseSchema(), 200),
        ("/api/v2/experiments/bigb?format=proto", proto_schema.ExperimentsResponse(), 200),
        ("/api/v2/experiments/bsserver?format=proto", proto_schema.ExperimentsResponse(), 200),
        ("/api/v2/experiments/bsserver/search?format=proto", proto_schema.ExperimentsResponse(), 200),
        ("/api/v2/experiments/bsserver/rsya?format=proto", proto_schema.ExperimentsResponse(), 200),
    ],
)
async def test_get_experiments(route, schema, status_code, client):
    await validate_route(route, schema, status_code, client)


@pytest.mark.parametrize(
    ("route, schema, status_code"), [
        ("/api/v2/models/filters", api_schema.ModelsFiltersResponseSchema(), 200),
        ("/api/v2/models/filters?format=proto", proto_schema.ModelsFiltersResponse(), 200),
        ("/api/v2/models/list", api_schema.ModelsListResponseSchema(), 200),
        ("/api/v2/models/list?format=proto", proto_schema.ModelsListResponse(), 200),
    ],
)
async def test_get_models_v2(route, schema, status_code, client):
    model_list_response = await validate_route(route, schema, status_code, client)
    if route == "/api/v2/models/list":
        for i, model_info in enumerate(model_list_response["data"]):
            if i == 50:  # TODO: @dim-gonch - too slow
                break
            name = model_info["name"]
            await validate_route(f"/api/v2/models/model/{name}", api_schema.ModelResponseSchema(), 200, client)


@pytest.mark.parametrize(
    ("route, schema, status_code"), [
        ("/api/v2/factors", api_schema.FactorsResponseSchema(), 200),
        ("/api/v2/factors?format=proto", proto_schema.FactorsResponse(), 200),
    ],
)
async def test_get_factors(route, schema, status_code, client):
    await validate_route(route, schema, status_code, client)

# @pytest.mark.parametrize(
#     ('route, schema, status_code'), [
#         ('/api/v1/sandbox/models', api_schema.SandboxResourceListResponseSchema(), 200),
#     ]
# )
# async def test_get_sandbox_models(route, schema, status_code, client):
#     await validate_route(route, schema, status_code, client)
