from builtins import str, object
import datetime
import re
import time

import pytest
from swissknife.assertions import AssertDictsAreEqual
from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.urlresolvers import reverse
from django.utils import timezone
from kelvin.common.utils import make_timestamp
from kelvin.courses.models import Course, CourseLessonLink, CourseStudent
from kelvin.lesson_assignments.models import LessonAssignment
from kelvin.lessons.models import LessonScenario, Lesson
from kelvin.problems.answers import Answer
from kelvin.resources.models import Resource
from kelvin.results.models import CourseLessonResult, CourseLessonSummary
from kelvin.subjects.models import Subject
from integration_tests.fixtures.courses import make_course_available_for_student

User = get_user_model()


@pytest.mark.django_db
class TestCLessonViewSet(object):
    """
    Тесты апи занятий в курсе
    """
    @pytest.mark.debug_test
    def test_web(self, jclient, meta_models):
        """
        Тест получения информации о курсозанятии для веба
        с main_theme и без main_theme
        """

        clesson1 = meta_models['clesson1']
        problem1, problem2 = meta_models['problems']
        meta = meta_models['meta']
        course = meta_models['course']
        theme = meta_models['theme']
        teacher = meta_models['teacher']
        lesson = clesson1.lesson
        lesson1_problem1 = meta_models['lesson1_problem1']
        lesson1_problem2 = meta_models['lesson1_problem2']

        date_updated_lesson = int(time.mktime(
            clesson1.lesson.date_updated.timetuple()))
        date_updated_clesson = int(time.mktime(
            clesson1.date_updated.timetuple()))
        date_updated_problem1 = int(time.mktime(
            problem1.date_updated.timetuple()))
        date_updated_problem2 = int(time.mktime(
            problem2.date_updated.timetuple()))
        date_updated_meta = int(time.mktime(
            meta.date_updated.timetuple()))

        expected_no_theme = ({
            u"name": u"Занятие 1",
            u"date_updated": date_updated_lesson,
            u"problems": [
                {
                    u"block_id": None,
                    u"finish_date": None,
                    u'real_max_points': 1,
                    u"id": lesson1_problem1.id,
                    u"start_date": None,
                    u"problem": {
                        u"date_updated": date_updated_problem1,
                        u"id": problem1.id,
                    },
                    u"type": 1,
                    u"options": {
                        u"show_tips": True,
                        u"max_attempts": 5,
                    },
                    u"theory": None,
                },
                {
                    u"block_id": None,
                    u"finish_date": None,
                    u'real_max_points': 1,
                    u"id": lesson1_problem2.id,
                    u"start_date": None,
                    u"problem": {
                        u"date_updated": date_updated_problem2,
                        u"id": problem2.id,
                    },
                    u"type": 1,
                    u"options": {
                        u"show_tips": True,
                        u"max_attempts": 5,
                    },
                    u"theory": None,
                }
            ],
            u"methodology": [],
            u"clesson": {
                u"comment": u"",
                u"show_all_problems": True,
                u"date_completed": None,
                u"max_attempts_in_group": 2,
                u"finish_date": None,
                u"date_updated": date_updated_clesson,
                u"visual_mode": 0,
                u"progress_indicator": None,
                u"start_date": None,
                u"accessible_to_teacher": True,
                u"course": {
                    u"id": course.id,
                    u"name": u"Новый спец курс",
                },
                u"url": None,
                u"mode": 1,
                u"date_assignment_passed": False,
                u"duration": None,
                u"access_code": None,
                u"date_assignment": None,
                u"show_answers_in_last_attempt": True,
                u"id": clesson1.id,
                u"evaluation_date": None,
                u"lesson_block_slug": None,
                u"available": True,
            },
            u"theme": theme.id,
            u"group_levels": [],
            u"owner": teacher.id,
            u"id": lesson.id,
        })
        expected_theme = ({
            u"name": u"Занятие 1",
            u"date_updated": date_updated_lesson,
            u"problems": [
                {
                    u"block_id": None,
                    u"finish_date": None,
                    u"id": lesson1_problem1.id,
                    u"start_date": None,
                    u'real_max_points': 1,
                    u"problem": {
                        u"custom_answer": False,
                        u"date_updated": date_updated_problem1,
                        u"markup": {
                            u"layout": [
                                {
                                    u"content": {
                                        u"text": u"{marker:1}",
                                        u"options": {
                                            u"style": u"normal",
                                        }
                                    },
                                    u"kind": u"text",
                                },
                                {
                                    u"content": {
                                        u"type": u"field",
                                        u"id": 1,
                                        u"options": {
                                            u"type_content": u"number",
                                        }
                                    },
                                    u"kind": u"marker",
                                }
                            ],
                            u"public_solution": u"Решение для учеников",
                            u"checks": {},
                            u"answers": {
                                u"1": 4,
                            },
                            u"solution": u"Решение для учителей",
                        },
                        u"visibility": 1,
                        u"max_points": 1,
                        u"meta": {
                            u"date_updated": date_updated_meta,
                            u"additional_themes": [],
                            u"difficulty": 1,
                            u"group_levels": [],
                            u"skills": [],
                            u"main_theme": {
                                u"level": 0,
                                u"code": u"thm",
                                u"subject": theme.subject.id,
                                u"id": theme.id,
                                u"name": u"theme",
                            },
                            u"id": meta.id,
                            u"exams": [],
                        },
                        u"owner": problem1.owner.id,
                        u"id": problem1.id,
                        u"resources": {},
                        u"subject": u"math",
                    },
                    u"type": 1,
                    u"options": {
                        u"show_tips": True,
                        u"max_attempts": 5,
                    },
                    u"theory": None,
                },
                {
                    u"block_id": None,
                    u"finish_date": None,
                    u"id": lesson1_problem2.id,
                    u"start_date": None,
                    u'real_max_points': 1,
                    u"problem": {
                        u"custom_answer": False,
                        u"date_updated": date_updated_problem2,
                        u"markup": {
                            u"layout": [
                                {
                                    u"content": {
                                        u"text": u"{marker:1}",
                                        u"options": {
                                            u"style": u"normal",
                                        }
                                    },
                                    u"kind": u"text",
                                },
                                {
                                    u"content": {
                                        u"type": u"choice",
                                        u"id": 1,
                                        u"options": {
                                            u"type_content": u"number",
                                            u"choices": [
                                                u"Брежнев",
                                                u"Горбачев",
                                                u"Ленин",
                                            ]
                                        }
                                    },
                                    u"kind": u"marker",
                                }
                            ],
                            u"public_solution": u"Решение для учеников",
                            u"checks": {},
                            u"answers": {
                                u"1": [
                                    1,
                                    2,
                                ]
                            },
                            u"solution": u"Решение для учителей",
                        },
                        u"visibility": 1,
                        u"max_points": 1,
                        u"meta": {
                            u"date_updated": date_updated_meta,
                            u"additional_themes": [],
                            u"difficulty": 1,
                            u"group_levels": [],
                            u"skills": [],
                            u"main_theme": {
                                u"level": 0,
                                u"code": u"thm",
                                u"subject": theme.subject.id,
                                u"id": theme.id,
                                u"name": u"theme",
                            },
                            u"id": meta.id,
                            u"exams": [],
                        },
                        u"owner": problem2.owner.id,
                        u"id": problem2.id,
                        u"resources": {},
                        u"subject": u"math",
                    },
                    u"type": 1,
                    u"options": {
                        u"show_tips": True,
                        u"max_attempts": 5,
                    },
                    u"theory": None,
                }
            ],
            u"methodology": [],
            u"clesson": {
                u"comment": u"",
                u"show_all_problems": True,
                u"date_completed": None,
                u"max_attempts_in_group": 2,
                u"finish_date": None,
                u"date_updated": date_updated_clesson,
                u"visual_mode": 0,
                u"progress_indicator": None,
                u"start_date": None,
                u"accessible_to_teacher": True,
                u"course": {
                    u"id": course.id,
                    u"name": u"Новый спец курс",
                },
                u"url": None,
                u"mode": 1,
                u"date_assignment_passed": False,
                u"duration": None,
                u"access_code": None,
                u"date_assignment": None,
                u"show_answers_in_last_attempt": True,
                u"id": clesson1.id,
                u"evaluation_date": None,
                u"lesson_block_slug": None,
                u"available": True,
            },
            u"theme": theme.id,
            u"group_levels": [],
            u"owner": teacher.id,
            u"id": lesson.id,
        })

        jclient.login(user=teacher)

        web_url = reverse(
            'v2:course_lesson-web', args=(clesson1.id,))
        response_no_theme = jclient.get(web_url).json()
        assert response_no_theme == expected_no_theme, (
            u"Неверный json без expand_main_theme"
        )

        web_url += '?expand_main_theme=true'
        response_theme = jclient.get(web_url).json()
        assert response_theme == expected_theme, (
            u"Неверный json с expand_main_theme"
        )

    def test_retrieve(self, jclient, clesson_with_theory_models, teacher,
                      content_manager, progress_indicator):
        """
        Тест получения занятия в рамках курса
        """
        (course, clessons, links, owner_teacher, subject,
         theme) = clesson_with_theory_models
        detail_url = reverse('v2:course_lesson-detail', args=(clessons[0].id,))

        # Анонимом получить нельзя
        response = jclient.get(detail_url)
        assert response.status_code == 401

        jclient.login(user=owner_teacher)
        response = jclient.get(detail_url)
        expected = {
            'id': clessons[0].lesson.id,
            'date_updated': int(time.mktime(
                clessons[0].lesson.date_updated.timetuple())),
            'name': u'Занятие 1',
            'owner': owner_teacher.id,
            'theme': theme.id,
            'problems': [
                {
                    'id': links[0].id,
                    'problem': {
                        'id': links[0].problem.id,
                        'date_updated': int(time.mktime(
                            links[0].problem.date_updated.timetuple())),
                    },
                    'options': {
                        'show_tips': True,
                        'max_attempts': 5,
                    },
                    'real_max_points': 1,
                    'theory': None,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                    'type': 1,
                },
                {
                    'id': links[1].id,
                    'problem': None,
                    'options': {},
                    'theory': {
                        'id': links[1].theory.id,
                        'date_updated': int(time.mktime(
                            links[1].theory.date_updated.timetuple())),
                    },
                    'real_max_points': 0,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                    'type': 3,
                },
                {
                    'id': links[2].id,
                    'problem': {
                        'id': links[2].problem.id,
                        'date_updated': int(time.mktime(
                            links[2].problem.date_updated.timetuple())),
                    },
                    'options': {
                        'show_tips': True,
                        'max_attempts': 5,
                    },
                    'real_max_points': 1,
                    'theory': None,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                    'type': 1,
                },
            ],
            'methodology': [],
            'clesson': {
                'id': clessons[0].id,
                'access_code': None,
                'course': {
                    'id': course.id,
                    'name': u'Новый спец курс',
                },
                'mode': 1,
                'duration': None,
                'max_attempts_in_group': 2,
                'show_answers_in_last_attempt': True,
                'show_all_problems': True,
                'accessible_to_teacher': (
                    clessons[0].accessible_to_teacher.strftime(
                        settings.REST_FRAMEWORK['DATETIME_FORMAT'])
                ),
                'date_assignment': None,
                'date_completed': None,
                'date_updated': int(time.mktime(
                    clessons[0].date_updated.timetuple())),
                'evaluation_date': None,
                'finish_date': None,
                'start_date': None,
                'url': None,
                'comment': '',
                'progress_indicator': {
                    'id': progress_indicator.id,
                    'slug': progress_indicator.slug,
                    'palette': progress_indicator.palette,
                },
                'visual_mode': LessonScenario.VisualModes.SEPARATE,
                'lesson_block_slug': None,
                'available': True,
            },
        }
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == expected, u'Неправильный ответ'

        detail_url = reverse('v2:course_lesson-detail', args=(clessons[1].id,))
        response = jclient.get(detail_url).json()
        assert 'clesson' in response
        assert 'date_assignment' in response['clesson']
        assert (response['clesson']['date_assignment']
                == '2010-10-29T10:29:02Z')
        assert 'visual_mode' in response['clesson']
        assert (response['clesson']['visual_mode']
                == LessonScenario.VisualModes.BLOCKS)

        # учитель не может получить закрытые уроки
        detail_url = reverse('v2:course_lesson-detail', args=(clessons[2].id,))
        jclient.login(user=owner_teacher)
        response = jclient.get(detail_url)
        assert response.status_code == 404, (
            u'Учитель не может получить урококурсы, недоступные учителям')

        # другой учитель не может получить не свой курс
        detail_url = reverse('v2:course_lesson-detail', args=(clessons[1].id,))
        jclient.login(user=teacher)
        response = jclient.get(detail_url)
        assert response.status_code == 403, (
            u'Учитель не может получить урококурсы чужого курса')

        # контент-менеджер может получить не свой курс, даже если он учитель
        detail_url = reverse('v2:course_lesson-detail', args=(clessons[1].id,))
        content_manager.is_teacher = True
        content_manager.save()
        jclient.login(user=content_manager)
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Контент-менеджер может получить урококурсы чужого курса')

    def test_retrieve_free(self, jclient,
                           lesson_models, teacher, subject_model):
        """
        Проверка получения урока бесплатного курса
        """
        lesson, problem1, problem2, link1, link2 = lesson_models

        some_owner = User.objects.create(is_teacher=True)
        course = Course.objects.create(
            name='Free course',
            owner=some_owner,
            free=True,
            subject=subject_model,
        )
        clesson = CourseLessonLink.objects.create(
            course=course,
            lesson=lesson,
            order=1,
            accessible_to_teacher=timezone.now(),
        )

        detail_url = reverse('v2:course_lesson-detail', args=(clesson.id,))
        jclient.login(user=teacher)
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_retrieve_assignment(self, jclient, lesson_with_theory_models,
                                 teacher, subject_model):
        """
        Тест получения занятия с персонализацией
        """
        (lesson, problem1, theory, problem2, link1, link2,
         link3) = lesson_with_theory_models

        student1 = User.objects.create(username='1')
        student2 = User.objects.create(username='2')
        student3 = User.objects.create(username='3')
        student4 = User.objects.create(username='4')
        parent1 = User.objects.create(username='parent1', is_parent=True)
        parent2 = User.objects.create(username='parent2', is_parent=True)

        parent1.parent_profile.children = [student1, student2, student3]
        parent2.parent_profile.children = [student4]

        course = Course.objects.create(
            name=u'Новый спец курс',
            owner=teacher,
            subject=subject_model,
        )
        CourseStudent.objects.create(course=course, student=student1)
        CourseStudent.objects.create(course=course, student=student2)
        CourseStudent.objects.create(course=course, student=student3)

        clesson = CourseLessonLink.objects.create(
            course=course,
            lesson=lesson,
            order=1,
        )

        # назначаем первому ученику первую задачу, второму - вторую (не теорию)
        LessonAssignment.objects.create(
            clesson=clesson, student=student1, problems=[link1.id],
        )
        LessonAssignment.objects.create(
            clesson=clesson, student=student2, problems=[link3.id],
        )

        expected_problems = [
            {
                'id': link1.id,
                'problem': {
                    'id': problem1.id,
                    'date_updated': int(time.mktime(
                        problem1.date_updated.timetuple())),
                },
                'options': {
                    'show_tips': True,
                    'max_attempts': 5,
                },
                'real_max_points': 1,
                'theory': None,
                'block_id': None,
                'start_date': None,
                'finish_date': None,
                'type': 1,
            },
            {
                'id': link2.id,
                'problem': None,
                'options': {},
                'theory': {
                    'id': theory.id,
                    'date_updated': int(time.mktime(
                        theory.date_updated.timetuple())),
                },
                'real_max_points': 0,
                'block_id': None,
                'start_date': None,
                'finish_date': None,
                'type': 3,
            },
            {
                'id': link3.id,
                'problem': {
                    'id': problem2.id,
                    'date_updated': int(time.mktime(
                        problem2.date_updated.timetuple())),
                },
                'real_max_points': 1,
                'options': {
                    'show_tips': True,
                    'max_attempts': 5,
                },
                'block_id': 1,
                'start_date': None,
                'finish_date': None,
                'theory': None,
                'type': 1,
            },
        ]

        detail_url = reverse('v2:course_lesson-detail', args=(clesson.id,))

        # аноним
        response = jclient.get(detail_url)
        assert response.status_code == 401

        # нет даты назначения урока
        jclient.login(user=student1)
        response = jclient.get(detail_url)
        assert response.status_code == 403

        # проставление даты назначения
        clesson.date_assignment = datetime.datetime(
            2010, 10, 29, tzinfo=timezone.utc,
        )
        clesson.save()

        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json()['problems'] == [expected_problems[0]]

        jclient.login(user=student2)
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json()['problems'] == [expected_problems[2]]

        jclient.login(user=student3)
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json()['problems'] == expected_problems

        # ученик не может получить урок из чужой группы
        jclient.login(user=student4)
        response = jclient.get(detail_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # родитель может получить уроки своего ребенка
        jclient.login(user=parent1)
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # нельзя получить уроки чужих детей
        jclient.login(user=parent2)
        response = jclient.get(detail_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_create_with_theory(self, jclient, problem_models, theory_model,
                                teacher):
        """
        Тест создания занятия с обычными задачами и теорией
        """
        problem1, problem2 = problem_models
        subject = Subject.objects.create()
        course = Course.objects.create(
            name=u'Теоретический курс',
            subject=subject,
            owner=teacher,
            id=1,
        )
        create_url = reverse('v2:course_lesson-list')
        post_data = {
            'name': u'Теоретическое занятие',
            'problems': [
                {
                    'type': 1,
                    'problem': {
                        'id': problem1.id,
                    },
                    'options': {
                        'show_tips': False,
                        'max_attempts': 1,
                    },
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                    'theory': None,
                },
                {
                    'type': 3,
                    'problem': None,
                    'options': {},
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                    'theory': theory_model.id,
                },
                {
                    'type': 1,
                    'problem': {
                        'id': problem2.id,
                    },
                    'options': {
                        'show_tips': False,
                        'max_attempts': 1,
                    },
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                    'theory': None,
                },

            ],
            'clesson': {
                'mode': 2,
                'duration': 45,
                'show_all_problems': False,
                'date_completed': None,
                'evaluation_date': '2017-09-20T01:20:10Z',
                'finish_date': '2017-09-11T01:20:10Z',
                'course': {
                    'id': course.id,
                },
            },
        }
        jclient.login(user=teacher)
        response = jclient.post(create_url, post_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()

        # удаляем сгенерированные поля
        assert answer.pop('id')
        assert answer.pop('date_updated')
        assert answer['clesson'].pop('id')
        assert answer['clesson'].pop('date_updated')
        assert answer['clesson']['accessible_to_teacher'] is not None
        assert (answer['clesson'].pop('accessible_to_teacher') <=
                timezone.now().strftime(
                    settings.REST_FRAMEWORK['DATETIME_FORMAT']))
        assert len(answer['problems']) == 3
        assert answer['problems'][0].pop('id')
        assert answer['problems'][0]['problem'].pop('date_updated')
        assert answer['problems'][1].pop('id')
        assert answer['problems'][1]['theory'].pop('date_updated')
        assert answer['problems'][2].pop('id')
        assert answer['problems'][2]['problem'].pop('date_updated')

        expected = {
            'name': u'Теоретическое занятие',
            'problems': [
                {
                    'type': 1,
                    'problem': {
                        'id': problem1.id,
                    },
                    'options': {
                        'show_tips': False,
                        'max_attempts': 1,
                    },
                    'real_max_points': 1,
                    'theory': None,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                },
                {
                    'type': 3,
                    'problem': None,
                    'options': {},
                    'theory': {
                        'id': theory_model.id,
                    },
                    'real_max_points': 0,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                },
                {
                    'type': 1,
                    'problem': {
                        'id': problem2.id,
                    },
                    'options': {
                        'show_tips': False,
                        'max_attempts': 1,
                    },
                    'real_max_points': 1,
                    'theory': None,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                },
            ],
            'clesson': {
                'mode': 2,
                'duration': 45,
                'show_all_problems': False,
                'date_assignment': None,
                'date_completed': None,
                'evaluation_date': '2017-09-20T01:20:10Z',
                'finish_date': '2017-09-11T01:20:10Z',
                'course': {
                    'id': course.id,
                    'name': course.name,
                },
                'access_code': None,
                'max_attempts_in_group': 2,
                'show_answers_in_last_attempt': True,
                'start_date': None,
                'url': None,
                'comment': '',
                'progress_indicator': None,
                'visual_mode': LessonScenario.VisualModes.SEPARATE,
                'lesson_block_slug': None,
                'available': True,
            },
            'methodology': [],
            'owner': teacher.id,
            'theme': None,
        }
        assert answer == expected

    def test_patch(
        self,
        jclient,
        clesson_with_theory_models,
        progress_indicator,
    ):
        """
        Тест частичного обновления
        """
        (course, clessons, links, teacher, subject,
         theme) = clesson_with_theory_models
        detail_url = reverse('v2:course_lesson-detail', args=(clessons[0].id,))
        old_accessible_to_teacher = (
            clessons[0].accessible_to_teacher.strftime(
                settings.REST_FRAMEWORK['DATETIME_FORMAT'])
        )
        patch_data = {
            'name': u'Контрольная работа 1',
            'problems': [
                {
                    'id': links[2].id,
                    'type': 1,
                    'problem': {
                        'id': links[2].problem.id,
                        'date_updated': int(time.mktime(
                            links[2].problem.date_updated.timetuple())),
                    },
                    'options': {
                        'show_tips': False,
                        'max_attempts': 1,
                    },
                },
            ],
            'clesson': {
                # можем частично обновить вложенный объект
                'mode': 2,
                'duration': 45,
                'show_all_problems': False,
                'date_assignment': '2010-02-20T01:20:10Z',
                'evaluation_date': '2017-09-20T01:20:10Z',
                'finish_date': '2017-09-11T01:20:10Z',
                'date_completed': None,
                'course': {
                    'id': course.id,
                },
                # поле только для чтения
                'accessible_to_teacher': '2011-02-20T01:20:10Z',
            },
        }
        old_date_updated = int(time.mktime(clessons[0].lesson.date_updated
                                           .timetuple()))

        jclient.login(user=teacher)
        response = jclient.patch(detail_url, patch_data)

        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        clessons[0].refresh_from_db()
        clessons[0].lesson.refresh_from_db()
        expected = {
            'id': clessons[0].lesson.id,
            'name': u'Контрольная работа 1',
            'owner': teacher.id,
            'theme': theme.id,
            'problems': [
                {
                    'id': links[2].id,
                    'type': 1,
                    'problem': {
                        'id': links[2].problem.id,
                        'date_updated': int(time.mktime(
                            links[2].problem.date_updated.timetuple())),
                    },
                    'options': {
                        'show_tips': False,
                        'max_attempts': 1,
                    },
                    'real_max_points': 1,
                    'theory': None,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                },
            ],
            'methodology': [],
            'clesson': {
                'id': clessons[0].id,
                'access_code': None,
                'course': {
                    'id': course.id,
                    'name': u'Новый спец курс',
                },
                'mode': 2,
                'duration': 45,
                'max_attempts_in_group': 2,
                'show_answers_in_last_attempt': True,
                'show_all_problems': False,
                'accessible_to_teacher': old_accessible_to_teacher,
                'date_assignment': clessons[0].date_created.strftime(
                    settings.REST_FRAMEWORK['DATETIME_FORMAT']),
                'date_completed': None,
                'date_updated': int(time.mktime(
                    clessons[0].date_updated.timetuple())),
                'evaluation_date': '2017-09-20T01:20:10Z',
                'finish_date': '2017-09-11T01:20:10Z',
                'start_date': None,
                'url': None,
                'comment': '',
                'progress_indicator': {
                    'id': progress_indicator.id,
                    'slug': progress_indicator.slug,
                    'palette': progress_indicator.palette,
                },
                'visual_mode': LessonScenario.VisualModes.SEPARATE,
                'lesson_block_slug': None,
                'available': True,
            },
        }
        answer = response.json()
        # дату обновления не можем проверить точно , т.к. отдается дата до
        # изменений в сигналах
        new_date_updated = int(time.mktime(clessons[0].lesson.date_updated
                                           .timetuple()))
        assert old_date_updated <= answer.pop('date_updated') <= new_date_updated
        assert answer == expected, u'Неправильный ответ'

        #  нельзя редактировать связь с проставленной датой назначения
        detail_url = reverse('v2:course_lesson-detail', args=(clessons[1].id,))
        patch_data = {
            'name': u'Контрольная работа 1',
        }
        jclient.login(user=teacher)
        response = jclient.patch(detail_url, patch_data)
        assert response.status_code == 403

    def test_patch_code(self, jclient, clesson_models):
        """
        Тест генерации кода при обновлении
        """
        (course, clessons, lesson1_problem1, lesson1_problem2,
         teacher, subject, theme) = clesson_models
        detail_url = reverse('v2:course_lesson-detail', args=(clessons[0].id,))
        jclient.login(user=teacher)

        # генерируем код
        response = jclient.patch(detail_url,
                                      {'clesson': {'access_code': True}})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        access_code = response.json()['clesson']['access_code']
        assert len(access_code) == 4

        # обновляем занятие своим кодом
        response = jclient.patch(detail_url,
                                      {'clesson': {'access_code': '1234'}})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json()['clesson']['access_code'] == '1234'

        # обнуляем код
        response = jclient.patch(detail_url,
                                      {'clesson': {'access_code': None}})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json()['clesson']['access_code'] is None

    def test_patch_not_editable(
        self,
        jclient,
        clesson_models,
        progress_indicator
    ):
        """
        Тест частичного редактирования со связью, занятие в котором
        обозначено нередактируемым
        """

        (course, clessons, lesson1_problem1, lesson1_problem2,
         teacher, subject, theme) = clesson_models
        resource = Resource.objects.create(name=u'Метода')
        clessons[0].lesson.methodology = [resource]
        clessons[0].lesson_editable = False
        clessons[0].save()
        old_lesson = clessons[0].lesson
        old_date_updated = int(time.mktime(old_lesson.date_updated
                                           .timetuple()))
        detail_url = reverse('v2:course_lesson-detail', args=(clessons[0].id,))
        patch_data = {
            'name': u'Контрольная работа 1',
            'problems': [
                {
                    'id': lesson1_problem2.id,
                    'type': 1,
                    'block_id': 1,
                    'start_date': None,
                    'finish_date': None,
                    'problem': {
                        'id': lesson1_problem2.problem.id,
                        'date_updated': int(time.mktime(
                            lesson1_problem2.problem.date_updated.timetuple())),
                    },
                    'options': {
                        'show_tips': False,
                        'max_attempts': 3,
                    },
                }
            ],
        }
        assert Lesson.objects.all().count() == 2, u'До запроса 2 занятия'

        jclient.login(user=teacher)
        response = jclient.patch(detail_url, patch_data)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        clessons[0].refresh_from_db()
        clessons[0].lesson.refresh_from_db()

        GUID_RE = settings.SWITCHMAN_URL + r'\/\w{8}-\w{4}-\w{4}-\w{4}-\w{12}'

        expected = {
            'id': clessons[0].lesson.id,
            'name': u'Контрольная работа 1',
            'owner': teacher.id,
            'theme': theme.id,
            'problems': [
                {
                    'type': 1,
                    'problem': {
                        'id': lesson1_problem2.problem.id,
                        'date_updated': int(time.mktime(
                            lesson1_problem2.problem.date_updated.timetuple())),
                    },
                    'options': {
                        'show_tips': False,
                        'max_attempts': 3,
                    },
                    'theory': None,
                    'real_max_points': 1,
                    'block_id': 1,
                    'start_date': None,
                    'finish_date': None,
                },
            ],
            'methodology': [
                {
                    'date_updated': int(time.mktime(
                        resource.date_updated.timetuple())),
                    'id': resource.id,
                    'name': u'Метода',
                    'type': '',
                    'nda': False,
                    'shortened_file_url': lambda x: bool(re.match(GUID_RE, x)),
                },
            ],
            'clesson': {
                'id': clessons[0].id,
                'access_code': None,
                'course': {
                    'id': course.id,
                    'name': u'Новый спец курс',
                },
                'mode': 1,
                'duration': None,
                'max_attempts_in_group': 2,
                'show_answers_in_last_attempt': True,
                'show_all_problems': True,
                'accessible_to_teacher': (
                    clessons[0].accessible_to_teacher.strftime(
                        settings.REST_FRAMEWORK['DATETIME_FORMAT'])
                ),
                'date_assignment': None,
                'date_completed': None,
                'date_updated': int(time.mktime(
                    clessons[0].date_updated.timetuple())),
                'evaluation_date': None,
                'finish_date': None,
                'start_date': None,
                'url': None,
                'comment': '',
                'progress_indicator': {
                    'id': progress_indicator.id,
                    'slug': progress_indicator.slug,
                    'palette': progress_indicator.palette,
                },
                'visual_mode': LessonScenario.VisualModes.SEPARATE,
                'lesson_block_slug': None,
            },
        }
        answer = response.json()
        # дату обновления не можем проверить точно , т.к. отдается дата до
        # изменений в сигналах
        new_date_updated = int(time.mktime(clessons[0].lesson.date_updated
                                           .timetuple()))
        assert old_date_updated <= answer.pop(
            'date_updated') <= new_date_updated
        assert len(answer['problems']) == 1
        assert answer['problems'][0].pop('id') != lesson1_problem2.id
        AssertDictsAreEqual(answer, expected)
        assert answer.pop('id') != old_lesson.id
        assert Lesson.objects.all().count() == 3, (
            u'Должно создаться новое занятие')
        assert clessons[0].lesson_editable is True, (
            u'Занятие в связи должно стать редактируемым')
        assert list(clessons[0].lesson.methodology.values_list(
            'id', flat=True)) == [resource.id], (
            u'У нового занятия должна быть методология')

        # проверяем, что старое занятие не изенилось
        old_lesson.refresh_from_db()
        assert len(old_lesson.problems.all()) == 2, (
            u'У старого занятия должно остаться две задачи')

    def test_patch_reorder_problems(self, jclient, clesson_models):
        """
        Проверяет, что можно изменять порядок задач
        """
        (course, clessons, lesson1_problem1, lesson1_problem2,
         teacher, subject, theme) = clesson_models
        detail_url = reverse('v2:course_lesson-detail', args=(clessons[0].id,))
        jclient.login(user=teacher)

        # меняем порядок задач
        data = {
            'problems': [
                {
                    'id': lesson1_problem2.id,
                },
                {
                    'id': lesson1_problem1.id,
                },
            ]
        }
        response = jclient.patch(detail_url, data)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer['problems'][0]['id'] == lesson1_problem2.id
        assert answer['problems'][1]['id'] == lesson1_problem1.id

    def test_patch_published_clesson(self, jclient, clesson_models):
        """
        Проверяет невозможность изменения занятия, когда оно уже выдано
        """
        (course, clessons, lesson1_problem1, lesson1_problem2,
         teacher, subject, theme) = clesson_models
        detail_url = reverse('v2:course_lesson-detail', args=(clessons[0].id,))
        clessons[0].date_assignment = timezone.now()
        clessons[0].save()
        course.save()
        jclient.login(user=teacher)
        data = {
            'problems': [
                {
                    'id': lesson1_problem2.id,
                },
                {
                    'id': lesson1_problem1.id,
                },
            ]
        }

        response = jclient.patch(detail_url, data)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_patch_clesson_with_variations(
            self, jclient, content_manager,
            student_in_course_with_lesson_variations):
        """
        Проверяет изменение назначения при изменении курсозанятия
        """
        clesson = student_in_course_with_lesson_variations['clessons'][0]
        student = student_in_course_with_lesson_variations['student']
        jclient.login(user=content_manager)
        data = {
            'duration': 35,
        }
        detail_url = reverse('v2:course_lesson-detail', args=(clesson.id,))
        assert LessonAssignment.objects.filter(
            student=student, clesson=clesson).count() == 0

        response = jclient.patch(detail_url, data)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert LessonAssignment.objects.filter(
            student=student, clesson=clesson).count() == 1

    def test_results(self, jclient, teacher, subject_model, lesson_models,
                     content_manager):
        """
        Тест сводки результатов по занятию
        """
        student1 = User.objects.create(username='1')
        student2 = User.objects.create(username='2')
        student3 = User.objects.create(username='3')
        course = Course.objects.create(
            name=u'Новый спец курс',
            subject=subject_model,
            owner=teacher,
            color='#abcabc',
        )
        lesson, problem1, problem2, link1, link2 = lesson_models
        clesson = CourseLessonLink.objects.create(
            course=course,
            lesson=lesson,
            order=1,
            accessible_to_teacher=timezone.now(),
            date_assignment=timezone.now(),
        )

        jclient.login(user=teacher)
        results_url = reverse('v2:course_lesson-results', args=(clesson.id,))

        # у курса нет назначенной группы
        response = jclient.get(results_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {'data': {'students': {}}, 'csv_url': None}

        # добавляем трех учеников в группу
        CourseStudent.objects.create(course=course, student=student1)
        CourseStudent.objects.create(course=course, student=student2)
        CourseStudent.objects.create(course=course, student=student3)

        # никто ничего не проходил, все задачи назначены всем
        expected = {
            'students': {
                str(student1.id): {
                    'mode': 1,
                    'points': 0,
                    'max_points': 0,
                    'completed': False,
                    'problems': {
                        str(link1.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                        str(link2.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                    },
                },
                str(student2.id): {
                    'mode': 1,
                    'points': 0,
                    'max_points': 0,
                    'completed': False,
                    'problems': {
                        str(link1.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                        str(link2.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                    },
                },
                str(student3.id): {
                    'mode': 1,
                    'points': 0,
                    'max_points': 0,
                    'completed': False,
                    'problems': {
                        str(link1.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                        str(link2.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                    },
                },
            },
        }
        response = jclient.get(results_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json()['data'] == expected, u'Неправильный ответ'

        # назначаем первому ученику первую задачу, второму - вторую
        LessonAssignment.objects.create(
            clesson=clesson, student=student1, problems=[link1.id],
        )
        LessonAssignment.objects.create(
            clesson=clesson, student=student2, problems=[link2.id],
        )

        expected = {
            'students': {
                str(student1.id): {
                    'mode': 1,
                    'points': 0,
                    'max_points': 0,
                    'completed': False,
                    'problems': {
                        str(link1.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                    },
                },
                str(student2.id): {
                    'mode': 1,
                    'points': 0,
                    'max_points': 0,
                    'completed': False,
                    'problems': {
                        str(link2.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                    },
                },
                str(student3.id): {
                    'mode': 1,
                    'points': 0,
                    'max_points': 0,
                    'completed': False,
                    'problems': {
                        str(link1.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                        str(link2.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                    },
                },
            },
        }
        response = jclient.get(results_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json()['data'] == expected, u'Неправильный ответ'

        # добавляем незавершенный ответ первому ученику, добавляем третьему
        # ученику первый пропущенный вопрос и второй неправильно
        summary_student1 = CourseLessonSummary.objects.create(
            clesson=clesson,
            student=student1,
        )
        CourseLessonResult.objects.create(
            summary=summary_student1,
            completed=False,
            points=1,
            max_points=1,
            answers={
                str(link1.id): [
                    {
                        'mistakes': 1,
                        'max_mistakes': 1,
                        'completed': False,
                        'points': 1,
                        'markers': {
                            '1': {
                                'mistakes': 1,
                                'max_mistakes': 1,
                            },
                        },
                    },
                ],
            },
        )
        summary_student3 = CourseLessonSummary.objects.create(
            clesson=clesson,
            student=student3,
        )
        student3_result = CourseLessonResult.objects.create(
            summary=summary_student3,
            completed=False,
            points=1,
            max_points=1,
            answers={
                str(link1.id): [
                    {
                        'mistakes': 1,
                        'max_mistakes': 1,
                        'completed': True,
                        'spent_time': 10,
                        'points': 0,
                        'markers': {
                            '1': {
                                'mistakes': 1,
                                'max_mistakes': 1,
                                'answer_status': -1,
                            },
                        },
                    },
                ],
                str(link2.id): [
                    {
                        'mistakes': 1,
                        'max_mistakes': 1,
                        'completed': True,
                        'spent_time': 20,
                        'points': 0,
                        'markers': {
                            '1': {
                                'mistakes': 1,
                                'max_mistakes': 1,
                            },
                        },
                    },
                ],
            },
        )
        expected = {
            'students': {
                str(student1.id): {
                    'mode': 1,
                    'points': 1,
                    'max_points': 1,  # значение берется из объекта результата
                    'completed': False,
                    'problems': {
                        str(link1.id): {
                            'status': None,
                            'time': 0,
                            'answered': False,
                            'attempt_number': 0,
                            'max_attempts': 5,
                            'type': 1,
                            'points': 1,
                            'max_points': 1,
                        },
                    },
                },
                str(student2.id): {
                    'mode': 1,
                    'points': 0,
                    'max_points': 0,
                    'completed': False,
                    'problems': {
                        str(link2.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                    },
                },
                str(student3.id): {
                    'mode': 1,
                    'points': 1,
                    'max_points': 1,
                    'completed': False,
                    'problems': {
                        str(link1.id): {
                            'status': Answer.SUMMARY_INCORRECT,
                            'time': 10,
                            'answered': True,
                            'attempt_number': 1,
                            'max_attempts': 5,
                            'type': 1,
                            'points': 0,
                            'max_points': 1,
                        },
                        str(link2.id): {
                            'status': Answer.SUMMARY_INCORRECT,
                            'time': 20,
                            'answered': True,
                            'attempt_number': 1,
                            'max_attempts': 5,
                            'type': 1,
                            'points': 0,
                            'max_points': 1,
                        },
                    },
                },
            },
        }
        response = jclient.get(results_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json()['data'] == expected, u'Неправильный ответ'

        # добавляем первому ученику вторую попытку на занятие с неправильным
        # ответом; второму - ничего; третьему вторую попытку на второй вопрос
        # с правильным ответом
        CourseLessonResult.objects.create(
            summary=summary_student1,
            completed=False,
            points=1,
            max_points=1,
            answers={
                str(link1.id): [
                    {
                        'mistakes': 1,
                        'max_mistakes': 1,
                        'completed': True,
                        'points': 1,
                        'markers': {
                            '1': {
                                'mistakes': 1,
                                'max_mistakes': 1,
                            },
                        },
                    },
                ],
            },
        )
        student3_result.answers = {
            str(link1.id): [
                {
                    'mistakes': 1,
                    'max_mistakes': 1,
                    'completed': True,
                    'spent_time': 10,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                    },
                },
            ],
            str(link2.id): [
                {
                    'mistakes': 1,
                    'max_mistakes': 1,
                    'completed': True,
                    'spent_time': 20,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                },
                {
                    'mistakes': 0,
                    'max_mistakes': 1,
                    'completed': True,
                    'spent_time': 20,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                },
            ],
        }
        student3_result.save()
        expected = {
            'students': {
                str(student1.id): {
                    'mode': 1,
                    'points': 1,
                    'max_points': 1,
                    'completed': False,
                    'problems': {
                        str(link1.id): {
                            'status': Answer.SUMMARY_INCORRECT,
                            'time': 0,
                            'answered': True,
                            'attempt_number': 1,
                            'max_attempts': 5,
                            'type': 1,
                            'points': 1,
                            'max_points': 1,
                        },
                    },
                },
                str(student2.id): {
                    'mode': 1,
                    'points': 0,
                    'max_points': 0,
                    'completed': False,
                    'problems': {
                        str(link2.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                    },
                },
                str(student3.id): {
                    'mode': 1,
                    'points': 1,
                    'max_points': 1,
                    'completed': False,
                    'problems': {
                        str(link1.id): {
                            'status': Answer.SUMMARY_INCORRECT,
                            'time': 10,
                            'answered': True,
                            'attempt_number': 1,
                            'max_attempts': 5,
                            'type': 1,
                            'points': 0,
                            'max_points': 1,
                        },
                        str(link2.id): {
                            'status': Answer.SUMMARY_CORRECT,
                            'time': 40,
                            'answered': True,
                            'attempt_number': 2,
                            'max_attempts': 5,
                            'type': 1,
                            'points': 0,
                            'max_points': 1,
                        },
                    },
                },
            },

        }
        response = jclient.get(results_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json()['data'] == expected, u'Неправильный ответ'

        # добавляем второму ученику попытку "вне занятия", результаты не
        # должны измениться
        summary_student2 = CourseLessonSummary.objects.create(
            clesson=clesson,
            student=student2,
        )
        CourseLessonResult.objects.create(
            summary=summary_student2,
            completed=True,
            work_out=True,
            points=1,
            max_points=1,
            answers={
                str(link1.id): [
                    {
                        'mistakes': 0,
                        'max_mistakes': 1,
                        'completed': True,
                        'points': 1,
                        'markers': {
                            '1': {
                                'mistakes': 0,
                                'max_mistakes': 1,
                            },
                        },
                    },
                ],
            },
        )
        response = jclient.get(results_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json()['data'] == expected, u'Неправильный ответ'

        # убираем ответ на второй вопрос
        student3_result.answers = {
            str(link1.id): [
                {
                    'mistakes': 1,
                    'max_mistakes': 1,
                    'completed': True,
                    'spent_time': 10,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'answer_status': -1,
                        },
                    },
                },
            ],
        }
        student3_result.save()
        expected = {
            'students': {
                str(student1.id): {
                    'mode': 1,
                    'points': 1,
                    'max_points': 1,
                    'completed': False,
                    'problems': {
                        str(link1.id): {
                            'status': Answer.SUMMARY_INCORRECT,
                            'time': 0,
                            'answered': True,
                            'attempt_number': 1,
                            'max_attempts': 5,
                            'type': 1,
                            'points': 1,
                            'max_points': 1,
                        },
                    },
                },
                str(student2.id): {
                    'mode': 1,
                    'points': 0,
                    'max_points': 0,
                    'completed': False,
                    'problems': {
                        str(link2.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                    },
                },
                str(student3.id): {
                    'mode': 1,
                    'points': 1,
                    'max_points': 1,
                    'completed': False,
                    'problems': {
                        str(link1.id): {
                            'status': Answer.SUMMARY_INCORRECT,
                            'time': 10,
                            'answered': True,
                            'attempt_number': 1,
                            'max_attempts': 5,
                            'type': 1,
                            'points': 0,
                            'max_points': 1,
                        },
                        str(link2.id): {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                    },
                },
            },
        }
        response = jclient.get(results_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json()['data'] == expected, u'Неправильный ответ'

        # Контент-менеджер должен тоже иметь доступ к результату
        jclient.logout()
        jclient.login(user=content_manager)
        response = jclient.get(results_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json()['data'] == expected, u'Неправильный ответ'

    def test_complete(self, jclient, clesson_models, progress_indicator):
        """
        Тест метода завершения занятия
        """
        (course, clessons, lesson1_problem1, lesson1_problem2,
         teacher, subject, theme) = clesson_models
        other_teacher = User.objects.create(username='some teacher')
        complete_url = reverse('v2:course_lesson-complete',
                               args=(clessons[0].id,))

        # надо быть авторизованным
        response = jclient.post(complete_url, None)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # проверяем, что другой учитель не может закончить урок
        jclient.login(user=other_teacher)
        response = jclient.post(complete_url, None)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # занятие должно быть назначено
        jclient.login(user=course.owner)
        response = jclient.post(complete_url, None)
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # назначаем занятие и завершаем
        clessons[0].date_assignment = timezone.now()
        clessons[0].save()
        response = jclient.post(complete_url, None)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        clessons[0].refresh_from_db()
        expected = {
            'id': clessons[0].lesson.id,
            'date_updated': int(time.mktime(
                clessons[0].lesson.date_updated.timetuple())),
            'name': u'Занятие 1',
            'owner': clessons[0].lesson.owner.id,
            'theme': theme.id,
            'problems': [
                {
                    'id': lesson1_problem1.id,
                    'type': 1,
                    'problem': {
                        'id': lesson1_problem1.problem.id,
                        'date_updated': make_timestamp(
                            lesson1_problem1.problem.date_updated),
                    },
                    'options': {
                        'show_tips': True,
                        'max_attempts': 5,
                    },
                    'real_max_points': 1,
                    'theory': None,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                },
                {
                    'id': lesson1_problem2.id,
                    'type': 1,
                    'problem': {
                        'id': lesson1_problem2.problem.id,
                        'date_updated': make_timestamp(
                            lesson1_problem2.problem.date_updated),
                    },
                    'options': {
                        'show_tips': True,
                        'max_attempts': 5,
                    },
                    'real_max_points': 1,
                    'theory': None,
                    'block_id': None,
                    'start_date': None,
                    'finish_date': None,
                },
            ],
            'methodology': [],
            'clesson': {
                'id': clessons[0].id,
                'access_code': None,
                'course': {
                    'id': course.id,
                    'name': u'Новый спец курс',
                },
                'mode': 1,
                'duration': None,
                'max_attempts_in_group': 2,
                'show_answers_in_last_attempt': True,
                'show_all_problems': True,
                'accessible_to_teacher': (
                    clessons[0].accessible_to_teacher.strftime(
                        settings.REST_FRAMEWORK['DATETIME_FORMAT'])
                ),
                'date_assignment': clessons[0].date_assignment.strftime(
                    settings.REST_FRAMEWORK['DATETIME_FORMAT']),
                'date_completed': clessons[0].date_completed.strftime(
                    settings.REST_FRAMEWORK['DATETIME_FORMAT']),
                'date_updated': make_timestamp(
                    clessons[0].date_updated),
                'evaluation_date': None,
                'finish_date': None,
                'start_date': None,
                'url': None,
                'comment': '',
                'progress_indicator': {
                    'id': progress_indicator.id,
                    'slug': progress_indicator.slug,
                    'palette': progress_indicator.palette,
                },
                'visual_mode': LessonScenario.VisualModes.SEPARATE,
                'lesson_block_slug': None,
                'available': True,
            },
        }

        assert response.json() == expected

        # Пытаемся завершить недоступное для редактирования занятие
        clessons[1].lesson_editable = False
        clessons[1].save()
        old_lesson = clessons[1].lesson
        complete_url = reverse('v2:course_lesson-complete',
                               args=(clessons[1].id,))
        response = jclient.post(complete_url, None)

        clessons[1].refresh_from_db()
        expected = {
            'id': clessons[1].lesson.id,
            'date_updated': int(time.mktime(
                clessons[1].lesson.date_updated.timetuple())),
            'name': u'Занятие 2',
            'owner': clessons[1].lesson.owner.id,
            'theme': theme.id,
            'problems': [],
            'methodology': [],
            'clesson': {
                'id': clessons[1].id,
                'access_code': None,
                'course': {
                    'id': course.id,
                    'name': u'Новый спец курс',
                },
                'mode': 1,
                'duration': None,
                'max_attempts_in_group': 2,
                'show_answers_in_last_attempt': True,
                'show_all_problems': True,
                'accessible_to_teacher': (
                    clessons[1].accessible_to_teacher.strftime(
                        settings.REST_FRAMEWORK['DATETIME_FORMAT'])
                ),
                'date_assignment': clessons[1].date_assignment.strftime(
                    settings.REST_FRAMEWORK['DATETIME_FORMAT']),
                'date_completed': clessons[1].date_completed.strftime(
                    settings.REST_FRAMEWORK['DATETIME_FORMAT']),
                'date_updated': make_timestamp(
                    clessons[1].date_updated),
                'evaluation_date': clessons[1].evaluation_date.strftime(
                    settings.REST_FRAMEWORK['DATETIME_FORMAT']),
                'finish_date': None,
                'start_date': None,
                'url': None,
                'comment': '',
                'progress_indicator': None,
                'visual_mode': LessonScenario.VisualModes.BLOCKS,
                'lesson_block_slug': None,
                'available': True,
            },
        }
        response_data = response.json()
        assert response_data == expected
        assert response_data['id'] != old_lesson.id

    @pytest.mark.xfail
    def test_finish(self, jclient, student, clesson_models):
        """
        Тест завершения учеником занятия
        """
        (course, clessons, lesson1_problem1, lesson1_problem2,
         teacher, subject, theme) = clesson_models
        finish_url = reverse('v2:course_lesson-finish', args=(clessons[0].id,))

        # надо быть авторизованным
        response = jclient.post(finish_url, None)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {
            'detail': 'Authentication credentials were not provided.'}

        # создаем сводку
        assert CourseLessonSummary.objects.all().count() == 0
        jclient.login(user=student)
        response = jclient.post(finish_url, None)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert CourseLessonSummary.objects.all().count() == 1
        summary = CourseLessonSummary.objects.all().first()
        assert summary.student_id == student.id
        assert summary.clesson_id == clessons[0].id
        assert summary.lesson_finished is True

        # делаем сводку незаконченной и повторяем запрос
        summary.lesson_finished = False
        summary.data = 'smth'
        summary.save()
        response = jclient.post(finish_url, None)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert CourseLessonSummary.objects.all().count() == 1
        summary = CourseLessonSummary.objects.all().first()
        assert summary.student_id == student.id
        assert summary.clesson_id == clessons[0].id
        assert summary.lesson_finished is True
        assert summary.data == 'smth'

    def test_latest_result(self, jclient, student, clesson_models):
        """
        Тест получения последнего результата по занятию
        """
        (course, clessons, lesson1_problem1, lesson1_problem2,
         teacher, subject, theme) = clesson_models
        latest_result_url = reverse('v2:course_lesson-latest-result',
                                    args=(clessons[0].id,))

        # аноним не имеет доступ к методу
        response = jclient.get(latest_result_url)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # у ученика нет результатов
        jclient.login(user=student)
        response = jclient.get(latest_result_url)
        assert response.status_code == 404, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # успешный запрос последнего результата
        summary = CourseLessonSummary.objects.create(
            student=student,
            clesson=clessons[0],
        )
        result1 = CourseLessonResult.objects.create(
            summary=summary,
            max_points=20,
            answers={},
        )
        result2 = CourseLessonResult.objects.create(
            summary=summary,
            max_points=40,
            answers={},
        )
        response = jclient.get(latest_result_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer.pop('date_updated')
        assert answer.pop('date_created')
        assert answer == {
            'id': result2.id,
            'student': student.id,
            'clesson': clessons[0].id,
            'work_out': False,
            'completed': True,
            'viewed': False,
            'answers': {},
            'max_points': 40,
            'points': None,
            'spent_time': None,
            'student_viewed_problems': {},
        }

    def test_source_course_clessons_availability(
            self, jclient, teacher, content_manager, subject_model):

        course_of_cm = Course.objects.create(
            name='Course#Cm',
            mode=Course.BOOK_COURSE,
            owner=content_manager,
            subject=subject_model,
        )
        course_of_teacher = Course.objects.create(
            name='Course#Teacher',
            mode=Course.USUAL_COURSE,
            owner=teacher,
            subject=subject_model,
        )
        lesson = Lesson.objects.create(
            name=u'Занятие #1',
            owner=content_manager
        )
        clesson = CourseLessonLink.objects.create(
            lesson=lesson,
            course=course_of_cm,
            accessible_to_teacher=None,
        )

        detail_url = reverse('v2:course_lesson-detail', args=(clesson.id,))

        # должен пустить к-м на занятие из своего курса
        jclient.login(user=content_manager)
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # не должен пустить учителя на занятие не из своего курса
        jclient.login(user=teacher)
        response = jclient.get(detail_url)
        assert response.status_code == 404, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # должен пустить учителя на занятие не из своего кураса
        # если у него в курсоисточниках есть данный курс
        course_of_teacher.source_courses.add(course_of_cm)
        course_of_teacher.save()

        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
