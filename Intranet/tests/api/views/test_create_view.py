import json
import datetime

from unittest import mock

from django.core.urlresolvers import reverse
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings

from intranet.audit.src.core import models
from intranet.audit.src.users.models import StatedPerson
from intranet.audit.src.files.models import File


def test_controlplan_create_success(db, client, risk, control, process, process_two):
    url = reverse("api_v1:controlplan", )
    data = {"risk": [risk.id],
            "control_type": "revealing",
            "control": control.id,
            "method": "it_dependent",
            "frequency": "adhoc",
            "key_control": False,
            "process": [process.id, process_two.id],
            }
    assert models.ControlPlan.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.ControlPlan.objects.count() == 1
    response_json.pop('id')
    assert response_json == {'legal': [], 'control_type': 'revealing', 'antifraud': False,
                             'key_control': False,
                             'process_data': [
                                 {'id': process.id, 'process_type': 'root',
                                  'parent': None, 'parent_data': None,
                                  'name': 'process_one',
                                  'actions': {'change': True, 'delete': True},
                                  },
                                 {'id': process_two.id, 'process_type': 'root',
                                  'parent': None, 'parent_data': None,
                                  'name': 'process_two',
                                  'actions': {'change': True, 'delete': True},
                                  },
                             ], 'account_data': [], 'evidence': None,  # noqa: E126
                             'regulation': None,
                             'assertion': [],
                             'control_data': {
                                 'id': control.id,
                                 'number': 'control number',
                                 'name': 'control',
                                 'actions': {'change': True, 'delete': True},
                             }, 'owner': [], 'control': control.id,  # noqa: E126
                             'process': [process.id, process_two.id], 'risk': [risk.id],
                             'service_data': [], 'system': [],
                             'risk_data': [
                                 {'id': risk.id,
                                  'number': 'risk number',
                                  'name': 'risk',
                                  'actions': {'change': True, 'delete': True},
                                  },
                             ], 'test_period_finished': None,  # noqa: E126
                             'service': [], 'account': [], 'method': 'it_dependent',
                             'business_unit': [], 'frequency': 'adhoc', 'comment': None,
                             'owner_data': [], 'assertion_data': [], 'legal_data': [],
                             'business_unit_data': [], 'test_period_started': None,
                             'description': None, 'system_data': [], 'reviewer': [],
                             'reviewer_data': [], 'status': 'draft',
                             }
    assert response.status_code == 201


def test_controlplan_create_fail(db, client, risk, control, process, process_two):
    url = reverse("api_v1:controlplan", )
    data = {"risk": [risk.id],
            "control_type": "revealing",
            "control": control.id,
            "process": [process.id, process_two.id],
            }
    assert models.ControlPlan.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert {'frequency': ['Это поле обязательно.'],
            'method': ['Это поле обязательно.']
            } == response_json['errors']
    assert models.ControlPlan.objects.count() == 0
    assert response.status_code == 400


def test_controlplan_create_with_owner_success(db, client, risk, control, process, process_two, test_vcr, ):
    url = reverse("api_v1:controlplan", )
    owner_id = 1120000000016772
    data = {"risk": [risk.id],
            "control_type": "revealing",
            "control": control.id,
            "method": "it_dependent",
            "frequency": "adhoc",
            "key_control": False,
            "process": [process.id, process_two.id],
            "owner": [owner_id]
            }
    assert models.ControlPlan.objects.count() == 0
    assert StatedPerson.objects.count() == 0
    assert models.Process.objects.count() == 2
    with test_vcr.use_cassette('controlplan_create_with_owner_success'):
        with mock.patch('intranet.audit.src.users.logic.staff_person.get_service_ticket',
                        lambda *args, **kwargs: 'test token'):
            response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.ControlPlan.objects.count() == 1
    assert StatedPerson.objects.count() == 1
    assert models.Process.objects.count() == 2
    control_plan = models.ControlPlan.objects.first()
    stated_person = StatedPerson.objects.get(uid=owner_id)
    assert control_plan.control == control
    assert control_plan.owner.count() == 1
    assert control_plan.owner.first() == stated_person
    assert len(response_json['owner_data']) == 1
    assert set(control_plan.process.values_list('id', flat=True)) == set([process.id, process_two.id])
    assert response_json['owner_data'][0]['uid'] == str(owner_id)
    assert response_json['owner_data'][0]['login'] == stated_person.login
    assert response.status_code == 201


