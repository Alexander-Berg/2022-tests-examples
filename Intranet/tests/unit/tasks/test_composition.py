import pretend
import pytest
import datetime

from mock import patch

from watcher.enums import MemberState
from watcher.crud.rating import (
    get_new_rating_values_by_schedules,
    get_rating_by_schedule_staff,
)
from watcher.db import Composition
from watcher.logic.exceptions import RatingNotFound
from watcher.tasks.composition import (
    update_composition,
    check_composition_staff,
    update_compositions,
)
from watcher.config import settings
from watcher.logic.timezone import now
from watcher import enums


@pytest.fixture
def comp_data(
    composition_factory, staff_factory, role_factory,
    scope_factory, member_factory,
):
    composition = composition_factory()

    scope = scope_factory()
    role = role_factory(scope=scope)

    scope_1 = scope_factory()
    role_1 = role_factory(scope=scope_1)

    scope_2 = scope_factory()
    role_2 = role_factory(scope=scope_2)

    staff = staff_factory()
    staff_1 = staff_factory()
    staff_2 = staff_factory()

    member_factory(
        staff=staff,
        role=role,
        service=composition.service,
    )
    member_factory(
        staff=staff,
        role=role_1,
        service=composition.service,
    )
    member_factory(
        staff=staff,
        role=role,
    )

    staff_1_member = member_factory(
        staff=staff_1,
        role=role_1,
        service=composition.service,
    )

    member_factory(
        staff=staff_2,
        role=role_2,
        service=composition.service,
    )

    return pretend.stub(
        composition=composition,
        scope=scope,
        scope_1=scope_1,
        role_1=role_1,
        scope_2=scope_2,
        role_2=role_2,
        role=role,
        staff=staff,
        staff_1=staff_1,
        staff_2=staff_2,
        staff_1_member=staff_1_member,
    )


def test_update_composition(comp_data, scope_session):
    """
    Изначально в участниках два человека,
    обновляем настройки - прописываем конкретного человека и роль
    конкретный человек и тот кто обладает этой ролью должны остаться,
    лишний человек удалиться
    """
    composition = comp_data.composition
    composition.participants.append(comp_data.staff)
    composition.participants.append(comp_data.staff_1)
    composition.staff.append(comp_data.staff_2)
    composition.roles.append(comp_data.role)

    scope_session.add(composition)
    scope_session.commit()
    update_composition(composition_id=composition.id)
    scope_session.refresh(composition)

    assert {obj.id for obj in composition.participants} == {comp_data.staff.id, comp_data.staff_2.id}


def test_update_composition_excluded(comp_data, scope_session):
    """
    Проверяет что excluded имеет приоритет в настройках
    если указать одну и ту же роль в excluded_roles и roles
    - она должна быть исключена
    """
    composition = comp_data.composition
    composition.participants.append(comp_data.staff_2)
    composition.roles.append(comp_data.role)
    composition.roles.append(comp_data.role_1)
    composition.excluded_roles.append(comp_data.role_1)

    scope_session.add(composition)
    scope_session.commit()
    update_composition(composition_id=composition.id)
    scope_session.refresh(composition)

    assert {obj.id for obj in composition.participants} == {comp_data.staff.id}


def test_update_composition_scopes(comp_data, scope_session):
    """
    Проверяем что если добавить scope в excluded и роль из него в
    roles - роль будет исключена
    """
    composition = comp_data.composition
    composition.participants.append(comp_data.staff_2)
    composition.roles.append(comp_data.role)
    composition.excluded_roles.append(comp_data.role)

    scope_session.add(composition)
    scope_session.commit()
    update_composition(composition_id=composition.id)
    scope_session.refresh(composition)

    assert not composition.participants


def test_update_composition_full(comp_data, role_factory, member_factory, scope_session):
    """
    Проверяем работу full_service настройки - должны быть добавлены
    все участники сервиса, кроме ролей/скоупов/людей указанных в исключениях,
    а так же кроме роли управляющих
    """
    member_factory(
        service=comp_data.composition.service,
        role=role_factory(code=settings.RESPONSIBLE_ROLE_CODE),
    )
    composition = comp_data.composition
    composition.full_service = True
    composition.excluded_scopes.append(comp_data.scope_1)

    scope_session.add(composition)
    scope_session.commit()
    update_composition(composition_id=composition.id)
    scope_session.refresh(composition)

    assert len(composition.participants) == 2
    assert {obj.id for obj in composition.participants} == {comp_data.staff.id, comp_data.staff_2.id}


