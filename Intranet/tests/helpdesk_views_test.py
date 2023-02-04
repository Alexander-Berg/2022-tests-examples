from datetime import timedelta, datetime
import json

import pytest
from mock import Mock, patch

from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse
from django_yauth.authentication_mechanisms.tvm import TvmServiceRequest

from staff.lib.auth import auth_mechanisms
from staff.lib.testing import GroupFactory, StaffFactory

from staff.preprofile.models.preprofile import PreprofileABCServices, PREPROFILE_STATUS, CANDIDATE_TYPE, Preprofile
from staff.preprofile.tests.utils import PreprofileFactory
from staff.preprofile.views import helpdesk_export, update_helpdesk_ticket


@pytest.mark.django_db()
def test_that_helpdesk_export_returns_valid_results(
    rf,
    tester,
    company,
    disable_preprofile_modified_auto_now,
    disable_preprofile_created_auto_now,
):
    request = rf.get(reverse('preprofile:helpdesk_export'))
    request.user = tester
    request.yauser = None
    request.auth_mechanism = auth_mechanisms.COOKIE

    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    helpdesk_export_returns_valid_results_test(request, tester, company)


@pytest.mark.django_db()
def test_helpdesk_export_tvm(
    rf,
    tester,
    company,
    disable_preprofile_modified_auto_now,
    disable_preprofile_created_auto_now,
):
    request = rf.get(reverse('preprofile:helpdesk_export'))
    request.yauser = Mock(spec=TvmServiceRequest)
    request.auth_mechanism = auth_mechanisms.TVM

    with patch('staff.lib.decorators._check_service_id', return_value=True):
        helpdesk_export_returns_valid_results_test(request, tester, company)


def helpdesk_export_returns_valid_results_test(request, tester, company):
    preprofile1 = PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.NEW,
        login='offer1',
        candidate_type=CANDIDATE_TYPE.FORMER_EMPLOYEE,
        femida_offer_id=1,
        recruiter=tester.get_profile(),
        modified_at=datetime.now(),
        created_at=datetime.now() - timedelta(hours=3),
    )

    preprofile2 = PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.READY,
        login='offer2',
        femida_offer_id=2,
        recruiter=tester.get_profile(),
        modified_at=datetime.now(),
        created_at=datetime.now() - timedelta(hours=2),
    )
    abc_service = GroupFactory(service_id=999)
    another_abc_service = GroupFactory(service_id=888)
    PreprofileABCServices.objects.create(group=abc_service, preprofile=preprofile2)
    PreprofileABCServices.objects.create(group=another_abc_service, preprofile=preprofile2)

    preprofile3 = PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.CLOSED,
        login='offer3',
        femida_offer_id=None,
        ext_form_link='http://ok-pod.ru',
        recruiter=tester.get_profile(),
        modified_at=datetime.now() - timedelta(hours=3),
        created_at=datetime.now() - timedelta(hours=3),
    )

    preprofile4 = PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.NEW,
        login='offer4',
        guid='some-guid',
        femida_offer_id=4,
        recruiter=tester.get_profile(),
        modified_at=datetime.now() - timedelta(hours=5),
        created_at=datetime.now() - timedelta(hours=5),
    )

    PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.CANCELLED,
        login='offer5',
        femida_offer_id=5,
        recruiter=tester.get_profile(),
        modified_at=datetime.now() - timedelta(days=8),
        created_at=datetime.now() - timedelta(days=8),
    )

    preprofile6 = PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.CANCELLED,
        login='offer6',
        femida_offer_id=6,
        recruiter=tester.get_profile(),
        modified_at=datetime.now() - timedelta(days=7, minutes=-1),
        created_at=datetime.now() - timedelta(days=7, minutes=-1),
    )

    PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.CLOSED,
        login='offer7',
        femida_offer_id=None,
        ext_form_link='http://ok-pod.ru',
        recruiter=tester.get_profile(),
        modified_at=datetime.now() - timedelta(days=8),
        created_at=datetime.now() - timedelta(days=8),
    )

    result = helpdesk_export(request)

    assert result.status_code == 200
    result = json.loads(result.content)

    assert len(result) == 5

    all_ids = (preprofile1.id, preprofile2.id, preprofile3.id, preprofile4.id, preprofile6.id)
    for item in result:
        assert item['id'] in all_ids
        if item['id'] == preprofile2.id:
            assert set(result[0]['abc_services']) == {
                abc_service.service_id,
                another_abc_service.service_id,
            }


@pytest.mark.django_db()
def test_that_helpdesk_ticket_can_be_updated(rf, tester, company, base_form, abc_services, red_rose_office):
    recruiter = StaffFactory()

    preprofile = PreprofileFactory(
        department_id=company.yandex.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        office=red_rose_office,
        recruiter=recruiter,
    )

    form = base_form
    form.update({
        'hdrfs_ticket': 'HDRFS-SOMETHING',
    })

    request = rf.post(
        reverse('preprofile:update_helpdesk_ticket', kwargs={'preprofile_id': preprofile.id}),
        json.dumps(form),
        content_type='application/json',
    )
    request.user = tester
    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))

    result = update_helpdesk_ticket(request, preprofile.id)

    result = json.loads(result.content)
    assert 'errors' not in result
    assert Preprofile.objects.get(id=preprofile.id).hdrfs_ticket == 'HDRFS-SOMETHING'
