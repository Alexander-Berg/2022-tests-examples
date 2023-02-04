from django.core.urlresolvers import reverse


def test_controlplan_list_success(db, client, control_plan, control_plan_two):
    url = reverse("api_v1:controlplan")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 2
    assert {control_plan.id, control_plan_two.id} == {plan['id'] for plan in response_json}
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_controltest_list_success(db, client, control_test):
    url = reverse("api_v1:controltest")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == control_test.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_account_list_success(db, client, account):
    url = reverse("api_v1:account")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == account.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_assertion_list_success(db, client, assertion):
    url = reverse("api_v1:assertion")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == assertion.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_business_unit_list_success(db, client, business_unit):
    url = reverse("api_v1:business_unit")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == business_unit.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_control_list_success(db, client, control):
    url = reverse("api_v1:control")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == control.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_control_step_list_success(db, client, control_step):
    url = reverse("api_v1:controlstep")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == control_step.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_deficiency_list_success(db, client, deficiency):
    url = reverse("api_v1:deficiency")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == deficiency.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_deficiency_group_list_success(db, client, deficiency_group):
    url = reverse('api_v1:deficiencygroup')
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == deficiency_group.id
    assert response.status_code == 200


def test_ipe_list_success(db, client, ipe):
    url = reverse("api_v1:ipe")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == ipe.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_legal_list_success(db, client, legal):
    url = reverse("api_v1:legal")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == legal.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_risk_list_success(db, client, risk):
    url = reverse("api_v1:risk")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == risk.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_service_list_success(db, client, service):
    url = reverse("api_v1:service")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == service.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_system_list_success(db, client, system):
    url = reverse("api_v1:system")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == system.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_process_list_success(db, client, process):
    url = reverse("api_v1:process")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == process.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_file_list_success(db, client, file):
    url = reverse("api_v1:file")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == file.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_controltestipe_list_success(db, client, controltestipe):
    url = reverse("api_v1:controltestipe")
    response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == controltestipe.id
    assert response.json()['actions'] == {'add': True, }
    assert response.status_code == 200


def test_controlplan_list_view_with_fields_success(db, client, control_plan, control_plan_two,
                                                   django_assert_num_queries, default_queries_count,
                                                   ):
    url = reverse("api_v1:controlplan")
    with django_assert_num_queries(default_queries_count+1):
        response = client.get(url, {
            'fields': 'id,key_control,comment,method,control_type',
        })
    response_json = response.json()
    assert response_json['results'] == [
        {
            'id': control_plan_two.id,
            'comment': 'comment for two',
            'control_type': 'warning',
            'method': 'auto',
            'key_control': True,
        },
        {
            'id': control_plan.id,
            'comment': 'comment for one',
            'control_type': 'warning',
            'method': 'manual',
            'key_control': True,
        },
    ]
    assert response_json['actions'] == {
        'add': True,
    }
    assert response.status_code == 200


def test_controlplan_list_view_with_fields_related_success(db, client, risk, control_plan,
                                                           control_plan_two,
                                                           django_assert_num_queries,
                                                           default_queries_count):
    url = reverse("api_v1:controlplan")
    with django_assert_num_queries(default_queries_count+2):
        response = client.get(url, {'fields': 'id,key_control,risk', })
    response_json = response.json()
    assert response_json['results'] == [
        {'id': control_plan_two.id,
         'risk_data': [
             {'id': risk.id,
              'name': 'risk',
              'number': 'risk number',
              'actions': {'change': True, 'delete': True},
              }
         ],
         'key_control': True,
         'risk': [risk.id],
         },
        {'id': control_plan.id,
         'risk_data': [
             {'id': risk.id,
              'name': 'risk',
              'number': 'risk number',
              'actions': {'change': True, 'delete': True},
              }
         ],
         'key_control': True,
         'risk': [risk.id],
         },
    ]
    assert response_json['actions'] == {
        'add': True,
    }
    assert response.status_code == 200


def test_account_list_view_queries_num_success(db, client, account, default_queries_count,
                                               django_assert_num_queries):
    url = reverse("api_v1:account")
    with django_assert_num_queries(default_queries_count + 1):
        response = client.get(url)
    response_json = response.json()['results']
    assert len(response_json) == 1
    assert response_json[0]['id'] == account.id
    assert response.status_code == 200