def test_update_composition_duplicate(comp_data, scope_session):
    """
    Проверяем корректную работу для случая если добавить в настройки
    скоуп и роль из этого скоупа, а так же человека из этого скоупа -
    дубликатов быть не должно
    """

    composition = comp_data.composition
    composition.staff.append(comp_data.staff)
    composition.roles.append(comp_data.role)
    composition.scopes.append(comp_data.scope)

    scope_session.add(composition)
    scope_session.commit()
    update_composition(composition_id=composition.id)
    scope_session.refresh(composition)

    assert len(composition.participants) == 1
    assert {obj.id for obj in composition.participants} == {comp_data.staff.id}


def test_update_composition_not_active(comp_data, scope_session):
    """
    Проверяем что не активные участники, исключаются из состава при обновлении
    даже если явно прописаны в настройках
    """
    composition = comp_data.composition
    composition.participants.append(comp_data.staff_1)
    composition.staff.append(comp_data.staff_1)
    composition.scopes.append(comp_data.scope_2)

    comp_data.staff_1_member.state = MemberState.deprived
    scope_session.add(comp_data.staff_1_member)
    scope_session.add(composition)
    scope_session.commit()
    update_composition(composition_id=composition.id)
    scope_session.refresh(composition)

    assert len(composition.participants) == 1
    assert {obj.id for obj in composition.participants} == {comp_data.staff_2.id}


def test_update_composition_exclude_duplicate(comp_data, scope_session):
    """
    Проверяем корректную работу для случая если добавить role и явно
    исключить staff имеющего эту роль - он не должен быть в participants
    в результате
    """

    composition = comp_data.composition
    composition.roles.append(comp_data.role)
    composition.roles.append(comp_data.role_2)
    composition.excluded_staff.append(comp_data.staff)

    scope_session.add(composition)
    scope_session.commit()
    update_composition(composition_id=composition.id)
    scope_session.refresh(composition)

    assert len(composition.participants) == 1
    assert {obj.id for obj in composition.participants} == {comp_data.staff_2.id}


@pytest.mark.parametrize('max_schedule_rating', (0.0, 168.0))
def test_get_new_rating_value_by_schedule(scope_session, rating_factory, max_schedule_rating):
    rating = rating_factory(rating=max_schedule_rating)
    rating_factory(rating=1000)

    expected_rating = 0.0 if max_schedule_rating == 0.0 else max_schedule_rating + 1.0
    assert get_new_rating_values_by_schedules(
        db=scope_session, schedule_ids=[rating.schedule_id]
    )[rating.schedule_id] == expected_rating


def test_update_composition_create_new_rating(comp_data, schedule_factory, revision_factory, interval_factory,
                                              slot_factory, rating_factory, scope_session):
    composition = comp_data.composition
    composition.participants.append(comp_data.staff)
    composition.participants.append(comp_data.staff_2)
    composition.staff.append(comp_data.staff_1)
    scope_session.add(composition)
    scope_session.commit()

    schedule = schedule_factory(service=composition.service)
    slot_factory(
        composition=composition,
        interval=interval_factory(schedule=schedule, revision=revision_factory(schedule=schedule))
    )
    rating_factory(staff=comp_data.staff, schedule=schedule)
    new_ratings = get_new_rating_values_by_schedules(db=scope_session, schedule_ids=[schedule.id])
    with patch('watcher.tasks.people_allocation.start_people_allocation.delay'):
        update_composition(composition_id=composition.id)

    rating_1 = get_rating_by_schedule_staff(db=scope_session, schedule_id=schedule.id, staff_id=comp_data.staff_1.id)
    assert comp_data.staff_1 in set(participant for participant in composition.participants)
    assert rating_1.rating == new_ratings[schedule.id]
    with pytest.raises(RatingNotFound):
        get_rating_by_schedule_staff(db=scope_session, schedule_id=schedule.id, staff_id=comp_data.staff_2.id)


