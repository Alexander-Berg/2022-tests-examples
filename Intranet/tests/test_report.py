import datetime
import json
from builtins import object

from mock import MagicMock

from kelvin.accounts.models import User
from kelvin.common.staff_reader import staff_reader
from kelvin.courses.models import Course, CourseLessonLink, CourseStudent
from kelvin.courses.report import CuratorReport, HRUsersStaffReport
from kelvin.lessons.models import Lesson
from kelvin.result_stats.models import StudentCourseStat


def get_mocked_course(mocker):
    course = Course(name=u'Course One')
    course.id = 123
    mocked_course_student_manager = mocker.patch.object(CourseStudent, 'objects')
    mocked_course_student_manager.filter.return_value = [
        CourseStudent(
            student_id=11,
            date_created=datetime.datetime(2018, 11, 7),
            date_completed=datetime.datetime(2019, 8, 1),
            completed=True,
        ),
    ]
    mocked_clesson_manager = mocker.patch.object(CourseLessonLink, 'objects')
    mocked_clesson_manager.filter().values_list.return_value = [101, 202, 303, 404]
    mocked_clesson_manager.filter().select_related.return_value = [
        CourseLessonLink(id=101, lesson=Lesson(name=u'Lesson 101'), course=course),
        CourseLessonLink(id=202, lesson=Lesson(name=u'Lesson 202'), course=course),
        CourseLessonLink(id=303, lesson=Lesson(name=u'Lesson 303'), course=course),
        CourseLessonLink(id=404, lesson=Lesson(name=u'Lesson 404'), course=course),
    ]

    mocked_stat_manager = mocker.patch.object(
        StudentCourseStat,
        'objects',
    )
    mocked_stat_manager.filter().select_related.return_value = [
        StudentCourseStat(
            student_id=11,
            points=1,
            problems_correct=2,
            problems_incorrect=3,
            problems_skipped=4,
            clesson_data={
                '101': {'efficiency': 50, 'points': 10},
                '303': {'efficiency': 75, 'points': 20},
                '404': {'efficiency': 25, 'points': 10},
            },
            total_efficiency=50,
            course=course,
        ),
        StudentCourseStat(
            student_id=22,
            points=2,
            problems_correct=3,
            problems_incorrect=4,
            problems_skipped=5,
            clesson_data={
                '101': {'efficiency': 25, 'points': 20},
                '202': {'efficiency': 50, 'points': 10},
                '303': {'efficiency': 75, 'points': 10},
                '404': {'efficiency': 100, 'points': 10},
            },
            total_efficiency=100,
            course=course,
        ),
    ]
    return course


def get_suggestuser_many(username='', usernames=None):
    results = [
        {
            'login': 'user1',
            'name': {
                'last': {'ru': u'Первый'},
                'first': {'ru': u'Один'},
            },
            'official': {
                'position': {'ru': u'Первопроходец'},
            },
            'department_group': {'name': u'Первообработка'},
        },
        {
            'login': 'user2',
            'name': {
                'last': {'ru': u'Второй'},
                'first': {'ru': u'Два'},
            },
            'official': {
                'position': {'ru': u'Второгодка'},
            },
            'department_group': {'name': u'Вторичная профилактика'},
        },
        {
            'login': 'user3',
            'name': {
                'last': {'ru': u'Третий'},
                'first': {'ru': u'Три'},
            },
            'official': {
                'position': {'ru': u'Тритон'},
            },
            'department_group': {'name': u'Третья фаза'},
        },
        {
            'login': 'user4',
            'name': {
                'last': {'ru': u'Четвертый'},
                'first': {'ru': u'Четыре'},
            },
            'official': {
                'position': {'ru': u'Чёрт'},
            },
            'department_group': {'name': u'Управление Ч'},
        },
    ]
    return results


