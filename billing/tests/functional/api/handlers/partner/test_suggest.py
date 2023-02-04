import pytest

from hamcrest import assert_that, equal_to

pytestmark = pytest.mark.usefixtures('mock_testing_app_authentication', 'setup_interactions_tvm')


@pytest.mark.asyncio
async def test_testing_response(testing_app):
    r = await testing_app.get('/api/web/v1/partners/suggest', params={'query': 'Яндекс'}, raise_for_status=True)

    expected_items = [
        {
            'inn': '7704340310',
            'region_name': 'Москва',
            'spark_id': 10229326,
            'ogrn': '5157746192731',
            'full_name': 'ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ  ЯНДЕКС.ТАКСИ',
            'name': 'ЯНДЕКС.ТАКСИ, ООО',
            'leader_name': 'Аникин Александр Михайлович',
            'address': '123112, г. Москва, проезд 1-Й Красногвардейский, д. 21 стр. 1 пом. 36.9 этаж 36',
        }
    ]
    assert_that(
        await r.json(),
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {
                    'items': expected_items,
                },
            }
        ),
    )
