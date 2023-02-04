from datetime import datetime, timedelta, date

import pytest

from staff.gap.workflows.utils import find_workflow
from staff.lib.testing import StaffFactory
from staff.map.models import COUNTRY_CODES

from staff.person.models import StaffExtraFields, Staff
from staff.person.tasks.vacation_accrual import UpdateVacationForBelarus

today = date.today()
today_dt = datetime(today.year, today.month, today.day)


@pytest.mark.django_db
def test_vacation_accrual(map_models, mocked_mongo):

    # Сотрудник 1, которому уже пора начислить отпускные (т.к. прошло 15 дней)
    person1 = StaffFactory(vacation=5, office=map_models['offices']['MRP'])
    extra1 = StaffExtraFields.objects.create(staff=person1)
    extra1.last_vacation_accrual_at = today-timedelta(days=15)
    extra1.save()

    # Сотрудник 2, которому пока еще рано начислять (не прошло 15 дней)
    person2 = StaffFactory(vacation=5, office=map_models['offices']['MRP'])
    extra2 = StaffExtraFields.objects.create(staff=person2)
    extra2.last_vacation_accrual_at = today-timedelta(days=14)
    extra2.save()

    assert person1.office.get_country_code() == COUNTRY_CODES.BELARUS
    assert person2.office.get_country_code() == COUNTRY_CODES.BELARUS

    UpdateVacationForBelarus(nolock=True)

    person1 = Staff.objects.get(pk=person1.pk)
    person2 = Staff.objects.get(pk=person2.pk)
    assert person1.vacation > 5.
    assert person2.vacation == 5.


@pytest.mark.django_db
def test_vacation_accrual_with_maternity_leave(gap_test, map_models):

    # Сотруднику пора начислить отпускные (т.к. прошло 15 дней)
    person = StaffFactory(vacation=5, office=map_models['offices']['MRP'])
    extra = StaffExtraFields.objects.create(
        staff=person
    )
    extra.last_vacation_accrual_at = today - timedelta(days=15)
    extra.save()

    MaternityWorkflow = find_workflow('maternity')
    base_gap = gap_test.get_base_gap(MaternityWorkflow)

    base_gap['date_from'] = today_dt - timedelta(days=15)
    base_gap['date_to'] = today_dt
    MaternityWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        person.id
    ).new_gap(base_gap)

    UpdateVacationForBelarus(nolock=True)

    # Сотрудник был полные 15 дней в отпуске по уходу за ребенком
    # В итоге у него нет отработанных дней
    person = Staff.objects.get(pk=person.pk)
    assert person.vacation == 5


@pytest.mark.django_db
def test_vacation_accrual_with_absences(map_models, gap_test):

    # Сотруднику пора начислить отпускные (т.к. прошло 15 дней)
    person = StaffFactory(vacation=5, office=map_models['offices']['MRP'])
    extra = StaffExtraFields.objects.create(staff=person)
    extra.last_vacation_accrual_at = today - timedelta(days=15)
    extra.save()

    AbsenceWorkflow = find_workflow('absence')
    base_gap = gap_test.get_base_gap(AbsenceWorkflow)

    base_gap['work_in_absence'] = False
    base_gap['full_day'] = True
    base_gap['date_from'] = today_dt - timedelta(days=15)
    base_gap['date_to'] = today_dt - timedelta(days=1)
    AbsenceWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        person.id,
    ).new_gap(base_gap)

    UpdateVacationForBelarus(nolock=True)

    # Сотрудник был полные 15 дней в отпуске по уходу за ребенком
    # В итоге у него нет отработанных дней
    person = Staff.objects.get(pk=person.pk)
    assert person.vacation == 5


@pytest.mark.django_db
def test_vacation_changes_when_gap_created_and_modified(map_models, gap_test):

    # Сотруднику пока не надо начислять отпускные (не прошло 15 дней)
    # У него накопилось 5 дней
    person = StaffFactory(vacation=5, office=map_models['offices']['MRP'])
    StaffExtraFields.objects.create(staff=person)

    VacationWorkflow = find_workflow('vacation')
    base_gap = gap_test.get_base_gap(VacationWorkflow)

    base_gap['date_from'] = today_dt + timedelta(days=4)
    base_gap['date_to'] = today_dt + timedelta(days=7)
    gap_info = VacationWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        person.id
    ).new_gap(base_gap)

    # Сотрудник взял 4 дня отпускных
    # Остается 5-4=1
    person = Staff.objects.get(pk=person.pk)
    assert person.vacation == 1

    base_gap['date_from'] = today_dt + timedelta(days=6)
    base_gap['date_to'] = today_dt + timedelta(days=7)
    VacationWorkflow.init_to_modify(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_id=gap_info['id']
    ).edit_gap(base_gap)

    # Сотрудник изменил дату начала своего отпуска
    # следовательно изменилась длительность
    person = Staff.objects.get(pk=person.pk)
    assert person.vacation == 3


@pytest.mark.django_db
def test_vacation_changes_when_gap_canceled(map_models, gap_test):

    # Сотруднику пока не надо начислять отпускные (не прошло 15 дней)
    # У него накопилось 5 дней
    person = StaffFactory(vacation=5, office=map_models['offices']['MRP'])
    StaffExtraFields.objects.create(staff=person)

    VacationWorkflow = find_workflow('vacation')
    base_gap = gap_test.get_base_gap(VacationWorkflow)

    base_gap['date_from'] = today_dt + timedelta(days=4)
    base_gap['date_to'] = today_dt + timedelta(days=7)
    gap_info = VacationWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        person.id
    ).new_gap(base_gap)

    # Сотрудник взял 4 дня отпускных
    # Остается 5-4=1
    person = Staff.objects.get(pk=person.pk)
    assert person.vacation == 1

    VacationWorkflow.init_to_modify(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_id=gap_info['id']
    ).cancel_gap(
        send_email=False,
        issue_comment_tag='cancel_vacation'
    )

    # Отменили, обратно становится 5
    person = Staff.objects.get(pk=person.pk)
    assert person.vacation == 5


@pytest.mark.django_db
def test_vacation_changes_when_selfpaid_gap_created_and_modified(map_models, gap_test):

    # Сотруднику пока не надо начислять отпускные (не прошло 15 дней)
    # У него накопилось 5 дней
    person = StaffFactory(vacation=5, office=map_models['offices']['MRP'])
    StaffExtraFields.objects.create(staff=person)

    VacationWorkflow = find_workflow('vacation')
    base_gap = gap_test.get_base_gap(VacationWorkflow)

    base_gap['is_selfpaid'] = True
    base_gap['date_from'] = today_dt + timedelta(days=4)
    base_gap['date_to'] = today_dt + timedelta(days=7)
    gap_info = VacationWorkflow.init_to_new(
        gap_test.DEFAULT_MODIFIER_ID,
        person.id
    ).new_gap(base_gap)

    person = Staff.objects.get(pk=person.pk)
    assert person.vacation == 5

    base_gap['is_selfpaid'] = False
    base_gap['date_to'] = today_dt + timedelta(days=6)
    VacationWorkflow.init_to_modify(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_id=gap_info['id']
    ).edit_gap(base_gap)

    person = Staff.objects.get(pk=person.pk)
    assert person.vacation == 2

    base_gap['is_selfpaid'] = True
    VacationWorkflow.init_to_modify(
        gap_test.DEFAULT_MODIFIER_ID,
        gap_id=gap_info['id']
    ).edit_gap(base_gap)

    person = Staff.objects.get(pk=person.pk)
    assert person.vacation == 5
