import logging
from datetime import datetime, timedelta
from time import sleep
import allure
import hamcrest as hm
import pytest

from billing.hot.tests.clients.processor.taxi_client import Client as TaxiClient
from billing.hot.tests.lib.date.timestamp import now_dt_ms, now_timestamp_utc
from billing.hot.tests.lib.matchers.base import (
    objects_with_properties,
    success_processor_response_entries,
)
from billing.hot.tests.lib.state import contract
from billing.hot.tests.lib.state import state

logging.disable(logging.DEBUG)

"""
* Создаем удержание за машину на 600р
* Создаем удержание за камеру на 400р
* Безнал 200р
* Взаимозачёт, удержали 200р
* Выплата
* Безнал 500р
* Взаимозачёт, удержали 500р
* Пришла отмена, мы отменим 300р
"""

CAR_AND_BOX = "0"
CAMERA = "1"

RENT_AMOUNT = 600.0
CAMERA_AMOUNT = 400.0


def gen_transfer_init_event_ids(ids):
    return ["-".join([str(i), "transfer-init"]) for i in ids]


async def cashless_action(accounts_db, taxi_client, sender_state, cashless_amount):
    async with taxi_client.cashless(
        sender_state,
        extended_params={
            "amount": cashless_amount,
        },
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))

    transaction_ids = sender_state.new_transaction_ids
    event_batches = await accounts_db.get_event_batches(transaction_ids)
    hm.assert_that(event_batches, hm.has_length(len(transaction_ids)))
    return transaction_ids


async def payout_action(
    accounts_db, taxi_client, sender_state, receiver_state, batches_count
):
    async with taxi_client.payout(sender_state) as response:
        hm.assert_that(response.status, hm.equal_to(200))

    hm.assert_that(sender_state.new_transaction_ids, hm.has_length(1))
    sender_payout_transaction_id = sender_state.new_transaction_ids[0]

    receiver_payout_transaction_id = "-".join(
        map(str, [sender_payout_transaction_id, receiver_state.client_id])
    )
    transfer_id = "-".join(map(str, [sender_payout_transaction_id, "transfer"]))
    event_batches = await accounts_db.get_event_batches(
        [sender_payout_transaction_id, receiver_payout_transaction_id, transfer_id]
    )
    hm.assert_that(event_batches, hm.has_length(batches_count))
    return response, event_batches


async def rent_transfer_init(accounts_db, taxi_client, sender_state, receiver_state):
    async with taxi_client.transfer_init(
        sender_state,
        receiver_state,
        extended_params={
            "amount": RENT_AMOUNT,
            "transaction_type": "PAYMENT",
            "transfer_type": "selfemployed_rent",
        },
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))

    # проверяем что в аккаунтер проросли события транзакций и трансферов
    transaction_ids = sender_state.new_transaction_ids
    transfer_ids = gen_transfer_init_event_ids(transaction_ids)
    event_batches = await accounts_db.get_event_batches(
        [*transaction_ids, *transfer_ids]
    )
    hm.assert_that(
        event_batches, hm.has_length(len(transaction_ids) + len(transfer_ids))
    )

    events = await accounts_db.get_events_by_batch_id([e.id for e in event_batches])

    hm.assert_that(
        events,
        objects_with_properties(
            {
                "amount": [RENT_AMOUNT],
                "account_type": ["transfer_source_selfemployed_rent"],
                "type": ["debit"],
            }
        ),
    )

    # и состояние стейта
    states = await accounts_db.get_states(
        "transfer_queue_state", [sender_state.client_id]
    )
    hm.assert_that(states, hm.has_length(1))
    hm.assert_that(
        states[0].state,
        hm.has_entries(
            {
                CAR_AND_BOX: hm.has_entries(
                    {
                        str(
                            sender_state.get_contract(contract.ServiceContract).id
                        ): hm.contains_inanyorder(
                            *[
                                hm.has_entries(
                                    {
                                        "transfer_type": "selfemployed_rent",
                                        "transaction_id": transaction_id,
                                        "amount": RENT_AMOUNT,
                                        "remains": RENT_AMOUNT,
                                        "transaction_type": "PAYMENT",
                                        "sender_billing_client_id": sender_state.client_id,
                                    }
                                )
                                for transaction_id in transaction_ids
                            ]
                        )
                    }
                )
            }
        ),
    )

    return transaction_ids


