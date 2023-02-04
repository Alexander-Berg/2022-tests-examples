import pytest
from maps.b2bgeo.identity.libs.payloads.py import (
    User,
    Company,
    UserCompany,
    UserRole,
    buildIdentityPayload,
)

STR_TO_ROLE = {
    'admin': UserRole.admin,
    'manager': UserRole.manager,
    'dispatcher': UserRole.dispatcher,
    'app': UserRole.app,
}


@pytest.mark.parametrize(
    "role",
    [
        UserRole.admin,
        UserRole.manager,
        UserRole.dispatcher,
        UserRole.app,
    ],
)
def test_user_role_binding(role):
    company = UserCompany(
        id=1,
        apikey='ab787a96-f434-49a2-87c4-49978132bb38',
        role=role,
    )
    assert company.role == role
    assert UserRole.manager != UserRole.dispatcher


def test_build_user_paylad():
    payload = {
        'subject': {
            'type': 'user',
            'value': {
                'id': 1,
                'is_super': True,
                'companies': [
                    {
                        'id': 123456,
                        'apikey': 'e4dd5e11-5b2d-4cb1-9de3-77ac55b164e8',
                        'role': 'manager',
                    },
                    {
                        'id': 123457,
                        'apikey': 'a7662f40-63f3-434a-8862-980a4121961a',
                        'role': 'manager',
                    },
                    {
                        'id': 123458,
                        'apikey': None,
                        'role': 'app',
                    },
                ],
            },
        },
    }

    user_payload = buildIdentityPayload(payload)
    assert type(user_payload) is User

    value = payload['subject']['value']

    assert user_payload.id == value['id']
    assert user_payload.is_super == value['is_super']
    assert len(user_payload.companies) == len(value['companies'])
    for company, value_company in zip(user_payload.companies, value['companies']):
        assert company.id == value_company['id']
        assert company.apikey == value_company['apikey']
        assert company.role == STR_TO_ROLE[value_company['role']]


def test_build_company_paylad():
    payload = {
        'subject': {
            'type': 'company',
            'value': {
                'id': 123456,
                'apikey': '3df96665-ae75-4077-b192-cc7cbf4d2513',
            },
        },
    }

    value = payload['subject']['value']

    company_payload = buildIdentityPayload(payload)
    assert type(company_payload) is Company

    assert company_payload.id == value['id']
    assert company_payload.apikey == value['apikey']
