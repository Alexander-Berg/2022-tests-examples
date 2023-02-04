# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

import pytest

from autodasha.solver_cl.change_contract.state import State
from autodasha.solver_cl.change_contract.changed_state import ChangedContractState

from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests.solvers.change_contract.common import AbstractBaseDBTestCase


class AbstractChangedStateCase(AbstractBaseDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    author = 'noob'


class Col0Case(AbstractChangedStateCase):
    _representation = 'col0'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         finish_dt=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
        ]

    def get_result(self):
        return {
            'dt': dt.datetime(2016, 2, 1),
            'is_signed': dt.datetime(2016, 1, 1),
            'finish_dt': dt.datetime(2016, 8, 1)
        }


class LastCollateralCase(AbstractChangedStateCase):
    _representation = 'last'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        dt_ = dt.datetime(2016, 2, 1)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt_, is_signed=dt_, finish_dt=dt_)
        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 3, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
        ]

    def get_result(self):
        return {
            'dt': dt.datetime(2016, 3, 1),
            'is_signed': dt.datetime(2016, 2, 1),
            'finish_dt': dt.datetime(2016, 8, 1)
        }


class MidCollateralCase(AbstractChangedStateCase):
    _representation = 'mid'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         is_booked_dt=dt.datetime(2016, 1, 1),
                                                         is_booked=1)
        self.collateral = db_utils.add_collateral(self.contract,
                                                  dt_=dt.datetime(2016, 2, 1),
                                                  is_signed=dt.datetime(2016, 2, 1),
                                                  is_booked=1)
        db_utils.add_collateral(self.contract,
                                dt_=dt.datetime(2016, 3, 1),
                                is_signed=dt.datetime(2016, 3, 1),
                                finish_dt=dt.datetime(2016, 3, 1))
        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 2, 15)),
            ('sent_dt', dt.datetime(2016, 2, 15)),
            ('remove_is_booked', True)
        ]

    def get_result(self):
        return {
            'dt': dt.datetime(2016, 3, 1),
            'is_booked_dt': dt.datetime(2016, 1, 1),
            'is_booked': None,
            'sent_dt': dt.datetime(2016, 2, 15),
            'finish_dt': dt.datetime(2016, 3, 1)
        }


class MovedCollateralCase(AbstractChangedStateCase):
    _representation = 'moved'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.collateral = db_utils.add_collateral(self.contract,
                                                  dt_=dt.datetime(2016, 2, 1),
                                                  is_signed=dt.datetime(2016, 2, 1),
                                                  sent_dt=dt.datetime(2016, 2, 1))
        db_utils.add_collateral(self.contract,
                                dt_=dt.datetime(2016, 3, 1),
                                is_signed=dt.datetime(2016, 3, 1),
                                sent_dt=dt.datetime(2016, 3, 1),
                                is_booked_dt=dt.datetime(2016, 3, 1))
        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 3, 15)),
        ]

    def get_result(self):
        return {
            'dt': dt.datetime(2016, 3, 15),
            'sent_dt': dt.datetime(2016, 2, 1),
            'is_signed': dt.datetime(2016, 2, 1),
            'is_booked_dt': dt.datetime(2016, 3, 1)
        }


class UnsignedCollateralCase(AbstractChangedStateCase):
    _representation = 'unsigned'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         is_booked=0)
        self.collateral = db_utils.add_collateral(self.contract,
                                                  dt_=dt.datetime(2016, 2, 1),
                                                  is_signed=dt.datetime(2016, 2, 1),
                                                  sent_dt=dt.datetime(2016, 2, 1),
                                                  is_booked_dt=dt.datetime(2016, 2, 1),
                                                  is_booked=1)
        db_utils.add_collateral(self.contract,
                                dt_=dt.datetime(2016, 3, 1),
                                is_signed=dt.datetime(2016, 3, 1),
                                is_booked_dt=dt.datetime(2016, 3, 1))
        return self.contract, [
            ('col', self.collateral),
            ('remove_is_signed', True),
        ]

    def get_result(self):
        return {
            'dt': dt.datetime(2016, 3, 1),
            'sent_dt': None,
            'is_signed': dt.datetime(2016, 3, 1),
            'is_booked_dt': dt.datetime(2016, 3, 1),
            'is_booked': 0
        }


@pytest.mark.parametrize('issue_data',
                         [case_() for case_ in AbstractChangedStateCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data'])
def test(session, comments_manager, issue_data):
    queue_object, st_issue, case = issue_data

    required_attributes = case.get_result()

    state = State(
        {'c': case.contract, 'col': case.collateral},
        issue=st_issue,
        session=session,
        comments_manager=comments_manager
    )
    changed_state = ChangedContractState(state, *required_attributes)

    attributes = {attr: changed_state.get(attr) for attr in required_attributes}
    assert attributes == required_attributes
