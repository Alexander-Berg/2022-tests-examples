from datetime import timedelta
from typing import Optional
from uuid import UUID

import pytest
from freezegun import freeze_time

from sendr_utils import alist, utcnow

from hamcrest import assert_that, contains_inanyorder, equal_to, has_item, has_length, has_properties, match_equality

from pay.bill_payments.bill_payments.core.actions.bill.search import SearchFinesAction
from pay.bill_payments.bill_payments.core.exceptions import DocumentNotFoundError, UserNotFoundError
from pay.bill_payments.bill_payments.interactions import KaznaClient
from pay.bill_payments.bill_payments.interactions.kazna import SearchResponse
from pay.bill_payments.bill_payments.interactions.kazna.entities import (
    Charge,
    ChargeData,
    DepartmentType,
    DocumentCode,
    PayerDoc,
    SearchRequest,
    SearchStatus,
)
from pay.bill_payments.bill_payments.storage.entities.bill import Bill
from pay.bill_payments.bill_payments.storage.entities.document import Document
from pay.bill_payments.bill_payments.storage.entities.enums import BillStatus
from pay.bill_payments.bill_payments.storage.entities.task import TaskState


def create_charge(identifier: int, document: Optional[Document] = None) -> Charge:
    if document is None:
        payer_doc = PayerDoc(code=DocumentCode.DRIVER_LICENSE, value=['123456'])
    else:
        payer_doc = PayerDoc(code=document.code, value=[document.value])
    return Charge(
        document=payer_doc,
        supplier_bill_id=f'{identifier}',
        department=DepartmentType.GIBDD,
        charge_data=ChargeData(bill_date=utcnow(), amount_to_pay=15000, amount=15000),
    )


@pytest.fixture
async def documents(storage, user):
    return [
        await storage.document.create(
            Document(
                document_id=UUID('11111111-0000-4b00-0000-000000123456'),
                uid=user.uid,
                code=DocumentCode.DRIVER_LICENSE,
                value='123456',
            )
        ),
        await storage.document.create(
            Document(
                document_id=UUID('11111111-0000-4b00-0000-000000234567'),
                uid=user.uid,
                code=DocumentCode.DRIVER_LICENSE,
                value='234567',
            )
        ),
        await storage.document.create(
            Document(
                document_id=UUID('11111111-0000-4b00-0000-000003456789'),
                uid=user.uid,
                code=DocumentCode.VEHICLE_REGISTRATION_CERTIFICATE,
                value='3456789',
            )
        ),
    ]


@pytest.fixture
def search_response():
    return SearchResponse(status=SearchStatus.COMPLETE, charges=[create_charge(1)])


@pytest.mark.asyncio
async def test_bill_created(user, mocker, search_response, storage, documents):
    search_mock = mocker.patch.object(KaznaClient, 'search', mocker.AsyncMock(return_value=search_response))

    await SearchFinesAction(uid=user.uid, revision=1).run()

    search_mock.assert_called_once()
    bills = await alist(storage.bill.find())
    assert_that(
        bills,
        contains_inanyorder(
            has_properties(
                uid=user.uid,
                supplier_bill_id=search_response.charges[0].supplier_bill_id,
                status=BillStatus.NEW,
                document_id=documents[0].document_id,
            )
        ),
    )
    updated_user = await storage.user.get(user.uid)
    assert_that(updated_user.synced_revision, equal_to(1))
    assert_that(updated_user.syncing_revision, equal_to(None))


@pytest.mark.asyncio
async def test_search_params(user, mocker, search_response, documents):
    search_mock = mocker.patch.object(KaznaClient, 'search', mocker.AsyncMock(return_value=search_response))

    await SearchFinesAction(uid=user.uid, revision=1).run()

    search_mock.assert_called_once()
    assert_that(
        search_mock.call_args[0][0],
        equal_to(
            SearchRequest(
                documents=match_equality(
                    contains_inanyorder(
                        PayerDoc(
                            code=DocumentCode.DRIVER_LICENSE,
                            value=match_equality(contains_inanyorder('123456', '234567')),
                        ),
                        PayerDoc(code=DocumentCode.VEHICLE_REGISTRATION_CERTIFICATE, value=['3456789']),
                    )
                ),
                department=DepartmentType.GIBDD,
                subscribe=user.subscription_id,
            )
        ),
    )


@pytest.mark.asyncio
async def test_bills_updated(user, mocker, search_response, storage, documents):
    for i in range(3):
        await storage.bill.create(
            Bill(
                uid=user.uid,
                document_id=documents[i % len(documents)].document_id,
                status=(BillStatus.NEW, BillStatus.PAID)[i % 2],
                supplier_bill_id=f'{i + 1}',
                amount=100 * (i + 1),
                amount_to_pay=100 * (i + 1),
                bill_date=utcnow(),
            )
        )

    search_response.charges.append(create_charge(2, documents[1]))
    mocker.patch.object(KaznaClient, 'search', mocker.AsyncMock(return_value=search_response))

    await SearchFinesAction(uid=user.uid, revision=1).run()

    bills = sorted(await alist(storage.bill.find()), key=lambda x: x.supplier_bill_id)
    assert_that(
        bills,
        contains_inanyorder(
            has_properties(
                supplier_bill_id='1',
                status=BillStatus.NEW,
                amount=search_response.charges[0].charge_data.amount,
                document_id=documents[0].document_id,
            ),
            has_properties(
                supplier_bill_id='2', status=BillStatus.PAID, amount=200, document_id=documents[1].document_id
            ),
            has_properties(
                supplier_bill_id='3',
                status=BillStatus.GONE,
            ),
        ),
    )


