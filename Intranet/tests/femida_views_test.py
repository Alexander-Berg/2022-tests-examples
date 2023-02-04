from datetime import datetime
import json
from datetime import timedelta, date

from mock import patch, Mock
import pytest

from django.conf import settings
from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from django_yauth.user import Application

from staff.groups.models import GROUP_TYPE_CHOICES
from staff.lib.testing import StaffFactory, OfficeFactory, GroupFactory
from staff.person.models import Staff, VERIFY_CODE_OBJECT_TYPES, VerifyCode, VERIFY_STATE
from staff.person_avatar.controllers.preprofile import PreprofileAvatarCollection
from staff.person_avatar.models import AvatarMetadata

from staff.preprofile.models import (
    OS_TYPE,
    EMAIL_DOMAIN,
    CANDIDATE_TYPE,
    Preprofile,
    FORM_TYPE,
    PREPROFILE_STATUS,
    AUTOHIRE_HARDWARE_PROFILE_TYPE,
    AUTOHIRE_HARDWARE_PROFILE_TYPE_TO_ID,
    NEED_TABLE,
)
from staff.preprofile.tests.utils import (
    PreprofileFactory,
    HardwareFactory,
    ProfileForDepartmentFactory,
)
from staff.preprofile.views import (
    approve,
    cancel_form,
    create_from_femida,
    edit_form,
    femida_submit_external_form,
    femida_update,
    femida_ds_attach_phone,
    femida_ds_verify_phone,
)
from staff.preprofile.tasks import AutoAdoptPreprofiles
from staff.preprofile.utils import normalize_phone


@pytest.fixture
def femida_abc_services():
    now = datetime.now()

    service_root = GroupFactory(
        name='__services__', url='__services__',
        service_id=None, department=None,
        parent=None,
        created_at=now, modified_at=now,
        type=GROUP_TYPE_CHOICES.SERVICE,
    )

    group_staff = GroupFactory(
        name='staff', url='staff', code='staff',
        service_id=123, department=None,
        parent=service_root,
        created_at=now, modified_at=now,
        type=GROUP_TYPE_CHOICES.SERVICE,
    )

    return [group_staff.id]


@pytest.fixture
def femida_robot(db):
    s = StaffFactory(login='robot-femida')
    s.user.username = s.login
    s.user.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    s.user.user_permissions.add(Permission.objects.get(codename='add_preprofile'))
    s.user.save()

    return s.user


def femida_update_url(id):
    return reverse('preprofile:femida_update', kwargs={'preprofile_id': id})


def set_client_application(request, application_name):
    setattr(request, 'client_application', Application(id='', name=application_name, home_page=''))


@pytest.mark.django_db()
def test_that_saved_form_can_be_cancelled(rf, company, femida_robot):
    model = PreprofileFactory(department_id=company.yandex.id)

    request = rf.post(reverse('preprofile:cancel_form', kwargs={'preprofile_id': model.id}))
    request.user = femida_robot
    set_client_application(request, 'femida')

    result = cancel_form(request, model.id)
    assert result.status_code == 200


@pytest.mark.django_db()
def test_that_saved_form_can_be_cancelled_only_by_femida(rf, company, femida_robot):
    model = PreprofileFactory(department_id=company.yandex.id)

    request = rf.post(reverse('preprofile:cancel_form', kwargs={'preprofile_id': model.id}))
    request.user = femida_robot
    set_client_application(request, 'staff')

    result = cancel_form(request, model.id)
    assert result.status_code == 403


@pytest.mark.django_db()
def test_that_saved_form_can_be_updated_only_by_femida(rf, company, femida_robot):
    model = PreprofileFactory(department_id=company.yandex.id)

    request = rf.post(
        femida_update_url(model.id),
        json.dumps({}),
        content_type='application/json'
    )
    request.user = femida_robot
    set_client_application(request, 'staff')

    result = femida_update(request, model.id)
    assert result.status_code == 403


