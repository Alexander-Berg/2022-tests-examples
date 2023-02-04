import pytest
from django.conf import settings

from intranet.search.abovemeta.steps.auth import CLOUD_UID_MIN
from intranet.search.tests.helpers.abovemeta_helpers import request_search


VALID_CLOUD_SERVICE = settings.ISEARCH['tvm']['cloud_applications'][0]


@pytest.mark.gen_test
def test_can_define_org_for_user(http_client, base_url, requester):
    org_id = 123
    requester.patch_blackbox(user_orgs_attr=str(org_id))
    response = yield request_search(http_client, base_url)

    assert response.code == 200
    assert requester.state.user_has_org
    assert requester.state.org_directory_id == org_id


@pytest.mark.gen_test
def test_can_define_org_for_user_multiple(http_client, base_url, requester):
    org_id = 123
    requester.patch_blackbox(user_orgs_attr='789,123,456')
    response = yield request_search(http_client, base_url, params={'org_id': org_id})

    assert response.code == 200
    assert requester.state.user_has_org
    assert requester.state.org_directory_id == org_id


@pytest.mark.gen_test
def test_can_define_org_for_external_admin(http_client, base_url, requester):
    org_id = 123
    requester.patch_blackbox(user_orgs_attr='')
    requester.patch_api_directory_for_external_admin(organizations=[org_id])
    response = yield request_search(http_client, base_url)

    assert response.code == 200
    assert not requester.state.user_has_org
    assert requester.state.org_directory_id == org_id


@pytest.mark.gen_test
def test_can_define_org_for_external_admin_multiple(http_client, base_url, requester):
    org_id = 123
    requester.patch_blackbox(user_orgs_attr='')
    requester.patch_api_directory_for_external_admin(organizations=[456, org_id, 789])
    response = yield request_search(http_client, base_url, params={'org_id': org_id})

    assert response.code == 200
    assert not requester.state.user_has_org
    assert requester.state.org_directory_id == org_id


@pytest.mark.gen_test
def test_can_define_external_admin_if_he_user_in_another_org(http_client, base_url, requester):
    org_id = 123
    requester.patch_blackbox(user_orgs_attr='456')
    requester.patch_api_directory_for_external_admin(organizations=[org_id])
    response = yield request_search(http_client, base_url, params={'org_id': org_id})

    assert response.code == 200
    assert not requester.state.user_has_org
    assert requester.state.org_directory_id == org_id


@pytest.mark.gen_test
def test_forbidden_if_cannot_define_org(http_client, base_url, requester):
    org_id = 123
    requester.patch_blackbox(user_orgs_attr='456')
    requester.patch_api_directory_for_external_admin(organizations=[789])
    response = yield request_search(http_client, base_url, params={'org_id': org_id})

    assert response.code == 403
    assert not requester.state.user_has_org
    assert requester.state.org_directory_id is None


@pytest.mark.gen_test
def test_can_auth_cloud_user_by_uid(http_client, base_url, requester):
    user_uid = str(CLOUD_UID_MIN + 50)
    headers = {'X-UID': user_uid, 'X-Ya-Service-Ticket': 'ticket'}
    tvm_service = VALID_CLOUD_SERVICE

    requester.patch_check_service_ticket(body={'src': tvm_service})
    response = yield request_search(http_client, base_url, headers=headers)

    assert response.code == 200
    assert requester.state.auth_method == 'cloud'
    assert requester.state.user_has_org
    assert requester.state.is_cloud_user
    assert requester.state.user_uid == user_uid


@pytest.mark.gen_test
def test_can_auth_cloud_user_by_cloud_uid(http_client, base_url, requester):
    user_uid = '1234'
    cloud_uid = 'abcd'
    headers = {'X-UID': user_uid, 'X-Cloud-UID': cloud_uid, 'X-Ya-Service-Ticket': 'ticket'}
    tvm_service = VALID_CLOUD_SERVICE

    requester.patch_check_service_ticket(body={'src': tvm_service})
    response = yield request_search(http_client, base_url, headers=headers)

    assert response.code == 200
    assert requester.state.auth_method == 'cloud'
    assert requester.state.user_has_org
    assert requester.state.is_cloud_user
    assert requester.state.user_uid == user_uid
    assert requester.state.cloud_uid == cloud_uid


@pytest.mark.parametrize('uid,service', [
    (str(CLOUD_UID_MIN), 'unknown_service'),  # неразрешённый сервис
    (str(CLOUD_UID_MIN - 1), VALID_CLOUD_SERVICE),  # uid не из диапазона
])
@pytest.mark.gen_test
def test_cloud_auth_forbidden(http_client, base_url, requester, uid, service):
    headers = {'X-UID': uid, 'X-Ya-Service-Ticket': 'ticket'}

    requester.patch_check_service_ticket(body={'src': service})
    response = yield request_search(http_client, base_url, headers=headers)  # noqa

    assert requester.state.auth_method != 'cloud'
