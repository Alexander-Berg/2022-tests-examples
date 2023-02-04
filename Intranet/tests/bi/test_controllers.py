# coding: utf-8
from mock import patch

from review.staff.models import Person

from review.bi.controllers import (
    sync_bi_income_data,
    sync_bi_detailed_income_data,
    sync_bi_assignment_data,
    sync_bi_vesting_data,
)
from review.bi.models import (
    BIPersonIncome,
    BIPersonDetailedIncome,
    BIPersonAssignment,
    BIPersonVesting,
)

from tests.bi.mocked_api import FakeBusinessIntelligenceAPI


@patch('review.bi.controllers.BusinessIntelligenceAPI', FakeBusinessIntelligenceAPI)
def test_sync_create_bi_income():
    result = sync_bi_income_data()

    assert result['fetched'] == 2
    assert result['created'] == 1
    assert result['updated'] == 0
    assert result['deleted'] == 0

    assert BIPersonIncome.objects.count() == 1

    income = BIPersonIncome.objects.first()
    assert income.person.login == 'test_user'
    assert isinstance(income.data, str)

    # Проверяем, что при повторном синке без изменения данных объекты не создаются/обновляются
    result = sync_bi_income_data()

    assert result['fetched'] == 2
    assert result['created'] == 0
    assert result['updated'] == 0
    assert result['deleted'] == 0


@patch('review.bi.controllers.BusinessIntelligenceAPI', FakeBusinessIntelligenceAPI)
def test_sync_create_bi_detailed_income():
    result = sync_bi_detailed_income_data()

    assert result['fetched'] == 3
    assert result['created'] == 3
    assert result['updated'] == 0
    assert result['deleted'] == 0

    assert BIPersonDetailedIncome.objects.count() == 3

    detailed_income = BIPersonDetailedIncome.objects.order_by('unique').first()
    assert detailed_income.person.login == 'test_user'
    assert isinstance(detailed_income.data, str)

    # Проверяем, что при повторном синке без изменения данных объекты не создаются/обновляются
    result = sync_bi_detailed_income_data()

    assert result['fetched'] == 3
    assert result['created'] == 0
    assert result['updated'] == 0
    assert result['deleted'] == 0


@patch('review.bi.controllers.BusinessIntelligenceAPI', FakeBusinessIntelligenceAPI)
def test_sync_update_bi_income(person_builder):
    new_user = person_builder()
    BIPersonIncome.objects.create(person=new_user, data={}, hash='321')

    test_user = Person.objects.get(login='test_user')
    income = BIPersonIncome.objects.create(
        person=test_user,
        data={},
        hash='123',
    )
    result = sync_bi_income_data()

    assert result['fetched'] == 2
    assert result['created'] == 0
    assert result['updated'] == 1
    assert result['deleted'] == 1

    assert BIPersonIncome.objects.count() == 1

    income.refresh_from_db()
    assert isinstance(income.data, str)

    # Проверяем, что при повторном синке без изменения данных объекты не создаются/обновляются
    result = sync_bi_income_data()

    assert result['fetched'] == 2
    assert result['created'] == 0
    assert result['updated'] == 0
    assert result['deleted'] == 0


@patch('review.bi.controllers.BusinessIntelligenceAPI', FakeBusinessIntelligenceAPI)
def test_sync_update_detailed_bi_income(person_builder):
    new_user = person_builder()
    BIPersonDetailedIncome.objects.create(
        unique=f'{new_user.login}_201901',
        person=new_user,
        data={},
        hash='321',
    )

    test_user = Person.objects.get(login='test_user')
    detailed_incomes = [
        BIPersonDetailedIncome.objects.create(
            unique=f'{test_user.login}_201901',
            person=test_user,
            data={},
            hash='123',
        ),
        BIPersonDetailedIncome.objects.create(
            unique=f'{test_user.login}_201902',
            person=test_user,
            data={},
            hash='456',
        )
    ]
    result = sync_bi_detailed_income_data()

    assert result['fetched'] == 3
    assert result['created'] == 1
    assert result['updated'] == 2
    assert result['deleted'] == 1

    assert BIPersonDetailedIncome.objects.count() == 3

    for di in detailed_incomes:
        di.refresh_from_db()
        assert isinstance(di.data, str)
        assert di.data != ''

    # Проверяем, что при повторном синке без изменения данных объекты не создаются/обновляются
    result = sync_bi_detailed_income_data()

    assert result['fetched'] == 3
    assert result['created'] == 0
    assert result['updated'] == 0
    assert result['deleted'] == 0


