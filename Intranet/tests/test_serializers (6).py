from builtins import object
from copy import deepcopy

import pytest
from mock import MagicMock, call

from django.core.exceptions import ValidationError as DjangoValidationError

from rest_framework import serializers

from kelvin.common.serializer_mixins import SerializerForeignKeyMixin, SkipNullsMixin
from kelvin.problems.answers import Answer
from kelvin.problems.models import ProblemHistory
from kelvin.problems.serializers import (
    AnswerDatabaseSerializer, AnswerSerializer, ProblemSerializer, TextResourceSerializer,
)
from kelvin.problems.tests.data.text_resource_data import (
    text_resources_valid, text_resources_with_invalid_formula_ids, text_resources_with_invalid_resource_ids,
)


class TestProblemSerializer(object):
    """
    Тесты сериализатора задачи
    """

    def test_validate(self, mocker):
        """
        Проверяем, что вызывается правильно валидатор, записываются ресурсы,
        если возникает ошибка, она переформировывается
        """
        mocked_validate_markup = mocker.patch(
            'kelvin.problems.validators.ProblemValidators.validate_markup')
        mocked_validate_markup_json = mocker.patch(
            'kelvin.problems.validators.ProblemValidators'
            '.validate_markup_json')
        mocked_validate_markup_json.return_value = None
        mocked_subject = mocker.patch('kelvin.problems.models.Subject.objects')
        mocked_subject.get.return_value = 'math'

        # нет `subject` в данных, исключений нет
        attrs = {
            'markup': {
                'markers': {},
            },
        }
        return_resources = ['1', '2']
        valid_attrs = {
            'markup': {
                'markers': {},
            },
            'resources': return_resources,
        }

        mocked_validate_markup.return_value = return_resources
        assert ProblemSerializer().validate(deepcopy(attrs)) == valid_attrs, (
            u'В результате должно добавиться поле `resources`')
        assert mocked_validate_markup.mock_calls == [call({'markers': {}})], (
            u'Валидация поля разметки должна быть вызвана со значением поля'
            u'разметки исходных данных'
        )
        assert mocked_validate_markup_json.mock_calls == [
            call({'markers': {}})], u'Должны проверить по json-схеме'
        assert mocked_subject.mock_calls == [], (
            u'Не должно быть запросов предмета')

        # добавляем `subject`
        mocked_validate_markup.reset_mock()
        mocked_validate_markup_json.reset_mock()
        mocked_subject.reset_mock()

        attrs['subject'] = {'slug': 'russian'}  # not Subject instance
        valid_attrs['subject'] = 'math'

        assert ProblemSerializer().validate(deepcopy(attrs)) == valid_attrs, (
            u'В результате должно добавиться поле `resources`')
        assert mocked_validate_markup.mock_calls == [call({'markers': {}})], (
            u'Валидация поля разметки должна быть вызвана со значением поля'
            u'разметки исходных данных'
        )
        assert mocked_validate_markup_json.mock_calls == [
            call({'markers': {}})], u'Должны проверить по json-схеме'
        assert mocked_subject.mock_calls == [call.get(slug='russian')], (
            u'Должны запросить предмет из базы')

        # При разных обычной и старой разметке новая идет только в `markup`
        mocked_validate_markup.reset_mock()
        mocked_validate_markup_json.reset_mock()
        mocked_subject.reset_mock()
        instance = MagicMock(markup={'1': '2'}, old_markup={'3': '4'})

        validated_attrs = ProblemSerializer(instance=instance).validate(
            deepcopy(attrs))

        assert validated_attrs == validated_attrs, (
            u'В результате должно добавиться поле `resources`')
        assert mocked_validate_markup.mock_calls == [call({'markers': {}})], (
            u'Валидация поля разметки должна быть вызвана со значением поля'
            u'разметки исходных данных'
        )
        assert mocked_validate_markup_json.mock_calls == [
            call({'markers': {}})], u'Должны проверить по json-схеме'
        assert mocked_subject.mock_calls == [call.get(slug='russian')], (
            u'Должны запросить предмет из базы')

        # При равных обновление должно затрагивать оба поля
        mocked_validate_markup.reset_mock()
        mocked_validate_markup_json.reset_mock()
        mocked_subject.reset_mock()
        instance = MagicMock(markup={'1': '2'}, old_markup={'1': '2'})

        validated_attrs = ProblemSerializer(instance=instance).validate(
            deepcopy(attrs))

        assert validated_attrs['old_markup'] == validated_attrs['markup'], (
            u'При одинаковых разметках новую нужно проставить в оба поля')
        validated_attrs.pop('old_markup')
        assert validated_attrs == valid_attrs, (
            u'В результате должно добавиться поле `resources`')
        assert mocked_validate_markup.mock_calls == [call({'markers': {}})], (
            u'Валидация поля разметки должна быть вызвана со значением поля'
            u'разметки исходных данных'
        )
        assert mocked_validate_markup_json.mock_calls == [
            call({'markers': {}})], u'Должны проверить по json-схеме'
        assert mocked_subject.mock_calls == [call.get(slug='russian')], (
            u'Должны запросить предмет из базы')

        # возникает исключение при проверке предмета
        mocked_validate_markup.reset_mock()
        mocked_validate_markup_json.reset_mock()
        mocked_subject.reset_mock()
        mocked_subject.get.side_effect = TypeError
        with pytest.raises(serializers.ValidationError) as excinfo:
            ProblemSerializer().validate(deepcopy(attrs))
        assert excinfo.value.detail == {'subject': 'does not exist'}, (
            u'Неправильно сформировано сообщение об ошибке')
        assert mocked_validate_markup.mock_calls == [call({'markers': {}})], (
            u'Валидация поля разметки должна быть вызвана со значением поля'
            u'разметки исходных данных'
        )
        assert mocked_subject.mock_calls == [call.get(slug='russian')], (
            u'Должны запросить предмет из базы')

        # возникает исключение при проверке разметки
        mocked_validate_markup.reset_mock()
        mocked_validate_markup_json.reset_mock()
        mocked_subject.reset_mock()
        mocked_validate_markup.side_effect = DjangoValidationError('message')
        with pytest.raises(serializers.ValidationError) as excinfo:
            ProblemSerializer().validate(deepcopy(attrs))
        assert excinfo.value.detail == {'markup': ['message']}, (
            u'Неправильно сформировано сообщение об ошибке')
        assert mocked_validate_markup.mock_calls == [call({'markers': {}})], (
            u'Валидация поля разметки должна быть вызвана со значением поля'
            u'разметки исходных данных'
        )
        assert mocked_validate_markup_json.mock_calls == [
            call({'markers': {}})], u'Должны проверить по json-схеме'
        assert mocked_subject.mock_calls == [], (
            u'Не должны запрашивать предмет из базы')

    def test_to_representation(self, mocker):
        """
        Проверяем обработку параметра `hide_answers` и удаление поля
        `cm_comment` в зависимости от пользователя
        """
        mocked_request = MagicMock()
        mocked_request.user.is_authenticated = True

        to_representation_data = {
            'markup': {
                'public_solution': u'Решение для учеников',
                'layout': [
                    {
                        'content': {
                            'type': 'any type',
                            'id': 1,
                            'options': {
                                'option1': True
                            }
                        },
                        'kind': 'marker'
                    },
                    {
                        'content': {
                            'type': 'new',
                            'id': 2,
                            'options': {}
                        },
                        'kind': 'marker'
                    }
                ],
                'checks': {},
                'answers': {
                    '1': 'right answer',
                    '2': {
                        'good': 'choice'
                    }
                },
                'solution': 'Решение для учителей',
            },
        }
        expected = {
            'markup': {
                'layout': [
                    {
                        'content': {
                            'type': 'any type',
                            'id': 1,
                            'options': {
                                'option1': True
                            }
                        },
                        'kind': 'marker'
                    },
                    {
                        'content': {
                            'type': 'new',
                            'id': 2,
                            'options': {}
                        },
                        'kind': 'marker'
                    }
                ],
            },
        }

        # мокаем первый миксин
        mocked_to_repr = mocker.patch.object(SkipNullsMixin,
                                             'to_representation')

        # вопрос с запросом скрыть ответы
        mocked_to_repr.return_value = deepcopy(to_representation_data)
        serializer = ProblemSerializer(context={
            'request': mocked_request,
            'hide_answers': '1',
        })
        assert serializer.to_representation(MagicMock()) == expected, (
            u'Должны быть удалены ответы из маркеров')

        # вопрос без запроса скрыть ответы
        mocked_to_repr.return_value = deepcopy(to_representation_data)
        serializer = ProblemSerializer(context={'request': mocked_request})
        assert (serializer.to_representation(MagicMock()) == to_representation_data), u'Данные не должны измениться'

        # вопрос с запросом от контент-менеджера
        to_representation_data['markup']['cm_comment'] = 'some comment'
        mocked_to_repr.return_value = deepcopy(to_representation_data)

        mocked_request.user.is_content_manager.return_value = True

        serializer = ProblemSerializer(context={'request': mocked_request})
        assert (serializer.to_representation(MagicMock()) == to_representation_data), (
            u'Должен появиться комментарий для контент-менеджеров'
        )

    def test_save(self, mocker):
        """
        Проверяет добавление версии
        """
        mocked_super_save = mocker.patch.object(
            SerializerForeignKeyMixin, 'save')
        mocked_super_save.return_value = 'problem'
        mocked_add_version = mocker.patch.object(ProblemHistory,
                                                 'add_problem_version')
        request = MagicMock()

        assert ProblemSerializer(
            context={'request': request}).save() == 'problem'
        assert mocked_add_version.mock_calls == [
            call('problem', request.user, u'Изменения через API')]
        assert mocked_super_save.called, (
            u'Не было вызова родительского `save`'
        )


