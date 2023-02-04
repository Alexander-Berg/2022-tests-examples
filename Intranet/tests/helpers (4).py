# coding: utf-8
import json
from bs4 import BeautifulSoup

from django import db
from django.conf import settings
from django.db import models
from django.test.utils import CaptureQueriesContext
from django.test import override_settings
from waffle.models import Switch


class NumQueryAssertion(CaptureQueriesContext):
    def __init__(self, expected_query_count, show_queries=True, connection=None):
        if connection is None:
            connection = db.connection

        super(NumQueryAssertion, self).__init__(connection)

        self.expected_query_count = expected_query_count
        self.show_queries = show_queries

    def __exit__(self, exc_type, exc_value, traceback):
        super(NumQueryAssertion, self).__exit__(exc_type, exc_value, traceback)

        if exc_type is not None:
            return

        executed = len(self)

        if executed != self.expected_query_count:

            msg = "%d queries executed, %d expected" % (executed, self.expected_query_count)

            if self.show_queries:
                queries = (query['sql'] for query in self.captured_queries)
                msg += "\nCaptured queries were:\n%s" % '\n'.join(queries)

            raise AssertionError(msg)


# Better looking alias
assert_num_queries = NumQueryAssertion


def id_getter(item, id_name='id'):
    if hasattr(item, id_name):
        return getattr(item, id_name)
    elif isinstance(item, int):
        return item
    elif isinstance(item, dict):
        return item[id_name]
    else:
        return item


def assert_ids_equal(first, second):
    first_set = set(id_getter(item) for item in first)
    second_set = set(id_getter(item) for item in second)
    assert first_set == second_set, (first_set, second_set)


def fetch_model(something, cls=None):
    from review.core.logic import domain_objs
    from review.core import models as core_models
    if isinstance(something, int):
        return cls.objects.get(pk=something)
    if isinstance(something, models.Model):
        return something.__class__.objects.get(pk=something.pk)
    if isinstance(something, domain_objs.PersonReview):
        return core_models.PersonReview.objects.get(id=something.id)


def check_db_data(something, expected):
    model_in_db = fetch_model(something)
    for field, expected_value in expected.items():
        model_value = getattr(model_in_db, field)
        assert model_value == expected_value, (field, expected_value, model_value)


def dump_model(obj, fields, locale='ru'):
    return {f: getattr(obj, f, getattr(obj, f'{f}_{locale}', None)) for f in fields}


def update_model(something, cls=None, **params):
    model = fetch_model(something, cls)
    for key, value in params.items():
        setattr(model, key, value)
    model.save()
    return model


def login_manager(login):
    yauth_test_user = settings.YAUTH_TEST_USER
    yauth_test_user['login'] = login or settings.DEFAULT_TEST_USER
    return override_settings(YAUTH_TEST_USER=yauth_test_user)


def get(client, path, request=None, login=None, status_code=200):
    if request is None:
        request = {}
    with login_manager(login):
        response = client.get(path, request)
    assert response.status_code == status_code, response.content
    return response


def get_json(
    client,
    path,
    request=None,
    login=None,
    expect_status=200,
    json_response=True,
):
    if request is None:
        request = {}
    with login_manager(login):
        response = client.get(path, request)
    if expect_status:
        assert response.status_code == expect_status, (
            response.status_code,
            response.content,
        )
    if json_response:
        return response.json()


def post_json(
    client,
    path,
    request,
    login=None,
    expect_status=200,
    json_response=True,
):
    with login_manager(login):
        response = client.post(
            path,
            json.dumps(request),
            content_type="application/json",
        )
    if expect_status:
        assert response.status_code == expect_status, (
            response.status_code,
            response.content,
        )
    if json_response:
        return response.json()


def post_multipart_data(client, path, request, login=None, expect_status=200):
    with login_manager(login):
        response = client.post(path=path, data=request)
    if expect_status:
        assert response.status_code == expect_status, response.content
    return response.json()


def delete_multipart(
    client,
    path,
    data='',
    login=None,
    expect_status=200,
):
    with login_manager(login):
        response = client.delete(path=path, data=data)
    assert response.status_code == expect_status, response.content
    return response.json()


def assert_is_subdict(first, second):
    """
    Если все ключи-начения первого есть во втором
    """
    is_first_dict = isinstance(first, dict)
    is_second_dict = isinstance(second, dict)
    err_msg = 'different structure %r \n %r' % (first, second)
    assert is_first_dict == is_second_dict, err_msg
    first_as_set = set(first.items())
    second_as_set = set(second.items())
    assert not first_as_set - second_as_set, first_as_set ^ second_as_set


def is_flat_dict(dict_):
    if not isinstance(dict_, dict):
        return False
    return not any(
        isinstance(value, (dict, list, tuple))
        for value in list(dict_.values())
    )


def is_simple_type(obj):
    return not isinstance(obj, (dict, list, tuple))


def assert_is_substructure(first, second):
    err_msg = 'different structure %r \n %r' % (first, second)

    is_first_simple = is_simple_type(first)
    is_second_simple = is_simple_type(second)
    assert is_first_simple == is_second_simple, err_msg

    if is_first_simple:
        assert first == second
        return

    is_first_flat = is_flat_dict(first)
    is_second_flat = is_flat_dict(second)
    assert is_first_flat == is_second_flat, err_msg

    if is_first_flat:
        assert_is_subdict(first, second)
        return

    is_first_list = isinstance(first, (list, tuple))
    is_second_list = isinstance(second, (list, tuple))
    assert is_first_list == is_second_list, err_msg

    if is_first_list:
        for first_item, second_item in zip(first, second):
            assert_is_substructure(first_item, second_item)
        return

    for key, first_value in first.items():
        assert key in second, (key, second)
        second_value = second[key]
        if isinstance(first_value, (list, dict)):
            assert_is_substructure(first_value, second_value)
            continue
        assert first_value == second_value


def assert_all_strings_in_text(
    text,
    strings,
    ignore_case=True,
    ignore_whitespace=True,
    ignore_html_tags=True,
):
    if ignore_case:
        text = text.lower()
    if ignore_whitespace:
        text = ' '.join(text.split())
    if ignore_html_tags:
        text = BeautifulSoup(text, "lxml").text
    for string in strings:
        if ignore_case:
            string = string.lower()
        assert string in text, (string, text)


def assert_all_strings_in_html(
    text,
    strings,
    **kwargs
):
    kwargs['ignore_html_tags'] = False
    assert_all_strings_in_text(text, strings, **kwargs)


def waffle_switch(name, active=True):
    Switch(name=name, active=active).save()
