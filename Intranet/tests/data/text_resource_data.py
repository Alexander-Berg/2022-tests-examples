text_resources_valid = (
    (
        'some text with one resource {resource:1} and no formulas',
        {},
        [1],
    ),
    (
        'some text with two resources {resource:1}, {resource:22}'
        'and a formula {formula:1}',
        {'1': 'a + b = c'},
        [1, 22],
    ),
    (
        'some text with two formulas {formula:1}, {formula:2}',
        {'1': 'a + b = c', '2': '(a + b)*c = ac + bc'},
        [],
    ),
    (
        'some text with repeating resource {resource:1} {resource:1}',
        {},
        [1],
    ),
    (
        'some text with repeating formulas {formula:1}, {formula:1}',
        {'1': 'a + b = c'},
        [],
    ),
    (
        'some text {resource:2} {resource:2}{resource:153}',
        {},
        [2, 153],
    ),
)

text_resources_with_invalid_resource_ids = (
    (
        'some text with one resource {resource:124}',
        [],
        'some resources do not exist: [124]',
    ),
    (
        'some text with two bad resources {resource:123}, '
        '{resource:124}',
        [],
        'some resources do not exist: [123, 124]'
    ),
    (
        'some text with repeating resource {resource:122} '
        '{resource:122}',
        [],
        'some resources do not exist: [122]'
    ),
    (
        'some text {resource:500} {resource:100}'
        '{resource:500}',
        [],
        'some resources do not exist: [100, 500]'
    ),
    (
        'some text with two bad resources {resource:123}, '
        '{resource:124} and one valid {resource:4}',
        [4],
        'some resources do not exist: [123, 124]'
    ),
)

text_resources_with_invalid_formula_ids = (
    (
        u'some text without formulas',
        {u'1': u'a + b = c'},
        u'Не найдены формулы: [], лишние формулы: [\'1\']',
    ),
    (
        u'some text with a missing formula {formula:1}',
        {u'2': u'a + b = c'},
        u'Не найдены формулы: [\'1\'], лишние формулы: [\'2\']',
    ),
    (
        u'some text with a missing formula {formula:1}',
        {},
        u'Не найдены формулы: [\'1\'], лишние формулы: []',
    ),
)

problem_markup_with_valid_objects_ids = (
    (
        {
            'layout': [
                {
                    'kind': 'text',
                    'content': {
                        'text': u'Любая строка с ресурсами {resource:2} '
                                u'{resource:1}',
                        'options': {
                            'style': 'normal',
                        },
                    },
                },
            ],
            'answers': {},
            'checks': {},
        },
        [1, 2],
    ),
    (
        {
            'layout': [
                {
                    'kind': 'text',
                    'content': {
                        'text': u'Любая строка \r\n с ресурсами {resource:2} '
                                u'{resource:1}',
                        'options': {
                            'style': 'normal',
                        },
                    },
                },
                {
                    'kind': 'text',
                    'content': {
                        'text': u'Любая строка с {resource:7} ресурсами '
                                u'{resource:2} {resource:1}',
                        'options': {
                            'style': 'normal',
                        },
                    },
                },
            ],
            'answers': {},
            'checks': {},
            'solution': u'{resource:12} текст',
        },
        [1, 2, 7, 12],
    ),
    (
        {
            'layout': [
                {
                    'kind': 'text',
                    'content': {
                        'text': u'Любая строка с ресурсами {resource:2} и '
                                u'формулой {formula:3}',
                        'options': {
                            'style': 'normal',
                        },
                    },
                },
                {
                    'kind': 'text',
                    'content': {
                        'text': u'Любая строка\n\n с {resource:1} ресурсами '
                                u'{resource:2} {resource:7}',
                        'options': {
                            'style': 'normal',
                        },
                    },
                },
            ],
            'formulas': {
                '2': {
                    'code': '$x + 3 = y$',
                    'url': 'qwerty',
                },
                '3': {
                    'code': '$y - 2 = x$',
                    'url': 'qwerty2',
                },
            },
            'answers': {},
            'checks': {},
            'solution': u'{resource:12} текст \r\n с формулой {formula:2}',
        },
        [1, 2, 7, 12],
    ),
    (
        {
            'layout': [
                {
                    'kind': 'text',
                    'content': {
                        'text': u'Любая строка с ресурсами {resource:2}',
                        'options': {
                            'style': 'normal',
                        },
                    },
                },
                {
                    'kind': 'marker',
                    'content': {
                        'id': 3,
                        'type': 'choice',
                        'options': {
                            'choices': ['1', '2'],
                        },
                    },
                },
                {
                    'kind': 'marker',
                    'content': {
                        'id': 4,
                        'type': 'field',
                        'options': {},
                    },
                },
                {
                    'kind': 'text',
                    'content': {
                        'text': u'{resource:7}Любая строка с {resource:1} '
                                u'ресурсами {resource:2}',
                        'options': {
                            'style': 'normal',
                        },
                    },
                },
            ],
            'answers': {
                '3': [],
                '4': '2',
            },
            'checks': {},
            'solution': u'{resource:12} текст',
        },
        [1, 2, 7, 12],
    ),
    (
        {
            'layout': [
                {
                    'kind': 'text',
                    'content': {
                        'text': u'Любая строка с ресурсами {resource:2}',
                        'options': {
                            'style': 'normal',
                        },
                    },
                },
                {
                    'kind': 'marker',
                    'content': {
                        'id': 4,
                        'type': 'matching',
                        'options': {
                            'keys': {
                                'naming': '',
                                'choices': [],
                            },
                            'values': {
                                'naming': '',
                                'choices': [],
                            },
                        },
                    },
                },
            ],
            'answers': {
                '4': {},
            },
            'checks': {},
        },
        [2],
    ),
    (
        {
            'layout': [
                {
                    'kind': 'text',
                    'content': {
                        'text': u'Любая строка',
                        'options': {},
                    },
                },
            ],
            'formulas': {
                '1': 'test'
            },
            'answers': {},
            'checks': {},
            'public_solution': u'{resource:1} {formula:1}',
        },
        [1],
    ),
    (
        {
            'layout': [
                {
                    'kind': 'text',
                    'content': {
                        'text': u'Любая строка',
                        'options': {},
                    },
                },
            ],
            'formulas': {
                '1': 'test'
            },
            'answers': {},
            'checks': {},
            'cm_comment': u'{resource:1} {formula:1}',
        },
        [1],
    ),
)

