from guardian.shortcuts import assign_perm

from django.conf import settings

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.core.visibility.parser import RuleTreeParser
from lms.users.tests.factories import LabUserFactory, UserFactory

from ..models import Course
from .factories import CourseFactory, CourseVisibilityFactory


class LabCourseVisibilityListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-visibility-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)
        self.course_visibility = CourseVisibilityFactory(course=self.course)

    def test_url(self):
        self.assertURLNameEqual('courses/{}/visibility/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(self.course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_get(self):
        assign_perm('courses.view_course', self.user, self.course)
        self.client.force_login(user=self.user)
        with self.assertNumQueries(8):
            response = self.client.get(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        result = response.data

        expected = {
            'course_id': self.course.id,
            'rules': self.course_visibility.rules,
            'comments': self.course_visibility.comments,
            'is_active': self.course_visibility.is_active,
            'created': serializers.DateTimeField().to_representation(self.course_visibility.created),
            'modified': serializers.DateTimeField().to_representation(self.course_visibility.modified),
        }
        self.assertEqual(result, expected)

    def test_create(self):
        course = CourseFactory(is_active=True)
        assign_perm('courses.change_course', self.user)
        self.client.force_login(user=self.user)

        data = {
            'rules': {"eq": ["staff_is_head", False]},
            'comments': "sample text",
            'is_active': True,
        }
        with self.assertNumQueries(9):
            response = self.client.put(self.get_url(course.id), data=data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)

        course.refresh_from_db()

        result = response.data
        expected = {
            'course_id': course.id,
            'rules': data['rules'],
            'comments': data['comments'],
            'is_active': data['is_active'],
            'created': serializers.DateTimeField().to_representation(course.visibility.created),
            'modified': serializers.DateTimeField().to_representation(course.visibility.modified),
        }
        self.assertEqual(result, expected)

        rules = RuleTreeParser(data['rules'])
        self.assertEqual(course.visibility.formula, rules.formula)
        self.assertEqual(course.visibility.parameters, rules.parameters)

    def test_create_course_not_found(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.change_course', self.user)
        data = {
            'rules': {"eq": ["staff_is_head", False]},
            'comments': "sample text",
            'is_active': True,
        }
        with self.assertNumQueries(5):
            response = self.client.put(self.get_url(2222), data=data, format='json')

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND, response.data)

    def test_create_wrong_schema(self):
        course = CourseFactory(is_active=True)
        assign_perm('courses.change_course', self.user)
        self.client.force_login(user=self.user)

        data = {
            'rules': {"eq": "bad"},
            'comments': "sample text",
            'is_active': True,
        }
        with self.assertNumQueries(6):
            response = self.client.put(self.get_url(course.id), data=data, format='json')

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST, response.data)
        result = response.data

        self.assertIn('rules', result)

        error_codes = [e.code for e in result['rules']]
        self.assertIn('invalid_jsonschema', error_codes, msg=result['rules'])

    def test_create_rule_error(self):
        course = CourseFactory(is_active=True)
        assign_perm('courses.change_course', self.user)
        self.client.force_login(user=self.user)

        data = {
            'rules': {"eq": ["wrong_rule_name", True]},
            'comments': "sample text",
            'is_active': True,
        }
        with self.assertNumQueries(6):
            response = self.client.put(self.get_url(course.id), data=data, format='json')

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST, response.data)
        result = response.data

        self.assertIn('rules', result)

        error_codes = [e.code for e in result['rules']]
        self.assertIn('unknown_rule', error_codes, msg=result['rules'])

    def test_update(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        data = {
            'rules': {"eq": ["staff_is_head", False]},
            'comments': "sample text",
            'is_active': True,
        }
        with self.assertNumQueries(9):
            response = self.client.put(self.get_url(self.course.id), data=data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        result = response.data

        self.course.refresh_from_db()
        visibility = self.course.visibility

        expected = {
            'course_id': self.course.id,
            'rules': data['rules'],
            'comments': data['comments'],
            'is_active': data['is_active'],
            'created': serializers.DateTimeField().to_representation(visibility.created),
            'modified': serializers.DateTimeField().to_representation(visibility.modified),
        }
        self.assertEqual(result, expected)

        rules = RuleTreeParser(data['rules'])
        self.assertEqual(visibility.formula, rules.formula)
        self.assertEqual(visibility.parameters, rules.parameters)

    def test_destroy(self):
        visibility = CourseVisibilityFactory(course__is_active=True)
        course = visibility.course
        assign_perm('courses.change_course', self.user, course)

        self.client.force_login(user=self.user)
        with self.assertNumQueries(11):
            response = self.client.delete(self.get_url(course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT, response.data)

        course.refresh_from_db()
        self.assertIsNone(getattr(course, 'visiblity', None))


class CourseTypeFilterTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:course-list'

    def build_expected(self, courses):
        return [{"id": course.id} for course in sorted(courses, key=lambda course: course.name)]

    def test_list_with_type_track(self):
        user = LabUserFactory(is_staff=True)
        courses = CourseFactory.create_batch(22, course_type=Course.TypeChoices.COURSE, is_active=True)
        tracks = CourseFactory.create_batch(9, course_type=Course.TypeChoices.TRACK, is_active=True)
        assign_perm('courses.view_course', user, courses + tracks)

        expected = self.build_expected(tracks)

        self.client.force_login(user=user)

        self.list_request(self.get_url() + "?type=track", num_queries=8, expected=expected, only_ids=True)
