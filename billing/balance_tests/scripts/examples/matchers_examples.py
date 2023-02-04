# coding: utf-8
from hamcrest import anything, greater_than, less_than, instance_of, all_of

import btestlib.utils as utils
from btestlib.matchers import has_only_entries, has_entries_casted, equal_to_casted_dict, contains_dicts_equal_to, \
    contains_dicts_with_entries
from simpleapi.matchers.deep_equals import deep_equals_to, deep_equals_ignore_order_to


def has_only_entries_example():
    expected = {'a': 1, 'b': 2, 'c': 3}
    actual = {'a': 1, 'b': 6, 'extra': 'pff'}

    '''
    Матчер среагирует на 3 ошибки:
       запись с ключом 'b' имеет неверное значение - 6 а не 2
       запись с ключом 'c' отсутствует
       есть лишняя запись с ключом 'extra' (его значение никак не валидируется)
    В отчет будет к сожалению выведена только первая из ошибок о неверном или отсутствующем поле и инфо о лишних полях
    Обычный has_entries не среагировал бы на наличие лишнего поля
    '''
    utils.check_that(actual, has_only_entries(expected))


def anything_example():
    """
    Часто возможна ситуация когда нам важно проверить что в словаре нет лишних записей,
    но проверить значение некоторых мы не хотим или не можем.
    В таком случае в эталоне для этой записи надо ставить значение anything()
    """
    expected = {'a': 1, 'b': 2, 'ignore_value': anything()}
    actual = {'a': 1, 'b': 2, 'ignore_value': "doesn't matter what is in this field"}

    '''отсутствие лишних полей будет проверено, присутствие нужных тоже и проверка пройдет успешно'''
    utils.check_that(actual, has_only_entries(expected))

    '''anything() можно использовать и в большинстве стандартных матчеров'''


def weak_equals_example():
    """
    Матчер для сравнения двух словарей
    Ключевая особенность заключается в том, что эталонный словарь может содержать в качестве значений любые матчеры

    Результат выдается в довольно удобочитаемом виде, но над ним еще можно поработать.
    В идеале это должен быть html с подсвеченными расхождениями, встроенный в allure-отчет

    В данном случае отчет будет таким:

    Expected: dicts deep equals
     but: actual and expected dicts have differences:
    {'values_changed': {'root[2]': {'actual_value': 2,
                                'expected_value': 'a value greater than <5>'},
                    'root[3][2]': {'actual_value': 5,
                                   'expected_value': 'a value less than <4>'},
                    'root[4]': {'actual_value': 4,
                                'expected_value': 'an instance of basestring'},
                    'root[5][2]': {'actual_value': [1, 2],
                                   'expected_value': '(<1> and <2> and <3>)'}}}

    """
    actual = {1: 1,
              2: 2,
              3: [1, 2, 5],
              4: 4,
              5: {1: 1,
                  2: [1, 2]}
              }
    expected = {1: 1,
                2: greater_than(5),
                3: [1, 2, less_than(4)],
                4: instance_of(basestring),
                5: {1: 1,
                    2: all_of(1, 2, 3)}}
    utils.check_that(actual, deep_equals_to(expected))


def weak_equals_ignore_order_example():
    """
    В некоторых случаях порядок элементов в упорядоченных структурах не важен.
    В этом случае можно использовать модификацию матчера weak_equals_ignore_order_to

    Если же при этом допустимы и повторения элементов, можно использовать
    weak_equals_ignore_order_to(report_repetitions=False)

    """
    actual = {1: 1,
              2: [1, 2, 3],
              }
    expected = {1: 1,
                2: [3, 2, 1, 1]}
    utils.check_that(actual, deep_equals_ignore_order_to(expected, report_repetitions=False))  # успех
    utils.check_that(actual, deep_equals_ignore_order_to(expected))  # ошибка


def weak_equals_custom_objects_example():
    """
    Помимо матчеров в словаре, матчер поддерживает большой набор типов, в том числе и произвольные классы

    Подробная документация здесь https://github.com/seperman/deepdiff

    Результат в данном примере:

    Expected: dicts deep equals
     but: actual and expected dicts have differences:
    {'values_changed': {'root[1].a': {'actual_value': 1, 'expected_value': 2}}}

    """

    class CustomClass(object):
        def __init__(self, a, b=None):
            self.a = a
            self.b = b

        def __str__(self):
            return "({}, {})".format(self.a, self.b)

        def __repr__(self):
            return self.__str__()

    actual = {1: CustomClass(1)}
    expected = {1: CustomClass(2)}
    utils.check_that(actual, deep_equals_to(expected))


