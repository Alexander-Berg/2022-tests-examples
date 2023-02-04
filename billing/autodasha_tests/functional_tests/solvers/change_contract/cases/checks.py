# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

from balance import muzzle_util as ut

from autodasha.core.api.tracker import IssueTransitions

from tests.autodasha_tests.common import db_utils
from tests.autodasha_tests.functional_tests import case_utils
from tests.autodasha_tests.functional_tests.solvers.change_contract.common import (
    AbstractBaseDBTestCase, RequiredResult
)

__all__ = ['AbstractCheckDBTestCase']


class AbstractCheckDBTestCase(AbstractBaseDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class AbstractApprovedDBTestCase(AbstractCheckDBTestCase):
    author = 'noob'

    def setup_config(self, session, config):
        config['CHANGE_CONTRACT']['payments_check'] = True

    def get_comments(self):
        return [
            ('boss', 'Подтверждено'),
            ('mscnad7', 'Подтверждено')
        ]


class StateFailSetParamsGeneralContractDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'state_fail_set_params_gen_c'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_booked_dt': dt.datetime(2016, 3, 1),
            'is_booked': 1,
            'is_faxed': dt.datetime(2016, 4, 1),
            'is_signed': dt.datetime(2016, 5, 1),
            'sent_dt': dt.datetime(2016, 6, 1),
            'finish_dt': dt.datetime(2016, 8, 1),
            'is_cancelled': dt.datetime(2016, 9, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('is_faxed', dt.datetime(2016, 6, 1)),
            ('remove_is_faxed', True),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_booked_dt', dt.datetime(2016, 3, 1)),
            ('is_booked', 1),
            ('is_faxed', dt.datetime(2016, 4, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('sent_dt', dt.datetime(2016, 6, 1)),
            ('finish_dt', dt.datetime(2016, 8, 1)),
            ('is_cancelled', dt.datetime(2016, 9, 1)),
        ])
        res.add_message('К изменению указаны противоречивые значения параметра "Подписан по факсу". '
                        'Уточни, пожалуйста, и заполни при необходимости форму еще раз.')
        return res


class AlreadyChangedDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'already_changed'

    def get_comments(self):
        return [
            ('big_boss', 'Подтверждено'),
            ('mscnad7', 'Подтверждено'),
            ('autodasha', 'Изменённые договор и допсоглашение добавлены в очередь на выгрузку в ОЕБС.')
        ]

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
        ]

    def get_result(self):
        return None


class NumNoCollateralDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'num_no_collateral'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': dt.datetime(2016, 5, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('col_num', 'Ахалай-Махалай'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_signed', dt.datetime(2016, 5, 1)),
        ])
        res.add_message('Номер ДС не указан. Заполни, пожалуйста, форму еще раз с указанием номера ДС.')
        return res


class CNotSignedDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_not_signed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': None,
            'finish_dt': dt.datetime(2016, 2, 1)
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('finish_dt', '2016-03-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.none, done=True, enqueued=True,
                             state=[('finish_dt', dt.datetime(2016, 3, 1)),])
        return res


class CNotSignedDTDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_not_signed_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': None,
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.none, done=True, enqueued=True,
                             state=[('dt', dt.datetime(2016, 2, 1)),
        ])
        return res


class CNotSignedDTOldDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_not_signed_dt_old'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': None,
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('dt', '2015-12-31'),
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none,
                              state=[('dt', dt.datetime(2015, 12, 31))])


class CNotSignedIsCancelledDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_not_signed_is_cancelled'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': None,
            'finish_dt': dt.datetime(2016, 2, 1)
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('is_cancelled', True),
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none,
                              state=[('is_cancelled', True)])


class CNotSignedNonStandardDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_not_signed_other_changes'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': None,
            'finish_dt': dt.datetime(2016, 2, 1)
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('other_changes', 'Убий сибя апстену')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False,
                             transition=IssueTransitions.none, assignee='mscnad7',
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('кто:mscnad7, заполнено поле "Другие изменения", посмотри, пожалуйста.')
        return res


class ColNotSignedDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'col_not_signed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': dt.datetime(2016, 5, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('col_num', '666'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True, delay=False,
                             transition=IssueTransitions.none, state=[('num', '666'), ])
        return res


class ColNotSignedDTDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'col_not_signed_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': dt.datetime(2016, 5, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 2, 1)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True, delay=False,
                             transition=IssueTransitions.none, state=[('dt', dt.datetime(2016, 2, 1)),
        ])
        return res


class ColNotSignedDTOldDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'col_not_signed_dt_old'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': dt.datetime(2016, 5, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2015, 1, 1), **params)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2015, 12, 31)),
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none,
                              state=[('dt', dt.datetime(2015, 12, 31))])


class ColNotSignedIsCancelledDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'col_not_signed_is_cancelled'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': dt.datetime(2016, 5, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('is_cancelled', True),
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral,
                              done=True, enqueued=True, delay=False,
                              transition=IssueTransitions.none,
                              state=[('is_cancelled', True)])


class ColNotSignedNonStandardDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'col_not_signed_other_changes'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': dt.datetime(2016, 5, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('other_changes', 'Убий сибя апстену')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False,
                             transition=IssueTransitions.none, assignee='mscnad7',
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('кто:mscnad7, заполнено поле "Другие изменения", посмотри, пожалуйста.')
        return res


class AbstractCRemoveRemovedDBTestCase(AbstractApprovedDBTestCase):
    _remove_param = None
    _remove_param_repr = None
    _sign_param = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)

        params = dict(is_signed=None)
        params[self._sign_param] = dt.datetime(2016, 1, 1)

        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            (self._remove_param, 'True'),
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.wont_fix, state=[
            (self._sign_param, dt.datetime(2016, 1, 1)),
            ('dt', dt.datetime(2016, 1, 1))
        ])
        res.add_message('Параметр "%s" в договоре/ДС не проставлен. '
                        'Уточни, пожалуйста, что требуется изменить и заполни форму еще раз.' % self._remove_param_repr)
        return res


class BookedCRemoveRemovedDBTestCase(AbstractCRemoveRemovedDBTestCase):
    _representation = 'c_remove_removed_is_booked'
    _remove_param = 'remove_is_booked'
    _remove_param_repr = 'Флаг брони подписи'
    _sign_param = 'is_signed'


