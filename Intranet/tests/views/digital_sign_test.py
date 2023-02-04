import datetime
import factory
import json
import pytest
from mock import patch

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.audit.models import Log
from staff.audit.factory import AUDIT_ACTIONS
from staff.lib.testing import (
    get_sform_err_by_field,
    get_sform_general_err,
    CityFactory,
    CountryFactory,
    OrganizationFactory,
    StaffFactory,
    StaffPhoneFactory,
)
from staff.lib import sms, attr_ext
from staff.person_profile.controllers import digital_sign as ds_controllers
from staff.person_profile.views import (
    digital_sign,
    meta_view,
)
from staff.person.models import (
    StaffPhone,
    VerifyCode,
    VERIFY_STATE,
)

TESTER_LOGIN = 'tester'
COUNTRY_WITH_DIGITAL_SIGN = settings.DIGITAL_SIGN_COUNTRIES[0]
COUNTRY_WITHOUT_DIGITAL_SIGN = 'rc'
assert COUNTRY_WITHOUT_DIGITAL_SIGN not in settings.DIGITAL_SIGN_COUNTRIES


class VerifyCodeFactory(factory.DjangoModelFactory):

    object_id = factory.LazyAttribute(lambda obj: obj.person and obj.person.id)
    phone_number = factory.LazyAttribute(lambda obj: obj.phone and obj.phone.number)

    class Meta:
        model = VerifyCode


@pytest.fixture
def tester():
    country = CountryFactory(code=COUNTRY_WITH_DIGITAL_SIGN)
    tester = StaffFactory(
        login=TESTER_LOGIN,
        organization=OrganizationFactory(city=CityFactory(country=country))
    )
    return tester


@pytest.fixture
def get_request(rf, tester):
    def inner(to_reverse):
        request = rf.get(reverse(to_reverse, kwargs={'login': tester.login}))
        request.user = tester.user
        setattr(request, 'service_is_readonly', False)
        return request, tester.login
    return inner


@pytest.fixture
def post_request(rf, tester):
    def inner(to_reverse, post_kwargs):
        request = rf.post(
            reverse(to_reverse, kwargs={'login': tester.login}),
            json.dumps(post_kwargs),
            'application/json',
        )
        request.user = tester.user
        setattr(request, 'service_is_readonly', False)
        return request, tester.login
    return inner


@pytest.mark.django_db
@pytest.mark.parametrize(
    'country,expect_access', [
        (COUNTRY_WITH_DIGITAL_SIGN, True),
        (COUNTRY_WITH_DIGITAL_SIGN.lower(), True),
        (COUNTRY_WITH_DIGITAL_SIGN.upper(), True),
        ('rc', False),
    ]
)
def test_access_to_digital_sign(
    rf,
    monkeypatch,
    tester,
    country,
    expect_access,
):
    tester.save()
    tester.organization.country_code = country
    tester.organization.save()
    request = rf.get(reverse('profile:meta', kwargs={'login': tester.login}))
    request.user = tester.user
    setattr(request, 'service_is_readonly', False)
    response = meta_view.meta(request, TESTER_LOGIN)
    pencils = json.loads(response.content).get('pencils', {})
    assert expect_access == pencils.get('edit_digital_sign', False)


@pytest.mark.django_db
def test_get_no_permissions(get_request, monkeypatch):
    with patch('staff.person_profile.views.digital_sign.can_view_digital_sign', return_value=False) as perms:
        params = get_request('profile:digital_sign')
        response = digital_sign.get(*params)
        assert response.status_code == 403
        perms.assert_called_once_with(None, params[0].permissions_ctl.properties, params[1])


@pytest.mark.django_db
def test_get_empty(get_request, monkeypatch):
    has_signed_agreement = False
    monkeypatch.setattr(
        digital_sign,
        'check_agreement_sign',
        lambda *a, **kw: has_signed_agreement,
    )
    params = get_request('profile:digital_sign')

    expected_answer = {'target': {'digital_sign': {
        'phone': None,
        'has_signed_agreement': has_signed_agreement
    }}}

    with patch('staff.person_profile.views.digital_sign.can_view_digital_sign', return_value=True) as perms:
        response = digital_sign.get(*params)
        assert json.loads(response.content) == expected_answer
        perms.assert_called_once_with(None, params[0].permissions_ctl.properties, params[1])


