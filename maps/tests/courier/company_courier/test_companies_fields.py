import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_get, local_patch
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import set_company_import_depot_garage
from ya_courier_backend.resources.logistic_company import WHITELIST_OWN_COMPANY_MAXIMAL_DETAILS


@skip_if_remote
def test_companies_fields(env: Environment):
    path = f"/api/v1/companies/{env.default_company.id}"
    company = local_get(env.client, path, headers=env.user_auth_headers)
    assert set(company.keys()) == set(WHITELIST_OWN_COMPANY_MAXIMAL_DETAILS)
    env.flask_app.schema_validator.validate_object("Company", company)


@skip_if_remote
@pytest.mark.parametrize("import_depot_garage", [True, False])
def test_companies_import_depot_garage_field(env: Environment, import_depot_garage):
    set_company_import_depot_garage(env, env.default_company.id, import_depot_garage)
    path = f"/api/v1/companies/{env.default_company.id}"
    company = local_get(env.client, path, headers=env.user_auth_headers)
    assert company['import_depot_garage'] == import_depot_garage


@skip_if_remote
def test_company_patch_sms_fields(env: Environment):
    path = f"/api/v1/companies/{env.default_company.id}"
    local_patch(env.client, path, headers=env.user_auth_headers,
                data={'enabled_sms_types': ['shift_start'],
                      'sms_time_window': {'start': '07:00:00', 'end': '19:00:00'}})
    company = local_get(env.client, path, headers=env.user_auth_headers)
    assert set(company.keys()) == set(WHITELIST_OWN_COMPANY_MAXIMAL_DETAILS)
    env.flask_app.schema_validator.validate_object("Company", company)
    assert company['enabled_sms_types'] == ['shift_start']
    assert company['sms_time_window'] == {'start': '07:00:00', 'end': '19:00:00'}


@skip_if_remote
def test_company_sms_time_window_end(env: Environment):
    path = f"/api/v1/companies/{env.default_company.id}"
    local_patch(env.client, path, headers=env.user_auth_headers,
                data={'sms_time_window': {'start': '07:00:00', 'end': '24:00:00'}})
    company = local_get(env.client, path, headers=env.user_auth_headers)
    assert set(company.keys()) == set(WHITELIST_OWN_COMPANY_MAXIMAL_DETAILS)
    env.flask_app.schema_validator.validate_object("Company", company)
    assert company['sms_time_window'] == {'start': '07:00:00', 'end': '23:59:59'}


@skip_if_remote
def test_only_unique_custom_fields_are_stored(env: Environment):
    path = f"/api/v1/companies/{env.default_company.id}"
    response = local_patch(env.client, path, headers=env.user_auth_headers,
                           data={'route_custom_fields': ['key', 'key']})
    assert response['route_custom_fields'] == ['key']
