from django.core.urlresolvers import reverse


def test_search_by_full_match_success(db, client, process, control_plan, control_plan_two):
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'search_by': process.name},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['comment'] == control_plan.comment
    assert response_json[0]['process_data'][0]['id'] == process.id
    assert response.status_code == 200


def test_search_by_partial_match_success(db, client, control_plan, control_plan_two):
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'search_by': 'process_'},)
    response_json = response.json()['results']
    assert len(response_json) == 2
    assert response.status_code == 200


def test_search_by_case_insensitive_success(db, client, control_plan, control_plan_two):
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'search_by': 'PROCESS_'},)
    response_json = response.json()['results']
    assert len(response_json) == 2
    assert response.status_code == 200


def test_search_by_with_whitespace_success(db, client, risk, control_plan, control_plan_two):
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'search_by': risk.number},)
    response_json = response.json()['results']
    assert len(response_json) == 2
    assert response.status_code == 200


def test_search_by_not_match_success(db, client, control_plan, control_plan_two):
    url = reverse("api_v1:controlplan")
    response = client.get(url, {'search_by': 'testtesttest'},)
    response_json = response.json()['results']
    assert len(response_json) == 0
    assert response.status_code == 200


def test_controltest_search_by_success(db, client, control_test, control_step):
    url = reverse("api_v1:controltest")
    response = client.get(url, {'search_by': control_step.step},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == control_test.id
    assert response_json[0]['controlstep_set_data'][0]['id'] == control_step.id
    assert response.status_code == 200


def test_account_search_by_success(db, client, account, ):
    url = reverse("api_v1:account")
    response = client.get(url, {'search_by': account.name},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == account.id
    assert response.status_code == 200


def test_assertion_search_by_success(db, client, assertion, ):
    url = reverse("api_v1:assertion")
    response = client.get(url, {'search_by': assertion.name},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == assertion.id
    assert response.status_code == 200


def test_business_unit_search_by_success(db, client, business_unit, ):
    url = reverse("api_v1:business_unit")
    response = client.get(url, {'search_by': business_unit.name},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == business_unit.id
    assert response.status_code == 200


def test_control_search_by_success(db, client, control, ):
    url = reverse("api_v1:control")
    response = client.get(url, {'search_by': control.number},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == control.id
    assert response.status_code == 200


def test_control_step_search_by_success(db, client, control_step, ):
    url = reverse("api_v1:controlstep")
    response = client.get(url, {'search_by': control_step.step},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == control_step.id
    assert response.status_code == 200


def test_deficiency_search_by_success(db, client, deficiency, ):
    url = reverse("api_v1:deficiency")
    response = client.get(url, {'search_by': deficiency.short_description},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == deficiency.id
    assert response.status_code == 200


def test_ipe_search_by_success(db, client, ipe, ):
    url = reverse("api_v1:ipe")
    response = client.get(url, {'search_by': ipe.name},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == ipe.id
    assert response.status_code == 200


def test_legal_search_by_success(db, client, legal, ):
    url = reverse("api_v1:legal")
    response = client.get(url, {'search_by': legal.name},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == legal.id
    assert response.status_code == 200


def test_risk_search_by_success(db, client, risk, ):
    url = reverse("api_v1:risk")
    response = client.get(url, {'search_by': risk.name},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == risk.id
    assert response.status_code == 200


def test_system_search_by_success(db, client, system, ):
    url = reverse("api_v1:system")
    response = client.get(url, {'search_by': system.name},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == system.id
    assert response.status_code == 200


def test_service_search_by_success(db, client, service, ):
    url = reverse("api_v1:service")
    response = client.get(url, {'search_by': service.name},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == service.id
    assert response.status_code == 200


def test_process_search_by_success(db, client, process, ):
    url = reverse("api_v1:process")
    response = client.get(url, {'search_by': process.name},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == process.id
    assert response.status_code == 200


def test_file_search_by_success(db, client, file, ):
    url = reverse("api_v1:file")
    response = client.get(url, {'search_by': file.name},)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == file.id
    assert response.status_code == 200