class TestHRUserStaffReport(object):
    """
    Тесты отчета по курсу для HR-партнера
    """
    def test_init(self):
        """
        Тест инициализации
        """
        course = MagicMock()
        user = MagicMock()
        report = HRUsersStaffReport(courses=[course], user=user)

        assert report.courses == [course], u'Неправильный курс'
        assert report._data is None, u'Начальные данные должны быть `None`'

    def test_data(self, mocker):
        """
        Тест получения данных отчета
        """
        course = get_mocked_course(mocker)

        user = MagicMock()
        mocked_user_objects = mocker.patch.object(
            User,
            'objects',
        )
        mocked_user_objects.filter.return_value = [
            User(
                id=11,
                username='user1',
            ),
            User(
                id=22,
                username='user2',
            ),
            User(
                id=33,
                username='user3',
            ),
        ]

        def get_hrusers(username, limit, offset, with_nested=False):
            results = [
                {
                    'login': 'user1',
                    'name': {
                        'last': {'ru': u'Первый'},
                        'first': {'ru': u'Один'},
                    },
                    'official': {
                        'position': {'ru': u'Первопроходец'},
                    },
                    'department_group': {'name': u'Первообработка'},
                },
                {
                    'login': 'user2',
                    'name': {
                        'last': {'ru': u'Второй'},
                        'first': {'ru': u'Два'},
                    },
                    'official': {
                        'position': {'ru': u'Второгодка'},
                    },
                    'department_group': {'name': u'Вторичная профилактика'},
                },
                {
                    'login': 'user3',
                    'name': {
                        'last': {'ru': u'Третий'},
                        'first': {'ru': u'Три'},
                    },
                    'official': {
                        'position': {'ru': u'Тритон'},
                    },
                    'department_group': {'name': u'Третья фаза'},
                },
                {
                    'login': 'user4',
                    'name': {
                        'last': {'ru': u'Четвертый'},
                        'first': {'ru': u'Четыре'},
                    },
                    'official': {
                        'position': {'ru': u'Чёрт'},
                    },
                    'department_group': {'name': u'Управление Ч'},
                },
            ]
            if offset:
                results = results[offset:]
            if limit:
                results = results[:limit]
            return results

        mocked_staff_reader = mocker.patch.object(staff_reader, 'get_hrusers')
        mocked_staff_reader.side_effect = get_hrusers

        expected_data = [
            {
                'username': 'user1',
                'first_name': u'Один',
                'last_name': u'Первый',
                'clessons_results_avg': 37.5,
                'status': 'join_course',
                'department_group_name': u'Первообработка',
                'clessons_results': [50, 0, 75, 25],
                'started_datetime': '2018-11-07T00:00:00Z',
                'started_datetime_original': datetime.datetime(2018, 11, 7),
                'date_completed': '2019-08-01T00:00:00Z',
                'date_completed_original': datetime.datetime(2019, 8, 1),
                'completed': True,
                'position': u'Первопроходец',
                'id': 11
            },
            {
                'username': 'user2',
                'first_name': u'Два',
                'last_name': u'Второй',
                'clessons_results_avg': 62.5,
                'status': 'join_course',
                'department_group_name': u'Вторичная профилактика',
                'clessons_results': [25, 50, 75, 100],
                'started_datetime': None,
                'date_completed': None,
                'completed': False,
                'position': u'Второгодка',
                'id': 22,
            },
            {
                'username': 'user3',
                'status': 'do_not_join_course',
                'first_name': u'Три',
                'last_name': u'Третий',
                'department_group_name': u'Третья фаза',
                'started_datetime': None,
                'date_completed': None,
                'completed': False,
                'position': u'Тритон',
                'id': 33,
            },
            {
                'username': 'user4',
                'status': 'do_not_join_moebius',
                'first_name': u'Четыре',
                'last_name': u'Четвертый',
                'department_group_name': u'Управление Ч',
                'started_datetime': None,
                'date_completed': None,
                'completed': False,
                'position': u'Чёрт',
                'id': None,
            }
        ]

        data = HRUsersStaffReport(courses=[course], user=user).data
        assert data == expected_data, u'Неправильные данные журнала'

        data = HRUsersStaffReport(courses=[course], user=user, offset=1).data
        assert data == expected_data[1:], u'Неправильная пагинация'

        data = HRUsersStaffReport(courses=[course], user=user, limit=3).data
        assert data == expected_data[:3], u'Неправильная пагинация'

        data = HRUsersStaffReport(
            courses=[course], user=user, offset=1, limit=2,
        ).data
        assert data == expected_data[1:3], u'Неправильная пагинация'


