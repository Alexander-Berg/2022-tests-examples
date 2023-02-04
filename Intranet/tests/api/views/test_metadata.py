from django.core.urlresolvers import reverse
from django.test import override_settings

from intranet.audit.src.api_v1.views.metadata.controlplan import ADDITIONAL_INFO_MAP
from intranet.audit.src.api_v1.views.metadata.base import MODEL_DEFAULT_LABEL_DATA


def test_equal_options_results_success(db, client,):
    url = reverse("api_v1:controlplan")
    response = client.options(url)
    response_options_json = response.json()

    url = reverse("api_v1:metadata", kwargs={'obj_class': 'controlplan'})
    response = client.get(url)
    response_get_json = response.json()
    assert response_get_json == response_options_json
    assert response.status_code == 200


def test_controlplan_list_view_assertion_options_success(db, client,):
    url = reverse("api_v1:controlplan")
    response = client.options(url)
    response_json = response.json()
    assertion_data = response_json['actions']['META']['assertion']
    assertion_additional_info = ADDITIONAL_INFO_MAP['assertion']
    assert assertion_data['type'] == assertion_additional_info['type']
    assert assertion_data['choices_endpoint'] == assertion_additional_info['choices_endpoint']
    assert response.status_code == 200


def test_controlplan_list_view_option_success(db, client, ):
    url = reverse("api_v1:controlplan")
    response = client.options(url)
    response_json = response.json()
    risk_data = response_json['actions']['META']['risk']
    assert risk_data['label'] == {'ru': 'Риск',
                                  'en': 'Risk',
                                  'default': 'Risk',
                                  }
    assert response.status_code == 200


def test_controlplan_detail_view_options_success(db, client, control_plan,):
    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    response = client.options(url)
    response_json = response.json()
    business_unit_data = response_json['actions']['META']['business_unit']
    assert business_unit_data['suggest'] == 'business_unit'
    assert business_unit_data['multi_choice'] is True
    assert response.status_code == 200


def test_controlplan_detail_view_options_not_multi_success(db, client, control_plan,):
    url = reverse("api_v1:controlplan_detail", kwargs={'pk': control_plan.id})
    response = client.options(url)
    response_json = response.json()
    business_unit_data = response_json['actions']['META']['control']
    assert business_unit_data['suggest'] == 'control'
    assert business_unit_data['multi_choice'] is False
    assert response.status_code == 200


def test_controlplan_list_view_options_owner_success(db, client,):
    url = reverse("api_v1:controlplan")
    response = client.options(url)
    response_json = response.json()
    owner_data = response_json['actions']['META']['owner']
    assert owner_data['multi_choice'] is True
    assert owner_data['suggest'] == 'stated_person'
    assert response.status_code == 200


@override_settings(MIDDLEWARE=list())
def test_no_auth_success(db, client,):
    url = reverse("api_v1:metadata", kwargs={'obj_class': 'controlplan'})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert response_json['name'] == 'Control Plan List'


def test_view_option_with_default_label_success(db, client, ):
    url = reverse("api_v1:controlplan")
    response = client.options(url)
    response_json = response.json()
    risk_data = response_json['actions']['META']['legal']
    assert risk_data['label'] == {
        'default': MODEL_DEFAULT_LABEL_DATA['legal'],
    }
    assert response.status_code == 200
