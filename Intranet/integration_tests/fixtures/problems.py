from builtins import object
import pytest

from kelvin.problems.answers import Answer
from kelvin.problems.models import (
    Problem, TextResource, TextResourceContentType,
)
from kelvin.problems.serializers import ContentTypeSerializer
from kelvin.resources.models import Resource


@pytest.fixture
def problem1(subject_model, content_manager):
    """
    Модель задачи
    """
    markup = {
        'layout': [
            {
                'content': {
                    'text': u'Вычислите значение **выражения** '
                            u'$-0{,}6:  5  : ( -0{,}3 )$.',
                    'options': {
                        'style': 'normal'
                    },
                },
                'kind': 'text'
            },
            {
                'content': {
                    'type': 'field',
                    'id': 1,
                    'options': {
                        'type_content': 'number'
                    },
                },
                'kind': 'marker'
            },
        ],
        'checks': {},
        'answers': {
            '1': '4'
        },
        'solution': '$-0{,}6:  5  : ( -0{,}3 ) = \\dfrac{ -0{,}12 }'
                    '{ -0{,}3 } = 0{,}4$.',
        'cm_comment': u'секретный комментарий',
    }

    return Problem.objects.create(
        markup=markup,
        subject=subject_model,
        name=u'Задача 1',
        owner=content_manager,
        visibility=Problem.VISIBILITY_PUBLIC,
    )


@pytest.fixture
def content_type():
    """
    Инстанс типа контента и его сериализованная форма
    """
    instance, _ = TextResourceContentType.objects.get_or_create(
        name=u'Теория',
        defaults={
            'resource': Resource.objects.get_or_create(file='test.jpg')[0],
        }
    )
    return ContentTypeFixture(instance)


class ContentTypeFixture(object):

    def __init__(self, instance):
        self.instance = instance

    @property
    def serialized(self):
        return dict(ContentTypeSerializer(self.instance).data)


@pytest.fixture
def problem_models(some_owner, subject_model):
    """Модели вопросов"""
    problem1 = Problem.objects.create(
        markup={
            'public_solution': u'Решение для учеников',
            'layout': [
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal',
                        },
                    },
                    'kind': 'text',
                },
                {
                    'content': {
                        'type': 'inline',
                        'id': 1,
                        'options': {
                            'inputs': {
                                '1': {
                                    'type': 'field',
                                    'group': 1,
                                    'options': {
                                        'width': 3,
                                        'type_content': 'number',
                                    },
                                },
                            },
                        }
                    },
                    'kind': 'marker',
                }
            ],
            'checks': {
                '1': {
                    '1': {
                        'type': 'EQUAL',
                        'sources': [
                            {
                                'type': 'INPUT',
                                'source': 1,
                            },
                            {
                                'type': 'NUMBER',
                                'source': 4,
                            },
                        ],
                    },
                },
            },
            'answers': {
                '1': {
                    '1': '4',
                },
            },
            'solution': u'Решение для учителей'
        },
        owner=some_owner,
        subject=subject_model,
    )
    problem2 = Problem.objects.create(
        markup={
            'public_solution': u'Решение для учеников 2',
            'layout': [
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal',
                        },
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'choice',
                        'id': 1,
                        'options': {
                            'type_content': 'number',
                            'choices': [
                                u'Брежнев',
                                u'Горбачев',
                                u'Ленин',
                            ],
                        },
                    },
                    'kind': 'marker',
                },
            ],
            'checks': {},
            'answers': {
                '1': [
                    1,
                    2,
                ],
            },
            'solution': u'Решение для учителей 2',
        },
        owner=some_owner,
        subject=subject_model,
    )
    return problem1, problem2


@pytest.fixture
def scorm_problem(some_owner, subject_model):
    """Модели вопросов"""
    return Problem.objects.create(
        markup={
            "layout": [
                {
                    "content": {
                        "type": "scorm",
                        "id": 1,
                        "options": {
                            "sco": {
                                "id": 2658
                            }
                        }
                    },
                    "kind": "marker"
                }
            ],
            "checks": {
                "1": {}
            },
            "answers": {
                "1": {}
            }
        },
        owner=some_owner,
        subject=subject_model,
    )


@pytest.fixture
def problem_with_input_union(some_owner, subject_model):
    """
    Модель задачи с сгруппированными инпутами
    """
    problem = Problem.objects.create(
        markup={
            'public_solution': u'Решение для учеников',
            'layout': [
                {
                    'content': {
                        'text': u'',
                        'options': {},
                    },
                    'kind': 'text',
                },
                {
                    'content': {
                        'type': 'inline',
                        'id': 1,
                        'options': {
                            'text': '{input:10}{input:20}',
                            'inputs': {
                                '10': {
                                    'type': 'field',
                                    'group': '100',
                                    'options': {
                                        'width': 3,
                                        'type_content': 'number',
                                    },
                                },
                                '20': {
                                    'type': 'field',
                                    'group': '100',
                                    'options': {
                                        'width': 3,
                                        'type_content': 'number',
                                    },
                                },
                            },
                        }
                    },
                    'kind': 'marker',
                }
            ],
            'checks': {
                '1': {
                    '100': {
                        'type': 'IS_PERMUTATION_FROM',
                        'sources': [
                            {
                                'type': 'MULTIUNION',
                                'sources': [
                                    {
                                        'type': 'INPUT',
                                        'source': 10,
                                    },
                                    {
                                        'type': 'INPUT',
                                        'source': 20,
                                    },
                                ],
                            },
                            {
                                'type': 'MULTIUNION',
                                'sources': [
                                    {
                                        'type': 'UNION',
                                        'sources': [
                                            {
                                                'type': 'NUMBER',
                                                'source': 1,
                                            },
                                            {
                                                'type': 'NUMBER',
                                                'source': 2,
                                            },
                                        ],
                                    },
                                    {
                                        'type': 'UNION',
                                        'sources': [
                                            {
                                                'type': 'NUMBER',
                                                'source': 2,
                                            },
                                            {
                                                'type': 'NUMBER',
                                                'source': 3,
                                            },
                                        ],
                                    },
                                ],
                            },
                        ],
                    },
                },
            },
            'answers': {
                '1': {
                    '10': ['1', '2'],
                    '20': ['2', '3'],
                },
            },
            'solution': u'Решение для учителей'
        },
        owner=some_owner,
        subject=subject_model,
    )
    return problem


@pytest.fixture
def theory_model(some_owner, subject_model, content_type):
    """Модель текстового ресурса-теории"""
    return TextResource.objects.create(
        name=u'Тестовый ресурс',
        owner=some_owner,
        content={'some': 'content'},
        content_type_object=content_type.instance,
        subject=subject_model,
    )


@pytest.fixture
def user_custom_answer(lesson_models):
    lesson, problem1, problem2, link1, link2 = lesson_models
    answers = {
        link1.id: [
            {
                'markers': {},
                'custom_answer': [
                    {
                        'type': 'solution',
                        'message': u'Отправляю решение',
                    },
                ],
            },
        ],
    }
    return answers


@pytest.fixture
def user_custom_answer_with_files(user_custom_answer):
    answers = user_custom_answer
    answers[list(answers.keys())[0]][0]['custom_answer'] = [
        {
            'type': 'solution',
            'message': u'Отправляю решение',
            'files': [
                {'public_key': '123'},
                {'public_key': '456'},
            ]
        },
        {
            'type': 'check',
            'message': u'Вот посмотри пример',
            'points': 8,
            'status': Answer.SUMMARY_CORRECT,
            'files': [
                {'public_key': '789'},
            ]
        },
    ]
    return answers
