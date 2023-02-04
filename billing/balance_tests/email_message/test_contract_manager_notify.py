# -*- coding: utf-8 -*-
import datetime
import pytest
import hamcrest as hm

from balance import constants as cst, mapper
from cluster_tools import manager_contract_notify
from mailer.balance_mailer import MessageData
from tests import object_builder as ob


TODAY = datetime.datetime.now() - datetime.timedelta(hours=1)
CONTRACT_FIRM_ID = cst.FirmId.YANDEX_OOO

original_and_faxed_title = manager_contract_notify.OriginalAndFaxedContractsBase.title
original_title = manager_contract_notify.OriginalContractsBase.title
faxed_title = manager_contract_notify.FaxedContracts.title
booked_title = manager_contract_notify.BookedContracts.title


@pytest.fixture(name='manager', scope='function')
def create_manager(session):
    return ob.SingleManagerBuilder.construct(
        session,
        name='Unit Test Name %s' % ob.get_big_number(),
        login='unit-test-12345',
        email='platform9.75@hogwarts.magic',
    )


@pytest.fixture(name='contract')
def create_signed_contract(
        session,
        manager,
        end_dt=TODAY,
        is_signed=None,
        is_booked=False,
        is_faxed=None,
):
    contract = ob.ContractBuilder.construct(
        session,
        manager=manager,
        is_signed=TODAY if is_signed else None,
        is_faxed=TODAY if is_faxed else None,
        is_booked=is_booked,
        person=ob.PersonBuilder(),
    )

    signed_contract = mapper.SignedContract(
        id=contract.id,
        contract_id=contract.id,
        contract_eid=contract.external_id,
        manager_code=manager.manager_code,
        manager_name=manager.name,
        manager_login=manager.login,
        manager_email=manager.email,
        person_id=ob.get_big_number(),
        person_name='PPP name',
        is_signed=TODAY if is_signed else None,
        is_faxed=TODAY if is_faxed else None,
        is_booked=is_booked,
    )
    session.add(signed_contract)
    session.flush()

    # т.к. триггер вставляет в end_dt значение sysdate
    for cch in session.query(mapper.ContractCollateralHistory).filter_by(contract2_id=contract.id):
        cch.end_dt = end_dt
    session.flush()

    return signed_contract


