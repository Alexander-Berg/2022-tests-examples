from datetime import datetime, timedelta

import arrow
import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries, has_key

from billing.hot.calculators.taxi.calculator.core.constants import CASHLESS_SID, REQUEST_SID
from billing.hot.calculators.taxi.calculator.tests.builder import (
    gen_general_contract, gen_loc, gen_transfer_cancel_event, gen_transfer_event
)
from billing.hot.calculators.taxi.calculator.tests.const import (
    CLIENT_ID, CLIENT_SD_ID, COMMON_DT, COMMON_DT_TS, GENERAL_CONTRACT_ID, GENERAL_SD_CONTRACT_ID
)

from . import expected_client_transactions

NOW = datetime.now()

car_amount = 200.0
box_amount = 300.0
camera_amount = 400.0

car_event = gen_transfer_event(CLIENT_ID, GENERAL_CONTRACT_ID, CLIENT_SD_ID, GENERAL_SD_CONTRACT_ID,
                               transaction_id='100', amount=car_amount, transfer_type='selfemployed_rent', dt=COMMON_DT)
car_state_event = dict(car_event)

expired_car_event = gen_transfer_event(CLIENT_ID, GENERAL_CONTRACT_ID, CLIENT_SD_ID, GENERAL_SD_CONTRACT_ID,
                                       transaction_id='11100', amount=car_amount, transfer_type='selfemployed_rent',
                                       dt=COMMON_DT, removed_timestamp=(NOW - timedelta(days=7, seconds=1)).timestamp())
expired_car_state_event = dict(expired_car_event)

box_event = gen_transfer_event(CLIENT_ID, GENERAL_CONTRACT_ID, CLIENT_SD_ID, GENERAL_SD_CONTRACT_ID,
                               transaction_id='101', amount=box_amount, transfer_type='lightbox_rent', dt=COMMON_DT)
box_state_event = dict(box_event)

removed_box_event_2 = gen_transfer_event(CLIENT_ID, GENERAL_CONTRACT_ID, CLIENT_SD_ID, GENERAL_SD_CONTRACT_ID,
                                         transaction_id='11101', amount=box_amount, transfer_type='lightbox_rent',
                                         dt=COMMON_DT, removed_timestamp=(NOW - timedelta(days=3)).timestamp())

removed_box_state_event_2 = dict(removed_box_event_2)
camera_event = gen_transfer_event(CLIENT_ID, GENERAL_CONTRACT_ID, CLIENT_SD_ID, GENERAL_SD_CONTRACT_ID,
                                  transaction_id='102', amount=camera_amount, transfer_type='signalq_rent',
                                  dt=COMMON_DT)
camera_state_event = dict(camera_event)

default_tariffer_payload = {'common_ts': COMMON_DT_TS, 'dry_run': False}

car_state_event.update({'remains': car_amount, 'processed': None,
                        'payload': None, 'tariffer_payload': {**default_tariffer_payload},
                        'removed_timestamp': None,
                        })
box_state_event.update({'remains': box_amount, 'processed': None,
                        'payload': None, 'tariffer_payload': {**default_tariffer_payload},
                        'removed_timestamp': None,
                        })

camera_state_event.update({'remains': camera_amount, 'processed': None,
                           'payload': None, 'tariffer_payload': {**default_tariffer_payload},
                           'removed_timestamp': None,
                           })

removed_car_state_event = {**car_state_event, **{'removed_timestamp': NOW.timestamp()}}

removed_camera_state_event = {**camera_state_event, **{'removed_timestamp': NOW.timestamp()}}

removed_box_state_event_2.update({'remains': box_amount, 'processed': None,
                                  'payload': None, 'tariffer_payload': {**default_tariffer_payload},
                                  })