@patch('review.bi.controllers.BusinessIntelligenceAPI', FakeBusinessIntelligenceAPI)
def test_sync_bi_assignments():
    test_user = Person.objects.get(login='test_user')

    # Назначение, которое должно быть удалено
    BIPersonAssignment.objects.create(
        id=123,
        person=test_user,
        data={},
        hash='123',
    )
    # Назначение, которое должно быть обновлено
    BIPersonAssignment.objects.create(
        id=8897,
        person=test_user,
        data={},
        hash='123',
    )
    result = sync_bi_assignment_data()

    assert result['fetched'] == 3
    assert result['created'] == 1
    assert result['updated'] == 1
    assert result['deleted'] == 1

    assert BIPersonAssignment.objects.count() == 2

    person_assignment_qs = BIPersonAssignment.objects.filter(person=test_user)
    assert person_assignment_qs.count() == 2

    for assignment in person_assignment_qs:
        assert isinstance(assignment.data, dict)
        # 14 полей парсится, убираем логин и зарплату
        assert len(assignment.data) == 12

    # Проверяем, что при повторном синке без изменения данных объекты не создаются/обновляются
    result = sync_bi_assignment_data()

    assert result['fetched'] == 3
    assert result['created'] == 0
    assert result['updated'] == 0
    assert result['deleted'] == 0


@patch('review.bi.controllers.BusinessIntelligenceAPI', FakeBusinessIntelligenceAPI)
def test_sync_create_bi_vesting():
    result = sync_bi_vesting_data()

    assert result['fetched'] == 2  # Для BIVestingSync это число не совпадает с кол-вом полученных пользователей
    assert result['created'] == 1
    assert result['updated'] == 0
    assert result['deleted'] == 0

    assert BIPersonVesting.objects.count() == 1

    vesting = BIPersonVesting.objects.first()
    assert vesting.person.login == 'test_user'
    assert isinstance(vesting.data, str)


@patch('review.bi.controllers.BusinessIntelligenceAPI', FakeBusinessIntelligenceAPI)
def test_bi_vesting_sync_do_not_updates_db_on_same_data():
    sync_bi_vesting_data()

    result = sync_bi_vesting_data()

    assert result['fetched'] == 2  # Для BIVestingSync это число не совпадает с кол-вом полученных пользователей
    assert result['created'] == 0
    assert result['updated'] == 0
    assert result['deleted'] == 0


@patch('review.bi.controllers.BusinessIntelligenceAPI', FakeBusinessIntelligenceAPI)
def test_bi_vesting_sync_updates_db_on_data_change(person_builder):
    new_user = person_builder()
    BIPersonVesting.objects.create(person=new_user, data={}, hash='321')

    test_user = Person.objects.get(login='test_user')
    BIPersonVesting.objects.create(
        person=test_user,
        data={},
        hash='123',
    )

    result = sync_bi_vesting_data()

    assert result['fetched'] == 2  # Для BIVestingSync это число не совпадает с кол-вом полученных пользователей
    assert result['created'] == 0
    assert result['updated'] == 1
    assert result['deleted'] == 1

    assert BIPersonVesting.objects.count() == 1

    vesting = BIPersonVesting.objects.first()
    assert vesting.person.login == 'test_user'
    assert isinstance(vesting.data, str)
