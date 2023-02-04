from builtins import object, range
from collections import namedtuple

import pytest

from kelvin.common.mixer_tools import django_mixer
from kelvin.common.utils_for_tests import CaptureQueriesContext, assert_queries_count_equal
from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.lessons.serializers import LessonInCLessonSerializer, LessonProblemLinkSerializer
from kelvin.problems.models import Problem, TextResource


@pytest.mark.skip("https://st.yandex-team.ru/EDUCATION-2256")
@pytest.mark.usefixtures('setup_db', 'db')
class TestPrefetchOptimizations(object):
    """
    Тесты прекэширования данных с помощью PrefetchRelationListSerializer
    """
    db = namedtuple(
        'db',
        ('lesson', 'links_count', 'expected_queries_count')
    )

    # Разрешенное число запросов к БД при использовании
    # сериализатора LessonInCLessonSerializer
    ALLOWED_AMOUNT_OF_QUERIES = 2

    @pytest.fixture(scope='class')
    def setup_db(self, db_class):
        """
        Фикстура создает базовый набор объектов для всех тестов:
        лекцию, пользователя, задачи, теории и связи между ними.

        Фикстура прикрепляет некоторые созданные объекты к атрибуту db
        класса теста.
        """
        lesson = django_mixer.blend(
            Lesson,
            name=django_mixer.RANDOM,
        )
        problems = django_mixer.cycle(5).blend(
            Problem,
            name=django_mixer.RANDOM,
            markup={},
            subject__slug=django_mixer.sequence('slug_{0}'),
        )
        theories = django_mixer.cycle(5).blend(TextResource)

        links_with_problem = django_mixer.cycle(5).blend(
            LessonProblemLink,
            lesson=lesson,
            type=LessonProblemLink.TYPE_COMMON,
            problem=django_mixer.sequence(*problems),
        )

        links_with_theory = django_mixer.cycle(5).blend(
            LessonProblemLink,
            lesson=lesson,
            type=LessonProblemLink.TYPE_THEORY,
            theory=django_mixer.sequence(*theories),
        )

        self.db.lesson = lesson
        self.db.links_count = len(links_with_problem) + len(links_with_theory)
        self.db.expected_queries_count = 2

    @pytest.fixture
    def context(self, mocker):
        request = mocker.Mock()
        request.user = self.db.lesson.owner

        return {'request': request}

    def test_check_allowed_amount_of_db_queries(self, context, mocker):
        """
        Проверяет, что основные операции с сериализатором
        LessonInCLessonSerializer вызывают определенное число запросов.
        """
        with assert_queries_count_equal(self.ALLOWED_AMOUNT_OF_QUERIES):
            representation_data = LessonInCLessonSerializer(
                context=context
            ).to_representation(self.db.lesson)

        with assert_queries_count_equal(self.ALLOWED_AMOUNT_OF_QUERIES):
            LessonInCLessonSerializer(
                context=context
            ).to_internal_value(representation_data)

    def test_data_prefetch_optimization(self, mocker, context):
        """
        Проверяет, что при использовании PrefetchRelationListSerializer
        количество запросов к БД уменьшается до количества полей указанных
        в указанных в prefetch_fk_fields (PrefetchRelatedField), а на
        сериализованных данные это никак не влияет.
        """
        # отключаем оптимизацию запросов
        mocker.patch.object(
            LessonProblemLinkSerializer,
            'prefetch_fk_fields',
            new_callable=mocker.PropertyMock,
            return_value=(),
        )

        queries_context = CaptureQueriesContext()

        with queries_context:
            representation_data_before = LessonInCLessonSerializer(
                context=context
            ).to_representation(self.db.lesson)

        representation_queries_count = len(queries_context)

        with assert_queries_count_equal(self.db.links_count):
            instance_data_before = LessonInCLessonSerializer(
                context=context
            ).to_internal_value(representation_data_before)

        # включаем оптимизацию запросов
        mocker.stopall()

        # количество запросов должно сократиться на количество
        # LessonProblemLink
        with assert_queries_count_equal(
            representation_queries_count - self.db.links_count
        ):

            representation_data_after = LessonInCLessonSerializer(
                context=context
            ).to_representation(self.db.lesson)

        with assert_queries_count_equal(self.db.expected_queries_count):
            instance_data_after = LessonInCLessonSerializer(
                context=context
            ).to_internal_value(representation_data_after)

        assert representation_data_before == representation_data_after, (
            u'Результаты сериализации отличаются'
        )

        assert instance_data_before == instance_data_after, (
            u'Результаты десериализации отличаются'
        )

    def test_skip_update_on_many_to_many_save(self, mocker, context):
        """
        Проверяет, что при использовании ManyToManyListSerializer
        и настройки m2m_skip_update, количество запросов сокращается
        на количество many-to-many элементов, которые нужно обновить.
        """
        # отключаем оптимизацию запросов
        mocker.patch.object(
            LessonInCLessonSerializer,
            'm2m_skip_update',
            new_callable=mocker.PropertyMock,
            return_value=(),
        )

        def _problem_link_iterator(representation):
            for index in range(len(representation['problems'])):
                problem = representation['problems'][index].get('problem')
                if not problem:
                    continue
                yield representation['problems'][index]

        def update_problems_options(representation, value):
            """Изменяем поле options во всех задачах"""
            for link in _problem_link_iterator(representation):
                link['options'] = value

        def assert_options_equal(representation, value):
            """Проверяем, что поле options совпадает"""
            for link in _problem_link_iterator(representation):
                assert link['options'] == value

        context['expand_problems'] = True

        representation_data = LessonInCLessonSerializer(
            context=context
        ).to_representation(self.db.lesson)

        options_before = {'max_attempts': 10, 'show_tips': True}
        update_problems_options(representation_data, options_before)

        queries_context = CaptureQueriesContext()

        with queries_context:
            serializer = LessonInCLessonSerializer(
                instance=self.db.lesson,
                data=representation_data,
                context=context,
            )
            serializer.is_valid(raise_exception=True)
            instance_before = serializer.save()

        representation_data_before = LessonInCLessonSerializer(
            context=context
        ).to_representation(instance_before)

        assert_options_equal(representation_data_before, options_before)

        queries_count_before = len(queries_context)

        # включаем оптимизацию
        mocker.stopall()

        options_after = {'max_attempts': 20, 'show_tips': False}
        update_problems_options(representation_data, options_after)

        with assert_queries_count_equal(
            queries_count_before - self.db.links_count
        ):

            serializer = LessonInCLessonSerializer(
                instance=self.db.lesson,
                data=representation_data,
                context=context,
            )
            serializer.is_valid(raise_exception=True)
            instance_after = serializer.save()

        representation_data_after = LessonInCLessonSerializer(
            context=context
        ).to_representation(instance_after)

        assert_options_equal(representation_data_after, options_after)
