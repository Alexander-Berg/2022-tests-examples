import json
from builtins import str, range, object
import copy
import time

import datetime
import pytest
from datetime import timedelta
from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.urlresolvers import reverse
from django.utils import timezone

from kelvin.common.error_responses import ErrorsComposer
from kelvin.projects.models import Project
from kelvin.accounts.models import UserProject, User
from kelvin.courses.models import (
    Course, CourseLessonLink, CourseStudent,
)
from kelvin.group_levels.models import GroupLevel
from kelvin.lesson_assignments.models import LessonAssignment
from kelvin.lessons.models import (
    LessonScenario, Lesson, LessonProblemLink, TextTemplate
)
from kelvin.mailing_lists.models import CourseMailingList
from kelvin.problems.answers import Answer
from kelvin.problems.models import Problem, TextResource
from kelvin.result_stats.models import (
    CourseLessonStat, DiagnosticsStat,
    LessonDiagnosticStats, StudentCourseStat, StudentDiagnosticsStat,
)
from kelvin.results.models import CourseLessonResult, CourseLessonSummary
from kelvin.subjects.models import Subject, Theme

from swissknife.assertions import AssertDictsAreEqual, AssertDictsAreStrictEqual
from integration_tests.fixtures.courses import make_course_available_for_student
from integration_tests.fixtures.projects import get_default_project


MAX_LESSONS = 2

User = get_user_model()


@pytest.fixture
def models_data():
    """
    Создаем уроки и предем и возвращаем их id
    """
    subject = Subject.objects.create(id=1)
    owner = User.objects.create(email='1@1.w')
    theme = Theme.objects.create(id=1, subject=subject)
    return subject, [
        Lesson.objects.create(owner=owner, theme=theme,
                              name=u'Тема {0}'.format(index)).id
        for index in range(MAX_LESSONS)
    ]


@pytest.fixture
def course_results(student, content_type):
    """
    Фикстура для тестирования выдачи результатов по курсу
    """

    subject = Subject.objects.create(slug='math')
    theme = Theme.objects.create(id=1, name='theme', code='thm',
                                 subject=subject)
    teacher = User.objects.create(email='1@1.w', is_teacher=True,
                                  username='1')
    course = Course.objects.create(
        name=u'Новый спец курс',
        subject=subject,
        owner=teacher,
        id=1,
        color='#abcabc',
    )

    CourseStudent.objects.create(course=course, student=student)

    problems = []
    for i in range(3):
        problems.append(Problem.objects.create(
            owner=teacher,
            subject=subject,
            markup={},
        ))

    theory1 = TextResource.objects.create(
        owner=teacher,
        subject=subject,
        content_type_object=content_type.instance,
    )

    lessons = []
    for i in range(2):
        lessons.append(
            Lesson.objects.create(
                owner=teacher,
                theme=theme,
                name=u'lesson',
            )
        )

    clessons = []
    for i, lesson in enumerate(lessons):
        clessons.append(CourseLessonLink.objects.create(
            course=course, lesson=lesson, order=i,
            accessible_to_teacher=datetime.datetime(
                year=2010, month=9, day=3, hour=10, minute=10, second=10,
                tzinfo=timezone.utc,
            ),
            date_assignment=datetime.datetime(
                year=2010, month=10, day=3, hour=10, minute=10, second=10,
                tzinfo=timezone.utc,
            ),
            duration=45,
            finish_date=datetime.datetime(
                year=2010, month=10, day=4, hour=10, minute=10, second=10,
                tzinfo=timezone.utc,
            ),
            evaluation_date=datetime.datetime(
                year=2010, month=10, day=5, hour=10, minute=10, second=10,
                tzinfo=timezone.utc,
            ),
        ))

    return {
        'subject': subject,
        'users': {
            'teacher': teacher,
            'student': student,
        },
        'problems': problems,
        'theory': [theory1],
        'lessons': lessons,
        'course': course,
        'clessons': clessons
    }


@pytest.fixture
def mailing_list(request, course):
    if request.param == 'skip':
        # не создаем ничего
        return None

    return CourseMailingList.objects.create(
        course=course,
        slug=request.param,
    )


