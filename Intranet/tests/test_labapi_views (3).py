from datetime import datetime, timedelta
from typing import Iterable, Union

import faker
from guardian.shortcuts import assign_perm

from django.conf import settings
from django.contrib.auth import get_user_model
from django.db.models import Max
from django.http import HttpResponse
from django.urls import reverse

from rest_framework import serializers, status
from rest_framework.response import Response
from rest_framework.test import APIRequestFactory, APITestCase, APITransactionTestCase, force_authenticate

from lms.assignments.tests.factories import AssignmentFactory
from lms.calendars.tests.factories import CalendarEventFactory, CalendarLayerFactory
from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.enrollments.models import Enrollment
from lms.moduletypes.models import ModuleType
from lms.preferences.tests.factories import ColorThemeFactory
from lms.staff.tests.factories import StaffOfficeFactory, UserWithStaffProfileFactory
from lms.tags.services import normalize_tag_name
from lms.tags.tests.factories import TagFactory
from lms.users.tests.factories import LabUserFactory, UserFactory
from lms.utils.tests.test_decorators import parameterized_expand_doc

from ..models import (
    Cohort, Course, CourseBlock, CourseGroup, CourseModule, CourseSettings, CourseTeam, LinkedCourse,
    StudentCourseProgress,
)
from ..serializers import CourseModuleCreateBaseSerializer, CourseModuleUpdateBaseSerializer
from ..views.labapi import CourseModuleLabViewSet
from .factories import (
    CohortFactory, CourseBlockFactory, CourseCategoryFactory, CourseCityFactory, CourseFactory, CourseGroupFactory,
    CourseModuleFactory, CourseStudentFactory, CourseTeamFactory, CourseVisibilityFactory, CourseWorkflowFactory,
    FakeCourseModule, LinkedCourseFactory, ModuleTypeFactory, ProviderFactory, StudentCourseProgressFactory,
    StudentModuleProgressFactory, StudyModeFactory, TutorExternalFactory, TutorInternalFactory,
)

fake = faker.Faker()

User = get_user_model()


class LabWorkflowListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:workflow-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.workflows = CourseWorkflowFactory.create_batch(3, is_active=True)

    def test_url(self):
        self.assertURLNameEqual('workflows/', base_url=settings.LABAPI_BASE_URL)

    def test_list(self):
        self.client.force_login(user=self.user)
        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        expected = [
            {
                'id': workflow.id,
                'name': workflow.name,
                'is_active': workflow.is_active,
            } for workflow in sorted(self.workflows, key=lambda workflow: workflow.name)
        ]
        self.assertEqual(response.data, expected)


class LabStudyModeListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:studymode-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.study_modes = StudyModeFactory.create_batch(2)
        self.inactive_mode = StudyModeFactory(is_active=False)

    def test_url(self):
        self.assertURLNameEqual('studymodes/', base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        expected = [
            {
                'id': study_mode.id,
                'slug': study_mode.slug,
                'name': study_mode.name,
            } for study_mode in sorted(self.study_modes, key=lambda study_mode: study_mode.id)
        ]
        self.assertEqual(response.data, expected)


class LabProviderListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:provider-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.providers = ProviderFactory.create_batch(5)

    def test_url(self):
        self.assertURLNameEqual('providers/', base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        result = response.data

        expected_ids = [p.id for p in sorted(self.providers, key=lambda x: x.name)]
        result_ids = [p['id'] for p in result]
        self.assertListEqual(result_ids, expected_ids)

        expected = [
            {
                'id': p.id,
                'name': p.name,
            } for p in sorted(self.providers, key=lambda x: x.name)
        ]
        self.assertEqual(result, expected)


class LabCourseCityListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-city-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.cities = CourseCityFactory.create_batch(5)

    def test_url(self):
        self.assertURLNameEqual('course_cities/', base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        expected = [
            {
                'id': c.id,
                'name': c.name,
                'slug': c.slug,
            } for c in sorted(self.cities, key=lambda x: x.name)
        ]
        self.assertEqual(response.data, expected)


class LabCourseListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)

        self.active_courses = CourseFactory.create_batch(5, is_active=True)
        self.author_courses = CourseFactory.create_batch(5, author=self.user)
        self.inactive_courses = CourseFactory.create_batch(2)
        self.archive_courses = CourseFactory.create_batch(2, is_archive=True)

        self.study_mode = StudyModeFactory(is_active=True)
        self.workflow = CourseWorkflowFactory()
        self.provider = ProviderFactory()
        self.color_theme = ColorThemeFactory()
        self.city = CourseCityFactory()
        self.categories = CourseCategoryFactory.create_batch(5)
        self.tags = TagFactory.create_batch(7)

        for i, course in enumerate(self.active_courses[:3]):
            course.tags.set([tag.id for tag in self.tags[i: i + 2]])

        for course in self.active_courses + self.inactive_courses + self.archive_courses:
            assign_perm('courses.view_course', self.user, course)

    @staticmethod
    def build_expected_list(courses):
        return [
            {
                'id': course.id,
                'slug': course.slug,
                'name': course.name,
                'shortname': course.shortname,
                'summary': course.summary,
                'image_url': course.image_url,
                'city_id': course.city_id,
                'study_mode_id': course.study_mode_id,
                'author': {
                    'id': course.author.id,
                    'username': course.author.username,
                    'first_name': course.author.first_name,
                    'last_name': course.author.last_name,
                },
                'enable_groups': course.enable_groups,
                'structure': course.structure,
                'categories': sorted(category.id for category in course.categories.all()),
                'available_for_enroll': course.available_for_enroll,
                'is_full': course.is_full,
                'begin_date': course.begin_date,
                'end_date': course.end_date,
                'enroll_begin': course.enroll_begin,
                'enroll_end': course.enroll_end,
                'is_active': course.is_active,
                'is_archive': course.is_archive,
                'payment_method': course.payment_method,
                'price': course.price,
                'type': course.course_type,
                'tags': list(course.tags.values_list('name', flat=True)),
                'created': serializers.DateTimeField().to_representation(course.created),
                'modified': serializers.DateTimeField().to_representation(course.modified),
            }
            for course in sorted(courses, key=lambda course: course.name)
        ]

    def assert_list_request(self, url, expected):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(8):
            response = self.client.get(url, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), len(expected))

        results = response.data.get('results')

        expected_ids = [c.id for c in sorted(expected, key=lambda c: c.name)]
        result_ids = [c['id'] for c in results]
        self.assertListEqual(result_ids, expected_ids)

        expected = self.build_expected_list(expected)
        self.assertEqual(results, expected)

    def test_url(self):
        self.assertURLNameEqual('courses/', base_url=settings.LABAPI_BASE_URL)

    def test_list(self):
        self.assert_list_request(
            url=self.get_url() + "?limit=20",
            expected=self.active_courses + self.inactive_courses + self.author_courses + self.archive_courses,
        )

    def test_list_is_active_filter(self):
        self.assert_list_request(
            url=self.get_url() + "?limit=20&is_active=true",
            expected=self.active_courses,
        )

        self.assert_list_request(
            url=self.get_url() + "?limit=20&is_active=false",
            expected=self.inactive_courses + self.author_courses + self.archive_courses,
        )

    def test_list_is_archive_filter(self):
        self.assert_list_request(
            url=self.get_url() + "?limit=20&is_archive=true",
            expected=self.archive_courses,
        )

        self.assert_list_request(
            url=self.get_url() + "?limit=20&is_archive=false",
            expected=self.active_courses + self.inactive_courses + self.author_courses,
        )

    def test_list_name_filter(self):
        name_value = self.active_courses[0].name[0]
        courses = self.active_courses + self.inactive_courses + self.author_courses + self.archive_courses
        courses_filtered_by_name = [course for course in courses if name_value.lower() in course.name.lower()]

        self.assert_list_request(
            url=self.get_url() + "?limit=20&name=" + name_value,
            expected=courses_filtered_by_name,
        )

    def test_list_author_only_filter(self):
        self.assert_list_request(
            url=self.get_url() + "?limit=20&author_only=true",
            expected=self.author_courses,
        )


class LabCourseCreateTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.categories = CourseCategoryFactory.create_batch(5)
        self.study_mode = StudyModeFactory(is_active=True)
        self.workflow = CourseWorkflowFactory()
        self.provider = ProviderFactory()
        self.city = CourseCityFactory()
        self.tags = TagFactory.create_batch(5)

    def test_url(self):
        self.assertURLNameEqual('courses/', base_url=settings.LABAPI_BASE_URL)

    def test_create_forbidden(self):
        self.client.force_login(user=self.user)
        new_course = CourseFactory.build()
        new_course_data = {
            'name': new_course.name,
            'slug': new_course.slug,
            'study_mode_id': self.study_mode.id,
            'payment_method': new_course.payment_method,
        }

        with self.assertNumQueries(4):
            response = self.client.post(self.get_url(), data=new_course_data, format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN, response.data)

    def test_create_with_all_fields_forbidden(self):
        self.client.force_login(user=self.user)
        new_course = CourseFactory.build()
        now = datetime.now()

        new_course_data = {
            'name': new_course.name,
            'slug': new_course.slug,
            'categories': [category.id for category in sorted(self.categories, key=lambda category: category.name)],
            'study_mode_id': self.study_mode.id,
            'provider_id': self.provider.id,
            'city_id': self.city.id,
            'shortname': 'test_short_name',
            'summary': new_course.summary,
            'description': new_course.description,
            'image_url': 'https://yastatic.net/test.svg',
            'enroll_begin': serializers.DateTimeField().to_representation(now),
            'enroll_end': serializers.DateTimeField().to_representation(now + timedelta(days=1)),
            'begin_date': serializers.DateTimeField().to_representation(now + timedelta(days=2)),
            'end_date': serializers.DateTimeField().to_representation(now + timedelta(days=5)),
            'price': '1000.00',
            'paid_percent': 50,
            'payment_terms': CourseSettings.PaymentMethodChoices.PERSONAL,
            'num_hours': 10,
            'format': '',
            'payment_method': new_course.payment_method,
            'tags': [tag.name for tag in self.tags],
        }

        with self.assertNumQueries(4):
            response = self.client.post(self.get_url(), data=new_course_data, format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN, response.data)

    def test_create(self):
        self.client.force_login(user=self.user)
        new_course = CourseFactory.build()
        assign_perm('courses.add_course', self.user)
        new_course_data = {
            'name': new_course.name,
            'slug': new_course.slug,
            'study_mode_id': self.study_mode.id,
            'workflow_id': self.workflow.id,
            'payment_method': new_course.payment_method,
            'structure': new_course.structure,
        }

        with self.assertNumQueries(25):
            response = self.client.post(self.get_url(), data=new_course_data, format='json')

        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)

        created_course = Course.objects.get(slug=new_course_data.get('slug'))
        expected = {
            'id': created_course.id,
            'slug': new_course_data['slug'],
            'name': new_course_data['name'],
            'shortname': created_course.shortname,
            'summary': created_course.summary,
            'description': created_course.description,
            'image_url': created_course.image_url,
            'available_for_enroll': created_course.available_for_enroll,
            'is_full': created_course.is_full,
            'begin_date': created_course.begin_date,
            'end_date': created_course.end_date,
            'enroll_begin': created_course.enroll_begin,
            'enroll_end': created_course.enroll_end,
            'price': created_course.price,
            'paid_percent': created_course.paid_percent,
            'payment_terms': created_course.payment_terms,
            'num_hours': created_course.num_hours,
            'city_id': created_course.city_id,
            'study_mode_id': new_course_data['study_mode_id'],
            'provider_id': created_course.provider_id,
            'workflow': {
                'id': created_course.workflow.id,
                'name': created_course.workflow.name,
                'is_active': created_course.workflow.is_active,
            },
            'author': {
                'id': created_course.author.id,
                'username': created_course.author.username,
                'first_name': created_course.author.first_name,
                'last_name': created_course.author.last_name,
            },
            'enable_groups': created_course.enable_groups,
            'enable_followers': created_course.enable_followers,
            'structure': created_course.structure,
            'format': '',
            'categories': sorted(category.id for category in created_course.categories.all()),
            'is_active': created_course.is_active,
            'is_archive': created_course.is_archive,
            'payment_method': new_course_data['payment_method'],
            'enrollments_only': created_course.enrollments_only,
            'multi_enrollments': created_course.multi_enrollments,
            'retries_allowed': created_course.retries_allowed,
            'show_in_catalog': created_course.show_in_catalog,
            'type': Course.TypeChoices.COURSE,
            'tags': [],
            'created': serializers.DateTimeField().to_representation(created_course.created),
            'modified': serializers.DateTimeField().to_representation(created_course.modified),
        }

        self.assertEqual(response.data, expected)

    def test_create_with_all_fields(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.add_course', self.user)
        new_course = CourseFactory.build()
        now = datetime.now()
        tags = [tag.name for tag in self.tags[:3]]
        tags.extend(fake.pystr() for _ in range(3))

        new_course_data = {
            'name': new_course.name,
            'slug': new_course.slug,
            'categories': [category.id for category in sorted(self.categories, key=lambda category: category.name)],
            'study_mode_id': self.study_mode.id,
            'provider_id': self.provider.id,
            'workflow_id': self.workflow.id,
            'city_id': self.city.id,
            'shortname': 'test_short_name',
            'summary': new_course.summary,
            'description': new_course.description,
            'structure': new_course.structure,
            'image_url': 'https://yastatic.net/test.svg',
            'enroll_begin': serializers.DateTimeField().to_representation(now),
            'enroll_end': serializers.DateTimeField().to_representation(now + timedelta(days=1)),
            'begin_date': serializers.DateTimeField().to_representation(now + timedelta(days=2)),
            'end_date': serializers.DateTimeField().to_representation(now + timedelta(days=5)),
            'price': '1000.00',
            'paid_percent': 50,
            'payment_terms': CourseSettings.PaymentMethodChoices.PERSONAL,
            'num_hours': 10,
            'format': Course.FormatChoices.SELF_STUDY,
            'payment_method': new_course.payment_method,
            'type': Course.TypeChoices.COURSE,
            'tags': tags,
        }

        with self.assertNumQueries(45):
            response = self.client.post(self.get_url(), data=new_course_data, format='json')

        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)

        created_course = Course.objects.get(slug=new_course_data.get('slug'))
        expected = {
            'id': created_course.id,
            'slug': new_course_data['slug'],
            'name': new_course_data['name'],
            'shortname': new_course_data['shortname'],
            'summary': new_course_data['summary'],
            'description': new_course_data['description'],
            'image_url': new_course_data['image_url'],
            'available_for_enroll': created_course.available_for_enroll,
            'is_full': created_course.is_full,
            'begin_date': new_course_data['begin_date'],
            'end_date': new_course_data['end_date'],
            'enroll_begin': new_course_data['enroll_begin'],
            'enroll_end': new_course_data['enroll_end'],
            'price': new_course_data['price'],
            'paid_percent': new_course_data['paid_percent'],
            'payment_terms': new_course_data['payment_terms'],
            'num_hours': new_course_data['num_hours'],
            'city_id': new_course_data['city_id'],
            'study_mode_id': new_course_data['study_mode_id'],
            'provider_id': new_course_data['provider_id'],
            'workflow': {
                'id': self.workflow.id,
                'name': self.workflow.name,
                'is_active': self.workflow.is_active,
            },
            'author': {
                'id': created_course.author.id,
                'username': created_course.author.username,
                'first_name': created_course.author.first_name,
                'last_name': created_course.author.last_name,
            },
            'enable_groups': created_course.enable_groups,
            'enable_followers': created_course.enable_followers,
            'structure': new_course_data['structure'],
            'format': Course.FormatChoices.SELF_STUDY,
            'categories': new_course_data['categories'],
            'is_active': created_course.is_active,
            'is_archive': created_course.is_archive,
            'payment_method': new_course_data['payment_method'],
            'enrollments_only': created_course.enrollments_only,
            'multi_enrollments': created_course.multi_enrollments,
            'retries_allowed': created_course.retries_allowed,
            'show_in_catalog': created_course.show_in_catalog,
            'type': Course.TypeChoices.COURSE,
            'tags': sorted(tags, key=lambda x: normalize_tag_name(x)),
            'created': serializers.DateTimeField().to_representation(created_course.created),
            'modified': serializers.DateTimeField().to_representation(created_course.modified),
        }

        self.assertEqual(response.data, expected)

    def test_create_track_with_all_fields(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.add_course', self.user)
        new_track = CourseFactory.build()
        now = datetime.now()
        tags = [tag.name for tag in self.tags[:3]]
        tags.extend(fake.pystr() for _ in range(3))

        new_track_data = {
            'name': new_track.name,
            'slug': new_track.slug,
            'categories': [category.id for category in sorted(self.categories, key=lambda category: category.name)],
            'study_mode_id': self.study_mode.id,
            'provider_id': self.provider.id,
            'workflow_id': self.workflow.id,
            'city_id': self.city.id,
            'shortname': 'test_short_name',
            'summary': new_track.summary,
            'description': new_track.description,
            'structure': Course.StructureChoices.MULTI,
            'image_url': 'https://yastatic.net/test.svg',
            'enroll_begin': serializers.DateTimeField().to_representation(now),
            'enroll_end': serializers.DateTimeField().to_representation(now + timedelta(days=1)),
            'begin_date': serializers.DateTimeField().to_representation(now + timedelta(days=2)),
            'end_date': serializers.DateTimeField().to_representation(now + timedelta(days=5)),
            'price': '1000.00',
            'paid_percent': 50,
            'payment_terms': CourseSettings.PaymentMethodChoices.PERSONAL,
            'num_hours': 10,
            'format': Course.FormatChoices.SELF_STUDY,
            'payment_method': new_track.payment_method,
            'type': Course.TypeChoices.TRACK,
            'tags': tags,
        }

        with self.assertNumQueries(53):
            response = self.client.post(self.get_url(), data=new_track_data, format='json')

        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)

        created_track = Course.objects.get(slug=new_track_data.get('slug'))
        expected = {
            'id': created_track.id,
            'slug': new_track_data['slug'],
            'name': new_track_data['name'],
            'shortname': new_track_data['shortname'],
            'summary': new_track_data['summary'],
            'description': new_track_data['description'],
            'image_url': new_track_data['image_url'],
            'available_for_enroll': created_track.available_for_enroll,
            'is_full': created_track.is_full,
            'begin_date': new_track_data['begin_date'],
            'end_date': new_track_data['end_date'],
            'enroll_begin': new_track_data['enroll_begin'],
            'enroll_end': new_track_data['enroll_end'],
            'price': new_track_data['price'],
            'paid_percent': new_track_data['paid_percent'],
            'payment_terms': new_track_data['payment_terms'],
            'num_hours': new_track_data['num_hours'],
            'city_id': new_track_data['city_id'],
            'study_mode_id': new_track_data['study_mode_id'],
            'provider_id': new_track_data['provider_id'],
            'workflow': {
                'id': self.workflow.id,
                'name': self.workflow.name,
                'is_active': self.workflow.is_active,
            },
            'author': {
                'id': created_track.author.id,
                'username': created_track.author.username,
                'first_name': created_track.author.first_name,
                'last_name': created_track.author.last_name,
            },
            'enable_groups': created_track.enable_groups,
            'enable_followers': created_track.enable_followers,
            'structure': new_track_data['structure'],
            'format': Course.FormatChoices.SELF_STUDY,
            'categories': new_track_data['categories'],
            'is_active': created_track.is_active,
            'is_archive': created_track.is_archive,
            'payment_method': new_track_data['payment_method'],
            'enrollments_only': created_track.enrollments_only,
            'multi_enrollments': created_track.multi_enrollments,
            'retries_allowed': created_track.retries_allowed,
            'show_in_catalog': created_track.show_in_catalog,
            'type': Course.TypeChoices.TRACK,
            'tags': sorted(tags, key=lambda x: normalize_tag_name(x)),
            'created': serializers.DateTimeField().to_representation(created_track.created),
            'modified': serializers.DateTimeField().to_representation(created_track.modified),
        }

        self.assertEqual(
            Enrollment.objects.filter(
                course=created_track,
                name=created_track.name,
                enroll_type=Enrollment.TYPE_INSTANT,
                is_default=True,
            ).count(),
            1
        )
        self.assertEqual(response.data, expected)


class LabCourseDetailTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory()
        self.author_courses = CourseFactory.create_batch(5, author=self.user)
        self.active_courses = CourseFactory.create_batch(5)
        self.unpermitted_courses = CourseFactory.create_batch(5)
        self.inactive_courses = CourseFactory.create_batch(2, is_active=False)
        self.study_mode = StudyModeFactory(is_active=True)
        self.tags = TagFactory.create_batch(5)
        self.active_courses[0].tags.set(self.tags[:3])

    def test_url(self):
        self.assertURLNameEqual('courses/{}/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(self.author_courses[0].id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_get_forbidden(self):
        course = self.unpermitted_courses[0]
        self.client.force_login(user=self.user)

        with self.assertNumQueries(8):
            response = self.client.get(self.get_url(course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    @parameterized_expand_doc(
        [
            ('author_courses', 7, False),
            ('active_courses', 9, True),
            ('inactive_courses', 9, True),
        ]
    )
    def test_get(self, course_attr, num_queries, need_assign_perm):
        course = getattr(self, course_attr)[0]
        if need_assign_perm:
            assign_perm('courses.view_course', self.user, course)
        self.client.force_login(user=self.user)

        with self.assertNumQueries(num_queries):
            response = self.client.get(self.get_url(course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        expected = {
            'id': course.id,
            'slug': course.slug,
            'name': course.name,
            'shortname': course.shortname,
            'summary': course.summary,
            'description': course.description,
            'image_url': course.image_url,
            'city_id': course.city_id,
            'study_mode_id': course.study_mode_id,
            'provider_id': course.provider_id,
            'workflow': {
                'id': course.workflow.id,
                'name': course.workflow.name,
                'is_active': course.workflow.is_active,
            },
            'author': {
                'id': course.author.id,
                'username': course.author.username,
                'first_name': course.author.first_name,
                'last_name': course.author.last_name,
            },
            'enable_groups': course.enable_groups,
            'categories': sorted(category.id for category in course.categories.all()),
            'available_for_enroll': course.available_for_enroll,
            'is_full': course.is_full,
            'begin_date': course.begin_date,
            'end_date': course.end_date,
            'enroll_begin': course.enroll_begin,
            'enroll_end': course.enroll_end,
            'is_active': course.is_active,
            'is_archive': course.is_archive,
            'enable_followers': course.enable_followers,
            'payment_method': course.payment_method,
            'price': course.price,
            'paid_percent': course.paid_percent,
            'payment_terms': course.payment_terms,
            'num_hours': course.num_hours,
            'structure': course.structure,
            'format': '',
            'enrollments_only': course.enrollments_only,
            'multi_enrollments': course.multi_enrollments,
            'retries_allowed': course.retries_allowed,
            'show_in_catalog': course.show_in_catalog,
            'type': course.course_type,
            'tags': list(course.tags.values_list('name', flat=True)),
            'created': serializers.DateTimeField().to_representation(course.created),
            'modified': serializers.DateTimeField().to_representation(course.modified),
        }
        self.assertEqual(response.data, expected)

    def test_update_forbidden(self):
        course = self.unpermitted_courses[0]
        self.client.force_login(user=self.user)

        update_data = {
            'name': 'testtestes',
            'slug': 'testslug',
            'study_mode_id': self.study_mode.id,
            'payment_method': 'free',
        }
        with self.assertNumQueries(8):
            response = self.client.put(self.get_url(course.id), data=update_data, format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    @parameterized_expand_doc(
        [
            ('author_courses', 22, False),
            ('active_courses', 24, True),
            ('inactive_courses', 24, True),
        ]
    )
    def test_update(self, course_attr, num_queries, need_assign_perm):
        course = getattr(self, course_attr)[0]
        if need_assign_perm:
            assign_perm('courses.change_course', self.user, course)
        self.client.force_login(user=self.user)

        update_data = {
            'name': 'testtestes',
            'slug': 'testslug',
            'study_mode_id': self.study_mode.id,
            'payment_method': 'free',
            'structure': Course.StructureChoices.NO_MODULES,
        }
        with self.assertNumQueries(num_queries):
            response = self.client.put(self.get_url(course.id), data=update_data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        updated_course = Course.objects.get(id=course.id)
        expected = {
            'id': updated_course.id,
            'slug': update_data['slug'],
            'name': update_data['name'],
            'shortname': updated_course.shortname,
            'summary': updated_course.summary,
            'description': updated_course.description,
            'image_url': updated_course.image_url,
            'available_for_enroll': updated_course.available_for_enroll,
            'is_full': updated_course.is_full,
            'begin_date': updated_course.begin_date,
            'end_date': updated_course.end_date,
            'enroll_begin': updated_course.enroll_begin,
            'enroll_end': updated_course.enroll_end,
            'price': updated_course.price,
            'paid_percent': updated_course.paid_percent,
            'payment_terms': updated_course.payment_terms,
            'num_hours': updated_course.num_hours,
            'city_id': updated_course.city_id,
            'study_mode_id': update_data['study_mode_id'],
            'provider_id': updated_course.provider_id,
            'workflow': {
                'id': updated_course.workflow.id,
                'name': updated_course.workflow.name,
                'is_active': updated_course.workflow.is_active,
            },
            'author': {
                'id': updated_course.author.id,
                'username': updated_course.author.username,
                'first_name': updated_course.author.first_name,
                'last_name': updated_course.author.last_name,
            },
            'enable_groups': updated_course.enable_groups,
            'enable_followers': updated_course.enable_followers,
            'structure': update_data['structure'],
            'format': '',
            'categories': sorted(category.id for category in updated_course.categories.all()),
            'is_active': updated_course.is_active,
            'is_archive': updated_course.is_archive,
            'payment_method': updated_course.payment_method,
            'enrollments_only': updated_course.enrollments_only,
            'multi_enrollments': updated_course.multi_enrollments,
            'retries_allowed': updated_course.retries_allowed,
            'show_in_catalog': updated_course.show_in_catalog,
            'type': updated_course.course_type,
            'tags': list(updated_course.tags.values_list('name', flat=True)),
            'created': serializers.DateTimeField().to_representation(updated_course.created),
            'modified': serializers.DateTimeField().to_representation(updated_course.modified),
        }
        self.assertEqual(response.data, expected)

    def test_partial_update_forbidden(self):
        course = self.unpermitted_courses[0]
        self.client.force_login(user=self.user)

        update_data = {
            'name': 'newtestcourse',
        }
        with self.assertNumQueries(8):
            response = self.client.patch(self.get_url(course.id), data=update_data, format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    @parameterized_expand_doc(
        [
            ('author_courses', 20, False),
            ('active_courses', 22, True),
            ('inactive_courses', 22, True),
        ]
    )
    def test_partial_update(self, course_attr, num_queries, need_assign_perm):
        course = getattr(self, course_attr)[0]
        if need_assign_perm:
            assign_perm('courses.change_course', self.user, course)
        self.client.force_login(user=self.user)

        update_data = {
            'name': 'newtestcourse',
        }
        with self.assertNumQueries(num_queries):
            response = self.client.patch(self.get_url(course.id), data=update_data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        updated_course = Course.objects.get(id=course.id)
        expected = {
            'id': updated_course.id,
            'slug': updated_course.slug,
            'name': update_data['name'],
            'shortname': updated_course.shortname,
            'summary': updated_course.summary,
            'description': updated_course.description,
            'image_url': updated_course.image_url,
            'available_for_enroll': updated_course.available_for_enroll,
            'is_full': updated_course.is_full,
            'begin_date': updated_course.begin_date,
            'end_date': updated_course.end_date,
            'enroll_begin': updated_course.enroll_begin,
            'enroll_end': updated_course.enroll_end,
            'price': updated_course.price,
            'paid_percent': updated_course.paid_percent,
            'payment_terms': updated_course.payment_terms,
            'num_hours': updated_course.num_hours,
            'city_id': updated_course.city_id,
            'study_mode_id': updated_course.study_mode_id,
            'provider_id': updated_course.provider_id,
            'workflow': {
                'id': updated_course.workflow.id,
                'name': updated_course.workflow.name,
                'is_active': updated_course.workflow.is_active,
            },
            'author': {
                'id': updated_course.author.id,
                'username': updated_course.author.username,
                'first_name': updated_course.author.first_name,
                'last_name': updated_course.author.last_name,
            },
            'enable_groups': updated_course.enable_groups,
            'enable_followers': updated_course.enable_followers,
            'structure': updated_course.structure,
            'format': '',
            'categories': sorted(category.id for category in updated_course.categories.all()),
            'is_active': updated_course.is_active,
            'is_archive': updated_course.is_archive,
            'payment_method': updated_course.payment_method,
            'enrollments_only': updated_course.enrollments_only,
            'multi_enrollments': updated_course.multi_enrollments,
            'retries_allowed': updated_course.retries_allowed,
            'show_in_catalog': updated_course.show_in_catalog,
            'type': updated_course.course_type,
            'tags': list(updated_course.tags.values_list('name', flat=True)),
            'created': serializers.DateTimeField().to_representation(updated_course.created),
            'modified': serializers.DateTimeField().to_representation(updated_course.modified),
        }
        self.assertEqual(response.data, expected)


class LabCourseCategoryListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-category-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.categories = CourseCategoryFactory.create_batch(5)

    def test_url(self):
        self.assertURLNameEqual('course_categories/', base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)
        with self.assertNumQueries(6):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), len(self.categories))
        results = response.data.get('results')

        expected = [
            {
                'id': c.id,
                'parent_id': c.parent_id,
                'slug': c.slug,
                'name': c.name,
                'depth': c.node_depth,
                'color_theme': {
                    'id': c.color_theme.id,
                    'name': c.color_theme.name,
                    'slug': c.color_theme.slug,
                    'is_active': c.color_theme.is_active,
                    'course_card_gradient_color': c.color_theme.course_card_gradient_color,
                    'created': serializers.DateTimeField().to_representation(c.color_theme.created),
                    'modified': serializers.DateTimeField().to_representation(c.color_theme.modified),
                },
                'created': serializers.DateTimeField().to_representation(c.created),
                'modified': serializers.DateTimeField().to_representation(c.modified),
            } for c in self.categories
        ]

        self.assertEqual(results, expected)


class LabCourseCategoryDetailTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-category-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.category = CourseCategoryFactory()

    def test_url(self):
        self.assertURLNameEqual('course_categories/{}/', args=(self.category.id,), base_url=settings.LABAPI_BASE_URL)

    def test_get(self):
        self.client.force_login(user=self.user)
        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(self.category.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        result = response.data

        expected = {
            'id': self.category.id,
            'parent_id': self.category.parent_id,
            'slug': self.category.slug,
            'name': self.category.name,
            'depth': self.category.node_depth,
            'color_theme': {
                'id': self.category.color_theme.id,
                'slug': self.category.color_theme.slug,
                'name': self.category.color_theme.name,
                'is_active': self.category.color_theme.is_active,
                'course_card_gradient_color': self.category.color_theme.course_card_gradient_color,
                'created': serializers.DateTimeField().to_representation(self.category.color_theme.created),
                'modified': serializers.DateTimeField().to_representation(self.category.color_theme.modified),
            },
            'created': serializers.DateTimeField().to_representation(self.category.created),
            'modified': serializers.DateTimeField().to_representation(self.category.modified),

        }

        self.assertEqual(result, expected)


class LabCourseCategoriesListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-categories'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()
        assign_perm('courses.view_course', self.user, self.course)
        self.categories = CourseCategoryFactory.create_batch(5)
        self.inactive_categories = CourseCategoryFactory.create_batch(2, is_active=False)
        self.course.categories.add(*(self.categories + self.inactive_categories))

    def test_url(self):
        self.assertURLNameEqual('courses/{}/categories/', args=(self.course.id,), base_url=settings.LABAPI_BASE_URL)

    def test_list(self):
        self.client.force_login(user=self.user)

        categories = self.categories + self.inactive_categories

        with self.assertNumQueries(10):
            response = self.client.get(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), len(categories))
        results = response.data.get('results')

        expected = [
            {
                'id': c.id,
                'parent_id': c.parent_id,
                'slug': c.slug,
                'name': c.name,
                'depth': c.node_depth,
                'color_theme': {
                    'id': c.color_theme.id,
                    'name': c.color_theme.name,
                    'slug': c.color_theme.slug,
                    'is_active': c.color_theme.is_active,
                    'created': serializers.DateTimeField().to_representation(c.color_theme.created),
                    'modified': serializers.DateTimeField().to_representation(c.color_theme.modified),
                    'course_card_gradient_color': c.color_theme.course_card_gradient_color,
                },
                'created': serializers.DateTimeField().to_representation(c.created),
                'modified': serializers.DateTimeField().to_representation(c.modified),
            } for c in sorted(categories, key=lambda x: x.name)
        ]

        self.assertEqual(results, expected)


class LabCourseGroupListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-group-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()
        self.course_groups = CourseGroupFactory.create_batch(5, course=self.course)
        self.other_course_groups = CourseFactory.create_batch(2)

    def test_url(self):
        self.assertURLNameEqual('courses/{}/groups/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.view_course', self.user, self.course)
        with self.assertNumQueries(10):
            response = self.client.get(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIsNone(response.data.get('next'))
        self.assertIsNone(response.data.get('previous'))
        self.assertEqual(response.data.get('count'), len(self.course_groups))
        results = response.data.get('results')
        expected = [
            {
                'id': course_group.id,
                'course_id': course_group.course_id,
                'slug': course_group.slug,
                'name': course_group.name,
                'summary': course_group.summary,
                'is_active': course_group.is_active,
                'can_join': course_group.can_join,
                'num_participants': course_group.num_participants,
                'max_participants': course_group.max_participants,
                'available_for_enroll': course_group.available_for_enroll,
                'is_enroll_open': course_group.is_enroll_open,
                'enroll_will_begin': course_group.enroll_will_begin,
                'has_open_seats': course_group.has_open_seats,
                'is_full': course_group.is_full,
                'begin_date': course_group.begin_date,
                'end_date': course_group.end_date,
                'enroll_begin': course_group.enroll_begin,
                'enroll_end': course_group.enroll_end,
                'tutor': None,
            } for course_group in sorted(self.course_groups, key=lambda course_group: course_group.name)
        ]
        self.assertListEqual(expected, results)

    def test_no_course(self):
        self.client.force_login(user=self.user)
        max_course_id = Course.objects.aggregate(max_id=Max('id')).get('max_id', 0) or 0
        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(max_course_id + 1), format='json')

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        response_data = response.data
        self.assertEqual('not_found', response_data['detail'].code)


class LabCourseGroupCreateTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-group-create'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)

    def test_url(self):
        self.assertURLNameEqual('course_groups/', base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.post(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_create(self):
        self.client.force_login(user=self.user)
        course = CourseFactory()
        tutor = TutorInternalFactory()
        assign_perm('courses.change_course', self.user, course)
        course_group = CourseGroupFactory.build(course=course)
        data = {
            'course_id': course.id,
            'slug': course_group.slug,
            'name': course_group.name,
            'summary': course_group.summary,
            'is_active': course_group.is_active,
            'can_join': course_group.can_join,
            'max_participants': course_group.max_participants,
            'tutor_id': tutor.id,
        }
        with self.assertNumQueries(23):
            response = self.client.post(self.get_url(), data=data, format='json')
        response_data = response.data
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        expected = data
        del expected['tutor_id']
        expected['id'] = response_data['id']
        expected['num_participants'] = response_data['num_participants']
        expected.update({
            'begin_date': course_group.begin_date,
            'end_date': course_group.end_date,
            'enroll_begin': course_group.enroll_begin,
            'enroll_end': course_group.enroll_end,
            'available_for_enroll': course_group.available_for_enroll,
            'is_enroll_open': course_group.is_enroll_open,
            'enroll_will_begin': course_group.enroll_will_begin,
            'has_open_seats': course_group.has_open_seats,
            'is_full': course_group.is_full,
            'tutor': {
                'id': tutor.id,
                'is_internal': True,
                'is_active': tutor.is_active,
                'user': {
                    'id': tutor.user.id,
                    'username': tutor.user.username,
                    'first_name': tutor.user.first_name,
                    'last_name': tutor.user.last_name,
                },
                'name': f'{tutor.user.first_name} {tutor.user.last_name}',
                'email': tutor.email,
                'url': f'{settings.STAFF_BASE_URL}/{tutor.user.username}',
                'position': tutor.position,
            },
        })

        self.assertEqual(expected, response_data, f"\n{expected}\n====\n{response_data}")


class LabCourseGroupDetailTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-group-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course_group = CourseGroupFactory()

    def test_url(self):
        self.assertURLNameEqual('course_groups/{}/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=False))

        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(1), format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_get(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.view_course', self.user, self.course_group.course)
        with self.assertNumQueries(8):
            response = self.client.get(self.get_url(self.course_group.id), format='json')
        response_data = response.data
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        expected = {
            'id': self.course_group.id,
            'course_id': self.course_group.course_id,
            'slug': self.course_group.slug,
            'name': self.course_group.name,
            'summary': self.course_group.summary,
            'is_active': self.course_group.is_active,
            'can_join': self.course_group.can_join,
            'num_participants': self.course_group.num_participants,
            'max_participants': self.course_group.max_participants,
            'available_for_enroll': self.course_group.available_for_enroll,
            'is_enroll_open': self.course_group.is_enroll_open,
            'enroll_will_begin': self.course_group.enroll_will_begin,
            'has_open_seats': self.course_group.has_open_seats,
            'is_full': self.course_group.is_full,
            'begin_date': self.course_group.begin_date,
            'end_date': self.course_group.end_date,
            'enroll_begin': self.course_group.enroll_begin,
            'enroll_end': self.course_group.enroll_end,
            'tutor': None,
        }
        self.assertEqual(expected, response_data)

    def test_put(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.change_course', self.user, self.course_group.course)
        new_course_group = CourseGroupFactory.build(id=self.course_group.id, course=self.course_group.course)
        data = {
            'course_id': self.course_group.course.id,
            'slug': new_course_group.slug,
            'name': new_course_group.name,
            'summary': new_course_group.summary,
            'is_active': new_course_group.is_active,
            'can_join': new_course_group.can_join,
            'max_participants': new_course_group.max_participants,
        }

        with self.assertNumQueries(20):
            response = self.client.put(self.get_url(self.course_group.id), data=data, format='json')
        response_data = response.data
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        expected = data

        self.assertEqual(CourseGroup.objects.filter(**expected).count(), 1)

        new_course_group = CourseGroup.objects.filter(**expected).first()

        expected.update({
            'id': new_course_group.id,
            'num_participants': new_course_group.num_participants,
            'begin_date': new_course_group.begin_date,
            'end_date': new_course_group.end_date,
            'enroll_begin': new_course_group.enroll_begin,
            'enroll_end': new_course_group.enroll_end,
            'available_for_enroll': new_course_group.available_for_enroll,
            'is_enroll_open': new_course_group.is_enroll_open,
            'enroll_will_begin': new_course_group.enroll_will_begin,
            'has_open_seats': new_course_group.has_open_seats,
            'is_full': new_course_group.is_full,
            'tutor': None,
        })

        self.assertEqual(expected, response_data)

    def test_patch(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.change_course', self.user, self.course_group.course)
        new_course_group = CourseGroupFactory.build(id=self.course_group.id, course=self.course_group.course)
        tutor = TutorExternalFactory()
        data = {
            'slug': new_course_group.slug,
            'summary': new_course_group.summary,
            'can_join': new_course_group.can_join,
            'max_participants': new_course_group.max_participants,
            'begin_date': new_course_group.begin_date,
            'end_date': new_course_group.end_date,
            'enroll_begin': new_course_group.enroll_begin,
            'enroll_end': new_course_group.enroll_end,
            'tutor_id': tutor.id,
        }
        with self.assertNumQueries(20):
            response = self.client.patch(self.get_url(self.course_group.id), data=data, format='json')
        response_data = response.data
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        expected = data
        del expected['tutor_id']

        self.assertEqual(CourseGroup.objects.filter(**expected).count(), 1)

        course_group = CourseGroup.objects.filter(**expected).first()

        expected.update({
            'id': course_group.id,
            'course_id': course_group.course_id,
            'name': course_group.name,
            'is_active': course_group.is_active,
            'num_participants': course_group.num_participants,
            'available_for_enroll': course_group.available_for_enroll,
            'is_enroll_open': course_group.is_enroll_open,
            'enroll_will_begin': course_group.enroll_will_begin,
            'has_open_seats': course_group.has_open_seats,
            'is_full': course_group.is_full,
            'tutor': {
                'id': tutor.id,
                'is_internal': tutor.is_internal,
                'is_active': tutor.is_active,
                'user': None,
                'name': tutor.name,
                'email': tutor.email,
                'url': tutor.url,
                'position': tutor.position,
            },
        })
        self.assertEqual(expected, response_data)

    def test_delete(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.change_course', self.user, self.course_group.course)
        with self.assertNumQueries(25):
            response = self.client.delete(self.get_url(self.course_group.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertEqual(CourseGroup.objects.filter(id=self.course_group.id).count(), 0)


class LabCourseTeamListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-team-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course_teams = CourseTeamFactory.create_batch(2)

    def test_url(self):
        self.assertURLNameEqual('course_teams/', base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(6):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), len(self.course_teams))

        results = response.data.get('results')
        expected = [
            {
                'id': course.id,
                'name': course.name,
            }
            for course in sorted(self.course_teams, key=lambda team: team.name)
        ]
        self.assertEqual(results, expected)

    def test_create(self):
        self.client.force_login(user=self.user)
        new_course_team = CourseTeamFactory.build()
        new_course_team_data = {
            'name': new_course_team.name,
        }

        with self.assertNumQueries(7):
            response = self.client.post(self.get_url(), data=new_course_team_data, format='json')

        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response.data)

        created_course_team = CourseTeam.objects.get(name=new_course_team.name)
        expected = {
            'id': created_course_team.id,
            'name': new_course_team_data['name'],
        }
        self.assertEqual(response.data, expected)


class LabCourseTeamDetailTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-team-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.teams = CourseTeamFactory.create_batch(5)

    def test_url(self):
        self.assertURLNameEqual('course_teams/{}/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(self.teams[0].id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_get(self):
        course_team = self.teams[0]
        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(course_team.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        expected = {
            'id': course_team.id,
            'name': course_team.name,
        }
        self.assertEqual(response.data, expected)

    def test_update(self):
        course_team = self.teams[0]
        self.client.force_login(user=self.user)

        update_data = {
            'name': 'new_team_name',
        }
        with self.assertNumQueries(8):
            response = self.client.put(self.get_url(course_team.id), data=update_data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        updated_course_team = CourseTeam.objects.get(id=course_team.id)
        expected = {
            'id': updated_course_team.id,
            'name': update_data['name'],
        }
        self.assertEqual(response.data, expected)

    def test_delete(self):
        course_team = self.teams[0]
        self.client.force_login(user=self.user)

        with self.assertNumQueries(14):
            response = self.client.delete(self.get_url(course_team.id))

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

        is_exists = CourseTeam.objects.filter(id=course_team.id).exists()
        self.assertFalse(is_exists)


class LabCourseTeamMembersListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-team-members'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.team_members = LabUserFactory.create_batch(3)
        self.team = CourseTeamFactory()

        for member in self.team_members:
            self.team.user_set.add(member)

    def test_url(self):
        self.assertURLNameEqual('course_teams/{}/members/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(self.team.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(7):
            response = self.client.get(self.get_url(self.team.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        results = response.data
        expected = [
            {
                'id': member.id,
                'username': member.username,
                'first_name': member.first_name,
                'last_name': member.last_name,
            } for member in sorted(self.team.members, key=lambda u: u.username)
        ]
        self.assertEqual(results, expected)

    def test_update(self):
        self.client.force_login(user=self.user)

        new_members = LabUserFactory.create_batch(5)
        update_data = [user.id for user in new_members]

        with self.assertNumQueries(11):
            response = self.client.put(self.get_url(self.team.id), data=update_data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        results = response.data
        expected = [
            {
                'id': member.id,
                'username': member.username,
                'first_name': member.first_name,
                'last_name': member.last_name,
            } for member in sorted(new_members, key=lambda u: u.username)
        ]
        self.assertEqual(results, expected)

    def test_delete(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(7):
            response = self.client.delete(self.get_url(self.team.id))

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

        self.assertEqual(self.team.user_set.count(), 0)


class LabCourseTeamsListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-course-teams'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.teams = CourseTeamFactory.create_batch(5)
        self.course = CourseFactory()
        self.course.teams.set(self.teams)

    def test_url(self):
        self.assertURLNameEqual('courses/{}/teams/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(self.course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(6):
            response = self.client.get(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        results = response.data
        expected = [
            {
                'id': team.id,
                'name': team.name,
            } for team in self.course.teams.all()
        ]
        self.assertEqual(results, expected)

    def test_update(self):
        self.client.force_login(user=self.user)

        new_teams = CourseTeamFactory.create_batch(3)
        update_data = [new_team.id for new_team in new_teams]

        with self.assertNumQueries(15):
            response = self.client.put(self.get_url(self.course.id), data=update_data, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        results = response.data
        expected = [
            {
                'id': team.id,
                'name': team.name,
            } for team in sorted(new_teams, key=lambda team: team.name)
        ]
        self.assertEqual(results, expected)

    def test_delete(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(8):
            response = self.client.delete(self.get_url(self.course.id))

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

        self.assertEqual(self.course.teams.count(), 0)


class LabCourseVisibilityCheckDefailTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-visibility-check'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(author=self.user, is_active=True)  # type: Course

    def test_url(self):
        self.assertURLNameEqual(
            'courses/{}/visibility/check/',
            args=(self.course.id,),
            base_url=settings.LABAPI_BASE_URL,
        )

    def test_permission_denied(self):
        response = self.client.get(self.get_url(self.course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def build_url(self, course, username=None):
        return '{}{}'.format(
            self.get_url(course.id),
            f'?username={username}' if username else ''
        )

    def assertCourseVisible(self, course, username=None, visible=True, num_queries=0):  # noqa: N802
        self.client.force_login(user=self.user)

        with self.assertNumQueries(num_queries):
            url = self.build_url(course, username)
            response = self.client.get(url, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        expected = {
            'visible': visible
        }
        self.assertEqual(response.data, expected)

    def test_detail_no_visibility(self):
        student = UserWithStaffProfileFactory()
        self.assertCourseVisible(self.course, student.username, num_queries=7)

    def test_detail_visible(self):
        office = StaffOfficeFactory()
        student = UserWithStaffProfileFactory(staffprofile__office=office)
        CourseVisibilityFactory(
            course=self.course,
            rules={"eq": ["staff_office", office.id]}
        )
        self.assertCourseVisible(self.course, student.username, num_queries=8)

    def test_detail_not_visible(self):
        office = StaffOfficeFactory()
        student = UserWithStaffProfileFactory()
        CourseVisibilityFactory(
            course=self.course,
            rules={"eq": ["staff_office", office.id]}
        )
        self.assertCourseVisible(self.course, student.username, visible=False, num_queries=8)

    def test_wrong_username(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            url = self.build_url(self.course, 'unknown_username')
            response = self.client.get(url, format='json')

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


class BaseCourseSlugViewSetDetailTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-slug-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()

    def test_url(self):
        self.assertURLNameEqual(
            'course_slug/{}/',
            args=(self.course.id,),
            base_url=settings.LABAPI_BASE_URL,
        )

    def test_get_course_id(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            url = self.get_url(self.course.slug)
            response = self.client.get(url, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data, {'id': self.course.id})

    def test_not_found(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            url = self.get_url(fake.slug())
            response = self.client.get(url, format='json')

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


class BaseCourseSlugViewSetValidateTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-slug-validate'

    def setUp(self):
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()

    def test_url(self):
        self.assertURLNameEqual(
            'course_slug/{}/validate/',
            args=(fake.slug(),),
            base_url=settings.LABAPI_BASE_URL,
        )

    def test_not_valid(self):
        self.client.force_login(user=self.user)

        long_string = fake.pystr(min_chars=300, max_chars=500)
        invalid_slugs = [
            'new', 'archived', 'drafts', 'all', 'with space', 'неанглийский', self.course.slug, long_string,
        ]

        for invalid_slug in invalid_slugs:
            with self.assertNumQueries(5):
                url = self.get_url(invalid_slug)
                response = self.client.get(url, format='json')

            self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_valid(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            url = self.get_url(fake.slug())
            response = self.client.get(url, format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)


class LabCourseModuleListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-module-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)
        self.course_modules: Iterable[CourseModule] = CourseModuleFactory.create_batch(5, course=self.course)

    def test_url(self):
        self.assertURLNameEqual('courses/{}/modules/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_list(self):
        self.client.force_login(user=self.user)
        url = self.get_url(self.course.id)

        with self.assertNumQueries(6):
            response: Union[Response, HttpResponse] = self.client.get(url, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        expected = [{
            'id': m.id,
            'module_type': {
                'name': m.module_type.name,
                'app_label': m.module_type.app_label,
                'model': m.module_type.model,
            },
            'name': m.name,
            'description': m.description,
            'estimated_time': m.estimated_time,
            'is_active': m.is_active,
            'created': serializers.DateTimeField().to_representation(m.created),
            'modified': serializers.DateTimeField().to_representation(m.modified),
            'block_id': None,
        } for m in self.course_modules]

        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), len(self.course_modules))
        results = response.data.get('results')
        self.assertEqual(results, expected, results)


class LabCourseModuleMovesTestCase(UrlNameMixin, APITestCase):
    URL_NAME_FORMAT = "labapi:course-module-{}"
    BASE_URL = settings.LABAPI_BASE_URL

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)
        self.course_modules = CourseModuleFactory.create_batch(10, course=self.course)

    def get_action_url(self, action: str, *args, **kwargs):
        action = action.replace('_', '-')
        url_name = self.URL_NAME_FORMAT.format(action)
        return reverse(url_name, args=args, kwargs=kwargs)

    def assertActionURLEqual(self, action: str):  # noqa N802
        url = f"/{self.BASE_URL}course_modules/1/{action}/"
        expected = self.get_action_url(action, 1)
        self.assertURLEqual(expected, url)

    def test_action_urls(self):
        self.assertActionURLEqual('move_up')
        self.assertActionURLEqual('move_down')
        self.assertActionURLEqual('move_top')
        self.assertActionURLEqual('move_bottom')
        self.assertActionURLEqual('move_above')
        self.assertActionURLEqual('move_below')

    def test_move_above(self):
        self.client.force_login(user=self.user)

        last_module: CourseModule = self.course_modules[-1]
        second_module: CourseModule = self.course_modules[1]

        url = self.get_action_url('move_above', last_module.id)

        payload = {
            "module_id": second_module.id,
        }

        with self.assertNumQueries(12):
            response: Union[Response, HttpResponse] = self.client.put(url, data=payload, format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT, msg=response.data)

        last_module.refresh_from_db()
        second_module.refresh_from_db()
        self.assertLess(last_module.order, second_module.order, msg="last_module должен быть выше second_module")

    def test_move_below(self):
        self.client.force_login(user=self.user)

        first_module: CourseModule = self.course_modules[0]
        third_module: CourseModule = self.course_modules[2]

        url = self.get_action_url('move_below', first_module.id)

        payload = {
            "module_id": third_module.id,
        }

        with self.assertNumQueries(12):
            response: Union[Response, HttpResponse] = self.client.put(url, data=payload, format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT, msg=response.data)

        first_module.refresh_from_db()
        third_module.refresh_from_db()
        self.assertGreater(first_module.order, third_module.order, msg="first_module должен быть ниже third_module")


class LabCourseModuleDetailTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-module-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)
        self.course_module = CourseModuleFactory(course=self.course)

    def test_url(self):
        self.assertURLNameEqual('course_modules/{}/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_detail(self):
        self.client.force_login(user=self.user)
        url = self.get_url(self.course_module.id)

        with self.assertNumQueries(5):
            response: Union[Response, HttpResponse] = self.client.get(url, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        response_data = response.data

        module: CourseModule = self.course_module
        expected = {
            'id': module.id,
            'module_type': {
                'name': module.module_type.name,
                'app_label': module.module_type.app_label,
                'model': module.module_type.model,
            },
            'name': module.name,
            'description': module.description,
            'estimated_time': module.estimated_time,
            'is_active': module.is_active,
            'created': serializers.DateTimeField().to_representation(module.created),
            'modified': serializers.DateTimeField().to_representation(module.modified),
            'block_id': None,
        }
        self.assertEqual(response_data, expected, response_data)


class LabTutorListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:tutor-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.tutors = TutorInternalFactory.create_batch(2) + TutorInternalFactory.create_batch(2)
        self.inactive_tutors = (
            TutorInternalFactory.create_batch(2, is_active=False) +
            TutorInternalFactory.create_batch(2, is_active=False)
        )

    def test_url(self):
        self.assertURLNameEqual('tutors/', args=(), base_url=settings.LABAPI_BASE_URL)

    def test_list(self):
        expected = [
            {
                'id': tutor.id,
                'is_internal': True,
                'is_active': tutor.is_active,
                'user': {
                    'id': tutor.user.id,
                    'username': tutor.user.username,
                    'first_name': tutor.user.first_name,
                    'last_name': tutor.user.last_name,
                },
                'name': f'{tutor.user.first_name} {tutor.user.last_name}',
                'email': tutor.email,
                'url': f'{settings.STAFF_BASE_URL}/{tutor.user.username}',
                'position': tutor.position,
            } for tutor in self.tutors
        ]
        self.client.force_login(user=self.user)
        self.list_request(url=self.get_url(), expected=expected, num_queries=5, pagination=False)


class LabTutorDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:tutor-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.tutor = TutorInternalFactory()

    def test_url(self):
        self.assertURLNameEqual('tutors/{}/', args=(self.tutor.id,), base_url=settings.LABAPI_BASE_URL)

    def test_detail(self):
        expected = {
            'id': self.tutor.id,
            'is_internal': True,
            'is_active': self.tutor.is_active,
            'user': {
                'id': self.tutor.user.id,
                'username': self.tutor.user.username,
                'first_name': self.tutor.user.first_name,
                'last_name': self.tutor.user.last_name,
            },
            'name': f'{self.tutor.user.first_name} {self.tutor.user.last_name}',
            'email': self.tutor.email,
            'url': f'{settings.STAFF_BASE_URL}/{self.tutor.user.username}',
            'position': self.tutor.position,
        }

        self.client.force_login(user=self.user)
        self.detail_request(url=self.get_url(self.tutor.id), expected=expected, num_queries=5)


class LabCohortListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:cohort-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()
        self.course_cohorts = CohortFactory.create_batch(5, course=self.course, logins=[self.user.username])
        self.other_course_cohorts = CohortFactory.create_batch(5, course=CourseFactory(), logins=[self.user.username])
        self.global_cohorts = CohortFactory.create_batch(5, course=None, logins=[self.user.username])

    def test_url(self):
        self.assertURLNameEqual('courses/{}/cohorts/', args=(self.course.id,), base_url=settings.LABAPI_BASE_URL)

    def test_list_without_global(self):
        expected = [
            {
                'id': cohort.id,
                'name': cohort.name,
                'status': cohort.status,
                'logins': cohort.logins,
                'course_id': self.course.id,
                'is_active': cohort.is_active,
                'created': serializers.DateTimeField().to_representation(cohort.created),
                'modified': serializers.DateTimeField().to_representation(cohort.modified),
            }
            for cohort
            in sorted(self.course_cohorts, key=lambda cohort: cohort.name)
        ]

        assign_perm('courses.view_course', self.user, self.course)
        self.client.force_login(user=self.user)
        self.list_request(url=self.get_url(self.course.id), expected=expected, pagination=False, num_queries=9)

    def test_list_with_global(self):
        expected = [
            {
                'id': cohort.id,
                'name': cohort.name,
                'status': cohort.status,
                'logins': cohort.logins,
                'course_id': cohort.course_id,
                'is_active': cohort.is_active,
                'created': serializers.DateTimeField().to_representation(cohort.created),
                'modified': serializers.DateTimeField().to_representation(cohort.modified),
            }
            for cohort
            in sorted(
                self.global_cohorts, key=lambda cohort: cohort.name,
            ) + sorted(
                self.course_cohorts, key=lambda cohort: cohort.name,
            )
        ]

        assign_perm('courses.view_course', self.user, self.course)
        self.client.force_login(user=self.user)
        url = self.get_url(self.course.id) + "?with_global=true"
        self.list_request(url=url, expected=expected, pagination=False, num_queries=9)


class LabCohortCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:cohort-create'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()

    def test_url(self):
        self.assertURLNameEqual('cohorts/', base_url=settings.LABAPI_BASE_URL)

    def test_create(self):
        cohort = CohortFactory.build(course=self.course, logins=[self.user.username])
        data = {
            'name': cohort.name,
            'logins': cohort.logins,
            'course_id': cohort.course_id,
            'is_active': cohort.is_active,
        }

        def expected(response):
            return {
                'id': response.data['id'],
                'name': cohort.name,
                'status': cohort.status,
                'error_messages': cohort.error_messages,
                'logins': cohort.logins,
                'course_id': self.course.id,
                'is_active': cohort.is_active,
                'created': serializers.DateTimeField().to_representation(response.data['created']),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)
        self.create_request(url=self.get_url(), data=data, expected=expected, num_queries=11)


class LabCohortDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:cohort-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()
        self.cohort = CohortFactory(course=self.course, logins=[self.user.username])

    def test_url(self):
        self.assertURLNameEqual('cohorts/{}/', args=(self.cohort.id,), base_url=settings.LABAPI_BASE_URL)

    def test_detail(self):
        expected = {
            'id': self.cohort.id,
            'name': self.cohort.name,
            'status': self.cohort.status,
            'error_messages': self.cohort.error_messages,
            'logins': self.cohort.logins,
            'course_id': self.course.id,
            'is_active': self.cohort.is_active,
            'created': serializers.DateTimeField().to_representation(self.cohort.created),
            'modified': serializers.DateTimeField().to_representation(self.cohort.modified),
        }

        assign_perm('courses.view_course', self.user, self.course)
        self.client.force_login(user=self.user)
        self.detail_request(url=self.get_url(self.cohort.id), expected=expected, num_queries=8)

    def test_update(self):
        new_cohort = CohortFactory.build(logins=[fake.word()])
        data = {
            'name': new_cohort.name,
            'logins': new_cohort.logins,
            'is_active': new_cohort.is_active,
        }

        def expected(response):
            return {
                'id': self.cohort.id,
                'name': data['name'],
                'status': self.cohort.status,
                'error_messages': self.cohort.error_messages,
                'logins': data['logins'],
                'course_id': self.course.id,
                'is_active': data['is_active'],
                'created': serializers.DateTimeField().to_representation(response.data['created']),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)
        self.update_request(url=self.get_url(self.cohort.id), data=data, expected=expected, num_queries=10)

    def test_delete(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)
        self.delete_request(url=self.get_url(self.cohort.id), num_queries=11)
        self.assertIsNone(Cohort.objects.filter(id=self.cohort.id).first())


class LabCourseStudentListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:course-student-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()
        self.course_students = CourseStudentFactory.create_batch(5, course=self.course)

    def test_url(self):
        self.assertURLNameEqual('courses/{}/students/', args=(self.course.id,), base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(8):
            response = self.client.get(self.get_url(self.course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.other_students = CourseStudentFactory.create_batch(5)
        expected = [
            {
                'id': course_student.id,
                'course_id': course_student.course_id,
                'group_id': course_student.group_id,
                'status': course_student.status,
                'is_passed': course_student.is_passed,
                'user': {
                    'id': course_student.user.id,
                    'username': course_student.user.username,
                    'first_name': course_student.user.first_name,
                    'last_name': course_student.user.last_name,
                },
                'created': serializers.DateTimeField().to_representation(course_student.created),
                'modified': serializers.DateTimeField().to_representation(course_student.modified),
            }
            for course_student
            in sorted(self.course_students, key=lambda course_student: course_student.created, reverse=True)
        ]

        assign_perm('courses.view_course', self.user, self.course)
        self.client.force_login(user=self.user)
        self.list_request(url=self.get_url(self.course.id), expected=expected, num_queries=10)


class LabCourseCalendarTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:course-calendar'

    def setUp(self) -> None:
        self.user = LabUserFactory()
        self.author_course = CourseFactory(author=self.user)
        self.not_permitted_course = CourseFactory()
        self.layer = CalendarLayerFactory(course=self.author_course)
        self.events = CalendarEventFactory.create_batch(size=3, course=self.author_course)

    def test_url(self):
        self.assertURLNameEqual(
            'courses/{}/calendar/',
            args=(self.author_course.id,),
            base_url=settings.LABAPI_BASE_URL
        )

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            url = self.get_url(self.author_course.id)
            response = self.client.get(url, format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_get_forbidden(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(8):
            url = self.get_url(self.not_permitted_course.id)
            response = self.client.get(url, format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_retrieve(self):
        self.client.force_login(user=self.user)

        expected = {
            "id": self.author_course.id,
            "calendar_layer_id": self.layer.id,
            "timeslots": [{
                "id": event.timeslot_id,
                "calendar_event_id": event.id,
                "begin_date": serializers.DateTimeField().to_representation(event.timeslot.begin_date),
                "end_date": serializers.DateTimeField().to_representation(event.timeslot.end_date),
            } for event in self.events]
        }

        url = self.get_url(self.author_course.id)
        self.detail_request(url=url, expected=expected, num_queries=8)


class LabCourseBlockListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:course-block-list'

    def build_expected(self, course_blocks):
        return [{
            'id': b.id,
            'name': b.name,
            'summary': b.summary,
            'is_active': b.is_active,
            'created': serializers.DateTimeField().to_representation(b.created),
            'modified': serializers.DateTimeField().to_representation(b.modified),
        } for b in course_blocks]

    def test_url(self):
        self.assertURLNameEqual('courses/{}/blocks/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_non_auth(self):
        user = LabUserFactory()
        course = CourseFactory(author=user, is_active=True)
        response = self.client.get(self.get_url(course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        user = LabUserFactory(is_staff=True)
        self.client.force_login(user=user)
        course = CourseFactory(author=user, is_active=True)
        course_blocks = CourseBlockFactory.create_batch(9, course=course)
        CourseBlockFactory.create_batch(9)
        expected = self.build_expected(course_blocks)
        url = self.get_url(course.id)
        self.list_request(url, expected, num_queries=6)


class LabCourseBlockCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:course-block-create'

    def test_url(self):
        self.assertURLNameEqual('course_blocks/', base_url=settings.LABAPI_BASE_URL)

    def test_post_non_auth(self):
        response = self.client.post(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_create(self):
        user = LabUserFactory()
        course = CourseFactory(author=user, is_active=True)
        course_block = CourseBlockFactory.build(course=course)

        data = {
            'course_id': course.id,
            'name': course_block.name,
        }

        def expected(response):
            return {
                'id': response.data['id'],
                'name': course_block.name,
                'summary': '',
                'is_active': True,
                'created': serializers.DateTimeField().to_representation(response.data['created']),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        self.client.force_login(user=user)
        self.create_request(url=self.get_url(), data=data, expected=expected, num_queries=7)

    def test_create_with_all_fields(self):
        user = LabUserFactory()
        course = CourseFactory(author=user, is_active=True)
        course_block = CourseBlockFactory.build(course=course, is_active=False)

        data = {
            'course_id': course.id,
            'name': course_block.name,
            'summary': course_block.summary,
            'is_active': course_block.is_active,
        }

        def expected(response):
            return {
                'id': response.data['id'],
                'name': course_block.name,
                'summary': course_block.summary,
                'is_active': course_block.is_active,
                'created': serializers.DateTimeField().to_representation(response.data['created']),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        self.client.force_login(user=user)
        self.create_request(url=self.get_url(), data=data, expected=expected, num_queries=7)


class LabCourseBlockDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:course-block-detail'

    def test_url(self):
        self.assertURLNameEqual('course_blocks/{}/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_get_non_auth(self):
        user = LabUserFactory()
        course = CourseFactory(author=user, is_active=True)
        course_block = CourseBlockFactory(course=course)
        response = self.client.get(self.get_url(course_block.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_retrieve(self):
        user = LabUserFactory(is_staff=True)
        self.client.force_login(user=user)
        course = CourseFactory(author=user, is_active=True)
        course_blocks = CourseBlockFactory.create_batch(9, course=course)

        def build_expected(course_block):
            return {
                'id': course_block.id,
                'name': course_block.name,
                'summary': course_block.summary,
                'is_active': course_block.is_active,
                'created': serializers.DateTimeField().to_representation(course_block.created),
                'modified': serializers.DateTimeField().to_representation(course_block.modified),
            }

        CourseBlockFactory.create_batch(9)
        expected = build_expected(course_blocks[3])
        url = self.get_url(course_blocks[3].id)
        self.detail_request(url=url, expected=expected, num_queries=5)

    def test_put_non_auth(self):
        user = LabUserFactory()
        course = CourseFactory(author=user, is_active=True)
        course_block = CourseBlockFactory(course=course)
        response = self.client.put(self.get_url(course_block.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_update(self):
        user = LabUserFactory()
        course = CourseFactory(author=user, is_active=True)
        course_block = CourseBlockFactory(course=course, is_active=True)

        data = {
            'name': fake.sentence(nb_words=5),
            'summary': fake.sentence(nb_words=11),
            'is_active': False,
        }

        def expected(response):
            return {
                'id': course_block.id,
                'name': data['name'],
                'summary': data['summary'],
                'is_active': data['is_active'],
                'created': serializers.DateTimeField().to_representation(course_block.created),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        url = self.get_url(course_block.id)
        self.client.force_login(user=user)
        self.update_request(url=url, data=data, expected=expected, num_queries=6)

    def test_patch_non_auth(self):
        user = LabUserFactory()
        course = CourseFactory(author=user, is_active=True)
        course_block = CourseBlockFactory(course=course)
        response = self.client.patch(self.get_url(course_block.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_partial_update(self):
        user = LabUserFactory()
        course = CourseFactory(author=user, is_active=True)
        course_block = CourseBlockFactory(course=course)

        data = {
            'summary': fake.sentence(nb_words=11),
        }

        def expected(response):
            return {
                'id': course_block.id,
                'name': course_block.name,
                'summary': data['summary'],
                'is_active': course_block.is_active,
                'created': serializers.DateTimeField().to_representation(course_block.created),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        url = self.get_url(course_block.id)
        self.client.force_login(user=user)
        self.update_request(url=url, data=data, expected=expected, method='patch', num_queries=6)

    def test_delete_non_auth(self):
        user = LabUserFactory()
        course = CourseFactory(author=user, is_active=True)
        course_block = CourseBlockFactory(course=course)
        response = self.client.delete(self.get_url(course_block.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_delete(self):
        user = LabUserFactory()
        course = CourseFactory(author=user, is_active=True)
        course_blocks = CourseBlockFactory.create_batch(9, course=course)
        self.client.force_login(user=user)
        url = self.get_url(course_blocks[3].id)

        self.delete_request(url=url, num_queries=9)

        self.assertIsNone(CourseBlock.objects.filter(id=course_blocks[3].id).first())


class CourseModuleDetailTestSerializer(serializers.ModelSerializer):
    block_id = serializers.PrimaryKeyRelatedField(
        source='block', queryset=CourseBlock.objects.all(), required=True,
    )
    course_id = serializers.PrimaryKeyRelatedField(
        source='course', queryset=Course.objects.all(), required=True,
    )

    class Meta:
        model = CourseModule
        fields = (
            'id', 'name', 'course_id', 'block_id', 'description', 'is_active',
        )
        read_only_fields = fields


class CourseModuleLabTestViewSet(CourseModuleLabViewSet):
    serializer_class = CourseModuleDetailTestSerializer
    serializer_classes = {
        'retrieve': CourseModuleDetailTestSerializer,
        'create': CourseModuleCreateBaseSerializer,
        'update': CourseModuleUpdateBaseSerializer,
        'partial_update': CourseModuleUpdateBaseSerializer,
    }
    queryset = FakeCourseModule.objects.select_related('module_type')

    module_type = None

    def __init__(self, *args, **kwargs):
        self.module_type = kwargs.pop('module_type', None)
        super().__init__(*args, **kwargs)

    def perform_create(self, serializer):
        serializer.save(module_type=self.module_type)


class LabCourseModuleCreateTestCase(UrlNameMixin, APITestCase):
    def setUp(self):
        self.user = LabUserFactory()
        self.courses = CourseFactory.create_batch(9, is_active=True, author=self.user)
        self.course = self.courses[0]
        self.another_course = self.courses[1]

        self.module_type = ModuleTypeFactory(app_label="dummies", model="dummy")

        CourseModuleFactory.create_batch(5, course=self.course, module_type=self.module_type)
        self.course_module = CourseModuleFactory.build(course=self.course, module_type=self.module_type)
        self.course_blocks = CourseBlockFactory.create_batch(3, course=self.course)
        self.course_block = self.course_blocks[1]

        self.view = CourseModuleLabTestViewSet.as_view({'post': 'create'}, module_type=self.module_type)

    def test_create_required_fields(self):
        request_payload = {
            'course_id': self.course.id,
            'name': self.course_module.name,
        }

        request = APIRequestFactory().post('/fake_course_modules_url/', data=request_payload, format='json')
        force_authenticate(request, user=self.user)

        with self.assertNumQueries(7):
            response = self.view(request=request)
            response.render()  # Cannot access `response.content` without this.
            response_data = response.data

        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response_data)
        expected = {
            'id': response_data['id'],
            'course_id': self.course.id,
            'name': self.course_module.name,
            'block_id': None,
            'description': '',
            'is_active': True,
        }
        self.assertEqual(response_data, expected, response_data)

    def test_create_all_fields(self):
        request_payload = {
            'course_id': self.course.id,
            'name': self.course_module.name,
            'block_id': self.course_block.id,
            'description': self.course_module.description,
            'is_active': False,
        }

        request = APIRequestFactory().post('/fake_course_modules_url/', data=request_payload, format='json')
        force_authenticate(request, user=self.user)

        with self.assertNumQueries(10):
            response = self.view(request=request)
            response.render()  # Cannot access `response.content` without this.
            response_data = response.data

        self.assertEqual(response.status_code, status.HTTP_201_CREATED, response_data)
        expected = {
            'id': response_data['id'],
            'course_id': self.course.id,
            'name': self.course_module.name,
            'block_id': self.course_block.id,
            'description': self.course_module.description,
            'is_active': False,
        }
        self.assertEqual(response_data, expected, response_data)

    def test_create_with_invalid_block_id(self):
        request_payload = {
            'course_id': self.another_course.id,
            'name': self.course_module.name,
            'block_id': self.course_block.id,
        }

        request = APIRequestFactory().post('/fake_course_modules_url/', data=request_payload, format='json')
        force_authenticate(request, user=self.user)

        with self.assertNumQueries(8):
            response = self.view(request=request)
            response.render()
            self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class LabCourseModuleUpdateTestCase(UrlNameMixin, APITestCase):
    def setUp(self):
        self.user = LabUserFactory()
        self.courses = CourseFactory.create_batch(9, is_active=True, author=self.user)
        self.course = self.courses[4]
        self.another_course = self.courses[5]

        self.module_type = ModuleTypeFactory(app_label="dummies", model="dummy")

        CourseModuleFactory.create_batch(5, course=self.course, module_type=self.module_type)
        self.course_blocks = CourseBlockFactory.create_batch(10, course=self.course)
        self.course_block = self.course_blocks[1]
        self.last_block = self.course_blocks[-1]
        self.course_modules = CourseModuleFactory.create_batch(
            7, course=self.course, module_type=self.module_type, block=self.course_block
        )
        self.course_module = self.course_modules[4]
        self.module_without_block = CourseModuleFactory(course=self.course, module_type=self.module_type)
        self.another_course_block = CourseBlockFactory(course=self.another_course)

        self.view = CourseModuleLabTestViewSet.as_view({'patch': 'partial_update'}, module_type=self.module_type)
        self.update_view = CourseModuleLabTestViewSet.as_view({'put': 'update'}, module_type=self.module_type)

    def test_partial_update_to_invalid_block_id(self):
        request_payload = {
            'block_id': self.another_course_block.id,
        }

        request = APIRequestFactory().patch('/fake_course_modules_url/', data=request_payload, format='json')
        force_authenticate(request, user=self.user)

        with self.assertNumQueries(9):
            response = self.view(request=request, pk=self.course_module.id)
            response.render()
            self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_partial_update_module_block(self):
        request_payload = {
            'block_id': self.last_block.id,
        }

        request = APIRequestFactory().patch('/fake_course_modules_url/', data=request_payload, format='json')
        force_authenticate(request, user=self.user)

        with self.assertNumQueries(10):
            response = self.view(request=request, pk=self.course_module.id)
            response.render()  # Cannot access `response.content` without this.
            response_data = response.data

        self.assertEqual(response.status_code, status.HTTP_200_OK, response_data)
        expected = {
            'id': self.course_module.id,
            'course_id': self.course.id,
            'name': self.course_module.name,
            'block_id': self.last_block.id,
            'description': self.course_module.description,
            'is_active': self.course_module.is_active,
        }
        self.assertEqual(response_data, expected, response_data)

    def test_add_block_to_module(self):
        request_payload = {
            'block_id': self.course_block.id,
        }

        request = APIRequestFactory().patch('/fake_course_modules_url/', data=request_payload, format='json')
        force_authenticate(request, user=self.user)

        with self.assertNumQueries(10):
            response = self.view(request=request, pk=self.module_without_block.id)
            response.render()  # Cannot access `response.content` without this.
            response_data = response.data

        self.assertEqual(response.status_code, status.HTTP_200_OK, response_data)
        expected = {
            'id': self.module_without_block.id,
            'course_id': self.course.id,
            'name': self.module_without_block.name,
            'block_id': self.course_block.id,
            'description': self.module_without_block.description,
            'is_active': self.module_without_block.is_active,
        }
        self.assertEqual(response_data, expected, response_data)

    def test_remove_block_from_module(self):
        request_payload = {
            'block_id': None,
        }

        request = APIRequestFactory().patch('/fake_course_modules_url/', data=request_payload, format='json')
        force_authenticate(request, user=self.user)

        with self.assertNumQueries(6):
            response = self.view(request=request, pk=self.course_module.id)
            response.render()  # Cannot access `response.content` without this.
            response_data = response.data

        self.assertEqual(response.status_code, status.HTTP_200_OK, response_data)
        expected = {
            'id': self.course_module.id,
            'course_id': self.course.id,
            'name': self.course_module.name,
            'block_id': None,
            'description': self.course_module.description,
            'is_active': self.course_module.is_active,
        }
        self.assertEqual(response_data, expected, response_data)

    def test_partial_update_module_and_leave_block_unchanged(self):
        request_payload = {
            'name': self.module_without_block.name,
        }

        request = APIRequestFactory().patch('/fake_course_modules_url/', data=request_payload, format='json')
        force_authenticate(request, user=self.user)

        with self.assertNumQueries(10):
            response = self.view(request=request, pk=self.course_module.id)
            response.render()  # Cannot access `response.content` without this.
            response_data = response.data

        self.assertEqual(response.status_code, status.HTTP_200_OK, response_data)
        expected = {
            'id': self.course_module.id,
            'course_id': self.course.id,
            'name': self.module_without_block.name,
            'block_id': self.course_block.id,
            'description': self.course_module.description,
            'is_active': self.course_module.is_active,
        }
        self.assertEqual(response_data, expected, response_data)

    def test_update_module_required_fields(self):
        request_payload = {
            'name': self.module_without_block.name,
        }

        request = APIRequestFactory().put('/fake_course_modules_url/', data=request_payload, format='json')
        force_authenticate(request, user=self.user)

        with self.assertNumQueries(10):
            response = self.update_view(request=request, pk=self.course_module.id)
            response.render()  # Cannot access `response.content` without this.
            response_data = response.data

        self.assertEqual(response.status_code, status.HTTP_200_OK, response_data)
        expected = {
            'id': self.course_module.id,
            'course_id': self.course.id,
            'name': self.module_without_block.name,
            'block_id': self.course_block.id,
            'description': self.course_module.description,
            'is_active': self.course_module.is_active,
        }
        self.assertEqual(response_data, expected, response_data)

    def test_update_module_all_fields(self):
        request_payload = {
            'block_id': None,
            'name': self.module_without_block.name,
            'description': self.module_without_block.description,
            'is_active': False,
        }

        request = APIRequestFactory().put('/fake_course_modules_url/', data=request_payload, format='json')
        force_authenticate(request, user=self.user)

        with self.assertNumQueries(6):
            response = self.update_view(request=request, pk=self.course_module.id)
            response.render()  # Cannot access `response.content` without this.
            response_data = response.data

        self.assertEqual(response.status_code, status.HTTP_200_OK, response_data)
        expected = {
            'id': self.course_module.id,
            'course_id': self.course.id,
            'name': self.module_without_block.name,
            'block_id': None,
            'description': self.module_without_block.description,
            'is_active': False,
        }
        self.assertEqual(response_data, expected, response_data)


class LinkedCourseLabViewSetCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:linked-course-create'

    def setUp(self):
        ModuleType.objects.get_for_model(LinkedCourse)
        self.course = CourseFactory(course_type=Course.TypeChoices.TRACK.value, is_active=True)
        self.linked_course = CourseFactory(course_type=Course.TypeChoices.COURSE.value, is_active=True)
        self.user = LabUserFactory(is_staff=True)

    def build_payload(self, linked_course_module: LinkedCourse):
        return {
            "course_id": linked_course_module.course.id,
            "linked_course_id": linked_course_module.linked_course.id,
            "name": linked_course_module.name,
            "description": linked_course_module.description,
            "is_active": linked_course_module.is_active,
            "estimated_time": linked_course_module.estimated_time,
        }

    def test_url(self):
        self.assertURLNameEqual('linkedcourse_modules/', args=(), base_url=settings.LABAPI_BASE_URL)

    def test_no_course_permissions(self):
        assign_perm('courses.view_course', self.user, self.linked_course)
        self.client.force_login(user=self.user)
        linked_course_module = LinkedCourseFactory.build(course=self.course, linked_course=self.linked_course)
        self.create_request(
            self.get_url(),
            data=self.build_payload(linked_course_module),
            status_code=status.HTTP_403_FORBIDDEN, num_queries=8
        )

    def test_no_linked_course_permissions(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)
        linked_course_module = LinkedCourseFactory.build(course=self.course, linked_course=self.linked_course)
        self.create_request(
            self.get_url(),
            data=self.build_payload(linked_course_module),
            status_code=status.HTTP_400_BAD_REQUEST, num_queries=12
        )

    def test_create(self):
        assign_perm('courses.change_course', self.user, self.course)
        assign_perm('courses.view_course', self.user, self.linked_course)
        self.client.force_login(user=self.user)

        linked_course_module = LinkedCourseFactory.build(course=self.course, linked_course=self.linked_course)

        def expected(response):
            return {
                'id': response.data['id'],
                'block_id': None,
                'linked_course': {
                    'id': linked_course_module.linked_course.id,
                    'slug': linked_course_module.linked_course.slug,
                    'name': linked_course_module.linked_course.name,
                    'shortname': linked_course_module.linked_course.shortname,
                },
                'name': linked_course_module.name,
                'description': linked_course_module.description,
                'is_active': linked_course_module.is_active,
                'estimated_time': linked_course_module.estimated_time,
                'created': serializers.DateTimeField().to_representation(response.data['created']),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        self.create_request(
            self.get_url(),
            data=self.build_payload(linked_course_module),
            expected=expected,
            num_queries=17
        )


class LinkedCourseLabViewSetDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:linked-course-detail'

    def setUp(self):
        ModuleType.objects.get_for_model(LinkedCourse)
        self.course = CourseFactory(course_type=Course.TypeChoices.TRACK.value, is_active=True)
        self.linked_course = CourseFactory(course_type=Course.TypeChoices.COURSE.value, is_active=True)
        self.linked_course_module = LinkedCourseFactory(course=self.course, linked_course=self.linked_course)
        self.user = LabUserFactory(is_staff=True)

    def build_payload(self, linked_course_module: LinkedCourse):
        return {
            "linked_course_id": linked_course_module.linked_course.id,
            "name": linked_course_module.name,
            "description": linked_course_module.description,
            "is_active": linked_course_module.is_active,
            "estimated_time": linked_course_module.estimated_time,
        }

    def build_expected(self, linked_course_module: LinkedCourse, response):
        return {
            'id': linked_course_module.id,
            'block_id': None,
            'linked_course': {
                'id': linked_course_module.linked_course.id,
                'slug': linked_course_module.linked_course.slug,
                'name': linked_course_module.linked_course.name,
                'shortname': linked_course_module.linked_course.shortname,
            },
            'name': linked_course_module.name,
            'description': linked_course_module.description,
            'is_active': linked_course_module.is_active,
            'estimated_time': linked_course_module.estimated_time,
            'created': serializers.DateTimeField().to_representation(self.linked_course_module.created),
            'modified': serializers.DateTimeField().to_representation(response.data['modified']),
        }

    def test_url(self):
        self.assertURLNameEqual(
            'linkedcourse_modules/{}/',
            args=(self.linked_course_module.id,),
            base_url=settings.LABAPI_BASE_URL
        )

    def test_permission_denied(self):
        self.client.force_login(user=self.user)
        self.detail_request(
            self.get_url(self.linked_course_module.id),
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=8
        )

    def test_detail(self):
        assign_perm('courses.view_course', self.user, self.course)
        self.client.force_login(user=self.user)

        def expected(response):
            result = self.build_expected(self.linked_course_module, response)
            result['modified'] = serializers.DateTimeField().to_representation(self.linked_course_module.modified)
            return result

        self.detail_request(
            self.get_url(self.linked_course_module.id),
            expected=expected,
            num_queries=8
        )

    def test_partial_update_no_linked_course(self):
        new_linked_course_module = LinkedCourseFactory.build(course=self.course, linked_course=self.linked_course)
        request_payload = self.build_payload(new_linked_course_module)
        request_payload.pop('linked_course_id')

        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        def expected(response):
            result = self.build_expected(new_linked_course_module, response)
            result['id'] = self.linked_course_module.id
            return result

        self.partial_update_request(
            self.get_url(self.linked_course_module.id),
            data=request_payload,
            expected=expected,
            num_queries=14
        )

    def test_update_no_linked_course_permissions(self):
        new_linked_course = CourseFactory(course_type=Course.TypeChoices.COURSE.value, is_active=True)
        new_linked_course_module = LinkedCourseFactory.build(course=self.course, linked_course=new_linked_course)
        request_payload = self.build_payload(new_linked_course_module)

        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        self.update_request(
            self.get_url(self.linked_course_module.id),
            data=request_payload,
            status_code=status.HTTP_400_BAD_REQUEST,
            num_queries=11
        )

    def test_update(self):
        new_linked_course = CourseFactory(course_type=Course.TypeChoices.COURSE.value, is_active=True)
        new_linked_course_module = LinkedCourseFactory.build(course=self.course, linked_course=new_linked_course)
        request_payload = self.build_payload(new_linked_course_module)

        assign_perm('courses.view_course', self.user, new_linked_course)
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        def expected(response):
            result = self.build_expected(new_linked_course_module, response)
            result['id'] = self.linked_course_module.id
            return result

        self.update_request(
            self.get_url(self.linked_course_module.id),
            data=request_payload,
            expected=expected,
            num_queries=17
        )

    def test_delete(self):
        user = LabUserFactory(is_staff=True)
        assign_perm('courses.change_course', user, self.course)
        self.client.force_login(user=user)

        self.delete_request(
            self.get_url(self.linked_course_module.id),
            num_queries=12
        )


class LabCourseProgressListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:course-progress-list'

    def setUp(self):
        self.user = LabUserFactory()
        self.author = LabUserFactory()
        self.other_courses = CourseFactory.create_batch(15, is_active=True, author=self.author)
        for other_course in self.other_courses:
            StudentModuleProgressFactory(course=other_course)
        self.course = CourseFactory(is_active=True, author=self.author)
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

    def test_url(self):
        self.assertURLNameEqual('courses/{}/progress/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_non_auth(self):
        url = self.get_url(self.course.id)
        with self.assertNumQueries(0):
            response = self.client.get(url, format='json')
            self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_no_permissions(self):
        self.client.force_login(user=self.user)
        url = self.get_url(self.course.id)
        with self.assertNumQueries(8):
            response = self.client.get(url, format='json')
            self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_no_students(self):
        self.client.force_login(user=self.user)
        course = CourseFactory(is_active=True, author=self.author)
        assign_perm('courses.view_course', self.user, course)
        url = self.get_url(course.id)
        self.list_request(url, expected=[], num_queries=9, check_ids=False)

    def test_no_progress(self):
        self.client.force_login(user=self.user)
        course = CourseFactory(is_active=True, author=self.author)
        student = CourseStudentFactory(course=course)
        assign_perm('courses.view_course', self.user, course)
        url = self.get_url(course.id)

        expected = [{
            'student': {
                'id': student.id,
                'status': student.status,
                'is_passed': student.is_passed,
                'completion_date': serializers.DateTimeField().to_representation(student.completion_date),
                'created': serializers.DateTimeField().to_representation(student.created),
                'user': {
                    'username': student.user.username,
                    'full_name': student.user.get_full_name()
                },
            },
            'modules': {},
            'score': 0,
        }]

        self.list_request(url, expected=expected, num_queries=12, check_ids=False)

    def build_expected_modules(self, module_progress_list):
        return {str(module_progress.module_id): module_progress.score for module_progress in module_progress_list}

    def calculate_expected_score(self, course_progress_list):
        return 0 if not course_progress_list else course_progress_list[0].score

    def build_expected(self, students):
        return [{
            'score': self.calculate_expected_score(list(student.course_progresses.all())),
            'modules': self.build_expected_modules(list(student.module_progresses.all())),
            'student': {
                'id': student.id,
                'status': student.status,
                'is_passed': student.is_passed,
                'completion_date': serializers.DateTimeField().to_representation(student.completion_date),
                'created': serializers.DateTimeField().to_representation(student.created),
                'user': {
                    'username': student.user.username,
                    'full_name': student.user.get_full_name()
                },
            },
        } for student in students]

    def test_list(self):
        self.client.force_login(user=self.author)
        url = self.get_url(self.course.id)

        expected = self.build_expected(self.students[:10])

        self.list_request(url, expected=expected, num_queries=10, check_ids=False)


class LabCourseProgressRecalculateTestCase(UrlNameMixin, GenericRequestMixin, APITransactionTestCase):
    URL_NAME = 'labapi:course-progress-recalculate'

    def setUp(self):
        self.user = LabUserFactory()
        self.author = LabUserFactory()
        self.course = CourseFactory(is_active=True, author=self.author)
        # используется модуль Assignment, потому как у него есть все сигналы, связанные с прогрессом
        self.modules = AssignmentFactory.create_batch(2, course=self.course, weight=1)
        self.students = CourseStudentFactory.create_batch(5, course=self.course)
        for student in self.students:
            StudentModuleProgressFactory(
                course=self.course,
                student=student,
                module=self.modules[0],
                score=100
            )

    def test_url(self):
        self.assertURLNameEqual(
            url='courses/{pk}/progress/recalculate/', kwargs={'pk': 1}, base_url=settings.LABAPI_BASE_URL
        )

    def test_non_auth(self):
        url = self.get_url(self.course.id)
        with self.assertNumQueries(0):
            response = self.client.put(url, format='json')
            self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_no_permissions(self):
        self.client.force_login(user=self.user)
        url = self.get_url(self.course.id)
        with self.assertNumQueries(9):
            response = self.client.put(url, format='json')
            self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_progress_recalculation(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.change_course', self.user, self.course)
        url = self.get_url(self.course.id)
        AssignmentFactory.create_batch(3, course=self.course, weight=1)
        with self.assertNumQueries(39):
            response = self.client.put(url, format='json')
            self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        for student in self.students:
            progress = StudentCourseProgress.objects.filter(student=student, course=self.course)
            self.assertEqual(progress.count(), 1)
            self.assertEqual(progress.first().score, 20)


class LabCourseBlockMovesTestCase(UrlNameMixin, APITestCase):
    URL_NAME_FORMAT = "labapi:course-block-{}"
    BASE_URL = settings.LABAPI_BASE_URL

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        CourseFactory.create_batch(5)
        self.course = CourseFactory(is_active=True)
        self.course_blocks = CourseBlockFactory.create_batch(10, course=self.course)

    def get_action_url(self, action: str, *args, **kwargs):
        action = action.replace('_', '-')
        url_name = self.URL_NAME_FORMAT.format(action)
        return reverse(url_name, args=args, kwargs=kwargs)

    def assertActionURLEqual(self, action: str):  # noqa N802
        url = f"/{self.BASE_URL}course_blocks/1/{action}/"
        expected = self.get_action_url(action, 1)
        self.assertURLEqual(expected, url)

    def test_action_urls(self):
        self.assertActionURLEqual('move_above')
        self.assertActionURLEqual('move_below')

    def test_move_above(self):
        self.client.force_login(user=self.user)

        last_block: CourseBlock = self.course_blocks[-1]
        second_block: CourseBlock = self.course_blocks[1]

        url = self.get_action_url('move_above', last_block.id)

        payload = {
            "block_id": second_block.id,
        }

        with self.assertNumQueries(10):
            response: Union[Response, HttpResponse] = self.client.put(url, data=payload, format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT, msg=response.data)

        last_block.refresh_from_db()
        second_block.refresh_from_db()
        self.assertLess(last_block.order, second_block.order, msg="last_block должен быть выше second_block")

    def test_move_below(self):
        self.client.force_login(user=self.user)

        first_block: CourseBlock = self.course_blocks[0]
        third_block: CourseBlock = self.course_blocks[2]

        url = self.get_action_url('move_below', first_block.id)

        payload = {
            "block_id": third_block.id,
        }

        with self.assertNumQueries(10):
            response: Union[Response, HttpResponse] = self.client.put(url, data=payload, format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT, msg=response.data)

        first_block.refresh_from_db()
        third_block.refresh_from_db()
        self.assertGreater(first_block.order, third_block.order, msg="first_block должен быть ниже third_block")