@pytest.mark.django_db
def test_get_connected(get_request, tester, monkeypatch):
    has_signed_agreement = True
    monkeypatch.setattr(
        digital_sign,
        'check_agreement_sign',
        lambda *a, **kw: has_signed_agreement,
    )
    phone = StaffPhoneFactory(
        staff=tester,
        for_digital_sign=True,
        number='+7 999 666-00-01',
    )

    expected_answer = {'target': {'digital_sign': {
        'phone': {
            'number': phone.number,
            'kind': phone.kind,
            'protocol': phone.protocol,
            'description': phone.description,
            'id': phone.id,
            'for_digital_sign': True,
        },
        'has_signed_agreement': has_signed_agreement,
    }}}
    params = get_request('profile:digital_sign')

    with patch('staff.person_profile.views.digital_sign.can_view_digital_sign', return_value=True) as perms:
        response = digital_sign.get(*params)
        assert json.loads(response.content) == expected_answer
        perms.assert_called_once_with(None, params[0].permissions_ctl.properties, params[1])


@pytest.mark.django_db
def test_attach_phone_get_form(get_request):
    response = digital_sign.attach_phone(*get_request('profile:digital_sign_attach_phone'))
    assert response.status_code == 200


@pytest.mark.django_db
def test_verify_code_get_form(get_request):
    response = digital_sign.verify_code(*get_request('profile:digital_sign_verify_code'))
    assert response.status_code == 200


@pytest.mark.django_db
def test_attach_phone_wrong_form(post_request):
    response = digital_sign.attach_phone(*post_request('profile:digital_sign_attach_phone', {'anyfield': 1}))
    assert response.status_code == 400


@pytest.mark.django_db
def test_verify_code_wrong_form(post_request):
    response = digital_sign.verify_code(*post_request('profile:digital_sign_verify_code', {'anyfield': 1}))
    assert response.status_code == 400


@pytest.mark.django_db
def test_attach_phone_from_another_person(post_request):
    phone = StaffPhoneFactory()
    response = digital_sign.attach_phone(*post_request(
        'profile:digital_sign_attach_phone',
        {'phone': phone.id},
    ))
    err_resp = get_sform_err_by_field(json.loads(response.content), 'phone')
    assert digital_sign.ERRS.PHONE_NOT_FOR_CUR_USER == err_resp.code


@pytest.mark.django_db
def test_reattach_new_phone(post_request, tester, monkeypatch):
    sms_texts = []

    def send_sms(number, text):
        sms_texts.append(text)
        return sms.RESPONSES.OK

    monkeypatch.setattr(sms, 'send', send_sms)
    monkeypatch.setattr(ds_controllers, '_connect_phone_in_oebs', lambda *a, **kw: (True, ''))
    old_phone = StaffPhoneFactory(
        staff=tester,
        for_digital_sign=True,
    )
    new_phone = StaffPhoneFactory(
        staff=tester,
        for_digital_sign=False,
    )

    # run transaction to connect new phone
    digital_sign.attach_phone(*post_request(
        'profile:digital_sign_attach_phone',
        {'phone': new_phone.id},
    ))
    # verify new phone
    code_model = VerifyCode.objects.get(phone=new_phone)
    digital_sign.verify_code(*post_request(
        'profile:digital_sign_verify_code',
        {'code': code_model.code},
    ))

    assert StaffPhone.objects.get(id=new_phone.id).for_digital_sign, 'New phone not connected'
    assert not StaffPhone.objects.get(id=old_phone.id).for_digital_sign, 'Old phone not unconnected'


@pytest.mark.django_db
def test_attach_phone_send_sms_ok(post_request, tester, monkeypatch):
    phone = StaffPhoneFactory(staff=tester)
    monkeypatch.setattr(sms, 'send', lambda *a, **kw: sms.RESPONSES.OK)
    digital_sign.attach_phone(*post_request(
        'profile:digital_sign_attach_phone',
        {'phone': phone.id},
    ))
    assert VerifyCode.objects.filter(
        phone=phone,
        person=tester,
        state=VERIFY_STATE.WAIT,
    ).count() == 1


