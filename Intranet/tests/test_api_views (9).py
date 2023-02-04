import faker

from django.conf import settings

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.courses.models import CourseStudent
from lms.courses.tests.factories import (
    CourseFactory, CourseFileFactory, CourseStudentFactory, StudentModuleProgressFactory,
)
from lms.users.tests.factories import UserFactory

from ..models import Scorm, ScormResourceStudentAttempt, ScormStudentAttempt
from .factories import ScormFactory, ScormFileFactory, ScormResourceFactory, ScormStudentAttemptFactory

fake = faker.Faker()


class ScormDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:scorm-detail'

    def setUp(self) -> None:
        self.scorm = ScormFactory()
        self.course_file = CourseFileFactory(course=self.scorm.course)
        ScormFileFactory(scorm=self.scorm, course_file=self.course_file)
        self.scorm.is_active = True
        self.scorm.save()

    def test_url(self):
        self.assertURLNameEqual('scorm_modules/{}/', args=(self.scorm.id,), base_url=settings.API_BASE_URL)

    def build_expected(self, scorm: Scorm):
        current_file = None if not scorm.current_file else {
            'id': scorm.current_file.id,
            'comment': scorm.current_file.comment,
            'created': serializers.DateTimeField().to_representation(scorm.current_file.created),
            'modified': serializers.DateTimeField().to_representation(scorm.current_file.modified),
        }
        return {
            'id': scorm.id,
            'name': scorm.name,
            'description': scorm.description,
            'is_active': scorm.is_active,
            'max_attempts': scorm.max_attempts,
            'created': serializers.DateTimeField().to_representation(scorm.created),
            'modified': serializers.DateTimeField().to_representation(scorm.modified),
            'current_file': current_file,
        }

    def test_no_auth(self):
        self.detail_request(url=self.get_url(self.scorm.id), status_code=status.HTTP_403_FORBIDDEN, num_queries=0)

    def test_is_not_student(self):
        self.client.force_login(user=UserFactory())
        self.detail_request(url=self.get_url(self.scorm.id), status_code=status.HTTP_403_FORBIDDEN, num_queries=6)

    def test_available_for_active_or_completed_students(self):
        for student_status in [CourseStudent.StatusChoices.ACTIVE, CourseStudent.StatusChoices.COMPLETED]:
            student = CourseStudentFactory(course=self.scorm.course, status=student_status)
            self.client.force_login(user=student.user)
            self.detail_request(
                url=self.get_url(self.scorm.id),
                num_queries=8,
            )

    def test_not_available_for_expelled_student(self):
        student = CourseStudentFactory(course=self.scorm.course, status=CourseStudent.StatusChoices.EXPELLED)
        self.client.force_login(user=student.user)
        self.detail_request(
            url=self.get_url(self.scorm.id),
            num_queries=6,
            status_code=status.HTTP_403_FORBIDDEN,
        )

    def test_detail(self):
        expected = self.build_expected(self.scorm)

        url = self.get_url(self.scorm.id)
        user = UserFactory()
        CourseStudentFactory(course=self.scorm.course, user=user)
        self.client.force_login(user=user)
        self.detail_request(url=url, expected=expected, num_queries=8)

    def test_detail_no_file(self):
        scorm = ScormFactory()

        url = self.get_url(scorm.id)
        user = UserFactory()
        CourseStudentFactory(course=scorm.course, user=user)
        self.client.force_login(user=user)
        self.detail_request(url=url, status_code=status.HTTP_404_NOT_FOUND, num_queries=3)


class ScormStudentAttemptTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-scorm-detail'

    def setUp(self) -> None:
        self.scorm = ScormFactory(max_attempts=2)
        self.course_file = CourseFileFactory(course=self.scorm.course)
        ScormFileFactory(scorm=self.scorm, course_file=self.course_file)
        self.resources = ScormResourceFactory.create_batch(2, scorm_file=self.scorm.current_file)
        self.user = UserFactory()
        self.course_student = CourseStudentFactory(user=self.user, course=self.scorm.course)

    def test_url(self):
        self.assertURLNameEqual('my/scorm_modules/{}/', args=(self.scorm.id,), base_url=settings.API_BASE_URL)

    def build_expected(self, current_attempt, scorm_file):
        return {
            'current_attempt': current_attempt,
            'scorm_file': {
                'id': scorm_file.id,
                'public_url': scorm_file.public_url,
                'comment': scorm_file.comment,
                'created': serializers.DateTimeField().to_representation(scorm_file.created),
                'modified': serializers.DateTimeField().to_representation(scorm_file.modified),
                'resources': [{
                    'id': resource.id,
                    'resource_id': resource.resource_id,
                    'href': resource.href,
                    'created': serializers.DateTimeField().to_representation(resource.created),
                    'modified': serializers.DateTimeField().to_representation(resource.modified),
                } for resource in scorm_file.resources.all()],
            }
        }

    def test_detail(self):
        attempt = ScormStudentAttemptFactory(
            scorm=self.scorm,
            student=self.course_student,
            scorm_file=self.scorm.current_file
        )

        url = self.get_url(self.scorm.id)
        self.client.force_login(user=self.user)
        expected = self.build_expected(attempt.current_attempt, attempt.scorm_file)
        self.detail_request(url=url, expected=expected, num_queries=6)

    def test_detail_multiple_students(self):
        expelled_student = CourseStudentFactory(
            user=self.user, course=self.scorm.course, status=CourseStudent.StatusChoices.EXPELLED
        )
        ScormStudentAttemptFactory(
            scorm=self.scorm,
            student=expelled_student,
            scorm_file=self.scorm.current_file
        )
        attempt = ScormStudentAttemptFactory(
            scorm=self.scorm,
            student=self.course_student,
            scorm_file=self.scorm.current_file
        )

        url = self.get_url(self.scorm.id)
        self.client.force_login(user=self.user)
        expected = self.build_expected(attempt.current_attempt, attempt.scorm_file)
        self.detail_request(url=url, expected=expected, num_queries=6)

    def test_new_attempt(self):
        url = self.get_url(self.scorm.id)
        self.client.force_login(user=self.user)

        for current_attempt, num_queries in zip([1, 2], [18, 19]):
            expected = self.build_expected(current_attempt, self.scorm.current_file)
            self.create_request(url=url, expected=expected, status_code=status.HTTP_200_OK, num_queries=num_queries)
            self.assertEqual(
                ScormStudentAttempt.objects.filter(scorm=self.scorm, student=self.course_student).count(), 1,
            )
            attempt = ScormStudentAttempt.objects.filter(scorm=self.scorm, student=self.course_student).first()
            self.assertEqual(attempt.current_attempt, current_attempt)

            for resource in self.resources:
                resource_attempt = ScormResourceStudentAttempt.objects.filter(
                    student=attempt.student,
                    scorm_resource=resource,
                    current_attempt=current_attempt,
                ).first()
                self.assertIsNotNone(resource_attempt)

        expected = {
            '__all__': ['превышено максимальное количество попыток'],
        }
        self.create_request(url=url, expected=expected, status_code=status.HTTP_400_BAD_REQUEST, num_queries=14)


class ScormStudentAttemptCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-scorm-attempt-create'

    def setUp(self) -> None:
        self.scorm = ScormFactory(max_attempts=2)
        self.course_file = CourseFileFactory(course=self.scorm.course)
        self.scorm_file = ScormFileFactory(scorm=self.scorm, course_file=self.course_file)
        self.scorm.is_active = True
        self.scorm.save()
        self.resources = ScormResourceFactory.create_batch(2, scorm_file=self.scorm_file)
        self.scorm.refresh_from_db()
        self.user = UserFactory()
        self.student = CourseStudentFactory(user=self.user, course=self.scorm.course)

    def test_url(self):
        self.assertURLNameEqual('my/scorm_modules/', base_url=settings.API_BASE_URL)

    def build_expected(self, current_attempt, scorm_file):
        return {
            'current_attempt': current_attempt,
            'scorm_file': {
                'id': scorm_file.id,
                'public_url': scorm_file.public_url,
                'comment': scorm_file.comment,
                'created': serializers.DateTimeField().to_representation(scorm_file.created),
                'modified': serializers.DateTimeField().to_representation(scorm_file.modified),
                'resources': [{
                    'id': resource.id,
                    'resource_id': resource.resource_id,
                    'href': resource.href,
                    'created': serializers.DateTimeField().to_representation(resource.created),
                    'modified': serializers.DateTimeField().to_representation(resource.modified),
                } for resource in scorm_file.resources.all()],
            }
        }

    def build_request_payload(self, scorm):
        return {
            'scorm_id': scorm.id,
        }

    def assert_scorm_resource_attempts(self, attempt: ScormStudentAttempt, total_count, expected_attempts):
        attempt.refresh_from_db()
        actual_attempt_ids = set(attempt.student.student_scorm_resource_attempts.filter(
            current_attempt=attempt.current_attempt
        ).values_list('id', flat=True))
        expected_attempt_ids = {attempt.id for attempt in expected_attempts}
        self.assertSetEqual(
            actual_attempt_ids, expected_attempt_ids, f'\n{actual_attempt_ids=}\n====\n{expected_attempt_ids=}'
        )
        actual_count = attempt.student.student_scorm_resource_attempts.count()
        self.assertEqual(actual_count, total_count, f'total resource count should be {total_count}')

    def get_scorm_resource_attempts(self, student: CourseStudent, scorm_resources, current_attempt):
        return ScormResourceStudentAttempt.objects.filter(
            student=student, scorm_resource__in=scorm_resources, current_attempt=current_attempt
        )

    def test_first_attempt(self):
        self.client.force_login(user=self.user)
        url = self.get_url()
        request_payload = self.build_request_payload(self.scorm)

        expected = self.build_expected(1, self.scorm.current_file)
        self.create_request(url=url, data=request_payload, expected=expected, num_queries=17)
        self.assertEqual(
            ScormStudentAttempt.objects.filter(scorm=self.scorm, student__user=self.user).count(), 1,
        )
        attempt = ScormStudentAttempt.objects.filter(scorm=self.scorm, student=self.student).first()
        self.assertEqual(attempt.current_attempt, 1)
        expected_attempts = self.get_scorm_resource_attempts(
            student=self.student, scorm_resources=self.resources, current_attempt=1
        )
        self.assert_scorm_resource_attempts(attempt=attempt, total_count=2, expected_attempts=expected_attempts)

    def test_not_course_student(self):
        user = UserFactory()
        self.client.force_login(user=user)
        url = self.get_url()
        request_payload = self.build_request_payload(self.scorm)

        with self.assertNumQueries(4):
            response = self.client.post(path=url, data=request_payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(
            ScormStudentAttempt.objects.filter(scorm=self.scorm, student__user=user).count(), 0,
        )

    def test_completed_course_student(self):
        user = UserFactory()
        CourseStudentFactory(
            user=user, course=self.scorm.course, status=CourseStudent.StatusChoices.COMPLETED
        )
        self.client.force_login(user=user)
        url = self.get_url()
        request_payload = self.build_request_payload(self.scorm)

        with self.assertNumQueries(5):
            response = self.client.post(path=url, data=request_payload, format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(
            ScormStudentAttempt.objects.filter(scorm=self.scorm, student__user=user).count(), 0,
        )

    def test_second_attempt_new_scorm_file(self):
        attempt = ScormStudentAttemptFactory(scorm=self.scorm, scorm_file=self.scorm.current_file, student=self.student)

        another_course_file = CourseFileFactory(course=self.scorm.course)
        another_scorm_file = ScormFileFactory(scorm=self.scorm, course_file=another_course_file)
        another_resources = ScormResourceFactory.create_batch(3, scorm_file=another_scorm_file)

        self.client.force_login(user=self.user)
        url = self.get_url()
        request_payload = self.build_request_payload(self.scorm)

        expected = self.build_expected(2, another_scorm_file)
        self.create_request(url=url, data=request_payload, expected=expected, num_queries=18)
        self.assertEqual(
            ScormStudentAttempt.objects.filter(scorm=self.scorm, student__user=self.user).count(), 1,
        )
        attempt.refresh_from_db()
        self.assertEqual(attempt.current_attempt, 2)
        expected_attempts = self.get_scorm_resource_attempts(
            student=self.student, scorm_resources=another_resources, current_attempt=2
        )
        self.assert_scorm_resource_attempts(attempt=attempt, total_count=5, expected_attempts=expected_attempts)

    def test_new_attempt_module_progress_saved(self):
        progress = StudentModuleProgressFactory(
            course=self.scorm.course, module=self.scorm, student=self.student, score=72
        )
        ScormStudentAttemptFactory(scorm=self.scorm, scorm_file=self.scorm.current_file, student=self.student)

        self.client.force_login(user=self.user)
        url = self.get_url()
        request_payload = self.build_request_payload(self.scorm)

        expected = self.build_expected(2, self.scorm.current_file)
        self.create_request(url=url, data=request_payload, expected=expected, num_queries=15)

        progress.refresh_from_db()
        self.assertEqual(progress.score, 72)

    def test_second_attempt_same_scorm_file(self):
        attempt = ScormStudentAttemptFactory(scorm=self.scorm, scorm_file=self.scorm.current_file, student=self.student)

        self.client.force_login(user=self.user)
        url = self.get_url()
        request_payload = self.build_request_payload(self.scorm)

        expected = self.build_expected(2, self.scorm.current_file)
        self.create_request(url=url, data=request_payload, expected=expected, num_queries=15)
        self.assertEqual(
            ScormStudentAttempt.objects.filter(scorm=self.scorm, student__user=self.user).count(), 1,
        )
        attempt.refresh_from_db()
        self.assertEqual(attempt.current_attempt, 2)
        expected_attempts = self.get_scorm_resource_attempts(
            student=self.student, scorm_resources=self.resources, current_attempt=2
        )
        self.assert_scorm_resource_attempts(attempt=attempt, total_count=4, expected_attempts=expected_attempts)

    def test_max_attempt_limit(self):
        attempt = ScormStudentAttemptFactory(scorm=self.scorm, scorm_file=self.scorm.current_file, student=self.student)
        self.scorm.max_attempts = 1
        self.scorm.save()

        self.client.force_login(user=self.user)
        url = self.get_url()
        request_payload = self.build_request_payload(self.scorm)

        expected_error = {
            '__all__': ['превышено максимальное количество попыток'],
        }
        self.create_request(
            url=url, data=request_payload, expected=expected_error, num_queries=12,
            status_code=status.HTTP_400_BAD_REQUEST,
        )

        self.scorm.max_attempts = 0
        self.scorm.save()

        expected = self.build_expected(2, self.scorm.current_file)
        self.create_request(url=url, data=request_payload, expected=expected, num_queries=15)
        self.assertEqual(
            ScormStudentAttempt.objects.filter(scorm=self.scorm, student__user=self.user).count(), 1,
        )
        attempt.refresh_from_db()
        self.assertEqual(attempt.current_attempt, 2)
        expected_attempts = self.get_scorm_resource_attempts(
            student=self.student, scorm_resources=self.resources, current_attempt=2
        )
        self.assert_scorm_resource_attempts(attempt=attempt, total_count=4, expected_attempts=expected_attempts)


class ScormResourceStudentAttemptTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-scorm-resource-detail'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory()
        self.scorm = ScormFactory(course=self.course)
        self.course_file = CourseFileFactory(course=self.course)
        ScormFileFactory(scorm=self.scorm, course_file=self.course_file)
        self.resource = ScormResourceFactory(scorm_file=self.scorm.current_file)
        self.student = CourseStudentFactory(course=self.course, user=self.user)
        ScormStudentAttemptFactory(scorm=self.scorm, student=self.student, scorm_file=self.scorm.current_file)
        self.resource_attempt = ScormResourceStudentAttempt.objects.get(
            student=self.student, scorm_resource=self.resource,
        )

    def test_url(self):
        self.assertURLNameEqual('my/scorm_resources/{}/', args=(self.resource.id,), base_url=settings.API_BASE_URL)

    def test_detail_not_student(self):
        user = UserFactory()
        url = self.get_url(self.resource.id)
        self.client.force_login(user=user)
        self.detail_request(url=url, num_queries=3, status_code=status.HTTP_404_NOT_FOUND)

    def test_detail_different_student(self):
        user = UserFactory()
        CourseStudentFactory(course=self.course, user=user)
        url = self.get_url(self.resource.id)
        self.client.force_login(user=user)
        self.detail_request(url=url, num_queries=3, status_code=status.HTTP_404_NOT_FOUND)

    def test_detail_completed_student(self):
        url = self.get_url(self.resource.id)
        self.client.force_login(user=self.user)

        self.student.status = CourseStudent.StatusChoices.COMPLETED
        self.student.save()
        self.detail_request(url=url, num_queries=3, status_code=status.HTTP_404_NOT_FOUND)

    def test_detail_expelled_student(self):
        url = self.get_url(self.resource.id)
        self.client.force_login(user=self.user)

        self.student.status = CourseStudent.StatusChoices.EXPELLED
        self.student.save()
        self.detail_request(url=url, num_queries=3, status_code=status.HTTP_404_NOT_FOUND)

    def test_detail(self):
        url = self.get_url(self.resource.id)
        self.client.force_login(user=self.user)
        expected = {
            'current_attempt': self.resource_attempt.current_attempt,
            'data': self.resource_attempt.data,
        }
        self.detail_request(url=url, expected=expected, num_queries=5)

    def test_update(self):
        url = self.get_url(self.resource.id)
        self.client.force_login(user=self.user)
        data = {
            'data': {"a scorm key": "a scorm value"},
        }
        expected = {
            'current_attempt': self.resource_attempt.current_attempt,
            'data': data['data'],
        }

        self.update_request(url=url, data=data, expected=expected, num_queries=13)
