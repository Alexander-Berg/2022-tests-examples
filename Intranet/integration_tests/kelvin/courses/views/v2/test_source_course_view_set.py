from builtins import object
import pytest
import re
import time

from django.conf import settings
from django.core.urlresolvers import reverse

from kelvin.courses.models import Course, CoursePermission
from integration_tests.fixtures.courses import make_course_available_for_student
from swissknife.assertions import AssertDictsAreEqual


@pytest.mark.django_db
class TestSourceCourseViewSet(object):
    """
    Тесты API курсов-источников
    """

    def test_source_course_detail(self, jclient, source_course, teacher):
        """
        Тест получения одного курса-источника
        """
        GUID_RE = settings.SWITCHMAN_URL + r'\/\w{8}-\w{4}-\w{4}-\w{4}-\w{12}'
        expected = {
            'id': source_course.id,
            'name': source_course.name,
            'cover': {
                'id': source_course.cover.id,
                'date_updated': int(
                    time.mktime(source_course.cover.date_updated.timetuple())),
                'file': source_course.cover.file.url,
                'name': source_course.cover.name,
                'type': source_course.cover.get_type(),
                'nda': False,
                'shortened_file_url': lambda x: bool(re.match(GUID_RE, x)),
            },
            'author': source_course.author,
        }

        detail_url = reverse('v2:source_course-detail',
                             args=(source_course.id,))

        course_permission = CoursePermission(
            user=teacher,
            course=source_course,
            permission=CoursePermission.OWNER | CoursePermission.CONTENT_MANAGER
        )
        course_permission.save()
        make_course_available_for_student(source_course, teacher)

        jclient.login(user=teacher)
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        AssertDictsAreEqual(answer, expected)

    def test_list_filter(self, jclient, subject_model, content_manager,
                         teacher):
        """
        Тест получения нескольких курсов-источников с фильтрацией по id
        """
        course1 = Course.objects.create(
            name=u'Книга 1',
            subject=subject_model,
            owner=content_manager,
            author=u'Автор книги',
            mode=Course.BOOK_COURSE,
        )
        course2 = Course.objects.create(
            name=u'Книга 2',
            subject=subject_model,
            owner=content_manager,
            author=u'Автор книги',
            mode=Course.BOOK_COURSE,
        )
        Course.objects.create(
            name=u'Ненужная книга',
            subject=subject_model,
            owner=content_manager,
            author=u'Автор книги',
            mode=Course.BOOK_COURSE,
        )

        expected = [
            {
                'id': course1.id,
                'name': u'Книга 1',
                'cover': None,
                'author': u'Автор книги',
            },
            {
                'id': course2.id,
                'name': u'Книга 2',
                'cover': None,
                'author': u'Автор книги',
            },
        ]

        list_url = '{path}?id={id1},{id2}'.format(
            path=reverse('v2:source_course-list'),
            id1=course1.id,
            id2=course2.id,
        )

        jclient.login(user=teacher)
        make_course_available_for_student(course1, teacher)
        make_course_available_for_student(course2, teacher)
        response = jclient.get(list_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer['results'] == expected
