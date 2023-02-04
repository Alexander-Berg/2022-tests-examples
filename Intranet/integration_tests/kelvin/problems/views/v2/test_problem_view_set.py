from builtins import object
import pytest

from django.contrib.auth import get_user_model
from django.core.urlresolvers import reverse
from django.utils.translation import ugettext_lazy as _

from kelvin.common.utils_for_tests import assert_has_error_message
from kelvin.problems.answers import Answer
from kelvin.problems.constants import BLOCK_MARKER_SCHEMA
from kelvin.problems.markers import Marker
from kelvin.problems.models import Problem
from kelvin.problem_meta.models import ProblemMeta, Skill
from kelvin.subjects.models import Subject, Theme

ALLOWED_MARKER_TYPES = (
    BLOCK_MARKER_SCHEMA['properties']['content']['properties']['type']['enum']
)

User = get_user_model()


@pytest.fixture
def models_data():
    """
    Создает объект метаинформации и связанные с ним объекты для создания
    других метаинформаций
    """
    subject = Subject.objects.create(name=u"Математика",
                                     slug='mathematics')
    subject_russian = Subject.objects.create(name=u"Русский язык",
                                             slug='russian')
    theme = Theme.objects.create(id=1, name=u"Арифметика", subject=subject)
    skill = Skill.objects.create(id=1, name=u"Счет до 10", subject=subject)
    meta = ProblemMeta.objects.create(difficulty=1, main_theme=theme)
    meta.skills = [skill]
    return subject, theme, skill, meta