@pytest.mark.django_db
def test_attach_phone_writes_audit_log(post_request, tester, monkeypatch):
    phone = StaffPhoneFactory(staff=tester)
    monkeypatch.setattr(sms, 'send', lambda *a, **kw: sms.RESPONSES.OK)
    request, login = post_request(
        'profile:digital_sign_attach_phone',
        {'phone': phone.id},
    )
    digital_sign.attach_phone(request, login)
    logs = list(Log.objects.filter(
        who=request.user,
        primary_key=phone.staff_id,
        action=AUDIT_ACTIONS.ATTACH_PHONE_TO_DS,
    ))
    assert len(logs) == 1
    assert json.loads(logs[0].data) == {'status': sms.RESPONSES.OK, 'object_type': 'staff'}


@pytest.mark.django_db
def test_attach_phone_send_sms_error(post_request, tester, monkeypatch):
    phone = StaffPhoneFactory(staff=tester)
    monkeypatch.setattr(sms, 'send', lambda *a, **kw: sms.RESPONSES.UNKNOWN_ERROR)
    digital_sign.attach_phone(*post_request(
        'profile:digital_sign_attach_phone',
        {'phone': phone.id},
    ))
    assert not VerifyCode.objects.filter(
        phone=phone,
        person=tester,
        state=VERIFY_STATE.WAIT,
    ).exists()


@pytest.mark.django_db
def test_attach_phone_already_attaching(post_request, tester, monkeypatch):
    phone = StaffPhoneFactory(staff=tester)
    monkeypatch.setattr(sms, 'send', lambda *a, **kw: sms.RESPONSES.OK)
    existing_code = VerifyCodeFactory(phone=phone, person=tester)
    digital_sign.attach_phone(*post_request(
        'profile:digital_sign_attach_phone',
        {'phone': phone.id},
    ))
    created_code = VerifyCodeFactory(
        phone=phone,
        person=tester,
        state=VERIFY_STATE.WAIT,
    )
    assert existing_code.id != created_code.id


@pytest.mark.django_db
def test_verify_code_no_wait_attachment(post_request):
    response = digital_sign.verify_code(*post_request('profile:digital_sign_verify_code', {'code': 1}))
    err_resp = get_sform_general_err(json.loads(response.content))
    assert digital_sign.VERIFY_ANSWERS.NO_WAIT_ATTACHMENT == err_resp.code


@pytest.mark.django_db
def test_verify_code_writes_audit_log(post_request):
    request, login = post_request('profile:digital_sign_verify_code', {'code': 1})
    response = digital_sign.verify_code(request, login)
    err_resp = get_sform_general_err(json.loads(response.content))
    assert Log.objects.filter(
        who=request.user,
        primary_key=None,
        action=AUDIT_ACTIONS.VERIFY_PHONE_CODE_DS,
        data=json.dumps({'status': err_resp.code}),
    ).exists()


def _assert_verify_failed(code_model_id):
    code_model = VerifyCode.objects.get(id=code_model_id)
    assert code_model.state == VERIFY_STATE.FAILED
    assert not code_model.phone.for_digital_sign


@pytest.mark.django_db
def test_verify_code_incorrect_code(post_request, tester):
    phone = StaffPhoneFactory(staff=tester)
    VerifyCodeFactory(phone=phone, person=tester, code='123456')
    response = digital_sign.verify_code(*post_request(
        'profile:digital_sign_verify_code',
        {'code': 'asdfgh'},
    ))

    assert not StaffPhone.objects.get(id=phone.id).for_digital_sign
    err_resp = get_sform_general_err(json.loads(response.content))
    assert digital_sign.VERIFY_ANSWERS.INCORRECT_CODE == err_resp.code


@pytest.mark.django_db
def test_verify_code_max_validate_exceeded(post_request, tester):
    phone = StaffPhoneFactory(staff=tester)
    right_code = '123456'
    VerifyCodeFactory(phone=phone, person=tester, code=right_code)
    for _ in range(settings.MAX_VALIDATE_TRIES):
        digital_sign.verify_code(*post_request(
            'profile:digital_sign_verify_code',
            {'code': 'asdfgh'},
        ))
    response = digital_sign.verify_code(*post_request(
        'profile:digital_sign_verify_code',
        {'code': right_code},
    ))

    assert not StaffPhone.objects.get(id=phone.id).for_digital_sign
    err_resp = get_sform_general_err(json.loads(response.content))
    assert digital_sign.VERIFY_ANSWERS.MAX_VALIDATE_TRIES_EXCEEDED == err_resp.code