@pytest.mark.asyncio
@freeze_time('2021-12-07 20:20:20')
async def test_task_retried_on_part_response(user, mocker, search_response, storage, documents):
    search_response.status = SearchStatus.PART
    mocker.patch.object(KaznaClient, 'search', mocker.AsyncMock(return_value=search_response))
    mocker.patch.object(KaznaClient, 'get_subscription', mocker.AsyncMock(return_value=[]))

    await SearchFinesAction(uid=user.uid, revision=1, sync_subscription=True).run()

    assert_that(await alist(storage.bill.find()), has_length(0))
    created_tasks = await alist(storage.task.find())
    assert_that(
        created_tasks,
        contains_inanyorder(
            has_properties(
                action_name='search_fines',
                run_at=utcnow() + timedelta(seconds=1),
                state=TaskState.PENDING,
                params={'max_retries': 10, 'action_kwargs': {'uid': user.uid, 'revision': 1}},
            )
        ),
    )


@pytest.mark.asyncio
@freeze_time('2021-12-07 20:20:20')
async def test_task_retried_if_previous_task_is_running(user, mocker, search_response, storage, documents):
    search_response.status = SearchStatus.PART
    mocker.patch.object(KaznaClient, 'search', mocker.AsyncMock(return_value=search_response))
    await SearchFinesAction(uid=user.uid, revision=1).run()
    new_revision = await storage.user.increment_revision(user.uid)

    await SearchFinesAction(uid=user.uid, revision=new_revision).run()

    assert_that(await alist(storage.bill.find()), has_length(0))
    assert_that((await storage.user.get(user.uid)).syncing_revision, equal_to(1))
    created_tasks = await alist(storage.task.find())
    assert_that(
        created_tasks,
        has_item(
            has_properties(
                action_name='search_fines',
                run_at=utcnow() + timedelta(seconds=1),
                state=TaskState.PENDING,
                params={'max_retries': 10, 'action_kwargs': {'uid': user.uid, 'revision': new_revision}},
            )
        ),
    )


@pytest.mark.asyncio
async def test_obsolete_task_cleans_state(user, mocker, search_response, storage, documents):
    search_response.status = SearchStatus.PART
    mocker.patch.object(KaznaClient, 'search', mocker.AsyncMock(return_value=search_response))
    await SearchFinesAction(uid=user.uid, revision=1).run()
    new_revision = await storage.user.increment_revision(user.uid)

    await SearchFinesAction(uid=user.uid, revision=1).run()

    updated_user = await storage.user.get(user.uid)
    assert_that(updated_user, has_properties(revision=new_revision, syncing_revision=None, synced_revision=0))


@pytest.mark.asyncio
@freeze_time('2021-12-07 20:20:20')
async def test_schedule(storage, user):
    revision = await SearchFinesAction.schedule(storage, user.uid)

    created_tasks = await alist(storage.task.find())
    assert_that(
        created_tasks,
        has_item(
            has_properties(
                action_name='search_fines',
                run_at=utcnow(),
                state=TaskState.PENDING,
                params={
                    'max_retries': 10,
                    'action_kwargs': {'uid': user.uid, 'revision': revision, 'sync_subscription': False},
                },
            )
        ),
    )
    assert_that(await storage.user.get(user.uid), has_properties(revision=revision))
    assert_that(await storage.user.get(user.uid), has_properties(revision=user.revision + 1))


@pytest.mark.asyncio
async def test_user_does_not_exist():
    with pytest.raises(UserNotFoundError):
        await SearchFinesAction(uid=404, revision=1).run()


@pytest.mark.asyncio
async def test_document_not_found(user, mocker, search_response, documents):
    search_response.charges[0].document.value = ['1']
    mocker.patch.object(KaznaClient, 'search', mocker.AsyncMock(return_value=search_response))

    with pytest.raises(DocumentNotFoundError):
        await SearchFinesAction(uid=user.uid, revision=1).run()


@pytest.mark.asyncio
async def test_unsubscribe_called(user, mocker, search_response):
    unsubscribe_mock = mocker.patch.object(KaznaClient, 'unsubscribe', mocker.AsyncMock())
    search_mock = mocker.patch.object(KaznaClient, 'search', mocker.AsyncMock(return_value=search_response))
    mocker.patch.object(
        KaznaClient,
        'get_subscription',
        mocker.AsyncMock(return_value=[PayerDoc(code=DocumentCode.DRIVER_LICENSE, value=['1122334455', '1122334400'])]),
    )

    await SearchFinesAction(uid=user.uid, revision=1, sync_subscription=True).run()

    search_mock.assert_not_called()
    unsubscribe_mock.assert_called_once()
    _, call_kwargs = unsubscribe_mock.call_args
    assert_that(
        call_kwargs,
        equal_to(
            {
                'subscription_id': user.subscription_id,
                'documents': [
                    PayerDoc(code=DocumentCode.DRIVER_LICENSE, value=['1122334455']),
                    PayerDoc(code=DocumentCode.DRIVER_LICENSE, value=['1122334400']),
                ],
            }
        ),
    )
