# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests.solvers.change_contract.common import (
    AbstractBaseDBTestCase, RequiredResult, get_approve_message
)

__all__ = ['AbstractApproveDBTestCase']


class AbstractApproveDBTestCase(AbstractBaseDBTestCase):
    _cases = []
    issue_dt = dt.datetime.today()

    __metaclass__ = case_utils.TestCaseMeta

    def setup_config(self, session, config):
        config['summoned_treasurers'] = {
                                            "RUSSIA":
                                            {
                                                "398": ["treasurer_kzt"],
                                                "810": ["treasurer_rur"],
                                                "933": ["treasurer_byn"],
                                                "840": ["treasurer_usd"],
                                                "978": ["treasurer_eur"],
                                                "OTHER": ["treasurer_other"]
                                            },
                                            "FOREIGNER":
                                            {
                                                "ALL": ["treasurer_1", "treasurer_2"]
                                            }
                                        }



class AbstractGeneralApproveDBTestCase(AbstractApproveDBTestCase):
    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]


class AbstractNonresApproveDBTestCase(AbstractApproveDBTestCase):
    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]


class AbstractPartnersApproveDBTestCase(AbstractApproveDBTestCase):
    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt_})
        return self.contract, [
            ('dt', '2016-02-01'),
        ]


class AbstractNonresPartnersApproveDBTestCase(AbstractApproveDBTestCase):
    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt_})
        return self.contract, [
            ('dt', '2016-02-01'),
        ]


class AbstractDistrApproveDBTestCase(AbstractApproveDBTestCase):
    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_distr_contract(session, client, person, dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt_})
        return self.contract, [
            ('dt', '2016-02-01'),
        ]


class AbstractGeneralUAApproveDBTestCase(AbstractApproveDBTestCase):
    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='ua')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, firm_id=2)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]


class AbstractGeneralUATApproveDBTestCase(AbstractApproveDBTestCase):
    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='ua')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, firm_id=23)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]


class BONotApprovedInTimeTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_not_approved_in_time'

    author = 'noob'

    issue_dt = dt.datetime(2019, 9, 2)
    current_date = dt.datetime(2019, 9, 6)

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']}
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=False,
            transition=IssueTransitions.wont_fix,
        )
        res.set_messages(outdated=True)

        return res


class BONotApprovedOverWeekendTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_not_approved_over_weekend'

    author = 'noob'

    issue_dt = dt.datetime(2019, 9, 6)
    current_date = dt.datetime(2019, 9, 9)

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']}
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=True,
            transition=IssueTransitions.none,
        )

        return res


class BONotRequestedDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_noob_not_requested'

    author = 'noob'

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none, summonees=['alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class BOChiefRequestedDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_chief_requested'

    author = 'alt_boss'

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none, summonees=['boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class BONotRequestedForcedDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_noob_not_requested_forced'

    author = 'forced_noob'

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none,
                             summonees=['forced_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class BORequestedDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_noob_requested'

    issue_dt = dt.datetime(2019, 9, 2)
    current_date = dt.datetime(2019, 9, 4)

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']}
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class BORequestedForcedDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_noob_requested_forced'

    author = 'forced_noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['forced_boss']}
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class BORequestedAltDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_noob_requested_alt'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['alt_boss']}
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class BOBossNotRequestedDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_boss_not_requested'

    author = 'boss'

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['bigboss2'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class BOBigBossNotRequestedDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_big_boss_not_requested'

    author = 'bigboss1'

    def get_comments(self):
        return []

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['bigboss2'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class BOMeganoobNotRequestedDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_meganoob_not_requested'

    author = 'meganoob'

    def get_comments(self):
        return []

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['bigboss1', 'bigboss2'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class AbstractBOApprovedDBTestCase(AbstractGeneralApproveDBTestCase):
    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, assignee='mscnad7',
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата начала" с 01.01.2016 на 01.02.2016;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class SupportNotApprovedInTimeTestCase(AbstractBOApprovedDBTestCase):
    _representation = 'support_not_approved_in_time'

    author = 'noob'

    issue_dt = dt.datetime(2019, 9, 2)
    current_date = dt.datetime(2019, 9, 6)

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'}
        ]


class BOMegaBossNotRequestedDBTestCase(AbstractBOApprovedDBTestCase):
    _representation = 'bo_megaboss_not_requested'

    author = 'megaboss'

    def get_comments(self):
        return []


class BONoobAltApprovedDBTestCase(AbstractBOApprovedDBTestCase):
    _representation = 'bo_noob_alt_approved'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'alt_boss', 'text': 'Подтверждено'}
        ]


class BONoobForcedApprovedDBTestCase(AbstractBOApprovedDBTestCase):
    _representation = 'bo_noob_forced_approved'

    author = 'forced_noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['forced_boss']},
            {'author': 'forced_boss', 'text': 'Подтверждено'}
        ]


class BOBossAltApprovedDBTestCase(AbstractBOApprovedDBTestCase):
    _representation = 'bo_boss_alt_approved'

    author = 'boss'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['bigboss1']},
            {'author': 'bigboss2', 'text': 'Подтверждено'}
        ]


class BOBossAltWrongApprovedDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_boss_alt_approved_wrong'

    author = 'boss'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['bigboss1']},
            {'author': 'alt_boss', 'text': 'Подтверждено'}
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class BOTopDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_top'

    author = 'kommando'

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, assignee='mscnad7',
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('кто:mscnad7, не удалось найти подходящего подтверждающего. Посмотри, пожалуйста.')
        return res


class BOTopWaitingDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_top_waiting'

    author = 'kommando'

    def get_comments(self):
        return [
            ('autodasha', 'кто:mscnad7, не удалось найти подходящего подтверждающего. Посмотри, пожалуйста.')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, assignee=None,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class BOTopApprovedDBTestCase(AbstractBOApprovedDBTestCase):
    _representation = 'bo_top_approved'

    author = 'kommando'

    def get_comments(self):
        return [
            ('autodasha', 'кто:mscnad7, не удалось найти подходящего подтверждающего. Посмотри, пожалуйста.'),
            ('god', 'Подтверждено')
        ]


class BOAbsentDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_absent'

    author = 'absent_noob'

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, assignee='mscnad7',
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('кто:mscnad7, подтверждающий и его руководители отсутствуют. Уточни, пожалуйста, кого звать?')
        return res


class BOAbsentWaitingDBTestCase(AbstractGeneralApproveDBTestCase):
    _representation = 'bo_absent_waiting'

    author = 'absent_noob'

    def get_comments(self):
        return [
            ('autodasha', 'кто:mscnad7, подтверждающий и его руководители отсутствуют. Уточни, пожалуйста, кого звать?')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, assignee=None,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class BOAbsentApprovedDBTestCase(AbstractBOApprovedDBTestCase):
    _representation = 'bo_absent_approved'

    author = 'absent_noob'

    def get_comments(self):
        return [
            ('autodasha', 'кто:mscnad7, подтверждающий и его руководители отсутствуют. Уточни, пожалуйста, кого звать?'),
            ('absent_boss2', 'Подтверждено')
        ]


class UABONoobNotRequestedDBTestCase(AbstractGeneralUAApproveDBTestCase):
    _representation = 'ua_bo_noob_not_requested'

    author = 'noob'

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none,
                             summonees=['pchelovik', 'bigboss1'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class UABOBossNotRequestedDBTestCase(AbstractGeneralUAApproveDBTestCase):
    _representation = 'ua_bo_boss_not_requested'

    author = 'pchelovik'

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none,
                             summonees=['bigboss1'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class UABONoobRequestedDBTestCase(AbstractGeneralUAApproveDBTestCase):
    _representation = 'ua_bo_noob_requested'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['pchelovik', 'bigboss1']}
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class UABONoobPartialApproved1DBTestCase(AbstractGeneralUAApproveDBTestCase):
    _representation = 'ua_bo_noob_partial_approved_1'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['pchelovik', 'bigboss1']},
            {'author': 'pchelovik', 'text': 'Подтверждено'}
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class UABONoobPartialApproved2DBTestCase(AbstractGeneralUAApproveDBTestCase):
    _representation = 'ua_bo_noob_partial_approved_2'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['pchelovik', 'bigboss1']},
            {'author': 'bigboss1', 'text': 'Подтверждено'}
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class UABONoobApprovedDBTestCase(AbstractGeneralUAApproveDBTestCase):
    _representation = 'ua_bo_noob_approved'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['pchelovik', 'bigboss1']},
            {'author': 'pchelovik', 'text': 'Подтверждено'},
            {'author': 'bigboss1', 'text': 'Подтверждено'},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, assignee='mscnad7',
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата начала" с 01.01.2016 на 01.02.2016;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class UABONoobAltApprovedDBTestCase(AbstractGeneralUAApproveDBTestCase):
    _representation = 'ua_bo_noob_alt_approved'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['pchelovik', 'bigboss1']},
            {'author': 'pchelovik', 'text': 'Подтверждено'},
            {'author': 'bigboss2', 'text': 'Подтверждено'},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, assignee='mscnad7',
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата начала" с 01.01.2016 на 01.02.2016;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class UATaxiBONotRequestedDBTestCase(AbstractGeneralUATApproveDBTestCase):
    _representation = 'ua_taxi_bo_not_requested'

    author = 'noob'

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none,
                             summonees=['medovik'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class UATaxiBORequestedDBTestCase(AbstractGeneralUATApproveDBTestCase):
    _representation = 'ua_taxi_bo_requested'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['medovik']}
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class UATaxiBOApprovedDBTestCase(AbstractGeneralUATApproveDBTestCase):
    _representation = 'ua_taxi_bo_approved'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['medovik']},
            {'author': 'medovik', 'text': 'Подтверждено'},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, assignee='mscnad7',
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата начала" с 01.01.2016 на 01.02.2016;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class TreasuryNotRequestedDBTestCaseRUB(AbstractApproveDBTestCase):
    _representation = 'tr_not_requested_rub'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_rur'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryNotRequestedDBTestCaseRUR(AbstractApproveDBTestCase):
    _representation = 'tr_not_requested_rur'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=810)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_rur'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryNotRequestedDBTestCaseKZT(AbstractApproveDBTestCase):
    _representation = 'tr_not_requested_kzt'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=398)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_kzt'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryNotRequestedDBTestCaseBYR(AbstractApproveDBTestCase):
    _representation = 'tr_not_requested_byr'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=974)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_byn'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryNotRequestedDBTestCaseBYN(AbstractApproveDBTestCase):
    _representation = 'tr_not_requested_byn'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=933)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_byn'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryNotRequestedDBTestCaseUSD(AbstractApproveDBTestCase):
    _representation = 'tr_not_requested_usd'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=840)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_usd'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryNotRequestedDBTestCaseEUR(AbstractApproveDBTestCase):
    _representation = 'tr_not_requested_eur'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=978)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_eur'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryNotRequestedDBTestCaseOther(AbstractApproveDBTestCase):
    _representation = 'tr_not_requested_other'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=949)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_other'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryNotRequestedDBTestCaseForeigner(AbstractApproveDBTestCase):
    _representation = 'tr_not_requested_foreigner'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_,
                                                         currency=810, firm_id=7)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_1', 'treasurer_2'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryNotRequestedDBTestCaseNoPerson(AbstractApproveDBTestCase):
    _representation = 'tr_not_requested_no_person'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_distr_contract(session, client, person, dt_)
        self.contract.person_id = None
        session.flush()
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, assignee='mscnad7',
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата начала" с 01.01.2016 на 01.02.2016;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class TreasuryRequestedDBTestCase(AbstractNonresApproveDBTestCase):
    _representation = 'tr_requested'

    author = 'noob'

    def get_comments(self):
        tr_msg = 'Договор с нерезидентом, требуется подтверждение казначейства.\n' + get_approve_message()
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'alt_boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': tr_msg, 'summonees': ['treasurer']},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class TreasuryAltRequestedDBTestCase(AbstractNonresApproveDBTestCase):
    _representation = 'tr_alt_requested'

    author = 'noob'

    def get_comments(self):
        tr_msg = 'Договор с нерезидентом, требуется подтверждение казначейства.\n' + get_approve_message()
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': tr_msg, 'summonees': ['noob_treasurer']},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class TreasuryWrongRequestedDBTestCaseRUB(AbstractApproveDBTestCase):
    _representation = 'tr_wrong_requested_rub'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        tr_msg = 'Договор с нерезидентом, требуется подтверждение казначейства.\n' + get_approve_message()
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': tr_msg, 'summonees': ['not_treasurer']},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_rur'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryWrongRequestedDBTestCaseRUR(AbstractApproveDBTestCase):
    _representation = 'tr_wrong_requested_rur'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=810)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        tr_msg = 'Договор с нерезидентом, требуется подтверждение казначейства.\n' + get_approve_message()
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': tr_msg, 'summonees': ['not_treasurer']},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_rur'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryWrongRequestedDBTestCaseKZT(AbstractApproveDBTestCase):
    _representation = 'tr_wrong_requested_kzt'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=398)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        tr_msg = 'Договор с нерезидентом, требуется подтверждение казначейства.\n' + get_approve_message()
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': tr_msg, 'summonees': ['not_treasurer']},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_kzt'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryWrongRequestedDBTestCaseBYR(AbstractApproveDBTestCase):
    _representation = 'tr_wrong_requested_byr'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=974)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        tr_msg = 'Договор с нерезидентом, требуется подтверждение казначейства.\n' + get_approve_message()
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': tr_msg, 'summonees': ['not_treasurer']},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_byn'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryWrongRequestedDBTestCaseBYN(AbstractApproveDBTestCase):
    _representation = 'tr_wrong_requested_byn'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=933)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        tr_msg = 'Договор с нерезидентом, требуется подтверждение казначейства.\n' + get_approve_message()
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': tr_msg, 'summonees': ['not_treasurer']},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_byn'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryWrongRequestedDBTestCaseUSD(AbstractApproveDBTestCase):
    _representation = 'tr_wrong_requested_usd'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=840)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        tr_msg = 'Договор с нерезидентом, требуется подтверждение казначейства.\n' + get_approve_message()
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': tr_msg, 'summonees': ['not_treasurer']},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_usd'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryWrongRequestedDBTestCaseEUR(AbstractApproveDBTestCase):
    _representation = 'tr_wrong_requested_eur'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=978)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        tr_msg = 'Договор с нерезидентом, требуется подтверждение казначейства.\n' + get_approve_message()
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': tr_msg, 'summonees': ['not_treasurer']},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_eur'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryWrongRequestedDBTestCaseOther(AbstractApproveDBTestCase):
    _representation = 'tr_wrong_requested_other'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=949)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        tr_msg = 'Договор с нерезидентом, требуется подтверждение казначейства.\n' + get_approve_message()
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': tr_msg, 'summonees': ['not_treasurer']},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_other'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class TreasuryWrongRequestedDBTestCaseForeigner(AbstractApproveDBTestCase):
    _representation = 'tr_wrong_requested_foreigner'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_,
                                                         currency=810, firm_id=7)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_comments(self):
        tr_msg = 'Договор с нерезидентом, требуется подтверждение казначейства.\n' + get_approve_message()
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': tr_msg, 'summonees': ['not_treasurer']},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_1', 'treasurer_2'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class BoTreasuryNotRequestedDBTestCaseRUB(AbstractApproveDBTestCase):
    _representation = 'bo_tr_not_requested_rub'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_rur', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class BoTreasuryNotRequestedDBTestCaseRUR(AbstractApproveDBTestCase):
    _representation = 'bo_tr_not_requested_rur'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=810)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_rur', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class BoTreasuryNotRequestedDBTestCaseKZT(AbstractApproveDBTestCase):
    _representation = 'bo_tr_not_requested_kzt'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=398)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_kzt', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class BoTreasuryNotRequestedDBTestCaseBYR(AbstractApproveDBTestCase):
    _representation = 'bo_tr_not_requested_byr'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=974)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_byn', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class BoTreasuryNotRequestedDBTestCaseBYN(AbstractApproveDBTestCase):
    _representation = 'bo_tr_not_requested_byn'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=933)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_byn', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class BoTreasuryNotRequestedDBTestCaseUSD(AbstractApproveDBTestCase):
    _representation = 'bo_tr_not_requested_usd'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=840)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_usd', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class BoTreasuryNotRequestedDBTestCaseEUR(AbstractApproveDBTestCase):
    _representation = 'bo_tr_not_requested_eur'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=978)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_eur', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class BoTreasuryNotRequestedDBTestCaseOther(AbstractApproveDBTestCase):
    _representation = 'bo_tr_not_requested_other'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_, currency=949)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_other', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class BoTreasuryNotRequestedDBTestCaseForeigner(AbstractApproveDBTestCase):
    _representation = 'bo_tr_not_requested_foreigner'

    author = 'noob'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, deal_passport=dt_,
                                                         currency=810, firm_id=7)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_1', 'treasurer_2', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class BoTreasuryRequestedTrDBTestCase(AbstractNonresApproveDBTestCase):
    _representation = 'bo_tr_requested_tr'

    author = 'noob'

    def get_comments(self):
        tr_msg = 'Договор с нерезидентом, требуется подтверждение казначейства.\n' + get_approve_message()
        return [
            {'author': 'autodasha', 'text': tr_msg, 'summonees': ['treasurer']},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class BoTreasuryApprovedTrDBTestCase(AbstractNonresApproveDBTestCase):
    _representation = 'bo_tr_approved_tr'

    author = 'noob'

    def get_comments(self):
        tr_msg = 'Договор с нерезидентом, требуется подтверждение казначейства.\n' + get_approve_message()
        return [
            {'author': 'autodasha', 'text': tr_msg, 'summonees': ['treasurer']},
            {'author': 'treasurer', 'text': 'Подтверждено'}
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class TreasuryApprovedDBTestCase(AbstractNonresApproveDBTestCase):
    _representation = 'bo_tr_approved'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'treasurer', 'text': 'Подтверждено'},
            {'author': 'boss', 'text': 'Подтверждено'}
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, assignee='mscnad7',
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата начала" с 01.01.2016 на 01.02.2016;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class BuhNotRequestedDBTestCase(AbstractPartnersApproveDBTestCase):
    _representation = 'buh_not_requested'

    author = 'noob'

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none, summonees=['alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class BuhDistrNotRequestedDBTestCase(AbstractDistrApproveDBTestCase):
    _representation = 'buh_distr_not_requested'

    author = 'noob'

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none, summonees=['alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class AbstractOtherChangesApproveDBTestCase(AbstractApproveDBTestCase):
    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        return self.contract, [
            ('dt', '2016-02-01'),
            ('other_changes', 'Хочу быть царицею морскою'),
        ]


class OtherChangesNotRequestedDBTestCase(AbstractOtherChangesApproveDBTestCase):
    _representation = 'other_changes_not_requested'

    author = 'noob'

    def get_comments(self):
        return []

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message(get_approve_message())
        return res


class OtherChangesRequestedDBTestCase(AbstractOtherChangesApproveDBTestCase):
    _representation = 'other_changes_requested'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class OtherChangesApprovedDBTestCase(AbstractOtherChangesApproveDBTestCase):
    _representation = 'other_changes_approved'

    author = 'noob'
    assignee = 'autodasha'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            ('boss', 'Подтверждено')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False,
                             transition=IssueTransitions.none, assignee='mscnad7',
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('кто:mscnad7, заполнено поле "Другие изменения", посмотри, пожалуйста.')
        return res


class OtherChangesBOTrBuhNotRequestedDBTestCaseRUB(AbstractApproveDBTestCase):
    _representation = 'other_changes_bo_tr_buh_not_requested_rub'

    author = 'noob'

    def get_comments(self):
        return []

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt_})
        return self.contract, [
            ('dt', '2016-02-01'),
            ('other_changes', 'Хочу быть царицею морскою'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_rur', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class OtherChangesBOTrBuhNotRequestedDBTestCaseRUR(AbstractApproveDBTestCase):
    _representation = 'other_changes_bo_tr_buh_not_requested_rur'

    author = 'noob'

    def get_comments(self):
        return []

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt_})
        return self.contract, [
            ('dt', '2016-02-01'),
            ('other_changes', 'Хочу быть царицею морскою'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_rur', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class OtherChangesBOTrBuhNotRequestedDBTestCaseKZT(AbstractApproveDBTestCase):
    _representation = 'other_changes_bo_tr_buh_not_requested_kzt'

    author = 'noob'

    def get_comments(self):
        return []

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_, currency=398)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt_})
        return self.contract, [
            ('dt', '2016-02-01'),
            ('other_changes', 'Хочу быть царицею морскою'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_kzt', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class OtherChangesBOTrBuhNotRequestedDBTestCaseBYR(AbstractApproveDBTestCase):
    _representation = 'other_changes_bo_tr_buh_not_requested_byr'

    author = 'noob'

    def get_comments(self):
        return []

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_, currency=974)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt_})
        return self.contract, [
            ('dt', '2016-02-01'),
            ('other_changes', 'Хочу быть царицею морскою'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_byn', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class OtherChangesBOTrBuhNotRequestedDBTestCaseBYN(AbstractApproveDBTestCase):
    _representation = 'other_changes_bo_tr_buh_not_requested_byn'

    author = 'noob'

    def get_comments(self):
        return []

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_, currency=933)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt_})
        return self.contract, [
            ('dt', '2016-02-01'),
            ('other_changes', 'Хочу быть царицею морскою'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_byn', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class OtherChangesBOTrBuhNotRequestedDBTestCaseEUR(AbstractApproveDBTestCase):
    _representation = 'other_changes_bo_tr_buh_not_requested_eur'

    author = 'noob'

    def get_comments(self):
        return []

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_, currency=978)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt_})
        return self.contract, [
            ('dt', '2016-02-01'),
            ('other_changes', 'Хочу быть царицею морскою'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_eur', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class OtherChangesBOTrBuhNotRequestedDBTestCaseUSD(AbstractApproveDBTestCase):
    _representation = 'other_changes_bo_tr_buh_not_requested_usd'

    author = 'noob'

    def get_comments(self):
        return []

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_, currency=840)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt_})
        return self.contract, [
            ('dt', '2016-02-01'),
            ('other_changes', 'Хочу быть царицею морскою'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_usd', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class OtherChangesBOTrBuhNotRequestedDBTestCaseOther(AbstractApproveDBTestCase):
    _representation = 'other_changes_bo_tr_buh_not_requested_other'

    author = 'noob'

    def get_comments(self):
        return []

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_, currency=949)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt_})
        return self.contract, [
            ('dt', '2016-02-01'),
            ('other_changes', 'Хочу быть царицею морскою'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_other', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class OtherChangesBOTrBuhNotRequestedDBTestCaseForeigner(AbstractApproveDBTestCase):
    _representation = 'other_changes_bo_tr_buh_not_requested_foreigner'

    author = 'noob'

    def get_comments(self):
        return []

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session, person_type='yt')
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_, firm_id=7)
        ins_q = 'INSERT INTO bo.t_partner_act_data (partner_contract_id, status, dt) VALUES (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt_})
        return self.contract, [
            ('dt', '2016-02-01'),
            ('other_changes', 'Хочу быть царицею морскою'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none, summonees=['treasurer_1', 'treasurer_2', 'alt_boss'],
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Договор с нерезидентом, требуется подтверждение казначейства.')
        res.add_message(get_approve_message())
        return res


class AbstractSupReqCChangeDBTestCase(AbstractApproveDBTestCase):
    _param = None
    _param_repr = None

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            ('boss', 'Подтверждено'),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {self._param: dt.datetime(2016, 1, 1)}
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            (self._param, '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True,
                             transition=IssueTransitions.none,
                             state=[(self._param, dt.datetime(2016, 1, 1))], assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "%s" с 01.01.2016 на 01.02.2016;' % self._param_repr)
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class ExternalIDSupReqCChangeDBTestCase(AbstractApproveDBTestCase):
    _representation = 'sup_change_eid'
    _param = 'external_id'
    _param_repr = 'Номер договора'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            ('boss', 'Подтверждено'),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            (self._param, 'KEKEKEKEK'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, done=True, enqueued=True,
                             transition=IssueTransitions.none,
                             state=[(self._param, 'KEKEKEKEK')])
        return res


class DTSupReqCChangeDBTestCase(AbstractSupReqCChangeDBTestCase):
    _representation = 'sup_change_dt'
    _param = 'dt'
    _param_repr = 'Дата начала'


class FinishDTSupReqCChangeDBTestCase(AbstractSupReqCChangeDBTestCase):
    _representation = 'sup_change_finish_dt'
    _param = 'finish_dt'
    _param_repr = 'Дата окончания'


class SentDTSupReqCChangeDBTestCase(AbstractSupReqCChangeDBTestCase):
    _representation = 'sup_change_sent_dt'
    _param = 'sent_dt'
    _param_repr = 'Отправлен оригинал'


class SupReqColAllChangeDBTestCase(AbstractApproveDBTestCase):
    _representation = 'sup_change_col_all'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            ('boss', 'Подтверждено'),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        params = {
            'is_booked': 0,
            'is_booked_dt': dt_,
            'is_signed': dt_,
            'is_faxed': dt_,
            'sent_dt': dt_,
            'finish_dt': dt_,
            'num': '01',
            'collateral_type_id': 90
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt_, **params)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('col', self.collateral),
            ('dt', '2016-07-01'),
            ('finish_dt', '2016-07-01'),
            ('is_signed', '2016-03-01'),
            ('is_faxed', '2016-04-01'),
            ('sent_dt', '2016-05-01'),
            ('col_num', '666'),
            ('is_booked', '2016-06-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True,
                             transition=IssueTransitions.none,
                             state=[
                                 ('is_booked_dt', dt.datetime(2016, 1, 1)),
                                 ('is_signed', dt.datetime(2016, 1, 1)),
                                 ('is_faxed', dt.datetime(2016, 1, 1)),
                                 ('finish_dt', dt.datetime(2016, 1, 1)),
                                 ('dt', dt.datetime(2016, 1, 1)),
                                 ('sent_dt', dt.datetime(2016, 1, 1)),
                                 ('num', '01'),
                                 ('is_booked', 0)
                             ],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s, ДС № 01 (расторжение договора) от 01.01.2016,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата брони подписи" с 01.01.2016 на 01.06.2016;')
        res.add_message('* меняем параметр "Флаг брони подписи" с Нет на Да;')
        res.add_message('* меняем параметр "Дата начала" с 01.01.2016 на 01.07.2016;')
        res.add_message('* меняем параметр "Дата окончания" с 01.01.2016 на 01.07.2016;')
        res.add_message('* меняем параметр "Подписан" с 01.01.2016 на 01.03.2016;')
        res.add_message('* меняем параметр "Подписан по факсу" с 01.01.2016 на 01.04.2016;')
        res.add_message('* меняем параметр "Отправлен оригинал" с 01.01.2016 на 01.05.2016;')
        res.add_message('* меняем параметр "Номер ДС" с 01 на 666;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class AbstractSupReqCRemoveDBTestCase(AbstractApproveDBTestCase):
    _param = None
    _param_repr = None

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            ('boss', 'Подтверждено'),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {self._param: dt.datetime(2016, 1, 1)}
        self.contract = db_utils.create_general_contract(session, client, person, **params)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('remove_%s' % self._param, 'True'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none,
                             state=[(self._param, dt.datetime(2016, 1, 1))], assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* убираем параметр "%s", был 01.01.2016;' % self._param_repr)
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class SentDTSupReqCRemoveDBTestCase(AbstractSupReqCChangeDBTestCase):
    _representation = 'sup_remove_sent_dt'
    _param = 'sent_dt'
    _param_repr = 'Отправлен оригинал'


class AbstractSupReqCSetDBTestCase(AbstractApproveDBTestCase):
    _param = None
    _param_repr = None

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            ('boss', 'Подтверждено'),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            (self._param, '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none, assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* добавляем параметр "%s", будет 01.02.2016;' % self._param_repr)
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class FinishDTSupReqCSetDBTestCase(AbstractSupReqCSetDBTestCase):
    _representation = 'sup_set_finish_dt'
    _param = 'finish_dt'
    _param_repr = 'Дата окончания'


class SentDTSupReqCSetDBTestCase(AbstractSupReqCSetDBTestCase):
    _representation = 'sup_set_sent_dt'
    _param = 'sent_dt'
    _param_repr = 'Отправлен оригинал'


class IsCancelledSupReqSetDBTestCase(AbstractApproveDBTestCase):
    _representation = 'sup_set_is_cancelled'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            ('boss', 'Подтверждено'),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('is_cancelled', 'True'),
            ('remove_is_signed', 'True')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none,
                             state=[
                                 ('is_cancelled', None),
                                 ('is_signed', dt.datetime(2016, 1, 1))
                             ],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Аннулирован" с Нет на Да;')
        res.add_message('* убираем параметр "Подписан", сейчас 01.01.2016;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class SupReqCombinedDBTestCase(AbstractApproveDBTestCase):
    _representation = 'sup_combined'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            ('boss', 'Подтверждено'),
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['accountant']},
            ('accountant', 'Подтверждено'),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_faxed': dt.datetime(2016, 1, 1),
            'is_booked': 1,
            'is_booked_dt': dt.datetime(2016, 2, 1),
            'num': '666'
        }
        self.contract = db_utils.create_partners_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.collateral = db_utils.add_collateral(self.contract, 2010, dt_=dt.datetime(2016, 1, 1), **params)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)

        ins_q = 'insert into bo.t_partner_act_data (partner_contract_id, status, dt) values (:id, 0, :dt)'
        session.execute(ins_q, {'id': self.contract.id, 'dt': dt.datetime(2016, 1, 1)})

        return self.contract, [
            ('col', self.collateral),
            ('is_signed', '2016-02-01'),
            ('is_booked', '2016-03-01'),
            ('finish_dt', '2016-03-01'),
            ('remove_is_faxed', 'True'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True, transition=IssueTransitions.none,
                             state=[
                                 ('dt', dt.datetime(2016, 1, 1)),
                                 ('is_booked_dt', dt.datetime(2016, 2, 1)),
                                 ('is_faxed', dt.datetime(2016, 1, 1)),
                                 ('is_booked', 1)
                             ],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s, ДС № 666 (РСЯ: изменение налогообложения) от 01.01.2016,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* добавляем параметр "Подписан", будет 01.02.2016;')
        res.add_message('* добавляем параметр "Дата окончания", будет 01.03.2016;')
        res.add_message('* убираем параметр "Подписан по факсу", сейчас 01.01.2016;')
        res.add_message('* меняем параметр "Дата брони подписи" с 01.02.2016 на 01.03.2016;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class IsBookedSupReqChangeDBTestCase(AbstractApproveDBTestCase):
    _representation = 'sup_change_is_booked'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            ('boss', 'Подтверждено'),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_booked': 1,
            'is_booked_dt': dt.datetime(2016, 1, 1)
        }
        self.contract = db_utils.create_general_contract(session, client, person, **params)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('is_booked', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none,
                             state=[
                                 ('is_booked_dt', dt.datetime(2016, 1, 1)),
                                 ('is_booked', 1)
                             ],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата брони подписи" с 01.01.2016 на 01.02.2016;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class IsBookedSupReqCSetDBTestCase(AbstractApproveDBTestCase):
    _representation = 'sup_set_is_booked'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            ('boss', 'Подтверждено'),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('is_booked', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none, assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* добавляем параметр "Дата брони подписи", будет 01.02.2016;')
        res.add_message('* меняем параметр "Флаг брони подписи" с Нет на Да;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class IsBookedSupReqChangeSetFlagDBTestCase(AbstractApproveDBTestCase):
    _representation = 'sup_change_set_is_booked'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            ('boss', 'Подтверждено'),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_booked': 0,
            'is_booked_dt': dt.datetime(2016, 1, 1)
        }
        self.contract = db_utils.create_general_contract(session, client, person, **params)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('is_booked', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none,
                             state=[
                                 ('is_booked_dt', dt.datetime(2016, 1, 1)),
                                 ('is_booked', 0)
                             ],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Дата брони подписи" с 01.01.2016 на 01.02.2016;')
        res.add_message('* меняем параметр "Флаг брони подписи" с Нет на Да;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class IsBookedSupReqChangeSetFlagSameDTDBTestCase(AbstractApproveDBTestCase):
    _representation = 'sup_change_set_is_booked_same_dt'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            ('boss', 'Подтверждено'),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_booked': 0,
            'is_booked_dt': dt.datetime(2016, 1, 1)
        }
        self.contract = db_utils.create_general_contract(session, client, person, **params)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('is_booked', '2016-01-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none,
                             state=[
                                 ('is_booked_dt', dt.datetime(2016, 1, 1)),
                                 ('is_booked', 0)
                             ],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Флаг брони подписи" с Нет на Да;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class IsBookedSupReqCRemoveDBTestCase(AbstractApproveDBTestCase):
    _representation = 'sup_remove_is_booked'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            ('boss', 'Подтверждено'),
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_booked': 1,
            'is_booked_dt': dt.datetime(2016, 1, 1)
        }
        self.contract = db_utils.create_general_contract(session, client, person, **params)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('remove_is_booked', 'True'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none,
                             state=[
                                 ('is_booked_dt', dt.datetime(2016, 1, 1)),
                                 ('is_booked', 1)
                             ],
                             assignee='mscnad7')
        res.add_message('кто:mscnad7, проверь, пожалуйста, и подтверди.')
        res.add_message('В договоре № %s от %s,' %
                        (self.contract.external_id, self.contract.col0.dt.strftime('%d.%m.%Y')))
        res.add_message('тип - %s, фирма - %s, сервисы - %s:' % self.contract_info)
        res.add_message('* меняем параметр "Флаг брони подписи" с Да на Нет;')
        res.add_message('')
        res.add_message(get_approve_message())
        return res


class SupReqWaitingDBTestCase(AbstractApproveDBTestCase):
    _representation = 'sup_waiting'

    author = 'noob'

    def get_comments(self):
        sup_cmt = '''кто:mscnad7, проверь, пожалуйста, и подтверди.
В договоре № %s от %s:
* добавляем параметр "Дата брони подписи", будет 01.02.2016;
* меняем параметр "Флаг брони подписи" с Нет на Да;

'''
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': sup_cmt + get_approve_message(), 'summonees': []}
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none, assignee=None,
                             state=[('dt', dt.datetime(2016, 1, 1))])
        return res


class NotApprovedFailedStateDBTestCase(AbstractApproveDBTestCase):
    _representation = 'not_approved_failed_state'

    author = 'noob'

    def get_comments(self):
        return []

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, is_faxed=dt.datetime(2016, 2, 1))
        return self.contract, [
            ('is_faxed', '2016-01-01'),
            ('remove_is_faxed', 'True'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.wont_fix,
                             state=[('is_faxed', dt.datetime(2016, 2, 1))])
        res.add_message('К изменению указаны противоречивые значения параметра "Подписан по факсу". '
                        'Уточни, пожалуйста, и заполни при необходимости форму еще раз.')
        return res


class NotApprovedFailedChecksDBTestCase(AbstractApproveDBTestCase):
    _representation = 'not_approved_failed_checks'

    author = 'noob'

    def get_comments(self):
        return []

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col_num', '666'),
            ('is_signed', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.wont_fix,
                             state=[('is_signed', dt.datetime(2016, 1, 1))])
        res.add_message('Номер ДС не указан. Заполни, пожалуйста, форму еще раз с указанием номера ДС.')
        return res


class IsSignedCol0WOApprove1DBTestCase(AbstractApproveDBTestCase):
    _representation = 'is_signed_col0_wo_approve_1'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'}
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.contract.col0.is_signed = None
        session.flush()
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('is_signed', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.none, done=True, enqueued=True,
                             state=[('is_signed', dt.datetime(2016, 2, 1))])
        return res


class IsSignedCol0WOApprove2DBTestCase(AbstractApproveDBTestCase):
    _representation = 'is_signed_col0_wo_approve_2'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'}
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('is_signed', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.none, done=True, enqueued=True,
                             state=[('is_signed', dt.datetime(2016, 2, 1))])
        return res


class IsSignedCol0WApprove2DBTestCase(AbstractApproveDBTestCase):
    _representation = 'is_signed_col0_w_approve_2'

    author = 'noob'

    def get_comments(self):
        sup_cmt = '''кто:mscnad7, проверь, пожалуйста, и подтверди.
В договоре № %s от %s, ДС № %s от %s,
* добавляем параметр "Подписан", будет 01.02.2016;
* меняем параметр "Дата начала" с 01.01.2016 на 01.02.2016;

        '''
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': sup_cmt + get_approve_message(), 'summonees': []}
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('is_signed', '2016-02-01'),
            ('dt', '2016-02-01')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none, assignee=None,
                             state=[('is_signed', dt.datetime(2016, 1, 1)), ('dt', dt.datetime(2016, 1, 1))])
        return res


class IsSignedCol0WOApprove3DBTestCase(AbstractApproveDBTestCase):
    _representation = 'is_signed_col0_wo_approve_3'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'}
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.contract.col0.is_faxed = self.contract.col0.is_signed
        session.flush()
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('remove_is_signed', True),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.none, done=True, enqueued=True,
                             state=[('is_signed', None)])
        return res


class IsSignedCol0WOApprove4DBTestCase(AbstractApproveDBTestCase):
    _representation = 'is_signed_col0_wo_approve_4'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'}
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.contract.col0.is_faxed = self.contract.col0.is_signed
        session.flush()
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('remove_is_signed', True),
            ('external_id', self.contract.external_id + '/test'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.none, done=True, enqueued=True,
                             state=[('is_signed', None), ('external_id', self.contract.external_id + '/test')])
        return res


class IsSignedCol0WApprove4DBTestCase(AbstractApproveDBTestCase):
    _representation = 'is_signed_col0_w_approve_4'

    author = 'noob'

    def get_comments(self):
        sup_cmt = '''кто:mscnad7, проверь, пожалуйста, и подтверди.
    В договоре № %s от %s, ДС № %s от %s,
    * добавляем параметр "Подписан", будет 01.02.2016;
    * меняем параметр "Дата начала" с 01.01.2016 на 01.02.2016;

            '''
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': sup_cmt + get_approve_message(), 'summonees': []}
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('is_signed', '2016-02-01'),
            ('dt', '2016-02-01'),
            ('external_id', self.contract.external_id + '/test'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=True, transition=IssueTransitions.none, assignee=None,
                             state=[('is_signed', dt.datetime(2016, 1, 1)),
                                    ('dt', dt.datetime(2016, 1, 1)),
                                    ('external_id', self.contract.external_id)])
        return res


class IsSignedColWApprove1DBTestCase(AbstractApproveDBTestCase):
    _representation = 'is_signed_col_w_approve_1'

    author = 'noob'

    def get_comments(self):
        sup_cmt = '''кто:mscnad7, проверь, пожалуйста, и подтверди.
В договоре № %s от %s, ДС № %s от %s,
* добавляем параметр "Подписан", будет 01.02.2016;

        '''
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': sup_cmt + get_approve_message(), 'summonees': []}
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, collateral_type_id=90, dt_=dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('col', self.collateral),
            ('is_signed', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True, transition=IssueTransitions.none,
                             assignee=None)
        return res


class IsCancelledCancellingColWOApproveDBTestCase(AbstractApproveDBTestCase):
    _representation = 'is_cancelled_cancelling_col_wo_approve'

    author = 'noob'

    def get_comments(self):
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, 90, dt_=dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('col', self.collateral),
            ('is_cancelled', 'True')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.none,
                             done=True, enqueued=True,
                             state=[('is_cancelled', True)])
        return res


class IsCancelledCancellingColWApproveDBTestCase(AbstractApproveDBTestCase):
    _representation = 'is_cancelled_cancelling_col_w_approve'

    author = 'noob'

    def get_comments(self):
        sup_cmt = '''кто:mscnad7, проверь, пожалуйста, и подтверди.
В договоре № %s от %s, ДС № %s от %s,
* меняем параметр "Аннулирован", с Нет на Да;

        '''
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': sup_cmt + get_approve_message(), 'summonees': []}
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, 90, dt_=dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('col', self.collateral),
            ('is_cancelled', 'True'),
            ('dt', '2016-02-01')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True, transition=IssueTransitions.none,
                             assignee=None, state=[('is_cancelled', None), ('dt', dt.datetime(2016, 1, 1))])
        return res


class IsCancelledColWApprove1DBTestCase(AbstractApproveDBTestCase):
    _representation = 'is_cancelled_col_w_approve_1'

    author = 'noob'

    def get_comments(self):
        sup_cmt = '''кто:mscnad7, проверь, пожалуйста, и подтверди.
В договоре № %s от %s, ДС № %s от %s,
* меняем параметр "Аннулирован", с Нет на Да;

        '''
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': sup_cmt + get_approve_message(), 'summonees': []}
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, collateral_type_id=80, dt_=dt_)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)
        return self.contract, [
            ('col', self.collateral),
            ('is_cancelled', 'True'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=True, transition=IssueTransitions.none,
                             assignee=None, state=[('is_cancelled', None)])
        return res


class SetParamsGeneralCollateralWTTDBTestCase(AbstractApproveDBTestCase):
    _representation = 'set_gen_col_w_tt'

    author = 'noob'

    def get_comments(self):
        sup_cmt = '''кто:mscnad7, проверь, пожалуйста, и подтверди.
В договоре № %s от %s, ДС № %s от %s,
* меняем параметр "Аннулирован", с Нет на Да;
Коллеги, обратите, пожалуйста, внимание:
* !!(кра)Вносимые изменения затрагивают существующие транзакции по договору.
Подтверди, пожалуйста, если изменения всё-таки нужно внести или закрой тикет.!!;

            '''
        return [
            {'author': 'autodasha', 'text': get_approve_message(), 'summonees': ['boss']},
            {'author': 'boss', 'text': 'Подтверждено'},
            {'author': 'autodasha', 'text': sup_cmt + get_approve_message(), 'summonees': []},
            {'author': 'mscnad7', 'text': 'Подтверждено'},
        ]

    def _get_data(self, session):
        contract_params = {
            'country': 225,
            'partner_commission_pct2': 1,
            'firm': 13,
            'services': {124},
        }
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         **contract_params)
        col_attrs = {
            'print_form_type': 3,
            'partner_commission_pct': 66,
            'partner_commission_pct2': 666,
            'partner_commission_type': 2,
            'partner_min_commission_sum': 6,
            'partner_max_commission_sum': 6666,
        }
        self.collateral = db_utils.add_collateral(self.contract, 1030, dt_=dt.datetime(2016, 6, 3), **col_attrs)
        self.contract_info = db_utils.get_contract_information_for_support_approve(session, self.contract)

        payment = db_utils.create_thirdparty_transaction(session, self.contract, 124, **{'dt': dt.datetime(2016, 6, 4)})

        return self.contract, [
            ('col', self.collateral),
            ('is_signed', dt.datetime(2016, 6, 5)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, done=True, enqueued=True,
                             transition=IssueTransitions.none, state=[('is_signed', dt.datetime(2016, 6, 5))])
        return res
