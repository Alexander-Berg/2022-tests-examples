from datetime import date, timedelta
from decimal import Decimal
from mock import Mock
from uuid import uuid1

import pytest

from staff.workspace_management import entities, use_cases
from staff.workspace_management.tests.repository_mock import RepositoryMock
from staff.workspace_management.tests.staff_mock import StaffMock


def create_request() -> use_cases.CreateSharePieRequest:
    return use_cases.CreateSharePieRequest(
        room_id=1,
        author_login='test_login',
        room_area=Decimal(100),
        shares=[],
    )


def test_create_share_pie_raises_exception_on_wrong_shares():
    # given
    request = create_request()
    request.shares = [
        use_cases.CreateShare(2, share_value=Decimal(50)),
        use_cases.CreateShare(3, share_value=Decimal(60)),
    ]
    usecase = use_cases.CreateSharePieUsecase(Mock(spec=entities.Repository), Mock(spec=entities.Staff))

    # when
    with pytest.raises(entities.WrongShareSumError):
        usecase.create(request)


def test_create_share_pie_saves_to_repository_new_pie_and_changes_date_for_old_pie():
    # given
    author_id = 5
    request = create_request()
    request.shares = [
        use_cases.CreateShare(2, share_value=Decimal(50)),
        use_cases.CreateShare(3, share_value=Decimal(40)),
    ]
    previous_pie = entities.RoomSharePie(
        room_share_pie_id=uuid1(),
        room_id=request.room_id,
        author_id=author_id,
        from_date=date.today() - timedelta(days=1),
        to_date=date.max,
        room_area=request.room_area,
        shares=[],
    )
    repository = RepositoryMock()
    repository.set_last_share_pie_for_room(previous_pie)
    staff = StaffMock()
    staff.set_person_id_for_login(request.author_login, 100500)
    usecase = use_cases.CreateSharePieUsecase(repository, staff, date.today())

    # when
    usecase.create(request)

    # then
    saved_previous_pie = next(
        share_pie
        for share_pie in repository.saved_share_pies
        if share_pie.room_share_pie_id == previous_pie.room_share_pie_id
    )
    assert saved_previous_pie.to_date == date.today()
    new_share_pie = next(
        share_pie
        for share_pie in repository.saved_share_pies
        if share_pie.room_share_pie_id == usecase.share_pie_id
    )
    assert new_share_pie.from_date == date.today()
    assert new_share_pie.to_date == date.max

    saved_shares_business_unit_ids = [share.business_unit_id for share in new_share_pie.shares]
    assert saved_shares_business_unit_ids == [share.business_unit_id for share in request.shares]


def test_create_share_pie_saves_to_repository_new_pie():
    # given
    request = create_request()
    request.shares = [
        use_cases.CreateShare(2, share_value=Decimal(50)),
        use_cases.CreateShare(3, share_value=Decimal(40)),
    ]
    repository = RepositoryMock()
    staff = StaffMock()
    staff.set_person_id_for_login(request.author_login, 100500)
    usecase = use_cases.CreateSharePieUsecase(repository, staff, date.today())

    # when
    usecase.create(request)

    # then
    new_share_pie = next(
        share_pie
        for share_pie in repository.saved_share_pies
        if share_pie.room_share_pie_id == usecase.share_pie_id
    )
    assert new_share_pie.from_date == date.today()
    assert new_share_pie.to_date == date.max

    saved_shares_business_unit_ids = [share.business_unit_id for share in new_share_pie.shares]
    assert saved_shares_business_unit_ids == [share.business_unit_id for share in request.shares]
