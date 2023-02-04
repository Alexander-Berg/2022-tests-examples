import re
from uuid import UUID

import pytest

from sendr_utils import alist

from hamcrest import (
    assert_that,
    contains_inanyorder,
    equal_to,
    has_entries,
    has_length,
    has_properties,
    has_property,
    match_equality,
)

from billing.yandex_pay_admin.yandex_pay_admin.api.schemas.cms.merchant import CreateMerchantForCMSRequestSchema
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.ticket import CreateModerationTicketAction
from billing.yandex_pay_admin.yandex_pay_admin.core.entities.enums import PayBackendType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import TaskType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import Contact

pytestmark = pytest.mark.usefixtures('mock_app_authentication', 'setup_interactions_tvm')


@pytest.mark.asyncio
async def test_success(
    app,
    params,
    storage,
):
    response = await app.post('/api/web/v1/cms/merchants', json=params)

    data = await response.json()
    assert_that(response.status, equal_to(200))
    assert_that(
        data,
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {
                    'merchant': match_equality(
                        has_entries(
                            {
                                'name': 'partner',
                                'origins': [
                                    match_equality(has_entries({'origin': 'https://origin.test'})),
                                ],
                                'callback_url': 'https://acme.tld',
                                'delivery_integration_params': {
                                    'yandex_delivery': None,
                                    'measurements': None,
                                },
                            }
                        )
                    )
                },
            }
        ),
    )

    origin_id = UUID(data['data']['merchant']['origins'][0]['origin_id'])

    origin_moderation = await storage.origin_moderation.find_by_origin_id(origin_id)
    origin_moderation_id = str(origin_moderation[0].origin_moderation_id)
    filters = {
        'action_name': CreateModerationTicketAction.action_name,
        'params': lambda field: field['action_kwargs']['origin_moderation_id'].astext == origin_moderation_id,
    }
    tasks = await alist(storage.task.find(filters=filters))
    assert_that(tasks, has_length(1))
    task = tasks[0]

    expected_ticket_task_params = {
        'max_retries': 10,
        'action_kwargs': {
            'origin_moderation_id': origin_moderation_id,
        },
    }

    assert_that(
        task,
        has_properties(
            params=expected_ticket_task_params,
            task_type=TaskType.RUN_ACTION,
            action_name=CreateModerationTicketAction.action_name,
        ),
    )


@pytest.mark.asyncio
async def test_reuses_merchant_if_already_exists(
    app,
    params,
    mock_startrek,
):
    params['origins'] = [{'origin': 'https://a.test'}]
    await app.post('/api/web/v1/cms/merchants', json=params, raise_for_status=True)

    params['origins'] = [{'origin': 'https://b.test'}]
    response = await app.post('/api/web/v1/cms/merchants', json=params, raise_for_status=True)
    data = await response.json()

    assert_that(
        data,
        has_entries(
            data=has_entries(
                merchant=has_entries(
                    origins=contains_inanyorder(
                        has_entries(
                            origin='https://a.test',
                        ),
                        has_entries(
                            origin='https://b.test',
                        ),
                    ),
                ),
            ),
        ),
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'key,expected_default',
    [
        ('contact', Contact()),
        ('tax_ref_number', None),
        ('ogrn', None),
        ('legal_address', None),
        ('postal_address', None),
        ('full_company_name', None),
        ('ceo_name', None),
    ],
)
async def test_update_registration_data(storage, app, params, key, expected_default):
    partner_registration_data = params['partner_registration_data']
    value = partner_registration_data.pop(key)

    response = await app.post('/api/web/v1/cms/merchants', json=params, raise_for_status=True)

    data = await response.json()
    partner_id = UUID(data['data']['merchant']['partner_id'])
    partner = await storage.partner.get(partner_id)
    assert_that(partner, has_property('registration_data', has_property(key, expected_default)))

    partner_registration_data[key] = value
    expected_entity = getattr(
        CreateMerchantForCMSRequestSchema().load(params).data['partner_registration_data'],
        key,
    )
    await app.post('/api/web/v1/cms/merchants', json=params, raise_for_status=True)
    partner = await storage.partner.get(partner_id)
    assert_that(partner, has_property('registration_data', has_property(key, expected_entity)))


@pytest.fixture
def params():
    return {
        'partner_name': 'partner',
        'origins': [{'origin': 'https://origin.test'}],
        'partner_registration_data': {
            'contact': {
                'email': 'email@test',
                'phone': '+1(234)567890',
                'first_name': 'first-name',
                'last_name': 'last-name',
                'middle_name': 'middle-name',
            },
            'tax_ref_number': '100000',
            'ogrn': '200000',
            'legal_address': 'legal address',
            'postal_address': 'postal address',
            'full_company_name': 'Acme Ltd',
            'ceo_name': 'John Doe',
        },
        'callback_url': 'https://acme.tld',
        'delivery_integration_params': {
            'yandex_delivery': {
                'oauth_token': 'token',
                'autoaccept': True,
            },
            'measurements': None,
        },
    }


@pytest.fixture(autouse=True)
def mock_pay_backends(mock_pay_backend_put_merchant, mock_pay_plus_backend_put_merchant):
    mock_pay_backend_put_merchant(PayBackendType.PRODUCTION)
    mock_pay_backend_put_merchant(PayBackendType.SANDBOX)
    mock_pay_plus_backend_put_merchant(PayBackendType.PRODUCTION)
    mock_pay_plus_backend_put_merchant(PayBackendType.SANDBOX)


@pytest.fixture(autouse=True)
def mock_startrek(aioresponses_mocker, yandex_pay_admin_settings):
    return aioresponses_mocker.post(
        re.compile(f'^{yandex_pay_admin_settings.STARTREK_API_URL}/v2/issues.*$'),
        payload={
            'id': '123',
            'key': '_TESTISSUE-123',
            'status': {
                'id': 'id',
                'key': 'key',
            },
        },
        repeat=True,
    )