class FaxedCRemoveRemovedDBTestCase(AbstractCRemoveRemovedDBTestCase):
    _representation = 'c_remove_removed_is_faxed'
    _remove_param = 'remove_is_faxed'
    _remove_param_repr = 'Подписан по факсу'
    _sign_param = 'is_signed'


class SignedCRemoveRemovedDBTestCase(AbstractCRemoveRemovedDBTestCase):
    _representation = 'c_remove_removed_is_signed'
    _remove_param = 'remove_is_signed'
    _remove_param_repr = 'Подписан'
    _sign_param = 'is_faxed'


class AbstractColRemoveRemovedDBTestCase(AbstractApprovedDBTestCase):
    _remove_param = None
    _remove_param_repr = None
    _sign_param = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)

        params = {
            self._sign_param: dt.datetime(2016, 1, 1),
            'dt_': dt.datetime(2016, 1, 1),
        }

        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.collateral = db_utils.add_collateral(self.contract, **params)
        return self.contract, [
            ('col', self.collateral),
            (self._remove_param, 'True'),
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            (self._sign_param, dt.datetime(2016, 1, 1)),
            ('dt', dt.datetime(2016, 1, 1))
        ])
        res.add_message('Параметр "%s" в договоре/ДС не проставлен. '
                        'Уточни, пожалуйста, что требуется изменить и заполни форму еще раз.' % self._remove_param_repr)
        return res


class BookedColRemoveRemovedDBTestCase(AbstractColRemoveRemovedDBTestCase):
    _representation = 'col_remove_removed_is_booked'
    _remove_param = 'remove_is_booked'
    _remove_param_repr = 'Флаг брони подписи'
    _sign_param = 'is_signed'


class FaxedColRemoveRemovedDBTestCase(AbstractColRemoveRemovedDBTestCase):
    _representation = 'col_remove_removed_is_faxed'
    _remove_param = 'remove_is_faxed'
    _remove_param_repr = 'Подписан по факсу'
    _sign_param = 'is_signed'


class SignedColRemoveRemovedDBTestCase(AbstractColRemoveRemovedDBTestCase):
    _representation = 'col_remove_removed_is_signed'
    _remove_param = 'remove_is_signed'
    _remove_param_repr = 'Подписан'
    _sign_param = 'is_faxed'


class NoChangesCDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'no_changes_c'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': dt.datetime(2016, 5, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, []

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('is_signed', dt.datetime(2016, 5, 1)),
        ])
        res.add_message("Не указано, какие изменения необходимо внести. Уточни, пожалуйста, и заполни форму еще раз.")
        return res


class NoChangesColDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'no_changes_col'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': dt.datetime(2016, 5, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        self.collateral = db_utils.add_collateral(self.contract, is_signed=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('is_signed', dt.datetime(2016, 1, 1)),
        ])
        res.add_message("Не указано, какие изменения необходимо внести. Уточни, пожалуйста, и заполни форму еще раз.")
        return res


class OnlyOtherChangesCDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'only_other_changes_c'

    assignee = 'autodasha'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': dt.datetime(2016, 5, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('other_changes', 'Принесите мне печеньку')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.none, assignee='mscnad7',
                             state=[('is_signed', dt.datetime(2016, 5, 1))])
        res.add_message('кто:mscnad7, заполнено поле "Другие изменения", посмотри, пожалуйста.')
        return res


class OnlyOtherChangesColDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'only_other_changes_col'

    assignee = 'autodasha'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        params = {
            'is_signed': dt.datetime(2016, 5, 1),
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        self.collateral = db_utils.add_collateral(self.contract, is_signed=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('other_changes', 'Принесите мне мороженку')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral,
                             delay=False, transition=IssueTransitions.none, assignee='mscnad7',
                             state=[('is_signed', dt.datetime(2016, 1, 1))])
        res.add_message('кто:mscnad7, заполнено поле "Другие изменения", посмотри, пожалуйста.')
        return res


class AbstractCSetUnchangedDBTestCase(AbstractApprovedDBTestCase):
    _param = None
    _param_repr = None
    _add_param = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)

        params = dict(is_signed=dt.datetime(2016, 1, 1))
        params[self._param] = dt.datetime(2016, 1, 1)
        params[self._add_param] = dt.datetime(2016, 1, 1)

        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            (self._param, '2016-01-01'),
            (self._add_param, '2015-12-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.wont_fix, state=[
            (self._add_param, dt.datetime(2016, 1, 1)),
            (self._param, dt.datetime(2016, 1, 1)),
        ])
        res.add_message('Проверь, пожалуйста, указанное тобой значение для параметра "%s" - '
                        'оно совпадает с текущим значением, и при необходимости заполни форму еще раз.'
                        % self._param_repr)
        return res


class DTCSetUnchangedDBTestCase(AbstractCSetUnchangedDBTestCase):
    _representation = 'c_set_unchanged_dt'
    _param = 'dt'
    _param_repr = 'Дата начала'
    _add_param = 'is_faxed'


class FinishDtCSetUnchangedDBTestCase(AbstractCSetUnchangedDBTestCase):
    _representation = 'c_set_unchanged_finish_dt'
    _param = 'finish_dt'
    _param_repr = 'Дата окончания'
    _add_param = 'dt'


class IsFaxedCSetUnchangedDBTestCase(AbstractCSetUnchangedDBTestCase):
    _representation = 'c_set_unchanged_is_faxed'
    _param = 'is_faxed'
    _param_repr = 'Подписан по факсу'
    _add_param = 'dt'


class IsSignedCSetUnchangedDBTestCase(AbstractCSetUnchangedDBTestCase):
    _representation = 'c_set_unchanged_is_signed'
    _param = 'is_signed'
    _param_repr = 'Подписан'
    _add_param = 'dt'


class SentDTCSetUnchangedDBTestCase(AbstractCSetUnchangedDBTestCase):
    _representation = 'c_set_unchanged_sent_dt'
    _param = 'sent_dt'
    _param_repr = 'Отправлен оригинал'
    _add_param = 'dt'


class IsBookedCSetUnchangedDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_set_unchanged_is_booked'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)

        params = dict(is_signed=dt.datetime(2016, 1, 1),
                      is_booked=1,
                      is_booked_dt=dt.datetime(2016, 1, 1))

        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('is_booked', '2016-01-01'),
            ('dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('is_booked', 1),
            ('is_booked_dt', dt.datetime(2016, 1, 1)),
            ('dt', dt.datetime(2016, 1, 1)),
        ])
        res.add_message('Проверь, пожалуйста, указанное тобой значение для параметра "Дата брони подписи" - '
                        'оно совпадает с текущим значением, и при необходимости заполни форму еще раз.')
        return res


class OfferConfirmationSetUnchangedDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_set_unchanged_offer_confirmation'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)

        self.contract = db_utils.create_general_contract(session, client, person, offer_confirmation_type='no')
        return self.contract, [
            ('offer_confirmation_type', 'Не требуется'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('offer_confirmation_type', 'no')
        ])
        res.add_message('Проверь, пожалуйста, указанное тобой значение для параметра "Способ подтверждения оферты" - '
                        'оно совпадает с текущим значением, и при необходимости заполни форму еще раз.')
        return res


class CSetChangedDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_set_changed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)

        dt_ = dt.datetime(2016, 1, 1)
        params = dict(is_signed=dt_,
                      is_booked=1,
                      is_booked_dt=dt_,
                      is_faxed=dt_,
                      sent_dt=dt_)

        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('is_booked', '2016-03-01'),
            ('dt', '2016-02-01'),
            ('is_signed', '2016-03-01'),
            ('is_faxed', '2016-03-01'),
            ('sent_dt', '2016-03-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.none, done=1, enqueued=1, state=[
            ('is_booked', 1),
            ('is_booked_dt', dt.datetime(2016, 3, 1)),
            ('is_signed', dt.datetime(2016, 3, 1)),
            ('is_faxed', dt.datetime(2016, 3, 1)),
            ('sent_dt', dt.datetime(2016, 3, 1)),
            ('dt', dt.datetime(2016, 2, 1)),
        ])
        return res


class CSetChangedBookedFlagDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_set_changed_booked_flag'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)

        dt_ = dt.datetime(2016, 1, 1)
        params = dict(is_booked=0, is_booked_dt=dt_)

        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **params)
        return self.contract, [
            ('is_booked', '2016-01-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.none, done=1, enqueued=1, state=[
            ('is_booked', 1),
            ('is_booked_dt', dt.datetime(2016, 1, 1)),
            ('dt', dt.datetime(2016, 1, 1)),
        ])
        return res


class AbstractColSetUnchangedDBTestCase(AbstractApprovedDBTestCase):
    _param = None
    _param_repr = None
    _add_param = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)

        params = dict(is_signed=dt.datetime(2016, 1, 1), dt_=dt.datetime(2016, 1, 1))
        params[self._param] = dt.datetime(2016, 1, 1)
        params[self._add_param] = dt.datetime(2016, 1, 1)

        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.collateral = db_utils.add_collateral(self.contract, **params)
        return self.contract, [
            ('col', self.collateral),
            (self._param, '2016-01-01'),
            (self._add_param, '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            (self._add_param, dt.datetime(2016, 1, 1)),
            (self._param, dt.datetime(2016, 1, 1)),
        ])
        res.add_message('Проверь, пожалуйста, указанное тобой значение для параметра "%s" - '
                        'оно совпадает с текущим значением, и при необходимости заполни форму еще раз.'
                        % self._param_repr)
        return res


class FinishDtColSetUnchangedDBTestCase(AbstractColSetUnchangedDBTestCase):
    _representation = 'col_set_unchanged_finish_dt'
    _param = 'finish_dt'
    _param_repr = 'Дата окончания'
    _add_param = 'is_faxed'


class IsFaxedColSetUnchangedDBTestCase(AbstractColSetUnchangedDBTestCase):
    _representation = 'col_set_unchanged_is_faxed'
    _param = 'is_faxed'
    _param_repr = 'Подписан по факсу'
    _add_param = 'is_signed'


class IsSignedColSetUnchangedDBTestCase(AbstractColSetUnchangedDBTestCase):
    _representation = 'col_set_unchanged_is_signed'
    _param = 'is_signed'
    _param_repr = 'Подписан'
    _add_param = 'is_faxed'


class SentDTColSetUnchangedDBTestCase(AbstractColSetUnchangedDBTestCase):
    _representation = 'col_set_unchanged_sent_dt'
    _param = 'sent_dt'
    _param_repr = 'Отправлен оригинал'
    _add_param = 'is_faxed'


class MultipleErrorsNonfatalDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'multiple_errors_nonfatal'
    _param = None
    _param_repr = None
    _add_param = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)

        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        return self.contract, [
            ('dt', '2016-02-01'),
            ('is_signed', '2016-01-01'),
            ('remove_is_faxed', 'True'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_signed', dt.datetime(2016, 1, 1)),
        ])
        res.add_message('Проверь, пожалуйста, указанное тобой значение для параметра "Подписан" - '
                        'оно совпадает с текущим значением, и при необходимости заполни форму еще раз.')
        res.add_message('Параметр "Подписан по факсу" в договоре/ДС не проставлен. '
                        'Уточни, пожалуйста, что требуется изменить и заполни форму еще раз.')
        return res


class MultipleErrorsFatalDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'multiple_errors_fatal'
    _param = None
    _param_repr = None
    _add_param = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)

        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col_num', '123123131'),
            ('dt', '2016-02-01'),
            ('is_signed', '2016-01-01'),
            ('remove_is_faxed', 'True'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_signed', dt.datetime(2016, 1, 1)),
        ])
        res.add_message('Номер ДС не указан. Заполни, пожалуйста, форму еще раз с указанием номера ДС.')
        return res


class FinishColSuccessDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'general_finish_matched'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.collateral = db_utils.add_collateral(self.contract, 90, dt.datetime(2016, 2, 1),
                                                  is_signed=dt.datetime(2016, 1, 1), finish_dt=dt.datetime(2016, 3, 1))
        return self.contract, [
            ('col', self.collateral),
            ('dt', '2016-05-01'),
            ('finish_dt', '2016-05-01'),
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral, transition=IssueTransitions.none,
                              done=True, enqueued=True, delay=False,
                              state=[
                                  ('dt', dt.datetime(2016, 5, 1)),
                                  ('finish_dt', dt.datetime(2016, 5, 1)),
                              ])


class FinishColOtherParamsDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'general_finish_other_params'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.collateral = db_utils.add_collateral(self.contract, 90, dt.datetime(2016, 2, 1),
                                                  is_signed=dt.datetime(2016, 1, 1), finish_dt=dt.datetime(2016, 2, 1))
        return self.contract, [
            ('col', self.collateral),
            ('is_faxed', '2016-05-01'),
        ]

    def get_result(self):
        return RequiredResult(self.contract, self.collateral, transition=IssueTransitions.none,
                              done=True, enqueued=True, delay=False,
                              state=[
                                  ('dt', dt.datetime(2016, 2, 1)),
                                  ('finish_dt', dt.datetime(2016, 2, 1)),
                                  ('is_faxed', dt.datetime(2016, 5, 1)),
                              ])


class AbstractFinishUnmatchedDBTestCase(AbstractApprovedDBTestCase):
    _start_dt = None
    _start_finish_dt = None
    _req_dt = None
    _req_finish_dt = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.collateral = db_utils.add_collateral(self.contract, 90, self._start_dt,
                                                  is_signed=dt.datetime(2016, 1, 1), finish_dt=self._start_finish_dt)
        changes = [
            ('col', self.collateral),
        ]
        if self._req_dt:
            changes.append(('dt', self._req_dt))
        if self._req_finish_dt:
            changes.append(('finish_dt', self._req_finish_dt))

        return self.contract, changes

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', self._start_dt),
            ('finish_dt', self._start_finish_dt),
        ])
        res.add_message('Дата окончания договора должна быть не раньше даты начала договора/ДС. '
                        'Уточни, пожалуйста, даты и при необходимости заполни форму еще раз.')
        return res


class GeneralFinishUnmatchedDTDBTestCase(AbstractFinishUnmatchedDBTestCase):
    _representation = 'general_finish_unmatched_dt'
    _start_dt = dt.datetime(2016, 2, 1)
    _start_finish_dt = dt.datetime(2016, 2, 1)
    _req_dt = '2016-03-01'


class GeneralFinishUnmatchedFinishDTDBTestCase(AbstractFinishUnmatchedDBTestCase):
    _representation = 'general_finish_unmatched_finish_dt'
    _start_dt = dt.datetime(2016, 2, 1)
    _start_finish_dt = dt.datetime(2016, 2, 1)
    _req_finish_dt = '2016-01-01'


class GeneralFinishUnmatchedBothDTDBTestCase(AbstractFinishUnmatchedDBTestCase):
    _representation = 'general_finish_unmatched_both_dt'
    _start_dt = dt.datetime(2016, 1, 1)
    _start_finish_dt = dt.datetime(2016, 1, 1)
    _req_dt = '2016-03-01'
    _req_finish_dt = '2016-02-01'


class PartnersFinishUnmatchedDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'partners_finish_unmatched_finish_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 3, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, 2050, dt_, is_signed=dt_, end_dt=dt_)
        return self.contract, [
            ('col', self.collateral),
            ('finish_dt', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 3, 1)),
            ('end_dt', dt.datetime(2016, 3, 1)),
        ])
        res.add_message('Дата окончания договора должна быть не раньше даты начала договора/ДС. '
                        'Уточни, пожалуйста, даты и при необходимости заполни форму еще раз.')
        return res


class ContractNotFoundDBTestCase(AbstractCheckDBTestCase):
    _representation = 'not_found_c'

    author = 'noob'

    def get_comments(self):
        return []

    def _get_data(self, session):
        self.contract = ut.Struct(external_id='ahalai-mahalai-autodasha', col0=ut.Struct())
        return self.contract, [
            ('is_signed', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.wont_fix)
        res.add_message('Договор не найден. Уточни, пожалуйста, номер договора и заполни форму еще раз.')
        return res


class CollateralNotFoundDBTestCase(AbstractCheckDBTestCase):
    _representation = 'not_found_col'

    author = 'noob'

    def get_comments(self):
        return []

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', ut.Struct(num='666')),
            ('is_signed', '2016-02-01'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False, transition=IssueTransitions.wont_fix)
        res.add_message('ДС не найдено. Уточни, пожалуйста, номер и дату ДС и заполни форму еще раз.')
        return res


class AbstractCancelSignedContractDBTestCase(AbstractApprovedDBTestCase):
    _attr = None
    _value = None
    _attr_repr = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        parameters = {
            self._attr: self._value,
            'is_booked_dt': dt.datetime(2016, 1, 1),
        }
        parameters.setdefault('is_signed', None)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1), **parameters)

        return self.contract, [
            ('is_cancelled', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_booked_dt', dt.datetime(2016, 1, 1)),
            (self._attr, self._value),
            ('is_cancelled', None),
        ])
        res.add_message('Аннулировать нельзя пока проставлена одна из галок: %s.' % self._attr_repr)
        return res


class CancelSignedContractDBTestCase(AbstractCancelSignedContractDBTestCase):
    _representation = 'c_cancel_signed'
    _attr = 'is_signed'
    _value = dt.datetime(2016, 1, 1)
    _attr_repr = 'Подпись'


class CancelFaxedContractDBTestCase(AbstractCancelSignedContractDBTestCase):
    _representation = 'c_cancel_faxed'
    _attr = 'is_faxed'
    _value = dt.datetime(2016, 1, 1)
    _attr_repr = 'Подпись по факсу'


class CancelPartialSignedContractDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_cancel_part_remove_signed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1),
                                                         is_booked=1, is_booked_dt=dt.datetime(2016, 1, 1))

        return self.contract, [
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_booked_dt', dt.datetime(2016, 1, 1)),
            ('is_booked', 1),
            ('is_signed', dt.datetime(2016, 1, 1)),
            ('is_cancelled', None),
        ])
        res.add_message('Аннулировать нельзя пока проставлена одна из галок: Бронь подписи.')
        return res


class CancelSetSignedContractDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_cancel_set_signed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))

        return self.contract, [
            ('is_cancelled', True),
            ('remove_is_signed', True),
            ('is_faxed', dt.datetime(2016, 1, 1))
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_signed', dt.datetime(2016, 1, 1)),
            ('is_cancelled', None),
            ('is_faxed', None),
        ])
        res.add_message('Аннулировать нельзя пока проставлена одна из галок: Подпись по факсу.')
        return res