init_params = [
    pytest.param(
        car_event, True, [],
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [car_state_event]}
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('transfer_source_selfemployed_rent',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': car_amount,
                     'type': 'debit'},
                ]
            }
        ],
        id='Add car event to empty states',
    ),
    pytest.param(
        camera_event, True, [],
        [
            {'state': {
                '1': {str(GENERAL_CONTRACT_ID): [camera_state_event]}
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('transfer_source_signalq_rent',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': camera_amount,
                     'type': 'debit'},
                ]
            }
        ],
        id='camera event to empty states',
    ),
    pytest.param(
        box_event, True,
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [car_state_event, car_state_event]},
                '1': {str(GENERAL_CONTRACT_ID): [camera_state_event]}
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [car_state_event, car_state_event, box_state_event]},
                '1': {str(GENERAL_CONTRACT_ID): [camera_state_event]}
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('transfer_source_lightbox_rent',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': box_amount,
                     'type': 'debit'},
                ]
            }
        ],
        id='Add box event to not empty states',
    ),
    pytest.param(
        camera_event, True,
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [box_state_event, car_state_event]},
                '1': {str(GENERAL_CONTRACT_ID): [car_state_event]}
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [box_state_event, car_state_event]},
                '1': {str(GENERAL_CONTRACT_ID): [car_state_event, camera_state_event]}
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('transfer_source_signalq_rent',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': camera_amount,
                     'type': 'debit'},
                ]
            }
        ],
        id='Add camera event to not empty states',
    ),
    pytest.param(
        camera_event, False,
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [box_state_event]},
                '1': {str(GENERAL_CONTRACT_ID): [car_state_event]}
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [box_state_event]},
                '1': {str(GENERAL_CONTRACT_ID): [car_state_event]}
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [],
        id='Add camera event to not empty states does not change states if not migrated',
    ),
]

cancel_params = [
    pytest.param(
        gen_transfer_cancel_event(CLIENT_ID, GENERAL_CONTRACT_ID, car_event['transaction_id']),
        True,
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [car_state_event, box_state_event]},
                '1': {str(GENERAL_CONTRACT_ID): [camera_state_event]}
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [removed_car_state_event, box_state_event]},
                '1': {str(GENERAL_CONTRACT_ID): [camera_state_event]},
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('transfer_source_selfemployed_rent',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': car_amount,
                     'type': 'credit'},
                    {'loc': gen_loc('transfer_cancel_selfemployed_rent',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': car_amount,
                     'type': 'debit'},
                ]
            }
        ],
        id='Cancel car event',
    ),
    pytest.param(
        gen_transfer_cancel_event(CLIENT_ID, GENERAL_CONTRACT_ID, removed_box_event_2['transaction_id']),
        True,
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [car_state_event, removed_box_state_event_2]},
                '1': {str(GENERAL_CONTRACT_ID): [camera_state_event]}
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [car_state_event, removed_box_state_event_2]},
                '1': {str(GENERAL_CONTRACT_ID): [camera_state_event]},
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [],
        id='Cancel cancelled box event',
    ),
    pytest.param(
        gen_transfer_cancel_event(CLIENT_ID, GENERAL_CONTRACT_ID, car_event['transaction_id']),
        True,
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [removed_box_state_event_2, expired_car_state_event, car_state_event,
                                                 box_state_event]},
                '1': {str(GENERAL_CONTRACT_ID): [camera_state_event]},
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [removed_box_state_event_2, removed_car_state_event, box_state_event]},
                '1': {str(GENERAL_CONTRACT_ID): [camera_state_event]},
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('transfer_source_selfemployed_rent',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': car_amount,
                     'type': 'credit'},
                    {'loc': gen_loc('transfer_cancel_selfemployed_rent',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': car_amount,
                     'type': 'debit'},
                ]
            }
        ],
        id='Cancel car event with one old and expired object',
    ),
    pytest.param(
        gen_transfer_cancel_event(CLIENT_ID, GENERAL_CONTRACT_ID, camera_event['transaction_id']),
        True,
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [box_state_event, car_state_event]},
                '1': {str(GENERAL_CONTRACT_ID): [car_state_event, camera_state_event]}
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {'state': {
                '0': {str(GENERAL_CONTRACT_ID): [box_state_event, car_state_event]},
                '1': {str(GENERAL_CONTRACT_ID): [car_state_event, removed_camera_state_event]},
            }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}
        ],
        [
            {
                'client_id': CLIENT_ID,
                'transactions': [
                    {'loc': gen_loc('transfer_source_signalq_rent',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': camera_amount,
                     'type': 'credit'},
                    {'loc': gen_loc('transfer_cancel_signalq_rent',
                                    **{'client_id': CLIENT_ID, 'contract_id': GENERAL_CONTRACT_ID, 'currency': 'RUB'}),
                     'amount': camera_amount,
                     'type': 'debit'},
                ]
            }
        ],
        id='Cancel camera event',
    ),
    pytest.param(
        gen_transfer_cancel_event(CLIENT_ID, GENERAL_CONTRACT_ID, car_event['transaction_id']),
        False,
        [{'state': {
            '0': {str(GENERAL_CONTRACT_ID): [box_state_event, car_state_event]},
            '1': {str(GENERAL_CONTRACT_ID): [car_state_event]}
        }, 'loc': gen_loc('transfer_queue_state', **{'client_id': 1})}],
        [],
        [],
        id='Cancel event raise 404 error if client not migrated',
    ),
]


