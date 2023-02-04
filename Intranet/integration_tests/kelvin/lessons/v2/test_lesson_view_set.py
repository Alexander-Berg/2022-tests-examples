from builtins import str, range, object
import pytest
import time

from django.contrib.auth import get_user_model
from django.core.urlresolvers import reverse

from kelvin.common.utils import make_timestamp
from kelvin.lessons.models import LessonProblemLink, LessonScenario

User = get_user_model()


@pytest.mark.xfail
@pytest.mark.django_db
class TestLessonViewSet(object):
    """
    Тесты апи занятий
    """
    def test_answer(self, lesson_models, jclient):
        """
        Проверка ответа на занятие
        """
        lesson, problem1, problem2, link1, link2 = lesson_models
        jclient.login()
        user_answers = {
            link1.id: {
                '1': {'user_answer': {'1': '4.0'}},
            },
            link2.id: {
                '1': {'user_answer': [0, 1]},
            }
        }
        expected = {
            'id': lesson.id,
            'owner': lesson.owner.id,
            'scenario': None,
            'methodology': [],
            'problems': [
                {
                    'id': link1.id,
                    'type': 1,
                    'problem': {'id': problem1.id},
                    'options': {
                        'show_tips': True,
                        'max_attempts': 5,
                    },
                    'theory': None,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                },
                {
                    'id': link2.id,
                    'type': 1,
                    'problem': {'id': problem2.id},
                    'options': {
                        'show_tips': True,
                        'max_attempts': 5,
                    },
                    'theory': None,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                },
            ],
            'attempt': {
                'answers': {
                    str(link1.id): [{
                        'markers': {
                            '1': {
                                'answer_status': {'1': True},
                                'user_answer': {'1': '4.0'},
                                'status': 1,
                                'mistakes': 0,
                                'max_mistakes': 1,
                            },
                        },
                        'status': 1,
                        'completed': True,
                        'spent_time': None,
                        'points': 20,
                        'comment': '',
                        'answered': False,
                    }],
                    str(link2.id): [{
                        'markers': {
                            '1': {
                                'answer_status': [0, 1],
                                'user_answer': [0, 1],
                                'status': 0,
                                'mistakes': 2,
                                'max_mistakes': 3,
                            },
                        },
                        'status': 0,
                        'completed': True,
                        'spent_time': None,
                        'points': 0,
                        'comment': '',
                        'answered': False,
                    }],
                },
                'points': 20,
                'max_points': 40,
                'spent_time': None,
                'lesson': lesson.id,
                'student': jclient.user.id,
                'work_out': False,
                'completed': True,
            },
        }

        answer_url = reverse('v2:lesson-answer', args=(lesson.id,))
        response = jclient.post(answer_url, user_answers)
        assert response.status_code == 201, (
            u'Неверный статус ответа, ответ: {0}'.format(response.json()))
        answer = response.json()
        assert answer['attempt'].pop('id'), (
            u'У попытки должен быть идентификатор')
        assert answer.pop('date_updated'), (
            u'У урока должна быть дата изменения')
        assert answer.pop('name'), u'У урока должно быть название'
        assert answer.pop('theme'), u'У урока должна быть тема'
        assert answer['attempt'].pop('date_created'), (
            u'У попытки должна быть дата создания')
        assert answer['attempt'].pop('date_updated'), (
            u'У попытки должна быть дата изменения')
        assert len(answer['problems']) == 2, u'В занятии 2 задачи'
        assert answer['problems'][0]['problem'].pop('date_updated'), (
            u'у первой задачи должно быть указано время изменения')
        assert answer['problems'][1]['problem'].pop('date_updated'), (
            u'у второй задачи должно быть указано время изменения')
        assert answer == expected, u'Неправильный ответ'

    def test_answer_with_hide_answers(self, lesson_models, jclient):
        """
        Проверка ответа на занятие со скрытием правильных ответов и
        с раскрытием всех вопросов
        """
        lesson, problem1, problem2, link1, link2 = lesson_models
        jclient.login()
        user_answers = {
            link1.id: {
                '1': {'user_answer': {'1': '4.0'}},
            },
            link2.id: {
                '1': {'user_answer': [0, 1]},
            }
        }
        expected_without_problems = {
            'id': lesson.id,
            'owner': lesson.owner.id,
            'scenario': None,
            'methodology': [],
            'attempt': {
                'answers': {
                    str(link1.id): [{
                        'markers': {
                            '1': {
                                'user_answer': {'1': '4.0'},
                            },
                        },
                        'completed': True,
                        'spent_time': None,
                        'answered': False,
                    }],
                    str(link2.id): [{
                        'markers': {
                            '1': {
                                'user_answer': [0, 1],
                            },
                        },
                        'completed': True,
                        'spent_time': None,
                        'answered': False,
                    }],
                },
                'spent_time': None,
                'lesson': lesson.id,
                'student': jclient.user.id,
                'work_out': False,
                'max_points': 40,
                'completed': True,
            },
        }

        answer_url = (reverse('v2:lesson-answer', args=(lesson.id,))
                      + '?hide_answers=1&expand_problems=1')
        response = jclient.post(answer_url, user_answers)
        assert response.status_code == 201, (
            u'Неверный статус ответа, ответ: {0}'.format(response.json()))
        answer = response.json()
        assert answer['attempt'].pop('id'), (
            u'У попытки должен быть идентификатор')
        assert answer.pop('date_updated'), (
            u'У урока должна быть дата изменения')
        assert answer.pop('name'), u'У урока должно быть название'
        assert answer.pop('theme'), u'У урока должна быть тема'
        assert answer['attempt'].pop('date_created'), (
            u'У попытки должна быть дата создания')
        assert answer['attempt'].pop('date_updated'), (
            u'У попытки должна быть дата изменения')
        problems = answer.pop('problems')
        assert len(problems) == 2, u'В занятии 2 задачи'
        for problem in problems:
            assert 'answers' not in problem['problem']['markup']
            assert 'checks' not in problem['problem']['markup']
        assert answer == expected_without_problems, u'Неправильный ответ'

    def test_retrieve_short(self, lesson_with_theory_models, jclient):
        """
        Тест получения занятия только с идентификаторами задач
        """
        (lesson, problem1, theory, problem2,
         link1, link2, link3) = lesson_with_theory_models
        get_url = reverse('v2:lesson-detail', args=(lesson.id,))

        response = jclient.get(get_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert set(answer.keys()) == {'id', 'problems', 'scenario',
                                      'date_updated', 'owner', 'name',
                                      'theme', 'methodology'}, (
            u'Неправильный набор полей в ответе')
        assert isinstance(answer['date_updated'], int), (
            u'Дата изменения указана в юникстайме')
        assert answer['id'] == lesson.id, u'Неправильный идентификатор занятия'
        assert len(answer['problems']) == 3, u'Должно быть 2 задачи и 1 теория'
        for problem_scenario_data in answer['problems']:
            assert set(problem_scenario_data.keys()) == {
                'id', 'type', 'options', 'problem', 'theory'}, (
                u'Неправильные поля у сценария вопроса внутри занятия')

            if problem_scenario_data['type'] == LessonProblemLink.TYPE_COMMON:
                data = problem_scenario_data['problem']
                assert problem_scenario_data['theory'] is None, (
                    u'Должен быть пустой ключ теории')
                assert problem_scenario_data['options'] == {
                    'max_attempts': 5, 'show_tips': True}
            else:
                data = problem_scenario_data['theory']
                assert problem_scenario_data['problem'] is None, (
                    u'Должен быть пустой ключ задачи')

            assert set(data.keys()) == {'id', 'date_updated'}, (
                u'У задач должны быть указаны только идентификатор и "версия"')

    @pytest.mark.parametrize('expand_param', (1, True, 'true'))
    def test_retrieve_expand(self, lesson_with_theory_models, jclient, 
                             expand_param):
        """
        Тест получения занятия с полными задачами
        """
        (lesson, problem1, theory, problem2,
         link1, link2, link3) = lesson_with_theory_models
        get_url = reverse('v2:lesson-detail', args=(lesson.id,))

        response = jclient.get(get_url, {'expand_problems': str(expand_param)})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert set(answer.keys()) == {'id', 'problems', 'scenario',
                                      'date_updated', 'owner', 'name',
                                      'theme', 'methodology'}, (
            u'Неправильный набор полей в ответе')
        assert isinstance(answer['date_updated'], int), (
            u'Дата изменения указана в юникстайме')
        assert answer['id'] == lesson.id, u'Неправильный идентификатор занятия'
        assert len(answer['problems']) == 3, u'Должно быть 2 задачи и 1 теория'
        for problem_scenario_data in answer['problems']:
            assert set(problem_scenario_data.keys()) == {
                'id', 'type', 'options', 'problem', 'theory'}, (
                u'Неправильные поля у сценария вопроса внутри занятия')

            if problem_scenario_data['type'] == LessonProblemLink.TYPE_COMMON:
                data = problem_scenario_data['problem']
                assert problem_scenario_data['theory'] is None, (
                    u'Должен быть пустой ключ теории')

                assert problem_scenario_data['options'] == {
                    'max_attempts': 5, 'show_tips': True}
                assert set(data.keys()) == {
                    'id', 'date_updated', 'owner', 'markup', 'resources',
                    'visibility', 'max_points', 'subject'}, (
                    u'Должны быть указаны все поля сериализатора задачи')
                assert 'public_solution' in data['markup']
                assert 'solution' in data['markup']
            else:
                data = problem_scenario_data['theory']
                assert problem_scenario_data['problem'] is None, (
                    u'Должен быть пустой ключ задачи')

                assert problem_scenario_data['options'] == {}
                assert set(data.keys()) == {
                    'id', 'date_updated', 'name', 'owner', 'content_type',
                    'content', 'resources', 'themes', 'formulas'}, (
                    u'Должны быть все поля сериализатора текстового ресурса'
                )

    @pytest.mark.parametrize('expand_param,hide_param',
                             ((1, 1), (True, 'any'), ('true', True)))
    def test_retrieve_expand_with_hide_answers(self, lesson_models, jclient,
                                               expand_param, hide_param):
        """
        Тест получения занятия с полными задачами без ответов
        """
        lesson, problem1, problem2, __, __ = lesson_models
        get_url = reverse('v2:lesson-detail', args=(lesson.id,))

        response = jclient.get(get_url, {'expand_problems': str(expand_param),
                                         'hide_answers': str(hide_param)})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert set(answer.keys()) == {'id', 'problems', 'scenario',
                                      'date_updated', 'owner', 'name',
                                      'theme', 'methodology'}, (
            u'Неправильный набор полей в ответе')
        assert isinstance(answer['date_updated'], int), (
            u'Дата изменения указана в юникстайме')
        assert answer['id'] == lesson.id, u'Неправильный идентификатор занятия'
        assert len(answer['problems']) == 2, u'Должно быть  2 задачи'
        for problem_scenario_data in answer['problems']:
            assert set(problem_scenario_data.keys()) == {
                'id', 'type', 'options', 'problem', 'theory'}, (
                u'Неправильные поля у сценария вопроса внутри занятия')
            problem_data = problem_scenario_data['problem']
            assert set(problem_data.keys()) == {
                'id', 'date_updated', 'owner', 'markup', 'resources',
                'visibility', 'max_points', 'subject'}, (
                u'У задач должны быть указаны только идентификатор и "версия"')
            assert 'public_solution' not in problem_data['markup']
            assert 'solution' not in problem_data['markup']
            assert 'answers' not in problem_data['markup']
            assert 'checks' not in problem_data['markup']

    def test_create_with_empty_scenario(self, jclient, teacher, problem_models,
                                        theory_model, theme_model):
        """
        Проверяем создание без сценария
        """
        problem1, problem2 = problem_models
        create_url = reverse('v2:lesson-list')
        jclient.login(user=teacher)
        create_data = {
            'theme': theme_model.id,
            'problems': [
                {
                    'type': 1,
                    'problem': problem2.id,
                    'options': {
                        'show_tips': False,
                        'max_attempts': 5,
                    },
                },
                {
                    'type': 1,
                    # так тоже можно задавать идентификатор
                    'problem': {'id': problem1.id},
                    # можно не задавать `options`, будут дефолтные значения
                },
                {
                    'type': 3,
                    'problem': None,
                    'theory': theory_model.id,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                    'options': None,
                },
            ],
            'scenario': {},
        }
        expected_problems = [
            {
                'type': 1,
                'problem': {
                    'id': problem2.id,
                    'date_updated': make_timestamp(problem2.date_updated),
                },
                'theory': None,
                'options': {
                    'show_tips': False,
                    'max_attempts': 5,
                },
            },
            {
                'type': 1,
                'problem': {
                    'id': problem1.id,
                    'date_updated': make_timestamp(problem1.date_updated),
                },
                'options': {
                    'show_tips': True,
                    'max_attempts': 5,
                },
                'theory': None,
                'block_id': None,
                'start_date': None,
                'finish_date': None,
            },
            {
                'type': 3,
                'problem': None,
                'options': None,
                'theory': {
                    'id': theory_model.id,
                    'date_updated': make_timestamp(theory_model.date_updated),
                },
                'block_id': None,
                'start_date': None,
                'finish_date': None,
            },
        ]
        expected_scenario = {
            'duration': None,
            'mode': 1,
            'show_answers_in_last_attempt': True,
            'max_attempts_in_group': 2,
            'show_all_problems': True,
            'url': '',
            'start_date': None,
            'comment': '',
        }
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Ошибка при создании занятия: {0}'.format(response.json()))
        answer = response.json()
        assert set(answer.keys()) == {'name', 'scenario', 'date_updated',
                                      'problems', 'theme', 'owner', 'id',
                                      'methodology'}, (
            u'Неправильный набор полей в ответе')
        assert answer['theme'] == theme_model.id, u'Неправильная тема'
        assert answer['owner'] == teacher.id, u'Неправильный создатель'
        scenario = answer['scenario']
        assert scenario.pop('id'), u'Должен быть идентификатор у сценария'
        assert answer['scenario'] == expected_scenario, (
            u'Сценарий создали со значениями по умолчанию')
        assert len(answer['problems']) == 3, (
            u'Должно быть две задачи и 1 теория')
        for i in range(3):
            link_id = answer['problems'][i].pop('id')
            assert link_id, u'У связи должен быть идентификатор'
            assert LessonProblemLink.objects.get(id=link_id).order == i + 1, (
                u'Должен проставиться правильный порядок'
            )
        assert answer['problems'] == expected_problems, (
            u'Должна создаться одна связь со значениями по умолчанию')

    def test_create_with_full_scenario(self, jclient, teacher, problem_models,
                                       theme_model):
        """
        Проверяем создание без сценария
        """
        problem1, problem2 = problem_models
        create_url = reverse('v2:lesson-list')
        jclient.login(user=teacher)
        create_data = {
            'theme': theme_model.id,
            'scenario': {
                'max_attempts_in_group': 4,
                'show_all_problems': False,
            },
            'problems': [
                {
                    'type': 1,
                    'problem': problem2.id,
                    # можно не задавать `options`, будут дефолтные значения
                },
            ],
        }
        expected_problems = [
            {
                'type': 1,
                'problem': {
                    'id': problem2.id,
                    'date_updated': int(time.mktime(
                        problem2.date_updated.timetuple()))
                },
                'options': {
                    'show_tips': True,
                    'max_attempts': 5,
                },
                'theory': None,
                'block_id': None,
                'start_date': None,
                'finish_date': None,
            },
        ]
        expected_scenario = {
            'duration': None,
            'mode': 1,
            'show_answers_in_last_attempt': True,
            'max_attempts_in_group': 4,
            'show_all_problems': False,
            'url': '',
            'start_date': None,
            'comment': '',
        }
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Ошибка при создании занятия: {0}'.format(response.json()))
        answer = response.json()
        assert set(answer.keys()) == {'name', 'scenario', 'date_updated',
                                      'problems', 'theme', 'owner', 'id',
                                      'methodology'}, (
            u'Неправильный набор полей в ответе')
        assert answer['theme'] == theme_model.id, u'Неправильная тема'
        assert answer['owner'] == teacher.id, u'Неправильный создатель'
        scenario_id = answer['scenario'].pop('id', None)
        assert scenario_id, u'Должен появиться идентификатор сценария'
        assert LessonScenario.objects.get(id=scenario_id).primary, (
            u'Сценарий должен быть основным')
        assert answer['scenario'] == expected_scenario, (
            u'Неправильный сценарий')
        assert len(answer['problems']) == 1, u'Должно быть две задачи'
        link_id = answer['problems'][0].pop('id')
        assert link_id, u'У связи должен быть идентификатор'
        assert answer['problems'] == expected_problems, (
            u'Должна создаться одна связь со значениями по умолчанию')
        assert LessonProblemLink.objects.get(id=link_id).order == 1, (
            u'Должен проставиться правильный порядок')

    def test_patch(self, jclient, teacher, lesson_models):
        """
        Проверяем обновление
        """
        jclient.login(user=teacher)
        lesson, problem1, problem2, link1, link2 = lesson_models
        lesson_scenario = LessonScenario(lesson=lesson, primary=True)
        lesson_scenario.save()
        patch_url = reverse('v2:lesson-detail', args=(lesson.id,))
        old_data = jclient.get(patch_url).json()
        patch_data = {
            'scenario': {
                'duration': 23,
                'mode': 2,
                'show_all_problems': False,
                'max_attempts_in_group': 4,
                'show_answers_in_last_attempt': False,
            },
            'problems': [
                {
                    'id': link2.id,
                    'type': 1,
                    'options': {
                        'show_tips': False,
                        'max_attempts': 5,
                    },
                    'problem': link1.problem_id,
                    'theory': None,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                },
            ],
        }
        expected = {
            'name': lesson.name,
            'scenario': {
                'id': lesson_scenario.id,
                'show_all_problems': False,
                'max_attempts_in_group': 4,
                'mode': 2,
                'duration': 23,
                'show_answers_in_last_attempt': False,
                'url': '',
                'start_date': None,
                'comment': '',
            },
            'problems': [
                {
                    'id': link2.id,
                    'type': 1,
                    'problem': {
                        'id': problem1.id,
                        'date_updated': (old_data['problems'][0]['problem']
                                         ['date_updated']),
                    },
                    'options': {
                        'show_tips': False,
                        'max_attempts': 5,
                    },
                    'theory': None,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                },
            ],
            'theme': old_data['theme'],
            'owner': old_data['owner'],
            'id': old_data['id'],
            'methodology': [],
        }
        response = jclient.patch(patch_url, patch_data)
        assert response.status_code == 200, (
            u'Ошибка при создании занятия: {0}'.format(response.json()))
        answer = response.json()
        answer.pop('date_updated')
        assert answer == expected, u'Неправильно изменилось занятие'
