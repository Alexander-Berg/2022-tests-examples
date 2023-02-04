from datetime import date, timedelta
from itertools import count

from mock import patch
import pytest

from django.conf import settings
from django.contrib.auth.models import Permission

from staff.lib.testing import StaffFactory
from staff.person.models import Staff, VerifyCode, VERIFY_CODE_OBJECT_TYPES, VERIFY_STATE

from staff.preprofile.models import FORM_TYPE, PREPROFILE_STATUS
from staff.preprofile.notifications import PreprofileNotification
from staff.preprofile.tasks import AutoAdoptPreprofiles, SendNotifies, PrepareDigitalSigns
from staff.preprofile.tests.utils import PreprofileFactory


@pytest.mark.django_db()
def test_automatic_robot_creation(company, stub_requests, robot_staff_user):
    virtual_office = company.offices['virtual']

    PreprofileFactory(
        department_id=company.yandex.id,
        femida_offer_id=1,
        login='robot-test',
        form_type=FORM_TYPE.ROBOT,
        status=PREPROFILE_STATUS.READY,
        guid='guid',
        uid='112',
        office=virtual_office,
    )

    robot_staff_user.user_permissions.add(Permission.objects.get(codename='add_personadoptapplication'))

    AutoAdoptPreprofiles()

    s = Staff.objects.get(login='robot-test')
    assert s.is_robot


@pytest.mark.django_db
def test_send_remind_notifications(company, monkeypatch):
    StaffFactory(login=settings.ROBOT_STAFF_LOGIN)
    today = date.today()
    simple_remind_date = today + timedelta(days=SendNotifies.SIMPLE_REMIND_DAYS_LEFT)
    urgent_remind_date = today + timedelta(days=SendNotifies.URGENT_REMIND_DAYS_LEFT)
    forgotten_date = today - timedelta(days=1)
    id_gen = count()

    def create_preprofile(**kwargs):
        cur_id = next(id_gen)
        preprofile_kwargs = dict(
            department_id=company.yandex.id,
            femida_offer_id=cur_id,
            login='just_random_login_{}'.format(cur_id),
            form_type=FORM_TYPE.EMPLOYEE,
            status=PREPROFILE_STATUS.NEW,
            guid='guid_{}'.format(cur_id),
            uid='11{}'.format(cur_id),
        )
        preprofile_kwargs.update(kwargs)
        return PreprofileFactory(**preprofile_kwargs)

    expected_contexts_result = [
        dict(preprofile=create_preprofile(join_at=simple_remind_date), is_urgent=False),
        dict(preprofile=create_preprofile(join_at=urgent_remind_date), is_urgent=True),
        dict(preprofile=create_preprofile(join_at=forgotten_date)),
    ]
    not_remind_date = today + timedelta(
        days=SendNotifies.SIMPLE_REMIND_DAYS_LEFT + SendNotifies.URGENT_REMIND_DAYS_LEFT
    )
    not_remind = [
        dict(join_at=not_remind_date),
        dict(join_at=simple_remind_date, status=PREPROFILE_STATUS.READY),
        dict(join_at=urgent_remind_date, form_type=FORM_TYPE.ROBOT),
    ]
    for kw in not_remind:
        create_preprofile(**kw)

    def mocked_send(self, *args, **kwargs):
        return self.context

    monkeypatch.setattr(PreprofileNotification, 'send', mocked_send)

    result = SendNotifies()
    assert len(result) == 3 and all(ctx in result for ctx in expected_contexts_result)


@pytest.mark.django_db
@patch('staff.person_profile.controllers.digital_sign._connect_phone_in_oebs', return_value=(True, ''))
@pytest.mark.parametrize('femida_offer_id', (123, None))
@pytest.mark.parametrize('status, days_from_today, called', (
    (PREPROFILE_STATUS.READY, 1, True),
    (PREPROFILE_STATUS.APPROVED, 1, False),
    (PREPROFILE_STATUS.READY, 0, False),
    (PREPROFILE_STATUS.READY, 2, False),
))
def test_prepare_digital_signs(mock_connect, company, femida_offer_id, status, days_from_today, called):
    code_types = VERIFY_CODE_OBJECT_TYPES
    StaffFactory(
        login='robot-femida',
        user__username='robot-femida',
    )
    preprofile = PreprofileFactory(
        department_id=company.yandex.id,
        join_at=date.today() + timedelta(days_from_today),
        status=status,
        femida_offer_id=femida_offer_id,
        phone='+7 700 111 2223',
        login='zorrro',
        is_eds_phone_verified=True,
    )
    code = VerifyCode.objects.create(
        object_type=code_types.OFFER if femida_offer_id else code_types.PREPROFILE,
        object_id=femida_offer_id or preprofile.id,
        state=VERIFY_STATE.VERIFIED,
        phone_number=preprofile.phone,
    )

    PrepareDigitalSigns()
    assert mock_connect.called is called
    if called:
        mock_connect.assert_called_once_with('zorrro', '+7 700 111 2223')

    code.refresh_from_db()
    assert code.state == VERIFY_STATE.VERIFIED