@pytest.mark.django_db()
def test_that_404_will_be_returned_on_absent_model_for_femida_update(rf, femida_robot):
    request = rf.post(
        femida_update_url(100500),
        json.dumps({}),
        content_type='application/json'
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    result = femida_update(request, 100500)
    assert result.status_code == 404


@pytest.mark.django_db()
def test_that_404_will_be_returned_on_trying_to_change_form_created_not_from_femida(rf, femida_robot, company):
    model = PreprofileFactory(department_id=company.yandex.id)

    request = rf.post(
        femida_update_url(model.id),
        json.dumps({}),
        content_type='application/json'
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    result = femida_update(request, model.id)
    assert result.status_code == 400


@pytest.mark.django_db()
def test_that_model_data_will_returned_on_get_from_femida_update_view(rf, femida_robot, company):
    model = PreprofileFactory(
        department_id=company.yandex.id,
        femida_offer_id=1,
        recruiter=StaffFactory(),
    )

    request = rf.get(
        femida_update_url(model.id),
        content_type='application/json'
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    result = femida_update(request, model.id)
    assert result.status_code == 200
    result = json.loads(result.content)
    assert result['join_at']
    assert result['login']


@pytest.mark.django_db()
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_that_model_data_can_be_changed_from_femida_update_view(dns_mock, validate_in_ldap, rf, femida_robot, company):
    model = PreprofileFactory(
        department_id=company.yandex.id,
        femida_offer_id=1,
    )
    after_tomorrow = date.today() + timedelta(days=2)

    request = rf.post(
        femida_update_url(model.id),
        json.dumps({
            'join_at': after_tomorrow.isoformat(),
            'login': 'olololo',
            'candidate_type': CANDIDATE_TYPE.NEW_EMPLOYEE,
            'department': company.yandex.url,
            'date_completion_internship': date.today().isoformat(),
        }),
        content_type='application/json'
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    result = femida_update(request, model.id)
    assert result.status_code == 200

    model = Preprofile.objects.get(id=model.id)
    assert model.login == 'olololo'
    assert model.join_at == after_tomorrow
    assert model.date_completion_internship == date.today()


@pytest.mark.django_db()
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_that_model_data_can_be_changed_from_femida_update_view_and_date_completion_internship_is_not_passed(
    dns_mock, validate_in_ldap, rf, femida_robot, company,
):
    model = PreprofileFactory(
        department_id=company.yandex.id,
        femida_offer_id=1,
    )

    after_tomorrow = date.today() + timedelta(days=2)

    request = rf.post(
        femida_update_url(model.id),
        json.dumps({
            'join_at': after_tomorrow.isoformat(),
            'login': 'olololo',
            'candidate_type': CANDIDATE_TYPE.NEW_EMPLOYEE,
            'department': company.yandex.url,
        }),
        content_type='application/json'
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    result = femida_update(request, model.id)
    assert result.status_code == 200

    model = Preprofile.objects.get(id=model.id)
    assert model.login == 'olololo'
    assert model.join_at == after_tomorrow
    assert model.date_completion_internship is None


@pytest.mark.django_db()
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_that_passes_validation_when_femida_sends_same_login_for_preprofile(
    dns_mock, validate_in_ldap, rf, femida_robot, company,
):
    model = PreprofileFactory(
        department_id=company.yandex.id,
        femida_offer_id=1,
        login='olololo',
        candidate_type=CANDIDATE_TYPE.NEW_EMPLOYEE,
    )

    after_tomorrow = date.today() + timedelta(days=2)

    request = rf.post(
        femida_update_url(model.id),
        json.dumps({
            'join_at': after_tomorrow.isoformat(),
            'login': 'olololo',
            'candidate_type': CANDIDATE_TYPE.NEW_EMPLOYEE,
            'department': company.yandex.url,
            'date_completion_internship': None,
        }),
        content_type='application/json'
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    result = femida_update(request, model.id)
    assert result.status_code == 200

    model = Preprofile.objects.get(id=model.id)
    assert model.login == 'olololo'
    assert model.join_at == after_tomorrow
    assert model.date_completion_internship is None


@pytest.mark.django_db()
@pytest.mark.parametrize('with_services', (True, False))
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_employee_form_can_be_saved_from_femida(
    dns_mock, validate_in_ldap, with_services,
    company, rf, tester, base_form, femida_abc_services, red_rose_office, yndx_org, femida_robot
):
    after_tomorrow = date.today() + timedelta(days=2)

    form = base_form
    update_data = {
        'department': company.yandex.url,
        'position_staff_text': '1',
        'office': red_rose_office.id,
        'login': 'employee',
        'lang_ui': 'en',
        'femida_offer_id': 100500,
        'organization': yndx_org.id,
        'os': OS_TYPE.WINDOWS,
        'recruiter': tester.staff.login,
        'date_completion_internship': after_tomorrow.isoformat(),
    }
    if with_services:
        update_data['abc_services'] = femida_abc_services

    form.update(update_data)

    request = rf.post(
        reverse('preprofile:create_from_femida'),
        json.dumps(form),
        content_type='application/json',
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    result = create_from_femida(request)

    result = json.loads(result.content)
    assert 'errors' not in result
    assert 'id' in result

    result_data = {
        'department': company.yandex.id,
        'position_staff_text': '1',
        'abc_services': femida_abc_services[0] if with_services else None,
        'office': red_rose_office.id,
        'login': 'employee',
        'lang_ui': 'en',
        'femida_offer_id': 100500,
        'organization': yndx_org.id,
        'os': OS_TYPE.WINDOWS,
        'recruiter': tester.staff.id,
        'need_vpn': True,
        'need_chair': False,
        'need_phone': None,
        'email_domain': EMAIL_DOMAIN.YANDEX_TEAM_RU,
        'date_completion_internship': after_tomorrow,
    }

    assert Preprofile.objects.count() == 1
    assert Preprofile.objects.values(*result_data.keys())[0] == result_data


@pytest.mark.django_db()
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_that_409_will_be_returned_on_same_femida_offer_id(
    dns_mock,
    validate_in_ldap,
    rf,
    tester,
    company,
    base_form,
    femida_abc_services,
    red_rose_office,
    yndx_org,
    femida_robot,
):
    form = base_form
    form.update({
        'department': company.yandex.url,
        'position_staff_text': '1',
        'approved_by': company.persons['yandex-chief'].login,
        'abc_services': femida_abc_services,
        'login': 'employee',
        'femida_offer_id': 100500,
        'office': red_rose_office.id,
        'organization': yndx_org.id,
        'os': OS_TYPE.WINDOWS,
        'recruiter': tester.staff.login,
    })

    request = rf.post(
        reverse('preprofile:create_from_femida'),
        json.dumps(form),
        content_type='application/json',
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    create_from_femida(request)

    result = create_from_femida(request)
    assert result.status_code == 409

    result = json.loads(result.content)
    assert 'errors' not in result
    assert 'id' in result


@pytest.mark.django_db()
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_photo_is_uploaded_to_avatar_service(
    dns_mock,
    validate_in_ldap,
    rf,
    tester,
    company,
    base_form,
    femida_abc_services,
    red_rose_office,
    yndx_org,
    femida_robot,
    monkeypatch,
):
    form = base_form
    form.update({
        'department': company.yandex.url,
        'position_staff_text': '1',
        'approved_by': company.persons['yandex-chief'].login,
        'abc_services': femida_abc_services,
        'login': 'employee',
        'femida_offer_id': 100500,
        'office': red_rose_office.id,
        'organization': yndx_org.id,
        'os': OS_TYPE.WINDOWS,
        'recruiter': tester.staff.login,
        'photo': 'http://some.photo.url',
    })

    def upload_patched(self, *args, **kwargs):
        self.metadata.is_deleted = False
        self.metadata.save()

    monkeypatch.setattr(
        PreprofileAvatarCollection.avatar_class,
        'upload',
        upload_patched,
    )

    def post_upload_patched(self, *args, **kwargs):
        AvatarMetadata.objects.filter(preprofile=self.owner).update(
            is_main=True,
            is_avatar=True,
        )

    monkeypatch.setattr(
        PreprofileAvatarCollection,
        'post_upload',
        post_upload_patched,
    )

    create_request = rf.post(
        reverse('preprofile:create_from_femida'),
        json.dumps(form),
        content_type='application/json',
    )
    create_request.user = femida_robot
    set_client_application(create_request, 'femida')
    create_result = create_from_femida(create_request)
    created_id = json.loads(create_result.content)['id']

    get_request = rf.get(reverse(
        'preprofile:edit_form',
        kwargs={'preprofile_id': created_id},
    ))
    get_request.user = tester
    get_result = edit_form(get_request, created_id)
    received_photo_url = json.loads(get_result.content)['data']['photo']['value']
    expected = 'https://{}/api/v1/user/preprofile/photo_{}/original.jpg'.format(
        settings.CENTER_MASTER,
        AvatarMetadata.objects.get(preprofile_id=created_id).id,
    )
    assert received_photo_url == expected


@pytest.mark.django_db()
@patch('staff.preprofile.login_validation.validate_for_dns', Mock())
@patch('staff.preprofile.login_validation.validate_in_ldap', Mock())
def test_employee_autohire_form_can_be_saved_from_femida(
    company, rf, tester, base_form, red_rose_office, yndx_org, femida_robot, robot_staff_user,
):
    """
    Проверяет создание препрофайла при автонайме
    """
    hardware_profile_type = AUTOHIRE_HARDWARE_PROFILE_TYPE.TOKEN
    hardware_profile_id = AUTOHIRE_HARDWARE_PROFILE_TYPE_TO_ID[hardware_profile_type]
    hardware_profile = HardwareFactory(profile_id=hardware_profile_id)
    ProfileForDepartmentFactory(profile=hardware_profile, department=company.outstaff)

    form = base_form
    update_data = {
        'department': company.outstaff.url,
        'position_staff_text': '1',
        'office': red_rose_office.id,
        'login': 'employee',
        'lang_ui': 'en',
        'femida_offer_id': 100500,
        'organization': yndx_org.id,
        'os': OS_TYPE.WINDOWS,
        'recruiter': tester.staff.login,
        'is_autohire': True,
        'hardware_profile_type': hardware_profile_type,
        'need_internal_phone': True,
        'need_sip_redirect': True,
    }
    form.update(update_data)

    request = rf.post(
        reverse('preprofile:create_from_femida'),
        json.dumps(form),
        content_type='application/json',
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    result = create_from_femida(request)

    result = json.loads(result.content)
    assert 'errors' not in result
    assert 'id' in result

    result_data = {
        'department': company.outstaff.id,
        'login': 'employee',
        'lang_ui': 'en',
        'femida_offer_id': 100500,
        'is_autohire': True,
        'hardware_profile_id': hardware_profile.id,
        'need_table': NEED_TABLE.NO,
        'status': PREPROFILE_STATUS.APPROVED,
        'need_internal_phone': True,
        'need_sip_redirect': True,
    }

    assert Preprofile.objects.count() == 1
    assert Preprofile.objects.values(*result_data.keys())[0] == result_data


@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_femida_submit_external_form(dns_mock, validate_in_ldap, rf, company, femida_robot):
    model = PreprofileFactory(
        department_id=company.yandex.id,
        login='olololo',
        candidate_type=CANDIDATE_TYPE.NEW_EMPLOYEE,
        form_type=FORM_TYPE.OUTSTAFF,
        status=PREPROFILE_STATUS.NEW,
    )

    request = rf.post(
        reverse('preprofile:femida_submit_external_form', kwargs={'preprofile_id': model.id}),
        json.dumps({
            'first_name': 'ooooooo',
            'last_name': 'oooooo',
            'middle_name': 'oooooo',
            'first_name_en': 'oooooo',
            'last_name_en': 'oooooo',
            'gender': 'M',
            'birthday': '2007-08-09',
            'photo': 'https:/ya.ru',
            'citizenship': 'RUSSIAN',
            'phone': '+79250475559',
            'email': 'wlame@yandex.ru',
            'address': 'sooo ooo ooo',
            'login': 'xxsdfsdfggfd',
            'join_at': '2018-08-31',
            'date_completion_internship': '2019-07-03',
        }),
        content_type='application/json',
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    with patch('staff.preprofile.controller_behaviours.femida_outstaff_external_behaviour.launch_person_avatar_task'):
        result = femida_submit_external_form(request, model.id)

    assert result.status_code == 200

    model = Preprofile.objects.get(id=model.id)
    assert model.login == 'xxsdfsdfggfd'
    assert model.status == PREPROFILE_STATUS.PREPARED
    assert model.date_completion_internship is None


@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_femida_submit_external_form_without_login_for_former_employee(
    dns_mock, validate_in_ldap, rf, company, femida_robot,
):
    target_person = company.persons['dep1-chief']
    target_person.is_dismissed = True
    target_person.save()
    model = PreprofileFactory(
        department_id=company.yandex.id,
        login=target_person.login,
        candidate_type=CANDIDATE_TYPE.FORMER_EMPLOYEE,
        form_type=FORM_TYPE.OUTSTAFF,
        status=PREPROFILE_STATUS.NEW,
    )

    request = rf.post(
        reverse('preprofile:femida_submit_external_form', kwargs={'preprofile_id': model.id}),
        json.dumps({  # without login
            'first_name': 'ooooooo',
            'last_name': 'oooooo',
            'middle_name': 'oooooo',
            'first_name_en': 'oooooo',
            'last_name_en': 'oooooo',
            'gender': 'M',
            'birthday': '2007-08-09',
            'photo': 'https:/ya.ru',
            'citizenship': 'RUSSIAN',
            'phone': '+79250475559',
            'email': 'wlame@yandex.ru',
            'address': 'sooo ooo ooo',
            'join_at': '2018-08-31',
            'date_completion_internship': '2019-07-03',
        }),
        content_type='application/json',
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    with patch('staff.preprofile.controller_behaviours.femida_outstaff_external_behaviour.launch_person_avatar_task'):
        result = femida_submit_external_form(request, model.id)

    assert result.status_code == 200

    model = Preprofile.objects.get(id=model.id)
    assert model.login == target_person.login
    assert model.status == PREPROFILE_STATUS.PREPARED


def test_femida_submit_external_form_without_login_for_former_employee_if_preprofile_has_no_login(
    rf, femida_robot, company,
):
    model = PreprofileFactory(
        department_id=company.yandex.id,
        candidate_type=CANDIDATE_TYPE.FORMER_EMPLOYEE,
        form_type=FORM_TYPE.OUTSTAFF,
        status=PREPROFILE_STATUS.NEW,
        login=None,
    )

    request = rf.post(
        reverse('preprofile:femida_submit_external_form', kwargs={'preprofile_id': model.id}),
        json.dumps({  # without login
            'first_name': 'ooooooo',
            'last_name': 'oooooo',
            'middle_name': 'oooooo',
            'first_name_en': 'oooooo',
            'last_name_en': 'oooooo',
            'lang_ui': 'en',
            'gender': 'M',
            'birthday': '2007-08-09',
            'photo': 'https:/ya.ru',
            'citizenship': 'RUSSIAN',
            'phone': '+79250475559',
            'email': 'wlame@yandex.ru',
            'address': 'sooo ooo ooo',
            'join_at': '2018-08-31',
            'date_completion_internship': '2019-07-03',
        }),
        content_type='application/json',
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    with patch('staff.preprofile.controller_behaviours.femida_outstaff_external_behaviour.launch_person_avatar_task'):
        result = femida_submit_external_form(request, model.id)

    assert result.status_code == 200

    model = Preprofile.objects.get(id=model.id)
    assert model.status == PREPROFILE_STATUS.PREPARED


@pytest.mark.django_db()
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_that_femida_can_change_join_date_after_creating_uid_or_guid(
    dns_mock, validate_in_ldap, rf, femida_robot, company,
):
    after_tomorrow = date.today() + timedelta(days=2)

    model = PreprofileFactory(
        department_id=company.yandex.id,
        femida_offer_id=1,
        login='olololo',
        candidate_type=CANDIDATE_TYPE.NEW_EMPLOYEE,
        uid='1110000003455',
        date_completion_internship=date.today(),
    )

    request = rf.post(
        femida_update_url(model.id),
        json.dumps({
            'join_at': after_tomorrow.isoformat(),
            'login': 'olololo',
            'candidate_type': CANDIDATE_TYPE.NEW_EMPLOYEE,
            'department': company.yandex.url,
            'date_completion_internship': after_tomorrow.isoformat(),
        }),
        content_type='application/json'
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    result = femida_update(request, model.id)
    assert result.status_code == 200

    model = Preprofile.objects.get(id=model.id)
    assert model.join_at == after_tomorrow
    assert model.date_completion_internship == after_tomorrow


@pytest.mark.django_db()
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_that_femida_can_change_candidate_type_but_cannot_change_login_after_creating_uid_or_guid(
    dns_mock, validate_in_ldap, rf, femida_robot, company,
):
    after_tomorrow = date.today() + timedelta(days=2)
    model = PreprofileFactory(
        department_id=company.yandex.id,
        femida_offer_id=1,
        login='olololo',
        candidate_type=CANDIDATE_TYPE.CURRENT_EMPLOYEE,
        uid='1110000003455',
    )

    request = rf.post(
        femida_update_url(model.id),
        json.dumps({
            'join_at': after_tomorrow.isoformat(),
            'login': 'olololo1',
            'candidate_type': CANDIDATE_TYPE.NEW_EMPLOYEE,
            'department': company.yandex.url,
        }),
        content_type='application/json'
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    result = femida_update(request, model.id)
    assert result.status_code == 200

    model = Preprofile.objects.get(id=model.id)

    assert model.candidate_type == CANDIDATE_TYPE.NEW_EMPLOYEE
    assert model.login == 'olololo'


@pytest.fixture
def create_rotation(rf, tester, femida_robot, company, base_form, femida_abc_services, red_rose_office, yndx_org):
    def creater(rotate_person, exclude_fields=None):
        base_form.update({
            'department': company.yandex.url,
            'abc_services': femida_abc_services,
            'office': red_rose_office.id,
            'login': rotate_person.login,
            'femida_offer_id': 100500,
            'organization': yndx_org.id,
            'recruiter': tester.staff.login,
            'candidate_type': CANDIDATE_TYPE.CURRENT_EMPLOYEE,
            'date_completion_internship': '2019-07-03',
        })
        for f in (exclude_fields or []):
            base_form.pop(f)

        request = rf.post(
            reverse('preprofile:create_from_femida'),
            json.dumps(base_form),
            content_type='application/json',
        )
        request.user = femida_robot
        set_client_application(request, 'femida')

        result = create_from_femida(request)
        return json.loads(result.content)

    return creater


@pytest.mark.django_db
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_rotation_form_can_be_saved_from_femida(
    dns_mock, validate_in_ldap, tester, company, create_rotation,
    femida_abc_services, red_rose_office, yndx_org,
):
    rotate_person = StaffFactory(birthday=date.today())

    result = create_rotation(rotate_person)
    assert 'errors' not in result
    assert 'id' in result

    result_data = {
        'department': company.yandex.id,
        'office': red_rose_office.id,
        'login': rotate_person.login,
        'femida_offer_id': 100500,
        'organization': yndx_org.id,
        'recruiter': tester.staff.id,

        'phone': rotate_person.mobile_phone,
        'email': rotate_person.home_email,
        'date_completion_internship': rotate_person.date_completion_internship,
        'abc_services': femida_abc_services[0],
    }
    same_fields_from_staff = [
        'first_name',
        'first_name_en',
        'middle_name',
        'last_name',
        'last_name_en',
        'birthday',
        'gender',
        'address',
        'date_completion_internship',
    ]
    for field in same_fields_from_staff:
        if field == 'date_completion_internship':
            # if result_data['date_completion_internship'] is None: preprofile - не стажер
            result_data[field] = getattr(rotate_person, field)
            continue
        result_data[field] = getattr(rotate_person, field) or ''

    assert Preprofile.objects.count() == 1
    assert Preprofile.objects.values(*result_data.keys())[0] == result_data


@pytest.mark.django_db
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_rotation_form_can_be_saved_from_femida_without_org_n_office(
    dns_mock,
    validate_in_ldap,
    create_rotation,
    yndx_org,
):
    rotate_person = StaffFactory(
        birthday=date.today(),
        organization=yndx_org,
        office=OfficeFactory(name='Shinra'),
    )

    result = create_rotation(rotate_person, exclude_fields=['organization', 'office'])
    assert 'errors' not in result
    assert 'id' in result
    created = Preprofile.objects.get(id=result['id'])
    assert created.organization == rotate_person.organization
    assert created.office == rotate_person.office


@pytest.mark.django_db
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_rotation_form_can_be_updated_from_femida(
    dns_mock, validate_in_ldap, rf, tester, company,
    create_rotation, femida_robot, robot_staff_user,
):
    rotate_person = StaffFactory(birthday=date.today())
    preprofile_id = create_rotation(rotate_person)['id']

    new_join_at = date.today() + timedelta(days=5)
    new_hr_ticket = 'HR-1234'
    new_adopt_ticket = 'ADOPT-1234'
    to_upd_data = {
        'department': company.dep1.url,
        'join_at': new_join_at.isoformat(),
        'hr_ticket': new_hr_ticket,
        'adopt_ticket': new_adopt_ticket,
        'date_completion_internship': date.today().isoformat(),
    }

    request = rf.post(
        reverse('preprofile:femida_update', kwargs={'preprofile_id': preprofile_id}),
        json.dumps(to_upd_data),
        content_type='application/json',
    )
    request.user = femida_robot
    set_client_application(request, 'femida')

    result = femida_update(request, preprofile_id)
    result = json.loads(result.content)
    assert 'errors' not in result

    expect_result = {
        'department': company.dep1.id,
        'join_at': new_join_at,
        'hr_ticket': new_hr_ticket,
        'adopt_ticket': new_adopt_ticket,
        'date_completion_internship': date.today(),
    }
    current_result = Preprofile.objects.filter(id=preprofile_id).values(*expect_result.keys()).get()
    assert current_result == expect_result


@pytest.mark.django_db
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_rotation_adopt(
    dns_mock,
    validate_in_ldap,
    rf,
    tester,
    company,
    create_rotation,
    femida_robot,
    robot_staff_user,
    mock_side_effects,
):
    rotate_person = StaffFactory(birthday=date.today())
    preprofile_id = create_rotation(rotate_person)['id']
    request = rf.post(
        reverse('preprofile:approve', kwargs={'preprofile_id': preprofile_id}),
        content_type='application/json',
    )
    request.user = tester
    approve(request, preprofile_id)
    Preprofile.objects.filter(id=preprofile_id).update(join_at=date.today())
    robot_staff_user.user_permissions.add(Permission.objects.get(codename='add_personadoptapplication'))

    AutoAdoptPreprofiles()

    preprofile = Preprofile.objects.get(id=preprofile_id)
    assert preprofile.adopted_by == robot_staff_user.staff
    same_fields = [
        'department',
        'office',
        'organization',
        'position',
    ]
    actual_person_info = Staff.objects.values(*same_fields).get(login=rotate_person.login)
    preprofile_info = Preprofile.objects.values('position_staff_text', *same_fields).get(id=preprofile_id)
    preprofile_info['position'] = preprofile_info.pop('position_staff_text')
    assert actual_person_info == preprofile_info


@pytest.mark.django_db
@patch('staff.person_profile.controllers.digital_sign.sms.send', lambda *a, **kw: 'ok')
@patch('staff.lib.decorators._check_service_id', lambda *a, **b: True)
def test_femida_ds_attach_phone(rf, femida_robot):
    data = {
        'object_type': VERIFY_CODE_OBJECT_TYPES.OFFER,
        'object_id': 100500,
        'phone_number': '+77021231231',
    }
    request = rf.post(
        reverse('preprofile:femida_ds_attach_phone'),
        json.dumps(data),
        content_type='application/json',
    )
    request.user = femida_robot
    request.yauser = None
    result = femida_ds_attach_phone(request)
    data['phone_number'] = normalize_phone(data['phone_number'])

    assert result.status_code == 200
    assert VerifyCode.objects.filter(state=VERIFY_STATE.WAIT, **data).count() == 1


@pytest.mark.django_db
@patch('staff.lib.decorators._check_service_id', lambda *a, **b: True)
@pytest.mark.parametrize('code, status_code, state', (
    ('xyz123', 200, VERIFY_STATE.VERIFIED),
    ('incorrect', 400, VERIFY_STATE.WAIT),
))
def test_femida_ds_verify_phone(rf, femida_robot, code, status_code, state):
    data = {
        'object_type': VERIFY_CODE_OBJECT_TYPES.OFFER,
        'object_id': 100500,
        'code': 'xyz123',
    }
    code_model = VerifyCode.objects.create(phone_number='+77021231231', **data)

    data['code'] = code
    request = rf.post(
        reverse('preprofile:femida_ds_verify_phone'),
        json.dumps(data),
        content_type='application/json',
    )
    request.user = femida_robot
    request.yauser = None
    result = femida_ds_verify_phone(request)
    code_model.refresh_from_db()

    assert result.status_code == status_code
    assert code_model.state == state
