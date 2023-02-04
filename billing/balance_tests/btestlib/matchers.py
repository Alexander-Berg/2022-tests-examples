# -*- coding: utf-8 -*-
__author__ = 'a-vasin'

from copy import deepcopy
from decimal import Decimal

from hamcrest import has_entries, equal_to
from hamcrest.core.base_matcher import BaseMatcher
from hamcrest.library.collection.isdict_containingentries import IsDictContainingEntries
from hamcrest.library.number.iscloseto import IsCloseTo

import btestlib.utils as utils


# -------------------------------------------------------------------------------------------------------------------

# Подробное описание матчеров для списков словарей
# https://wiki.yandex-team.ru/testirovanie/functesting/billing/auto/python/manual/#matchery2.0

def equal_to_casted_dict(dictionary, casted=True):
    return CastedDictComparison(dictionary, ignore_extra=False, casted=casted)


def has_entries_casted(dictionary, casted=True):
    return CastedDictComparison(dictionary, ignore_extra=True, casted=casted)


def contains_dicts_equal_to(expected_dictionary_list, same_length=True, in_order=False, casted=True):
    if in_order:
        return ContainsDictsInOrder(expected_dictionary_list, ignore_extra=False,
                                    same_length=same_length, casted=casted)
    return ContainsDictsEqualTo(expected_dictionary_list, same_length, casted)


def contains_dicts_with_entries(expected_dictionary_list, same_length=True, in_order=False, casted=True):
    if in_order:
        return ContainsDictsInOrder(expected_dictionary_list, ignore_extra=True, same_length=same_length, casted=casted)
    return ContainsDictsWithEntries(expected_dictionary_list, same_length, casted)


class CastedDictComparison(BaseMatcher):
    def __init__(self, expected_dictionary, ignore_extra, casted=True):
        self.expected_dictionary = utils.decode_obj(expected_dictionary)
        self.missing_keys = set([])
        self.extra_keys = set([])
        self.different_values = {}
        self.ignore_extra = ignore_extra
        self.casted = casted

    @staticmethod
    def wrap_value(value):
        if isinstance(value, list) or isinstance(value, dict) or isinstance(value, tuple) or isinstance(value, set):
            return equal_to(value)
        return value

    # a-vasin: для удобства отображения удаляем все незначащие поля
    def remove_extra(self, dictionary):
        if self.ignore_extra and dictionary:
            dictionary = deepcopy(dictionary)
            for key in list(dictionary.keys()):
                if key not in self.expected_dictionary:
                    del dictionary[key]

        return dictionary

    def _matches(self, dictionary):
        dictionary = self.remove_extra(dictionary)
        casted_dictionary = utils.cast_dictionary(dictionary, self.expected_dictionary) if self.casted else dictionary

        # a-vasin: проставляем equal_to, так как нам интересен только первый уровень вложенности,
        # для последующих нам нужно полное сравнение
        # Пример:
        # без equal_to: check_that({1: {1: 1, 2: 2}, 2: 2}, has_entries_casted({1: {1: 1}})) -> OK
        # с equal_to: check_that({1: {1: 1, 2: 2}, 2: 2}, has_entries_casted({1: {1: 1}})) -> ERROR
        expected_dictionary = {k: CastedDictComparison.wrap_value(v) for k, v in self.expected_dictionary.iteritems()}

        self.missing_keys, self.extra_keys, self.different_values = utils.deep_diff_compare(casted_dictionary,
                                                                                            expected_dictionary)
        if self.ignore_extra:
            self.extra_keys = set([])

        return len(self.extra_keys) + len(self.missing_keys) + len(self.different_values) == 0

    def describe_to(self, description):
        description.append_text(u'casted dictionary comparison\n')
        CastedDictComparison.describe_to_static(description, self.expected_dictionary)

    @staticmethod
    def describe_to_static(description, expected_dict):
        description.append_text(utils.Presenter.pretty(expected_dict))

    def describe_mismatch(self, item, mismatch_description):
        CastedDictComparison.describe_mismatch_static(mismatch_description, self.missing_keys, self.extra_keys,
                                                      self.different_values)

    @staticmethod
    def describe_mismatch_static(mismatch_description, missing_keys, extra_keys, different_values):
        if missing_keys:
            mismatch_description.append_text(u'missing keys: {}\n'.format(utils.Presenter.pretty(missing_keys)))

        if extra_keys:
            mismatch_description.append_text(u'extra keys: {}\n'.format(utils.Presenter.pretty(extra_keys)))

        if different_values:
            mismatch_description.append_text(
                u'different values: {}\n'.format(utils.Presenter.pretty(different_values)))