class TestCuratorReport(object):
    """
    Тесты отчета по курсу для куратора
    """
    def test_init(self):
        """
        Тест инициализации
        """
        course = MagicMock()
        usernames = []
        report = CuratorReport(courses=[course], usernames=usernames)

        assert report.courses == [course], u'Неправильный курс'
        assert report._data is None, u'Начальные данные должны быть `None`'

    def test_data(self, mocker):
        """
        Тест получения данных отчета
        """
        course = get_mocked_course(mocker)

        usernames = ['user1', 'user2', 'user3']
        mocked_user_objects = mocker.patch.object(
            User,
            'objects',
        )
        mocked_user_objects.filter.return_value = [
            User(
                id=11,
                username='user1',
            ),
            User(
                id=22,
                username='user2',
            ),
            User(
                id=33,
                username='user3',
            ),
        ]

        def get_suggestuser_many(username='', usernames=None):
            results = [
                {
                    'login': 'user1',
                    'name': {
                        'last': {'ru': u'Первый'},
                        'first': {'ru': u'Один'},
                    },
                    'official': {
                        'position': {'ru': u'Первопроходец'},
                    },
                    'department_group': {'name': u'Первообработка'},
                },
                {
                    'login': 'user2',
                    'name': {
                        'last': {'ru': u'Второй'},
                        'first': {'ru': u'Два'},
                    },
                    'official': {
                        'position': {'ru': u'Второгодка'},
                    },
                    'department_group': {'name': u'Вторичная профилактика'},
                },
                {
                    'login': 'user3',
                    'name': {
                        'last': {'ru': u'Третий'},
                        'first': {'ru': u'Три'},
                    },
                    'official': {
                        'position': {'ru': u'Тритон'},
                    },
                    'department_group': {'name': u'Третья фаза'},
                },
                {
                    'login': 'user4',
                    'name': {
                        'last': {'ru': u'Четвертый'},
                        'first': {'ru': u'Четыре'},
                    },
                    'official': {
                        'position': {'ru': u'Чёрт'},
                    },
                    'department_group': {'name': u'Управление Ч'},
                },
            ]

            return [result for result in results if result['login'] in usernames]

        mocked_staff_reader = mocker.patch.object(staff_reader, 'get_suggestuser_many')
        mocked_staff_reader.side_effect = get_suggestuser_many

        expected_data = [
            {
                'username': 'user1',
                'first_name': u'Один',
                'last_name': u'Первый',
                'score': 50,
                'course_name': u'Course One',
                'lesson_name': u'Lesson 101',
                'id': 11
            },
            {
                'username': 'user1',
                'first_name': u'Один',
                'last_name': u'Первый',
                'score': 0,
                'course_name': u'Course One',
                'lesson_name': u'Lesson 202',
                'id': 11
            },
            {
                'username': 'user1',
                'first_name': u'Один',
                'last_name': u'Первый',
                'score': 75,
                'course_name': u'Course One',
                'lesson_name': u'Lesson 303',
                'id': 11
            },
            {
                'username': 'user1',
                'first_name': u'Один',
                'last_name': u'Первый',
                'score': 25,
                'course_name': u'Course One',
                'lesson_name': u'Lesson 404',
                'id': 11
            },
            {
                'username': 'user2',
                'first_name': u'Два',
                'last_name': u'Второй',
                'score': 25,
                'course_name': u'Course One',
                'lesson_name': u'Lesson 101',
                'id': 22
            },
            {
                'username': 'user2',
                'first_name': u'Два',
                'last_name': u'Второй',
                'score': 50,
                'course_name': u'Course One',
                'lesson_name': u'Lesson 202',
                'id': 22
            },
            {
                'username': 'user2',
                'first_name': u'Два',
                'last_name': u'Второй',
                'score': 75,
                'course_name': u'Course One',
                'lesson_name': u'Lesson 303',
                'id': 22
            },
            {
                'username': 'user2',
                'first_name': u'Два',
                'last_name': u'Второй',
                'score': 100,
                'course_name': u'Course One',
                'lesson_name': u'Lesson 404',
                'id': 22
            },
        ]

        data = CuratorReport(courses=[course], usernames=usernames).data
        assert data == expected_data, u'Неправильные данные журнала'

        data = CuratorReport(courses=[course], usernames=usernames, offset=1).data
        assert data == expected_data[1:], u'Неправильная пагинация'

        data = CuratorReport(courses=[course], usernames=usernames, limit=4).data
        assert data == expected_data[:4], u'Неправильная пагинация'

        data = CuratorReport(
            courses=[course], usernames=usernames, offset=3, limit=2,
        ).data
        assert data == expected_data[3:5], u'Неправильная пагинация'

    def test_table_smoke(self, mocker):
        """
        Тест того, что таблица отдается без исключений
        """
        course = get_mocked_course(mocker)

        usernames = ['user1', 'user2', 'user3']
        mocked_user_objects = mocker.patch.object(
            User,
            'objects',
        )
        mocked_user_objects.filter.return_value = [
            User(
                id=11,
                username='user1',
            ),
            User(
                id=22,
                username='user2',
            ),
            User(
                id=33,
                username='user3',
            ),
        ]

        mocked_staff_reader = mocker.patch.object(staff_reader, 'get_suggestuser_many')
        mocked_staff_reader.side_effect = get_suggestuser_many

        expected_table_data = [
            [u'ФИ', u'Логин', u'Курс', u'Модуль', u'Успеваемость, %'],
            [u'Один Первый', 'user1', 'Course One', 'Lesson 101', 50],
            [u'Один Первый', 'user1', 'Course One', 'Lesson 202', 0],
            [u'Один Первый', 'user1', 'Course One', 'Lesson 303', 75],
            [u'Один Первый', 'user1', 'Course One', 'Lesson 404', 25],
            [u'Два Второй', 'user2', 'Course One', 'Lesson 101', 25],
            [u'Два Второй', 'user2', 'Course One', 'Lesson 202', 50],
            [u'Два Второй', 'user2', 'Course One', 'Lesson 303', 75],
            [u'Два Второй', 'user2', 'Course One', 'Lesson 404', 100],
        ]

        table_data = CuratorReport(courses=[course], usernames=usernames).table()

        assert table_data == expected_table_data, u'Неправильные данные таблицы'
