from datetime import date, timedelta
import pytest

from staff.dismissal.models import Dismissal, DISMISSAL_STATUS
from staff.dismissal.objects import DismissalCtl

from .factories import (
    DismissalFactory,
    CheckPointFactory,
    ClearanceChitTemplateFactory,
    CheckPointTemplateFactory
)


@pytest.mark.django_db
def test_create_disabled_dismissal(groups_and_user):
    dismissal = DismissalCtl(author=groups_and_user.hr_user.staff).create(staff=groups_and_user.staff)

    assert dismissal.staff == groups_and_user.staff
    assert dismissal.department == groups_and_user.yandex
    assert dismissal.office == groups_and_user.morozov
    assert dismissal.created_by == groups_and_user.hr
    assert dismissal.intranet_status == 0


@pytest.mark.django_db
def test_create_enabled_dismissal(groups_and_user):
    dismissal = DismissalCtl(author=groups_and_user.hr_user.staff).create(staff=groups_and_user.staff, enable=True)

    assert dismissal.intranet_status == 1


@pytest.mark.django_db
def test_update(groups_and_user):
    dismissal = DismissalFactory(
        staff=groups_and_user.staff,
        office=groups_and_user.morozov,
        department=groups_and_user.yandex,
        created_by=groups_and_user.hr,
        status='I', quit_datetime_real=None
    )

    cleaned_data = {
        'quit_date': date.today(),
        'note': 'hello, hr',
        'delete_email_address': True,
        'delete_correspondence': False,
        'forward_correspondence_to': groups_and_user.hr,
        'forward_correspondence_to__id': groups_and_user.hr.id,
        'give_correspondence_to': None,
        'give_correspondence_to__id': None,
        'delete_files': False,
        'keep_files': True,
        'give_files_to': None,
        'give_files_to__id': None,

        'delete_from_search': '',
        'comment': '',
        'initiator': '',
        'need_hr_help': False,
        'reason': '',
        'new_employer': '',
        'impression': '',
        'rehire_recommendation': '',
    }
    ctl = DismissalCtl(author=groups_and_user.hr_user.staff, dismissal=dismissal)
    ctl.update(cleaned_data, enable=True)
    dismissal = Dismissal.objects.get()

    assert dismissal.intranet_status == 1
    for attr, value in cleaned_data.items():
        if not attr.endswith('__id'):
            assert getattr(dismissal, attr) == value


@pytest.mark.django_db
def test_get_or_create(groups_and_user):
    DismissalFactory(
        staff=groups_and_user.staff,
        office=groups_and_user.morozov,
        department=groups_and_user.yandex,
        created_by=groups_and_user.hr,
        status=DISMISSAL_STATUS.DONE
    )
    DismissalFactory(
        staff=groups_and_user.staff,
        office=groups_and_user.morozov,
        department=groups_and_user.yandex,
        created_by=groups_and_user.hr,
        status=DISMISSAL_STATUS.DONE
    )
    active_dismissal = DismissalFactory(
        staff=groups_and_user.staff,
        office=groups_and_user.morozov,
        department=groups_and_user.yandex,
        created_by=groups_and_user.hr,
        status=DISMISSAL_STATUS.IN_PROGRESS
    )

    dismissal = DismissalCtl().get_or_create(staff=groups_and_user.staff)
    assert dismissal == active_dismissal


@pytest.mark.django_db
def test_dismiss(groups_and_user):
    dismissal = DismissalFactory(
        staff=groups_and_user.staff,
        office=groups_and_user.morozov,
        department=groups_and_user.yandex,
        created_by=groups_and_user.hr
    )

    ctl = DismissalCtl(author=groups_and_user.hr_user.staff, dismissal=dismissal)
    ctl.complete(by_whom=groups_and_user.hr)

    dismissal = Dismissal.objects.get()
    assert dismissal.staff.is_dismissed
    assert dismissal.staff.quit_at is not None
    assert dismissal.status == DISMISSAL_STATUS.DONE


@pytest.mark.django_db
def test_dismissal_status_change(groups_and_user):
    dismissal = DismissalFactory(
        staff=groups_and_user.staff,
        office=groups_and_user.morozov,
        department=groups_and_user.yandex,
        created_by=groups_and_user.hr
    )
    cp = CheckPointFactory(dismissal=dismissal, checked=False)

    ctl = DismissalCtl(author=groups_and_user.hr_user.staff, dismissal=dismissal)
    ctl.update_status()

    assert Dismissal.objects.get().status == DISMISSAL_STATUS.NEW

    groups_and_user.staff.is_dismissed = True
    groups_and_user.staff.save()
    ctl.update_status()

    assert Dismissal.objects.get().status == DISMISSAL_STATUS.CHIT_NOT_COMPLETE

    cp.checked = True
    cp.save()
    ctl.update_status()
    assert Dismissal.objects.get().status == DISMISSAL_STATUS.DONE


@pytest.mark.django_db
def test_create_clearance_chit(groups_and_user):
    cp_tpl_1 = CheckPointTemplateFactory()
    cp_tpl_1.responsibles.add(groups_and_user.staff)
    cp_tpl_1.responsibles.add(groups_and_user.hr)
    cp_tpl_1.save()

    cp_tpl_2 = CheckPointTemplateFactory()
    cp_tpl_2.responsibles.add(groups_and_user.staff)
    cp_tpl_2.save()

    cctf = ClearanceChitTemplateFactory(department=groups_and_user.yandex, office=None)
    cctf.checkpoints.add(cp_tpl_1)
    cctf.checkpoints.add(cp_tpl_2)
    cctf.save()

    dismissal = DismissalFactory(
        staff=groups_and_user.staff,
        office=groups_and_user.morozov,
        department=groups_and_user.yandex,
        created_by=groups_and_user.hr,
    )

    DismissalCtl(dismissal=dismissal)._create_clearance_chit()
    checkpoints = Dismissal.objects.get().checkpoints.all()
    assert len(checkpoints) == 2


@pytest.mark.django_db
def test_moving_quit_date_to_future_resets_status(groups_and_user):
    dismissal = DismissalFactory(
        staff=groups_and_user.staff,
        office=groups_and_user.morozov,
        department=groups_and_user.yandex,
        created_by=groups_and_user.hr,
        status=DISMISSAL_STATUS.DATE_PASSED,
        quit_datetime_real=None,
        quit_date=date.today() - timedelta(days=7),
        deadline=date.today() - timedelta(days=7),
    )

    cleaned_data = {
        'quit_date': date.today() + timedelta(days=7),
        'deadline': date.today() + timedelta(days=7),
        'move_to_ext': False,
        'note': '',
        'delete_email_address': True,
        'delete_correspondence': False,
        'forward_correspondence_to': groups_and_user.hr,
        'forward_correspondence_to__id': groups_and_user.hr.id,
        'give_correspondence_to': None,
        'give_correspondence_to__id': None,
        'delete_files': False,
        'keep_files': True,
        'give_files_to': None,
        'delete_from_search': '',
        'comment': '',
        'initiator': '',
        'need_hr_help': False,
        'reason': '',
        'new_employer': '',
        'impression': '',
        'rehire_recommendation': '',
    }
    ctl = DismissalCtl(author=groups_and_user.hr_user.staff, dismissal=dismissal)
    ctl.update(cleaned_data, enable=True)

    dismissal = Dismissal.objects.get()

    assert dismissal.status == DISMISSAL_STATUS.IN_PROGRESS