# Примеры матчеров с кастованием
# На вики https://wiki.yandex-team.ru/testirovanie/functesting/billing/auto/python/manual/#matcherydljaspiskovslovarejj

def has_entries_casted_example():
    """
    Аналог стандартного has_entries для словарей, но параметры могут иметь разный тип

    Ошибка:
    Expected: casted dictionary comparison
    but: missing keys: set(["root['missing_key']"])
    extra keys: set(["root['extra_key']"])
    different values: {"root['key']": {'actual_value': '1', 'expected_value': '2'}}
    """

    utils.check_that({'key1': 1, 'key2': 2}, has_entries_casted({'key1': '1'}))  # OK
    utils.check_that({'key1': 1, 'key2': 2}, has_entries_casted({'key1': '2', 'key3': '3'}))  # ERROR


def equal_to_casted_dict_example():
    """
    Аналог стандартного equal_to, но только для словарей и с приведением типов значений

    Ошибка:
    Expected: casted dictionary comparison
    but: different values: {"root['key1']": {'actual_value': '1', 'expected_value': '2'}}
    """

    utils.check_that({'key': 1}, equal_to_casted_dict({'key': '1'}))  # OK
    utils.check_that({'extra_key': 1, 'key': 1}, equal_to_casted_dict({'key': '2', 'missing_key': 1}))  # ERROR


def dict_list_items_equal_to_example():
    """
    Проверка на то, что actual список словарей содержит все словари из expected листа словарей.

    Размер списков совпадать не обязан.

    Ошибка:
    Expected: casted dictionary list comparison without order
    but:
    for expected dictionary {'key2': 2}
    closest mismatch is {'key1': 1}
    missing keys: set(["root['key2']"])
    extra keys: set(["root['key1']"])
    """

    utils.check_that([{'key1': 1}, {'key2': 2}], contains_dicts_equal_to([{'key2': 2}, {'key1': 1}]))  # OK
    utils.check_that([{'key1': 1}, {'key2': 2}], contains_dicts_equal_to([{'key2': 2}]))  # OK
    utils.check_that([{'key1': 1}], contains_dicts_equal_to([{'key2': 2}, {'key1': 1}]))  # ERROR


def dict_list_items_equal_to_in_order_example():
    """
    Проверка на то, что actual список словарей содержит все словари
    из expected листа словарей в строго заданном порядке.

    Размер списков должен быть одинаков.

    Ошибки:
    1) Expected: casted dictionary list comparison in order
       but: expected length was: 3
       actual length is: 2

    2) Expected: casted dictionary list comparison in order
       but: expected length was: 1
       actual length is: 2

    3) Expected: casted dictionary list comparison in order
       but:
       for expected dictionary {'key2': 2}
       closest mismatch is {'key1': 1}
       missing keys: set(["root['key2']"])
       extra keys: set(["root['key1']"])

       for expected dictionary {'key1': 1}
       closest mismatch is {'key2': 2}
       missing keys: set(["root['key1']"])
       extra keys: set(["root['key2']"])
    """

    utils.check_that([{'key1': 1}, {'key2': 2}],
                     contains_dicts_equal_to([{'key1': 1}, {'key2': 2}], in_order=True))  # OK
    utils.check_that([{'key1': 1}, {'key2': 2}],
                     contains_dicts_equal_to([{'key1': 1}, {'key2': 2}, {'key3': 3}], in_order=True))  # ERROR
    utils.check_that([{'key1': 1}, {'key2': 2}], contains_dicts_equal_to([{'key1': 1}], in_order=True))  # ERROR
    utils.check_that([{'key1': 1}, {'key2': 2}],
                     contains_dicts_equal_to([{'key2': 2}, {'key1': 1}], in_order=True))  # ERROR


