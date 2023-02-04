# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import collections
import pytest
import datetime

from autodasha.core.api.tracker import IssueTransitions
from autodasha.db import mapper as a_mapper
from autodasha.solver_cl import AddSetMarkup
from autodasha.utils.solver_utils import *

import balance.mapper as mapper

from tests.autodasha_tests.common.db_utils import *
from tests.autodasha_tests.functional_tests import case_utils


PRODUCT_ID_PATTERN = r'(\d+)'
MARKUP_PATTERN = r'([\w+-]+)'
DESCR_PATTERN = r'(.+)'
PCT_PATTERN = r'([+-]?\d*[\.,]?\d+?)%?'

WAITING_THRESHOLD = 90

MODES = {
    'Добавить новую наценку(и)': 'add',
    'Проставить наценку(и)': 'set'
}

CHECK_COMMENTS = {
    'existing_markup_check':
        'Наценка ',
    'existing_product_markup_check':
        'В продукте '
}

COMMENTS = {
    'already_solved':
        'Эта задача уже была выполнена. Направьте новый запрос через '
        '((https://wiki.yandex-team.ru/otdelsoprovozhdeniya/dokument/avtomatformi/ формы)). '
        'Если не найдёте подходящую, заполните, пожалуйста, общую форму.',
    'invalid_form':
        'Задача создана некорректно.\n'
        '{}'
        'Пожалуйста, создай новую задачу через форму.',
    'invalid_mode':
        'Указан некорректный режим (должен быть один из: {}).\n',
    'invalid_products':
        'Список продуктов не соответствует необходимому формату.\n',
    'product_not_found':
        'Продукт {} не найден.\n',
    'markup_not_found':
        'Наценка {} не найдена.\n',
    'duplicate_markups_found':
        'Найдены несколько наценок с кодом {}.',
    'different_description_markup':
        'Наценка {} уже существует с описанием ("{}"), отличным от указанного ("{}").\n',
    'duplicate_product_markups_found':
        'В продукте {} проставлены несколько наценок с кодом {}.',
    'different_pct_product_markup':
        'В продукте {} уже проставлена наценка {} с процентом ({}), отличным от указанного ({}).\n',
    'existing_product_markup_check':
        CHECK_COMMENTS['existing_product_markup_check'],
    'existing_product_markup':
        CHECK_COMMENTS['existing_product_markup_check'] +
        '{} уже проставлена наценка {} с указанным процентом {}.\n',
    'no_markups':
        'Список наценок пуст.\n',
    'all_markups_invalid':
        'Список наценок не соответствует необходимому формату.\n',
    'ambiguous_markups':
        'Список наценок неоднозначен.\n',
    'existing_markup_check':
        CHECK_COMMENTS['existing_markup_check'],
    'existing_markup':
        CHECK_COMMENTS['existing_markup_check'] +
        '{} "{}" уже существует.\n',
    'new_markup':
        'Наценка {} ("{}") создана.\n',
    'new_product_markup':
        'В продукт {} проставлена наценка {} с процентом {}.\n',
    'invalid_contents':
        'Следующие строки не соответствуют необходимому формату и не были обработаны:\n'
        '{}.\n'
        'Пожалуйста, исправь их и создай новую задачу через форму.',
    'cascade_update_failed':
        'Данные в BALANCE не обновились.'
}


class AbstractMockTestCase(case_utils.AbstractMockTestCase):
    _default_lines = {
        'summary': 'Добавить / проставить наценки',
        'mode': 'Что требуется сделать: %s\r',
        'products': 'Номер продукта: %s\r',
        'markups': 'Добавление наценки: %s\r',
        'product_markups': 'Проставление наценки: %s\r',
        'comment': 'Комментарий: -'
    }


class AbstractParseFailTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class AbstractParseSuccessTestCase(AbstractMockTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta


class InvalidModeCase(AbstractParseFailTestCase):
    _representation = 'invalid_mode'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(mode='Сделайте что-нибудь'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invalid_mode'].format(', '.join(MODES.keys())))


class ValidModeAddCase(AbstractParseSuccessTestCase):
    _representation = 'valid_mode_add'

    def get_data(self, mock_manager):
        self.mode = 'Добавить новую наценку(и)'
        lines = [
            self._get_default_line(mode=self.mode),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return MODES[self.mode]


class ValidModeSetCase(AbstractParseSuccessTestCase):
    _representation = 'valid_mode_set'

    def get_data(self, mock_manager):
        self.mode = 'Проставить наценку(и)'
        lines = [
            self._get_default_line(mode=self.mode),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return MODES[self.mode]


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in (InvalidModeCase,)],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_mode_fail_cases(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = AddSetMarkup(mock_queue_object, issue)
    with pytest.raises(Exception) as e:
        solver.get_mode()
    assert required_res == e.value.message


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in (ValidModeAddCase, ValidModeSetCase,)],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_mode_success_cases(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = AddSetMarkup(mock_queue_object, issue)
    res = solver.get_mode()
    assert required_res == res


class AddNoMarkupsCase(AbstractParseFailTestCase):
    _representation = 'add_no_markups'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(mode='Добавить новую наценку(и)'),
            self._get_default_line(markups=''),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['all_markups_invalid'])


class AddAllMarkupsInvalidCase(AbstractParseFailTestCase):
    _representation = 'add_all_markups_invalid'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1
        test_2
        """
        lines = [
            self._get_default_line(mode='Добавить новую наценку(и)'),
            self._get_default_line(markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['all_markups_invalid'])


class AddAllMarkupsAmbiguousCase(AbstractParseFailTestCase):
    _representation = 'add_all_markups_ambiguous'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1 Test markup #1
        test_1 Test markup #2
        """
        lines = [
            self._get_default_line(mode='Добавить новую наценку(и)'),
            self._get_default_line(markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['ambiguous_markups'])


class AddSomeMarkupsAmbiguousCase(AbstractParseFailTestCase):
    _representation = 'add_some_markups_ambiguous'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1 Test markup #1
        test_1 Test markup #2
        test_2 Test markup #2
        """
        lines = [
            self._get_default_line(mode='Добавить новую наценку(и)'),
            self._get_default_line(markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['ambiguous_markups'])


class SetNoMarkupsCase(AbstractParseFailTestCase):
    _representation = 'set_no_markups'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(mode='Проставить наценку(и)'),
            self._get_default_line(products='500000'),
            self._get_default_line(product_markups=''),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['all_markups_invalid'])


class SetAllMarkupsInvalidCase(AbstractParseFailTestCase):
    _representation = 'set_all_markups_invalid'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1
        test_2
        """
        lines = [
            self._get_default_line(mode='Проставить наценку(и)'),
            self._get_default_line(products='500000'),
            self._get_default_line(product_markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['all_markups_invalid'])


class SetAllMarkupsAmbiguousCase(AbstractParseFailTestCase):
    _representation = 'set_all_markups_ambiguous'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1 10
        test_1 20
        """
        lines = [
            self._get_default_line(mode='Проставить наценку(и)'),
            self._get_default_line(products='500000'),
            self._get_default_line(product_markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['ambiguous_markups'])


class SetSomeMarkupsAmbiguousCase(AbstractParseFailTestCase):
    _representation = 'set_some_markups_ambiguous'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1 10
        test_1 20
        test_2 20
        """
        lines = [
            self._get_default_line(mode='Проставить наценку(и)'),
            self._get_default_line(products='500000'),
            self._get_default_line(product_markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['ambiguous_markups'])


class AddValidMarkupsCase(AbstractParseSuccessTestCase):
    _representation = 'add_valid_markups'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1 Test markup #1
        test_2 Test markup #2
        """
        lines = [
            self._get_default_line(mode='Добавить новую наценку(и)'),
            self._get_default_line(markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'add', None, [('test_1', 'Test markup #1'), ('test_2', 'Test markup #2')], []


class AddValidMarkupsDuplicateCase(AbstractParseSuccessTestCase):
    _representation = 'add_valid_markups_duplicate'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1 Test markup #1
        test_2 Test markup #2
        test_2 Test markup #2
        """
        lines = [
            self._get_default_line(mode='Добавить новую наценку(и)'),
            self._get_default_line(markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'add', None, [('test_1', 'Test markup #1'), ('test_2', 'Test markup #2')], []


class AddValidMarkupsNoiseCase(AbstractParseSuccessTestCase):
    _representation = 'add_valid_markups_noise'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1 Test markup #1
        test_2 Test markup #2
        test_3Testmarkup#3
        """
        lines = [
            self._get_default_line(mode='Добавить новую наценку(и)'),
            self._get_default_line(markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'add', None, [('test_1', 'Test markup #1'), ('test_2', 'Test markup #2')], ['test_3Testmarkup#3']


class AddValidMarkupsNoiseDuplicateCase(AbstractParseSuccessTestCase):
    _representation = 'add_valid_markups_noise_duplicate'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1 Test markup #1
        test_2 Test markup #2
        test_2 Test markup #2
        test_3Testmarkup#3
        test_3Testmarkup#3
        """
        lines = [
            self._get_default_line(mode='Добавить новую наценку(и)'),
            self._get_default_line(markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'add', None, [('test_1', 'Test markup #1'), ('test_2', 'Test markup #2')], ['test_3Testmarkup#3']


class SetValidMarkupsCase(AbstractParseSuccessTestCase):
    _representation = 'set_valid_markups'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1 10
        test_2 20%
        test_3 30.5
        test_4 -40,8%
        """
        lines = [
            self._get_default_line(mode='Проставить наценку(и)'),
            self._get_default_line(products='500000'),
            self._get_default_line(product_markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'set', [500000], [('test_1', D(10)), ('test_2', D(20)), ('test_3', D(30.5)), ('test_4', D(-40.8))], []


class SetValidMarkupsDuplicateCase(AbstractParseSuccessTestCase):
    _representation = 'set_valid_markups_duplicate'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1 10
        test_1 +10
        test_1 10.0
        test_2 +20.0
        test_2 20%
        test_2 20
        test_3 30.5
        test_4 -40,8%
        """
        lines = [
            self._get_default_line(mode='Проставить наценку(и)'),
            self._get_default_line(products='500000'),
            self._get_default_line(product_markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'set', [500000], [('test_1', D(10)), ('test_2', D(20)), ('test_3', D(30.5)), ('test_4', D(-40.8))], []


class SetValidMarkupsNoiseCase(AbstractParseSuccessTestCase):
    _representation = 'set_valid_markups_noise'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1 10
        test_1 +10
        test_1 10.0
        test_2 +20.0
        test_2 20%
        test_2 20
        test_3+30%
        test_3 30.5
        test_4 -40,8%
        """
        lines = [
            self._get_default_line(mode='Проставить наценку(и)'),
            self._get_default_line(products='500000'),
            self._get_default_line(product_markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'set', [500000], [('test_1', D(10)), ('test_2', D(20)), ('test_3', D(30.5)), ('test_4', D(-40.8))],\
               ['test_3+30%']


class SetValidMarkupsNoiseDuplicateCase(AbstractParseSuccessTestCase):
    _representation = 'set_valid_markups_noise_duplicate'

    def get_data(self, mock_manager):
        markups = \
        """
        test_1 10
        test_1 +10
        test_1 10.0
        test_2 +20.0
        test_2 20%
        test_2 20
        test_3+30%
        test_3+30%
        test_3 30.5
        test_4 -40,8%
        """
        lines = [
            self._get_default_line(mode='Проставить наценку(и)'),
            self._get_default_line(products='500000'),
            self._get_default_line(product_markups=markups),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return 'set', [500000], [('test_1', D(10)), ('test_2', D(20)), ('test_3', D(30.5)), ('test_4', D(-40.8))],\
               ['test_3+30%']


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in (AddNoMarkupsCase, AddAllMarkupsInvalidCase, AddAllMarkupsAmbiguousCase,
                                              AddSomeMarkupsAmbiguousCase, SetNoMarkupsCase, SetAllMarkupsInvalidCase,
                                              SetAllMarkupsAmbiguousCase, SetSomeMarkupsAmbiguousCase,)],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_contents_fail_cases(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = AddSetMarkup(mock_queue_object, issue)
    with pytest.raises(Exception) as e:
        solver.parse_issue()
    assert required_res == e.value.message


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in (AddValidMarkupsCase, AddValidMarkupsDuplicateCase,
                                              AddValidMarkupsNoiseCase, AddValidMarkupsNoiseDuplicateCase,
                                              SetValidMarkupsCase, SetValidMarkupsDuplicateCase,
                                              SetValidMarkupsNoiseCase, SetValidMarkupsNoiseDuplicateCase,)],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_contents_success_cases(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = AddSetMarkup(mock_queue_object, issue)
    res = solver.parse_issue()
    assert required_res == res


class SetNoProductsCase(AbstractParseFailTestCase):
    _representation = 'set_no_products'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(mode='Проставить наценку(и)'),
            self._get_default_line(products=''),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invalid_products'])


class SetNoProductsCase(AbstractParseFailTestCase):
    _representation = 'set_no_products'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(mode='Проставить наценку(и)'),
            self._get_default_line(products=''),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invalid_products'])


class SetInvalidProductsCase(AbstractParseFailTestCase):
    _representation = 'set_invalid_products'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(mode='Проставить наценку(и)'),
            self._get_default_line(products='product1, product2'),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return COMMENTS['invalid_form'].format(COMMENTS['invalid_products'])


class SetValidProductsCase(AbstractParseSuccessTestCase):
    _representation = 'set_valid_products'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(products='500000, 500001 ; 500002 500003'),
            self._get_default_line(product_markups=''),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return [500000, 500001, 500002, 500003]


class SetValidProductsDuplicateCase(AbstractParseSuccessTestCase):
    _representation = 'set_valid_products_duplicate'

    def get_data(self, mock_manager):
        lines = [
            self._get_default_line(products='500000, 500001 ; 500002 500003 , 500001 ;500002 500003,500000'),
            self._get_default_line(product_markups=''),
            self._get_default_line('comment')
        ]
        return self._get_default_line('summary'), lines

    def get_result(self):
        return [500000, 500001, 500002, 500003]


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in (SetNoProductsCase, SetInvalidProductsCase,)],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_product_ids_fail_cases(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = AddSetMarkup(mock_queue_object, issue)
    with pytest.raises(Exception) as e:
        solver.parse_product_ids()
    assert required_res == e.value.message


@pytest.mark.parametrize('mock_issue_data',
                         [case() for case in (SetValidProductsCase, SetValidProductsDuplicateCase,)],
                         ids=lambda case: str(case),
                         indirect=['mock_issue_data'])
def test_product_ids_success_cases(mock_queue_object, mock_issue_data):
    issue, case = mock_issue_data
    required_res = case.get_result()
    solver = AddSetMarkup(mock_queue_object, issue)
    res = solver.parse_product_ids()
    assert required_res == res


class RequiredResult(case_utils.RequiredResult):

    def __init__(self, **kwargs):
        self.delay = False
        self.commit = True
        super(RequiredResult, self).__init__(**kwargs)


class AbstractDBAddTestCase(case_utils.AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    _summary = 'Добавить / проставить наценки'
    _description = \
"""
Что требуется сделать: Добавить новую наценку(и)
Добавление наценки: {markups}
Комментарий: -
""".strip()

    def __init__(self):
        super(AbstractDBAddTestCase, self).__init__()

    def setup_config(self, session, config):
        self.config = config

    def get_markup(self, obj):
        return obj.code, obj.description


class AddNewMarkupsCase(AbstractDBAddTestCase):
    _representation = 'add_new_markups'

    def _get_data(self, session):
        markups = [
            ('test_1', 'Test markup #1'),
            ('test_2', 'Test markup #2')
        ]
        return {
            'markups': '\n'.join('{} {}'.format(code, description) for code, description in markups)
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none)
        res.commit = True
        res.delay = True
        return res


class AddMixedMarkupsCase(AbstractDBAddTestCase):
    _representation = 'add_mixed_markups'

    def _get_data(self, meta_session):
        self.markups = [
            ('test_1', 'Test markup #1'),
            ('test_2', 'Test markup #2')
        ]
        create_markup(meta_session, code=self.markups[0][0], description=self.markups[0][1])
        return {
            'markups': '\n'.join('{} {}'.format(code, description) for code, description in self.markups)
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none)
        res.add_message(COMMENTS['existing_markup'].format(*self.markups[0]))
        res.commit = True
        res.delay = True
        return res


class AddNewMarkupsContinuationCase(AbstractDBAddTestCase):
    _representation = 'add_new_markups_continuation'

    def _get_data(self, meta_session):
        # create markups and move back to the past so we immediately get into state after insert
        self.issue_dt = datetime.datetime.now() - datetime.timedelta(minutes=1)
        self.markups = [
            ('test_1', 'Test markup #1'),
            ('test_2', 'Test markup #2')
        ]
        for code, description in self.markups:
            create_markup(meta_session, code=code, description=description)
        return {
            'markups': '\n'.join('{} {}'.format(code, description) for code, description in self.markups)
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_message(''.join(COMMENTS['new_markup'].format(code, description) for code, description in self.markups))
        res.commit = True
        res.delay = False
        return res


class AddMixedMarkupsContinuationCase(AbstractDBAddTestCase):
    _representation = 'add_mixed_markups_continuation'

    def _get_data(self, meta_session):
        # create markups and move back to the past so we immediately get into state after insert
        self.issue_dt = datetime.datetime.now() - datetime.timedelta(minutes=1)
        self.markups = [
            ('test_1', 'Test markup #1'),
            ('test_2', 'Test markup #2')
        ]
        for code, description in self.markups:
            create_markup(meta_session, code=code, description=description)
        return {
            'markups': '\n'.join('{} {}'.format(code, description) for code, description in self.markups)
        }

    def get_comments(self):
        return [
            ('autodasha', COMMENTS['existing_markup'].format(*self.markups[1]))
        ]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_message(COMMENTS['new_markup'].format(*self.markups[0]))
        res.commit = True
        res.delay = False
        return res


class AddDuplicateMarkupCase(AbstractDBAddTestCase):
    _representation = 'add_duplicate_markup'

    def _get_data(self, meta_session):
        self.markups = [
            ('test_1', 'Test markup #1'),
            ('test_1', 'Test markup #1'),
            ('test_2', 'Test markup #2')
        ]
        for code, description in self.markups:
            create_markup(meta_session, code=code, description=description)
        return {
            'markups': '\n'.join('{} {}'.format(code, description) for code, description in set(self.markups))
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.opened, assignee=self.config.responsible_manager)
        res.add_message(COMMENTS['duplicate_markups_found'].format('test_1'))
        res.commit = True
        res.delay = False
        return res


class AddDiffDescrMarkupCase(AbstractDBAddTestCase):
    _representation = 'add_different_descr_markup'

    def _get_data(self, meta_session):
        self.markups = [
            ('test_1', 'Test markup #1'),
            ('test_3', 'Test markup #3')
        ]
        for code, description in self.markups:
            create_markup(meta_session, code=code, description=description)
        return {
            'markups':
                """
                test_1 Test markup #2
                test_2 Test markup #2
                test_3 Test markup #3
                """
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_message(COMMENTS['invalid_form'].format(
            COMMENTS['different_description_markup'].format('test_1', 'Test markup #1', 'Test markup #2')))
        res.commit = True
        res.delay = False
        return res


class AbstractDBSetTestCase(case_utils.AbstractDBTestCase):
    _cases = []
    __metaclass__ = case_utils.TestCaseMeta

    _summary = 'Добавить / проставить наценки'
    _description = \
"""
Что требуется сделать: Проставить наценку(и)
Номер продукта: {products}
Проставление наценки: {product_markups}
Комментарий: -
""".strip()

    def __init__(self):
        super(AbstractDBSetTestCase, self).__init__()

    def setup_config(self, session, config):
        self.config = config

    def get_product_markup(self, obj):
        pct = int(obj.pct) if int(obj.pct) == obj.pct else float(obj.pct)
        return str(obj.product_id), str(obj.markup.code), str(pct)


class SetNewMarkupsCase(AbstractDBSetTestCase):
    _representation = 'set_new_markups'

    def _get_data(self, meta_session):
        markups = [
            ('test_1', 10),
            ('test_2', 20)
        ]
        products = [500000, 500001]
        for code, pct in markups:
            create_markup(meta_session, code=code)
        for product in products:
            create_product(meta_session, id=product)
        return {
            'products': ', '.join(str(p) for p in products),
            'product_markups': '\n'.join('{} {}'.format(markup, pct) for markup, pct in markups)
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none)
        res.commit = True
        res.delay = True
        return res


class SetMixedMarkupsCase(AbstractDBSetTestCase):
    _representation = 'set_mixed_markups'

    def _get_data(self, meta_session):
        markups = [
            ('test_1', 10),
            ('test_2', 20)
        ]
        products = [500000, 500001]
        self.markups = []
        for code, pct in markups:
            self.markups.append(create_markup(meta_session, code=code))
        self.products = []
        for product in products:
            self.products.append(create_product(meta_session, id=product))
        self.product_markups = []
        self.product_markups.append(create_product_markup(meta_session, datetime.datetime.now(), self.products[0],
                                                          self.markups[0], pct=markups[0][1]))
        return {
            'products': ', '.join(str(p) for p in products),
            'product_markups': '\n'.join('{} {}'.format(markup, pct) for markup, pct in markups)
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.none)
        res.add_message(COMMENTS['existing_product_markup'].format(*self.get_product_markup(self.product_markups[0])))
        res.commit = True
        res.delay = True
        return res


class SetNewMarkupsContinuationCase(AbstractDBSetTestCase):
    _representation = 'set_new_markups_continuation'

    def _get_data(self, meta_session):
        # create product markups and move back to the past so we immediately get into state after insert
        self.issue_dt = datetime.datetime.now() - datetime.timedelta(minutes=5)
        markups = [
            ('test_1', 10),
            ('test_2', 20)
        ]
        products = [500000, 500001]
        self.markups = [create_markup(meta_session, code=code) for code, pct in markups]
        self.products = [create_product(meta_session, id=product) for product in products]
        self.product_markups = [create_product_markup(meta_session, datetime.datetime.now(), product, markup, pct=pct)
                                for product in self.products for markup, (code, pct) in zip(self.markups, markups)]
        return {
            'products': ', '.join(str(p) for p in products),
            'product_markups': '\n'.join('{} {}'.format(markup, pct) for markup, pct in markups)
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_message(''.join(COMMENTS['new_product_markup'].format(*self.get_product_markup(product_markup))
                                for product_markup in self.product_markups))
        res.commit = True
        res.delay = False
        return res


class SetMixedMarkupsContinuationCase(AbstractDBSetTestCase):
    _representation = 'set_mixed_markups_continuation'

    def _get_data(self, meta_session):
        # create markups and move back to the past so we immediately get into state after insert
        self.issue_dt = datetime.datetime.now() - datetime.timedelta(minutes=5)
        markups = [
            ('test_1', 10),
            ('test_2', 20)
        ]
        products = [500000, 500001]
        self.markups = [create_markup(meta_session, code=code) for code, pct in markups]
        self.products = [create_product(meta_session, id=product) for product in products]
        self.product_markups = [create_product_markup(meta_session, datetime.datetime.now(), product, markup, pct=pct)
                                for product in self.products for markup, (code, pct) in zip(self.markups, markups)]
        return {
            'products': ', '.join(str(p) for p in products),
            'product_markups': '\n'.join('{} {}'.format(markup, pct) for markup, pct in markups)
        }

    def get_comments(self):
        return [
            ('autodasha', ''.join(COMMENTS['existing_product_markup'].format(*self.get_product_markup(product_markup))
                                  for product_markup in self.product_markups[1:]))
        ]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_message(''.join(COMMENTS['new_product_markup'].format(*self.get_product_markup(product_markup))
                                for product_markup in self.product_markups[:1]))
        res.commit = True
        res.delay = False
        return res


class SetNewMarkupsInvalidContinuationCase(AbstractDBSetTestCase):
    _representation = 'set_new_markups_invalid_continuation'

    def _get_data(self, meta_session):
        # create product markups and move back to the past so we immediately get into state after insert
        self.issue_dt = datetime.datetime.now() - datetime.timedelta(minutes=5)
        markups = [
            ('test_1', 10),
            ('test_2', 20)
        ]
        self.invalid_markups = [
            'test_3+30', 'test_4'
        ]
        products = [500000, 500001]
        self.markups = [create_markup(meta_session, code=code) for code, pct in markups]
        self.products = [create_product(meta_session, id=product) for product in products]
        self.product_markups = [create_product_markup(meta_session, datetime.datetime.now(), product, markup, pct=pct)
                                for product in self.products for markup, (code, pct) in zip(self.markups, markups)]
        return {
            'products': ', '.join(str(p) for p in products),
            'product_markups': '\n'.join('{} {}'.format(markup, pct) for markup, pct in markups)
                               + '\n' + '\n'.join(self.invalid_markups)
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_message(''.join(COMMENTS['new_product_markup'].format(*self.get_product_markup(product_markup))
                                for product_markup in self.product_markups))
        res.add_message(COMMENTS['invalid_contents'].format('\n'.join(self.invalid_markups)))
        res.commit = True
        res.delay = False
        return res


class SetMixedMarkupsInvalidContinuationCase(AbstractDBSetTestCase):
    _representation = 'set_mixed_markups_invalid_continuation'

    def _get_data(self, meta_session):
        # create markups and move back to the past so we immediately get into state after insert
        self.issue_dt = datetime.datetime.now() - datetime.timedelta(minutes=5)
        markups = [
            ('test_1', 10),
            ('test_2', 20)
        ]
        self.invalid_markups = [
            'test_3+30', 'test_4'
        ]
        products = [500000, 500001]
        self.markups = [create_markup(meta_session, code=code) for code, pct in markups]
        self.products = [create_product(meta_session, id=product) for product in products]
        self.product_markups = [create_product_markup(meta_session, datetime.datetime.now(), product, markup, pct=pct)
                                for product in self.products for markup, (code, pct) in zip(self.markups, markups)]
        return {
            'products': ', '.join(str(p) for p in products),
            'product_markups': '\n'.join('{} {}'.format(markup, pct) for markup, pct in markups)
                               + '\n' + '\n'.join(self.invalid_markups)
        }

    def get_comments(self):
        return [
            ('autodasha', ''.join(COMMENTS['existing_product_markup'].format(*self.get_product_markup(product_markup))
                                  for product_markup in self.product_markups[1:]))
        ]

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.fixed)
        res.add_message(''.join(COMMENTS['new_product_markup'].format(*self.get_product_markup(product_markup))
                                for product_markup in self.product_markups[:1]))
        res.add_message(COMMENTS['invalid_contents'].format('\n'.join(self.invalid_markups)))
        res.commit = True
        res.delay = False
        return res


class SetProductNotFoundCase(AbstractDBSetTestCase):
    _representation = 'set_product_not_found'

    def _get_data(self, meta_session):
        markups = [
            ('test_1', 10),
            ('test_2', 20)
        ]
        products = [500000, 500001]
        for code, pct in markups:
            create_markup(meta_session, code=code)
        for product in products:
            create_product(meta_session, id=product)
        products.append(500002)
        return {
            'products': ', '.join(str(p) for p in products),
            'product_markups': '\n'.join('{} {}'.format(markup, pct) for markup, pct in markups)
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_message(COMMENTS['invalid_form'].format(COMMENTS['product_not_found'].format(500002)))
        res.commit = True
        res.delay = False
        return res


class SetMarkupNotFoundCase(AbstractDBSetTestCase):
    _representation = 'set_markup_not_found'

    def _get_data(self, meta_session):
        markups = [
            ('test_1', 10),
            ('test_2', 20)
        ]
        products = [500000, 500001]
        for code, pct in markups:
            create_markup(meta_session, code=code)
        for product in products:
            create_product(meta_session, id=product)
        return {
            'products': ', '.join(str(p) for p in products),
            'product_markups': '\n'.join('{} {}'.format(markup, pct) for markup, pct in markups)
                               + '\ntest_3 30'
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_message(COMMENTS['invalid_form'].format(COMMENTS['markup_not_found'].format('test_3')))
        res.commit = True
        res.delay = False
        return res


class SetDuplicateMarkupCase(AbstractDBSetTestCase):
    _representation = 'set_duplicate_markup'

    def _get_data(self, meta_session):
        markups = [
            ('test_1', 10),
            ('test_2', 20)
        ]
        products = [500000, 500001]
        for code, pct in markups:
            create_markup(meta_session, code=code)
        create_markup(meta_session, code='test_1')
        for product in products:
            create_product(meta_session, id=product)
        return {
            'products': ', '.join(str(p) for p in products),
            'product_markups': '\n'.join('{} {}'.format(markup, pct) for markup, pct in markups)
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.opened, assignee=self.config.responsible_manager)
        res.add_message(COMMENTS['duplicate_markups_found'].format('test_1'))
        res.commit = True
        res.delay = False
        return res


class SetDuplicateProductMarkupCase(AbstractDBSetTestCase):
    _representation = 'set_duplicate_product_markup'

    def _get_data(self, meta_session):
        markups = [
            ('test_1', 10),
            ('test_2', 20)
        ]
        products = [500000, 500001]
        self.markups = []
        for code, pct in markups:
            self.markups.append(create_markup(meta_session, code=code))
        self.products = []
        for product in products:
            self.products.append(create_product(meta_session, id=product))
        self.product_markups = []
        self.product_markups.append(create_product_markup(meta_session, datetime.datetime.now(), self.products[0],
                                                          self.markups[0], pct=10))
        self.product_markups.append(create_product_markup(meta_session, datetime.datetime.now(), self.products[0],
                                                          self.markups[0], pct=10))
        return {
            'products': ', '.join(str(p) for p in products),
            'product_markups': '\n'.join('{} {}'.format(markup, pct) for markup, pct in markups)
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.opened, assignee=self.config.responsible_manager)
        res.add_message(COMMENTS['duplicate_product_markups_found'].format(self.product_markups[0].product_id,
                                                                           self.product_markups[0].markup.code))
        res.commit = True
        res.delay = False
        return res


class SetDiffPctProductMarkupCase(AbstractDBSetTestCase):
    _representation = 'set_different_pct_product_markup'

    def _get_data(self, meta_session):
        markups = [
            ('test_1', 10),
            ('test_2', 20)
        ]
        products = [500000, 500001]
        self.markups = []
        for code, pct in markups:
            self.markups.append(create_markup(meta_session, code=code))
        self.products = []
        for product in products:
            self.products.append(create_product(meta_session, id=product))
        self.product_markups = []
        self.product_markups.append(create_product_markup(meta_session, datetime.datetime.now(), self.products[0],
                                                          self.markups[0], pct=10))
        return {
            'products': ', '.join(str(p) for p in products),
            'product_markups':
                """
                test_1 20
                test_2 20
                """
        }

    def get_result(self):
        res = RequiredResult(transition=IssueTransitions.wont_fix)
        res.add_message(COMMENTS['invalid_form'].format(COMMENTS['different_pct_product_markup'].format(
            *(self.get_product_markup(self.product_markups[0]) + (20,)))))
        res.commit = True
        res.delay = False
        return res


@pytest.mark.parametrize('issue_data_meta',
                         [case() for case in AbstractDBAddTestCase._cases + AbstractDBSetTestCase._cases],
                         ids=lambda case: str(case),
                         indirect=['issue_data_meta'])
def test_db(meta_session, issue_data_meta):
    queue_object, st_issue, case = issue_data_meta
    required_res = case.get_result()
    solver = AddSetMarkup(queue_object, st_issue)
    # solver inserts in meta session and checks in balance session, uncheckable without commit
    # so emulate both balance and meta with one session
    solver.meta_session = meta_session
    solver.session = meta_session
    res = solver.solve()
    meta_session.flush()
    meta_session.expire_all()
    report = res.issue_report

    assert res.commit == required_res.commit
    assert res.delay == required_res.delay
    assert report.transition == required_res.transition
    assert report.assignee == required_res.assignee
    assert len(report.comments) <= 1
    assert ''.join(set(cmt.text for cmt in report.comments)) \
           == '\n'.join(list(collections.OrderedDict.fromkeys(required_res.comments)))