class TestTextResourceSerializer(object):
    """
    Тесты сериализатора текстовых ресурсов
    """
    @pytest.mark.parametrize('content,formulas,existing_resources',
                             text_resources_valid)
    def test_validate_positive(self, mocker, content, formulas,
                               existing_resources):
        """
        Проверяем валидацию при наличии тегов
        существующих ресурсов в поле content

        :param existing_resources: массив айдишников существующих ресурсов
        """
        mocked = mocker.patch('kelvin.problems.models.Resource.objects')
        mocked.filter.return_value.values_list.return_value = (
            existing_resources)

        data = {
            'content': content,
            'formulas': formulas,
        }

        validated_data = TextResourceSerializer().validate(data)
        assert validated_data['content'] == content, (
            u'поле content изменилось при валидации')
        assert validated_data['resources'] == set(existing_resources), (
            u'неверный набор ресурсов')

    @pytest.mark.parametrize('content,existing_resources,error',
                             text_resources_with_invalid_resource_ids)
    def test_validate_negative_resource_errors(self, mocker, content,
                                               existing_resources, error):
        """
        Проверяем валидацию при наличии тегов
        несуществующих ресурсов в поле content

        :param existing_resources: массив айдишников существующих ресурсов
        """
        mocked = mocker.patch('kelvin.problems.models.Resource.objects')
        mocked.filter.return_value.values_list.return_value = (
            existing_resources)

        data = {
            'content': content,
        }

        with pytest.raises(serializers.ValidationError) as excinfo:
            TextResourceSerializer().validate(data)
        assert excinfo.value.detail == {'content': error}

    @pytest.mark.parametrize('content,formulas,error',
                             text_resources_with_invalid_formula_ids)
    def test_validate_negative_formula_errors(self, mocker, content, formulas,
                                              error):
        """
        Тесты валидации при несоответствии формул в `content` формулам в
        `formulas`
        """
        mocked_resources = mocker.patch(
            'kelvin.problems.models.Resource.objects')
        mocked_resources.filter.return_value.values_list.return_value = []

        data = {'content': content, 'formulas': formulas}

        with pytest.raises(serializers.ValidationError) as excinfo:
            TextResourceSerializer().validate(data)
        assert excinfo.value.detail == {'content': error}