problem_markup_with_invalid_objects_ids = (
    (
        {
            'layout': [
                {
                    'content': {
                        'text': u'Любая строка с ресурсами {resource:2}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                }
            ],
            'checks': {},
            'answers': {}
        },
        [],
        [u'Не найдены ресурсы с идентификаторами: [\'2\']'],
    ),
    (
        {
            'layout': [
                {
                    'content': {
                        'text': u'Любая строка с ресурсами {resource:2}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'text': u'Любая строка с {resource:1} ресурсами '
                                u'{resource:2}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                }
            ],
            'checks': {},
            'answers': {},
            'solution': u'{resource:12} текст'
        },
        [2, 7, 11, 13],
        [u'Не найдены ресурсы с идентификаторами: [\'1\', \'12\']'],
    ),
    (
        {
            'layout': [
                {
                    'content': {
                        'text': u'Любая {marker:1} строка с ресурсами '
                                u'{resource:2}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                }
            ],
            'checks': {},
            'answers': {},
            'solution': '{formula:1}'
        },
        [1, 3],
        [u'Не найдены формулы в `formulas`: [\'1\'],\n'
         u'лишние формулы в `formulas`: []',
         u'Не найдены ресурсы с идентификаторами: [\'2\']'],
    ),
    (
        {
            'layout': [
                {
                    'content': {
                        'text': u'Любая {marker:1} строка {formula:2} с '
                                u'ресурсами {resource:2}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'field',
                        'id': 12,
                        'options': {}
                    },
                    'kind': 'marker'
                },
                {
                    'content': {
                        'type': 'field',
                        'id': 2,
                        'options': {}
                    },
                    'kind': 'marker'
                }
            ],
            'formulas': {
                '3': 'test'
            },
            'checks': {},
            'answers': {
                '12': '',
                '2': '32',
            },
            'solution': '{formula:1}'
        },
        [1, 5],
        [u'Не найдены формулы в `formulas`: [\'1\', \'2\'],\nлишние формулы '
         u'в `formulas`: [\'3\']',
         u'Не найдены ресурсы с идентификаторами: [\'2\']'],
    ),
    (
        {
            'layout': [
                {
                    'content': {
                        'text': u'Любая строка с ресурсами {resource:2}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'matching',
                        'id': 12,
                        'options': {
                            'keys': {
                                'naming': 'numbers',
                                'choices': [
                                    u'{resource:5}',
                                    u'Текст'
                                ]
                            },
                            'values': {
                                'naming': 'numbers',
                                'choices': [
                                    u'Еще текст',
                                    u'Формула {formula:22}'
                                ]
                            }
                        }
                    },
                    'kind': 'marker'
                },
                {
                    'content': {
                        'type': 'choice',
                        'id': 2,
                        'options': {
                            'choices': [
                                u'Ресурсы {resource:3} и формулы {formula:20}',
                                u'Вторая картинка {resource:4}',
                                u'И формула {formula:21}'
                            ]
                        }
                    },
                    'kind': 'marker'
                }
            ],
            'formulas': {
                '3': 'test'
            },
            'checks': {},
            'answers': {
                '2': [
                    0,
                    1
                ],
                '12': {}
            },
            'solution': '{formula:1}',
            'public_solution': '{formula:2} {resource:6}',
            'cm_comment': '{formula:4} {resource:7}'
        },
        [1],
        [u'Не найдены формулы в `formulas`: [\'1\', \'2\', \'20\', '
         u'\'21\', \'22\', \'4\'],\nлишние формулы в `formulas`: [\'3\']',
         u'Не найдены ресурсы с идентификаторами: [\'2\', \'3\', \'4\', '
         u'\'5\', \'6\', \'7\']'],
    ),
    (
        {
            'layout': [
                {
                    'content': {
                        'type': 'inline',
                        'id': 1,
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {'1': 3},
        },
        [],
        [u'В маркере 1 найдена ошибка: ["\'options\' is a required '
         u'property"]',
         u'Не найдена проверка для маркера 1'],
    ),
)
