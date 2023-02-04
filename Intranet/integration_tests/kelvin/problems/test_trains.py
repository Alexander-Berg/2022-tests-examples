from builtins import object
import pytest
import json

from django.core.urlresolvers import reverse
from rest_framework.status import HTTP_200_OK, HTTP_400_BAD_REQUEST

from kelvin.problems.markers import Marker


@pytest.mark.django_db
class TestMarkerViewSet(object):
    """
    Класс для тестирования ручки проверки маркеров
    """

    check_data = (
        (
            # Выбор
            [
                {
                    'type': 'choice',
                    'options': {
                        'type_display': None,
                        'choices': [
                            u'Брежнев',
                            u'Горбачев',
                            u'Ленин',
                        ],
                    },
                },
                [0, 1],
            ],
            [0],
            [[Marker.CORRECT], 1],
            HTTP_200_OK,
        ),
        (
            # Макароны: Все к одному (пользователь указал правильный ответ)
            [
                {
                    'type': 'macaroni',
                    'options': {
                        'left': {
                            'naming': 'abc',
                            'choices': ['1', '2', '3'],
                        },
                        'right': {
                            'naming': 'abc',
                            'choices': ['4'],
                        },
                    },
                },
                [[[0, 0], [1, 0], [2, 0]]],
            ],
            [[0, 0], [2, 0], [1, 0]],
            [{"compare_with_answer": 0}, 0],
            HTTP_200_OK,
        ),
        (
            # Макароны: Все к одному (пользователь указал неправильный ответ)
            [
                {
                    'type': 'macaroni',
                    'options': {
                        'left': {
                            'naming': 'abc',
                            'choices': ['1', '2', '3'],
                        },
                        'right': {
                            'naming': 'abc',
                            'choices': ['4', '5'],
                        },
                    },
                },
                [[[0, 0], [1, 0], [2, 0]]],
            ],
            [[0, 0], [2, 0], [1, 1]],
            [{
                "edges_status": [1, 1, 0],
                "missing_edges": [[1, 0]],
                "compare_with_answer": 0,
            }, 2],
            HTTP_200_OK,
        ),
        (
            # Инлайновый маркер
            [
                {
                    'type': 'inline',
                    'options': {
                        'text': u'Какой{input:4}то текст с{input:3} разными '
                                u'инпутами. Вставь пропуще{input:1}ые буквы. '
                                u'Ра{input:2}тавь буквы\nИ еще до кучи '
                                u'{resource:1}{formula:1}{resource:2}',
                        'inputs': {
                            '1': {
                                'type': 'field',
                                'group': 1,
                                'options': {
                                    'width': 2,
                                    'type_content': 'text',
                                },
                            },
                            '2': {
                                'type': 'choice',
                                'group': 1,
                                'options': {
                                    'choices': [
                                        u'cc',
                                        u'зз',
                                        u'c',
                                        u'зс',
                                    ],
                                },
                            },
                            '3': {
                                'type': 'comma',
                                'group': 1,
                                'options': {},
                            },
                            '4': {
                                'type': 'separator',
                                'group': 2,
                                'options': {
                                    'choices': [
                                        'hyphen',
                                        'together',
                                    ],
                                },
                            },
                            '5': {
                                'type': 'field',
                                'group': 3,
                                'options': {
                                    'width': 2,
                                    'type_content': 'number',
                                },
                            },
                        },
                    },
                },
                {
                    '1': u'нн',
                    '2': 0,
                    '3': False,
                    '4': 0,
                },
                {
                    '1': {
                        'type': 'AND',
                        'sources': [
                            {
                                'type': 'EQUAL',
                                'sources': [
                                    {
                                        'source': 1,
                                        'type': 'INPUT',
                                    },
                                    {
                                        'source': u'нн',
                                        'type': 'STRING',
                                    },
                                ],
                            },
                            {
                                'type': 'EQUAL',
                                'sources': [
                                    {
                                        'source': 2,
                                        'type': 'INPUT',
                                    },
                                    {
                                        'source': 3,
                                        'type': 'NUMBER',
                                    },
                                ],
                            },
                            {
                                'type': 'EQUAL',
                                'sources': [
                                    {
                                        'source': 3,
                                        'type': 'INPUT',
                                    },
                                    {
                                        'source': False,
                                        'type': 'BOOLEAN',
                                    },
                                ],
                            },
                        ],
                    },
                    '2': {
                        'type': 'EQUAL',
                        'sources': [
                            {
                                'source': 4,
                                'type': 'INPUT',
                            },
                            {
                                'source': 0,
                                'type': 'NUMBER',
                            },
                        ],
                    },
                    '3': {
                        'type': 'EQUAL',
                        'sources': [
                            {
                                'source': 5,
                                'type': 'INPUT',
                            },
                            {
                                'source': 7,
                                'type': 'NUMBER',
                            },
                        ],
                    },
                },
            ],
            {'1': u'нн', '2': 3, '3': False, '4': 0, '5': '7'},
            [{'1': True, '2': True, '3': True}, 0],
            HTTP_200_OK,
        ),
    )

    @pytest.mark.parametrize('marker_data,user_answer,result,status_code',
                             check_data)
    def test_check(self, marker_data, user_answer, result, status_code,
                   jclient):
        """
        Проверка подсчета ошибок
        """

        url = reverse('v2:train-check-answer')
        response = jclient.post(
            url,
            {
                'marker_data': json.dumps(marker_data),
                'user_answer': json.dumps(user_answer),
            }
        )
        assert response.status_code == status_code, (
            u'Не совпадает код результата')

        assert response.json() == result, u'Не совпадает результат'
