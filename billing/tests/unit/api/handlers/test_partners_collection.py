from dataclasses import asdict
from uuid import UUID

import pytest

from sendr_pytest.helpers import ensure_all_fields
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.create import CreatePartnerAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.list import ListPartnersAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import Contact, Partner, RegistrationData
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.role import Role
from billing.yandex_pay_admin.yandex_pay_admin.tests.utils import check_error, replace_payload


class TestCreatePartner:
    @pytest.fixture
    def payload(self, partner_object):
        return {
            'name': partner_object.name,
            'registration_data': asdict(partner_object.registration_data),
        }

    @pytest.mark.asyncio
    async def test_returned(self, payload, app, mock_action, partner_object, user):
        mock_action(CreatePartnerAction, partner_object)

        r = await app.post('/api/web/v1/partners', json=payload)

        assert_that(r.status, equal_to(200))
        data = await r.json()
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
                        'partner_id': str(partner_object.partner_id),
                        'user_role': 'owner',
                    },
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_calls_action(self, payload, app, mock_action, partner_object, user):
        action_mock = mock_action(CreatePartnerAction, partner_object)

        await app.post('/api/web/v1/partners', json=payload)

        action_mock.assert_run_once_with(
            user=user,
            name=payload['name'],
            registration_data=partner_object.registration_data,
        )

    @pytest.mark.asyncio
    async def test_calls_action__when_optionals_omitted(self, payload, app, mock_action, partner_object, user):
        action_mock = mock_action(CreatePartnerAction, partner_object)
        payload['registration_data'] = {}

        await app.post('/api/web/v1/partners', json=payload)

        action_mock.assert_run_once_with(
            user=user,
            name=payload['name'],
            registration_data=RegistrationData(),
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'full_path, value, expected_error',
        (
            pytest.param('name', None, 'Field may not be null.', id='name_not_null'),
            pytest.param('registration_data', None, 'Field may not be null.', id='registration_data_not_null'),
            pytest.param(
                'registration_data.tax_ref_number', None, 'Field may not be null.', id='tax_ref_number_not_null'
            ),
            pytest.param(
                'registration_data.tax_ref_number', 123, 'Not a valid string.', id='tax_ref_number_string_required'
            ),
            pytest.param('registration_data.contact', None, 'Field may not be null.', id='contact_not_null'),
        ),
    )
    async def test_post_validate_payload(self, app, mock_action, payload, full_path, value, expected_error):
        payload = replace_payload(payload, full_path, value)
        action_mock = mock_action(CreatePartnerAction)

        r = await app.post('/api/web/v1/partners', json=payload)

        await check_error(r, full_path, expected_error)
        action_mock.assert_not_run()


class TestListPartners:
    @pytest.mark.asyncio
    async def test_get(self, app, mock_action, partner_object, user):
        action_mock = mock_action(ListPartnersAction, [partner_object])

        r = await app.get('/api/web/v1/partners')

        assert_that(r.status, equal_to(200))
        data = await r.json()

        assert_that(
            data,
            equal_to(
                {
                    'status': 'success',
                    'code': 200,
                    'data': {
                        'partners': [
                            {
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
                                'partner_id': str(partner_object.partner_id),
                                'user_role': 'owner',
                            }
                        ]
                    },
                }
            ),
        )

        action_mock.assert_run_once_with(user=user, user_role=None)

    @pytest.mark.asyncio
    @pytest.mark.parametrize('user_role', list(RoleType))
    async def test_user_role_passed_to_action(self, app, mock_action, partner_entity, user, user_role):
        action_mock = mock_action(ListPartnersAction, [partner_entity])

        r = await app.get('/api/web/v1/partners', params={'user_role': user_role.value})

        assert_that(r.status, equal_to(200))
        action_mock.assert_run_once_with(user=user, user_role=user_role)


@pytest.fixture
def partner_object(agent) -> Partner:
    return ensure_all_fields(Partner)(
        partner_id=UUID('bfb3ec37-7fcc-4778-919a-9c9538974e0b'),
        name='some partner name',
        registration_data=ensure_all_fields(RegistrationData)(
            contact=ensure_all_fields(Contact)(
                email='email@test',
                phone='+1(000)555-0100',
                first_name='John',
                last_name='Doe',
                middle_name='Татьянович',
            ),
            tax_ref_number='0123 АБ',
            ogrn='ogrn',
            kpp='kpp',
            legal_address='Moscow',
            postal_address='Beverly Hills, 90210',
            postal_code='90210',
            full_company_name='Yandex LLC',
            ceo_name='some ceo name',
        ),
        agent_id=agent.agent_id,
        agent_partner_id='agent_partner_id',
        balance_client_id='balance-client-id',
        balance_person_id='balance-person-id',
        balance_contract_id='balance-contract-id',
        balance_ext_contract_id='balance-ext-contract-id',
        revision=1,
        created=utcnow(),
        updated=utcnow(),
        trial_started_at=utcnow(),
        blocked=False,
        role=Role(
            partner_id=UUID('bfb3ec37-7fcc-4778-919a-9c9538974e0b'),
            uid=7777,
            role=RoleType.OWNER,
        ),
    )
