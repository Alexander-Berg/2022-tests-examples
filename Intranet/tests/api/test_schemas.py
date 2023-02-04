import pytest

pytestmark = pytest.mark.asyncio


async def test_schemas(client):
    """
    Если упал этот тест - значит в openapi.json сломана отдаваемые схемы
    (Есть две одинаково называющиеся схемы)
    """
    response = await client.get('api/openapi.json')

    assert response.status_code == 200
    data = response.json()
    assert data['components']['schemas']
    for schema in data['components']['schemas']:
        assert not schema.startswith('intranet__trip'), f'{schema} - is not valid name'


async def test_operation_ids(client):
    response = await client.get('api/openapi.json')

    assert response.status_code == 200
    data = response.json()
    assert data['paths']
    operation_ids = sorted(
        endpoint_info['operationId']
        for endpoints in data['paths'].values()
        for endpoint_info in endpoints.values()
    )
    previous_operation_id = None
    duplicated_ids = []
    for operation_id in operation_ids:
        if operation_id == previous_operation_id:
            duplicated_ids.append(operation_id)
        previous_operation_id = operation_id
    assert len(duplicated_ids) == 0, f'{duplicated_ids=}'