def dict_list_items_has_entries_example():
    """
    Проверка на то, что actual список словарей содержит словари, которые содержат записи из expected листа словарей.

    Размер списков совпадать не обязан.

    Ошибки:
    1) Expected: casted dictionary list comparison without order
       but:
       for expected dictionary {'key3': 3}
       closest mismatch is {'key1': 1}
       missing keys: set(["root['key3']"])
    2) Expected: casted dictionary list comparison without order
       but:
       for expected dictionary {'key2': 3}
       closest mismatch is {'key2': 2}
       different values: {"root['key2']": {'actual_value': 2, 'expected_value': 3}}
    3) Expected: casted dictionary list comparison without order
       but:
       for expected dictionary {'key2': 3, 'key3': 3}
       closest mismatch is {'key1': 1, 'key2': 2}
       missing keys: set(["root['key3']"])
       different values: {"root['key2']": {'actual_value': 2, 'expected_value': 3}}
    """

    utils.check_that([{'key1': 1, 'key2': 2}], contains_dicts_with_entries([{'key1': 1}, {'key2': 2}]))  # OK
    utils.check_that([{'key1': 1}, {'key2': 2}], contains_dicts_with_entries([{'key1': 1}, {'key2': 2}]))  # OK
    utils.check_that([{'key2': 2}, {'key3': 3}, {'key1': 1}],
                     contains_dicts_with_entries([{'key1': 1}, {'key2': 2}]))  # OK
    utils.check_that([{'key1': 1, 'key3': 3}, {'key2': 2, 'key3': 3}],
                     contains_dicts_with_entries([{'key1': 1}, {'key2': 2}]))  # OK
    utils.check_that([{'key1': 1}, {'key2': 2}], contains_dicts_with_entries([{'key3': 3}]))  # ERROR
    utils.check_that([{'key1': 1}, {'key2': 2}], contains_dicts_with_entries([{'key2': 3}]))  # ERROR
    utils.check_that([{'key1': 1}, {'key2': 2, 'key1': 1}],
                     contains_dicts_with_entries([{'key2': 3, 'key3': 3}]))  # ERROR


def dict_list_items_has_entries_in_order_example():
    """
    Проверка на то, что actual список словарей содержит все записи из соответствующего словаря expected листа.

    Размер списков должен быть одинаков.

    Ошибки:
    1) Expected: casted dictionary list comparison in order
       but: expected length was: 3
       actual length is: 2
    2) Expected: casted dictionary list comparison in order
       but: expected length was: 1
       actual length is: 2
    3) Expected: casted dictionary list comparison in order
       but:
       for expected dictionary {'key1': 1}
       closest mismatch is {'key2': 2}
       missing keys: set(["root['key1']"])

       for expected dictionary {'key2': 2}
       closest mismatch is {'key1': 1}
       missing keys: set(["root['key2']"])
    4) Expected: casted dictionary list comparison in order
       but: expected length was: 2
       actual length is: 1
    5) Expected: casted dictionary list comparison in order
       but: expected length was: 1
       actual length is: 2
    """

    utils.check_that([{'key1': 1}, {'key2': 2}],
                     contains_dicts_with_entries([{'key1': 1}, {'key2': 2}], in_order=True))  # OK
    utils.check_that([{'key1': 1, 'key3': 3}, {'key2': 2, 'key3': 3}],
                     contains_dicts_with_entries([{'key1': 1}, {'key2': 2}], in_order=True))  # OK
    utils.check_that([{'key1': 1}, {'key2': 2}],
                     contains_dicts_with_entries([{'key1': 1}, {'key2': 2}, {'key3': 3}], in_order=True))  # ERROR
    utils.check_that([{'key1': 1}, {'key2': 2}], contains_dicts_with_entries([{'key1': 1}], in_order=True))  # ERROR
    utils.check_that([{'key2': 2}, {'key1': 1}],
                     contains_dicts_with_entries([{'key1': 1}, {'key2': 2}], in_order=True))  # ERROR
    utils.check_that([{'key1': 1, 'key2': 2}],
                     contains_dicts_with_entries([{'key1': 1}, {'key2': 2}], in_order=True))  # ERROR
    utils.check_that([{'key1': 1}, {'key2': 2}], contains_dicts_with_entries([{'key3': 3}], in_order=True))  # ERROR


if __name__ == '__main__':
    dict_list_items_has_entries_in_order_example()
