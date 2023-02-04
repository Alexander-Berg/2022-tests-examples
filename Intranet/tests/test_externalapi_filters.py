from datetime import datetime, timedelta
from itertools import chain
from unittest.mock import MagicMock, patch

from django.conf import settings
from django.utils.timezone import utc

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import UrlNameMixin
from lms.enrollments.models import EnrolledUser
from lms.enrollments.tests.factories import EnrolledUserFactory
from lms.users.tests.factories import ServiceAccountFactory, UserFactory

from ..models import CourseStudent
from .factories import CourseFactory, CourseGroupFactory, CourseStudentFactory, ServiceAccountCourseFactory
from .mixins import CourseListChoiceFilterMixin

COURSE_LIST_NUM_QUERIES = 7


class CourseAvailableForEnrollFilterTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'externalapi:course-list'

    def setUp(self):
        self.service_account = ServiceAccountFactory(tvm_id=settings.DEBUG_TVM_SERVICE_ID)

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
                'is_active': c.is_active,
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
            for course in available_courses:
                ServiceAccountCourseFactory(course=course, service_account=self.service_account)

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

            other_courses = [
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
            for course in other_courses:
                ServiceAccountCourseFactory(course=course, service_account=self.service_account)
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
                ServiceAccountCourseFactory(course=course, service_account=self.service_account)
                occupancy = course.occupancy
                occupancy.maximum = 2
                occupancy.save()
                EnrolledUserFactory(course=course, status=EnrolledUser.StatusChoices.ENROLLED)
                EnrolledUserFactory(course=course, status=EnrolledUser.StatusChoices.PENDING)
                course.refresh_from_db()

            with self.assertNumQueries(COURSE_LIST_NUM_QUERIES):
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
            for course in available_courses:
                ServiceAccountCourseFactory(course=course, service_account=self.service_account)

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
            cs = courses_begin_none_end_none + courses_end_none + courses_begin_none + courses_begin_end
            for course in cs:
                ServiceAccountCourseFactory(course=course, service_account=self.service_account)

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

            with self.assertNumQueries(COURSE_LIST_NUM_QUERIES):
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


class CourseFilterTestCase(CourseListChoiceFilterMixin, APITestCase):
    URL_NAME = 'externalapi:course-list'

    def setUp(self):
        self.service_account = ServiceAccountFactory(tvm_id=settings.DEBUG_TVM_SERVICE_ID)

    def test_list_filter_course_type(self):
        courses_dict = self.create_courses_by_field('course_type')
        for courses in courses_dict.values():
            for course in courses:
                ServiceAccountCourseFactory(course=course,
                                            service_account=self.service_account)
        self.assert_course_filter('type', courses_dict, num_queries=COURSE_LIST_NUM_QUERIES)

    def test_list_filter_format(self):
        courses_dict = self.create_courses_by_field('format')
        for courses in courses_dict.values():
            for course in courses:
                ServiceAccountCourseFactory(course=course,
                                            service_account=self.service_account)
        self.assert_course_filter('course_format', courses_dict, num_queries=COURSE_LIST_NUM_QUERIES)

    def test_list_filter_payment_method(self):
        courses_dict = self.create_courses_by_field('payment_method')
        for courses in courses_dict.values():
            for course in courses:
                ServiceAccountCourseFactory(course=course,
                                            service_account=self.service_account)
        self.assert_course_filter('payment_method', courses_dict, num_queries=COURSE_LIST_NUM_QUERIES)


class CourseProgressFilterTestCase(CourseListChoiceFilterMixin, APITestCase):
    URL_NAME = 'externalapi:course-results-list'

    def setUp(self):
        self.course = CourseFactory(is_active=True)
        self.url = self.get_url(self.course.pk)
        service_account = ServiceAccountFactory(tvm_id=settings.DEBUG_TVM_SERVICE_ID)
        ServiceAccountCourseFactory(course=self.course, service_account=service_account)
        self.another_course = CourseFactory(is_active=True)
        CourseStudentFactory.create_batch(7, course=self.another_course)

    def build_expected(self, course_students):
        return [{"id": cs.id} for cs in sorted(course_students, key=lambda cs: cs.pk)]

    def test_progress_filter_status(self):
        active_students = CourseStudentFactory.create_batch(5, course=self.course,
                                                            status=CourseStudent.StatusChoices.ACTIVE)
        expelled_students = CourseStudentFactory.create_batch(2, course=self.course,
                                                              status=CourseStudent.StatusChoices.EXPELLED)
        completed_students = CourseStudentFactory.create_batch(1, course=self.course,
                                                               status=CourseStudent.StatusChoices.COMPLETED)
        expected = self.build_expected(active_students)
        url = f"{self.url}?status={CourseStudent.StatusChoices.ACTIVE}"
        self.list_request(url, num_queries=5, expected=expected, only_ids=True)
        url = f"{self.url}?status={CourseStudent.StatusChoices.ACTIVE},{CourseStudent.StatusChoices.EXPELLED}"
        expected = self.build_expected(chain(active_students, expelled_students))
        self.list_request(url, num_queries=5, expected=expected, only_ids=True)
        # Стандартное джанговское поведение не поддерживается
        url = f"{self.url}?status={CourseStudent.StatusChoices.EXPELLED}&status={CourseStudent.StatusChoices.COMPLETED}"
        expected = self.build_expected(completed_students)
        self.list_request(url, num_queries=5, expected=expected, only_ids=True)

    def test_progress_filter_is_passed(self):
        passed = CourseStudentFactory.create_batch(5, course=self.course, is_passed=True)
        not_passed = CourseStudentFactory.create_batch(2, course=self.course, is_passed=False)
        expected = self.build_expected(chain(passed, not_passed))
        self.list_request(self.url, num_queries=5, expected=expected, only_ids=True)
        self.list_request(url=f"{self.url}?isFinished=1", num_queries=5,
                          expected=self.build_expected(passed), only_ids=True)
        self.list_request(url=f"{self.url}?isFinished=True", num_queries=5,
                          expected=self.build_expected(passed), only_ids=True)
        self.list_request(url=f"{self.url}?isFinished=false", num_queries=5,
                          expected=self.build_expected(not_passed), only_ids=True)

    def test_progress_filter_started_at(self):
        course_students = CourseStudentFactory.create_batch(3, course=self.course)
        course_student1, course_student2, course_student3 = course_students
        course_student1.created = datetime(year=2021, month=3, day=8)
        course_student2.created = datetime(year=2021, month=3, day=10)
        course_student3.created = datetime(year=2021, month=3, day=14)
        course_student1.save()
        course_student2.save()
        course_student3.save()
        url = f"{self.url}?startedAt_before=2021-03-10T00:00:00"
        expected = self.build_expected([course_student1, course_student2])
        self.list_request(url, num_queries=5, expected=expected, only_ids=True)
        url = f"{self.url}?startedAt_before=2021-03-10T00:00:00Z&startedAt_after=2021-03-08T00:00:00Z"
        expected = self.build_expected([course_student1, course_student2])
        self.list_request(url, num_queries=5, expected=expected, only_ids=True)
        url = f"{self.url}?startedAt_before=2021-03-10T00:00:00&startedAt_after=2021-03-09T00:00:00Z"
        expected = self.build_expected([course_student2])
        self.list_request(url, num_queries=5, expected=expected, only_ids=True)

    def test_progress_filter_yandex_login(self):
        course_students = CourseStudentFactory.create_batch(5, course=self.course)
        user = UserFactory(username='my-login')
        course_student = CourseStudentFactory(course=self.course, user=user)
        expected = self.build_expected(chain(course_students, [course_student]))
        self.list_request(self.url, num_queries=5, expected=expected, only_ids=True)
        expected = self.build_expected([course_student])
        self.list_request(f"{self.url}?yandexLogin=my-login", num_queries=5, expected=expected, only_ids=True)
        self.list_request(f"{self.url}?yandexLogin=my-login2", num_queries=2, expected=[], only_ids=True)
