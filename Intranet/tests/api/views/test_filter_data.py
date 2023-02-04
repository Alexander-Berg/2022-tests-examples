from django.core.urlresolvers import reverse


def test_filter_data_success(db, client, process_three,
                             sub_process, sub_process_two, sub_process_three,
                             django_assert_num_queries, default_queries_count):
    url = reverse("api_v1:filter_data")
    with django_assert_num_queries(default_queries_count + 7):
        response = client.get(url)
    response_json = response.json()
    assert response_json['process'][0]['name'] == process_three.name
    assert response.status_code == 200
    assert 'significance_evaluation' not in response_json


def test_filter_data_deficiency_success(db, client):
    url = reverse("api_v1:filter_data", kwargs={'obj_class': 'deficiency'})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert 'significance_evaluation' in response_json


def test_filter_data_filter_model_success(db, client):
    url = reverse("api_v1:filter_data", kwargs={'obj_class': 'ipe'})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert 'process' not in response_json
    assert 'system' in response_json


def test_filter_data_no_related_models_success(db, client,
                                               control_plan_with_subprocess,
                                               sub_process_two, sub_process,
                                               process_three,
                                               ):
    url = reverse("api_v1:filter_data", kwargs={'obj_class': 'controlplan'})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    filter_process = response_json['process']
    assert len(filter_process) == 1
    assert filter_process[0]['name'] == process_three.name
