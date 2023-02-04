# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt
from balance.actions.invoice_turnon import InvoiceTurnOn

from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests.solvers.change_contract.common import (
    AbstractBaseDBTestCase, RequiredResult
)

__all__ = ['AbstractPADBTestCase']


class AbstractPADBTestCase(AbstractBaseDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    author = 'noob'

    def get_comments(self):
        return [
            ('boss', 'Подтверждено'),
            ('mscnad7', 'Подтверждено')
        ]

    def __init__(self, *args, **kwargs):
        super(AbstractPADBTestCase, self).__init__(*args, **kwargs)
        self.personal_account = None
        self.charge_note = None


class CancelContractPAEmptyDBTestCase(AbstractPADBTestCase):
    _representation = 'c_cancel_pa_empty'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_,
                                                         firm_id=13,
                                                         payment_type=3,
                                                         personal_account=1)

        self.personal_account = db_utils.create_personal_account(session, contract=self.contract)

        return self.contract, [
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True, delay=False,
                             transition=IssueTransitions.none,
                             state=[
                                 ('dt', dt.datetime(2016, 1, 1)),
                                 ('is_signed', None),
                                 ('is_cancelled', True),
                             ],
                             pa_state=[(self.personal_account, 2)])
        return res


class CancelContractMultiplePAEmptyDBTestCase(AbstractPADBTestCase):
    _representation = 'c_cancel_multiple_pa_empty'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_,
                                                         firm_id=1,
                                                         payment_type=3,
                                                         personal_account=1)

        self.personal_account = db_utils.create_personal_account(session, contract=self.contract)
        self.personal_account2 = db_utils.create_personal_account(session, contract=self.contract)

        return self.contract, [
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True, delay=False,
                             transition=IssueTransitions.none,
                             state=[
                                 ('dt', dt.datetime(2016, 1, 1)),
                                 ('is_signed', None),
                                 ('is_cancelled', True),
                             ],
                             pa_state=[
                                 (self.personal_account, 2),
                                 (self.personal_account2, 2)
                             ])
        return res


class CancelContractMultiplePAConsumedDBTestCase(AbstractPADBTestCase):
    _representation = 'c_cancel_multiple_pa_consumed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_,
                                                         firm_id=10,
                                                         payment_type=3,
                                                         personal_account=1)

        self.personal_account = db_utils.create_personal_account(session, contract=self.contract)
        self.personal_account2 = db_utils.create_personal_account(session, contract=self.contract)
        order = db_utils.create_order(session, client)
        self.personal_account2.transfer(order, 2, 1, skip_check=1)
        return self.contract, [
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix,
                             state=[
                                 ('dt', dt.datetime(2016, 1, 1)),
                                 ('is_signed', dt.datetime(2016, 1, 1)),
                                 ('is_cancelled', None),
                             ],
                             pa_state=[
                                 (self.personal_account, 0),
                                 (self.personal_account2, 0)
                             ])
        res.add_message('Выполнить нельзя, по договору есть выставленные счета.')
        return res


class CancelContractPAConsumedDBTestCase(AbstractPADBTestCase):
    _representation = 'c_cancel_pa_consumed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_,
                                                         firm_id=13,
                                                         payment_type=3,
                                                         personal_account=1)

        order = db_utils.create_order(session, client)
        self.personal_account = db_utils.create_personal_account(session, contract=self.contract)
        self.personal_account.transfer(order, 2, 1, skip_check=1)

        return self.contract, [
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix,
                             state=[
                                ('dt', dt.datetime(2016, 1, 1)),
                                ('is_signed', dt.datetime(2016, 1, 1)),
                                ('is_cancelled', None),
                             ],
                             pa_state=[
                                 (self.personal_account, 0)
                             ])
        res.add_message('Выполнить нельзя, по договору есть выставленные счета.')
        return res


class ChangeContractPAEmptyDBTestCase(AbstractPADBTestCase):
    _representation = 'change_pa_empty'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_,
                                                         finish_dt=dt.datetime(2016, 2, 1),
                                                         firm_id=121,
                                                         payment_type=3,
                                                         personal_account=1)

        self.personal_account = db_utils.create_personal_account(session,
                                                                 contract=self.contract, dt_=dt.datetime(2016, 1, 1))

        return self.contract, [
            ('finish_dt', dt.datetime(2016, 3, 1)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True, delay=False,
                             transition=IssueTransitions.none,
                             state=[
                                 ('dt', dt.datetime(2016, 1, 1)),
                                 ('finish_dt', dt.datetime(2016, 3, 1)),
                             ],
                             pa_state=[(self.personal_account, 0)])
        return res


class UnsignContractPAEmptyDBTestCase(AbstractPADBTestCase):
    _representation = 'unsign_pa_empty'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_,
                                                         firm_id=1,
                                                         payment_type=3,
                                                         personal_account=1)

        self.personal_account = db_utils.create_personal_account(session,
                                                                 contract=self.contract, dt_=dt.datetime(2016, 1, 1))

        return self.contract, [
            ('remove_is_signed', True),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True, delay=False,
                             transition=IssueTransitions.none,
                             state=[
                                 ('is_signed', None),
                             ],
                             pa_state=[(self.personal_account, 2)])
        return res


class CancelContractPAEmptyCNEmptyDBTestCase(AbstractPADBTestCase):
    _representation = 'c_cancel_pa_empty_cn_empty'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_,
                                                         firm_id=13,
                                                         payment_type=3,
                                                         personal_account=1)

        self.personal_account = db_utils.create_personal_account(session, contract=self.contract)
        self.charge_note = db_utils.create_charge_note(session, self.personal_account)
        self.charge_note.contract = self.contract

        return self.contract, [
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True, delay=False,
                             transition=IssueTransitions.none,
                             state=[
                                 ('dt', dt.datetime(2016, 1, 1)),
                                 ('is_signed', None),
                                 ('is_cancelled', True),
                             ],
                             pa_state=[
                                 (self.personal_account, 2)
                             ],
                             cn_state=[
                                 (self.charge_note, 2)
                             ]
                             )
        return res


class CancelContractPAEmptyCNNotEmptyDBTestCase(AbstractPADBTestCase):
    _representation = 'c_cancel_pa_not_empty'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_,
                                                         firm_id=13,
                                                         payment_type=3,
                                                         personal_account=1)

        self.personal_account = db_utils.create_personal_account(session, contract=self.contract)
        self.charge_note = db_utils.create_charge_note(session, self.personal_account)
        InvoiceTurnOn(self.charge_note, sum=100).do()
        session.flush()

        return self.contract, [
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix,
                             state=[
                                ('dt', dt.datetime(2016, 1, 1)),
                                ('is_signed', dt.datetime(2016, 1, 1)),
                                ('is_cancelled', None),
                             ],
                             pa_state=[
                                 (self.personal_account, 0)
                             ],
                             cn_state=[
                                 (self.charge_note, 0)
                             ]
                             )
        res.add_message('Выполнить нельзя, по договору есть выставленные счета.')
        return res