@pytest.mark.django_db
class TestProblemViewSet(object):
    """
    Тесты рест-интерфейса задач
    """
    def test_retrieve(self, jclient, content_manager):
        """
        Тест получения задачи android-клиентом и неким другим
        """
        subject = Subject.objects.create(name=u'Физика')
        problem = Problem.objects.create(
            markup={'new': 'markup'},
            old_markup={'old': 'markup'},
            subject=subject,
            name=u'somename',
            visibility=Problem.VISIBILITY_PUBLIC,
            owner=content_manager,
        )
        retrieve_url = reverse('v2:problem-detail', args=(problem.id, ))

        jclient.login(user=content_manager)

        response = jclient.get(retrieve_url)
        assert response.status_code == 200, u'Неправильный статус ответа'
        answer = response.json()
        assert answer['markup'] == {'new': 'markup'}, (
            u'Должна вернуться актуальная разметка')
        assert answer['owner'] == {
            'id': content_manager.id,
            'is_content_manager': True,
            'is_teacher': False,
        }, u'Должно быть развернутое поле владельца'

    generated_text = (
        u'bq(littext).. Маленький старич{marker:1}к '
        u'в мундир{marker:2} с медал{marker:3}ю{marker:17} '
        u'пош{marker:4}л мне навстречу. Помня, что говорила '
        u'мама, я щ{marker:5}лкнул каблуками{marker:18} и '
        u'низко покл{marker:6}нился{marker:19} сняв за козырек '
        u'фура{marker:7}ку. «Здравствуй, здравствуй! – сказал '
        u'старич{marker:8}к. – Положь фураж{marker:9}чку вон '
        u'туда. В первый, поди?» Я тщательно{marker:20} и '
        u'почтительно{marker:21} покл{marker:10}нился ещ{marker:11} '
        u'раз. «Ну, иди, иди, накланялся!» – засмеялся '
        u'старич{marker:12}к{marker:22} и{marker:23} взяв из '
        u'угла щ{marker:13}тку{marker:24} пош{marker:14}л '
        u'подметать к{marker:15}р{marker:16}дор.\n\n%(right_string) '
        u'(Л.Кассиль)%\n'
    )

    post_problem_data = (
        {
            'markup': {
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
                            '{ -0{,}3 } = 0{,}4$.'
            },
            'custom_answer': False,
            'meta': 1,
            'visibility': 1,
            'max_points': 10,
        },
        {
            'markup': {
                'layout': [
                    {
                        'content': {
                            'text': u'Вычислите значение **выражения** '
                                    u'$-0{,}6:  5  : ( -0{,}3 )$.',
                            'options': {
                                'style': 'normal'
                            }
                        },
                        'kind': 'text'
                    },
                    {
                        'content': {
                            'type': 'field',
                            'id': 1,
                            'options': {
                                'type_content': 'number'
                            }
                        },
                        'kind': 'marker'
                    },
                ],
                'checks': {},
                'answers': {
                    '1': '4'
                },
                'solution': '$-0{,}6:  5  : ( -0{,}3 ) = \\dfrac{ -0{,}12 }'
                            '{ -0{,}3 } = 0{,}4$.'
            },
            'custom_answer': False,
            'meta': 1,
            'visibility': 1,
            'max_points': 10,
            'subject': 'russian',
        },
        {
            'markup': {
                'layout': [
                    {
                        'content': {
                            'text': u'Вычислите значение **выражения** '
                                    u'$-0{,}6:  5  : ( -0{,}3 )$.',
                            'options': {
                                'style': 'normal'
                            }
                        },
                        'kind': 'text'
                    },
                    {
                        'content': {
                            'type': 'field',
                            'id': 1,
                            'options': {
                                'type_content': 'number'
                            }
                        },
                        'kind': 'marker'
                    }
                ],
                'checks': {},
                'answers': {
                    '1': '4'
                },
                'solution': '$-0{,}6:  5  : ( -0{,}3 ) = \\dfrac{ -0{,}12 }'
                            '{ -0{,}3 } = 0{,}4$.'
            },
            'meta': {
                'difficulty': 1, 'main_theme': 1, 'skills': [1],
                'additional_themes': [], 'group_levels': [], 'exams': [],
            },
            'custom_answer': False,
            'expand_meta': True,
            'visibility': 2,
            'max_points': 10,
        },
        {
            'markup': {
                'layout': [
                    {
                        'content': {
                            'text': u'Вычислите значение **выражения** '
                                    u'$-0{,}6:  5  : ( -0{,}3 )$.',
                            'options': {
                                'style': 'normal'
                            }
                        },
                        'kind': 'text'
                    },
                    {
                        'content': {
                            'type': 'field',
                            'id': 1,
                            'options': {
                                'type_content': 'number'
                            }
                        },
                        'kind': 'marker'
                    }
                ],
                'checks': {},
                'answers': {
                    '1': '4'
                },
                'solution': '$-0{,}6:  5  : ( -0{,}3 ) = \\dfrac{ -0{,}12 }'
                            '{ -0{,}3 } = 0{,}4$.'
            },
            'custom_answer': False,
            'meta': {
                'difficulty': 1, 'main_theme': 1, 'skills': [1],
                'additional_themes': [], 'group_levels': [], 'exams': [],
            },
            'max_points': 10,
        },
        {
            'markup': {
                'layout': [
                    {
                        'content': {
                            'text': u'Вставьте пропущенные буквы и '
                                    u'знаки препинания.\n'
                                    u'Порадуйтесь картинке: !resource_name!',
                            'options': {
                                'style': 'normal'
                            }
                        },
                        'kind': 'text'
                    },
                    {
                        'content': {
                            'text': generated_text,
                            'options': {
                                'style': 'normal'
                            }
                        },
                        'kind': 'text'
                    },
                    {
                        'content': {
                            'type': 'matching',
                            'id': 24,
                            'options': {
                                'keys': {
                                    'naming': '',
                                    'choices': []
                                },
                                'values': {
                                    'naming': '',
                                    'choices': []
                                }
                            }
                        },
                        'kind': 'marker'
                    },
                    {
                        'content': {
                            'type': 'choice',
                            'id': 20,
                            'options': {
                                'choices': [
                                    'True',
                                    'False'
                                ]
                            }
                        },
                        'kind': 'marker'
                    },
                    {
                        'content': {
                            'type': 'choice',
                            'id': 21,
                            'options': {
                                'choices': [
                                    'True',
                                    'False'
                                ]
                            }
                        },
                        'kind': 'marker'
                    },
                    {
                        'content': {
                            'type': 'choice',
                            'id': 22,
                            'options': {
                                'choices': [
                                    'True',
                                    'False'
                                ]
                            }
                        },
                        'kind': 'marker'
                    },
                    {
                        'content': {
                            'type': 'choice',
                            'id': 23,
                            'options': {
                                'choices': [
                                    'True',
                                    'False'
                                ]
                            }
                        },
                        'kind': 'marker'
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
                                            'type_content': 'text',
                                        },
                                    },
                                },
                            }
                        },
                        'kind': 'marker'
                    },
                    {
                        'content': {
                            'type': 'choice',
                            'id': 12,
                            'options': {
                                'type_display': 'vertical',
                                'choices': [
                                    u'о',
                                    u'е'
                                ]
                            }
                        },
                        'kind': 'marker'
                    },
                    {
                        'content': {
                            'type': 'choice',
                            'id': 17,
                            'options': {
                                'choices': [
                                    'True',
                                    'False'
                                ]
                            }
                        },
                        'kind': 'marker'
                    },
                    {
                        'content': {
                            'type': 'choice',
                            'id': 19,
                            'options': {
                                'choices': [
                                    'True',
                                    'False'
                                ]
                            }
                        },
                        'kind': 'marker'
                    },
                    {
                        'content': {
                            'type': 'choice',
                            'id': 18,
                            'options': {
                                'choices': [
                                    'True',
                                    'False'
                                ]
                            }
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
                                    'type': 'STRING',
                                    'source': u'о',
                                },
                            ],
                        },
                    },
                },
                'answers': {
                    '24': {},
                    '20': [
                        False
                    ],
                    '21': [
                        False
                    ],
                    '22': [
                        False
                    ],
                    '23': [
                        False
                    ],
                    '1': {'1': u'о'},
                    '12': [
                        0
                    ],
                    '15': u'о',
                    '14': u'ё',
                    '17': [
                        False
                    ],
                    '16': u'и',
                    '19': [
                        False
                    ],
                    '18': [
                        False
                    ]
                }
            },
            'custom_answer': False,
            'max_points': 30,
        },
    )

    @pytest.mark.parametrize('case', post_problem_data)
    def test_create_problem(self, jclient, case, teacher, models_data):
        """
        Тест создания задачи
        """
        subject, theme, skill, meta = models_data
        create_url = reverse('v2:problem-list')

        # TODO авторизация в тестах
        # берем первый экземпляр задачи и пытаемся сохранить
        # проверяем, что надо быть авторизованным
        response = jclient.post(create_url, case)
        assert response.status_code == 401

        expand_meta = case.pop('expand_meta', None)
        if expand_meta:
            create_url += "?expand_meta=True"

        if isinstance(case.get('meta'), int):
            # заменяем идентификатор метаинформации на реальный
            case['meta'] = meta.id

        jclient.login(user=teacher)
        response = jclient.post(create_url, case)
        assert response.status_code == 201, (
            u'Должны успешно создать задачу, ответ: {0}'.format(
                response.json()))
        answer = response.json()
        assert 'id' in answer, u'Нет идентификатора задачи в ответе'
        assert 'date_updated' in answer, u'Нет даты обновления в ответе'
        assert 'resources' in answer, u'Нет поля ресурсов в ответе'
        id_ = answer.pop('id')
        answer.pop('date_updated')
        assert answer.pop('resources') == {}

        # Метаинформацию проверим отдельно
        if 'meta' in case:
            assert 'meta' in answer, u"Нет метаинформации в ответе"
            if isinstance(case['meta'], int):
                assert case['meta'] == answer['meta']
            else:
                assert answer['meta'].pop('id', None), (
                    u"Нет id метаинформации в ответе")
                assert answer['meta'].pop('date_updated', None), (
                    u"Нет даты-времени обновления метаинформации в ответе")

                if expand_meta:
                    # TODO переписать все тесты EDU-569
                    assert answer['meta'] == case['meta'], (
                        u"Данные метаинформации в ответе не совпадают "
                        u"с данными запроса"
                    )
                else:
                    assert answer['meta'] == {}, (
                        u"При отсутствии `expand_meta` в запросе "
                        u"метаинформация в ответе ограничивается полями `id` и"
                        u" `date_updated`"
                    )
            case.pop('meta')
            answer.pop('meta')

        # visibility по умолчанию — VISIBILITY_PRIVATE
        if 'visibility' not in case:
            assert answer['visibility'] == Problem.VISIBILITY_PRIVATE
            answer.pop('visibility')

        # удаляем \r, в модели тоже уже должен быть удаленным
        if 'containers' in case['markup']:
            first_container = case['markup']['containers'][0]
            first_container['content'] = first_container['content'].replace('\r', '')

        # по умолчанию должен проставиться предмет математика
        if 'subject' not in case:
            case['subject'] = 'mathematics'

        assert answer.pop('owner') == teacher.id
        assert answer == case

    post_problem_data_invalid = (
        (
            {},
            {'markup': [_('This field is required.')]}
        ),
        (
            {
                'markup': {},
            },
            {'markup': ['\'layout\' is a required property']},
        ),
        (
            {
                'markup': {
                    'layout': [],
                },
            },
            {'markup': ['\'answers\' is a required property']},
        ),
        (
            {
                'markup': {
                    'layout': [],
                    'answers': {},
                },
            },
            {'markup': ['\'checks\' is a required property']},
        ),
        (
            {
                'markup': {
                    'layout': [{'kind': 'text', 'content': {'text': '', 'options': {}}}],
                    'answers': {},
                    'checks': {},
                },
                'subject': 'biology',
            },
            {'subject': ['does not exist']},
        ),
        # tests for `layout` field
        (
            {
                'markup': {
                    'layout': {},
                    'answers': {},
                    'checks': {},
                },
                'subject': 'biology',
            },
            {'markup': ['{} is not of type \'array\'']},
        ),
        # tests for `formulas` field
        # TODO tests for formulas field
        (
            {
                'markup': {
                    'layout': [{'kind': 'text', 'content': {'text': '', 'options': {}}}],
                    'answers': {},
                    'checks': {},
                    'formulas': {
                        'key': {},
                    },
                },
            },
            {'markup': ["'key' does not match any of the regexes: '^\\\\d+$'"]}
        ),
        (
            {
                'markup': {
                    'layout': [{'kind': 'text', 'content': {'text': '', 'options': {}}}],
                    'answers': {},
                    'checks': {},
                    'formulas': {
                        '123': {},
                    },
                },
            },
            {'markup': ['\'code\' is a required property']},
        ),
        (
            {
                'markup': {
                    'layout': [{'kind': 'text', 'content': {'text': '', 'options': {}}}],
                    'answers': {},
                    'checks': {},
                    'formulas': {
                        '222': {'code': 222},
                    },
                },
            },
            {'markup': ['222 is not of type \'string\'']},
        ),
        (
            {
                'markup': {
                    'layout': [{'kind': 'text', 'content': {'text': '', 'options': {}}}],
                    'answers': {},
                    'checks': {},
                    'formulas': {
                        '222': {
                            'code': '22 + 11',
                            'url': [],
                        },
                    },
                },
            },
            {'markup': ['[] is not of type \'string\'']},
        ),
        (
            {
                'markup': {
                    'layout': [{'kind': 'text', 'content': {'text': '', 'options': {}}}],
                    'answers': {},
                    'checks': {},
                    'formulas': {
                        '222': {
                            'code': '22 + 11',
                            'unexpected': [],
                        },
                    },
                },
            },
            {'markup': ['Additional properties are not allowed (\'unexpected\' was unexpected)']}
        ),
        # тесты на наличие формул и маркеров
        (
            {
                'markup': {
                    'formulas': {
                        '124': {
                            'code': '$x+2=r'
                        }
                    },
                    'layout': [
                        {
                            'content': {
                                'text': u'Текст задачи',
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
            },
            {'markup': [u'Не найдены формулы в `formulas`: [],\nлишние формулы в `formulas`: [\'124\']']}
        ),
        (
            {
                'markup': {
                    'formulas': {
                        '3': {
                            'code': '$x+2=r'
                        }
                    },
                    'layout': [
                        {
                            'content': {
                                'text': u'Текст задачи с маркерами{marker:1}',
                                'options': {
                                    'style': 'normal'
                                }
                            },
                            'kind': 'text'
                        },
                        {
                            'content': {
                                'text': u'Task {marker:4}{formula:3}',
                                'options': {
                                    'style': 'normal'
                                }
                            },
                            'kind': 'text'
                        },
                        {
                            'content': {
                                'type': 'choice',
                                'id': 2,
                                'options': {}
                            },
                            'kind': 'marker'
                        }
                    ],
                    'checks': {},
                    'answers': {
                        '2': []
                    },
                    'solution': 'Решение {formula:1}'
                },
            },
            {'markup': [
                u'В маркере 2 найдена ошибка: ["\'choices\' is a required property"]',  # noqa
                u'Не найдены формулы в `formulas`: [\'1\'],\nлишние формулы в `formulas`: []']}  # noqa
        ),
        (
            {
                'markup': {
                    'formulas': {
                        '3': {
                            'code': '$x+2=r'
                        }
                    },
                    'layout': [
                        {
                            'content': {
                                'text': u'Текст задачи {resource:666} с '
                                        u'маркерами{marker:1}',
                                'options': {
                                    'style': 'normal'
                                }
                            },
                            'kind': 'text'
                        },
                        {
                            'content': {
                                'text': u'Task {marker:4}{formula:3}',
                                'options': {
                                    'style': 'normal'
                                }
                            },
                            'kind': 'text'
                        },
                        {
                            'content': {
                                'type': 'choice',
                                'id': 2,
                                'options': {}
                            },
                            'kind': 'marker'
                        }
                    ],
                    'checks': {},
                    'answers': {
                        '2': []
                    },
                    'solution': 'Решение {formula:1}'
                },
            },
            {'markup': [u'В маркере 2 найдена ошибка: ["\'choices\' is a required property"]',  # noqa
                        u'Не найдены формулы в `formulas`: [\'1\'],\nлишние формулы в `formulas`: []',  # noqa
                        u'Не найдены ресурсы с идентификаторами: [\'666\']']}
        ),
    )

    @pytest.mark.parametrize('post_data,error', post_problem_data_invalid)
    def test_create_problem_invalid(self, jclient, post_data, error,
                                    teacher):
        """
        Проверяем сообщения об ошибках при неправильном формате задачи
        """
        create_url = reverse('v2:problem-list')
        post_data = dict(post_data, owner=teacher.id)
        jclient.login(user=teacher)
        response = jclient.post(create_url, post_data)
        assert response.status_code == 400, u'Должна возникнуть ошибка'

        response_dict = response.json()

        for source, messages in list(error.items()):
            for message in messages:
                assert_has_error_message(
                    response_dict,
                    source=source,
                    message=message,
                )

    @pytest.mark.parametrize('case', post_problem_data)
    def test_create_without_owner(self, jclient, case, models_data,
                                  teacher):
        """
        Проверяем, что у задачи проставляется владелец - автор запроса
        """
        subject, theme, skill, meta = models_data
        if isinstance(case.get('meta'), int):
            # заменяем идентификатор метаинформации на реальный
            case['meta'] = meta.id

        create_url = reverse('v2:problem-list')

        jclient.login(user=teacher)
        response = jclient.post(create_url, case)
        assert response.status_code == 201
        assert response.json()['owner'] == teacher.id

    def test_patch_problem_markup(self, jclient, content_manager):
        """
        Частичное обновление разметки задачи
        """
        subject = Subject.objects.create(name=u'Физика')
        problem_equal_markups = Problem.objects.create(
            markup={'layout': [], 'answers': {}, 'checks': {}},
            old_markup={'layout': [], 'answers': {}, 'checks': {}},
            subject=subject,
            name=u'somename',
            visibility=Problem.VISIBILITY_PUBLIC,
            owner=content_manager,
        )
        jclient.login(user=content_manager)

        patch_data = {
            'markup': {
                'layout': [
                    {'kind': 'text', 'content': {'text': 'test', 'options': {}}}
                ],
                'answers': {},
                'checks': {}
            },
        }
        patch_url = reverse(
            'v2:problem-detail', args=(problem_equal_markups.id, ))

        response = jclient.patch(patch_url, patch_data)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        answer = response.json()
        assert answer['markup'] == patch_data['markup']
        problem_equal_markups.refresh_from_db()
        assert problem_equal_markups.markup == patch_data['markup']
        assert problem_equal_markups.old_markup == patch_data['markup']

    answer_data = (
        # ответ на задачу с полем ввода
        (
            {
                'markup': {
                    'layout': [
                        {
                            'content': {
                                'text': u'Какой-то текст',
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
                                }
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
                                }
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
                },
            },
            {
                '1': {'user_answer': {'1': '3.0'}},
                '2': {'user_answer': {'1': 'qwert'}},
            },
            {
                'markers': {
                    '1': {
                        'answer_status': {'1': True},
                        'user_answer': {'1': '3.0'},
                        'status': Marker.CORRECT,
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                    '2': {
                        'answer_status': {'1': False},
                        'user_answer': {'1': 'qwert'},
                        'status': Marker.INCORRECT,
                        'mistakes': 1,
                        'max_mistakes': 1,
                    },
                },
                'status': Answer.INCORRECT,
                'completed': True,
                'spent_time': None,
                'points': None,
                'comment': '',
                'answered': False,
            },
        ),
        # ответ на задачу с полем выбора
        (
            {
                'markup': {
                    'layout': [
                        {
                            'content': {
                                'text': u'{marker:2}',
                                'options': {
                                    'style': 'normal'
                                }
                            },
                            'kind': 'text'
                        },
                        {
                            'content': {
                                'type': 'choice',
                                'id': 2,
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
                        '2': [
                            0
                        ]
                    }
                },
            },
            {
                '2': {'user_answer': [2, 3]},
            },
            {
                'markers': {
                    '2': {
                        'answer_status': [Marker.INCORRECT, Marker.INCORRECT],
                        'user_answer': [2, 3],
                        'status': Marker.INCORRECT,
                        'mistakes': 3,
                        'max_mistakes': 3,
                    },
                },
                'status': Answer.INCORRECT,
                'completed': True,
                'spent_time': None,
                'points': None,
                'comment': '',
                'answered': False,
            },
        ),
        # ответ на задачу с сопоставлением
        (
            {
                'markup': {
                    'layout': [
                        {
                            'content': {
                                'text': u'{marker:2}',
                                'options': {
                                    'style': 'normal'
                                }
                            },
                            'kind': 'text'
                        },
                        {
                            'content': {
                                'type': 'matching',
                                'id': 2,
                                'options': {
                                    'keys': {
                                        'naming': 'abcLocal',
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
                                        'naming': 'abcLocal',
                                        'choices': [
                                            {
                                                'value': u'Зайцы',
                                                'key': 'A'
                                            },
                                            {
                                                'value': u'Собаки',
                                                'key': 'B'
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
                        '2': {
                            u'Б': [
                                'A',
                                'B'
                            ],
                            u'А': [
                                'A'
                            ],
                            u'В': 'B'
                        }
                    }
                },
            },
            {
                '2': {'user_answer': {u'Б': [u'B'], u'В': u'B'}},
            },
            {
                'markers': {
                    '2': {
                        'answer_status': {u'Б': [Marker.CORRECT],
                                          u'В': Marker.CORRECT},
                        'user_answer': {u'Б': [u'B'], u'В': u'B'},
                        'status': Marker.INCORRECT,
                        'mistakes': 2,
                        'max_mistakes': 3,
                    },
                },
                'status': Answer.INCORRECT,
                'completed': True,
                'spent_time': None,
                'points': None,
                'comment': '',
                'answered': False,
            },
        ),
    )

    @pytest.mark.xfail
    @pytest.mark.parametrize('problem_data,answer_data,attempt', answer_data)
    def test_answer(self, jclient, content_manager, problem_data, answer_data,
                    attempt, subject_model):
        """
        Проверяем ручку ответа
        """
        problem = Problem(owner=content_manager, subject=subject_model,
                          **problem_data)
        problem.save()
        answer_url = reverse('v2:problem-answer', args=[problem.id])
        jclient.login(user=content_manager)
        response = jclient.post(answer_url, answer_data)
        assert response.status_code == 200, u'Неправильный код ответа'
        response_json = response.json()
        assert 'markup' in response_json, u'Должен быть вопрос в ответе'
        assert 'attempt' in response_json, u'Должна быть попытка в ответе'
        assert response_json['attempt'] == attempt, (
            u'Неправильно сериализована попытка')

    answer_with_hide_answers_data = (
        (

            {
                'markup': {
                    'layout': [
                        {
                            'content': {
                                'text': u'{marker:2}',
                                'options': {
                                    'style': 'normal'
                                }
                            },
                            'kind': 'text'
                        },
                        {
                            'content': {
                                'type': 'choice',
                                'id': 2,
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
                        '2': [
                            0
                        ]
                    }
                },
            },
            {
                '2': {'user_answer': [2, 3]},
            },
            {
                'markers': {
                    '2': {
                        'user_answer': [2, 3],
                    },
                },
                'completed': True,
                'spent_time': None,
                'answered': False,
            },
        ),
    )

    @pytest.mark.xfail
    @pytest.mark.parametrize('problem_data,answer_data,attempt',
                             answer_with_hide_answers_data)
    def test_answer_with_hide_answers(self, jclient, content_manager,
                                      problem_data, answer_data, attempt,
                                      subject_model):
        """
        Проверяем ручку ответа с параметром `hide_answers`
        """
        problem = Problem(owner=content_manager, subject=subject_model,
                          **problem_data)
        problem.save()
        answer_url = (reverse('v2:problem-answer', args=[problem.id])
                      + '?hide_answers=1')
        jclient.login(user=content_manager)
        response = jclient.post(answer_url, answer_data)
        assert response.status_code == 200, u'Неправильный код ответа'
        response_json = response.json()
        assert 'markup' in response_json, u'Должен быть вопрос в ответе'
        assert 'answers' not in response_json['markup'], u'Не должно быть ответов'
        assert 'checks' not in response_json['markup'], u'Не должно быть проверок'
        assert 'attempt' in response_json, u'Должна быть попытка в ответе'
        assert response_json['attempt'] == attempt, (
            u'Неправильно сериализована попытка')

    def test_ordering_list(self, jclient, content_manager, subject_model):
        """Проверяет возможность сортировки списка задач"""
        problem1 = Problem.objects.create(
            owner=content_manager,
            subject=subject_model,
            markup={},
        )
        problem2 = Problem.objects.create(
            owner=content_manager,
            subject=subject_model,
            markup={},
        )
        problem3 = Problem.objects.create(
            owner=content_manager,
            subject=subject_model,
            markup={},
        )

        # стандартный порядок
        list_url = reverse('v2:problem-list')
        jclient.login(user=content_manager)
        response = jclient.get(list_url)
        assert response.status_code == 200, u'Неправильный код ответа'
        response_json = response.json()
        assert [problem['id'] for problem in response_json['results']] == [
            problem1.id, problem2.id, problem3.id]

        # порядок от новых к старым
        response = jclient.get(list_url + '?ordering=-date_created')
        assert response.status_code == 200, u'Неправильный код ответа'
        response_json = response.json()
        assert [problem['id'] for problem in response_json['results']] == [
            problem3.id, problem2.id, problem1.id]

    def test_content_manager_list(self, jclient, content_manager, teacher,
                                  subject_model):
        """
        Проверяет, что контент-менеджеру отдаются задачи только
        контент-менеджеров
        """
        problem1 = Problem.objects.create(
            owner=content_manager,
            subject=subject_model,
            markup={},
        )
        problem2 = Problem.objects.create(
            owner=content_manager,
            subject=subject_model,
            markup={},
        )
        problem3 = Problem.objects.create(
            owner=content_manager,
            subject=subject_model,
            markup={},
        )
        problem4 = Problem.objects.create(
            owner=teacher,
            subject=subject_model,
            markup={},
        )
        another_content_manager = User.objects.create(
            username='CM',
            email='cm@education-team.ru',
            is_content_manager=True,
        )

        list_url = reverse('v2:problem-list')
        jclient.login(user=another_content_manager)
        response = jclient.get(list_url)
        assert response.status_code == 200, u'Неправильный код ответа'
        response_json = response.json()
        assert [problem['id'] for problem in response_json['results']] == [
            problem1.id, problem2.id, problem3.id]
