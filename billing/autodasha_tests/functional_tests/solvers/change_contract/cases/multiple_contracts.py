# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests.solvers.change_contract.common import (
    AbstractBaseDBTestCase, RequiredResult
)

__all__ = ['AbstractMultipleContractsDBTestCase']


class AbstractMultipleContractsDBTestCase(AbstractBaseDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    author = 'noob'

    def get_comments(self):
        return [
            ('boss', 'Подтверждено'),
            ('mscnad7', 'Подтверждено')
        ]


class MultipleContractsRequestDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'c_request'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        other_c = db_utils.create_general_contract(session, client, person, dt_)
        self.contract = db_utils.create_general_contract(session, client, person, dt_ + dt.timedelta(1), finish_dt=dt_)
        other_c.external_id = self.contract.external_id
        return self.contract, [
            ('finish_dt', '2016-03-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, transition=IssueTransitions.none, summonees=['noob'],
                             delay=1, done=0, enqueued=0, state=[
            ('dt', dt.datetime(2016, 1, 2)),
            ('finish_dt', dt.datetime(2016, 1, 1)),
        ])
        res.add_message('Договоров с таким номером больше одного, для идентификации скопируй, '
                        'пожалуйста, и приложи адресную строку из нужного.')
        res.add_message('https://yandex.ru/images/')
        return res


class MultipleContractsCancelledRequestDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'c_cancelled_request'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, finish_dt=dt_)
        other_c = db_utils.create_general_contract(session, client, person,
                                                   dt_ + dt.timedelta(1), is_cancelled=dt.datetime.now())
        other_c.external_id = self.contract.external_id
        return self.contract, [
            ('finish_dt', '2016-03-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, transition=IssueTransitions.none, summonees=['noob'],
                             delay=1, done=0, enqueued=0, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('finish_dt', dt.datetime(2016, 1, 1)),
        ])
        res.add_message('Договоров с таким номером больше одного, для идентификации скопируй, '
                        'пожалуйста, и приложи адресную строку из нужного.')
        res.add_message('https://yandex.ru/images/')
        return res


class MultipleContractsRequestedDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'c_requested'

    def get_comments(self):
        dasha_cmt = 'Договоров с таким номером больше одного, дятел. Гони ссылку. https://yandex.ru/images/'
        return [
            ('autodasha', dasha_cmt, dt.datetime(2016, 1, 15)),
            ('boss', 'Подтверждено', dt.datetime(2016, 2, 1)),
            ('mscnad7', 'Подтверждено', dt.datetime(2016, 3, 1))
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        other_c = db_utils.create_general_contract(session, client, person, dt_)
        self.contract = db_utils.create_general_contract(session, client, person, dt_ + dt.timedelta(1), finish_dt=dt_)
        other_c.external_id = self.contract.external_id
        return self.contract, [
            ('finish_dt', '2016-03-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, transition=IssueTransitions.none, delay=1, done=0, enqueued=0, state=[
            ('dt', dt.datetime(2016, 1, 2)),
            ('finish_dt', dt.datetime(2016, 1, 1)),
        ])
        return res


class MultipleContractsRequestedAfterLinkDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'c_requested_after_link'

    def get_comments(self):
        dasha_cmt = 'Договоров с таким номером больше одного, дятел. Гони ссылку. https://yandex.ru/images/'
        lnk = 'https://admin.balance.yandex-team.ru/contract-edit.xml?contract_id=666'
        return [
            ('loh_ushastii', lnk, dt.datetime(2016, 1, 14)),
            ('autodasha', dasha_cmt, dt.datetime(2016, 1, 15)),
            ('boss', 'Подтверждено', dt.datetime(2016, 2, 1)),
            ('mscnad7', 'Подтверждено', dt.datetime(2016, 3, 1))
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        other_c = db_utils.create_general_contract(session, client, person, dt_)
        self.contract = db_utils.create_general_contract(session, client, person, dt_ + dt.timedelta(1), finish_dt=dt_)
        other_c.external_id = self.contract.external_id
        return self.contract, [
            ('finish_dt', '2016-03-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, transition=IssueTransitions.none, delay=1, done=0, enqueued=0, state=[
            ('dt', dt.datetime(2016, 1, 2)),
            ('finish_dt', dt.datetime(2016, 1, 1)),
        ])
        return res


class MultipleContractsLinkDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'c_link'

    def get_comments(self):
        dasha_cmt = 'Договоров с таким номером больше одного, дятел. Гони ссылку. https://yandex.ru/images/'
        lnk = 'https://admin.balance.yandex-team.ru/contract-edit.xml?contract_id=%s' % self.contract.id
        return [
            ('autodasha', dasha_cmt, dt.datetime(2016, 1, 15)),
            ('loh_ushastii', lnk, dt.datetime(2016, 1, 16)),
            ('boss', 'Подтверждено', dt.datetime(2016, 2, 1)),
            ('mscnad7', 'Подтверждено', dt.datetime(2016, 3, 1))
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        other_c = db_utils.create_general_contract(session, client, person, dt_)
        self.contract = db_utils.create_general_contract(session, client, person, dt_ + dt.timedelta(1), finish_dt=dt_)
        other_c.external_id = self.contract.external_id
        return self.contract, [
            ('finish_dt', '2016-03-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, transition=IssueTransitions.none, delay=0, done=1, enqueued=1, state=[
            ('dt', dt.datetime(2016, 1, 2)),
            ('finish_dt', dt.datetime(2016, 3, 1)),
        ])
        return res


class MultipleContractsWrongLinkDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'c_wrong_link'

    def get_comments(self):
        dasha_cmt = 'Договоров с таким номером больше одного, дятел. Гони ссылку. https://yandex.ru/images/'
        lnk = 'https://admin.balance.yandex-team.ru/contract-edit.xml?contract_id=%s' % self.other_c.id
        return [
            ('autodasha', dasha_cmt, dt.datetime(2016, 1, 15)),
            ('loh_ushastii', lnk, dt.datetime(2016, 1, 16)),
            ('boss', 'Подтверждено', dt.datetime(2016, 2, 1)),
            ('mscnad7', 'Подтверждено', dt.datetime(2016, 3, 1))
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.other_c = db_utils.create_general_contract(session, client, person, dt_)
        self.contract = db_utils.create_general_contract(session, client, person, dt_ + dt.timedelta(1), finish_dt=dt_)
        return self.contract, [
            ('finish_dt', '2016-03-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, transition=IssueTransitions.wont_fix, delay=0, done=0, enqueued=0, state=[
            ('dt', dt.datetime(2016, 1, 2)),
            ('finish_dt', dt.datetime(2016, 1, 1)),
        ])
        res.add_message('Указанный ID договора не соответствует номеру договора из текста задачи.')
        return res


class MultipleContractsNotFoundLinkDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'c_not_found_link'

    def get_comments(self):
        dasha_cmt = 'Договоров с таким номером больше одного, дятел. Гони ссылку. https://yandex.ru/images/'
        lnk = 'https://admin.balance.yandex-team.ru/contract-edit.xml?contract_id=1'
        return [
            ('autodasha', dasha_cmt, dt.datetime(2016, 1, 15)),
            ('loh_ushastii', lnk, dt.datetime(2016, 1, 16)),
            ('boss', 'Подтверждено', dt.datetime(2016, 2, 1)),
            ('mscnad7', 'Подтверждено', dt.datetime(2016, 3, 1))
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        other_c = db_utils.create_general_contract(session, client, person, dt_)
        self.contract = db_utils.create_general_contract(session, client, person, dt_ + dt.timedelta(1), finish_dt=dt_)
        other_c.external_id = self.contract.external_id
        return self.contract, [
            ('finish_dt', '2016-03-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, transition=IssueTransitions.wont_fix, delay=0, done=0, enqueued=0, state=[
            ('dt', dt.datetime(2016, 1, 2)),
            ('finish_dt', dt.datetime(2016, 1, 1)),
        ])
        res.add_message('Договор не найден. Уточни, пожалуйста, номер договора и заполни форму еще раз.')
        return res


class MultipleContractsOnlyCancelledDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'c_only_cancelled'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, is_cancelled=dt_)
        return self.contract, [
            ('finish_dt', '2016-03-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, transition=IssueTransitions.none, delay=False, done=1, enqueued=1, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_cancelled', dt.datetime(2016, 1, 1)),
            ('finish_dt', dt.datetime(2016, 3, 1)),
        ])
        return res


class MultipleColDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'col'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        db_utils.add_collateral(self.contract, dt_=dt_, num='666', is_signed=dt_)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt_ + dt.timedelta(1), num='666', is_signed=dt_)
        return self.contract, [
            ('col', self.collateral),
            ('is_faxed', '2016-06-06'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral,
                             transition=IssueTransitions.none, delay=False, done=1, enqueued=1,
                             state=[
                                 ('dt', dt.datetime(2016, 1, 2)),
                                 ('is_signed', dt.datetime(2016, 1, 1)),
                                 ('is_faxed', dt.datetime(2016, 6, 6)),
                             ])
        return res


class MultipleColDTDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'col_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt_, num='666', is_signed=dt_)
        db_utils.add_collateral(self.contract, dt_=dt_ + dt.timedelta(1), num='666', is_signed=dt_)
        return self.contract, [
            ('col', self.collateral),
            ('col_dt', '2016-01-01'),
            ('is_faxed', '2016-06-06'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral,
                             transition=IssueTransitions.none, delay=False, done=1, enqueued=1,
                             state=[
                                 ('dt', dt.datetime(2016, 1, 1)),
                                 ('is_signed', dt.datetime(2016, 1, 1)),
                                 ('is_faxed', dt.datetime(2016, 6, 6)),
                             ])
        return res


class MultipleColCancelledDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'col_cancelled'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt_, num='666', is_signed=dt_)
        db_utils.add_collateral(self.contract, dt_=dt_ + dt.timedelta(1), num='666', is_signed=dt_,
                                is_cancelled=dt.datetime.now())
        return self.contract, [
            ('col', self.collateral),
            ('is_faxed', '2016-06-06'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral,
                             transition=IssueTransitions.none, delay=False, done=1, enqueued=1,
                             state=[
                                 ('dt', dt.datetime(2016, 1, 1)),
                                 ('is_signed', dt.datetime(2016, 1, 1)),
                                 ('is_faxed', dt.datetime(2016, 6, 6)),
                             ])
        return res


class MultipleColCancelledDTDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'col_cancelled_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        db_utils.add_collateral(self.contract, dt_=dt_, num='666', is_signed=dt_)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt_ + dt.timedelta(1),
                                                  is_signed=dt_,
                                                  num='666', is_cancelled=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('col_dt', '2016-01-02'),
            ('is_faxed', '2016-06-06'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral,
                             transition=IssueTransitions.none, delay=False, done=1, enqueued=1,
                             state=[
                                 ('dt', dt.datetime(2016, 1, 2)),
                                 ('is_signed', dt.datetime(2016, 1, 1)),
                                 ('is_faxed', dt.datetime(2016, 6, 6)),
                                 ('is_cancelled', dt.datetime(2016, 1, 1)),
                             ])
        return res


class MultipleColUnsignedDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'col_unsigned'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt_, num='666', is_signed=dt_)
        db_utils.add_collateral(self.contract, dt_=dt_ + dt.timedelta(1),
                                num='666', is_cancelled=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('is_faxed', '2016-06-06'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral,
                             transition=IssueTransitions.none, delay=False, done=1, enqueued=1,
                             state=[
                                 ('dt', dt.datetime(2016, 1, 1)),
                                 ('is_signed', dt.datetime(2016, 1, 1)),
                                 ('is_faxed', dt.datetime(2016, 6, 6)),
                             ])
        return res


class MultipleColUnsignedDTDBTestCase(AbstractMultipleContractsDBTestCase):
    _representation = 'col_unsigned_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        db_utils.add_collateral(self.contract, dt_=dt_, num='666', is_signed=dt_)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt_ + dt.timedelta(1), num='666')
        return self.contract, [
            ('col', self.collateral),
            ('col_dt', '2016-01-02'),
            ('is_faxed', '2016-06-06'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True, delay=False,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 2)),
                                                                      ('is_faxed', dt.datetime(2016, 6, 6))], )
        return res
