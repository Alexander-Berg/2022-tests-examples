from datetime import date, timedelta
from decimal import Decimal
import random
import uuid

import pytest
from mock import AsyncMock, Mock, patch

from intranet.trip.src import enums


pytestmark = pytest.mark.asyncio


def get_random_date():
    return date(
        year=random.randint(2021, 2022),
        month=random.randint(1, 12),
        day=random.randint(1, 28),
    )


@pytest.mark.parametrize(
    'observer_holding_id, transaction_holding_id, is_coordinator, status',
    (
        (1, 2, True, 200),   # Координатор Яндекса смотрит транзакции
        (2, 2, True, 404),   # Координатор холдинга смотрит транзакции своего холдинга
        (2, 3, True, 404),   # Координатор холдинга смотрит транзакции не своего холдинга
        (1, 1, False, 404),  # Какой-то человек, не имеющие к нему отношения
    ),
)
async def test_transaction_list(
    f,
    uow,
    client,
    observer_holding_id,
    transaction_holding_id,
    is_coordinator,
    status,
):
    company_id = trip_id = person_id = 1
    observer_company_id = observer_id = 2

    await f.create_holding(holding_id=observer_holding_id)
    await f.create_company(company_id=observer_company_id, holding_id=observer_holding_id)
    await f.create_person(
        person_id=observer_id,
        is_coordinator=is_coordinator,
        company_id=observer_company_id,
    )
    observer = await uow.persons.get_user(person_id=observer_id)

    await f.create_holding(holding_id=transaction_holding_id)
    await f.create_company(company_id=company_id, holding_id=transaction_holding_id)
    await f.create_person(person_id=person_id, company_id=company_id)
    await f.create_trip(
        trip_id=trip_id,
        person_ids=[person_id],
    )

    transaction_id = uuid.uuid4()
    await f.create_transaction(
        transaction_id=transaction_id,
        company_id=company_id,
        person_id=person_id,
        trip_id=trip_id,
        execution_date=get_random_date(),
    )

    with patch(
        'intranet.trip.src.middlewares.auth.DevMiddleware.get_user_or_error',
        AsyncMock(return_value=observer),
    ):
        response = await client.get(url=f'api/billing/companies/{company_id}/transactions')

    assert response.status_code == status

    if status == 200:
        data = response.json()
        assert len(data['data']) == 1
        assert data['data'][0]['transaction_id'] == str(transaction_id)


@pytest.mark.parametrize(
    'observer_holding_id, transaction_holding_id, is_coordinator, status',
    (
        (1, 2, True, 200),   # Координатор Яндекса смотрит транзакции
        (2, 2, True, 200),   # Координатор холдинга смотрит транзакции своего холдинга
        (2, 3, True, 404),   # Координатор холдинга смотрит транзакции не своего холдинга
        (1, 1, False, 404),  # Какой-то человек смотрит транзакции, не имеющие к нему отношения
    ),
)
async def test_transaction_list_for_client(
    f,
    uow,
    client,
    observer_holding_id,
    transaction_holding_id,
    is_coordinator,
    status,
):
    company_id = trip_id = person_id = 1
    observer_company_id = observer_id = 2

    await f.create_holding(holding_id=observer_holding_id)
    await f.create_company(company_id=observer_company_id, holding_id=observer_holding_id)
    await f.create_person(
        person_id=observer_id,
        is_coordinator=is_coordinator,
        company_id=observer_company_id,
    )
    observer = await uow.persons.get_user(person_id=observer_id)

    await f.create_holding(holding_id=transaction_holding_id)
    await f.create_company(company_id=company_id, holding_id=transaction_holding_id)
    await f.create_person(person_id=person_id, company_id=company_id)
    await f.create_trip(
        trip_id=trip_id,
        person_ids=[person_id],
    )

    transaction_id = uuid.uuid4()
    await f.create_transaction(
        transaction_id=transaction_id,
        company_id=company_id,
        person_id=person_id,
        trip_id=trip_id,
        execution_date=get_random_date(),
    )

    with patch(
        'intranet.trip.src.middlewares.auth.DevMiddleware.get_user_or_error',
        AsyncMock(return_value=observer),
    ):
        response = await client.get(
            url=f'api/billing/companies/{company_id}/transactions/for_client',
        )

    assert response.status_code == status

    if status == 200:
        data = response.json()
        assert len(data['data']) == 1
        assert data['data'][0]['transaction_id'] == str(transaction_id)
        assert 'service_id' not in data['data'][0]
        assert 'invoice_date' not in data['data'][0]


