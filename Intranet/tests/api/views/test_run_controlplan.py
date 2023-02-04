from unittest import mock

from django.core.urlresolvers import reverse

from intranet.audit.src.core import models
from intranet.audit.src.users.models import StatedPerson


def test_run_controlplan_success(db, client, control_plan):
    url = reverse("api_v1:controlplan_run")
    assert models.ControlTest.objects.count() == 0
    response = client.post(url, {'obj_pks': control_plan.id, },)
    assert response.status_code == 302
    assert models.ControlTest.objects.count() == 1
    assert models.ControlTest.objects.first().control_plan_id == control_plan.id


def test_run_controlplan_success_many(db, client, control_plan, control_plan_two,):
    url = reverse("api_v1:controlplan_run")
    assert models.ControlTest.objects.count() == 0
    response = client.post(url, {'obj_pks': ','.join([str(control_plan.id),
                                                      str(control_plan_two.id)])}, )
    assert response.status_code == 200
    assert models.ControlTest.objects.count() == 2
    assert models.ControlTest.objects.filter(control_plan=control_plan).count() == 1
    assert models.ControlTest.objects.filter(control_plan=control_plan_two).count() == 1
    assert response.json() == {'control_test_created': 2}


def test_run_controlplan_fail_no_pks(db, client):
    url = reverse("api_v1:controlplan_run")
    assert models.ControlTest.objects.count() == 0
    response = client.post(url)
    assert response.status_code == 409
    assert models.ControlTest.objects.count() == 0
    assert response.json() == {'debug_message': 'Request with incorrect parameters was made',
                               'error_code': 'BAD_REQUEST', 'level': 'ERROR',
                               'message': ['You should pass obj_pks in request']}


def test_run_controlplan_fail_wrong_pks(db, client):
    url = reverse("api_v1:controlplan_run")
    assert models.ControlTest.objects.count() == 0
    response = client.post(url, {'obj_pks': '1,2'}, )
    assert response.status_code == 409
    assert models.ControlTest.objects.count() == 0
    assert response.json() == {'debug_message': 'Request with incorrect parameters was made',
                               'error_code': 'BAD_REQUEST', 'level': 'ERROR',
                               'message': ["Check id's correction - ControlPlans does not exists"]}


def test_run_controlplan_success_tester_id(db, client, control_plan, test_vcr, ):
    url = reverse("api_v1:controlplan_run")
    assert models.ControlTest.objects.count() == 0
    assert StatedPerson.objects.count() == 0
    with test_vcr.use_cassette('run_controlplan_success_tester_id'):
        with mock.patch('intranet.audit.src.users.logic.staff_person.get_service_ticket',
                        lambda *args, **kwargs: 'test token'):

            response = client.post(url, {'obj_pks': control_plan.id,
                                         'assign_to': '1120000000016772',
                                         },
                                   )
    assert response.status_code == 302
    assert models.ControlTest.objects.count() == 1
    assert StatedPerson.objects.count() == 1
    control_test = models.ControlTest.objects.first()
    assert control_test.control_plan_id == control_plan.id
    tester = control_test.tester.first()
    assert tester.uid == '1120000000016772'
    assert tester.position == 'Младший разработчик'
    assert tester.login == 'smosker'
    assert tester.department == 'Группа составления программ для ЭВМ'
    assert tester.department_slug == 'yandex_infra_tech_tools_content_dev_wiki'


def test_run_controlplan_success_many_tester_id(db, client, control_plan, test_vcr, ):
    url = reverse("api_v1:controlplan_run")
    assert models.ControlTest.objects.count() == 0
    assert StatedPerson.objects.count() == 0
    with test_vcr.use_cassette('run_controlplan_success_many_tester_id'):
        with mock.patch('intranet.audit.src.users.logic.staff_person.get_service_ticket',
                        lambda *args, **kwargs: 'test token'):
            response = client.post(url, {'obj_pks': control_plan.id,
                                         'assign_to': '1120000000016772,1120000000000529',
                                         },
                                   )
    assert response.status_code == 302
    assert models.ControlTest.objects.count() == 1
    assert StatedPerson.objects.count() == 2
    control_test = models.ControlTest.objects.first()
    assert control_test.control_plan_id == control_plan.id
    tester_one = control_test.tester.get(uid='1120000000016772')
    assert tester_one.login == 'smosker'
    assert '1120000000016772' in tester_one.id
    assert tester_one.position == 'Младший разработчик'
    tester_two = control_test.tester.get(uid='1120000000000529')
    assert tester_two.login == 'volozh'
    assert '1120000000000529' in tester_two.id
    assert tester_two.position == 'Генеральный директор группы компаний «Яндекс»'


def test_run_controlplan_fail_wrong_tester_id(db, client, control_plan, test_vcr, ):
    url = reverse("api_v1:controlplan_run")
    assert models.ControlTest.objects.count() == 0
    assert StatedPerson.objects.count() == 0
    with test_vcr.use_cassette('run_controlplan_fail_wrong_tester_id'):
        with mock.patch('intranet.audit.src.users.logic.staff_person.get_service_ticket',
                        lambda *args, **kwargs: 'test token'):
            response = client.post(url, {'obj_pks': control_plan.id,
                                         'assign_to': '123',
                                         },
                                   )
    assert response.status_code == 500
    assert StatedPerson.objects.count() == 0
    assert response.json()['message'] == ['Could not create persons: "[\'123\']"']


def test_run_controlplan_fail_wrong_tester_id_many(db, client, control_plan, test_vcr, ):
    url = reverse("api_v1:controlplan_run")
    assert models.ControlTest.objects.count() == 0
    assert StatedPerson.objects.count() == 0
    with test_vcr.use_cassette('run_controlplan_fail_wrong_tester_id_many'):
        with mock.patch('intranet.audit.src.users.logic.staff_person.get_service_ticket',
                        lambda *args, **kwargs: 'test token'):
            response = client.post(url, {'obj_pks': control_plan.id,
                                         'assign_to': '1234,123',
                                         },
                                   )
    assert response.status_code == 500
    assert StatedPerson.objects.count() == 0
    assert response.json()['message'] == ['Could not create persons: "[\'1234\', \'123\']"']
