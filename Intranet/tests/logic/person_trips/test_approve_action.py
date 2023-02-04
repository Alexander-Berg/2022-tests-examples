import pytest
from mock import patch

from intranet.trip.src.enums import PTStatus
from intranet.trip.src.logic.person_trips import ApproveAction
from intranet.trip.src.exceptions import WorkflowError, PermissionDenied


pytestmark = pytest.mark.asyncio


@pytest.fixture
async def user(f, uow):
    await f.create_person(person_id=1)
    user = await uow.persons.get_user(person_id=1)
    return user


async def _create_person_trip(f, **fields):
    fields['trip_id'] = fields.get('trip_id', 1)
    fields['person_id'] = fields.get('person_id', 2)
    await f.create_person(person_id=fields['person_id'])
    await f.create_trip(trip_id=fields['trip_id'])
    await f.create_person_trip(**fields)


@pytest.mark.parametrize('is_ya_team, is_approver, pt_fields, exception, exc_message', (
    (
        True,
        True,
        {'is_approved': False},
        WorkflowError,
        '',
    ),  # В yandex-team действие недоступно
    (
        False,
        True,
        {'status': PTStatus.cancelled, 'is_approved': False},
        WorkflowError,
        'Wrong person trip status',
    ),  # Нельзя согласовать отмененную услугу
    (
        False,
        True,
        {'is_approved': True},
        WorkflowError,
        'Person trip is already approved',
    ),  # Нельзя согласовать уже согласованную ранее заявку
    (
        False,
        False,
        {'is_approved': False},
        PermissionDenied,
        '',
    ),  # Нет прав согласовать командировку
))
async def test_check_permissions(
    f,
    uow,
    user,
    is_ya_team,
    is_approver,
    pt_fields,
    exception,
    exc_message,
):
    await _create_person_trip(f, **pt_fields)
    if is_approver:
        await f.create_approver_relation(approver_id=user.person_id, person_id=2)
    action = await ApproveAction.init(uow, user=user, trip_id=1, person_id=2)

    with patch('intranet.trip.src.logic.person_trips.settings.IS_YA_TEAM', is_ya_team):
        with pytest.raises(exception) as exc_info:
            await action.check_permissions()
    assert str(exc_info.value) == exc_message


async def test_approve_action(f, uow, user):
    await _create_person_trip(f, is_approved=False)
    await f.create_approver_relation(approver_id=user.person_id, person_id=2)
    action = await ApproveAction.init(uow, user=user, trip_id=1, person_id=2)

    with patch('intranet.trip.src.logic.person_trips.settings.IS_YA_TEAM', False):
        await action.check_and_execute()

    person_trip = await uow.person_trips.get_person_trip(trip_id=1, person_id=2)
    assert person_trip.is_approved is True