class AbstractCancelSignedColDBTestCase(AbstractApprovedDBTestCase):
    _attr = None
    _value = None
    _attr_repr = None

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        parameters = {
            self._attr: self._value,
            'is_booked_dt': dt_,
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt_, **parameters)

        return self.contract, [
            ('col', self.collateral),
            ('is_cancelled', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_booked_dt', dt.datetime(2016, 1, 1)),
            (self._attr, self._value),
            ('is_cancelled', None),
        ])
        res.add_message('Аннулировать нельзя пока проставлена одна из галок: %s.' % self._attr_repr)
        return res


class CancelSignedColDBTestCase(AbstractCancelSignedColDBTestCase):
    _representation = 'col_cancel_signed'
    _attr = 'is_signed'
    _value = dt.datetime(2016, 1, 1)
    _attr_repr = 'Подпись'


class CancelFaxedColDBTestCase(AbstractCancelSignedColDBTestCase):
    _representation = 'col_cancel_faxed'
    _attr = 'is_faxed'
    _value = dt.datetime(2016, 1, 1)
    _attr_repr = 'Подпись по факсу'


class CancelPartialSignedColDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'col_cancel_part_remove_signed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt_, is_signed=dt_, is_booked=1, is_booked_dt=dt_)

        return self.contract, [
            ('col', self.collateral),
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_booked_dt', dt.datetime(2016, 1, 1)),
            ('is_booked', 1),
            ('is_signed', dt.datetime(2016, 1, 1)),
            ('is_cancelled', None),
        ])
        res.add_message('Аннулировать нельзя пока проставлена одна из галок: Бронь подписи.')
        return res


class CancelSetSignedColDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'col_cancel_set_signed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt_, is_signed=dt_)

        return self.contract, [
            ('col', self.collateral),
            ('is_cancelled', True),
            ('remove_is_signed', True),
            ('is_faxed', dt.datetime(2016, 1, 1))
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_signed', dt.datetime(2016, 1, 1)),
            ('is_cancelled', None),
            ('is_faxed', None),
        ])
        res.add_message('Аннулировать нельзя пока проставлена одна из галок: Подпись по факсу.')
        return res


class CancelSetSignedDistrContractDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'distr_c_cancel_set_signed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_distr_contract(session, client, person, dt.datetime(2016, 1, 1))

        return self.contract, [
            ('is_cancelled', True),
            ('remove_is_signed', True),
            ('is_faxed', dt.datetime(2016, 1, 1))
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_signed', dt.datetime(2016, 1, 1)),
            ('is_cancelled', None),
            ('is_faxed', None),
        ])
        res.add_message('Аннулировать нельзя пока проставлена одна из галок: Подпись по факсу.')
        return res


class CancelContractInvoicesDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_cancel_invoices'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)

        order = db_utils.create_order(session, client)
        invoice = db_utils.create_invoice(session, client, [(order, 1)])
        invoice.contract = self.contract

        return self.contract, [
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_signed', dt.datetime(2016, 1, 1)),
            ('is_cancelled', None),
        ])
        res.add_message('Выполнить нельзя, по договору есть выставленные счета.')
        return res


class UnsignContractInvoicesDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_unsign_invoices'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        parameters = {
            'is_signed': None,
            'is_faxed': dt_
        }
        self.contract = db_utils.create_general_contract(session, client, person, dt_, **parameters)

        order = db_utils.create_order(session, client)
        invoice = db_utils.create_invoice(session, client, [(order, 1)])
        invoice.contract = self.contract

        return self.contract, [
            ('remove_is_faxed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_faxed', dt.datetime(2016, 1, 1)),
            ('is_cancelled', None),
        ])
        res.add_message('Выполнить нельзя, по договору есть выставленные счета.')
        return res


class CancelContractInvoicesUnsignedDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_cancel_invoices_unsigned'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, is_signed=None)

        order = db_utils.create_order(session, client)
        invoice = db_utils.create_invoice(session, client, [(order, 1)])
        invoice.contract = self.contract

        return self.contract, [
            ('is_cancelled', True),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_cancelled', None),
        ])
        res.add_message('Выполнить нельзя, по договору есть выставленные счета.')
        return res


class CancelColInvoicesDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'col_cancel_invoices'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt_, is_signed=dt_)

        order = db_utils.create_order(session, client)
        invoice = db_utils.create_invoice(session, client, [(order, 1)])
        invoice.contract = self.contract

        return self.contract, [
            ('col', self.collateral),
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True,
                             delay=False, transition=IssueTransitions.none, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_signed', None),
            ('is_cancelled', True),
        ])
        return res


class CancelContractHiddenInvoicesDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_cancel_hidden_invoices'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)

        order = db_utils.create_order(session, client)
        invoice = db_utils.create_invoice(session, client, [(order, 1)])
        invoice.contract = self.contract
        invoice.hidden = 2

        return self.contract, [
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True, delay=False,
                             transition=IssueTransitions.none, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_signed', None),
            ('is_cancelled', True),
        ])
        return res


class GenColAfterFinishDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'gen_col_after_finish'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract,
                                                  dt_=dt.datetime(2016, 2, 1),
                                                  is_signed=dt.datetime(2016, 2, 1))

        db_utils.add_collateral(self.contract, dt_=dt.datetime(2016, 3, 1),
                                is_signed=dt.datetime(2016, 3, 1),
                                finish_dt=dt.datetime(2016, 3, 1))

        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 3, 15))
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 2, 1)),
            ('is_signed', dt.datetime(2016, 2, 1)),
            ('is_cancelled', None),
        ])
        res.add_message('Дата ДС не может быть позже даты окончания договора.')
        return res


class GenColFinishBeforeDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'gen_col_finish_before'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        db_utils.add_collateral(self.contract,
                                dt_=dt.datetime(2016, 2, 1),
                                is_signed=dt.datetime(2016, 2, 1))

        self.collateral = db_utils.add_collateral(self.contract, 90,
                                                  dt_=dt.datetime(2016, 3, 1),
                                                  is_signed=dt.datetime(2016, 3, 1),
                                                  finish_dt=dt.datetime(2016, 3, 1))

        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 1, 15)),
            ('finish_dt', dt.datetime(2016, 1, 15)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 3, 1)),
            ('is_signed', dt.datetime(2016, 3, 1)),
            ('finish_dt', dt.datetime(2016, 3, 1)),
        ])
        res.add_message('Дата ДС не может быть позже даты окончания договора.')
        return res


class PartnersColAfterFinishDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'partners_col_after_finish'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract,
                                                  dt_=dt.datetime(2016, 2, 1),
                                                  is_signed=dt.datetime(2016, 2, 1))

        db_utils.add_collateral(self.contract, dt_=dt.datetime(2016, 3, 1),
                                is_signed=dt.datetime(2016, 3, 1),
                                end_dt=dt.datetime(2016, 3, 1))

        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 3, 15))
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 2, 1)),
            ('is_signed', dt.datetime(2016, 2, 1)),
            ('is_cancelled', None),
        ])
        res.add_message('Дата ДС не может быть позже даты окончания договора.')
        return res


class PartnersColFinishBeforeDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'partners_col_finish_before'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_partners_contract(session, client, person, dt_)
        db_utils.add_collateral(self.contract,
                                dt_=dt.datetime(2016, 2, 1),
                                is_signed=dt.datetime(2016, 2, 1))

        self.collateral = db_utils.add_collateral(self.contract, 2050,
                                                  dt_=dt.datetime(2016, 3, 1),
                                                  is_signed=dt.datetime(2016, 3, 1),
                                                  end_dt=dt.datetime(2016, 3, 1))

        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 1, 15)),
            ('finish_dt', dt.datetime(2016, 1, 15)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 3, 1)),
            ('is_signed', dt.datetime(2016, 3, 1)),
            ('end_dt', dt.datetime(2016, 3, 1)),
        ])
        res.add_message('Дата ДС не может быть позже даты окончания договора.')
        return res


class ColBeforeCol0DBTestCase(AbstractApprovedDBTestCase):
    _representation = 'col_before_col0'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 2, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract,
                                                  dt_=dt.datetime(2016, 3, 1),
                                                  is_signed=dt.datetime(2016, 3, 1))

        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 1, 15))
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 3, 1)),
            ('is_signed', dt.datetime(2016, 3, 1)),
            ('is_cancelled', None),
        ])
        res.add_message('Дата ДС не может быть раньше даты начала договора.')
        return res


class Col0BeforeColDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'col0_before_col'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        db_utils.add_collateral(self.contract,
                                dt_=dt.datetime(2016, 2, 1),
                                is_signed=dt.datetime(2016, 2, 1))

        return self.contract, [
            ('dt', dt.datetime(2016, 2, 15))
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_signed', dt.datetime(2016, 1, 1)),
            ('is_cancelled', None),
        ])
        res.add_message('Дата ДС не может быть раньше даты начала договора.')
        return res


class StartDTDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'invoice_before_start_dt'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))

        order = db_utils.create_order(session, client)
        invoice = db_utils.create_invoice(session, client, [(order, 1)], person=person)
        invoice.contract = self.contract
        invoice.dt = dt.datetime(2016, 1, 10)

        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False,
                             transition=IssueTransitions.wont_fix,
                             state=[('dt', dt.datetime(2016, 1, 1))])
        res.add_message('Выполнить нельзя, есть счета, выставленные до новой даты.'
                        ' Уточни, пожалуйста, дату и заполни форму еще раз.')

        return res


class CancelContractCollateralsDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_cancel_collaterals'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        db_utils.add_collateral(self.contract, dt_=dt_, is_signed=dt_)

        return self.contract, [
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_signed', dt.datetime(2016, 1, 1)),
            ('is_cancelled', None),
        ])
        res.add_message('К договору есть не аннулированные ДС, аннулировать договор нельзя. '
                        'Предварительно заполни, пожалуйста, форму на аннулирование ДС.')
        return res


class CancelSignedContractCollateralsDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_signed_cancel_collaterals'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        db_utils.add_collateral(self.contract, dt_=dt_, is_signed=dt_)

        return self.contract, [
            ('is_cancelled', True),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, delay=False, transition=IssueTransitions.wont_fix, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_signed', dt.datetime(2016, 1, 1)),
            ('is_cancelled', None),
        ])
        res.add_message('Аннулировать нельзя пока проставлена одна из галок: Подпись.')
        return res


class CancelContractCancelledCollateralsDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'c_cancel_cancelled_collaterals'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        db_utils.add_collateral(self.contract, dt_=dt_, is_signed=None, is_cancelled=dt_)

        return self.contract, [
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True, delay=False,
                             transition=IssueTransitions.none, state=[
            ('dt', dt.datetime(2016, 1, 1)),
            ('is_signed', None),
            ('is_cancelled', dt.datetime.now()),
        ])
        return res


class CancelColCollateralsDBTestCase(AbstractApprovedDBTestCase):
    _representation = 'col_cancel_collaterals'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)
        self.collateral = db_utils.add_collateral(self.contract, dt_=dt_, is_signed=dt_)
        db_utils.add_collateral(self.contract, dt_=dt_ + dt.timedelta(1), is_signed=dt_ + dt.timedelta(1))

        return self.contract, [
            ('col', self.collateral),
            ('is_cancelled', True),
            ('remove_is_signed', True)
        ]

    def get_result(self):
        res = RequiredResult(self.contract, self.collateral, done=True, enqueued=True, delay=False,
                             transition=IssueTransitions.none, state=[
                ('dt', dt.datetime(2016, 1, 1)),
                ('is_signed', None),
                ('is_cancelled', dt.datetime.now()),
            ])
        return res