class ContainsDicts(BaseMatcher):
    def __init__(self, expected_dictionary_list, ignore_extra, casted=True):
        self.matchers = [CastedDictComparison(expected_dictionary, ignore_extra, casted)
                         for expected_dictionary in expected_dictionary_list]
        self.descriptions_list = []
        self.actual_length = None

    def describe_to(self, description):
        description.append_text(u'\n[\n')
        for matcher in self.matchers:
            CastedDictComparison.describe_to_static(description, matcher.expected_dictionary)
            description.append_text(u'\n')
        description.append_text(u']')

    def describe_mismatch(self, item, mismatch_description):
        if self.actual_length is not None:
            mismatch_description.append_text(u'\n')
            mismatch_description.append_text(u'expected length was: {}\n'.format(len(self.matchers)))
            mismatch_description.append_text(u'actual length is: {}\n'.format(self.actual_length))
            mismatch_description.append_text(u'of dictionaries:\n')
            mismatch_description.append_text(utils.Presenter.pretty(self.descriptions_list))
            return

        for expected_dict, actual_dict, missing_keys, extra_keys, different_values in self.descriptions_list:
            mismatch_description.append_text(u'\n')
            mismatch_description.append_text(u'for expected dictionary {}\n'.format(
                utils.Presenter.pretty(expected_dict)))
            mismatch_description.append_text(u'closest mismatch is {}\n'.format(
                utils.Presenter.pretty(actual_dict)))
            CastedDictComparison.describe_mismatch_static(mismatch_description, missing_keys, extra_keys,
                                                          different_values)

    def describe_single_mismatch(self, errors_list, matcher):
        # a-vasin: Сначала сортируем по количеству разных значений в убывающем порядке
        # А затем уже по количеству ошибок, чтобы первый ответ был с максимальным количеством общих ключей
        # (sorted - гарантировано стабильная сортировка)
        errors_list = sorted(errors_list, key=lambda key: len(key[3]), reverse=True)
        closest_mismatch = sorted(errors_list, key=lambda key: len(key[1]) + len(key[2]) + len(key[3]))[0]

        # a-vasin: (matcher.expected_dictionary, *closest_mismatch) можно только в >= Python 3.5 =(
        self.descriptions_list.append(
            (matcher.expected_dictionary,
             matcher.remove_extra(closest_mismatch[0]),
             closest_mismatch[1],
             closest_mismatch[2],
             closest_mismatch[3])
        )


class ContainsDictsEqualTo(ContainsDicts):
    def __init__(self, expected_dictionary_list, same_length, casted=True):
        super(ContainsDictsEqualTo, self).__init__(expected_dictionary_list, ignore_extra=False, casted=casted)
        self.same_length = same_length

    def _matches(self, dictionary_list):
        dictionary_list = deepcopy(dictionary_list)

        self.descriptions_list = []
        self.actual_length = None

        if len(dictionary_list) < len(self.matchers) or \
                (self.same_length and len(dictionary_list) != len(self.matchers)):
            self.actual_length = len(dictionary_list)
            self.descriptions_list = dictionary_list
            return False

        for matcher in self.matchers:
            errors_list = []
            for index, dictionary in enumerate(dictionary_list):
                if matcher.matches(dictionary):
                    errors_list = None
                    break
                else:
                    errors_list.append((dictionary, matcher.missing_keys, matcher.extra_keys, matcher.different_values))

            if errors_list is None:
                del dictionary_list[index]
                continue

            self.describe_single_mismatch(errors_list, matcher)

        return len(self.descriptions_list) == 0

    def describe_to(self, description):
        description.append_text(u'dictionary list items "equal_to" comparison')
        super(ContainsDictsEqualTo, self).describe_to(description)