@pytest.mark.django_db
def test_verify_code_expired_transaction(post_request, tester):
    phone = StaffPhoneFactory(staff=tester)
    code_model = VerifyCodeFactory(phone=phone, person=tester, code=1234)
    code_model.created_at = datetime.datetime(2000, 1, 1)
    code_model.save()
    response = digital_sign.verify_code(*post_request(
        'profile:digital_sign_verify_code',
        {'code': code_model.code},
    ))

    code_model = VerifyCode.objects.get(id=code_model.id)
    assert code_model.state == VERIFY_STATE.FAILED
    assert not code_model.phone.for_digital_sign
    err_resp = get_sform_general_err(json.loads(response.content))
    assert digital_sign.VERIFY_ANSWERS.EXPIRED == err_resp.code


@pytest.mark.django_db
def test_verify_code_oebs_error(post_request, tester, monkeypatch):
    phone = StaffPhoneFactory(staff=tester)
    code_model = VerifyCodeFactory(phone=phone, person=tester, code=1234)
    oebs_error_text = 'OEBS error text'
    monkeypatch.setattr(ds_controllers, '_connect_phone_in_oebs', lambda *a, **kw: (False, oebs_error_text))
    response = digital_sign.verify_code(*post_request(
        'profile:digital_sign_verify_code',
        {'code': code_model.code},
    ))

    assert not StaffPhone.objects.get(id=phone.id).for_digital_sign
    err_resp = get_sform_general_err(json.loads(response.content))
    assert digital_sign.VERIFY_ANSWERS.OEBS_ERROR == err_resp.code
    assert oebs_error_text == err_resp.params['message']


@pytest.mark.django_db
def test_verify_code_ok(post_request, tester, monkeypatch):
    phone = StaffPhoneFactory(staff=tester)
    code_model = VerifyCodeFactory(phone=phone, person=tester, code=1234)
    monkeypatch.setattr(ds_controllers, '_connect_phone_in_oebs', lambda *a, **kw: (True, ''))
    response = digital_sign.verify_code(*post_request(
        'profile:digital_sign_verify_code',
        {'code': code_model.code},
    ))

    code_model = VerifyCode.objects.get(id=code_model.id)
    assert json.loads(response.content) == {}
    assert code_model.state == VERIFY_STATE.CONFIRMED
    assert code_model.phone.for_digital_sign


TEST_CASES_FOR_DIGITAL_SIGN_STATUS = [
    {
        'oebs_resp': {
            'proc': [{'doc': [{'deadline': '2018-10-28T18:10:45Z'}]}],
            'unsignedDssDocCount': 1,
        },
        'expected_result': {'target': {'digital_sign_status': {
            'closest_deadline': '2018-10-28T18:10:45Z',
            'unsigned_status': 1,
        }}},
        'err': 'Unparsed correct datetime',
    },
    {
        'oebs_resp': {},
        'expected_result': {'target': {'digital_sign_status': {
            'closest_deadline': None,
            'unsigned_status': None,
        }}},
        'err': 'Empty oebs answer fail',
    },
    {
        'oebs_resp': {
            'proc': [{'doc': [{'deadline': '2018-10-28T18:10:45Z'}]}],
        },
        'expected_result': {'target': {'digital_sign_status': {
            'closest_deadline': '2018-10-28T18:10:45Z',
            'unsigned_status': None,
        }}},
        'err': 'Unexisting unsignedDssDocCount fail',
    },
    {
        'oebs_resp': {
            'unsignedDssDocCount': 1,
        },
        'expected_result': {'target': {'digital_sign_status': {
            'closest_deadline': None,
            'unsigned_status': 1,
        }}},
        'err': 'Unexisting proc fail',
    },
    {
        'oebs_resp': {
            'proc': [{}],
            'unsignedDssDocCount': 1,
        },
        'expected_result': {'target': {'digital_sign_status': {
            'closest_deadline': None,
            'unsigned_status': 1,
        }}},
        'err': 'Unexisting key "doc" fail',
    },
    {
        'oebs_resp': {
            'proc': [{'doc': [{}]}],
            'unsignedDssDocCount': 1,
        },
        'expected_result': {'target': {'digital_sign_status': {
            'closest_deadline': None,
            'unsigned_status': 1,
        }}},
        'err': 'Doc without deadline field fail',
    },
    {
        'oebs_resp': {
            'proc': [{'doc': [{'deadline': 'asdgf'}]}],
            'unsignedDssDocCount': 1,
        },
        'expected_result': {'target': {'digital_sign_status': {
            'closest_deadline': None,
            'unsigned_status': 1,
        }}},
        'err': 'Incorrect deadline format fail',
    },
    {
        'oebs_resp': {
            'proc': [{'doc': [{'deadline': None}]}],
            'unsignedDssDocCount': 1,
        },
        'expected_result': {'target': {'digital_sign_status': {
            'closest_deadline': None,
            'unsigned_status': 1,
        }}},
        'err': 'Incorrect deadline type fail',
    },
]