def test_controltest_create_success(db, client, control_plan):
    url = reverse("api_v1:controltest", )
    data = {"control_plan": control_plan.id,
            "controlstep_set": [],
            }
    assert models.ControlTest.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.ControlTest.objects.count() == 1
    control_test = models.ControlTest.objects.first()
    assert control_test.control_plan == control_plan
    assert response_json['control_plan'] == control_plan.id
    assert response.status_code == 201


def test_controltest_create_from_existed_success(db, client, control_plan, control_test,
                                                 control_step, deficiency, test_vcr,):
    control_test.status = 'archived'
    control_test.testing_date = "2017-05-13"
    control_test.test_period_started = "2017-06-13"
    control_test.save()
    control_test.deficiency.add(deficiency)

    url = reverse("api_v1:controltest", )
    data = {"control_plan": control_plan.id,
            "controlstep_set": [],
            "test_period_started": "2017-01-13",
            "test_period_finished": "2017-02-16",
            "tester": ["1120000000016772"]
            }

    with test_vcr.use_cassette('controlplan_create_with_owner_success'):
        with mock.patch('intranet.audit.src.users.logic.staff_person.get_service_ticket',
                        lambda *args, **kwargs: 'test token'):
            response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert response.status_code == 201
    assert models.ControlTest.objects.count() == 2
    control_test.refresh_from_db()
    assert control_test.controlstep_set.count() == 1

    control_test = models.ControlTest.objects.get(status='draft')
    assert control_test.control_plan == control_plan
    assert response_json['control_plan'] == control_plan.id
    assert control_test.test_period_started == datetime.date(2017, 1, 13)
    assert control_test.test_period_finished == datetime.date(2017, 2, 16)
    assert control_test.controlstep_set.count() == 1
    assert control_test.deficiency.count() == 0
    step = control_test.controlstep_set.first()
    assert step.step == control_step.step
    assert step.comment == control_step.comment
    assert step.result is None


def test_account_create_success(db, client, ):
    url = reverse("api_v1:account", )
    data = {"name": "test account name",
            }
    assert models.Account.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.Account.objects.count() == 1
    account = models.Account.objects.first()
    assert account.name == "test account name"
    assert response_json['id'] == account.id
    assert response.status_code == 201


def test_assertion_create_success(db, client, ):
    url = reverse("api_v1:assertion", )
    data = {"name": "test assertion name",
            }
    assert models.Assertion.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.Assertion.objects.count() == 1
    assertion = models.Assertion.objects.first()
    assert assertion.name == "test assertion name"
    assert response_json['id'] == assertion.id
    assert response.status_code == 201


def test_business_unit_create_success(db, client, ):
    url = reverse("api_v1:business_unit", )
    data = {"name": "test bu name",
            }
    assert models.BusinessUnit.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.BusinessUnit.objects.count() == 1
    business_unit = models.BusinessUnit.objects.first()
    assert business_unit.name == "test bu name"
    assert response_json['id'] == business_unit.id
    assert response.status_code == 201


def test_control_create_success(db, client, ):
    url = reverse("api_v1:control", )
    data = {"name": "test control name",
            "number": "some number",
            "control": "control",
            }
    assert models.Control.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.Control.objects.count() == 1
    control = models.Control.objects.first()
    assert control.name == "test control name"
    assert response_json['id'] == control.id
    assert response.status_code == 201


def test_control_step_create_success(db, client, ):
    url = reverse("api_v1:controlstep", )
    data = {"step": "some step"
            }
    assert models.ControlStep.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.ControlStep.objects.count() == 1
    control_step = models.ControlStep.objects.first()
    assert control_step.step == "some step"
    assert response_json['id'] == control_step.id
    assert response.status_code == 201


