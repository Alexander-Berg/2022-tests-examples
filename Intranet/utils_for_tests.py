from builtins import object
from unittest import TestCase

from django.db import DEFAULT_DB_ALIAS, connections
from django.test.utils import CaptureQueriesContext as _CaptureQueriesContext


def assert_dict_contains_subset(subset, dictionary, msg=None):
    """
    Проверяет, что первый словарь является "подмножеством" второго.

    https://github.com/pytest-dev/nose2pytest/blob/master/nose2pytest/assert_tools.py # noqa
    """
    dictionary = dictionary
    missing_keys = sorted(list(set(subset.keys()) - set(dictionary.keys())))
    mismatch_vals = {k: (subset[k], dictionary[k])
                     for k in subset if k in dictionary and
                     subset[k] != dictionary[k]}
    if msg is None:
        assert missing_keys == [], 'Missing keys = {}'.format(missing_keys)
        assert mismatch_vals == {}, (
            'Mismatched values (s, d) = {}'.format(mismatch_vals)
        )
    else:
        assert missing_keys == [], msg
        assert mismatch_vals == {}, msg


def assert_items_equal(expected_list, actual_list, msg=None):
    """
    Функция проверяет, что списки содержат одни и те же элементы,
    причем порядок не важен.
    """
    TestCase(methodName='__init__').assertItemsEqual(
        expected_list, actual_list, msg
    )


def extract_error_message(response_dict):
    """
    Вытаскивает из тела ответа только сообщение об ошибке, например:

    >>> extract_error_message({
    ...     'errors': [{
    ...          'code': 'invalid',
    ...          'message': u"'code' is a required property",
    ...          'source': u'markup'
    ...      }]
    ... })
    ... "code' is a required property"
    """
    assert 'errors' in response_dict
    assert 'message' in response_dict['errors'][0]
    return response_dict['errors'][0]['message']


def assert_has_error_message(response, source=None, message=None, code=None):
    """
    Проверяет, есть ли в переданном запросе сообщение
    с запрощенными параметрами.
    """
    if isinstance(response, list):
        errors = response
    elif isinstance(response, dict):
        errors = response.get('errors', [response])
    else:
        raise AssertionError('Unexpected response type {!r}'.format(response))

    simple_message = {
        'source': source,
        'message': message,
        'code': code,
    }

    # отфильтруем None
    simple_message = {k: v for k, v in simple_message.items()
                      if v is not None}

    for error in errors:
        try:
            assert_dict_contains_subset(simple_message, error)
            # сопадение найдено
            return
        except AssertionError:
            pass

    raise AssertionError('Error {!r} not found in response {!r}'.format(
        simple_message, response
    ))


class CaptureQueriesContext(_CaptureQueriesContext):

    def __init__(self, connection=None, using=None):
        self.finalizers = []

        connection = connections[using or DEFAULT_DB_ALIAS]
        super(CaptureQueriesContext, self).__init__(connection)

    def __exit__(self, exc_type, exc_value, traceback):
        super(CaptureQueriesContext, self).__exit__(exc_type, exc_value, traceback)  # noqa

        if exc_type is not None:
            return

        for finalizer in self.finalizers:
            finalizer(self)

        self.finalizers = []

    def __str__(self):
        return (
            'CaptureQueriesContext stats:\n'
            'Count: {count}\n'
            'Queries:\n'
            '{queries}'.format(
                count=len(self),
                queries='\n'.join(
                    query['sql'] for query in self.captured_queries
                )
            )
        )


def assert_queries_count_equal(count, connection=None, using=None):
    """
    Адаптированный под pytest context-manager из django: _AssertNumQueriesContext  # noqa
    Позволяет замерить количество запросов в контексте менеджера
    и выводит логи запросов, например:

    with assert_queries_count_equal(10):
        # some db queries
        for i in range(10):
            Model.objects.create(id=i)

    """

    def check_queries_count(self):
        executed = len(self)
        assert executed == count, (
            "%d queries executed, %d expected\nCaptured queries were:\n%s" % (
                executed, count,
                '\n'.join(
                    query['sql'] for query in self.captured_queries
                )
            )
        )

    context = CaptureQueriesContext(connection=connection, using=using)
    context.finalizers.append(check_queries_count)
    return context


class MockedQOr(object):
    """
    Объект для сравнения с выражениями вида
    Q(smth=1) | Q(smth_else=3) | Q(smth__smth_else="crocs")
    или
    Q(Q(smth=1) | Q(smth_else=3)) & Q(smth=1) | Q(smth_else=3)
    """

    @staticmethod
    def flatenize(items=None):
        if not items:
            items = []
        flatten_list = []
        for item in items:
            if not hasattr(item, 'children'):
                flatten_list.append(item)
            else:
                flatten_list.extend(MockedQOr.flatenize(item.children))
        return flatten_list

    def __init__(self, *args, **kwargs):
        children = list(args)
        children.extend(list(kwargs.items()))
        self.children = MockedQOr.flatenize(children)

    def __eq__(self, other):
        other_children = MockedQOr.flatenize(other.children)
        return set(self.children) == set(other_children)
