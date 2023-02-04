import pytest

from kelvin.problems.answers import check_answer, Answer
from kelvin.problems.markers import Marker
from kelvin.problems.models import Problem

CHECK_ANSWER_DATA = (
    (
        Problem(markup={
            'layout': [
                {
                    'content': {
                        'text': u'Текст задачи.',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'inline',
                        'id': 1,
                        'options': {
                            'text': u'Ответ: {input:1}',
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
                        },
                    },
                    'kind': 'marker'
                },
                {
                    'content': {
                        'type': 'inline',
                        'id': 2,
                        'options': {
                            'text': u'Ответ: {input:1}',
                            'inputs': {
                                '1': {
                                    'type': 'field',
                                    'group': 1,
                                    'options': {
                                        'type_content': 'text',
                                    },
                                },
                            },
                        },
                    },
                    'kind': 'marker'
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
                                'source': 3,
                            },
                        ],
                    },
                },
                '2': {
                    '1': {
                        'type': 'EQUAL',
                        'sources': [
                            {
                                'type': 'INPUT',
                                'source': 1,
                            },
                            {
                                'type': 'STRING',
                                'source': 'qwerty',
                            },
                        ],
                    },
                },
            },
            'answers': {
                '1': {'1': '3'},
                '2': {'1': 'qwerty'},
            }
        }),
        Answer(markers={
            '1': {
                'user_answer': {'1': '3'},
            },
            '2': {
                'user_answer': {'1': 'qwerty'},
            },
        }, theory={}),
        Answer(
            markers={
                '1': {
                    'user_answer': {'1': '3'},
                    'answer_status': {'1': True},
                    'mistakes': 0,
                    'max_mistakes': 1,
                },
                '2': {
                    'user_answer': {'1': 'qwerty'},
                    'answer_status': {'1': True},
                    'mistakes': 0,
                    'max_mistakes': 1,
                },
            },
            theory={},
            mistakes=0,
            max_mistakes=2,
        )
    ),
    (
        Problem(markup={
            'layout': [
                {
                    'content': {
                        'text': u'Текст задачи.',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'inline',
                        'id': 1,
                        'options': {
                            'text': u'Ответ: {input:1}',
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
                        },
                    },
                    'kind': 'marker'
                },
                {
                    'content': {
                        'type': 'inline',
                        'id': 2,
                        'options': {
                            'text': u'Ответ: {input:1}',
                            'inputs': {
                                '1': {
                                    'type': 'field',
                                    'group': 1,
                                    'options': {
                                        'type_content': 'text',
                                    },
                                },
                            },
                        },
                    },
                    'kind': 'marker'
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
                                'source': 3,
                            },
                        ],
                    },
                },
                '2': {
                    '1': {
                        'type': 'EQUAL',
                        'sources': [
                            {
                                'type': 'INPUT',
                                'source': 1,
                            },
                            {
                                'type': 'STRING',
                                'source': 'qwerty',
                            },
                        ],
                    },
                },
            },
            'answers': {
                '1': {'1': '3.0'},
                '2': {'1': 'qwerty'},
            }
        }),
        Answer(markers={
            '1': {
                'user_answer': {'1': '3.0'},
            },
            '2': {
                'user_answer': {'1': 'QWerty'},
            },
        }, theory={}),
        Answer(
            markers={
                '1': {
                    'user_answer': {'1': '3.0'},
                    'answer_status': {'1': True},
                    'mistakes': 0,
                    'max_mistakes': 1,
                },
                '2': {
                    'user_answer': {'1': 'QWerty'},
                    'answer_status': {'1': True},
                    'mistakes': 0,
                    'max_mistakes': 1,
                },
            },
            mistakes=0,
            max_mistakes=2,
            theory={},
        )
    ),
    (
        Problem(markup={
            'layout': [
                {
                    'content': {
                        'text': u'Текст задачи.',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'inline',
                        'id': 1,
                        'options': {
                            'text': u'Ответ: {input:1}',
                            'inputs': {
                                '1': {
                                    'type': 'field',
                                    'group': 1,
                                    'options': {
                                        'type_content': 'strict',
                                    },
                                },
                            },
                        },
                    },
                    'kind': 'marker'
                },
                {
                    'content': {
                        'type': 'inline',
                        'id': 2,
                        'options': {
                            'text': u'Ответ: {input:1}',
                            'inputs': {
                                '1': {
                                    'type': 'field',
                                    'group': 1,
                                    'options': {
                                        'type_content': 'text',
                                    },
                                },
                            },
                        },
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {
                '1': {
                    '1': {
                        'type': 'OR',
                        'sources': [
                            {
                                'type': 'EQUAL',
                                'sources': [
                                    {
                                        'type': 'INPUT',
                                        'source': 1,
                                    },
                                    {
                                        'type': 'STRING',
                                        'source': u'Александр Павлович',
                                    },
                                ],
                            },
                            {
                                'type': 'EQUAL',
                                'sources': [
                                    {
                                        'type': 'INPUT',
                                        'source': 1,
                                    },
                                    {
                                        'type': 'STRING',
                                        'source': u'Александр',
                                    },
                                ],
                            },
                        ],
                    },
                },
                '2': {
                    '1': {
                        'type': 'EQUAL',
                        'sources': [
                            {
                                'type': 'INPUT',
                                'source': 1,
                            },
                            {
                                'type': 'STRING',
                                'source': 'qwerty any',
                            },
                        ],
                    },
                },
            },
            'answers': {
                '1': {'1': u'Александр Павлович'},
                '2': {'1': '  qwerty aNy '},
            }
        }),
        Answer(markers={
            '1': {
                'user_answer': {'1': u'Александр'},
            },
            '2': {
                'user_answer': {'1': '  QWerty aNy '},
            },
        }, theory={}),
        Answer(
            markers={
                '1': {
                    'user_answer': {'1': u'Александр'},
                    'answer_status': {'1': True},
                    'mistakes': 0,
                    'max_mistakes': 1,
                },
                '2': {
                    'user_answer': {'1': '  QWerty aNy '},
                    'answer_status': {'1': True},
                    'mistakes': 0,
                    'max_mistakes': 1,
                },
            },
            mistakes=0,
            max_mistakes=2,
            theory={},
        )
    ),
    (
        Problem(markup={
            'layout': [
                {
                    'content': {
                        'text': u'Текст задачи.',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'inline',
                        'id': 1,
                        'options': {
                            'text': u'Ответ: {input:1}',
                            'inputs': {
                                '1': {
                                    'type': 'field',
                                    'group': 1,
                                    'options': {
                                        'type_content': 'strict',
                                    },
                                },
                            },
                        },
                    },
                    'kind': 'marker'
                },
                {
                    'content': {
                        'type': 'inline',
                        'id': 2,
                        'options': {
                            'text': u'Ответ: {input:1}',
                            'inputs': {
                                '1': {
                                    'type': 'field',
                                    'group': 1,
                                    'options': {
                                        'type_content': 'number',
                                    },
                                },
                            },
                        },
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {
                '1': {
                    '1': {
                        'type': 'OR',
                        'sources': [
                            {
                                'type': 'EQUAL',
                                'sources': [
                                    {
                                        'type': 'INPUT',
                                        'source': 1,
                                    },
                                    {
                                        'type': 'STRING',
                                        'source': u'Александр Павлович',
                                    },
                                ],
                            },
                            {
                                'type': 'EQUAL',
                                'sources': [
                                    {
                                        'type': 'INPUT',
                                        'source': 1,
                                    },
                                    {
                                        'type': 'STRING',
                                        'source': u'Александр',
                                    },
                                ],
                            },
                        ],
                    },
                },
                '2': {
                    '1': {
                        'type': 'OR',
                        'sources': [
                            {
                                'type': 'EQUAL',
                                'sources': [
                                    {
                                        'type': 'INPUT',
                                        'source': 1,
                                    },
                                    {
                                        'type': 'NUMBER',
                                        'source': 3,
                                    },
                                ],
                            },
                            {
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
                        ],
                    },
                },
            },
            'answers': {
                '1': {'1': u'Александр Павлович'},
                '2': {'1': '3'},
            }
        }),
        Answer(markers={
            '1': {
                'user_answer': {'1': u'Николай'},
            },
            '2': {
                'user_answer': {'1': '4.0'},
            },
        }, theory={}),
        Answer(
            markers={
                '1': {
                    'user_answer': {'1': u'Николай'},
                    'answer_status': {'1': False},
                    'mistakes': 1,
                    'max_mistakes': 1,
                },
                '2': {
                    'user_answer': {'1': '4.0'},
                    'answer_status': {'1': True},
                    'mistakes': 0,
                    'max_mistakes': 1,
                },
            },
            mistakes=1,
            max_mistakes=2,
            theory={},
        )
    ),
    # задача с маркером выбора
    (
        Problem(markup={
            'layout': [
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'choice',
                        'id': 1,
                        'options': {
                            'type_display': None,
                            'choices': [
                                u'Брежнев',
                                u'Горбачев',
                                u'Ленин'
                            ]
                        }
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': [
                    0
                ]
            }
        }),
        Answer(markers={
            '1': {
                'user_answer': [2],
            },
        }, theory={}),
        Answer(
            markers={
                '1': {
                    'user_answer': [2],
                    'answer_status': [Marker.INCORRECT],
                    'mistakes': 2,
                    'max_mistakes': 3,
                },
            },
            mistakes=2,
            max_mistakes=3,
            theory={},
        )
    ),
    # задача с сопоставлением
    (
        Problem(markup={
            'layout': [
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'matching',
                        'id': 1,
                        'options': {
                            'keys': {
                                'choices': [
                                    {
                                        'value': u'Кролик',
                                        'key': u'А'
                                    },
                                    {
                                        'value': u'Лисица',
                                        'key': u'Б'
                                    },
                                    {
                                        'value': u'Собака',
                                        'key': u'В'
                                    }
                                ],
                                'title': u'Звери'
                            },
                            'type_display': None,
                            'values': {
                                'choices': [
                                    {
                                        'value': u'Зайцы',
                                        'key': u'A'
                                    },
                                    {
                                        'value': u'Собаки',
                                        'key': u'B'
                                    }
                                ],
                                'title': u'Семейство'
                            }
                        }
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': {
                    u'Б': [
                        u'A',
                        u'B'
                    ],
                    u'А': [
                        u'A'
                    ],
                    u'В': u'B'
                }
            }
        }),
        Answer(markers={
            '1': {
                'user_answer': {u'А': ['A'], u'Б': ['B'], u'В': 'B'},
            },
        }, theory={}),
        Answer(
            markers={
                '1': {
                    'user_answer': {u'А': ['A'], u'Б': ['B'], u'В': 'B'},
                    'answer_status': {u'А': [Marker.CORRECT],
                                      u'Б': [Marker.CORRECT],
                                      u'В': Marker.CORRECT},
                    'mistakes': 1,
                    'max_mistakes': 3,
                },
            },
            mistakes=1,
            max_mistakes=3,
            theory={},
        )
    ),
    # задача с макаронами
    (
        Problem(markup={
            'layout': [
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'macaroni',
                        'id': 1,
                        'options': {
                            'left': {
                                'naming': 'abc',
                                'choices': [1, 2, 3],
                            },
                            'right': {
                                'naming': 'abc',
                                'choices': [4, 5, 6],
                            },
                        },
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': [
                    [[0, 0], [2, 2]],
                    [[0, 0], [1, 0], [2, 0]],
                ],
            }
        }),
        Answer(markers={
            '1': {
                'user_answer': [[0, 0], [2, 0], [1, 1]],
            },
        }, theory={}),
        Answer(
            markers={
                '1': {
                    'user_answer': [[0, 0], [2, 0], [1, 1]],
                    'answer_status': {
                        "edges_status": [1, 1, 0],
                        "missing_edges": [[1, 0]],
                        "compare_with_answer": 1,
                    },
                    'mistakes': 2,
                    'max_mistakes': 1,
                },
            },
            mistakes=2,
            max_mistakes=1,
            theory={},
        )
    ),
    # задача с соединением областей
    (
        Problem(markup={
            'layout': [
                {
                    'content': {
                        'text': u'Нужно что-то c чем-то посоединять',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'connectareas',
                        'id': 1,
                        'options': {
                            'color_map_file': 3605,
                            'line_width': '8.0',
                            'order_important': False,
                            'hit_areas': [
                                [
                                    '#0000FF',
                                    'b1'
                                ],
                                [
                                    '#00FF00',
                                    'b2'
                                ],
                                [
                                    '#00FFFF',
                                    'b3'
                                ],
                                [
                                    '#FF0000',
                                    'm1'
                                ],
                                [
                                    '#FF00FF',
                                    'm2'
                                ]
                            ],
                            'image_file': 3604,
                            'swap_enabled': True
                        }
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': [
                    [
                        [
                            {
                                'areaId': 'b1',
                                'points': [
                                    0.2381818181818182,
                                    0.32
                                ]
                            },
                            {
                                'areaId': 'm1',
                                'points': [
                                    0.24727272727272728,
                                    0.7145454545454546
                                ]
                            }
                        ],
                        [
                            {
                                'areaId': 'b2',
                                'points': [
                                    0.509090909090909,
                                    0.32545454545454544
                                ]
                            },
                            {
                                'areaId': 'm1',
                                'points': [
                                    0.7254545454545455,
                                    0.7509090909090909
                                ]
                            }
                        ],
                        [
                            {
                                'areaId': 'b3',
                                'points': [
                                    0.7418181818181818,
                                    0.31272727272727274
                                ]
                            },
                            {
                                'areaId': 'm2',
                                'points': [
                                    0.46545454545454545,
                                    0.84
                                ]
                            }
                        ]
                    ]
                ]
            }
        }),
        Answer(markers={
            '1': {
                'user_answer': [
                    [
                        {'areaId': 'm1'},
                        {'areaId': 'b2'},
                    ],
                    [
                        {'areaId': 'b1'},
                        {'areaId': 'b2'},
                    ],
                ],
            },
        }, theory={}),
        Answer(
            markers={
                '1': {
                    'user_answer': [
                        [
                            {'areaId': 'm1'},
                            {'areaId': 'b2'},
                        ],
                        [
                            {'areaId': 'b1'},
                            {'areaId': 'b2'},
                        ],
                    ],
                    'answer_status': {
                        'compared_with': 0,
                        'status': [Marker.CORRECT, Marker.INCORRECT],
                    },
                    'mistakes': 1,
                    'max_mistakes': 1,
                },
            },
            mistakes=1,
            max_mistakes=1,
            theory={},
        ),
    ),
    (
        Problem(markup={
            'layout': [
                {
                    'content': {
                        'text': u'Нужно что-то c чем-то посоединять',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'connectareas',
                        'id': 1,
                        'options': {
                            'color_map_file': 3605,
                            'line_width': '8.0',
                            'order_important': True,
                            'hit_areas': [
                                [
                                    '#0000FF',
                                    'b1'
                                ],
                                [
                                    '#00FF00',
                                    'b2'
                                ],
                                [
                                    '#00FFFF',
                                    'b3'
                                ],
                                [
                                    '#FF0000',
                                    'm1'
                                ],
                                [
                                    '#FF00FF',
                                    'm2'
                                ]
                            ],
                            'image_file': 3604,
                            'swap_enabled': True
                        }
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': [
                    [
                        [
                            {
                                'areaId': 'b1',
                                'points': [
                                    0.2381818181818182,
                                    0.32
                                ]
                            },
                            {
                                'areaId': 'm1',
                                'points': [
                                    0.24727272727272728,
                                    0.7145454545454546
                                ]
                            }
                        ],
                        [
                            {
                                'areaId': 'b2',
                                'points': [
                                    0.509090909090909,
                                    0.32545454545454544
                                ]
                            },
                            {
                                'areaId': 'm1',
                                'points': [
                                    0.7254545454545455,
                                    0.7509090909090909
                                ]
                            }
                        ],
                        [
                            {
                                'areaId': 'b3',
                                'points': [
                                    0.7418181818181818,
                                    0.31272727272727274
                                ]
                            },
                            {
                                'areaId': 'm2',
                                'points': [
                                    0.46545454545454545,
                                    0.84
                                ]
                            }
                        ]
                    ]
                ]
            }
        }),
        Answer(markers={
            '1': {
                'user_answer': [
                    [
                        {'areaId': 'b2'},
                        {'areaId': 'm1'},
                    ],
                    [
                        {'areaId': 'b1'},
                        {'areaId': 'm1'},
                    ],
                    [
                        {'areaId': 'b3'},
                        {'areaId': 'm2'},
                    ],
                ],
            },
        }, theory={}),
        Answer(
            markers={
                '1': {
                    'user_answer': [
                        [
                            {'areaId': 'b2'},
                            {'areaId': 'm1'},
                        ],
                        [
                            {'areaId': 'b1'},
                            {'areaId': 'm1'},
                        ],
                        [
                            {'areaId': 'b3'},
                            {'areaId': 'm2'},
                        ],
                    ],
                    'answer_status': {
                        'compared_with': 0,
                        'status': [
                            Marker.CORRECT,
                            Marker.CORRECT,
                            Marker.CORRECT,
                        ],
                    },
                    'mistakes': 0,
                    'max_mistakes': 1,
                },
            },
            mistakes=0,
            max_mistakes=1,
            theory={},
        )
    ),
    # задача на раскрашивание областей
    (
        Problem(markup={
            'layout': [
                {
                    'content': {
                        'text': u'Раскрась квадрат в три цвета так, чтобы в каждой строчке и в каждом стоблце встречались все цвета.',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'coloring',
                        'id': 1,
                        'options': {
                            'is_ordered': True,
                            'pallete_views': [
                                {
                                    'active_background': 148,
                                    'color_symbol': 'o',
                                    'y': 1,
                                    'background': 149,
                                    'width': 0.1,
                                    'color': '#ff0000',
                                    'x': 0.35,
                                    'height': 0.1
                                },
                                {
                                    'active_background': 146,
                                    'color_symbol': 'g',
                                    'y': 1,
                                    'background': 145,
                                    'width': 0.1,
                                    'color': '#00ff00',
                                    'x': 0.45,
                                    'height': 0.1
                                },
                                {
                                    'active_background': 147,
                                    'color_symbol': 'b',
                                    'y': 1,
                                    'background': 144,
                                    'width': 0.1,
                                    'color': '#0000ff',
                                    'x': 0.55,
                                    'height': 0.1
                                }
                            ],
                            'init_coloring_image_views': {
                                'height': 0.9,
                                'width': 0.9,
                                'check_points_group': [
                                    {
                                        'is_ordered': True,
                                        'check_points': [
                                            {
                                                'y': 0.5,
                                                'x': 0.25
                                            },
                                            {
                                                'y': 0.25,
                                                'x': 0.5
                                            }
                                        ]
                                    },
                                    {
                                        'is_ordered': True,
                                        'check_points': [
                                            {
                                                'y': 0.75,
                                                'x': 0.25
                                            },
                                            {
                                                'y': 0.25,
                                                'x': 0.75
                                            }
                                        ]
                                    },
                                    {
                                        'is_ordered': True,
                                        'check_points': [
                                            {
                                                'y': 0.75,
                                                'x': 0.5
                                            },
                                            {
                                                'y': 0.5,
                                                'x': 0.75
                                            }
                                        ]
                                    }
                                ],
                                'background': 143,
                                'y': 0.05,
                                'x': 0.05
                            }
                        }
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': [
                    [
                        'o',
                        'b'
                    ],
                    [
                        'b',
                        'o'
                    ],
                    [
                        'g',
                        'g'
                    ]
                ]
            }
        }),
        Answer(markers={
            '1': {
                'user_answer': [
                    ['b', 'b'],
                    ['b', 'o'],
                    ['g', 'g', 'g']
                ],
            },
        }, theory={}),
        Answer(
            markers={
                '1': {
                    'user_answer': [
                        ['b', 'b'],
                        ['b', 'o'],
                        ['g', 'g', 'g']
                    ],
                    'answer_status': Marker.INCORRECT,
                    'mistakes': 1,
                    'max_mistakes': 1,
                },
            },
            mistakes=1,
            max_mistakes=1,
            theory={},
        ),
    ),
    # Задача на сложный ввод
    (
        Problem(markup={
            'layout': [
                {
                    'content': {
                        'text': u'Задача на ввод. Напиши все правильно.',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'inline',
                        'id': 1,
                        'options': {
                            'text': u'Какой{input:4}то текст с{input:3} разными инпутами. Вставь пропуще{input:1}ые буквы. Ра{input:2}тавь буквы\n{input:5}',
                            'inputs': {
                                '1': {
                                    'group': 1,
                                    'type': 'field',
                                    'options': {
                                        'width': 2,
                                        'type_content': 'text',
                                    },
                                },
                                '2': {
                                    'group': 1,
                                    'type': 'choice',
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
                                    'group': 1,
                                    'type': 'comma',
                                    'options': {},
                                },
                                '4': {
                                    'group': 2,
                                    'type': 'separator',
                                    'options': {
                                        'choices': [
                                            'hyphen',
                                            'together',
                                        ],
                                    },
                                },
                                '5': {
                                    'group': 3,
                                    'type': 'field',
                                    'options': {
                                        'type_content': 'strict',
                                    },
                                },
                            },
                        },
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {
                '1': {
                    '1': {
                        'sources': [
                            {
                                'sources': [
                                    {
                                        'source': 1,
                                        'type': 'INPUT'
                                    },
                                    {
                                        'source': u'нн',
                                        'type': 'STRING'
                                    }
                                ],
                                'type': 'EQUAL'
                            },
                            {
                                'sources': [
                                    {
                                        'source': 2,
                                        'type': 'INPUT'
                                    },
                                    {
                                        'source': 3,
                                        'type': 'NUMBER'
                                    }
                                ],
                                'type': 'EQUAL'
                            },
                            {
                                'sources': [
                                    {
                                        'source': 3,
                                        'type': 'INPUT'
                                    },
                                    {
                                        'source': False,
                                        'type': 'BOOLEAN'
                                    }
                                ],
                                'type': 'EQUAL'
                            }
                        ],
                        'type': 'AND'
                    },
                    '2': {
                        'sources': [
                            {
                                'source': 4,
                                'type': 'INPUT'
                            },
                            {
                                'source': 0,
                                'type': 'NUMBER'
                            }
                        ],
                        'type': 'EQUAL',
                    },
                    '3': {
                        'sources': [
                            {
                                'source': 5,
                                'type': 'INPUT'
                            },
                            {
                                'source': u'Текст',
                                'type': 'STRING'
                            }
                        ],
                        'type': 'EQUAL',
                    }
                }
            },
            'answers': {
                '1': {
                    '1': u'нн',
                    '3': False,
                    '2': 0,
                    '4': 0,
                    '5': u'Текст',
                }
            }
        }),
        Answer(markers={
            '1': {
                'user_answer': {
                    '1': u'нн',
                    '2': 3,
                    '3': False,
                    '4': 0,
                    '5': u'Текст',
                },
            },
        }, theory={}),
        Answer(
            markers={
                '1': {
                    'user_answer': {
                        '1': u'нн',
                        '2': 3,
                        '3': False,
                        '4': 0,
                        '5': u'Текст',
                    },
                    'answer_status': {
                        '1': True,
                        '2': True,
                        '3': True,
                    },
                    'mistakes': 0,
                    'max_mistakes': 1,
                },
            },
            mistakes=0,
            max_mistakes=1,
            theory={},
        ),
    )
)


@pytest.mark.parametrize('problem,answer,expected_checked_answer',
                         CHECK_ANSWER_DATA)
def test_check_answer(problem, answer, expected_checked_answer):
    """
    Тест проверки ответа

    :param problem: вопрос, на который дан ответ
    :param answer: ответ пользователя
    :param expected_checked_answer: ожидаемый проверенный ответ
    """
    checked_answer = check_answer(problem, answer)
    if checked_answer.mistakes != expected_checked_answer.mistakes:
        import pdb; pdb.set_trace()
    assert checked_answer.mistakes == expected_checked_answer.mistakes, (
        u'Неправильное число ошибок у проверенного ответа')
    assert (checked_answer.max_mistakes ==
            expected_checked_answer.max_mistakes), (
        u'Неправильное максимально число ошибок у проверенного ответа')
    assert checked_answer.markers == expected_checked_answer.markers, (
        u'Неправильный словарь маркеров у проверенного ответа')
