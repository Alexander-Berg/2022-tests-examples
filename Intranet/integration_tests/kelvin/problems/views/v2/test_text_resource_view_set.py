from builtins import map, range, object
import random

import pytest
from django.core.urlresolvers import reverse
from mixer.main import Mixer

from kelvin.common.error_responses import ErrorsComposer
from kelvin.common.utils_for_tests import (
    assert_dict_contains_subset,
    assert_has_error_message,
)
from kelvin.problems.models import Resource, TextResource
from kelvin.subjects.models import Subject, Theme


MAX_THEMES = 4
MAX_RESOURCES = 4


mixer = Mixer(locale='ru')


@pytest.fixture
def good_theme_ids():
    """
    Генерируем несколько "хороших" (существующих) айдишников тем
    """
    themes_count = random.randint(1, MAX_THEMES)
    subject = Subject.objects.create()
    return [
        Theme.objects.create(subject=subject).id
        for _ in range(themes_count)
    ]


@pytest.fixture
def good_resource_ids():
    """
    Генерируем несколько "хороших" (существующих) айдишников ресурсов
    """
    resources_count = random.randint(1, MAX_RESOURCES)
    return [
        Resource.objects.create().id
        for _ in range(resources_count)
    ]


@pytest.fixture(
    params=[
        lambda content_type_id: {'id': content_type_id},
        lambda content_type_id: content_type_id,
    ],
    ids=[
        'dict-like-object-id',
        'simple-id',
    ]
)
def content_type_request_factory(request):
    """
    Представление content_type_object при создании новых объектов,
    возможны 2 варианта:

    1. {'content_type_object': integer}
    2. {'content_type_object': {'id': integer}}
    """
    return request.param


@pytest.fixture(
    params=[
        201,
        400,
    ],
    ids=[
        'existing-content-type',
        'not-found-content-type',
    ]
)
def content_type_post_data(content_type, content_type_request_factory,
                           request, good_theme_ids):
    """
    Фикстура подготоваливает тестовые данные для запросов на создание
    текстовых ресурсов.
    """
    post_data_passed = {
        'name': mixer.faker.name(),
        'content': mixer.faker.text(),
        'themes': good_theme_ids,
        'content_type_object': None,
    }

    post_data_expected = post_data_passed.copy()

    if request.param == 201:
        post_data_passed['content_type_object'] = content_type_request_factory(
            content_type.instance.id
        )

        post_data_expected['content_type_object'] = content_type.serialized

    elif request.param == 400:
        not_existing_id = content_type.instance.id + 1000
        post_data_passed['content_type_object'] = content_type_request_factory(
            not_existing_id
        )

        post_data_expected = ErrorsComposer.compose_response_body(
            code='doesnt_exist',
            source='content_type_object',
            message='Some objects for Content type object '
                    'doesn\'t exist: {}'.format(not_existing_id)
        )

    return {
        'passed': post_data_passed,
        'expected': (request.param, post_data_expected),
    }


@pytest.fixture(
    params=list(range(5)),
    ids=[
        'without-name',
        'invalid-content',
        'without-content-type-object',
        'without-name-and-content-type-object',
        'wrong-theme',
    ]
)
def post_text_resource_data_bad(content_type, request):
    """
    Фикстура создает набор данных для создания текстовых ресурсов с невалидными
    значениями и ожидаемом ответом сервера
    """
    data = (
        (
            {
                'content': 'Some content',
                'content_type_object': content_type.serialized.get('id'),
                'themes': [],
            },
            {'name': 'required'},
        ),
        (
            {
                'name': 'Some name',
                'content': 'Some content with bad resource {resource:31337}',
                'content_type_object': content_type.serialized.get('id'),
                'themes': [],
            },
            {'content': 'invalid'},
        ),
        (
            {
                'name': 'Some cheatsheet',
                'content': 'Some content',
                'content_type_object': 0,
                'themes': [],
            },
            {'content_type_object': 'doesnt_exist'},
        ),
        (
            {
                'content': 'Some content',
                'content_type_object': 0,
                'themes': [],
            },
            {
                'content_type_object': 'doesnt_exist',
                'name': 'required',
            },
        ),
        (
            {
                'content': 'Some content',
                'content_type_object': 0,
                'themes': [1048576],  # несуществующий айдишник темы
            },
            {
                'content_type_object': 'doesnt_exist',
                'name': 'required',
                'themes': 'does_not_exist',
            },
        ),
    )
    return data[request.param]


