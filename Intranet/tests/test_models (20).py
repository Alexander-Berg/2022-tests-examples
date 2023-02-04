from builtins import object, range

import pytest
from mock import MagicMock, call

from django.contrib.auth import get_user_model

from kelvin.problems.models import Problem, ProblemMeta, TextResource, screenshot_upload_path

User = get_user_model()


class TestTextResource(object):
    """
    Тесты модели текстовых ресурсов
    """
    test_data = (
        ({
            'name': 'some name',
        }, u'some name'),
        ({
            'name': 'some other name',
        }, u'some other name'),
        ({
            'name': 'MORE NAMES FOR THE GOD OF NAMES',
        }, u'MORE NAMES FOR THE GOD OF NAMES'),
    )

    @pytest.mark.parametrize('data,expected', test_data)
    def test_unicode(self, data, expected):
        """
        Тест метода `TextResource.__str__(self)`
        """
        text_resource = TextResource(**data)
        assert text_resource.__str__() == expected, (
            u'Неправильное строковое представление текстового ресурса')


class TestProblem(object):
    """
    Тесты задач
    """
    unicode_data = (
        (
            {
                'id': 1,
                'markup': {},
                'owner': User(id=1, email='q@q.com'),
            },
            u'Задача 1',
        ),
        (
            {
                'id': 2,
                'markup': {},
                'meta': ProblemMeta(id=4),
                'owner': User(id=3, email='q@q.com'),
            },
            u'Задача 2',
        ),
    )

    @pytest.mark.parametrize('problem_data,expected_name', unicode_data)
    def test_unicode(self, problem_data, expected_name):
        """
        Проверка названия в админке
        """
        assert Problem(**problem_data).__str__() == expected_name, (
            u'Неправильное название задачи')

    def test_get_original(self, mocker):
        """
        Проверяет метод `get_original`
        """
        meta = ProblemMeta(id=2)
        problem = Problem(id=1, original=False, markup={}, meta=meta)
        mocked_objects = mocker.patch.object(Problem, 'objects')
        other_problem = Problem(id=2, original=True, markup={}, meta=meta)
        mocked_objects.filter.return_value = [other_problem]

        assert problem.get_original().id == 2, (
            u'Неверный id вернувшегося оригинала')
        assert mocked_objects.filter.called, u'filter метод не вызван'
        mocked_objects.filter.assert_called_with(meta__id=2, original=True)

        mocked_objects.reset_mock()
        problem.original = True
        assert problem.get_original().id == 1, (
            u'Неверный id вернувшегося оригинала')
        assert not mocked_objects.filter.called, (
            u'filter метод не должен быть вызван')

        mocked_objects.reset_mock()
        problem.original = False
        problem.meta = None
        assert problem.get_original().id == 1, (
            u'Неверный id вернувшегося оригинала')
        assert not mocked_objects.filter.called, (
            u'filter метод не должен быть вызван')

        mocked_objects.reset_mock()
        mocked_objects.filter.return_value = []
        problem.meta = meta
        assert problem.get_original() is None, u'метод должен вернуть None'
        assert mocked_objects.filter.called, u'filter метод не был вызван'

    def test_get_clones(self, mocker):
        """
        Проверяет метод `get_clones`
        """
        meta = ProblemMeta(id=2)
        problem = Problem(id=1, original=False, markup={}, meta=meta)
        mocked_objects = mocker.patch.object(Problem, 'objects')
        other_problems = [Problem(id=i, original=False, markup={}, meta=meta)
                          for i in range(1, 3)]
        mocked_objects.filter.return_value = other_problems

        assert problem.get_clones() == other_problems, (
            u'Неверный id вернувшегося оригинала')
        assert mocked_objects.filter.called, u'filter метод не вызван'
        mocked_objects.filter.assert_called_with(meta__id=2, original=False)

    @pytest.mark.parametrize('difficulty, max_points',
                             ((1, 10), (2, 30), (3, 50), (None, 30)))
    def test_calculate_max_points(self, mocker, difficulty, max_points):
        """
        Проверяет метод `_calculate_max_points`
        """
        meta = None
        if difficulty:
            meta = ProblemMeta(id=1, difficulty=difficulty)
        problem = Problem(id=1, markup={}, meta=meta)
        assert problem._calculate_max_points() == max_points

    is_markup_android_supported = (
        ([], True),
        (
            [{'kind': 'marker', 'content': {'id': 1, 'type': 'chopsticks'}}],
            True
        ),
        (
            [{'kind': 'marker', 'content': {'id': 1, 'type': 'field'}}],
            False
        ),
        (
            [{'kind': 'marker', 'content': {'id': 1, 'type': 'edit'}},
             {'kind': 'marker', 'content': {'id': 2, 'type': 'textandfile'}}],
            False
        ),
    )

    @pytest.mark.parametrize('layout,expected',
                             is_markup_android_supported)
    def test_is_markup_android_supported(self, layout, expected):
        assert Problem.is_markup_android_supported(
            {'layout': layout}) == expected

    save_cases = (
        (
            Problem(id=1, markup='{}'),
            Problem(id=1, markup='{}', max_points=1),
        ),
        (
            Problem(id=2, markup='{}'),
            Problem(id=2, markup='{}', max_points=1),
        ),
        (
            # Нужно скопировать основную разметку в старую
            Problem(
                markup={'layout': [
                    {'kind': 'marker',
                     'content': {'id': 1, 'type': 'inline'}}
                ]},
                max_points=1,
            ),
            Problem(
                markup={'layout': [
                    {'kind': 'marker',
                     'content': {'id': 1, 'type': 'inline'}}
                ]},
                old_markup={'layout': [
                    {'kind': 'marker',
                     'content': {'id': 1, 'type': 'inline'}}
                ]},
                max_points=1,
            ),
        ),
    )

    @pytest.mark.parametrize(('case', 'result'), save_cases)
    def test_save(self, mocker, case, result):
        """
        Тест сохранения модели. Проверяем, что пустая строка заменяется на
        None
        """
        mocked_super_save = mocker.patch('django.db.models.Model.save')
        case.save()

        assert case.max_points == result.max_points
        assert case.old_markup == result.old_markup

        assert mocked_super_save.called, (
            u'Не было вызова родительского `save`'
        )


def test_screenshot_upload_path(mocker):
    """
    Тест получения пути для загрузки скриншота
    """
    problem = MagicMock()
    problem.id = 55
    mocked_safe_filename = mocker.patch('kelvin.problems.models.safe_filename')
    mocked_safe_filename.return_value = 'safe.png'
    assert (screenshot_upload_path(problem, 'unsafe.png') == 'safe.png'), u'Неправильные имя и путь файла'
    assert mocked_safe_filename.mock_calls == [
        call('unsafe.png', 'screenshot/55')], (
        u'Должно быть преобразование имени файла в безопасное и уникальное')
    assert problem.mock_calls == [], (
        u'Не должно быть обращений к инстансу задачи')