class TestTransfer:
    @pytest.mark.asyncio
    @pytest.mark.parametrize('event,migrated,transfer_queue_states,states,client_transactions', init_params)
    async def test_init(self, make_request, event, migrated, transfer_queue_states, states, client_transactions):
        dt = COMMON_DT if migrated else COMMON_DT + timedelta(seconds=1)
        jval = {
            'event': event,
            'references': {
                'migration_info': [{'namespace': 'taxi', 'filter': 'Client', 'object_id': CLIENT_ID,
                                    'from_dt': dt.isoformat(), 'dry_run': False}],
                'sender_contracts': [
                    gen_general_contract(
                        GENERAL_CONTRACT_ID, CLIENT_ID, 1, services=[CASHLESS_SID]
                    )
                ],
                'recipient_contracts': [
                    gen_general_contract(
                        GENERAL_SD_CONTRACT_ID, CLIENT_SD_ID, 2, services=[REQUEST_SID]
                    )
                ],
                'lock': {
                    'states': transfer_queue_states,
                },
            },
        }
        res = (await make_request('transfer-init', jval))['data']

        assert_that(res, has_key('event'))
        assert_that(res['event'], has_entries(event))
        assert res['states'] == states
        assert_that(res['client_transactions'],
                    contains_inanyorder(*expected_client_transactions(client_transactions)))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('event,migrated,transfer_queue_states,states,client_transactions', cancel_params)
    async def test_cancel(self, make_request, mocker,
                          event, migrated, transfer_queue_states, states, client_transactions,
                          ):
        dt = COMMON_DT if migrated else arrow.now() + timedelta(seconds=10)
        jval = {
            'event': event,
            'references': {
                'migration_info': [{'namespace': 'taxi', 'filter': 'Client', 'object_id': CLIENT_ID,
                                    'from_dt': dt.isoformat(), 'dry_run': False}],
                'sender_contracts': [
                    gen_general_contract(
                        GENERAL_CONTRACT_ID, CLIENT_ID, 1, services=[CASHLESS_SID]
                    )
                ],
                'lock': {
                    'states': transfer_queue_states,
                },
            },
        }
        mocker.patch('billing.hot.calculators.taxi.calculator.core.actions.transfer.utc_timestamp',
                     return_value=NOW.timestamp())
        res = (await make_request('transfer-cancel', jval))['data']
        if not migrated:
            assert_that(res, has_entries({'message': 'TRANSFER_NOT_FOUND_ERROR'}))
            return

        assert_that(res, has_key('event'))
        assert_that(res['event'], has_entries(event))
        assert res['states'] == states
        assert_that(res['client_transactions'],
                    contains_inanyorder(*expected_client_transactions(client_transactions)))