class TestAnswerSerializer(object):
    """
    Тесты сериализатора ответа
    """
    to_representations_data = (
        (
            {
                'markers': {
                    '1': {
                        'answer': 2,
                        'mistakes': 4,
                        'answer_status': 0,
                    },
                    '2': {
                        'any': 2,
                        'mistakes': 4,
                        'key': 0,
                    },
                    '3': {
                        'any': 2,
                        'mistakes': 3,
                        'key': 0,
                    },
                },
                'status': 0,
            },
            {
                'markers': {
                    '1': {
                        'answer': 2,
                        'status': 0,
                        'answer_status': 0,
                        'mistakes': 4,
                    },
                    '2': {
                        'any': 2,
                        'key': 0,
                        'status': 0,
                        'mistakes': 4,
                    },
                    '3': {
                        'any': 2,
                        'key': 0,
                        'status': 0,
                        'mistakes': 3,
                    },
                },
                'status': 0,
            }
        ),
        (
            {
                'markers': {
                    '1': {
                        'answer': 2,
                        'mistakes': None,
                        'answer_status': 0,
                    },
                    '2': {
                        'any': 2,
                        'key': 0,
                    },
                },
                'status': 2,
            },
            {
                'markers': {
                    '1': {
                        'answer': 2,
                        'status': 2,
                        'answer_status': 0,
                        'mistakes': None,
                    },
                    '2': {
                        'any': 2,
                        'key': 0,
                        'status': 0,
                    },
                },
                'status': 2,
            }
        ),
        (
            {
                'status': 0,
                'any': 'data',
            },
            {
                'status': 0,
                'any': 'data',
            },
        ),
    )

    @pytest.mark.parametrize('representation,serialized_value',
                             to_representations_data)
    def test_to_representation(self, mocker, representation, serialized_value):
        """
        Проверяем, что удаляем поле `mistakes`
        """
        mocked_to_representation = mocker.patch.object(serializers.Serializer,
                                                       'to_representation')
        mocked_to_representation.return_value = representation
        answer = MagicMock()
        assert (AnswerSerializer().to_representation(answer) == serialized_value), u'Неправильно сериализованы данные'
        assert mocked_to_representation.mock_calls == [call(answer)], (
            u'Должен быть вызов родительского метода')

    to_representations_with_hide_answers_data = (
        (
            {
                'markers': {
                    '1': {
                        'answer': 2,
                        'mistakes': 4,
                        'answer_status': 0,
                    },
                    '2': {
                        'any': 2,
                        'mistakes': 4,
                        'key': 0,
                    },
                },
                'status': 0,
            },
            {
                'markers': {
                    '1': {
                        'answer': 2,
                    },
                    '2': {
                        'any': 2,
                        'key': 0,
                    },
                },
            },
        ),
        (
            {
                'status': 0,
                'any': 'data',
            },
            {
                'any': 'data',
            },
        ),
    )

    @pytest.mark.parametrize('representation,serialized_value',
                             to_representations_with_hide_answers_data)
    def test_to_representation_with_hide_answers(self, mocker, representation,
                                                 serialized_value):
        """
        Проверяем, что удаляем поле `mistakes`
        """
        mocked_to_representation = mocker.patch.object(serializers.Serializer,
                                                       'to_representation')
        mocked_to_representation.return_value = representation
        answer = MagicMock()
        ctx = {'hide_answers': 1}
        assert (
            AnswerSerializer(context=ctx).to_representation(answer) == serialized_value
        ), u'Неправильно сериализованы данные'
        assert mocked_to_representation.mock_calls == [call(answer)], (
            u'Должен быть вызов родительского метода')

    get_points_data = (
        (
            Answer({}, {}, points=None, checked_points=None),
            None,
        ),
        (
            Answer({}, {}, points=3, checked_points=None),
            3,
        ),
        (
            Answer({}, {}, points=None, checked_points=4),
            4,
        ),
        (
            Answer({}, {}, points=3, checked_points=4),
            4,
        ),
    )

    @pytest.mark.parametrize('user_answer,expected', get_points_data)
    def test_get_points(self, user_answer, expected):
        assert AnswerSerializer().get_points(user_answer) == expected

    validate_markers_data = (
        # пропускаются ответы без `user_answer`
        (
            {
                '1': '2',
            },
            {
                '1': {'type': 'any'},
            },
            {},
        ),
        # пропускаются ответы, маркеров которых нет в контексте
        (
            {
                '2': {'user_answer': 23.4},
                '3': {'user_answer': 'answer'},
            },
            {
                '1': {'type': 'any'},
                '2': {'type': 'any'},
            },
            {
                '2': {
                    'user_answer': 23.4,
                },
            },
        ),
    )

    @pytest.mark.parametrize('user_answer,problem_markers,validated_markers',
                             validate_markers_data)
    def test_validate_markers(self, user_answer, problem_markers,
                              validated_markers):
        """
        Проверка изменения ответа пользователя при десериализации
        """
        serializer = AnswerSerializer(context={'markers': problem_markers})
        assert serializer.validate_markers(user_answer) == validated_markers, (
            u'Неправильно сформирован ответ пользователя')

    def test_create(self):
        """
        Проверка создания ответа из валидированных данных
        """
        user_answer = {
            '1': {
                'user_answer': ['2'],
            },
            '2': {
                'user_answer': 23.4,
            },
        }
        serializer = AnswerSerializer()
        answer = serializer.create({'markers': user_answer})
        assert isinstance(answer, Answer), u'Должны создать объект ответа'
        assert answer.mistakes is None, (
            u'У непроверенного ответа не должно быть проставлены ошибки')
        assert answer.markers == user_answer, (
            u'Неправильно сформировано поле `markers` ответа')

    def test_validate(self):
        """Проверка метода валидации"""
        attrs_valid = {
            'markers': {},
        }
        assert AnswerSerializer().validate(attrs_valid) == attrs_valid, (
            u'Данные не должны измениться')

        attrs_invalid = {
            'completed': False,
            'mistakes': 8,
        }
        with pytest.raises(serializers.ValidationError) as excinfo:
            AnswerSerializer().validate(attrs_invalid)
        assert excinfo.value.detail == (
            [u'markers, mistakes or spent_time fields should be in answer: '
             u'{\'completed\': False, \'mistakes\': 8}']), (
            u'Неправильное сообщение об ошибке')


class TestAnswerDatabaseSerializer(object):
    """
    Тесты сериализатора ответов для БД
    """

    def test_create(self):
        """
        Проверка создания ответа из валидированных данных
        """
        user_answer = {
            '1': {
                'user_answer': ['2'],
            },
            '2': {
                'user_answer': 23.4,
            },
        }
        serializer = AnswerDatabaseSerializer()
        answer = serializer.create({
            'markers': user_answer,
            'theory': {},
            'completed': False,
            'mistakes': 10,
            'max_mistakes': 45,
            'spent_time': 33,
        })
        assert isinstance(answer, Answer), u'Должны создать объект ответа'
        assert answer.markers == user_answer, (
            u'Неправильно сформировано поле `markers` ответа')
        assert answer.mistakes == 10
        assert answer.max_mistakes == 45
        assert answer.spent_time == 33
