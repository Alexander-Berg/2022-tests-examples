from guardian.shortcuts import assign_perm

from django.conf import settings

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import UrlNameMixin
from lms.courses.tests.factories import CourseFactory
from lms.users.tests.factories import LabUserFactory, UserFactory

from ..models import ReportFile


class LabStudentSlotsReportCreateTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:student-slots-report-create'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()

    def test_url(self):
        self.assertURLNameEqual(
            'courses/{}/reports/student_slots/',
            args=(self.course.id,),
            base_url=settings.LABAPI_BASE_URL,
        )

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.post(self.get_url(self.course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_create(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.change_course', self.user, self.course)

        with self.assertNumQueries(11):
            response = self.client.post(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_202_ACCEPTED)

        data = response.data

        report_file = ReportFile.objects.get(id=data['id'])

        expected = {
            'id': str(report_file.id),
            'status': ReportFile.StatusChoices.CREATED.value,
            'error_message': '',
            'file': None,
            'course': self.course.id,
        }
        self.assertEqual(data, expected)