@pytest.mark.parametrize(
    'is_yandex_coordinator, is_holding_coordinator, status',
    (
        (True, True, 200),    # Координатор Яндекса смотрит транзакцию
        (False, True, 404),   # Координатор холдинга смотрит транзакцию
        (False, False, 404),  # Не координатор смотрит транзакцию
    ),
)
async def test_transaction_detail(
    f,
    client,
    is_yandex_coordinator,
    is_holding_coordinator,
    status,
):
    company_id = person_id = trip_id = 1
    transaction_id = uuid.uuid4()

    await f.create_company(company_id)
    await f.create_person(person_id, company_id)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])
    await f.create_transaction(
        transaction_id=transaction_id,
        company_id=company_id,
        person_id=person_id,
        trip_id=trip_id,
        execution_date=get_random_date(),
    )

    patched_is_yandex_coordinator = patch(
        'intranet.trip.src.api.endpoints.billing.is_yandex_coordinator',
        Mock(return_value=is_yandex_coordinator),
    )

    patched_is_holding_coordinator = patch(
        'intranet.trip.src.api.auth.permissions.is_holding_coordinator',
        Mock(return_value=is_holding_coordinator),
    )

    with (
        patched_is_yandex_coordinator,
        patched_is_holding_coordinator,
    ):
        response = await client.get(
            url=f'api/billing/companies/{company_id}/transactions/{transaction_id}',
        )

    assert response.status_code == status


@pytest.mark.parametrize(
    'is_yandex_coordinator, is_holding_coordinator, status',
    (
        (True, True, 200),  # Координатор Яндекса смотрит транзакцию
        (False, True, 200),  # Координатор холдинга смотрит транзакцию
        (False, False, 404),  # Не координатор смотрит транзакцию
    ),
)
async def test_transaction_detail_for_client(
    f,
    client,
    is_yandex_coordinator,
    is_holding_coordinator,
    status,
):
    company_id = person_id = trip_id = 1
    transaction_id = uuid.uuid4()

    await f.create_company(company_id)
    await f.create_person(person_id, company_id)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])
    await f.create_transaction(
        transaction_id=transaction_id,
        company_id=company_id,
        person_id=person_id,
        trip_id=trip_id,
        execution_date=get_random_date(),
    )

    patched_is_yandex_coordinator = patch(
        'intranet.trip.src.api.endpoints.billing.is_yandex_coordinator',
        Mock(return_value=is_yandex_coordinator),
    )

    patched_is_holding_coordinator = patch(
        'intranet.trip.src.api.auth.permissions.is_holding_coordinator',
        Mock(return_value=is_holding_coordinator),
    )

    with (
        patched_is_yandex_coordinator,
        patched_is_holding_coordinator,
    ):
        response = await client.get(
            url=f'api/billing/companies/{company_id}/transactions/for_client/{transaction_id}',
        )

    assert response.status_code == status