@pytest.mark.parametrize('autoupdate', (True, False))
@pytest.mark.parametrize('has_current_shifts', (True, False))
def test_check_composition_staff(scope_session, watcher_robot, shift_factory, comp_data, start_people_allocation_mock, staff_factory, slot_factory,
                                 schedules_group_factory, has_current_shifts, autoupdate):
    composition: Composition = comp_data.composition
    composition.autoupdate = autoupdate
    scope_session.add(composition)
    scope_session.commit()

    stranger_1 = staff_factory()
    stranger_2 = staff_factory()

    composition.staff.append(comp_data.staff_1)
    composition.staff.append(stranger_1)
    composition.excluded_staff.append(comp_data.staff_2)
    composition.excluded_staff.append(stranger_2)
    composition.participants.append(stranger_1)
    composition.participants.append(comp_data.staff_1)

    schedule_group = schedules_group_factory()
    slot = slot_factory(composition=composition)
    slot.interval.schedule.schedules_group = schedule_group
    scope_session.add(slot.interval.schedule)
    scope_session.add(comp_data.composition)
    scope_session.commit()

    shift = shift_factory(
        status=enums.ShiftStatus.completed,
        schedule=slot.interval.schedule,
        staff=stranger_1,
        slot=slot,
    )

    if has_current_shifts:
        shift.status = enums.ShiftStatus.active
        scope_session.add(shift)
        scope_session.commit()

    check_composition_staff(composition_id=composition.id)

    scope_session.refresh(composition)

    assert stranger_1.id not in set(staff.id for staff in composition.participants)
    assert comp_data.staff_1.id in set(staff.id for staff in composition.participants)
    assert stranger_1.id not in set(staff.id for staff in composition.staff)
    assert comp_data.staff_1.id in set(staff.id for staff in composition.staff)
    assert stranger_2.id not in set(staff.id for staff in composition.excluded_staff)
    assert comp_data.staff_2.id in set(staff.id for staff in composition.excluded_staff)
    if has_current_shifts:
        start_people_allocation_mock.assert_called_once_with(
            schedules_group_id=schedule_group.id,
            start_date=shift.start,
        )
    else:
        start_people_allocation_mock.assert_not_called()


def test_check_composition_staff_non_autoupdate_skip(scope_session, comp_data, start_people_allocation_mock):
    composition: Composition = comp_data.composition
    composition.autoupdate = False
    scope_session.add(composition)
    scope_session.commit()

    check_composition_staff(composition_id=composition.id)

    start_people_allocation_mock.assert_not_called()


def test_update_composition_non_autoupdate_skip(scope_session, comp_data):
    updated_at = now() - datetime.timedelta(days=1)

    composition = comp_data.composition
    composition.autoupdate = False
    composition.roles.append(comp_data.role)
    composition.updated_at = updated_at
    scope_session.add(composition)
    scope_session.commit()

    update_composition(composition_id=composition.id)

    scope_session.refresh(composition)
    assert not len(composition.participants)
    assert composition.updated_at != updated_at


def test_check_composition_staff_permitted_staff(scope_session, comp_data, start_people_allocation_mock):
    composition: Composition = comp_data.composition

    composition.staff.append(comp_data.staff_1)
    composition.excluded_staff.append(comp_data.staff_2)
    composition.participants.append(comp_data.staff_1)
    scope_session.add(composition)
    scope_session.commit()

    check_composition_staff(composition_id=composition.id)

    scope_session.refresh(composition)
    assert comp_data.staff_1.id in set(staff.id for staff in composition.participants)

    start_people_allocation_mock.assert_not_called()


@pytest.mark.parametrize('has_staff', [True, False])
def test_update_compositions(composition_factory, composition_to_staff_factory, has_staff):
    composition = composition_factory(
        updated_at=now() - datetime.timedelta(hours=50)
    )
    composition_1 = composition_factory(
        service=composition.service,
        updated_at=now()
    )
    if has_staff:
        composition_to_staff_factory(
            composition=composition_1,
        )
    with patch('watcher.tasks.composition.update_composition') as mock_update_composition:
        with patch('watcher.tasks.composition.check_composition_staff') as mock_check_composition_staff:
            update_compositions()

    mock_update_composition.assert_called_once_with(composition_id=composition.id, _lock=False)
    if has_staff:
        mock_check_composition_staff.assert_called_once_with(composition_id=composition_1.id, _lock=False)
    else:
        mock_check_composition_staff.assert_not_called()
