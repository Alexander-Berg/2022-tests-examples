from typing import Optional

import faker

from django.conf import settings
from django.test import override_settings

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.tags.tests.factories import TagFactory
from lms.users.tests.factories import ServiceAccountFactory, UserFactory

from ...assignments.tests.factories import AssignmentFactory
from .factories import (
    CourseFactory, CourseStudentFactory, ServiceAccountCourseFactory, StudentCourseProgressFactory,
    StudentModuleProgressFactory,
)

fake = faker.Faker()


class ExternalCourseListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'externalapi:course-list'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.courses = CourseFactory.create_batch(10, is_active=True)
        self.inactive_courses = CourseFactory.create_batch(2, is_active=False)
        self.tags = TagFactory.create_batch(10)
        for i, course in enumerate(self.courses[:5]):
            course.tags.set(self.tags[i: i + 2])

    def build_expected(self, courses):
        return [
            {
                'id': c.id,
                'slug': c.slug,
                'name': c.name,
                'shortname': c.shortname,
                'summary': c.summary,
                'city_id': c.city_id,
                'study_mode_id': c.study_mode_id,
                'image_url': c.image_url,
                'available_for_enroll': c.available_for_enroll,
                'is_active': c.is_active,
                'is_full': c.is_full,
                'structure': c.structure,
                'format': c.format,
                'author': {
                    'id': c.author.id,
                    'username': c.author.username,
                    'first_name': c.author.first_name,
                    'last_name': c.author.last_name,
                },
                'categories': [cc.id for cc in c.categories.all()],
                'begin_date': c.begin_date,
                'end_date': c.end_date,
                'enroll_begin': c.enroll_begin,
                'enroll_end': c.enroll_end,
                'payment_method': c.payment_method,
                'paid_percent': c.paid_percent,
                'payment_terms': c.payment_terms,
                'price': c.price,
                'num_hours': c.num_hours,
                'status': c.status,
                'type': c.course_type,
                'tags': list(c.tags.values_list('name', flat=True)),
                'enable_groups': c.enable_groups,
                'created': serializers.DateTimeField().to_representation(c.created),
                'modified': serializers.DateTimeField().to_representation(c.modified),
            } for c in sorted(courses, key=lambda x: x.name)
        ]

    def test_url(self):
        self.assertURLNameEqual('courses/', base_url=f'{settings.EXTERNAL_API_BASE_URL}v1/')

    @override_settings(
        DEBUG_TVM_SERVICE_ID=None
    )
    def test_non_auth(self):
        response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    @override_settings(
        DEBUG_TVM_SERVICE_ID=42
    )
    def test_no_permission(self):
        with self.assertNumQueries(1):
            response = self.client.get(self.get_url(), format='json')
            self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_course_visibility(self):
        course1, course2 = CourseFactory.create_batch(2)
        service_account = ServiceAccountFactory(tvm_id=settings.DEBUG_TVM_SERVICE_ID,
                                                is_active=True)
        ServiceAccountCourseFactory(course=course1, service_account=service_account)
        expected = self.build_expected([course1])
        self.list_request(self.get_url(), expected, num_queries=7)

    @override_settings(
        DEBUG_TVM_SERVICE_ID=42
    )
    def test_list(self):
        self.client.force_login(user=self.user)
        url = self.get_url()
        service_account = ServiceAccountFactory(tvm_id=42, is_active=True)
        for course in self.courses:
            ServiceAccountCourseFactory(service_account=service_account,
                                        course=course)
        expected = self.build_expected(self.courses)
        # force_login (2) + service account + COUNT(*) + course list +
        # categories + tags + groups + _opened_groups
        self.list_request(url, expected, num_queries=9)


class ExternalCourseProgressListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'externalapi:course-results-list'

    def setUp(self):
        self.service_account = ServiceAccountFactory(tvm_id=settings.DEBUG_TVM_SERVICE_ID)
        self.other_courses = CourseFactory.create_batch(5, is_active=True)
        for other_course in self.other_courses:
            StudentModuleProgressFactory(course=other_course)
        self.course = CourseFactory(is_active=True)
        self.modules = AssignmentFactory.create_batch(10, course=self.course, weight=1)
        self.students = []
        for i in range(25):
            student = CourseStudentFactory(course=self.course)
            for j in range(i % 10):
                StudentModuleProgressFactory(
                    course=self.course,
                    student=student,
                    module=self.modules[j],
                    score=fake.pyint(min_value=0, max_value=100)
                )
            StudentCourseProgressFactory(
                course=self.course,
                student=student,
                score=fake.pyint(min_value=0, max_value=100)
            )
            student.refresh_from_db()
            self.students.append(student)

    @staticmethod
    def build_expected_modules(module_progress_list):
        return [{
            'id': module_progress.module_id,
            'score': module_progress.score,
            'score_updated_at': serializers.DateTimeField().to_representation(module_progress.modified),
        } for module_progress in module_progress_list]

    @staticmethod
    def calculate_expected_score(course_progress_list) -> Optional[int]:
        return None if not course_progress_list else course_progress_list[0].score

    def build_expected(self, students):
        return [{
            'id': student.id,
            'started_at': serializers.DateTimeField().to_representation(student.created),
            'status': student.status,
            'is_finished': student.is_passed,
            'finished_at': serializers.DateTimeField().to_representation(student.passing_date),
            'yandex_login': student.user.username,
            'score': self.calculate_expected_score(list(student.course_progresses.all())),
            'modules': self.build_expected_modules(list(student.module_progresses.all())),
        } for student in students]

    def test_url(self):
        self.assertURLNameEqual('courses/{}/results/', args=(1,), base_url=f'{settings.EXTERNAL_API_BASE_URL}v1/')

    @override_settings(DEBUG_TVM_SERVICE_ID=None)
    def test_non_auth(self):
        url = self.get_url(self.course.pk)
        response = self.client.get(url, format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    @override_settings(DEBUG_TVM_SERVICE_ID=42)
    def test_no_permissions_unknown_tvm_id(self):
        url = self.get_url(self.course.pk)
        with self.assertNumQueries(1):
            response = self.client.get(url, format='json')
            self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_no_permissions_unknown_course(self):
        course = CourseFactory(is_active=True)
        url = self.get_url(course.pk)
        with self.assertNumQueries(1):
            response = self.client.get(url, format='json')
            self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_no_students(self):
        course = CourseFactory(is_active=True)
        ServiceAccountCourseFactory(course=course, service_account=self.service_account)
        url = self.get_url(course.pk)
        # service account + count(*)
        self.list_request(url, expected=[], num_queries=2, check_ids=False)

    def test_no_progress(self):
        course = CourseFactory(is_active=True)
        ServiceAccountCourseFactory(course=course, service_account=self.service_account)
        student = CourseStudentFactory(course=course)
        url = self.get_url(course.pk)
        expected = self.build_expected([student])
        expected_result = expected[0]
        expected_result['score'] = None
        expected_result['modules'] = []
        # service account + count(*) + students + course progress + module progress
        self.list_request(url, expected=expected, num_queries=5, check_ids=False)

    def test_list(self):
        ServiceAccountCourseFactory(course=self.course, service_account=self.service_account)
        url = self.get_url(self.course.pk)
        limit = fake.pyint(min_value=1, max_value=len(self.students))
        expected = self.build_expected(self.students[:limit])
        self.list_request(f'{url}?limit={limit}', expected=expected, num_queries=5, check_ids=False)