@pytest.mark.parametrize('is_yandex_coordinator, status', (
    (True, 201),
    (False, 404),
))
async def test_transaction_create(f, client, is_yandex_coordinator, status):
    company_id = person_id = trip_id = 1

    await f.create_company(company_id)
    await f.create_person(person_id=person_id, company_id=company_id)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])

    with patch(
        'intranet.trip.src.logic.billing.is_yandex_coordinator',
        Mock(return_value=is_yandex_coordinator),
    ):
        response = await client.post(
            url=f'api/billing/companies/{company_id}/transactions',
            json={
                'type': enums.TransactionType.purchase,
                'person_id': person_id,
                'trip_id': trip_id,
                'service_type': enums.TransactionServiceType.avia,
                'execution_date': '2022-01-01',
                'status': enums.TransactionStatus.paid,
                'price': '1000.00',
                'company_id': company_id,
                'fee': '400.00',
                'is_penalty': False,
            },
        )

        assert response.status_code == status


@pytest.mark.parametrize('is_yandex_coordinator, status', (
    (True, 200),
    (False, 404),
))
async def test_transaction_update(f, client, is_yandex_coordinator, status):
    company_id = person_id = trip_id = 1
    transaction_id = uuid.uuid4()

    await f.create_company(company_id)
    await f.create_person(person_id, company_id)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])
    await f.create_transaction(
        transaction_id=transaction_id,
        company_id=company_id,
        person_id=person_id,
        trip_id=trip_id,
        execution_date=get_random_date(),
    )

    with patch(
        'intranet.trip.src.logic.billing.is_yandex_coordinator',
        Mock(return_value=is_yandex_coordinator),
    ):
        response = await client.patch(
            url=f'api/billing/companies/{company_id}/transactions/{transaction_id}',
            json={
                'price': '2000.00',
            },
        )

    assert response.status_code == status


@pytest.mark.parametrize('is_yandex_coordinator, status', (
    (True, 204),
    (False, 404),
))
async def test_transaction_delete(f, client, is_yandex_coordinator, status):
    person_id = company_id = trip_id = 1
    transaction_id = uuid.uuid4()

    await f.create_company(company_id)
    await f.create_person(person_id, company_id)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])
    await f.create_transaction(
        transaction_id=transaction_id,
        company_id=company_id,
        person_id=person_id,
        trip_id=trip_id,
        execution_date=get_random_date(),
    )

    with patch(
        'intranet.trip.src.logic.billing.is_yandex_coordinator',
        Mock(return_value=is_yandex_coordinator),
    ):
        response = await client.delete(
            url=f'api/billing/companies/{company_id}/transactions/{transaction_id}',
        )

        assert response.status_code == status


@pytest.mark.parametrize('holding_id, observer_holding_id, is_coordinator, status', (
    (2, 1, True, 200),   # 1 Координатор Янекса смотрит баланс компании
    (2, 2, True, 200),   # 2 Координатор холдинга смотрит баланс компании из своего холдинга
    (2, 3, True, 404),   # 3 Координатор холдинга смотрит баланс компании не из своего холдинга
    (2, 3, False, 404),  # 4 Не координатор смотрит баланс компании
))
async def test_company_balance(
    f,
    uow,
    client,
    holding_id,
    observer_holding_id,
    is_coordinator,
    status,
):
    company_id = trip_id = person_id = 1
    other_company_id = 2
    observer_id = observer_company_id = 3

    transaction_price = 1000
    provider_fee = yandex_fee = 250
    deposits_data = [
        # (amount, charge_date)
        (500, date(2022, 2, 2)),
        (700, date(2022, 3, 3)),
        (800, date(2022, 4, 4)),
    ]

    await f.create_holding(holding_id=holding_id)
    await f.create_company(company_id=company_id, holding_id=holding_id)
    await f.create_company(company_id=other_company_id, holding_id=holding_id)
    await f.create_person(person_id=person_id, company_id=company_id)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])

    await f.create_holding(holding_id=observer_holding_id)
    await f.create_company(company_id=observer_company_id, holding_id=observer_holding_id)
    await f.create_person(
        person_id=observer_id,
        company_id=observer_company_id,
        is_coordinator=is_coordinator,
    )
    await f.create_transaction(
        transaction_id=uuid.uuid4(),
        company_id=company_id,
        person_id=person_id,
        trip_id=trip_id,
        execution_date=get_random_date(),
        price=transaction_price,
        provider_fee=provider_fee,
        yandex_fee=yandex_fee,
    )

    for deposit_amount, deposit_charge_date in deposits_data:
        await f.create_deposit(
            company_id=company_id,
            author_id=person_id,
            amount=deposit_amount,
            charge_date=deposit_charge_date,
        )

    observer = await uow.persons.get_user(person_id=observer_id)

    with patch(
        'intranet.trip.src.middlewares.auth.DevMiddleware.get_user_or_error',
        AsyncMock(return_value=observer),
    ):
        response = await client.get(
            url=f'api/billing/companies/{company_id}/balance',
        )

    assert response.status_code == status

    if status == 200:
        expected_data = {
            'amount': str(
                sum(am for am, _ in deposits_data) - (transaction_price + provider_fee + yandex_fee)
            ),
            'company_id': company_id,
            'last_deposit_date': f'{deposits_data[-1][1]:%Y-%m-%d}',
            'status': enums.BalanceStatus.ok,
        }
        assert response.json() == expected_data