# a-vasin: тут экспонента по времени работы, а есть алгоритм Куна для двудольных графов
# http://e-maxx.ru/algo/kuhn_matching
class ContainsDictsWithEntries(ContainsDicts):
    def __init__(self, expected_dictionary_list, same_length, casted=True):
        super(ContainsDictsWithEntries, self).__init__(expected_dictionary_list, ignore_extra=True, casted=casted)
        self.same_length = same_length
        self.all_sets = None

    def _matches(self, dictionary_list):
        self.descriptions_list = []
        self.actual_length = None
        self.all_sets = None

        if len(dictionary_list) < len(self.matchers) or \
                (self.same_length and len(dictionary_list) != len(self.matchers)):
            self.actual_length = len(dictionary_list)
            self.descriptions_list = dictionary_list
            return False

        all_matches = []
        for matcher in self.matchers:
            errors_list = []
            matches = set()
            for index, dictionary in enumerate(dictionary_list):
                if matcher.matches(dictionary):
                    matches.add(index)
                else:
                    errors_list.append((dictionary, matcher.missing_keys, matcher.extra_keys, matcher.different_values))

            if len(matches) > 0:
                all_matches.append(matches)
                continue

            self.describe_single_mismatch(errors_list, matcher)

        if len(self.descriptions_list) > 0:
            return False

        # a-vasin: тут мы тупо генерируем все перестановки, на самом деле тут не очень страшно
        # читайте это как all_sets = {set(match, *_set) for match in matches for _set in all_sets}
        # просто в set нельзя класть set, поэтому tuple
        self.descriptions_list = dictionary_list
        self.all_sets = {()}
        for matches in all_matches:
            self.all_sets = {tuple(set((match,) + _set)) for match in matches for _set in self.all_sets}

        return any(len(_set) == len(self.matchers) for _set in self.all_sets)

    def describe_to(self, description):
        description.append_text(u'dictionary list items "has_entries" comparison')
        super(ContainsDictsWithEntries, self).describe_to(description)

    def describe_mismatch(self, item, mismatch_description):
        # a-vasin: если есть идеи как это писать понятнее, то я открыт для предложений
        if self.all_sets is not None:
            max_len = max(len(_set) for _set in self.all_sets)
            max_len_sets = [_set for _set in self.all_sets if len(_set) == max_len]

            mismatch_description.append_text(u'matched only dictionaries:\n')
            for number, max_len_set in enumerate(max_len_sets):
                matched_dictionaries = {index: self.descriptions_list[index] for index in max_len_set}
                mismatch_description.append_text(utils.Presenter.pretty(matched_dictionaries))

                if number != len(max_len_set) - 1:
                    mismatch_description.append_text(u'\n\nOR\n\n')

            mismatch_description.append_text(u'\nfrom dictionaries:\n')
            mismatch_description.append_text(utils.Presenter.pretty(self.descriptions_list))
            return

        super(ContainsDictsWithEntries, self).describe_mismatch(item, mismatch_description)


class ContainsDictsInOrder(ContainsDicts):
    def __init__(self, expected_dictionary_list, ignore_extra, same_length=True, casted=True):
        super(ContainsDictsInOrder, self).__init__(expected_dictionary_list, ignore_extra, casted)
        self.same_length = same_length

    def _matches(self, dictionary_list):
        self.descriptions_list = []
        self.actual_length = None

        if len(dictionary_list) < len(self.matchers) or \
                (self.same_length and len(dictionary_list) != len(self.matchers)):
            self.actual_length = len(dictionary_list)
            self.descriptions_list = dictionary_list
            return False

        for matcher, dictionary in zip(self.matchers, dictionary_list[:len(self.matchers)]):
            if not matcher.matches(dictionary):
                self.descriptions_list.append(
                    (matcher.expected_dictionary, dictionary, matcher.missing_keys, matcher.extra_keys,
                     matcher.different_values))

        return len(self.descriptions_list) == 0

    def describe_to(self, description):
        description.append_text(u'casted dictionary list comparison in order')
        super(ContainsDictsInOrder, self).describe_to(description)