class GeneralContractNDSCase(AbstractApprovedDBTestCase):
    _representation = 'general_contract_nds_change'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_, nds=0)

        return self.contract, [
            ('nds', 'РФ, стандартный НДС'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False,
                             transition=IssueTransitions.wont_fix)
        res.add_message('НДС можно менять только расходным, партнерским и дистрибуционным договорам. '
                        'Регион фирмы договора - только Россия. '
                        'Доступные значения: РФ, стандартный НДС и НДС 0.'
                        'Для других изменений НДС пересоздай, пожалуйста, тикет, заполнив Другие изменения.')
        return res


class DistrNotRussiaContractNDSCase(AbstractApprovedDBTestCase):
    _representation = 'distr_not_russia_nds_change'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_distr_contract(session, client, person, dt_, nds=0, firm=7)

        return self.contract, [
            ('nds', 'РФ, стандартный НДС'),
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False,
                             transition=IssueTransitions.wont_fix)
        res.add_message('НДС можно менять только расходным, партнерским и дистрибуционным договорам. '
                        'Регион фирмы договора - только Россия. '
                        'Доступные значения: РФ, стандартный НДС и НДС 0.'
                        'Для других изменений НДС пересоздай, пожалуйста, тикет, заполнив Другие изменения.')
        return res


class DistrNoAllowedContractNDSCase(AbstractApprovedDBTestCase):
    _representation = 'distr_not_allowed_nds_change'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_distr_contract(session, client, person, dt_, nds=0, firm=7)

        return self.contract, [
            ('nds', 'Казахстан, стандартный НДС'),
            ('external_id', 'lalkaa')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False,
                             transition=IssueTransitions.wont_fix, state=[('external_id', self.contract.external_id)])
        res.add_message('НДС можно менять только расходным, партнерским и дистрибуционным договорам. '
                        'Регион фирмы договора - только Россия. '
                        'Доступные значения: РФ, стандартный НДС и НДС 0.'
                        'Для других изменений НДС пересоздай, пожалуйста, тикет, заполнив Другие изменения.')
        return res


class UnilateralActsBadContractType(AbstractApprovedDBTestCase):
    _representation = 'unilateral_acts_bad_contract_type'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_distr_contract(session, client, person, dt.datetime(2016, 1, 1))
        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
            ('unilateral_acts', 'Да')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False,
                             transition=IssueTransitions.wont_fix, state=[('dt', self.contract.col0.dt)])
        res.add_message('Односторонние акты доступны только для коммерческих договоров и договоров РСЯ. '
                        'Уточни, пожалуйста, договор и заполни форму еще раз.')
        return res


class PrintFormTypeNotCol(AbstractApprovedDBTestCase):
    _representation = 'print_form_type_not_col'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        return self.contract, [
            ('is_signed', dt.datetime(2016, 5, 1)),
            ('print_form_type', 'Без печатной формы')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False,
                             transition=IssueTransitions.wont_fix, state=[('is_signed', self.contract.col0.is_signed)])
        res.add_message('Не заполнено поле № ДС. Изменять тип печатной формы возможно только в допсоглашениях. '
                        'Уточни, пожалуйста, номер допсоглашения и заполни форму еще раз.')
        return res


class PaymentTypeActsGenContract(AbstractApprovedDBTestCase):
    _representation = 'payment_type_acts_gen_c'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        return self.contract, [
            ('dt', dt.datetime(2016, 2, 1)),
            ('payment_type', 'Акт раз в квартал')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False,
                             transition=IssueTransitions.wont_fix, state=[('dt', self.contract.col0.dt)])
        res.add_message('Период актов можно менять только у партнерских договоров. '
                        'Уточни, пожалуйста, договор и заполни форму еще раз.')
        return res


class PaymentTypeActsGenCol(AbstractApprovedDBTestCase):
    _representation = 'payment_type_acts_gen_col'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))
        self.collateral = db_utils.add_collateral(self.contract, is_signed=dt.datetime(2016, 1, 1))
        return self.contract, [
            ('col', self.collateral),
            ('dt', dt.datetime(2016, 2, 1)),
            ('payment_type', 'Акт раз в квартал')
        ]

    def get_result(self):
        res = RequiredResult(self.contract, delay=False,
                             transition=IssueTransitions.wont_fix, state=[('dt', self.contract.col0.dt)])
        res.add_message('Период актов можно менять только у партнерских договоров. '
                        'Уточни, пожалуйста, договор и заполни форму еще раз.')
        return res


class IncorrectContractForRecalcCase(AbstractApprovedDBTestCase):
    _representation = 'incorrect_contract_for_recalc'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_general_contract(session, client, person, dt_)

        # Проверяем, что никакие параметры не меняем,
        # если договор невалидный для пересчета
        return self.contract, [
            ('need_recalc_partner_reward', 'yes'),
            ('external_id', 'lalka')
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=False,
            transition=IssueTransitions.wont_fix,
            state=[('external_id', self.contract.external_id)]
        )
        res.add_message(
            'Некорректный договор для пересчета партнерского вознаграждения.'
        )
        return res


class IncorrectPeriodForRecalcCase(AbstractApprovedDBTestCase):
    _representation = 'incorrect_period_for_recalc'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_spendable_contract(
            session, client, person, 135, on_dt=dt_
        )

        return self.contract, [
            ('need_recalc_partner_reward', 'yes'),
            ('partner_reward_recalc_period', 'тут какой-то период')
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=False,
            transition=IssueTransitions.wont_fix, state=[]
        )
        res.add_message('Период для пересчета не заполнен или заполнен некорректно.')
        return res


class NotAllowedPeriodForRecalcCase(AbstractApprovedDBTestCase):
    _representation = 'now_allowed_period_for_recalc'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_spendable_contract(
            session, client, person, 135, on_dt=dt_
        )

        current_datetime = dt.datetime.now()
        begin_dt = ut.month_first_day(current_datetime)
        end_dt = ut.month_last_day(current_datetime)

        period = '%s - %s' % (begin_dt.date(), end_dt.date())

        return self.contract, [
            ('need_recalc_partner_reward', 'yes'),
            ('partner_reward_recalc_period', period)
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=False,
            transition=IssueTransitions.wont_fix, state=[]
        )
        res.add_message('Указанный период невозможно пересчитать. Причины могут быть разными:')
        res.add_message('1. Месяц еще не наступил - будущие периоды пересчитать не можем, так как в них вознаграждения не может быть.')
        res.add_message('2. Месяц является текущим - пересчитать не можем, так как за текущий месяц вознаграждения не может быть.')
        res.add_message('3. Месяц является предыдущим, но автоматическая генерация по нему еще не отработала. '
                        'Скорее всего на календаре 1 или 2 число месяца, который следует за указанным. '
                        'В таком случае заголовки пересчитаются самостоятельно чуть позже.')

        return res


