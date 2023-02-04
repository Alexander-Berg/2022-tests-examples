import pytest

from intranet.trip.src.logic.ok import fill_approvement_data
from intranet.trip.src.api.schemas import ApprovementData
from ..factories import date_from


pytestmark = pytest.mark.asyncio


async def create_person_trip(f, purpose_ids=[], trip_id=1, staff_trip_uuid='1'):
    await f.create_person(person_id=1, login='test_login')
    await f.create_trip(trip_id=trip_id, author_id=1, staff_trip_uuid=staff_trip_uuid)
    await f.create_person_trip(trip_id=trip_id, person_id=1, gap_date_from=date_from)
    for purpose_id in purpose_ids:
        await f.create_purpose(purpose_id=purpose_id)
        await f.create_person_trip_purpose(purpose_id=purpose_id, trip_id=trip_id, person_id=1)


async def test_automatic_approval_for_purpose(f, uow):
    await create_person_trip(f, purpose_ids=[17])
    approvement_data = ApprovementData(
        flow_context={'author_login': 'test_login'},
        trip_uuid='1',
        is_automatic_approval=None,
    )
    await fill_approvement_data(
        uow=uow,
        approvement_data=approvement_data,
    )
    assert approvement_data.flow_context['is_automatic_approval'] is True


async def test_automatic_approval_off(f, uow):
    await create_person_trip(f)
    approvement_data = ApprovementData(
        flow_context={'author_login': 'test_login'},
        trip_uuid='1',
        is_automatic_approval=False,
    )
    await fill_approvement_data(
        uow=uow,
        approvement_data=approvement_data,
    )
    assert approvement_data.flow_context['is_automatic_approval'] is False


async def test_automatic_approval_on(f, uow):
    await create_person_trip(f)
    approvement_data = ApprovementData(
        flow_context={'author_login': 'test_login'},
        trip_uuid='1',
        is_automatic_approval=True,
    )
    await fill_approvement_data(
        uow=uow,
        approvement_data=approvement_data,
    )
    assert approvement_data.flow_context['is_automatic_approval'] is True


async def test_automatic_approval_with_unknown_trip_uuid(f, uow):
    await create_person_trip(f, purpose_ids=[17])
    approvement_data = ApprovementData(
        flow_context={'author_login': 'test_login'},
        trip_uuid='11',
        is_automatic_approval=None,
    )
    await fill_approvement_data(
        uow=uow,
        approvement_data=approvement_data,
    )
    assert approvement_data.flow_context['is_automatic_approval'] is False


async def test_not_automatic_approval_for_purpose_with_replay(f, uow):
    await create_person_trip(f, purpose_ids=[17])
    await create_person_trip(f, trip_id=2, purpose_ids=[17], staff_trip_uuid='11')
    approvement_data = ApprovementData(
        flow_context={'author_login': 'test_login'},
        trip_uuid='1',
        is_automatic_approval=None,
    )
    await fill_approvement_data(
        uow=uow,
        approvement_data=approvement_data,
    )
    assert approvement_data.flow_context['is_automatic_approval'] is False


@pytest.mark.parametrize('purpose_ids_with_automatic_approval', (
    [3],  # Деловые переговоры с внешними партнерами
    [8],  # Логистика
    [9],  # Поддержка ИТ-инфраструктуры
    [11],  # Инженерно-монтажные работы
    [16],  # Организация конференции, семинара или аналогичного мероприятия
    [18],  # Производство, посещение объектов, приемка работ
    [3, 8],
    [3, 9],
    [8, 9],
    [3, 17],
))
async def test_automatic_approval_for_purpose_with_automatic_approval(
        f,
        uow,
        purpose_ids_with_automatic_approval,
):
    await create_person_trip(f, purpose_ids=purpose_ids_with_automatic_approval)
    approvement_data = ApprovementData(
        flow_context={'author_login': 'test_login'},
        trip_uuid='1',
        is_automatic_approval=None,
    )
    await fill_approvement_data(
        uow=uow,
        approvement_data=approvement_data,
    )
    assert approvement_data.flow_context['is_automatic_approval'] is True


@pytest.mark.parametrize('purpose_ids_with_automatic_approval', (
    [3],  # Деловые переговоры с внешними партнерами
    [8],  # Логистика
    [9],  # Поддержка ИТ-инфраструктуры
    [11],  # Инженерно-монтажные работы
    [16],  # Организация конференции, семинара или аналогичного мероприятия
    [18],  # Производство, посещение объектов, приемка работ
    [3, 8],
    [3, 9],
    [8, 9],
))
async def test_automatic_approval_for_purpose_with_replay(
        f,
        uow,
        purpose_ids_with_automatic_approval,
):
    await create_person_trip(f, purpose_ids=purpose_ids_with_automatic_approval)
    await create_person_trip(
        f,
        trip_id=2,
        purpose_ids=purpose_ids_with_automatic_approval,
        staff_trip_uuid='11',
    )
    approvement_data = ApprovementData(
        flow_context={'author_login': 'test_login'},
        trip_uuid='1',
        is_automatic_approval=None,
    )
    await fill_approvement_data(
        uow=uow,
        approvement_data=approvement_data,
    )
    assert approvement_data.flow_context['is_automatic_approval'] is True