@pytest.mark.parametrize('transaction_holding_id, observer_holding_id, is_coordinator, status', (
    (2, 1, True, 200),   # 1
    (2, 2, True, 200),   # 2
    (2, 3, True, 404),   # 3
    (2, 3, False, 404),  # 4
))
async def test_company_expenses(
    f,
    uow,
    client,
    transaction_holding_id,
    observer_holding_id,
    is_coordinator,
    status,
):
    """
    1) Координатор Янекса смотрит расходы компании
    2) Координатор холдинга смотрит расходы компании из своего холдинга
    3) Координатор холдинга смотрит расходы компании не из своего холдинга
    4) Кто-то смотрит расходы компании
    """
    company_id = trip_id = person_id = 1
    other_company_id = 2
    observer_id = observer_company_id = 3

    await f.create_holding(holding_id=transaction_holding_id)
    await f.create_company(company_id=company_id, holding_id=transaction_holding_id)
    await f.create_company(company_id=other_company_id, holding_id=transaction_holding_id)
    await f.create_person(person_id=person_id, company_id=company_id)
    await f.create_trip(trip_id=trip_id, person_ids=[person_id])

    await f.create_holding(holding_id=observer_holding_id)
    await f.create_company(company_id=observer_company_id, holding_id=observer_holding_id)
    await f.create_person(
        person_id=observer_id,
        company_id=observer_company_id,
        is_coordinator=is_coordinator,
    )

    observer = await uow.persons.get_user(person_id=observer_id)

    date_from = date(2022, 1, 1)
    date_to = date(2023, 1, 1)

    transaction_data = [
        # (execution_date - date_from, company_id, price, yandex_fee, provider_fee)
        (timedelta(days=-10), company_id, 1000, 250, 150),
        (timedelta(days=0), company_id, 1000, 250, 150),
        (timedelta(days=60), company_id, 1000, 250, 150),
        (timedelta(days=700), company_id, 1000, 250, 150),
        (timedelta(days=30), other_company_id, 1000, 250, 150),
    ]
    expected_expenses = 2800

    for td, cid, price, yandex_fee, provider_fee in transaction_data:
        await f.create_transaction(
            transaction_id=uuid.uuid4(),
            company_id=cid,
            person_id=person_id,
            trip_id=trip_id,
            execution_date=date_from + td,
            price=price,
            yandex_fee=yandex_fee,
            provider_fee=provider_fee,
        )

    query_params = f'date_from={date_from:%Y-%m-%d}&date_to={date_to:%Y-%m-%d}'
    with patch(
        'intranet.trip.src.middlewares.auth.DevMiddleware.get_user_or_error',
        AsyncMock(return_value=observer),
    ):
        response = await client.get(
            url=f'api/billing/companies/{company_id}/expenses?{query_params}',
        )

    assert response.status_code == status

    if status == 200:
        data = response.json()
        assert Decimal(data['amount']) == Decimal(expected_expenses)