@pytest.mark.django_db
class TestTextResourceViewSet(object):
    """
    Тесты рест-интерфейса текстовых ресурсов
    """
    post_text_resource_data_good = (
        (
            {
                'name': 'Some cheatsheet',
                'content': 'Some content',
                'themes': [],
            },
            {
                'name': 'Some cheatsheet',
                'content': 'Some content',
                'formulas': {},
                'themes': [],
            },
        ),
        (
            {
                'name': 'Some cheatsheet',
                'content': 'Some content {formula:1}',
                'formulas': {'1': {'code': '-(-a) = a'}},
                'themes': [],
            },
            {
                'name': 'Some cheatsheet',
                'content': 'Some content {formula:1}',
                'formulas': {'1': {'code': '-(-a) = a'}},
                'themes': [],
            },
        ),
    )

    @pytest.mark.parametrize('post_data,expected',
                             post_text_resource_data_good)
    def test_create_text_resource_positive(
            self, jclient, post_data, expected, good_theme_ids,
            good_resource_ids, teacher, content_type):
        """
        Тест создания текстового ресурса с правильным вводом

        :param post_data: словарь с данными текстового ресурса
        :type content_type: integration_tests.fixtures.problems.ContentTypeFixture # noqa
        """
        create_url = reverse('v2:text_resource-list')
        post_data['content_type_object'] = content_type.serialized
        post_data['themes'].extend(good_theme_ids)
        post_data['content'] += " ".join(
            '{{resource:{0}}}'.format(id_) for id_ in good_resource_ids
        )

        # В ожидаемое тоже добавляем эти поля
        expected['content_type_object'] = content_type.serialized
        expected['themes'].extend(good_theme_ids)
        expected['owner'] = teacher.id
        expected['content'] += " ".join(
            '{{resource:{0}}}'.format(id_) for id_ in good_resource_ids
        )

        response = jclient.post(create_url, post_data)

        # проверяем, что надо быть авторизованным
        assert response.status_code == 401

        # логинимся под учителем
        jclient.login(user=teacher)

        response = jclient.post(create_url, post_data)
        assert response.status_code == 201
        answer = response.json()
        assert answer.pop('id'), (
            u'Нет идентификатора текстового ресурса в ответе')
        assert answer.pop('date_updated'), u'Нет даты обновления в ответе'
        assert 'themes' in answer, u'Нет тем в ответе'
        assert 'resources' in answer, u'Нет ресурсов в ответе'

        # проверяем, что в resources записались правильные ресурсы
        assert set(answer.pop('resources').keys()) == set(
            map(str, good_resource_ids))

        # Проверяем ответ и то, что объект появился в базе
        assert answer == expected
        assert TextResource.objects.all().count() == 1

    def test_create_text_resource_negative(
            self, jclient, post_text_resource_data_bad,
            good_theme_ids, good_resource_ids, teacher):
        """
        Тест создания текстового ресурса с неправильным вводом
        """
        create_url = reverse('v2:text_resource-list')
        post_data, expected_error = post_text_resource_data_bad
        post_data['themes'].extend(good_theme_ids)
        post_data['content'] += " ".join(
            '{{resource:{0}}}'.format(id_) for id_ in good_resource_ids
        )
        response = jclient.post(create_url, post_data)

        # проверяем, что надо быть авторизованным
        assert response.status_code == 401

        # логинимся под учителем
        jclient.login(user=teacher)
        response = jclient.post(create_url, post_data)
        assert response.status_code == 400
        answer = response.json()

        # в ответ получаем правильное сообщение об ошибке
        for source, code in list(expected_error.items()):
            assert_has_error_message(answer, source=source, code=code)
        assert not TextResource.objects.all().exists()

    def test_create_text_resource_with_content_type_object(
            self, jclient, teacher, content_type_post_data):
        """
        Тест для создания ресурса с указанием его типа в виде id.

        :type jclient: integration_tests.conftest.JSONClient
        """
        text_resource_create_url = reverse('v2:text_resource-list')

        post_data = content_type_post_data['passed']

        jclient.login(user=teacher)

        response = jclient.post(text_resource_create_url, post_data)
        status_code, expected_data = content_type_post_data['expected']

        assert response.status_code == status_code
        assert_dict_contains_subset(expected_data, response.json())