class NotAllowedPeriodForRecalc2Case(AbstractApprovedDBTestCase):
    _representation = 'now_allowed_period_for_recalc_2'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_spendable_contract(
            session, client, person, 135, on_dt=dt_
        )

        current_datetime = dt.datetime.now()
        begin_dt = ut.month_first_day(ut.add_months_to_date(current_datetime, 1))
        end_dt = ut.month_last_day(ut.add_months_to_date(current_datetime, 1))

        period = '%s - %s' % (begin_dt.date(), end_dt.date())

        return self.contract, [
            ('need_recalc_partner_reward', 'yes'),
            ('partner_reward_recalc_period', period)
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=False,
            transition=IssueTransitions.wont_fix, state=[]
        )
        res.add_message('Указанный период невозможно пересчитать. Причины могут быть разными:')
        res.add_message('1. Месяц еще не наступил - будущие периоды пересчитать не можем, так как в них вознаграждения не может быть.')
        res.add_message('2. Месяц является текущим - пересчитать не можем, так как за текущий месяц вознаграждения не может быть.')
        res.add_message('3. Месяц является предыдущим, но автоматическая генерация по нему еще не отработала. '
                        'Скорее всего на календаре 1 или 2 число месяца, который следует за указанным. '
                        'В таком случае заголовки пересчитаются самостоятельно чуть позже.')

        return res


class NotAllowedPeriodForRecalc3Case(AbstractApprovedDBTestCase):
    _representation = 'now_allowed_period_for_recalc_3'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        dt_ = dt.datetime(2016, 1, 1)
        self.contract = db_utils.create_spendable_contract(
            session, client, person, 135, on_dt=dt_
        )

        current_datetime = dt.datetime.now()
        begin_dt = ut.month_first_day(ut.add_months_to_date(current_datetime, -1))
        end_dt = ut.month_last_day(ut.add_months_to_date(current_datetime, -1))

        db_utils.create_nirvana_mnclose_sync_row(
            session, 'export_partner_act_data', 'new_openable',
            ut.month_first_day(current_datetime)
        )

        period = '%s - %s' % (begin_dt.date(), end_dt.date())

        return self.contract, [
            ('need_recalc_partner_reward', 'yes'),
            ('partner_reward_recalc_period', period)
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=False,
            transition=IssueTransitions.wont_fix, state=[]
        )
        res.add_message('Указанный период невозможно пересчитать. Причины могут быть разными:')
        res.add_message('1. Месяц еще не наступил - будущие периоды пересчитать не можем, так как в них вознаграждения не может быть.')
        res.add_message('2. Месяц является текущим - пересчитать не можем, так как за текущий месяц вознаграждения не может быть.')
        res.add_message('3. Месяц является предыдущим, но автоматическая генерация по нему еще не отработала. '
                        'Скорее всего на календаре 1 или 2 число месяца, который следует за указанным. '
                        'В таком случае заголовки пересчитаются самостоятельно чуть позже.')

        return res


class OfferConfirmationNotCol0Case(AbstractApprovedDBTestCase):
    _representation = 'offer_confirmation_not_col0'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person)
        self.collateral = db_utils.add_collateral(self.contract)

        return self.contract, [
            ('col', self.collateral),
            ('offer_confirmation_type', 'Не требуется')
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=False,
            transition=IssueTransitions.wont_fix, state=[]
        )
        res.add_message(
            'Способ подтверждения оферты можно менять только в начальных условиях договора. '
            'Пожалуйста, заполни форму еще раз, не указывая № ДС.'
        )

        return res


class OfferConfirmationCase(AbstractApprovedDBTestCase):
    _representation = 'offer_confirmation_col0'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person)

        return self.contract, [
            ('offer_confirmation_type', 'Не требуется')
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=False,
            transition=IssueTransitions.none,
            done=True,
            enqueued=True,
            state=[('offer_confirmation_type', 'no')]
        )

        return res


class OfferConfirmationNotGeneralCase(AbstractApprovedDBTestCase):
    _representation = 'offer_confirmation_not_general'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_spendable_contract(session, client, person, 135)

        return self.contract, [
            ('offer_confirmation_type', 'Не требуется')
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=False,
            transition=IssueTransitions.wont_fix, state=[]
        )
        res.add_message(
            'Способ подтверждения оферты можно менять только у коммерческих договоров. '
            'Уточни, пожалуйста, договор и заполни форму еще раз.'
        )

        return res


class PaymentTermNotAllowedCase(AbstractApprovedDBTestCase):
    _representation = 'payment_term_not_allowed'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person)

        return self.contract, [
            ('payment_term', '666')
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=False,
            transition=IssueTransitions.wont_fix, state=[]
        )
        res.add_message(
            'Указано недопустимое значение параметра "Срок оплаты счетов": 666. '
            'Уточни, пожалуйста, срок оплаты счетов и заполни форму еще раз.'
        )

        return res


class PaymentTermNotGeneralCase(AbstractApprovedDBTestCase):
    _representation = 'payment_term_not_general'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_spendable_contract(session, client, person, 135)

        return self.contract, [
            ('payment_term', '15')
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=False,
            transition=IssueTransitions.wont_fix, state=[]
        )
        res.add_message(
            'Срок оплаты счетов можно менять только у коммерческих договоров. '
            'Уточни, пожалуйста, договор и заполни форму еще раз.'
        )

        return res


class PaymentTermCol0Case(AbstractApprovedDBTestCase):
    _representation = 'payment_term_col0'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person)

        return self.contract, [
            ('payment_term', '15')
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=False,
            done=True,
            enqueued=True,
            transition=IssueTransitions.none, state=[('payment_term', 15)]
        )

        return res


class PaymentTermInvalidColTypeCase(AbstractApprovedDBTestCase):
    _representation = 'payment_term_invalid_col_type'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person)
        self.collateral = db_utils.add_collateral(self.contract, 90)

        return self.contract, [
            ('col', self.collateral),
            ('payment_term', '15')
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            delay=False,
            transition=IssueTransitions.wont_fix, state=[]
        )
        res.add_message(
            'Недопустимый тип ДС для параметра "Срок оплаты счетов": расторжение договора. '
            'Уточни, пожалуйста, ДС и заполни форму еще раз.'
        )

        return res


class PaymentTermValidColTypeCase(AbstractApprovedDBTestCase):
    _representation = 'payment_term_valid_col_type'

    def _get_data(self, session):
        client, person = db_utils.create_client_person(session)
        self.contract = db_utils.create_general_contract(session, client, person)
        self.collateral = db_utils.add_collateral(self.contract, 1005)

        return self.contract, [
            ('col', self.collateral),
            ('payment_term', '15')
        ]

    def get_result(self):
        res = RequiredResult(
            self.contract,
            self.collateral,
            delay=False,
            done=True,
            enqueued=True,
            transition=IssueTransitions.none, state=[('payment_term', 15)]
        )

        return res