@pytest.mark.django_db
class TestCourseViewSet(object):
    """
    Тесты рест-интерфейса курсов
    """

    @pytest.mark.parametrize(
        'mailing_list, serialized',
        [
            ('skip', None),
            ('', None),
            ('C7IDTTK2-IRI1', 'C7IDTTK2-IRI1'),
        ],
        indirect=['mailing_list'],
    )
    def test_mailing_list_appearance(self, mailing_list, serialized,
                                     jclient, course):
        """
        Тест отображения поля с идентификатором списка рассылки
        """
        detail_url = reverse('v2:course-detail', args=(course.id,))
        make_course_available_for_student(course, jclient.super_user)

        jclient.login(is_superuser=True)
        response = jclient.get(detail_url)

        assert response.json()['mailing_list'] == serialized

    def test_detail(self, jclient, content_manager, student, student2):
        """
        Тест получения одного курса
        """
        subject = Subject.objects.create(slug='math')
        theme = Theme.objects.create(id=1, name='theme', code='thm',
                                     subject=subject)
        teacher = User.objects.create(email='1@1.w', is_teacher=True,
                                      username='1')
        course = Course.objects.create(
            name=u'Новый спец курс',
            subject=subject,
            owner=teacher,
            id=1,
            color='#abcabc',
            project=get_default_project(),
        )
        lesson1 = Lesson.objects.create(
            id=1,
            owner=teacher,
            theme=theme,
            name=u'Занятие 1',
        )
        lesson2 = Lesson.objects.create(
            id=2,
            owner=teacher,
            theme=theme,
            name=u'Занятие 2',
        )
        link1 = CourseLessonLink.objects.create(
            course=course, lesson=lesson1, order=1)
        link2 = CourseLessonLink.objects.create(
            course=course, lesson=lesson2, order=2,
            accessible_to_teacher=datetime.datetime(
                year=2011, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            date_assignment=datetime.datetime(
                year=2010, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            lesson_block_slug='abc',
        )

        detail_url = reverse('v2:course-detail', args=(course.id,))

        # FIXME перенести в тест доступов
        # ученикам доступно, если в курсе
        make_course_available_for_student(course, student)
        jclient.login(user=student)
        response = jclient.get(detail_url)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        CourseStudent.objects.create(course=course, student=student)
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert len(answer['lessons']) == 2

        # если по занятию нет назнаенных задач, занятие не приходит
        LessonAssignment.objects.create(
            clesson=link1,
            student=student,
            problems=[],
        )
        LessonAssignment.objects.create(
            clesson=link2,
            student=student,
            problems=[123],
        )
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert len(answer['lessons']) == 1
        assert answer['lessons'][0]['id'] == link2.id, (
            u'Должен отображаться только второй курс')

        course.color = '#abcabc'
        course.save()

        course.refresh_from_db()
        expected = {
            'lessons': [
                {
                    'show_all_problems': True,
                    'max_attempts_in_group': 2,
                    'mode': 1,
                    'duration': None,
                    'finish_date': None,
                    'lesson': {
                        'date_updated': int(time.mktime(
                            lesson2.date_updated.timetuple())),
                        'id': lesson2.id,
                        'max_points': 0,
                    },
                    'show_answers_in_last_attempt': True,
                    'id': link2.id,
                    'access_code': None,
                    'accessible_to_teacher': '2011-10-29T10:29:02Z',
                    'date_assignment': '2010-10-29T10:29:02Z',
                    'date_updated': int(time.mktime(
                        link2.date_updated.timetuple())),
                    'date_completed': None,
                    'url': None,
                    'start_date': None,
                    'comment': '',
                    'progress_indicator': None,
                    'visual_mode': LessonScenario.VisualModes.SEPARATE,
                    'lesson_block_slug': 'abc',
                },
                {
                    'show_all_problems': True,
                    'max_attempts_in_group': 2,
                    'mode': 1,
                    'duration': None,
                    'finish_date': None,
                    'lesson': {
                        'date_updated': int(time.mktime(
                            lesson1.date_updated.timetuple())),
                        'id': lesson1.id,
                        'max_points': 0,
                    },
                    'show_answers_in_last_attempt': True,
                    'id': link1.id,
                    'access_code': None,
                    'accessible_to_teacher': None,
                    'date_assignment': None,
                    'date_updated': int(time.mktime(
                        link1.date_updated.timetuple())),
                    'date_completed': None,
                    'url': None,
                    'start_date': None,
                    'comment': '',
                    'progress_indicator': None,
                    'visual_mode': LessonScenario.VisualModes.SEPARATE,
                    'lesson_block_slug': None,
                },
            ],
            'name': u'Новый спец курс',
            'id': course.id,
            'color': course.color,
            'description': '',
            'group_levels': [],
            'cover': None,
            'date_updated': int(time.mktime(course.date_updated.timetuple())),
            'free': False,
            'code': None,
            'subject': 'math',
            'subject_id': subject.id,
            'closed': False,
            'supports_web': True,
            'supports_ios': True,
            'supports_android': False,
            'news': '',
            'info': None,
            'mode': 1,
            'source_courses': [],
            'author': "",
            'mailing_list': None,
            'progress_indicator': None,
            'is_assigned': False,
            'permissions': {'edit': 1, 'roles': 1, 'stats': 1, 'view': 1, 'review': 1},
            'project': get_default_project().id,
        }

        # Анонимам курсы недоступны
        jclient.logout()
        response = jclient.get(detail_url)
        assert response.status_code == 401

        jclient.login(user=teacher)
        make_course_available_for_student(course, teacher)
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        AssertDictsAreStrictEqual(
            actual=answer,
            expected=expected,
            except_keys=['code', ],
            message=u'Неправильный ответ'
        )

        # расширенный вид
        expected_expand = copy.deepcopy(expected)
        expected_expand['lessons'][0]['lesson']['theme'] = {
            'id': theme.id,
            'name': theme.name,
        }
        expected_expand['lessons'][0]['lesson']['name'] = u'Занятие 2'
        expected_expand['lessons'][0]['lesson']['problems_count'] = 0
        del expected_expand['lessons'][0]['lesson']['date_updated']

        expected_expand['lessons'][1]['lesson']['theme'] = {
            'id': theme.id,
            'name': theme.name,
        }
        expected_expand['lessons'][1]['lesson']['name'] = u'Занятие 1'
        expected_expand['lessons'][1]['lesson']['problems_count'] = 0
        del expected_expand['lessons'][1]['lesson']['date_updated']

        response = jclient.get(detail_url + '?expand_lessons=True')
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        AssertDictsAreStrictEqual(
            actual=answer,
            expected=expected_expand,
            except_keys=['code', ],
            message=u'Неправильный ответ'
        )

        # расширенный вид с учениками
        CourseStudent.objects.create(course=course, student=student2)
        expected_expand2 = copy.deepcopy(expected_expand)
        expected_expand2['students'] = [
            {
                'id': student.id,
                'username': u'Петя Иванов',
                'first_name': None,
                'last_name': None,
                'middle_name': None,
                'parent_code': student.parent_code,
                'is_dismissed': False,
            },
            {
                'id': student2.id,
                'username': u'Иван Петров',
                'first_name': None,
                'last_name': None,
                'middle_name': None,
                'parent_code': student2.parent_code,
                'is_dismissed': False,
            },
        ]
        response = jclient.get(detail_url +
                               '?expand_lessons=true&with_students=true')
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()

        AssertDictsAreStrictEqual(
            actual=answer,
            expected=expected_expand2,
            except_keys=['code', ],
            message=u'Неправильный ответ'
        )

        # Контент-менеджеру доступен и расширенный, и стандартный вид
        jclient.logout()
        make_course_available_for_student(course, content_manager)
        jclient.login(user=content_manager)
        response = jclient.get(detail_url + '?expand_lessons=True')
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        expected_expand['permissions'] = {'edit': 0, 'roles': 0, 'stats': 0, 'view': 0, 'review': 0}
        AssertDictsAreStrictEqual(
            actual=answer,
            expected=expected_expand,
            except_keys=['code', ],
            message=u'Неправильный ответ'
        )

        detail_url = reverse('v2:course-detail', args=(course.id,))
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        expected['permissions'] = {'edit': 0, 'roles': 0, 'stats': 0, 'view': 0, 'review': 0}

        AssertDictsAreStrictEqual(
            actual=answer,
            expected=expected,
            except_keys=['code', ],
            message=u'Неправильный ответ'
        )

        # Правильно ли отдаются курсы-источники
        course2 = Course.objects.create(
            name=u'Пустой курс книга 1',
            subject=subject,
            owner=teacher,
            id=2,
            mode=2,
            author='Test author',
        )
        course3 = Course.objects.create(
            name=u'Пустой курс книга 2',
            subject=subject,
            owner=teacher,
            id=3,
            mode=2,
        )
        course.source_courses = [course2, course3]
        course.save()
        expected['source_courses'] = [2, 3]
        expected['date_updated'] = int(time.mktime(
            course.date_updated.timetuple()))

        jclient.logout()
        jclient.login(user=teacher)
        make_course_available_for_student(course2, teacher)

        # Проверим правильно ли сохранились тип и автор одного из курсов
        detail_url = reverse('v2:course-detail', args=(course2.id,))
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer['mode'] == 2, u'Неправильный тип занятия'
        assert answer['author'] == 'Test author', u'Неправильный автор'

        # Проверим правильно ли выдаются курсы-источники
        detail_url = reverse('v2:course-detail', args=(course.id,))
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        expected['permissions'] = {'edit': 1, 'roles': 1, 'stats': 1, 'view': 1, 'review': 1}

        AssertDictsAreStrictEqual(
            actual=answer,
            expected=expected,
            except_keys=['code', ],
            message=u'Неправильный ответ'
        )

    @pytest.mark.xfail
    def test_web(self, jclient, student):
        """
        Тест получения одного курса в веб-формате
        """
        subject = Subject.objects.create(slug='math')
        theme = Theme.objects.create(id=1, name='theme', code='thm',
                                     subject=subject)
        teacher = User.objects.create(email='1@1.w', is_teacher=True,
                                      username='1')
        course = Course.objects.create(
            name=u'Новый спец курс',
            subject=subject,
            owner=teacher,
            id=1,
            color='#abcabc',
        )
        lesson1 = Lesson.objects.create(
            owner=teacher,
            theme=theme,
            name=u'Занятие 1',
        )
        lesson2 = Lesson.objects.create(
            owner=teacher,
            theme=theme,
            name=u'Занятие 2',
        )
        link1 = CourseLessonLink.objects.create(
            course=course, lesson=lesson1, order=1)
        link2 = CourseLessonLink.objects.create(
            course=course, lesson=lesson2, order=2,
            accessible_to_teacher=datetime.datetime(
                year=2011, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            date_assignment=datetime.datetime(
                year=2010, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
        )
        CourseStudent.objects.create(course=course, student=student)
        mail_list = CourseMailingList.objects.create(
            course=course,
            slug='AVADAKEDAVRA-666',
        )

        web_url = reverse('v2:course-web', args=(course.id,))
        expected = {
            'lessons': [
                {
                    'show_all_problems': True,
                    'max_attempts_in_group': 2,
                    'mode': 1,
                    'duration': None,
                    'finish_date': None,
                    'lesson': {
                        'date_updated': int(time.mktime(
                            lesson2.date_updated.timetuple())),
                        'id': lesson2.id,
                    },
                    'show_answers_in_last_attempt': True,
                    'id': link2.id,
                    'access_code': None,
                    'accessible_to_teacher': True,
                    'date_assignment': '2010-10-29T10:29:02Z',
                    'date_updated': int(time.mktime(
                        link2.date_updated.timetuple())),
                    'date_completed': None,
                    'url': None,
                    'start_date': None,
                    'comment': '',
                    'date_assignment_passed': True,
                    'on_air': None,
                    'results': None,
                    'progress_indicator': None,
                    'visual_mode': LessonScenario.VisualModes.SEPARATE,
                    'lesson_block_slug': None,
                },
                {
                    'show_all_problems': True,
                    'max_attempts_in_group': 2,
                    'mode': 1,
                    'duration': None,
                    'finish_date': None,
                    'lesson': {
                        'date_updated': int(time.mktime(
                            lesson1.date_updated.timetuple())),
                        'id': lesson1.id,
                    },
                    'show_answers_in_last_attempt': True,
                    'id': link1.id,
                    'access_code': None,
                    'accessible_to_teacher': False,
                    'date_assignment': None,
                    'date_updated': int(time.mktime(
                        link1.date_updated.timetuple())),
                    'date_completed': None,
                    'url': None,
                    'start_date': None,
                    'comment': '',
                    'date_assignment_passed': False,
                    'on_air': None,
                    'results': None,
                    'progress_indicator': None,
                    'visual_mode': LessonScenario.VisualModes.SEPARATE,
                    'lesson_block_slug': None,
                },
            ],
            'name': u'Новый спец курс',
            'id': course.id,
            'color': course.color,
            'description': '',
            'group_levels': [],
            'cover': None,
            'date_updated': int(time.mktime(course.date_updated.timetuple())),
            'free': False,
            'code': None,
            'subject': 'math',
            'closed': False,
            'supports_web': True,
            'supports_ios': True,
            'supports_android': False,
            'news': '',
            'info': None,
            'mode': 1,
            'author': "",
            'mailing_list': mail_list.slug,
            'progress_indicator': None,
        }
        jclient.login(user=student)
        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer == expected, u'Неправильный ответ'

        # неназначенные занятия не должны отдаваться ученику
        # учитель при этом по-прежнему видит занятия
        LessonAssignment.objects.create(
            clesson=link1,
            student=student,
            problems=[],
        )
        # учитель видит курсы-источники
        expected['source_courses'] = []
        jclient.login(user=teacher)
        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer == expected, u'Неправильный ответ'
        expected.pop('source_courses')

        jclient.login(user=student)
        expected['lessons'] = expected['lessons'][:1]
        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer == expected, u'Неправильный ответ'

        # в ответ добавляются результаты по контрольной работе или
        # диагностике
        lesson3 = Lesson.objects.create(
            owner=teacher,
            theme=theme,
            name=u'Занятие 3',
        )
        link3 = CourseLessonLink.objects.create(
            course=course, lesson=lesson3, order=3,
            accessible_to_teacher=datetime.datetime(
                year=2011, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            date_assignment=datetime.datetime(
                year=2010, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            mode=CourseLessonLink.CONTROL_WORK_MODE,
            duration=45,
            finish_date=datetime.datetime(
                year=3010, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            evaluation_date=datetime.datetime(
                year=3010, month=11, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
        )
        expected['lessons'] += [{
            'show_all_problems': True,
            'max_attempts_in_group': 2,
            'mode': 2,
            'duration': 45,
            'lesson': {
                'date_updated': int(time.mktime(
                    lesson3.date_updated.timetuple())),
                'id': lesson3.id,
            },
            'show_answers_in_last_attempt': True,
            'id': link3.id,
            'access_code': None,
            'accessible_to_teacher': True,
            'date_assignment': '2010-10-29T10:29:02Z',
            'date_updated': int(time.mktime(link3.date_updated.timetuple())),
            'date_completed': None,
            'url': None,
            'start_date': None,
            'comment': '',
            'date_assignment_passed': True,
            'on_air': None,
            'results': None,
            'evaluation_date': '3010-11-29T10:29:02Z',
            'finish_date': '3010-10-29T10:29:02Z',
            'is_evaluated': False,
            'viewed': None,
            'is_finished': False,
            'started_at': None,
            'time_left': None,
            'progress_indicator': None,
            'visual_mode': LessonScenario.VisualModes.SEPARATE,
            'lesson_block_slug': None,
        }]

        # после изменения связи обновляется курс
        course.refresh_from_db()
        expected['date_updated'] = int(
            time.mktime(course.date_updated.timetuple()))

        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer == expected, u'Неправильный ответ'

        link3.mode = CourseLessonLink.DIAGNOSTICS_MODE
        link3.save()
        expected['lessons'][1]['mode'] = 4
        expected['lessons'][1]['date_updated'] = int(time.mktime(
            link3.date_updated.timetuple()))

        # после изменения связи обновляется курс
        course.refresh_from_db()
        expected['date_updated'] = int(
            time.mktime(course.date_updated.timetuple()))

        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer == expected, u'Неправильный ответ'

        # наличие результата по диагностике (или контрольной)
        result = CourseLessonResult.objects.create(
            summary=CourseLessonSummary.objects.create(
                clesson=link3,
                student=student,
            ),
            max_points=100,
            viewed=True,
        )
        result.date_created = '2012-10-29T10:29:02Z'
        result.save()
        expected['lessons'][1]['time_left'] = 0
        expected['lessons'][1]['started_at'] = '2012-10-29T10:29:02Z'
        expected['lessons'][1]['viewed'] = True

        # после изменения связи обновляется курс
        course.refresh_from_db()
        expected['date_updated'] = int(
            time.mktime(course.date_updated.timetuple()))

        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()

        # TODO Should not be time dependant!
        link2.refresh_from_db()
        link3.refresh_from_db()
        expected['lessons'][0]['date_updated'] = int(
            time.mktime(link2.date_updated.timetuple())
        )
        expected['lessons'][1]['date_updated'] = int(
            time.mktime(link3.date_updated.timetuple())
        )

        assert answer == expected, u'Неправильный ответ'

    @pytest.mark.xfail
    def test_results_not_assigned_problem(self, jclient, course_results):
        """
        Тест получения результатов в courses/<id>/web в случае когда
            в курсе 1 тренировка
            2 задачи, одна из которых назначена, вотрая нет
            резльтат ученика есть по всем задачам (правильный везде)
        Ожидаемый результат: { "right": 1, "wrong": 0, "all": 1, }
        """

        course_results['clessons'][0].mode = CourseLessonLink.TRAINING_MODE
        course_results['clessons'][0].save()

        lesson_problem1 = LessonProblemLink.objects.create(
            lesson=course_results['lessons'][0],
            problem=course_results['problems'][0],
            order=1, options={'max_attempts': 5, 'show_tips': True},
        )
        lesson_problem2 = LessonProblemLink.objects.create(
            lesson=course_results['lessons'][0],
            problem=course_results['problems'][1],
            order=2, options={'max_attempts': 5, 'show_tips': True},
        )

        LessonAssignment.objects.create(
            clesson=course_results['clessons'][0],
            student=course_results['users']['student'],
            problems=[lesson_problem1.id])

        CourseLessonResult.objects.create(
            summary=CourseLessonSummary.objects.create(
                clesson=course_results['clessons'][0],
                student=course_results['users']['student'],
            ),
            completed=True,
            points=15,
            max_points=15,
            answers={
                str(lesson_problem.id): [
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
                ] for lesson_problem in [lesson_problem1, lesson_problem2]
            }
        )

        web_url = reverse('v2:course-web', args=(course_results['course'].id,))
        course_results['course'].refresh_from_db()

        jclient.login(user=course_results['users']['student'])
        response = jclient.get(web_url)

        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer['lessons'][0]['results'] == {
            "right": 1,
            "wrong": 0,
            "all": 1,
        }, u'Неправильный ответ'

    @pytest.mark.xfail
    def test_results_not_assigned_problem2(self, jclient, course_results):
        """
        Тест получения результатов в courses/<id>/web в случае когда
            тренировка 1
                задача 1 (привязана)
                задача 2 (не привязана)
            тренировка 2
                задача 2 (привязана)
            резльтат ученика есть по всем задачам (правильный везде)
        Ожидаемый результат:
            { "right": 1, "wrong": 0, "all": 1, }
            { "right": 1, "wrong": 0, "all": 1, }
        """

        course_results['clessons'][0].mode = CourseLessonLink.TRAINING_MODE
        course_results['clessons'][0].save()

        lesson1_problem1 = LessonProblemLink.objects.create(
            lesson=course_results['lessons'][0],
            problem=course_results['problems'][0],
            order=1, options={'max_attempts': 5, 'show_tips': True},
        )
        lesson1_problem2 = LessonProblemLink.objects.create(
            lesson=course_results['lessons'][0],
            problem=course_results['problems'][1],
            order=2, options={'max_attempts': 5, 'show_tips': True},
        )
        lesson2_problem2 = LessonProblemLink.objects.create(
            lesson=course_results['lessons'][1],
            problem=course_results['problems'][1],
            order=1, options={'max_attempts': 5, 'show_tips': True},
        )

        LessonAssignment.objects.create(
            clesson=course_results['clessons'][0],
            student=course_results['users']['student'],
            problems=[lesson1_problem1.id])
        LessonAssignment.objects.create(
            clesson=course_results['clessons'][1],
            student=course_results['users']['student'],
            problems=[lesson2_problem2.id])

        CourseLessonResult.objects.create(
            summary=CourseLessonSummary.objects.create(
                clesson=course_results['clessons'][0],
                student=course_results['users']['student'],
            ),
            completed=True,
            points=15,
            max_points=15,
            answers={
                str(lesson_problem.id): [
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
                ] for lesson_problem in [lesson1_problem1, lesson1_problem2]
            }
        )
        CourseLessonResult.objects.create(
            summary=CourseLessonSummary.objects.create(
                clesson=course_results['clessons'][1],
                student=course_results['users']['student'],
            ),
            completed=True,
            points=15,
            max_points=15,
            answers={
                str(lesson_problem.id): [
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
                ] for lesson_problem in [lesson2_problem2]
            }
        )

        web_url = reverse('v2:course-web', args=(course_results['course'].id,))
        course_results['course'].refresh_from_db()

        jclient.login(user=course_results['users']['student'])
        response = jclient.get(web_url)

        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer['lessons'][0]['results'] == {
            "right": 1,
            "wrong": 0,
            "all": 1,
        }, u'Неправильный ответ по уроку 1'
        assert answer['lessons'][1]['results'] == {
            "right": 0,
            "wrong": 1,
            "all": 1,
        }, u'Неправильный ответ по уроку 2'

    @pytest.mark.xfail()
    def test_results_not_assigned_all_problems(self, jclient, course_results):
        """
        Тест получения результатов в courses/<id>/web в случае когда
            в курсе 1 тренировка
            2 задачи, и нет LessonAssignment (означает, что назначено все)
            резльтат ученика есть по всем задачам (правильный + неправильный)
        Ожидаемый результат: { "right": 1, "wrong": 1, "all": 2, }
        """

        course_results['clessons'][0].mode = CourseLessonLink.TRAINING_MODE
        course_results['clessons'][0].save()

        lesson_problem1 = LessonProblemLink.objects.create(
            lesson=course_results['lessons'][0],
            problem=course_results['problems'][0],
            order=1, options={'max_attempts': 5, 'show_tips': True},
        )
        lesson_problem2 = LessonProblemLink.objects.create(
            lesson=course_results['lessons'][0],
            problem=course_results['problems'][1],
            order=2, options={'max_attempts': 5, 'show_tips': True},
        )

        CourseLessonResult.objects.create(
            summary=CourseLessonSummary.objects.create(
                clesson=course_results['clessons'][0],
                student=course_results['users']['student'],
            ),
            completed=True,
            points=15,
            max_points=15,
            answers={
                str(lesson_problem1.id): [
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
                str(lesson_problem2.id): [
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
            }
        )

        web_url = reverse('v2:course-web', args=(course_results['course'].id,))
        course_results['course'].refresh_from_db()

        jclient.login(user=course_results['users']['student'])
        response = jclient.get(web_url)

        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer['lessons'][0]['results'] == {
            "right": 1,
            "wrong": 1,
            "all": 2,
        }, u'Неправильный ответ'

    @pytest.mark.xfail
    def test_results_anonymous_passed_test(self, jclient, course_results):
        """
        Тест получения результатов в courses/<id>/web в случае когда
            в курсе 1 завершенная контрольная, доступная анонимам
            2 задачи, и ничего не назначено (означает, что назначено все)
        Ожидаемый результат: { "right": 0, "wrong": 2, "all": 2, }
        """

        course_results['course'].allow_anonymous = True
        course_results['course'].save()

        course_results['clessons'][0].mode = CourseLessonLink.CONTROL_WORK_MODE
        course_results['clessons'][0].save()

        lesson_problem1 = LessonProblemLink.objects.create(
            lesson=course_results['lessons'][0],
            problem=course_results['problems'][0],
            order=1,
        )
        lesson_problem2 = LessonProblemLink.objects.create(
            lesson=course_results['lessons'][0],
            problem=course_results['problems'][1],
            order=2, options={'max_attempts': 5, 'show_tips': True},
        )

        web_url = reverse('v2:course-web', args=(course_results['course'].id,))
        course_results['course'].refresh_from_db()

        response = jclient.get(web_url)

        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer['lessons'][0]['results'] == {
            "right": 0,
            "wrong": 2,
            "all": 2,
        }, u'Неправильный ответ'

    @pytest.mark.xfail
    def test_web_results(self, jclient, student, content_type):
        """
        Тест получение результатов в courses/<id>/web

        :type content_type: integration_tests.kelvin.conftest.ContentTypeFixture # noqa
        """
        subject = Subject.objects.create(slug='math')
        theme = Theme.objects.create(id=1, name='theme', code='thm',
                                     subject=subject)
        teacher = User.objects.create(email='1@1.w', is_teacher=True,
                                      username='1')
        course = Course.objects.create(
            name=u'Новый спец курс',
            subject=subject,
            owner=teacher,
            id=1,
            color='#abcabc',
        )

        CourseStudent.objects.create(course=course, student=student)

        web_url = reverse('v2:course-web', args=(course.id,))

        problem1 = Problem.objects.create(
            owner=teacher, subject=subject, markup={},
        )
        problem2 = Problem.objects.create(
            owner=teacher, subject=subject, markup={},
        )
        problem3 = Problem.objects.create(
            owner=teacher, subject=subject, markup={},
        )
        theory1 = TextResource.objects.create(
            owner=teacher,
            subject=subject,
            content_type_object=content_type.instance,
        )

        # 1. Контрольная работа, не закончилась.
        lesson1 = Lesson.objects.create(
            owner=teacher,
            theme=theme,
            name=u'Занятие 1 (контрольная работа, не проверена)',
        )

        lesson1_problem1 = LessonProblemLink.objects.create(
            lesson=lesson1, problem=problem1, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )

        clesson1 = CourseLessonLink.objects.create(
            course=course, lesson=lesson1, order=1,
            accessible_to_teacher=datetime.datetime(
                year=2011, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            date_assignment=datetime.datetime(
                year=2010, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            mode=CourseLessonLink.CONTROL_WORK_MODE,
            duration=45,
            finish_date=datetime.datetime(
                year=3012, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            evaluation_date=datetime.datetime(
                year=3012, month=11, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
        )

        LessonAssignment.objects.create(clesson=clesson1, student=student,
                                        problems=[lesson1_problem1.id])

        CourseLessonResult.objects.create(
            summary=CourseLessonSummary.objects.create(
                clesson=clesson1,
                student=student,
            ),
            completed=True,
            points=15,
            max_points=15,
            answers={
                str(lesson1_problem1.id): [
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

        course.refresh_from_db()
        jclient.login(user=student)
        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer['lessons'][-1]['results'] is None, u'Неправильный ответ'

        # 2. Контрольная работа. Закончилась, не проверена.
        lesson2 = Lesson.objects.create(
            owner=teacher,
            theme=theme,
            name=u'Занятие 2 (контрольная работа)',
        )

        lesson2_problem1 = LessonProblemLink.objects.create(
            lesson=lesson2, problem=problem1, order=2,
            options={'max_attempts': 5, 'show_tips': True},
        )

        clesson2 = CourseLessonLink.objects.create(
            course=course, lesson=lesson1, order=1,
            accessible_to_teacher=datetime.datetime(
                year=2011, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            date_assignment=datetime.datetime(
                year=2010, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            mode=CourseLessonLink.CONTROL_WORK_MODE,
            duration=45,
            finish_date=datetime.datetime(
                year=2012, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            evaluation_date=datetime.datetime(
                year=3012, month=11, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
        )

        LessonAssignment.objects.create(clesson=clesson2, student=student,
                                        problems=[lesson2_problem1.id])

        CourseLessonResult.objects.create(
            summary=CourseLessonSummary.objects.create(
                clesson=clesson2,
                student=student,
            ),
            completed=True,
            points=15,
            max_points=15,
            answers={
                str(lesson2_problem1.id): [
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

        course.refresh_from_db()
        jclient.login(user=student)
        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer['lessons'][-1]['results'] is None, u'Неправильный ответ'

        # 3. Контрольная работа. Закончилась, проверена, ученик что-то ответил
        lesson3 = Lesson.objects.create(
            owner=teacher,
            theme=theme,
            name=u'Занятие 3',
        )

        lesson3_problem1 = LessonProblemLink.objects.create(
            lesson=lesson3, problem=problem1, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )
        lesson3_problem2 = LessonProblemLink.objects.create(
            lesson=lesson3, problem=problem2, order=2,
            options={'max_attempts': 5, 'show_tips': True},
        )

        clesson3 = CourseLessonLink.objects.create(
            course=course, lesson=lesson3, order=3,
            accessible_to_teacher=datetime.datetime(
                year=2011, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            date_assignment=datetime.datetime(
                year=2010, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            mode=CourseLessonLink.CONTROL_WORK_MODE,
            duration=45,
            finish_date=datetime.datetime(
                year=2012, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            evaluation_date=datetime.datetime(
                year=2012, month=11, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
        )

        LessonAssignment.objects.create(clesson=clesson3, student=student,
                                        problems=[lesson3_problem1.id,
                                                  lesson3_problem2.id])

        CourseLessonResult.objects.create(
            summary=CourseLessonSummary.objects.create(
                clesson=clesson3,
                student=student,
            ),
            completed=True,
            points=15,
            max_points=15,
            answers={
                str(lesson3_problem1.id): [
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
                str(lesson3_problem2.id): [
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
            },
        )

        course.refresh_from_db()
        jclient.login(user=student)
        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer['lessons'][-1]['results'] == {
            "right": 1,
            "wrong": 1,
            "all": 2,
        }, u'Неправильный ответ'

        # 4. Контрольная работа. Закончилась, проверена, ученик ее проспал
        lesson4 = Lesson.objects.create(
            owner=teacher,
            theme=theme,
            name=u'Занятие 4',
        )

        lesson4_problem1 = LessonProblemLink.objects.create(
            lesson=lesson4, problem=problem1, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )
        lesson4_problem2 = LessonProblemLink.objects.create(
            lesson=lesson4, problem=problem2, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )

        clesson4 = CourseLessonLink.objects.create(
            course=course, lesson=lesson4, order=4,
            accessible_to_teacher=datetime.datetime(
                year=2011, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            date_assignment=datetime.datetime(
                year=2010, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            mode=CourseLessonLink.CONTROL_WORK_MODE,
            duration=45,
            finish_date=datetime.datetime(
                year=2012, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            evaluation_date=datetime.datetime(
                year=2012, month=11, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
        )

        LessonAssignment.objects.create(clesson=clesson4, student=student,
                                        problems=[lesson4_problem1.id,
                                                  lesson4_problem2.id])

        course.refresh_from_db()
        jclient.login(user=student)
        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer['lessons'][-1]['results'] == {
            "right": 0,
            "wrong": 2,
            "all": 2,
        }, u'Неправильный ответ'

        # 5. Тренировка. Ученик что-то прислал + Есть теория
        lesson5 = Lesson.objects.create(
            owner=teacher,
            theme=theme,
            name=u'Занятие 5',
        )

        lesson5_problem1 = LessonProblemLink.objects.create(
            lesson=lesson5, problem=problem1, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )
        lesson5_problem2 = LessonProblemLink.objects.create(
            lesson=lesson5, problem=problem2, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )
        lesson5_problem3 = LessonProblemLink.objects.create(
            lesson=lesson5, problem=problem3, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )
        lesson5_theory1 = LessonProblemLink.objects.create(
            lesson=lesson5, theory=theory1, order=1,
        )

        clesson5 = CourseLessonLink.objects.create(
            course=course, lesson=lesson5, order=5,
            accessible_to_teacher=datetime.datetime(
                year=2011, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            date_assignment=datetime.datetime(
                year=2010, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            mode=CourseLessonLink.TRAINING_MODE,
        )

        LessonAssignment.objects.create(clesson=clesson5, student=student,
                                        problems=[lesson5_problem1.id,
                                                  lesson5_problem2.id,
                                                  lesson5_problem3.id,
                                                  lesson5_theory1.id])

        CourseLessonResult.objects.create(
            summary=CourseLessonSummary.objects.create(
                clesson=clesson5,
                student=student,
            ),
            completed=True,
            points=15,
            max_points=15,
            answers={
                str(lesson5_problem1.id): [
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
                str(lesson5_problem2.id): [
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
            },
        )

        course.refresh_from_db()
        jclient.login(user=student)
        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer['lessons'][-1]['results'] == {
            "right": 1,
            "wrong": 1,
            "all": 3,
        }, u'Неправильный ответ'

        # 6. Тренировка. Ученик ничего не прислал
        lesson6 = Lesson.objects.create(
            owner=teacher,
            theme=theme,
            name=u'Занятие 6',
        )

        lesson6_problem1 = LessonProblemLink.objects.create(
            lesson=lesson6, problem=problem1, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )

        clesson6 = CourseLessonLink.objects.create(
            course=course, lesson=lesson6, order=6,
            accessible_to_teacher=datetime.datetime(
                year=2011, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            date_assignment=datetime.datetime(
                year=2010, month=10, day=29, hour=10, minute=29, second=2,
                tzinfo=timezone.utc,
            ),
            mode=CourseLessonLink.TRAINING_MODE,
        )

        LessonAssignment.objects.create(clesson=clesson6, student=student,
                                        problems=[lesson6_problem1.id])

        course.refresh_from_db()
        jclient.login(user=student)
        response = jclient.get(web_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        answer = response.json()
        assert answer['lessons'][-1]['results'] is None, u'Неправильный ответ'

    def test_assigned(self, student, student2, teacher, subject_model,
                      jclient):
        """
        Тест списка назначенных ученику курсов
        """
        course1 = Course.objects.create(name=u'Курс 1', subject=subject_model,
                                        owner=teacher)
        CourseStudent.objects.create(course=course1, student=student)
        course2 = Course.objects.create(name=u'Курс 2', subject=subject_model,
                                        owner=teacher)
        CourseStudent.objects.create(course=course2, student=student)
        CourseStudent.objects.create(course=course2, student=student2)
        course3 = Course.objects.create(name=u'Курс 3', subject=subject_model,
                                        owner=teacher, supports_android=False)
        CourseStudent.objects.create(course=course3, student=student)
        CourseStudent.objects.create(course=course3, student=student2)
        course4 = Course.objects.create(name=u'Курс 4', subject=subject_model,
                                        owner=teacher, free=True)
        course5 = Course.objects.create(name=u'Курс 4', subject=subject_model,
                                        owner=teacher, supports_android=False)
        assigned_url = reverse('v2:course-assigned')

        # запрос курсов для всех
        jclient.login(user=student)
        expected_ids = {course1.id, course2.id, course3.id}
        response = jclient.get(assigned_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert set(course['id'] for course in response.json()) == expected_ids

        # запрос курсов родителем
        parent = User.objects.create(username='parent', is_parent=True)
        parent.parent_profile.children.add(student)
        jclient.login(user=parent)
        expected_ids = {course1.id, course2.id, course3.id}
        response = jclient.get(assigned_url, {'child': student.id})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert set(course['id'] for course in response.json()) == expected_ids

        # запрос курсов не своего ребенка
        response = jclient.get(assigned_url, {'child': student2.id})
        assert response.status_code == 404, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )

        # запрос с неправильным параметром ребенка
        response = jclient.get(assigned_url, {'child': 'baby'})
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )

    def test_student_stats(self, jclient, teacher, student, theme_model,
                           subject_model, theory_model, content_manager):
        """
        Тест результатов ученика по курсу
        """
        # курс
        course = Course.objects.create(
            name=u'Новый спец курс', subject=subject_model, owner=teacher,
        )

        # занятия
        lesson1 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Урок 1')
        lesson2 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Урок 2')
        lesson3 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Урок 3')
        lesson4 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Урок 4')

        # вопросы
        problem1 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={}
        )
        problem2 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={}
        )
        problem3 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={}
        )
        problem4 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={}
        )

        # связи занятие-задача
        lesson1_problem1 = LessonProblemLink.objects.create(
            lesson=lesson1, problem=problem1, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )
        lesson1_problem2 = LessonProblemLink.objects.create(
            lesson=lesson1, problem=problem2, order=2,
            options={'max_attempts': 5, 'show_tips': True},
        )
        lesson1_problem3 = LessonProblemLink.objects.create(
            lesson=lesson1, problem=problem3, order=3,
            options={'max_attempts': 3, 'show_tips': True},
        )

        # Теория для первого занятия. Не должна попасть в сводку
        lesson1_theory = LessonProblemLink.objects.create(
            lesson=lesson1, theory=theory_model, order=4,
            type=LessonProblemLink.TYPE_THEORY,
        )

        lesson2_problem3 = LessonProblemLink.objects.create(
            lesson=lesson2, problem=problem3, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )
        lesson2_problem4 = LessonProblemLink.objects.create(
            lesson=lesson2, problem=problem4, order=2,
            options={'max_attempts': 5, 'show_tips': True},
        )
        lesson3_problem2 = LessonProblemLink.objects.create(
            lesson=lesson3, problem=problem2, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )

        lesson4_problem1 = LessonProblemLink.objects.create(
            lesson=lesson4, problem=problem1, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )

        # связи курс-занятие
        clesson1 = CourseLessonLink.objects.create(
            lesson=lesson1, course=course, order=1)
        clesson2 = CourseLessonLink.objects.create(
            lesson=lesson2, course=course, order=1)
        clesson3 = CourseLessonLink.objects.create(
            lesson=lesson3, course=course, order=1)
        clesson4 = CourseLessonLink.objects.create(
            lesson=lesson4, course=course, order=1,
            mode=CourseLessonLink.CONTROL_WORK_MODE,
        )

        course_owner = {
            'id': teacher.id,
            'username': teacher.username,
            'last_name': teacher.last_name,
            'first_name': teacher.first_name,
            'middle_name': teacher.middle_name,
        }
        # адрес получения результатов
        stats_url = reverse('v2:course-student-stats', args=(course.id,))
        make_course_available_for_student(course, teacher)
        jclient.login(user=teacher)

        # запрос без параметра
        response = jclient.get(stats_url)
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )

        # запрос учителем не "своего" ученика
        response = jclient.get(stats_url + '?student={0}'.format(student.id))
        assert response.status_code == 404, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )

        # добавляем ученика в группу, нет ни одного назначенного занятия
        CourseStudent.objects.create(course=course, student=student)
        make_course_available_for_student(course, student)
        expected = {
            'success_percent': None,
            'journal': [],
            'course_owner': course_owner,
        }
        response = jclient.get(stats_url + '?student={0}'.format(student.id))
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert response.json() == expected

        # назначаем первое занятие
        date_now = timezone.now().replace(second=0, microsecond=0)
        clesson1.date_assignment = date_now
        clesson1.save()

        LessonAssignment.objects.create(
            clesson=clesson1, student=student,
            problems=[
                lesson1_problem2.id, lesson1_problem3.id, lesson1_theory.id],
        )

        expected = {
            'success_percent': None,
            'journal': [
                {
                    'date': clesson1.date_created.strftime(
                        settings.REST_FRAMEWORK['DATETIME_FORMAT']),
                    'id': clesson1.id,
                    'name': u'Урок 1',
                    'mode': 1,
                    'clesson_points': 0,
                    'clesson_max_points': 0,
                    'max_points': 0,
                    'points': 0,
                    'completed': False,
                    'problems': [
                        {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                        {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                    ],
                },
            ],
            'course_owner': course_owner,
        }
        response = jclient.get(stats_url + '?student={0}'.format(student.id))
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert response.json() == expected

        # добавляем результаты по первому занятию и статистику
        summary = CourseLessonSummary.objects.create(
            clesson=clesson1,
            student=student,
        )
        CourseLessonResult.objects.create(
            summary=summary,
            completed=False,
            points=2,
            max_points=2,
            answers={
                str(lesson1_problem1.id): [
                    # неназначенная задача, в статистике ее не будет
                    {
                        'mistakes': 1,
                        'max_mistakes': 1,
                        'completed': False,
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
                str(lesson1_problem3.id): [
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
                        'spent_time': 30,
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
        stat, created = CourseLessonStat.objects.get_or_create(
            clesson=clesson1,
        )
        assert not created, u'Статистика должна быть создана селери-таском'
        stat.percent_complete = 30
        stat.average_points = 36
        stat.average_max_points = 47
        stat.save()

        expected = {
            'success_percent': 100,
            'journal': [
                {
                    'date': clesson1.date_created.strftime(
                        settings.REST_FRAMEWORK['DATETIME_FORMAT']),
                    'id': clesson1.id,
                    'name': u'Урок 1',
                    'mode': 1,
                    'clesson_points': 36.,
                    'clesson_max_points': 47.,
                    'max_points': 2,
                    'points': 2,
                    'completed': False,
                    'problems': [
                        {
                            'answered': False,
                            'status': None,
                            'type': 1,
                            'max_points': 1,
                        },
                        {
                            'status': Answer.SUMMARY_CORRECT,
                            'answered': True,
                            'attempt_number': 2,
                            'time': 50,
                            'max_attempts': 3,
                            'type': 1,
                            'points': 1,
                            'max_points': 1,
                        },
                    ],
                },
            ],
            'course_owner': course_owner,
        }
        response = jclient.get(stats_url + '?student={0}'.format(student.id))
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert response.json() == expected

        # назначаем второе занятие
        date2 = timezone.now().replace(second=0, microsecond=0) - datetime.timedelta(days=1)
        clesson2.date_assignment = date2
        clesson2.save()

        # запись должна оказаться первой -
        # не понимаю как это работало, строки в journal сортируются по полю date_updated (которое всегда now())
        expected['journal'] += [{
            'date': clesson2.date_created.strftime(
                settings.REST_FRAMEWORK['DATETIME_FORMAT']),
            'id': clesson2.id,
            'name': u'Урок 2',
            'mode': 1,
            'clesson_points': 0,
            'clesson_max_points': 0,
            'max_points': 0,
            'points': 0,
            'completed': False,
            'problems': [
                {
                    'answered': False,
                    'status': None,
                    'type': 1,
                    'max_points': 1,
                },
                {
                    'answered': False,
                    'status': None,
                    'type': 1,
                    'max_points': 1,
                },
            ],

        }]
        response = jclient.get(stats_url + '?student={0}'.format(student.id))
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        resp = response.json()
        # print("json=", json.dumps(resp, sort_keys=True, indent=2))
        # print("expected=", json.dumps(expected, sort_keys=True, indent=2))
        assert json.dumps(resp, sort_keys=True) == json.dumps(expected, sort_keys=True)

        # Контент-менеджер тоже может посмотреть статистику
        jclient.logout()
        jclient.login(user=content_manager)
        make_course_available_for_student(course, content_manager)
        response = jclient.get(stats_url + '?student={0}'.format(student.id))
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert response.json() == expected

        # если ученику не назначены задачи, то занятие не отображается
        LessonAssignment.objects.create(
            clesson=clesson2, student=student,
            problems=[],
        )
        expected['journal'] = expected['journal'][:1]
        jclient.login(user=teacher)
        response = jclient.get(stats_url + '?student={0}'.format(student.id))
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert response.json() == expected

        # назначаем контрольную работу
        jclient.logout()
        jclient.login(user=content_manager)
        date4 = timezone.now()
        clesson4.date_assignment = date4
        clesson4.evaluation_date = date4 + datetime.timedelta(days=1)
        clesson4.save()
        expected['journal'].append({
            'date': date4.strftime(
                settings.REST_FRAMEWORK['DATETIME_FORMAT']),
            'id': clesson4.id,
            'name': u'Урок 4',
            'mode': 2,
            'clesson_max_points': 0,
            'max_points': 0,
            'completed': False,
            'problems': [
                {
                    'answered': False,
                    'status': None,
                    'type': 1,
                    'max_points': 1,
                },
            ],
        })
        response = jclient.get(stats_url + '?student={0}'.format(student.id))
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert json.dumps(response.json(), sort_keys=True) == json.dumps(expected, sort_keys=True)

        # "Публикуем" контрольную работу
        clesson4.evaluation_date = date4 - datetime.timedelta(days=1)
        clesson4.save()

        # Задача из незавершенной должна стать неправильной
        # Также должны появиться очки
        expected['journal'][-1] = {
            'date': date4.strftime(
                settings.REST_FRAMEWORK['DATETIME_FORMAT']),
            'id': clesson4.id,
            'name': u'Урок 4',
            'mode': 2,
            'clesson_points': 0,
            'clesson_max_points': 0,
            'points': 0,
            'max_points': 0,
            'completed': False,
            'problems': [
                {
                    'answered': True,
                    'status': Answer.SUMMARY_INCORRECT,
                    'type': 1,
                    'max_points': 1,
                },
            ],
        }
        response = jclient.get(stats_url + '?student={0}'.format(student.id))
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert response.json() == expected

    def test_add_clesson(self, jclient, teacher, some_owner, subject_model,
                         theme_model):
        """
        Тест добавления занятия в курс
        """
        # курсы
        course1 = Course.objects.create(
            name=u'Открытый спец курс', subject=subject_model, owner=some_owner,
        )
        course2 = Course.objects.create(
            name=u'Учительский курс', subject=subject_model, owner=teacher,
        )

        # занятия
        lesson1 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Урок 1')
        lesson2 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Урок 2')
        lesson3 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Урок 3')

        # связи курс-занятие
        now = timezone.now()
        course1_lesson1 = CourseLessonLink.objects.create(
            lesson=lesson1, course=course1, order=1,
            accessible_to_teacher=now, lesson_editable=False,
            date_assignment=now,
        )
        course1_lesson2 = CourseLessonLink.objects.create(
            lesson=lesson2, course=course1, order=2,
            accessible_to_teacher=None, lesson_editable=False,
        )
        course2_lesson3 = CourseLessonLink.objects.create(
            lesson=lesson3, course=course2, order=1,
            accessible_to_teacher=now,
        )

        post_url = reverse('v2:course-add-clesson', args=(course2.id,))
        jclient.login(user=teacher)

        # в данных должен быть идентификатор
        response = jclient.post(post_url, {})
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert response.json() == ErrorsComposer.compose_response_body(
            code='invalid',
            message='`id` field required',
        )

        # должен быть правильный идентификатор
        response = jclient.post(post_url, {'id': 'qwerty'})
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert response.json() == ErrorsComposer.compose_response_body(
            code='invalid',
            message='wrong `id` value',
        )

        # урок должен быть доступен учителю
        response = jclient.post(post_url, {'id': course1_lesson2.id})
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert response.json() == ErrorsComposer.compose_response_body(
            code='permission_denied',
            message='lesson not accessible',
        )
        make_course_available_for_student(course2, teacher)

        # правильный запрос
        response = jclient.post(post_url, {'id': course1_lesson1.id})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert response.content.decode() == ''

        links = course2.courselessonlink_set.all().order_by('order')
        assert len(links) == 2, u'Должно быть 2 занятия в курсе'
        assert links[0].id == course2_lesson3.id, u'Первое занятие старое'
        new_clesson = links[1]
        assert new_clesson.order == 2, (
            u'Занятие должно стать последним в списке')
        assert new_clesson.date_assignment is None, (
            u'Занятие не должно быть назначенным')
        assert new_clesson.accessible_to_teacher < timezone.now(), (
            u'Занятие должно быть доступно учителю')
        assert new_clesson.lesson_editable is False, (
            u'Занятие должно быть нередактируемым')

    @pytest.mark.django_db
    def test_stats(self, jclient, teacher, subject_model, theme_model, student):
        """
        Тест получения статистики группы по занятиям курса
        """
        now = timezone.now()
        # Курс
        course = Course.objects.create(
            name=u'Учительский курс', subject=subject_model, owner=teacher,
        )
        CourseStudent.objects.create(course=course, student=student)

        # Занятия
        lesson1 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Урок 1')
        lesson2 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Урок 2')
        lesson3 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Урок 3')
        lesson4 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Уже прошедшая КР')
        lesson5 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Еще не завершенная КР')

        # Связи курс-занятие
        course_lesson1 = CourseLessonLink.objects.create(
            lesson=lesson1, course=course, order=1,
        )
        course_lesson2 = CourseLessonLink.objects.create(
            lesson=lesson2, course=course, order=2,
        )
        course_lesson3 = CourseLessonLink.objects.create(
            lesson=lesson3, course=course, order=3,
        )
        course_lesson4 = CourseLessonLink.objects.create(
            lesson=lesson4,
            course=course,
            order=4,
            mode=CourseLessonLink.CONTROL_WORK_MODE,
            date_assignment=now - timedelta(hours=1),
            duration=1,
            finish_date=now - timedelta(minutes=59),
            evaluation_date=now - timedelta(minutes=59),
        )
        course_lesson5 = CourseLessonLink.objects.create(
            lesson=lesson5,
            course=course,
            order=5,
            mode=CourseLessonLink.CONTROL_WORK_MODE,
            date_assignment=now - timedelta(hours=1),
            duration=120,
            finish_date=now + timedelta(hours=1),
            evaluation_date=now + timedelta(hours=1),
        )

        # Статистики только для первой и третьей связи
        stat1 = CourseLessonStat.objects.create(
            clesson=course_lesson1,
            percent_complete=21,
            percent_fail=2,
            results_count=1,
            max_results_count=2,
        )
        stat2 = CourseLessonStat.objects.create(
            clesson=course_lesson3,
            percent_complete=42,
        )

        # Статистики по контрольным
        stat4 = CourseLessonStat.objects.create(
            clesson=course_lesson4,
            percent_complete=42,
        )
        stat5 = CourseLessonStat.objects.create(
            clesson=course_lesson5,
            percent_complete=42,
        )

        # Статистики по ученикам
        stat_student = StudentCourseStat.objects.create(
            student=student,
            course=course,
            total_efficiency=95,
            clesson_data={
                str(course_lesson1.id): {
                    "problems_skipped": 1,
                    "efficiency": 68,
                    "max_points": 200,
                    "points": 136,
                    "problems_correct": 9,
                    "problems_incorrect": 0
                },
                str(course_lesson3.id): {
                    "problems_skipped": 3,
                    "efficiency": 43,
                    "max_points": 140,
                    "points": 60,
                    "problems_correct": 3,
                    "problems_incorrect": 1
                },
            }
        )

        expected_json = {
            'lessons': {
                str(course_lesson1.id): {
                    'percent_complete': stat1.percent_complete,
                    'percent_fail': stat1.percent_fail,
                    'results_count': stat1.results_count,
                    'max_results_count': stat1.max_results_count,
                },
                str(course_lesson3.id): {
                    'percent_complete': stat2.percent_complete,
                    'percent_fail': stat2.percent_fail,
                    'results_count': stat2.results_count,
                    'max_results_count': stat2.max_results_count,
                },
                str(course_lesson4.id): {
                    'percent_complete': stat4.percent_complete,
                    'percent_fail': stat4.percent_fail,
                    'results_count': stat4.results_count,
                    'max_results_count': stat4.max_results_count,
                },
            },
            'students': {
                str(student.id): {
                    'total_efficiency': 95,
                    'lessons': [68, 43],
                }
            },
            'students_count':
                CourseStudent.objects.filter(course=course, student=student).count(),
        }

        stats_url = reverse('v2:course-stats', args=(course.id,))
        make_course_available_for_student(course, teacher)
        jclient.login(user=teacher)
        response = jclient.get(stats_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content)
        )
        assert response.json() == expected_json, u'Неправильный ответ'

    def test_journal(self, jclient, teacher, subject_model, theme_model):
        """
        Тест журнала учеников в курсе
        """
        # курс и ученики в курсе
        course = Course.objects.create(
            name=u'Учительский курс', subject=subject_model, owner=teacher,
        )
        student1 = User.objects.create(username='user1')
        student2 = User.objects.create(username='user2')
        student3 = User.objects.create(username='user3')
        student4 = User.objects.create(username='user4')
        CourseStudent.objects.create(course=course, student=student1)
        CourseStudent.objects.create(course=course, student=student2)
        CourseStudent.objects.create(course=course, student=student3)
        CourseStudent.objects.create(course=course, student=student4)

        # Занятия
        lesson1 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Урок 1')
        lesson2 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Урок 2')
        lesson3 = Lesson.objects.create(owner=teacher, theme=theme_model,
                                        name=u'Урок 3')

        # Связи курс-занятие
        course_lesson1 = CourseLessonLink.objects.create(
            lesson=lesson1, course=course, order=1,
        )
        course_lesson2 = CourseLessonLink.objects.create(
            lesson=lesson2, course=course, order=2,
        )
        course_lesson3 = CourseLessonLink.objects.create(
            lesson=lesson3, course=course, order=3,
        )

        # Статистика для первых трех учеников
        student1_stat = StudentCourseStat.objects.create(
            student=student1,
            course=course,
            points=1,
            problems_correct=2,
            problems_incorrect=3,
            problems_skipped=4,
            clesson_data={
                str(course_lesson1.id): {'progress': 50, 'points': 10, 'max_points': 20},
                str(course_lesson2.id): {'progress': 75, 'points': 20, 'max_points': 20},
                str(course_lesson3.id): {'progress': 25, 'points': 10, 'max_points': 20},
            },
            total_efficiency=50,
        )
        student2_stat = StudentCourseStat.objects.create(
            student=student2,
            course=course,
            points=2,
            problems_correct=3,
            problems_incorrect=4,
            problems_skipped=5,
            clesson_data={
                str(course_lesson1.id): {'progress': 50, 'points': 0, 'max_points': 20},
                str(course_lesson2.id): {'progress': 75, 'points': 40, 'max_points': 50},
            },
            total_efficiency=100,
        )
        student3_stat = StudentCourseStat.objects.create(
            student=student3,
            course=course,
            points=3,
            problems_correct=4,
            problems_incorrect=5,
            problems_skipped=6,
            clesson_data={
                str(course_lesson2.id): {'progress': 75, 'points': 30, 'max_points': 50},
                str(course_lesson3.id): {'progress': 25, 'points': 20, 'max_points': 50},
            },
            total_efficiency=75,
        )

        expected_json = {
            'students': {
                str(student1.id): {
                    'points': 1,
                    'problems_correct': 2,
                    'problems_incorrect': 3,
                    'problems_skipped': 4,
                    'total_efficiency': 50,
                    'lessons': {
                        str(course_lesson1.id): {'progress': 50, 'points': 10, 'max_points': 20},
                        str(course_lesson2.id): {'progress': 75, 'points': 20, 'max_points': 20},
                        str(course_lesson3.id): {'progress': 25, 'points': 10, 'max_points': 20},
                    },
                    'student_id': student1.id,
                    'staff_group': {'link': 'https://staff.yandex-team.ru/departments/?q=', 'name': ''},
                },
                str(student2.id): {
                    'points': 2,
                    'problems_correct': 3,
                    'problems_incorrect': 4,
                    'problems_skipped': 5,
                    'total_efficiency': 100,
                    'lessons': {
                        str(course_lesson1.id): {'progress': 50, 'points': 0, 'max_points': 20},
                        str(course_lesson2.id): {'progress': 75, 'points': 40, 'max_points': 50},
                        str(course_lesson3.id): {'progress': None, 'max_points': None, 'points': None},
                    },
                    'student_id': student2.id,
                    'staff_group': {'link': 'https://staff.yandex-team.ru/departments/?q=', 'name': ''},
                },
                str(student3.id): {
                    'points': 3,
                    'problems_correct': 4,
                    'problems_incorrect': 5,
                    'problems_skipped': 6,
                    'total_efficiency': 75,
                    'lessons': {
                        str(course_lesson1.id): {'progress': None, 'max_points': None, 'points': None},
                        str(course_lesson2.id): {'progress': 75, 'points': 30, 'max_points': 50},
                        str(course_lesson3.id): {'progress': 25, 'points': 20, 'max_points': 50},
                    },
                    'student_id': student3.id,
                    'staff_group': {'link': 'https://staff.yandex-team.ru/departments/?q=', 'name': ''},
                },
            },
        }

        journal_url = reverse('v2:course-journal', args=(course.id,))
        make_course_available_for_student(course, teacher)
        jclient.login(user=teacher)
        response = jclient.get(journal_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert response.json() == {'data': expected_json, 'csv_url': None}, (
            u'Неправильный ответ')

    @pytest.mark.xfail
    def test_diagnostics(self, jclient, student, student2):
        """
        Тест получения результатов диагностики
        """
        # курсы-диагностики
        text_template = TextTemplate.objects.create(
            name=u'Итог диагностики',
            template=u'Ты молодец!',  # TODO
        )
        now = timezone.now()
        subject = Subject.objects.create(slug='math')
        teacher = User.objects.create(email='1@1.w', is_teacher=True,
                                      username='1')
        level1 = GroupLevel.objects.create(
            name=u'1 класс',
            baselevel=1,
            slug="slug1",
        )
        level2 = GroupLevel.objects.create(
            name=u'2 класс',
            baselevel=1,
            slug="slug2",
        )
        course1 = Course.objects.create(
            name=u'Первый курс',
            subject=subject,
            owner=teacher,
            color='#abcabc',
        )
        course2 = Course.objects.create(
            name=u'Второй курс',
            subject=subject,
            owner=teacher,
            color='#abcabc',
        )
        lesson1 = Lesson.objects.create(
            owner=teacher,
            name=u'Занятие 1',
        )
        lesson2 = Lesson.objects.create(
            owner=teacher,
            name=u'Занятие 2',
        )
        link1 = CourseLessonLink.objects.create(
            course=course1,
            lesson=lesson1,
            order=1,
            mode=CourseLessonLink.DIAGNOSTICS_MODE,
            duration=20,
            finish_date=now,
            diagnostics_text=text_template,
        )
        link2 = CourseLessonLink.objects.create(
            course=course1,
            lesson=lesson2,
            order=2,
            mode=CourseLessonLink.DIAGNOSTICS_MODE,
            duration=20,
            finish_date=now + datetime.timedelta(days=1),
            diagnostics_text=text_template,
        )
        link3 = CourseLessonLink.objects.create(
            course=course2,
            lesson=lesson1,
            order=1,
            mode=CourseLessonLink.DIAGNOSTICS_MODE,
            duration=20,
            finish_date=now,
            diagnostics_text=text_template,
        )

        # ученик 1 имеет один результат на второе занятие и один на другой курс
        # ученик 2 имеет два результата в одном курсе
        CourseLessonResult.objects.create(
            summary=CourseLessonSummary.objects.create(
                student=student,
                clesson=link2,
            ),
            answers={},
            points=25,
            max_points=70,
            spent_time=15,
            completed=True,
        )
        CourseLessonResult.objects.create(
            summary=CourseLessonSummary.objects.create(
                student=student,
                clesson=link3,
            ),
            answers={},
            points=4,
            max_points=30,
            spent_time=15,
            completed=True,
        )
        # CourseLessonResult

        # запрашиваем результаты диагностики без наличия статистики
        assert StudentDiagnosticsStat.objects.count() == 0, (
            u'До запроса нет статистики ученика по диагностике')
        diagnostics_url = reverse('v2:course-diagnostics', args=(course1.id,))
        jclient.login(user=student)
        response = jclient.get(diagnostics_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert StudentDiagnosticsStat.objects.count() == 1, (
            u'Должна появиться статистика')
        stat = StudentDiagnosticsStat.objects.get(course=course1,
                                                  student=student)
        assert response.json() == {
            'course': {
                'id': course1.id,
                'code': None,
                'subject': 'math',
                'name': u'Первый курс',
            },
            'id': stat.id,
            'percent': 100,
            'result': [
                {
                    'clesson_id': link1.id,
                    'name': u'Занятие 1',
                    'percent': None,
                    'percent_average': None,
                    'text': u'Ты молодец!',
                },
                {
                    'clesson_id': link2.id,
                    'name': u'Занятие 2',
                    'percent': 36,
                    'percent_average': None,
                    'text': u'Ты молодец!',
                },
            ],
            'student': {
                'id': student.id,
                'first_name': None,
                'last_name': None,
                'middle_name': None,
                'username': u'Петя Иванов',
            }
        }

        # удаляем статистику ученика
        stat.delete()

        # создаем статистики по диагностике, рекомендации курсов
        DiagnosticsStat.objects.create(course=course1, points=0, count=1)
        DiagnosticsStat.objects.create(course=course1, points=25, count=1)
        DiagnosticsStat.objects.create(course=course1, points=50, count=2)
        DiagnosticsStat.objects.create(course=course1, points=100, count=1)
        DiagnosticsStat.objects.create(course=course2, points=0, count=10)

        LessonDiagnosticStats.objects.create(clesson=link1, average=20)
        LessonDiagnosticStats.objects.create(clesson=link2, average=70)
        LessonDiagnosticStats.objects.create(clesson=link3, average=80)

        # запрашиваем результаты диагностики с существующей статистикой
        response = jclient.get(diagnostics_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(
                response.content.decode('utf-8')
            )
        )
        assert StudentDiagnosticsStat.objects.count() == 1, (
            u'Должна появиться статистика')
        stat = StudentDiagnosticsStat.objects.get(course=course1,
                                                  student=student)
        assert response.json() == {
            'course': {
                'id': course1.id,
                'code': None,
                'subject': 'math',
                'name': u'Первый курс',
            },
            'id': stat.id,
            'percent': 40,
            'result': [
                {
                    'clesson_id': link1.id,
                    'name': u'Занятие 1',
                    'percent': None,
                    'percent_average': 20,
                    'text': u'Ты молодец!',
                },
                {
                    'clesson_id': link2.id,
                    'name': u'Занятие 2',
                    'percent': 36,
                    'percent_average': 70,
                    'text': u'Ты молодец!',
                },
            ],
            'student': {
                'id': student.id,
                'first_name': None,
                'last_name': None,
                'middle_name': None,
                'username': u'Петя Иванов',
            }
        }

    @pytest.mark.django_db
    def test_course_suggest_by_course_name(self, jclient):
        subject = Subject.objects.create(
            slug='math',
            name=u'Основы безопасности жизнедеятельности'
        )
        project = Project.objects.create(
            slug="math_project",
            title=u"Математика - всему голова"
        )
        teacher = User.objects.create(
            email='1@1.w',
            is_teacher=True,
            username='1'
        )
        UserProject.objects.create(
            user=teacher,
            project=project,
        )
        course = Course.objects.create(
            name=u'Новый спец 1 курс',
            subject=subject,
            owner=teacher,
            project=project,
            id=1,
        )

        out_of_project_course = Course.objects.create(
            name=u'Новый спец 1 курс , который подходит по саджесту не в проекте и не должен попасть в выдачу',
            subject=subject,
            owner=teacher,
            id=2,
        )

        jclient.login(user=teacher)
        response = jclient.get('/api/v2/courses/suggest/?q=1')

        assert(response.json()['results'][0]['id'] == course.id)

    @pytest.mark.django_db
    def test_course_suggest_by_subject_name(self, jclient):
        subject = Subject.objects.create(
            slug='math',
            name=u'Основы безопасности жизнедеятельности'
        )
        project = Project.objects.create(
            slug="math_project",
            title=u"Математика - всему голова"
        )
        teacher = User.objects.create(
            email='1@1.w',
            is_teacher=True,
            username='1'
        )
        UserProject.objects.create(
            user=teacher,
            project=project,
        )
        course = Course.objects.create(
            name=u'Новый спец 1 курс',
            subject=subject,
            owner=teacher,
            project=project,
            id=1,
        )

        out_of_project_course = Course.objects.create(
            name=u'Новый спец 1 курс по безопасности, который подходит по саджесту не в проекте и не должен попасть в выдачу',
            subject=subject,
            owner=teacher,
            id=2,
        )

        jclient.login(user=teacher)
        response = jclient.get(u'/api/v2/courses/suggest/?q=безопасности')

        assert(len(response.json()['results']) == 1)
        assert(response.json()['count'] == 1)
        assert(response.json()['results'][0]['id'] == course.id)

    @pytest.mark.django_db
    def test_course_suggest_paging(self, jclient):
        subject = Subject.objects.create(
            slug='math',
            name=u'Основы безопасности жизнедеятельности'
        )
        project = Project.objects.create(
            slug="math_project",
            title=u"Математика - всему голова"
        )
        teacher = User.objects.create(
            email='1@1.w',
            is_teacher=True,
            username='1'
        )
        UserProject.objects.create(
            user=teacher,
            project=project,
        )
        course1 = Course.objects.create(
            name=u'Новый спец 1 курс',
            subject=subject,
            owner=teacher,
            project=project,
            id=1,
        )
        course2 = Course.objects.create(
            name=u'Новый спец 1 курс по безопасности',
            subject=subject,
            owner=teacher,
            project=project,
            id=2,
        )

        jclient.login(user=teacher)
        response = jclient.get(u'/api/v2/courses/suggest/?q=1&page_size=1&page=1')

        assert(len(response.json()['results']) == 1)
        assert(response.json()['count'] == 2)
        assert(response.json()['results'][0]['id'] == course1.id)