class TestManagerNotify(object):
    params_name = 'is_signed, is_faxed, is_booked, end_dt, change_params, title'
    params = [
        pytest.param(True, True, False, TODAY, None, original_and_faxed_title,
                     id='is_signed + is_faxed'),
        pytest.param(True, True, True, TODAY, None, original_and_faxed_title,
                     id='is_signed + is_booked'),
        pytest.param(True, False, False, TODAY, None, original_title,
                     id='is_signed'),
        pytest.param(False, True, True, TODAY, None, booked_title,
                     id='is_booked'),
        pytest.param(False, True, False, TODAY, None, faxed_title,
                     id='is_faxed'),
        pytest.param(False, False, False, TODAY, None, None,
                     id='not is_signed + not is_faxed + not is_booked'),
        pytest.param(False, False, False, TODAY, {'is_signed': TODAY}, original_title,
                     id='signed later'),
        pytest.param(False, False, False, TODAY, {'is_faxed': TODAY}, faxed_title,
                     id='faxed later'),
        pytest.param(False, False, False, TODAY, {'is_faxed': TODAY, 'is_booked': True}, booked_title,
                     id='booked later'),
        pytest.param(True, False, False, TODAY, {'service_id': 1}, original_title,
                     id='signed earlier'),
        pytest.param(False, True, False, TODAY, {'service_id': 1}, faxed_title,
                     id='faxed earlier'),
        pytest.param(False, True, True, TODAY, {'is_booked': False, 'is_faxed': TODAY}, faxed_title,
                     id='booked, then faxed'),
    ]

    @staticmethod
    def _get_contracts_match(contracts):
        return hm.contains_inanyorder(*[
            hm.has_entries({
                'contract_id': c.contract_id,
                'contract_eid': c.contract_eid,
                'person_name': c.person_name,
            })
            for c in contracts
        ])

    def _change_contract(self, session, signed_contract, changes):
        real_contract = session.query(mapper.Contract).getone(signed_contract.contract_id)
        real_contract.col0.is_signed = signed_contract.is_signed
        real_contract.col0.is_faxed = signed_contract.is_faxed
        for attr_name, val in changes.items():
            setattr(signed_contract, attr_name, val)
        session.flush()

    @pytest.mark.parametrize(
        params_name,
        params + [
            pytest.param(True, False, False, TODAY - datetime.timedelta(days=3), None, None,
                         id='old contract'),
            pytest.param(True, True, False, TODAY - datetime.timedelta(days=3), {'service_id': 1}, None,
                         id='nothing has been changed'),
        ],
    )
    def test_get_contracts(self, session, manager,
                           is_signed, is_faxed, is_booked, end_dt, change_params, title):
        signed_contract = create_signed_contract(session, manager, end_dt, is_signed, is_booked, is_faxed)
        if change_params:
            self._change_contract(session, signed_contract, change_params)

        res = manager_contract_notify.get_contracts(session)
        if title:
            hm.assert_that(
                res,
                hm.contains(
                    hm.contains(signed_contract, hm.contains_inanyorder(*signed_contract.historical_collaterals)),
                ),
            )

        else:
            assert res == []

    @pytest.mark.parametrize(params_name, params)
    def test_prepare_single_data(self, session, manager,
                                 is_signed, is_faxed, is_booked, end_dt, change_params, title):
        signed_contract = create_signed_contract(session, manager, end_dt, is_signed, is_booked, is_faxed)
        if change_params:
            self._change_contract(session, signed_contract, change_params)

        try:
            res = manager_contract_notify.prepare_data([(signed_contract, signed_contract.historical_collaterals)])
        except manager_contract_notify.InvalidContract:
            res = []

        if title:
            hm.assert_that(
                res,
                hm.contains(
                    hm.has_entries({
                        'manager_code': manager.manager_code,
                        'manager_name': manager.name,
                        'manager_email': manager.email,
                        'contract_groups': hm.contains(
                            hm.contains(title, self._get_contracts_match([signed_contract])),
                        ),
                    }),
                ),
            )

        else:
            assert res == []

    def test_prepare_data(self, session):
        manager1 = create_manager(session)
        manager2 = create_manager(session)

        contracts = {
            11: create_signed_contract(session, manager1, is_signed=True, is_faxed=True, is_booked=False),
            12: create_signed_contract(session, manager1, is_signed=True, is_faxed=False, is_booked=False),
            13: create_signed_contract(session, manager1, is_signed=False, is_faxed=True, is_booked=False),
            14: create_signed_contract(session, manager1, is_signed=False, is_faxed=True, is_booked=True),

            21: create_signed_contract(session, manager2, is_signed=False, is_faxed=True, is_booked=True),
            22: create_signed_contract(session, manager2, is_signed=True, is_faxed=True, is_booked=False),
            23: create_signed_contract(session, manager2, is_signed=True, is_faxed=True, is_booked=False),
        }

        prepared_data = manager_contract_notify.prepare_data(
            [(c, c.historical_collaterals) for c in contracts.itervalues()]
        )

        hm.assert_that(
            prepared_data,
            hm.contains_inanyorder(
                hm.has_entries({
                    'manager_code': manager1.manager_code,
                    'manager_name': manager1.name,
                    'manager_email': manager1.email,
                    'contract_groups': hm.contains(
                        hm.contains(u'Проставлена отметка БРОНЬ о подписании договора по факсу:', self._get_contracts_match([contracts[14]])),
                        hm.contains(u'Получены факсовые копии договоров:', self._get_contracts_match([contracts[13]])),
                        hm.contains(u'Получены оригиналы договоров:', self._get_contracts_match([contracts[12]])),
                        hm.contains(u'Получены оригиналы и факсовые копии договоров:', self._get_contracts_match([contracts[11]])),
                    ),
                }),
                hm.has_entries({
                    'manager_code': manager2.manager_code,
                    'manager_name': manager2.name,
                    'manager_email': manager2.email,
                    'contract_groups': hm.contains(
                        hm.contains(u'Проставлена отметка БРОНЬ о подписании договора по факсу:', self._get_contracts_match([contracts[21]])),
                        hm.contains(u'Получены оригиналы и факсовые копии договоров:', self._get_contracts_match([contracts[22], contracts[23]])),
                    ),
                }),
            )
        )

    def test_emails(self, app):
        manager_code_1 = ob.get_big_number()
        manager_code_2 = ob.get_big_number()
        prepared_data = [
            {
                'manager_code': manager_code_1,
                'manager_name': 'Manager name 1',
                'manager_email': 'manager_1@mail.com',
                'contract_groups': [
                    (
                        u'Заголовок 1',
                        [
                            {'contract_id': 11, 'contract_eid': 'eid 11', 'person_name': 'Name 11'},
                            {'contract_id': 12, 'contract_eid': 'eid 12', 'person_name': 'Name 12'},
                            {'contract_id': 13, 'contract_eid': 'eid 13', 'person_name': 'Name 13'},
                        ],
                    ),
                    (u'Заголовок 2', [{'contract_id': 2, 'contract_eid': 'eid 2', 'person_name': 'Name 2'}]),
                ],
            },
            {
                'manager_code': manager_code_2,
                'manager_name': 'Manager name 2',
                'manager_email': 'manager_2@mail.com',
                'contract_groups': [
                    (u'Заголовок 1', [{'contract_id': 3, 'contract_eid': 'eid 3', 'person_name': 'Name 3'}]),
                ],
            },
        ]

        emails = manager_contract_notify.create_emails(prepared_data)

        body1 = u"""
Добрый день, Manager name 1!

Заголовок 1
    1. <a href="https://admin.balance.yandex.ru/contract.xml?contract_id=11">eid 11</a> Name 11
    2. <a href="https://admin.balance.yandex.ru/contract.xml?contract_id=12">eid 12</a> Name 12
    3. <a href="https://admin.balance.yandex.ru/contract.xml?contract_id=13">eid 13</a> Name 13

Заголовок 2
    1. <a href="https://admin.balance.yandex.ru/contract.xml?contract_id=2">eid 2</a> Name 2

С уважением,
Администрация Баланса.
"""
        body2 = u"""
Добрый день, Manager name 2!

Заголовок 1
    1. <a href="https://admin.balance.yandex.ru/contract.xml?contract_id=3">eid 3</a> Name 3

С уважением,
Администрация Баланса.
"""

        hm.assert_that(
            emails,
            hm.contains_inanyorder(
                hm.has_properties({'recepient_name': 'Manager name 1', 'recepient_address': 'manager_1@mail.com'}),
                hm.has_properties({'recepient_name': 'Manager name 2', 'recepient_address': 'manager_2@mail.com'}),
            ),
        )

        msgs = [MessageData.from_message_mapper(m, app.cfg) for m in emails]
        hm.assert_that(
            msgs,
            hm.contains_inanyorder(
                hm.has_properties({
                    'subject': u'Подписаны договоры',
                    'sender': 'info-noreply@support.yandex.com',
                    'sender_name': u'Яндекс.Баланс',
                    'body': u'',
                    'attach_list': hm.contains(
                        hm.has_property('data', body1),
                    ),
                }),
                hm.has_properties({
                    'subject': u'Подписаны договоры',
                    'sender': 'info-noreply@support.yandex.com',
                    'sender_name': u'Яндекс.Баланс',
                    'body': u'',
                    'attach_list': hm.contains(
                        hm.has_property('data', body2),
                    ),
                }),
            ),
        )

    def test_invalid_contract(self, session, manager):
        contract = create_signed_contract(session, manager, end_dt=TODAY - datetime.timedelta(days=2))
        manager_data = manager_contract_notify.ManagerContractData(contract)

        with pytest.raises(manager_contract_notify.InvalidContract) as exc_info:
            manager_data.add_contract(contract, contract.historical_collaterals)
        assert exc_info.value.message == u'Invalid contract %s for sending. Check requests!' % contract.contract_id