async def camera_transfer_init(
    accounts_db,
    taxi_client,
    sender_state,
    receiver_state,
    rent_transaction_ids,
):
    async with taxi_client.transfer_init(
        sender_state,
        receiver_state,
        extended_params={
            "amount": CAMERA_AMOUNT,
            "transaction_type": "PAYMENT",
            "transfer_type": "signalq_rent",
        },
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
    # проверяем что в аккаунтер проросли события транзакций и трансферов
    transaction_ids = sender_state.new_transaction_ids
    transfer_ids = gen_transfer_init_event_ids(transaction_ids)
    event_batches = await accounts_db.get_event_batches(
        [*transaction_ids, *transfer_ids]
    )
    hm.assert_that(
        event_batches, hm.has_length(len(transaction_ids) + len(transfer_ids))
    )
    events = await accounts_db.get_events_by_batch_id([e.id for e in event_batches])

    hm.assert_that(
        events,
        objects_with_properties(
            {
                "amount": [CAMERA_AMOUNT],
                "account_type": ["transfer_source_signalq_rent"],
                "type": ["debit"],
            }
        ),
    )

    states = await accounts_db.get_states(
        "transfer_queue_state", [sender_state.client_id]
    )
    hm.assert_that(states, hm.has_length(1))
    hm.assert_that(
        states[0].state,
        hm.has_entries(
            {
                # проверим что не были удалены ранее созданные
                CAR_AND_BOX: hm.has_entries(
                    {
                        str(
                            sender_state.get_contract(contract.ServiceContract).id
                        ): hm.contains_inanyorder(
                            *[
                                hm.has_entries(
                                    {
                                        "transfer_type": "selfemployed_rent",
                                        "transaction_id": transaction_id,
                                        "amount": RENT_AMOUNT,
                                        "remains": RENT_AMOUNT,
                                        "transaction_type": "PAYMENT",
                                        "sender_billing_client_id": sender_state.client_id,
                                    }
                                )
                                for transaction_id in rent_transaction_ids
                            ]
                        )
                    }
                ),
                # и добавлены новые
                CAMERA: hm.has_entries(
                    {
                        str(
                            sender_state.get_contract(contract.ServiceContract).id
                        ): hm.contains_inanyorder(
                            *[
                                hm.has_entries(
                                    {
                                        "transfer_type": "signalq_rent",
                                        "transaction_id": transaction_id,
                                        "amount": CAMERA_AMOUNT,
                                        "remains": CAMERA_AMOUNT,
                                        "transaction_type": "PAYMENT",
                                        "sender_billing_client_id": sender_state.client_id,
                                    }
                                )
                                for transaction_id in transaction_ids
                            ]
                        )
                    }
                ),
            }
        ),
    )

    return transaction_ids


async def first_payout(
    accounts_db,
    taxi_client,
    sender_state,
    receiver_state,
    rent_transaction_ids,
    camera_transaction_ids,
    cashless_amount,
):
    response, event_batches = await payout_action(
        accounts_db, taxi_client, sender_state, receiver_state, 2
    )
    hm.assert_that(
        await response.json(),
        success_processor_response_entries(
            {
                "event": {
                    "tariffer_payload": {
                        "processed_transfer_events": hm.contains(
                            *[
                                hm.has_entries(
                                    {
                                        "transfer_type": "signalq_rent",
                                        "amount": CAMERA_AMOUNT,
                                        "remains": CAMERA_AMOUNT - cashless_amount,
                                    }
                                )
                            ]
                        )
                    }
                }
            }
        ),
    )
    sleep(0.5)
    """
    приоритет удержаний - камеры, аренда авто/бокса
    положили 200 - должны частично покрыть выплату за камеру, остаток 200
    """

    events = await accounts_db.get_events_by_batch_id([e.id for e in event_batches])
    hm.assert_that(
        events,
        objects_with_properties(
            {
                "account_attribute_1": [
                    *[str(sender_state.client_id)] * 5,
                ],  # user_id
                "amount": [0, *[cashless_amount] * 4],
                "account_type": [
                    "payout",
                    "transfer_source_signalq_rent",
                    "transfer_hold_signalq_rent",
                    "cashless",
                    "transfer_hold_signalq_rent",
                ],
                "type": ["credit", "credit", "debit", "debit", "credit"],
            }
        ),
    )

    states = await accounts_db.get_states(
        "transfer_queue_state", [sender_state.client_id]
    )
    hm.assert_that(states, hm.has_length(1))
    hm.assert_that(
        states[0].state,
        hm.has_entries(
            {
                CAR_AND_BOX: hm.has_entries(
                    {
                        str(
                            sender_state.get_contract(contract.ServiceContract).id
                        ): hm.contains_inanyorder(
                            *[
                                hm.has_entries(
                                    {
                                        "transfer_type": "selfemployed_rent",
                                        "transaction_id": transaction_id,
                                        "amount": RENT_AMOUNT,
                                        "remains": RENT_AMOUNT,
                                        "transaction_type": "PAYMENT",
                                        "sender_billing_client_id": sender_state.client_id,
                                    }
                                )
                                for transaction_id in rent_transaction_ids
                            ]
                        )
                    }
                ),
                CAMERA: hm.has_entries(
                    {
                        str(
                            sender_state.get_contract(contract.ServiceContract).id
                        ): hm.contains_inanyorder(
                            *[
                                hm.has_entries(
                                    {
                                        "transfer_type": "signalq_rent",
                                        "transaction_id": transaction_id,
                                        "amount": CAMERA_AMOUNT,
                                        "remains": CAMERA_AMOUNT - cashless_amount,
                                        "transaction_type": "PAYMENT",
                                        "sender_billing_client_id": sender_state.client_id,
                                    }
                                )
                                for transaction_id in camera_transaction_ids
                            ]
                        )
                    }
                ),
            }
        ),
    )


async def second_payout(
    accounts_db,
    taxi_client,
    sender_state,
    receiver_state,
    rent_transaction_ids,
    camera_transaction_ids,
    fst_cashless_amount,
    sd_cashless_amount,
):
    total_cashless_amount = fst_cashless_amount + sd_cashless_amount
    response, event_batches = await payout_action(
        accounts_db, taxi_client, sender_state, receiver_state, 3
    )
    hm.assert_that(
        await response.json(),
        success_processor_response_entries(
            {
                "event": {
                    "tariffer_payload": {
                        "processed_transfer_events": hm.contains(
                            *[
                                hm.has_entries(
                                    {
                                        "transfer_type": "signalq_rent",
                                        "amount": CAMERA_AMOUNT,
                                        "remains": 0,
                                    }
                                ),
                                hm.has_entries(
                                    {
                                        "transfer_type": "selfemployed_rent",
                                        "amount": RENT_AMOUNT,
                                        "remains": RENT_AMOUNT
                                                   - (total_cashless_amount - CAMERA_AMOUNT),
                                    }
                                ),
                            ]
                        ),
                    }
                }
            }
        ),
    )
    sleep(0.5)
    """
    приоритет удержаний - камеры, аренда авто/бокса
    положили 200 + 500 - должны полностью покрыть выплату за камеру и частично за авто, остаток 1000 - 700 = 300
    """

    events = await accounts_db.get_events_by_batch_id([e.id for e in event_batches])
    camera_remains = CAMERA_AMOUNT - fst_cashless_amount
    rent_amount = sd_cashless_amount - camera_remains
    hm.assert_that(
        events,
        objects_with_properties(
            {
                "account_attribute_1": [
                    str(sender_state.client_id),
                    *[*[str(sender_state.client_id)] * 4],  # camera
                    *[
                        *[str(sender_state.client_id)] * 4,
                        *[str(receiver_state.client_id)] * 2,
                    ],  # rent
                ],  # user_id
                "amount": [0, *[camera_remains] * 4, *[rent_amount] * 6],
                "account_type": [
                    "payout",
                    # camera netting
                    "transfer_source_signalq_rent",
                    "transfer_hold_signalq_rent",
                    "cashless",
                    "transfer_hold_signalq_rent",
                    # rent netting
                    "transfer_source_selfemployed_rent",
                    "transfer_hold_selfemployed_rent",
                    "cashless",
                    "transfer_hold_selfemployed_rent",
                    "payout",
                    "foreign_income_selfemployed_rent",
                ],
                "type": [
                    "credit",
                    *["credit", "debit", "debit", "credit"],
                    *["credit", "debit", "debit", "credit", "credit", "debit"],
                ],
            }
        ),
    )

    states = await accounts_db.get_states(
        "transfer_queue_state", [sender_state.client_id]
    )
    hm.assert_that(states, hm.has_length(1))
    hm.assert_that(
        states[0].state,
        hm.has_entries(
            {
                CAR_AND_BOX: hm.has_entries(
                    {
                        str(
                            sender_state.get_contract(contract.ServiceContract).id
                        ): hm.contains_inanyorder(
                            *[
                                hm.has_entries(
                                    {
                                        "transfer_type": "selfemployed_rent",
                                        "transaction_id": transaction_id,
                                        "amount": RENT_AMOUNT,
                                        "remains": RENT_AMOUNT - rent_amount,
                                        "transaction_type": "PAYMENT",
                                        "sender_billing_client_id": sender_state.client_id,
                                    }
                                )
                                for transaction_id in rent_transaction_ids
                            ]
                        )
                    }
                ),
                CAMERA: hm.empty(),
            }
        ),
    )


async def rent_transfer_cancel(
    accounts_db, taxi_client, sender_state, rent_transaction_ids, remains, dry_run
):
    rent_transaction_id = rent_transaction_ids[0]
    # отменяем остаток трансфера за аренду авто
    cancel_timestamp = now_timestamp_utc()
    async with taxi_client.transfer_cancel(
        sender_state,
        rent_transaction_id,
    ) as response:
        if dry_run:
            hm.assert_that(response.status, hm.equal_to(404))
        else:
            hm.assert_that(response.status, hm.equal_to(200))
            hm.assert_that(
                await response.json(),
                success_processor_response_entries(
                    {
                        "event": {
                            "tariffer_payload": {
                                "cancelled_event": {
                                    "transaction_id": rent_transaction_id,
                                    "remains": remains,
                                }
                            }
                        }
                    }
                ),
            )

    # проверяем что в аккаунтер проросли события транзакций и трансферов
    cancel_event_id = rent_transaction_id + "-" + "transfer-cancel"
    cancel_transaction_id = rent_transaction_id + "-" + "transfer-cancel-transaction"
    cancel_event_batches = await accounts_db.get_event_batches(
        [cancel_event_id, cancel_transaction_id]
    )
    hm.assert_that(cancel_event_batches, hm.has_length(2))
    cancel_events = await accounts_db.get_events_by_batch_id(
        [e.id for e in cancel_event_batches]
    )

    hm.assert_that(
        cancel_events,
        objects_with_properties(
            {
                "amount": [remains] * 2,
                "account_type": [
                    "transfer_source_selfemployed_rent",
                    "transfer_cancel_selfemployed_rent",
                ],
                "type": ["credit", "debit"],
            }
        ),
    )

    states = await accounts_db.get_states(
        "transfer_queue_state", [sender_state.client_id]
    )
    """
    приоритет удержаний - камеры, аренда авто/бокса
    положили 200 + 500 - должны полностью покрыть выплату за камеру и частично за авто, остаток 1000 - 700 = 300
    """
    hm.assert_that(states, hm.has_length(1))
    hm.assert_that(
        states[0].state,
        hm.has_entries(
            {
                CAR_AND_BOX: hm.has_entries(
                    {
                        str(
                            sender_state.get_contract(contract.ServiceContract).id
                        ): hm.contains_inanyorder(
                            *[
                                hm.has_entries(
                                    {
                                        # отмененное событие
                                        "removed_timestamp": hm.greater_than(
                                            cancel_timestamp
                                        ),
                                        "transfer_type": "selfemployed_rent",
                                        "transaction_id": rent_transaction_id,
                                        "amount": RENT_AMOUNT,
                                        "remains": remains,
                                        "transaction_type": "PAYMENT",
                                        "sender_billing_client_id": sender_state.client_id,
                                    }
                                )
                            ]
                        )
                    }
                )
            }
        ),
    )


async def sender_payout_check(taxi_oebs_pipeline, payout_client, sender_state, dry_run):
    # т.к у отправителя все деньги ушли на выплаты третьим лицам - выплаты в его сторону нет
    now = datetime.now()
    async with payout_client.payout_by_client(sender_state) as response:
        hm.assert_that(response.status, hm.equal_to(201))

    records = await payout_client.poll_payout_info(
        sender_state,
        from_date=now - timedelta(days=1),
        statuses=["done"],
        expected_records=1,
    )

    hm.assert_that(records, hm.has_length(1))
    hm.assert_that(
        records[0],
        hm.has_entries(
            {
                "amount": "0",
                "dry_run": dry_run,
            }
        ),
    )
    await taxi_oebs_pipeline.run_ok_pipeline(
        sender_state,
        expected_payouts_count=0,
        from_dt_ms=now_dt_ms(now),
        dry_run=dry_run,
    )


async def receiver_payout_check(
    taxi_oebs_pipeline, payout_client, receiver_state, payout_amount, dry_run
):
    now = datetime.now()
    async with payout_client.payout_by_client(receiver_state) as response:
        hm.assert_that(response.status, hm.equal_to(201))

    records = await payout_client.poll_payout_info(
        receiver_state,
        from_date=now - timedelta(days=1),
        statuses=["pending"],
        expected_records=1,
    )

    hm.assert_that(records, hm.has_length(1))
    hm.assert_that(
        records[0],
        hm.has_entries(
            {
                "dry_run": dry_run,
            }
        ),
    )

    await taxi_oebs_pipeline.run_ok_pipeline(
        receiver_state,
        expected_payouts_count=1,
        from_dt_ms=now_dt_ms(now),
        dry_run=dry_run,
    )

    records = await payout_client.poll_payout_info(
        receiver_state,
        from_date=now - timedelta(days=1),
        statuses=["done"],
        expected_records=1,
    )

    hm.assert_that(records, hm.has_length(1))
    hm.assert_that(
        records[0],
        hm.has_entries(
            {
                "amount": str(int(payout_amount)),
                "dry_run": dry_run,
            }
        ),
    )


@pytest.mark.asyncio
@pytest.mark.parametrize("dry_run", [True, False])
async def test_transfer(
    accounts_db,
    taxi_client: TaxiClient,
    create_state_builder,
    taxi_oebs_pipeline,
    payout_client,
    dry_run,
):
    st = state.PipelineState.generate()
    sender_builder = create_state_builder(st)
    sender_builder.fill_contracts(dry_run=dry_run)
    sender_state = sender_builder.built_state()

    receiver_builder = create_state_builder()
    receiver_builder.fill_contracts([contract.TransferContract])
    receiver_state = receiver_builder.built_state()

    with allure.step('Инициализируем трансфер за аренду авто'):
        rent_transaction_ids = await rent_transfer_init(
            accounts_db, taxi_client, sender_state, receiver_state
        )

    with allure.step('Инициализируем трансфер за аренду камеры'):
        camera_transaction_ids = await camera_transfer_init(
            accounts_db, taxi_client, sender_state, receiver_state, rent_transaction_ids
        )

    with allure.step('Кладем на счет 200 безнала'):
        fst_cashless_amount = 200
        await cashless_action(accounts_db, taxi_client, sender_state, fst_cashless_amount)

    with allure.step('Взаимозачет 1'):
        await first_payout(
            accounts_db,
            taxi_client,
            sender_state,
            receiver_state,
            rent_transaction_ids,
            camera_transaction_ids,
            fst_cashless_amount,
        )

    with allure.step('Кладем на счет 500 безнала'):
        sd_cashless_amount = 500
        await cashless_action(accounts_db, taxi_client, sender_state, sd_cashless_amount)

    with allure.step('Взаимозачет 2'):
        await second_payout(
            accounts_db,
            taxi_client,
            sender_state,
            receiver_state,
            rent_transaction_ids,
            camera_transaction_ids,
            fst_cashless_amount,
            sd_cashless_amount,
        )

    with allure.step('rent_transfer_cancel'):
        total_cashless_amount = fst_cashless_amount + sd_cashless_amount
        remains = CAMERA_AMOUNT + RENT_AMOUNT - total_cashless_amount
        await rent_transfer_cancel(
            accounts_db, taxi_client, sender_state, rent_transaction_ids, remains, dry_run
        )

    with allure.step('Деньги за camera события не выплачиваются'):
        await receiver_payout_check(
            taxi_oebs_pipeline,
            payout_client,
            receiver_state,
            total_cashless_amount - CAMERA_AMOUNT,
            dry_run,
        )

    with allure.step('sender_payout_check'):
        await sender_payout_check(taxi_oebs_pipeline, payout_client, sender_state, dry_run)