# -------------------------------------------------------------------------------------------------------------------

def matches_in_time(matcher, timeout=120, sleep_time=5):
    return MatchInTime(matcher, timeout, sleep_time)


class MatchInTime(BaseMatcher):
    def __init__(self, matcher, timeout, sleep_time):
        self.matcher = matcher
        self.timeout = timeout
        self.sleep_time = sleep_time
        self.last_found_value = None

    def _matches(self, item):
        try:
            utils.wait_until(item, self.matcher, timeout=self.timeout, sleep_time=self.sleep_time)
            return True
        except utils.ConditionHasNotOccurred as condition_exception:
            self.last_found_value = condition_exception.last_found_value
            return False

    def describe_mismatch(self, item, mismatch_description):
        mismatch_description.append_text('last mismatch description <')
        self.matcher.describe_mismatch(self.last_found_value, mismatch_description)
        mismatch_description.append_text('>')

    def describe_to(self, description):
        description.append_text('condition <')
        self.matcher.describe_to(description)
        description.append_text('> in time ')
        description.append_text(self.timeout)
        description.append_text(' with sleep time ')
        description.append_text(self.sleep_time)


# -------------------------------------------------------------------------------------------------------------------

def has_only_entries(*keys_valuematchers, **kv_args):
    """
    Матчер дополняет функциональности has_entries
    сначала проверяет, что в записи эталона присутствуют в реальном словаре - идентично has_entries,
    потом что в реальном словаре нет записей кроме тех что есть в эталоне
    """
    return IsDictContainingOnlyEntries(has_entries(*keys_valuematchers, **kv_args).value_matchers)


class IsDictContainingOnlyEntries(IsDictContainingEntries):
    def __init__(self, value_matchers):
        super(IsDictContainingOnlyEntries, self).__init__(dict(value_matchers))

    def matches(self, dictionary, mismatch_description=None):
        entries_matches = super(IsDictContainingOnlyEntries, self).matches(dictionary, mismatch_description)
        extra_entries_keys = set(dictionary.keys()).difference({key for key, value_matcher in self.value_matchers})
        if extra_entries_keys and mismatch_description:
            mismatch_description.append_text(' has extra keys ').append_description_of(', '.join(extra_entries_keys))
        return entries_matches and not extra_entries_keys

    # тупо копипастить метод только чтобы добавить слово only но думаю ничего не поделаешь
    def describe_to(self, description):
        description.append_text('a dictionary containing only {')
        first = True
        for key, value in self.value_matchers:
            if not first:
                description.append_text(', ')
            self.describe_keyvalue(key, value, description)
            first = False
        description.append_text('}')


def matcher_for(condition, descr=None):
    """
    Порождает матчер
    @param condition функция, принимающая на вход один аргумент, и возвращающая булевское значение.
    По-сути выполняет роль matcher._matches(item)
    """

    class NewMatcher(BaseMatcher):
        def _matches(self, item):
            return condition(item)

        def describe_to(self, description):
            description.append_text(descr)

            # todo: возможно понадобится добавить mismatch_description
            # но пока не очень понятно как здесь работать с item

    return NewMatcher()


class IsCloseToDecimal(IsCloseTo):
    def __init__(self, value, delta):
        self.value = Decimal(value)
        self.delta = Decimal(delta)

    def _matches(self, item):
        if not isinstance(item, Decimal):
            item = Decimal(item)
        return Decimal(item - self.value).copy_abs() <= self.delta

    def __repr__(self):
        return "<%s.%s(%s ± %s)>" % (self.__class__.__module__, self.__class__.__name__, self.value, self.delta)


def close_to(value, delta):
    return IsCloseToDecimal(value, delta)