def test_deficiency_create_success(db, client, ):
    url = reverse("api_v1:deficiency", )
    data = {"short_description": "some description",
            "control_test": [],
            }
    assert models.Deficiency.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.Deficiency.objects.count() == 1
    deficiency = models.Deficiency.objects.first()
    assert deficiency.short_description == "some description"
    assert response_json['id'] == deficiency.id
    assert response.status_code == 201


def test_deficiency_group_create_success(db, client):
    url = reverse('api_v1:deficiencygroup')
    data = {
        'full_description': 'full description',
    }
    manager = models.DeficiencyGroup.objects
    assert manager.count() == 0

    response = client.post(url, json.dumps(data), content_type='application/json')
    assert response.status_code == 201
    assert manager.count() == 1

    deficiency_group = manager.first()
    assert deficiency_group.full_description == data['full_description']

    response_json = response.json()
    assert response_json['id'] == deficiency_group.id


def test_ipe_create_success(db, client, ):
    url = reverse("api_v1:ipe", )
    data = {"name": "some name"
            }
    assert models.IPE.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.IPE.objects.count() == 1
    ipe = models.IPE.objects.first()
    assert ipe.name == "some name"
    assert response_json['id'] == ipe.id
    assert response.status_code == 201


def test_legal_create_success(db, client, ):
    url = reverse("api_v1:legal", )
    data = {"name": "some name"
            }
    assert models.Legal.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.Legal.objects.count() == 1
    legal = models.Legal.objects.first()
    assert legal.name == "some name"
    assert response_json['id'] == legal.id
    assert response.status_code == 201


def test_risk_create_success(db, client, ):
    url = reverse("api_v1:risk", )
    data = {"name": "some name",
            "number": "some number",
            }
    assert models.Risk.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.Risk.objects.count() == 1
    risk = models.Risk.objects.first()
    assert risk.name == "some name"
    assert response_json['id'] == risk.id
    assert response.status_code == 201


def test_service_create_success(db, client, ):
    url = reverse("api_v1:service", )
    data = {"name": "some name",
            }
    assert models.Service.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.Service.objects.count() == 1
    service = models.Service.objects.first()
    assert service.name == "some name"
    assert response_json['id'] == service.id
    assert response.status_code == 201


def test_system_create_success(db, client, ):
    url = reverse("api_v1:system", )
    data = {"name": "some name",
            }
    assert models.System.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.System.objects.count() == 1
    system = models.System.objects.first()
    assert system.name == "some name"
    assert response_json['id'] == system.id
    assert response.status_code == 201


def test_process_create_success(db, client, ):
    url = reverse("api_v1:process", )
    data = {"name": "some name",
            "process_type": models.Process.TYPES.root,
            }
    assert models.Process.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.Process.objects.count() == 1
    process = models.Process.objects.first()
    assert process.name == "some name"
    assert response_json['id'] == process.id
    assert response.status_code == 201


@override_settings(DEFAULT_FILE_STORAGE='django.core.files.storage.FileSystemStorage')
def test_file_create_success(db, client, ):
    url = reverse("api_v1:file",)
    data = {"file": SimpleUploadedFile('some_name.txt', b'some text data'),
            }
    assert File.objects.count() == 0
    response = client.post(url, data=data)
    response_json = response.json()
    assert File.objects.count() == 1
    file = File.objects.first()
    assert file.name == "some_name.txt"
    assert file.content_type == 'text/plain'
    assert response_json['id'] == file.id
    assert response.status_code == 201


def test_controltestipe_create_success(db, client, ipe, control_test):
    url = reverse("api_v1:controltestipe", )
    data = {
        "ipe": ipe.id,
        "control_test": control_test.id,
    }
    assert models.ControlTestIPE.objects.count() == 0
    response = client.post(url, json.dumps(data), content_type='application/json', )
    response_json = response.json()
    assert models.ControlTestIPE.objects.count() == 1
    controltestipe = models.ControlTestIPE.objects.first()
    assert response_json['id'] == controltestipe.id
    assert response.status_code == 201
