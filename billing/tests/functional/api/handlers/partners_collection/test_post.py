import pytest

from sendr_utils import json_value

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.tests.utils import is_uuid

pytestmark = pytest.mark.usefixtures('mock_app_authentication', 'setup_interactions_tvm')


@pytest.fixture
def create_partner(app, partner_entity):
    async def _inner():
        r = await app.post('/api/web/v1/partners', json=json_value(partner_entity))
        assert_that(r.status, equal_to(200))
        return await r.json()

    return _inner


@pytest.mark.asyncio
async def test_returned(create_partner):
    data = await create_partner()

    assert_that(
        data,
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {
                    'name': 'some partner name',
                    'registration_data': {
                        'contact': {
                            'email': 'email@test',
                            'phone': '+1(000)555-0100',
                            'first_name': 'John',
                            'last_name': 'Doe',
                            'middle_name': 'Татьянович',
                        },
                        'tax_ref_number': '0123 АБ',
                        'ogrn': 'ogrn',
                        'kpp': 'kpp',
                        'legal_address': 'Moscow',
                        'postal_address': 'Beverly Hills, 90210',
                        'postal_code': '90210',
                        'full_company_name': 'Yandex LLC',
                        'ceo_name': 'some ceo name',
                    },
                    'partner_id': is_uuid(),
                    'user_role': 'owner',
                },
            }
        ),
    )
