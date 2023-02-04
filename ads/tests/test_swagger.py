import json

import pytest  # noqa
from deepdiff import DeepDiff

from ads.emily.viewer.backend.app.config import API_VERSION

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def swagger_json(client) -> dict:
    raw_swagger_spec = await client.get('/api/docs/swagger.json')
    return await raw_swagger_spec.json()


async def test_swagger_has_unique_operationid(swagger_json):
    operation_ids = {}
    for route, path in swagger_json['paths'].items():
        for method, request in path.items():
            operationId = request.get('operationId')
            assert operationId is not None, f"No operationId found for {method.upper()}: {route}"
            assert operationId not in operation_ids, f"Not unique operationId, duplicate routes: {operation_ids[operationId], route}"
            operation_ids[operationId] = route


async def test_swagger_has_valid_version(swagger_json):
    assert swagger_json['info']['version'] == API_VERSION


async def test_swagger_equal_client_swagger(swagger_json, swagger_json_disk):
    diff = DeepDiff(swagger_json_disk, swagger_json, ignore_order=True)
    if diff:
        # pylint: disable=no-member
        jdiff = json.loads(diff.to_json())
        raise AssertionError(f"Swagger differ from client, please generate new API version: make generate-api\n{json.dumps(jdiff, indent=3, ensure_ascii=False)}")
