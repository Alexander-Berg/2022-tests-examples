from datetime import datetime, timedelta
from unittest.mock import MagicMock, patch

from django.conf import settings
from django.db.models import Max
from django.utils import timezone
from django.utils.timezone import utc

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.enrollments.models import EnrolledUser
from lms.enrollments.tests.factories import EnrolledUserFactory
from lms.staff.tests.factories import StaffCityFactory, StaffOfficeFactory, UserWithStaffProfileFactory
from lms.users.tests.factories import UserFactory

from ..models import Course
from .factories import (
    CourseCategoryFactory, CourseFactory, CourseGroupFactory, CourseModuleFactory, CourseStudentFactory,
    CourseVisibilityFactory, ModuleTypeFactory, StudentModuleProgressFactory,
)
from .mixins import CourseListChoiceFilterMixin


class CourseAvailableForEnrollFilterTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def build_expected(self, courses):
        return [
            {
                'id': c.id,
                'slug': c.slug,
                'name': c.name,
                'shortname': c.shortname,
                'summary': c.summary,
                'image_url': c.image_url,
                'city_id': c.city_id,
                'study_mode_id': c.study_mode_id,
                'structure': c.structure,
                'format': c.format,
                'author': {
                    'id': c.author.id,
                    'username': c.author.username,
                    'first_name': c.author.first_name,
                    'last_name': c.author.last_name,
                },
                'categories': [cc.id for cc in c.categories.all()],
                'available_for_enroll': c.available_for_enroll,
                'is_full': c.is_full,
                'begin_date': serializers.DateTimeField().to_representation(c.begin_date),
                'end_date': serializers.DateTimeField().to_representation(c.end_date),
                'enroll_begin': serializers.DateTimeField().to_representation(c.enroll_begin),
                'enroll_end': serializers.DateTimeField().to_representation(c.enroll_end),
                'payment_method': c.payment_method,
                'paid_percent': c.paid_percent,
                'payment_terms': c.payment_terms,
                'price': c.price,
                'num_hours': c.num_hours,
                'enable_groups': c.enable_groups,
                'status': c.status,
                'type': c.course_type,
                'tags': list(c.tags.values_list('name', flat=True)),
                'created': serializers.DateTimeField().to_representation(c.created),
                'modified': serializers.DateTimeField().to_representation(c.modified),
            } for c in sorted(courses, key=lambda x: x.name)
        ]

    def test_list(self):
        user = UserFactory()
        now_moment = datetime(year=2020, month=11, day=10, tzinfo=utc)
        mocked_now = MagicMock()
        mocked_now.return_value = now_moment
        mocked_now.replace.return_value = now_moment
        with patch('lms.courses.querysets.timezone.now', new=mocked_now):
            courses_begin_none_end_none = CourseFactory.create_batch(
                3, is_active=True, is_archive=False, enroll_begin=None, enroll_end=None,
            )
            courses_end_none = CourseFactory.create_batch(
                3, is_active=True, is_archive=False, enroll_begin=now_moment - timedelta(days=1), enroll_end=None,
            )
            courses_begin_none = CourseFactory.create_batch(
                3, is_active=True, is_archive=False, enroll_begin=None, enroll_end=now_moment + timedelta(days=1),
            )
            courses_begin_end = CourseFactory.create_batch(
                3, is_active=True, is_archive=False,
                enroll_begin=now_moment - timedelta(days=1), enroll_end=now_moment + timedelta(days=1),
            )
            available_courses = courses_begin_none_end_none + courses_end_none + courses_begin_none + courses_begin_end

            courses_group_not_full = [
                courses_begin_none_end_none[0], courses_end_none[0], courses_begin_none[0], courses_begin_end[0],
            ]
            for course_group_not_full in courses_group_not_full:
                occupancy = course_group_not_full.occupancy
                occupancy.maximum = 3
                occupancy.save()
                EnrolledUserFactory(course=course_group_not_full, status=EnrolledUser.StatusChoices.ENROLLED)
                EnrolledUserFactory(course=course_group_not_full, status=EnrolledUser.StatusChoices.PENDING)

            courses_group_unlimited = [
                courses_begin_none_end_none[1], courses_end_none[1], courses_begin_none[1], courses_begin_end[1],
            ]
            for course_group_unlimited in courses_group_unlimited:
                occupancy = course_group_unlimited.occupancy
                occupancy.maximum = 0
                occupancy.save()
                EnrolledUserFactory(course=course_group_unlimited, status=EnrolledUser.StatusChoices.ENROLLED)
                EnrolledUserFactory(course=course_group_unlimited, status=EnrolledUser.StatusChoices.PENDING)

            courses_group_no_occupancy = [
                courses_begin_none_end_none[2], courses_end_none[2], courses_begin_none[2], courses_begin_end[2],
            ]
            for course_group_no_occupancy in courses_group_no_occupancy:
                occupancy = course_group_no_occupancy.occupancy
                occupancy.delete()
                course_group_no_occupancy.refresh_from_db()

            _ = [
                CourseFactory(
                    is_active=True, is_archive=False, enroll_begin=now_moment + timedelta(days=1), enroll_end=None,
                ),
                CourseFactory(
                    is_active=True, is_archive=False, enroll_begin=None, enroll_end=now_moment - timedelta(days=1),
                ),
                CourseFactory(
                    is_active=True, is_archive=False,
                    enroll_begin=now_moment + timedelta(days=1), enroll_end=now_moment + timedelta(days=2),
                ),
                CourseFactory(
                    is_active=True, is_archive=False,
                    enroll_begin=now_moment - timedelta(days=2), enroll_end=now_moment - timedelta(days=1),
                )
            ]
            unavailable_courses_by_occupancies = [
                CourseFactory(is_active=True, is_archive=False, enroll_begin=None, enroll_end=None),
                CourseFactory(
                    is_active=True, is_archive=False, enroll_begin=now_moment - timedelta(days=1), enroll_end=None,
                ),
                CourseFactory(
                    is_active=True, is_archive=False, enroll_begin=None, enroll_end=now_moment + timedelta(days=1),
                ),
                CourseFactory(
                    is_active=True, is_archive=False,
                    enroll_begin=now_moment - timedelta(days=1), enroll_end=now_moment + timedelta(days=1),
                )
            ]
            for course in unavailable_courses_by_occupancies:
                occupancy = course.occupancy
                occupancy.maximum = 2
                occupancy.save()
                EnrolledUserFactory(course=course, status=EnrolledUser.StatusChoices.ENROLLED)
                EnrolledUserFactory(course=course, status=EnrolledUser.StatusChoices.PENDING)
                course.refresh_from_db()

            self.client.force_login(user=user)

            with self.assertNumQueries(10):
                response = self.client.get(self.get_url() + "?available_for_enroll=true&limit=100", format='json')

            self.assertEqual(response.status_code, status.HTTP_200_OK)
            self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
            self.assertEqual(response.data.get('count'), len(available_courses))
            results = response.data.get('results')

            expected_ids = [c.id for c in sorted(available_courses, key=lambda x: x.name)]
            result_ids = [c['id'] for c in results]
            self.assertListEqual(result_ids, expected_ids)

            expected = self.build_expected(available_courses)

            self.assertEqual(results, expected)

    def test_list_enable_groups(self):
        user = UserFactory()
        now_moment = datetime(year=2020, month=11, day=10, tzinfo=utc)
        mocked_now = MagicMock()
        mocked_now.return_value = now_moment
        mocked_now.replace.return_value = now_moment
        with patch('lms.courses.querysets.timezone.now', new=mocked_now):
            courses_begin_none_end_none = CourseFactory.create_batch(
                6, is_active=True, is_archive=False, enroll_begin=None, enroll_end=None,
            )
            courses_end_none = CourseFactory.create_batch(
                6, is_active=True, is_archive=False, enroll_begin=now_moment - timedelta(days=2), enroll_end=None,
            )
            courses_begin_none = CourseFactory.create_batch(
                6, is_active=True, is_archive=False, enroll_begin=None, enroll_end=now_moment + timedelta(days=2),
            )
            courses_begin_end = CourseFactory.create_batch(
                6, is_active=True, is_archive=False,
                enroll_begin=now_moment - timedelta(days=2), enroll_end=now_moment + timedelta(days=2),
            )
            available_courses = courses_begin_none_end_none + courses_end_none + courses_begin_none + courses_begin_end

            courses_group_unlimited = [
                courses_begin_none_end_none[0], courses_end_none[0], courses_begin_none[0], courses_begin_end[0],
            ]
            for course_group_unlimited in courses_group_unlimited:
                available_group = CourseGroupFactory(course=course_group_unlimited, max_participants=0)
                EnrolledUserFactory(
                    course=course_group_unlimited, group=available_group, status=EnrolledUser.StatusChoices.ENROLLED,
                )
                EnrolledUserFactory(
                    course=course_group_unlimited, group=available_group, status=EnrolledUser.StatusChoices.PENDING,
                )

                unavailable_group = CourseGroupFactory(course=course_group_unlimited, max_participants=2)
                EnrolledUserFactory(
                    course=course_group_unlimited, group=unavailable_group, status=EnrolledUser.StatusChoices.ENROLLED,
                )
                EnrolledUserFactory(
                    course=course_group_unlimited, group=unavailable_group, status=EnrolledUser.StatusChoices.PENDING,
                )

            courses_group_limited = [
                courses_begin_none_end_none[1], courses_end_none[1], courses_begin_none[1], courses_begin_end[1],
            ]
            for course_group_limited in courses_group_limited:
                available_group = CourseGroupFactory(course=course_group_limited, max_participants=3)
                EnrolledUserFactory(
                    course=course_group_limited, group=available_group, status=EnrolledUser.StatusChoices.ENROLLED,
                )
                EnrolledUserFactory(
                    course=course_group_limited, group=available_group, status=EnrolledUser.StatusChoices.PENDING,
                )

                unavailable_group = CourseGroupFactory(course=course_group_limited, max_participants=2)
                EnrolledUserFactory(
                    course=course_group_limited, group=unavailable_group, status=EnrolledUser.StatusChoices.ENROLLED,
                )
                EnrolledUserFactory(
                    course=course_group_limited, group=unavailable_group, status=EnrolledUser.StatusChoices.PENDING,
                )

            courses_group_begin_end = [
                courses_begin_none_end_none[2], courses_end_none[2], courses_begin_none[2], courses_begin_end[2],
            ]
            for course_group_begin_end in courses_group_begin_end:
                CourseGroupFactory(
                    course=course_group_begin_end,
                    enroll_begin=now_moment - timedelta(days=1),
                    enroll_end=now_moment + timedelta(days=1),
                )
                CourseGroupFactory(
                    course=course_group_begin_end,
                    enroll_end=now_moment - timedelta(days=1)
                )

            courses_group_end_none = [
                courses_begin_none_end_none[3], courses_end_none[3], courses_begin_none[3], courses_begin_end[3],
            ]
            for course_group_end_none in courses_group_end_none:
                CourseGroupFactory(
                    course=course_group_end_none,
                    enroll_begin=now_moment - timedelta(days=1),
                )
                CourseGroupFactory(
                    course=course_group_end_none,
                    enroll_begin=now_moment + timedelta(days=1)
                )

            courses_group_begin_none = [
                courses_begin_none_end_none[4], courses_end_none[4], courses_begin_none[4], courses_begin_end[4],
            ]
            for course_group_begin_none in courses_group_begin_none:
                CourseGroupFactory(
                    course=course_group_begin_none,
                    enroll_end=now_moment + timedelta(days=1),
                )
                CourseGroupFactory(
                    course=course_group_begin_none,
                    enroll_end=now_moment - timedelta(days=1)
                )

            courses_group_begin = [
                courses_begin_none_end_none[5], courses_end_none[5], courses_begin_none[5], courses_begin_end[5],
            ]
            for course_group_begin in courses_group_begin:
                CourseGroupFactory(
                    course=course_group_begin,
                    enroll_begin=now_moment - timedelta(days=1),
                )
                CourseGroupFactory(
                    course=course_group_begin,
                    enroll_begin=now_moment + timedelta(days=1)
                )

            courses_begin_none_end_none = CourseFactory.create_batch(
                4, is_active=True, is_archive=False, enroll_begin=None, enroll_end=None,
            )
            courses_end_none = CourseFactory.create_batch(
                4, is_active=True, is_archive=False, enroll_begin=now_moment - timedelta(days=2), enroll_end=None,
            )
            courses_begin_none = CourseFactory.create_batch(
                4, is_active=True, is_archive=False, enroll_begin=None, enroll_end=now_moment + timedelta(days=2),
            )
            courses_begin_end = CourseFactory.create_batch(
                4, is_active=True, is_archive=False,
                enroll_begin=now_moment - timedelta(days=2), enroll_end=now_moment + timedelta(days=2),
            )
            _ = courses_begin_none_end_none + courses_end_none + courses_begin_none + courses_begin_end

            courses_group_full = [
                courses_begin_none_end_none[0], courses_end_none[0], courses_begin_none[0], courses_begin_end[0],
            ]
            for course_group_full in courses_group_full:
                unavailable_group1 = CourseGroupFactory(course=course_group_full, max_participants=2)
                EnrolledUserFactory(
                    course=course_group_full, group=unavailable_group1, status=EnrolledUser.StatusChoices.ENROLLED,
                )
                EnrolledUserFactory(
                    course=course_group_full, group=unavailable_group1, status=EnrolledUser.StatusChoices.PENDING,
                )

            courses_group_end_none = [
                courses_begin_none_end_none[1], courses_end_none[1], courses_begin_none[1], courses_begin_end[1],
            ]
            for course_group_end_none in courses_group_end_none:
                CourseGroupFactory(
                    course=course_group_end_none,
                    enroll_begin=now_moment + timedelta(days=1),
                )

            courses_group_begin_none = [
                courses_begin_none_end_none[2], courses_end_none[2], courses_begin_none[2], courses_begin_end[2],
            ]
            for course_group_begin_none in courses_group_begin_none:
                CourseGroupFactory(
                    course=course_group_begin_none,
                    enroll_end=now_moment - timedelta(days=1),
                )

            courses_group_begin_none_end_none = [
                courses_begin_none_end_none[3], courses_end_none[3], courses_begin_none[3], courses_begin_end[3],
            ]
            for course_group_begin_none_end_none in courses_group_begin_none_end_none:
                CourseGroupFactory(
                    course=course_group_begin_none_end_none,
                    enroll_begin=now_moment + timedelta(days=1),
                    enroll_end=now_moment - timedelta(days=1),
                )

            self.client.force_login(user=user)

            with self.assertNumQueries(10):
                response = self.client.get(self.get_url() + "?available_for_enroll=true&limit=100", format='json')

            self.assertEqual(response.status_code, status.HTTP_200_OK)
            self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
            self.assertEqual(response.data.get('count'), len(available_courses))
            results = response.data.get('results')

            expected_names = [c.name for c in sorted(available_courses, key=lambda x: x.name)]
            result_names = [c['name'] for c in results]
            self.assertListEqual(result_names, expected_names)

            expected_ids = [c.id for c in sorted(available_courses, key=lambda x: x.name)]
            result_ids = [c['id'] for c in results]
            self.assertListEqual(result_ids, expected_ids)

            for available_course in available_courses:
                available_course.refresh_from_db()

            expected = self.build_expected(available_courses)

            self.assertEqual(results, expected)


class CourseGroupAvailableForEnrollFilterTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-groups'

    def test_list(self):
        user = UserFactory()
        now_moment = timezone.now()

        course = CourseFactory(is_active=True, is_archive=False, enroll_begin=None, enroll_end=None)
        available_groups = [
            CourseGroupFactory(course=course, enroll_begin=None, enroll_end=None, max_participants=0),
            CourseGroupFactory(course=course, enroll_begin=None, enroll_end=None, max_participants=3),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment - timedelta(days=1), enroll_end=None, max_participants=0,
            ),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment - timedelta(days=1), enroll_end=None, max_participants=3,
            ),
            CourseGroupFactory(
                course=course,
                enroll_begin=now_moment - timedelta(days=1), enroll_end=now_moment + timedelta(days=1),
                max_participants=0,
            ),
            CourseGroupFactory(
                course=course,
                enroll_begin=now_moment - timedelta(days=1), enroll_end=now_moment + timedelta(days=1),
                max_participants=3,
            ),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment - timedelta(days=1), enroll_end=None, max_participants=0,
            ),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment - timedelta(days=1), enroll_end=None, max_participants=3,
            ),

        ]

        unavailable_groups_by_dates = [
            CourseGroupFactory(course=course, enroll_begin=None, enroll_end=None, max_participants=2),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment - timedelta(days=1), enroll_end=None, max_participants=2,
            ),
            CourseGroupFactory(
                course=course,
                enroll_begin=now_moment - timedelta(days=1), enroll_end=now_moment + timedelta(days=1),
                max_participants=2,
            ),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment - timedelta(days=1), enroll_end=None, max_participants=2,
            ),
        ]
        [
            CourseGroupFactory(
                course=course, enroll_begin=None, enroll_end=now_moment - timedelta(days=1), max_participants=0,
            ),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment + timedelta(days=1), enroll_end=None, max_participants=0,
            ),
        ]
        for group in available_groups + unavailable_groups_by_dates:
            EnrolledUserFactory(course=course, group=group, status=EnrolledUser.StatusChoices.ENROLLED)
            EnrolledUserFactory(course=course, group=group, status=EnrolledUser.StatusChoices.PENDING)
            group.refresh_from_db()

        self.client.force_login(user=user)

        with self.assertNumQueries(5):
            response = self.client.get(
                self.get_url(course.id) + "?available_for_enroll=true&limit=20", format='json',
            )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), len(available_groups))
        results = response.data.get('results')

        expected_ids = [cg.id for cg in sorted(available_groups, key=lambda x: x.name)]
        result_ids = [cg['id'] for cg in results]
        self.assertListEqual(result_ids, expected_ids)

        expected = [
            {
                'id': cg.id,
                'name': cg.name,
                'summary': cg.summary,
                'can_join': cg.can_join,
                'num_participants': cg.num_participants,
                'max_participants': cg.max_participants,
                'available_for_enroll': cg.available_for_enroll,
                'is_full': cg.is_full,
                'is_enroll_open': cg.is_enroll_open,
                'enroll_will_begin': cg.enroll_will_begin,
                'has_open_seats': cg.has_open_seats,
                'begin_date': serializers.DateTimeField().to_representation(cg.begin_date),
                'end_date': serializers.DateTimeField().to_representation(cg.end_date),
                'enroll_begin': serializers.DateTimeField().to_representation(cg.enroll_begin),
                'enroll_end': serializers.DateTimeField().to_representation(cg.enroll_end),
                'tutor': None,
            } for cg in sorted(available_groups, key=lambda cg: cg.name)
        ]
        self.assertEqual(results, expected)


class CourseGroupAvailableForViewFilterTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-groups'

    def setUp(self):
        self.user = UserFactory()

    def test_list(self):
        now_moment = timezone.now()

        course = CourseFactory(is_active=True, is_archive=False, enroll_begin=None, enroll_end=None)
        available_groups = [
            CourseGroupFactory(course=course, enroll_begin=None, enroll_end=None, max_participants=0),
            CourseGroupFactory(course=course, enroll_begin=None, enroll_end=None, max_participants=3),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment - timedelta(days=1), enroll_end=None, max_participants=0,
            ),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment - timedelta(days=1), enroll_end=None, max_participants=3,
            ),
            CourseGroupFactory(
                course=course,
                enroll_begin=now_moment - timedelta(days=1), enroll_end=now_moment + timedelta(days=1),
                max_participants=0,
            ),
            CourseGroupFactory(
                course=course,
                enroll_begin=now_moment - timedelta(days=1), enroll_end=now_moment + timedelta(days=1),
                max_participants=3,
            ),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment - timedelta(days=1), enroll_end=None, max_participants=0,
            ),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment - timedelta(days=1), enroll_end=None, max_participants=3,
            ),
            CourseGroupFactory(course=course, enroll_begin=None, enroll_end=None, max_participants=2),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment - timedelta(days=1), enroll_end=None, max_participants=2,
            ),
            CourseGroupFactory(
                course=course,
                enroll_begin=now_moment - timedelta(days=1), enroll_end=now_moment + timedelta(days=1),
                max_participants=2,
            ),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment - timedelta(days=1), enroll_end=None, max_participants=2,
            ),
        ]
        available_by_dates = [
            CourseGroupFactory(
                course=course, enroll_begin=now_moment + timedelta(days=1), enroll_end=None, max_participants=0,
            ),
            CourseGroupFactory(
                course=course, enroll_begin=now_moment + timedelta(days=1), enroll_end=None, max_participants=2,
            ),
        ]
        [
            CourseGroupFactory(
                course=course, enroll_begin=None, enroll_end=now_moment - timedelta(days=1), max_participants=0,
            ),
            CourseGroupFactory(
                course=course, enroll_begin=None, enroll_end=now_moment - timedelta(days=1), max_participants=2,
            ),
        ]
        for group in available_groups:
            EnrolledUserFactory(course=course, group=group, status=EnrolledUser.StatusChoices.ENROLLED)
            EnrolledUserFactory(course=course, group=group, status=EnrolledUser.StatusChoices.PENDING)
        for group in available_groups + available_by_dates:
            group.refresh_from_db()

        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            response = self.client.get(
                self.get_url(course.id) + "?limit=100", format='json',
            )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), len(available_groups + available_by_dates))
        results = response.data.get('results')

        expected_ids = [cg.id for cg in sorted(available_groups + available_by_dates, key=lambda x: x.name)]
        result_ids = [cg['id'] for cg in results]
        self.assertListEqual(result_ids, expected_ids)

        expected = [
            {
                'id': cg.id,
                'name': cg.name,
                'summary': cg.summary,
                'can_join': cg.can_join,
                'num_participants': cg.num_participants,
                'max_participants': cg.max_participants,
                'available_for_enroll': cg.available_for_enroll,
                'is_full': cg.is_full,
                'is_enroll_open': cg.is_enroll_open,
                'enroll_will_begin': cg.enroll_will_begin,
                'has_open_seats': cg.has_open_seats,
                'begin_date': serializers.DateTimeField().to_representation(cg.begin_date),
                'end_date': serializers.DateTimeField().to_representation(cg.end_date),
                'enroll_begin': serializers.DateTimeField().to_representation(cg.enroll_begin),
                'enroll_end': serializers.DateTimeField().to_representation(cg.enroll_end),
                'tutor': None,
            } for cg in sorted(available_groups + available_by_dates, key=lambda cg: cg.name)
        ]

        self.assertEqual(results, expected, msg=f'\nresults=\n{results},\n expected=\n{expected}\n')


class CourseVisibilityListFilterTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def setUp(self) -> None:
        self.city = StaffCityFactory()
        self.office = StaffOfficeFactory(city=self.city)
        self.user = UserWithStaffProfileFactory(staffprofile__office=self.office)

        self.course1: Course = CourseFactory(is_active=True, is_archive=False)
        self.course2: Course = CourseFactory(is_active=True, is_archive=False)
        self.course3: Course = CourseFactory(is_active=True, is_archive=False)

        self.course_visibility2 = CourseVisibilityFactory(
            course=self.course2,
            rules={"eq": ["staff_city", self.city.id]}
        )
        self.course_visibility3 = CourseVisibilityFactory(
            course=self.course3,
            rules={"ne": ["staff_city", self.city.id]}
        )

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
                'structure': c.structure,
                'format': c.format,
                'author': {
                    'id': c.author.id,
                    'username': c.author.username,
                    'first_name': c.author.first_name,
                    'last_name': c.author.last_name,
                },
                'categories': [cc.id for cc in c.categories.all()],
                'available_for_enroll': c.available_for_enroll,
                'is_full': c.is_full,
                'begin_date': c.begin_date,
                'end_date': c.end_date,
                'enroll_begin': c.enroll_begin,
                'enroll_end': c.enroll_end,
                'payment_method': c.payment_method,
                'price': c.price,
                'paid_percent': c.paid_percent,
                'payment_terms': c.payment_terms,
                'num_hours': c.num_hours,
                'status': c.status,
                'type': c.course_type,
                'tags': list(c.tags.values_list('name', flat=True)),
                'enable_groups': c.enable_groups,
                'created': serializers.DateTimeField().to_representation(c.created),
                'modified': serializers.DateTimeField().to_representation(c.modified),
            } for c in sorted(courses, key=lambda x: x.name)
        ]

    def test_list(self):
        self.client.force_login(user=self.user)
        expected = self.build_expected([self.course1, self.course2])
        self.list_request(self.get_url(), expected=expected, num_queries=11)


class CourseVisibilityDetailListFilterTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-detail'

    def setUp(self):
        self.city = StaffCityFactory()
        self.office = StaffOfficeFactory(city=self.city)
        self.user = UserWithStaffProfileFactory(staffprofile__office=self.office)

        self.course = CourseFactory(is_active=True, is_archive=False)

    def test_url(self):
        self.assertURLNameEqual('courses/{pk}/', kwargs={'pk': 1}, base_url=settings.API_BASE_URL)

    def test_not_found(self):
        max_course_id = Course.objects.aggregate(max_id=Max('id')).get('max_id', 0) or 0

        self.client.force_login(user=self.user)
        with self.assertNumQueries(3):
            response = self.client.get(self.get_url(max_course_id + 1), format='json')

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        response_data = response.data
        self.assertEqual('not_found', response_data['detail'].code)

    def test_visible(self):
        _ = CourseVisibilityFactory(
            course=self.course,
            rules={"eq": ["staff_city", self.city.id]}
        )

        self.client.force_login(user=self.user)
        with self.assertNumQueries(6):
            response = self.client.get(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_invisible(self):
        _ = CourseVisibilityFactory(
            course=self.course,
            rules={"ne": ["staff_city", self.city.id]}
        )

        self.client.force_login(user=self.user)
        with self.assertNumQueries(6):
            response = self.client.get(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)


class CourseCategotyHideEmptyCategoriesFilterTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-category-list'

    def test_categories_with_available_courses(self):
        city = StaffCityFactory()
        office = StaffOfficeFactory(city=city)
        user = UserWithStaffProfileFactory(staffprofile__office=office)

        (
            category_empty, category_empty_for_user,
            categoty_with_unavailable_course, categoty_with_only_available_courses,
        ) = CourseCategoryFactory.create_batch(4)

        inactive_courses = CourseFactory.create_batch(2, is_active=False)
        archive_courses = CourseFactory.create_batch(2, is_archive=True)
        invisible_courses = CourseFactory.create_batch(2, show_in_catalog=False)

        available_courses = CourseFactory.create_batch(3, is_active=True)
        [
            CourseVisibilityFactory(course=course, rules={"eq": ["staff_city", city.id]})
            for course in available_courses
        ]

        unavailable_courses = CourseFactory.create_batch(2, is_active=True)
        [
            CourseVisibilityFactory(course=course, rules={"ne": ["staff_city", city.id]})
            for course in unavailable_courses
        ]

        unavailable_courses[0].categories.add(category_empty_for_user)
        inactive_courses[0].categories.add(category_empty_for_user)
        archive_courses[0].categories.add(category_empty_for_user)
        invisible_courses[0].categories.add(category_empty_for_user)

        unavailable_courses[1].categories.add(categoty_with_unavailable_course)
        available_courses[0].categories.add(categoty_with_unavailable_course)
        inactive_courses[1].categories.add(categoty_with_unavailable_course)
        archive_courses[1].categories.add(categoty_with_unavailable_course)
        invisible_courses[1].categories.add(categoty_with_unavailable_course)

        available_courses[1].categories.add(categoty_with_only_available_courses)
        available_courses[2].categories.add(categoty_with_only_available_courses)

        self.client.force_login(user=user)

        expected = [
            {
                'id': category.id,
                'parent_id': category.parent_id,
                'slug': category.slug,
                'name': category.name,
                'depth': category.node_depth,
                'color_theme': {
                    'id': category.color_theme.id,
                    'name': category.color_theme.name,
                    'slug': category.color_theme.slug,
                    'is_active': category.color_theme.is_active,
                    'course_card_gradient_color': category.color_theme.course_card_gradient_color,
                    'created': serializers.DateTimeField().to_representation(category.color_theme.created),
                    'modified': serializers.DateTimeField().to_representation(category.color_theme.modified),
                }
            } for category in [categoty_with_unavailable_course, categoty_with_only_available_courses]
        ]

        self.list_request(url=self.get_url() + "?with_available_courses=true", expected=expected, num_queries=6)


class CourseTypeFilterTestCase(CourseListChoiceFilterMixin, APITestCase):
    def test_list_by_filter_values(self):
        user = UserFactory()
        courses = self.create_courses_by_field('course_type')
        self.client.force_login(user=user)
        self.assert_course_filter('type', courses)


class CourseFormatFilterTestCase(CourseListChoiceFilterMixin, APITestCase):
    def test_list_by_filter_values(self):
        user = UserFactory()
        courses = self.create_courses_by_field('format')
        self.client.force_login(user=user)
        self.assert_course_filter('course_format', courses)


class CoursePaymentMethodFilterTestCase(CourseListChoiceFilterMixin, APITestCase):
    def test_list_by_filter_values(self):
        user = UserFactory()
        courses = self.create_courses_by_field('payment_method')
        self.client.force_login(user=user)
        self.assert_course_filter('payment_method', courses)


class MyCoursesListFilterTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-courses'

    def build_expected(self, courses, user):
        course_ids = [course.id for course in sorted(courses, key=lambda course: course.name)]
        enrolled_users = EnrolledUser.objects.filter(course_id__in=course_ids, user_id=user.id)
        return [{"id": enrolled_user.id} for enrolled_user in enrolled_users]

    def test_list_with_type_track(self):
        user = UserFactory()
        courses = CourseFactory.create_batch(22, course_type=Course.TypeChoices.COURSE)
        tracks = CourseFactory.create_batch(9, course_type=Course.TypeChoices.TRACK)
        for course in courses + tracks:
            EnrolledUserFactory(user=user, course=course)
        expected = self.build_expected(tracks, user)

        self.client.force_login(user=user)

        self.list_request(self.get_url() + "?type=track", num_queries=6, expected=expected, only_ids=True)


class StudentModuleProgressListFilterTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-course-module-list'

    def test_url(self):
        self.assertURLNameEqual('my/courses/{pk}/modules/', kwargs={'pk': 1}, base_url=settings.API_BASE_URL)

    def test_pagination_all(self):
        course = CourseFactory(is_active=True)
        student = CourseStudentFactory(course=course)
        self.client.force_login(user=student.user)
        module_type = ModuleTypeFactory(app_label="dummies", model="dummy")
        expected = []
        for i in range(25):
            module = CourseModuleFactory(course=course, module_type=module_type)
            progress = StudentModuleProgressFactory(course=course, student=student, module=module, score=100 - i)
            expected.append({'module_id': module.id, 'score': progress.score})

        self.list_request(self.get_url(course.id) + '?all=true', num_queries=5, expected=expected, check_ids=False)
