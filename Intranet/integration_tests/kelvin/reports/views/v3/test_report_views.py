import pytest

from django.conf import settings
from django.contrib.auth import get_user_model
from django.urls import reverse

from kelvin.courses.models import Course
from kelvin.reports.models import Query, CourseReportRequest
from kelvin.subjects.models import Subject

from integration_tests.fixtures.projects import get_default_project


User = get_user_model()

TEST_COURSE_ID = 1


@pytest.mark.django_db
class TestCourseReportRequestView(object):
    VIEW_URL_NAME = 'v3-reports-course-reports-requests'

    def test_list(self, jclient, some_owner):
        list_url = reverse(self.VIEW_URL_NAME, args=(TEST_COURSE_ID,))
        response = jclient.get(list_url)
        assert response.status_code == 401, 'неправильный статус ответа'

        jclient.login(user=some_owner)
        response = jclient.get(list_url)
        assert response.status_code == 403

        # создаем курс
        subject = Subject.objects.create(slug='math')
        teacher = User.objects.create(email='teacher@example.com', is_teacher=True, username='test_teacher')
        course = Course.objects.create(
            name='Test Course',
            subject=subject,
            owner=teacher,
            id=TEST_COURSE_ID,
            color='#ffffff',
            project=get_default_project(),
        )

        course2 = Course.objects.create(
            name='Test Another Course',
            subject=subject,
            owner=teacher,
            id=2,
            color='#ffffff',
            project=get_default_project(),
        )

        # создаем YQL-запрос
        query1 = Query.objects.create(
            title='Query 1',
            content='USE hahn;\n\nSELECT 1\n',
            created_by=teacher,
        )

        # создаем запрос на отчет по курсу
        report_request: CourseReportRequest = CourseReportRequest.objects.create(
            course=course,
            user=some_owner,
            query=query1,
            format=CourseReportRequest.FORMAT_CSV,
        )

        # создаем еще один запрос на отчет (по другому курсу)
        CourseReportRequest.objects.create(
            course=course2,
            user=some_owner,
            query=query1,
            format=CourseReportRequest.FORMAT_CSV,
        )

        jclient.login(user=teacher)
        response = jclient.get(list_url)
        assert response.status_code == 200
        assert response.json() == {
            'count': 1, 'next': None, 'previous': None, 'results': [{
                'id': report_request.id,
                'query_id': query1.id,
                'parameters': {},
                'format': CourseReportRequest.FORMAT_CSV,
                'status': CourseReportRequest.STATUS_IDLE,
                'errors': {},
                'issues': {},
                'has_result_data': False,
                'notify_by_email': True,
                'created': report_request.created.strftime(settings.REST_FRAMEWORK['DATETIME_FORMAT']),
                'modified': report_request.modified.strftime(settings.REST_FRAMEWORK['DATETIME_FORMAT']),
            }]
        }, 'курс должен быть в списке'

    def test_create(self, jclient, some_owner):
        create_url = reverse(self.VIEW_URL_NAME, args=(TEST_COURSE_ID,))

        # создаем курс
        subject = Subject.objects.create(slug='math')
        teacher = User.objects.create(email='teacher@example.com', is_teacher=True, username='test_teacher')
        course = Course.objects.create(
            name='Test Course',
            subject=subject,
            owner=teacher,
            id=TEST_COURSE_ID,
            color='#ffffff',
            project=get_default_project(),
        )

        # создаем YQL-запрос
        query1 = Query.objects.create(
            title='Query 1',
            content='USE hahn;\n\nSELECT 1\n',
            created_by=teacher,
        )

        create_data = {
            'query_id': query1.id,
            'parameters': {},
            'format': CourseReportRequest.FORMAT_CSV,
            'notify_by_email': True,
        }
        jclient.login(user=teacher)
        response = jclient.post(create_url, create_data)

        new_report_query = CourseReportRequest.objects.last()

        assert response.status_code == 201
        assert response.json() == {
            'id': new_report_query.id,
            'query_id': query1.id,
            'parameters': {},
            'format': CourseReportRequest.FORMAT_CSV,
            'status': CourseReportRequest.STATUS_IDLE,
            'errors': {},
            'issues': {},
            'has_result_data': False,
            'notify_by_email': True,
            'created': new_report_query.created.strftime(settings.REST_FRAMEWORK['DATETIME_FORMAT']),
            'modified': new_report_query.modified.strftime(settings.REST_FRAMEWORK['DATETIME_FORMAT']),
        }