@pytest.mark.parametrize(
    'test_case', TEST_CASES_FOR_DIGITAL_SIGN_STATUS
)
@pytest.mark.django_db
def test_digital_sign_status(get_request, monkeypatch, test_case):
    oebs_resp = ds_controllers.OebsDocsStatus(**test_case['oebs_resp'])
    expected_result = test_case['expected_result']
    err = test_case['err']
    monkeypatch.setattr(
        digital_sign,
        'get_docs_status_from_oebs',
        lambda *a, **kw: oebs_resp,
    )
    response = digital_sign.get_status(*get_request('profile:digital_sign_status'))
    assert expected_result == json.loads(response.content), err


@pytest.mark.django_db
def test_digital_sign_oebs_err(get_request, monkeypatch):
    def mock(*args, **kwargs):
        raise Exception('asd')
    monkeypatch.setattr(digital_sign, 'get_docs_status_from_oebs', mock)

    response = digital_sign.get_status(*get_request('profile:digital_sign_status'))
    assert {'error': 'oebs_error'} == json.loads(response.content)


@pytest.mark.django_db
def test_digital_sign_certification_status_ok(get_request):
    oebs_data = {
        'existsCertificate': 'Да',
        'hasEmpCert': 'Нет',
        'ticket': 'TEST-1',
        'ticketForReissue': 'TEST-2',
        'period': {
            'effectiveStartDate': '2021-01-01',
            'effectiveEndDate': '2022-02-02',
        },
    }
    expected_result = {
        'target': {
            'digital_sign_certification_status': {
                'exists': True,
                'active': False,
                'ticket': 'TEST-1',
                'reissue_ticket': 'TEST-2',
                'effective_start_date': '2021-01-01',
                'effective_end_date': '2022-02-02',
            },
        },
    }
    oebs_resp = attr_ext.from_kwargs(ds_controllers.DSSCertificationStatus, **oebs_data)
    with patch(
            'staff.person_profile.views.digital_sign.get_digital_sign_certification_status_from_oebs',
            return_value=oebs_resp
    ):
        response = digital_sign.get_certification_status(*get_request('profile:digital_sign_certification_status'))
    assert json.loads(response.content) == expected_result


@pytest.mark.django_db
def test_digital_sign_certification_status_no_certificate(get_request):
    oebs_data = {
        'existsCertificate': 'Нет',
        'hasEmpCert': 'Нет',
    }
    expected_result = {
        'target': {
            'digital_sign_certification_status': {
                'exists': False,
                'active': False,
                'ticket': None,
                'reissue_ticket': None,
                'effective_start_date': None,
                'effective_end_date': None,
            },
        },
    }
    oebs_resp = attr_ext.from_kwargs(ds_controllers.DSSCertificationStatus, **oebs_data)
    with patch(
            'staff.person_profile.views.digital_sign.get_digital_sign_certification_status_from_oebs',
            return_value=oebs_resp
    ):
        response = digital_sign.get_certification_status(*get_request('profile:digital_sign_certification_status'))
    assert json.loads(response.content) == expected_result
