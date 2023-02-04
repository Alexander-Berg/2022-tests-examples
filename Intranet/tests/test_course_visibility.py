from datetime import date, datetime, timedelta
from typing import List
from unittest.mock import patch

import faker

from django.test import TransactionTestCase
from django.utils import timezone

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.staff.models import StaffGroup, StaffLeadership
from lms.staff.tests.factories import (
    StaffCityFactory, StaffGroupFactory, StaffLeadershipFactory, StaffOfficeFactory, UserWithStaffProfileFactory,
)
from lms.users.tests.factories import UserFactory

from ..models import CourseVisibility
from ..services import is_course_available_for_user
from .factories import CohortFactory, CourseFactory, CourseVisibilityFactory

fake = faker.Faker()


class StaffJoinedDateRuleTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def setUp(self):
        self.user = UserWithStaffProfileFactory(staffprofile__joined_at=datetime(2020, 5, 15, tzinfo=timezone.utc))
        (
            self.course1, self.course2, self.course3, self.course4, self.course5, self.course6, self.course7,
        ) = CourseFactory.create_batch(7, is_active=True, is_archive=False)

        self.course_visibility2 = CourseVisibilityFactory(
            course=self.course2,
            rules={"eq": ["staff_joined_date", {"from": "2020-05-01", "to": "2020-06-01"}]}
        )
        self.course_visibility3 = CourseVisibilityFactory(
            course=self.course3,
            rules={"eq": ["staff_joined_date", {"from": "2020-04-01", "to": "2020-05-01"}]}
        )
        self.course_visibility4 = CourseVisibilityFactory(
            course=self.course4,
            rules={"eq": ["staff_joined_date", {"from": "2020-04-01"}]}
        )
        self.course_visibility5 = CourseVisibilityFactory(
            course=self.course5,
            rules={"eq": ["staff_joined_date", {"from": "2020-06-01"}]}
        )
        self.course_visibility6 = CourseVisibilityFactory(
            course=self.course6,
            rules={"eq": ["staff_joined_date", {"to": "2020-04-01"}]}
        )
        self.course_visibility7 = CourseVisibilityFactory(
            course=self.course7,
            rules={"eq": ["staff_joined_date", {"to": "2020-06-01"}]}
        )

    def test_list(self):
        expected = {
            self.course1.id, self.course2.id, self.course4.id, self.course7.id,
        }

        self.client.force_login(user=self.user)
        with self.assertNumQueries(11):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), len(expected))
        results = response.data.get('results')

        self.assertSetEqual({result['id'] for result in results}, expected)


class StaffWorkPeriodRuleTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def setUp(self):
        self.user = UserWithStaffProfileFactory(staffprofile__joined_at=datetime(2020, 5, 15, tzinfo=timezone.utc))

        (
            self.course1, self.course2, self.course3, self.course4, self.course5, self.course6, self.course7,
            self.course8, self.course9,
        ) = CourseFactory.create_batch(9, is_active=True, is_archive=False)

        self.course_visibility2 = CourseVisibilityFactory(
            course=self.course2,
            rules={"eq": ["staff_working_period", {"from": "10", "to": "20"}]}
        )
        self.course_visibility3 = CourseVisibilityFactory(
            course=self.course3,
            rules={"eq": ["staff_working_period", {"from": "20", "to": "30"}]}
        )
        self.course_visibility4 = CourseVisibilityFactory(
            course=self.course4,
            rules={"eq": ["staff_working_period", {"from": "10"}]}
        )
        self.course_visibility5 = CourseVisibilityFactory(
            course=self.course5,
            rules={"eq": ["staff_working_period", {"from": "20"}]}
        )
        self.course_visibility6 = CourseVisibilityFactory(
            course=self.course6,
            rules={"eq": ["staff_working_period", {"to": "10"}]}
        )
        self.course_visibility7 = CourseVisibilityFactory(
            course=self.course7,
            rules={"eq": ["staff_working_period", {"to": "20"}]}
        )
        self.course_visibility8 = CourseVisibilityFactory(
            course=self.course8,
            rules={"eq": ["staff_working_period", {"from": "100000000000"}]}
        )
        self.course_visibility9 = CourseVisibilityFactory(
            course=self.course9,
            rules={"eq": ["staff_working_period", {"to": "100000000000"}]}
        )

    def test_list(self):
        expected = {
            self.course1.id, self.course2.id, self.course4.id, self.course7.id, self.course9.id,
        }

        self.client.force_login(user=self.user)
        with patch('lms.core.visibility.validators.date') as mocked_date:
            mocked_date.today.return_value = date(2020, 6, 1)
            mocked_date.min = date.min
            with self.assertNumQueries(11):
                response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), len(expected))
        results = response.data.get('results')

        self.assertSetEqual({result['id'] for result in results}, expected)


class StaffLeadershipDateRuleTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def setUp(self):
        self.user = UserWithStaffProfileFactory()
        self.staff_profile = self.user.staffprofile
        self.staff_leadership = StaffLeadershipFactory(
            profile=self.staff_profile,
            joined_at=datetime(2020, 5, 15, tzinfo=timezone.utc),
        )

        (
            self.course1, self.course2, self.course3, self.course4, self.course5, self.course6, self.course7,
        ) = CourseFactory.create_batch(7, is_active=True, is_archive=False)

        self.course_visibility2 = CourseVisibilityFactory(
            course=self.course2,
            rules={"eq": ["staff_leadership_date", {"from": "2020-05-01", "to": "2020-06-01"}]}
        )
        self.course_visibility3 = CourseVisibilityFactory(
            course=self.course3,
            rules={"eq": ["staff_leadership_date", {"from": "2020-04-01", "to": "2020-05-01"}]}
        )
        self.course_visibility4 = CourseVisibilityFactory(
            course=self.course4,
            rules={"eq": ["staff_leadership_date", {"from": "2020-05-01"}]}
        )
        self.course_visibility5 = CourseVisibilityFactory(
            course=self.course5,
            rules={"eq": ["staff_leadership_date", {"from": "2020-06-01"}]}
        )
        self.course_visibility6 = CourseVisibilityFactory(
            course=self.course6,
            rules={"eq": ["staff_leadership_date", {"to": "2020-05-01"}]}
        )
        self.course_visibility7 = CourseVisibilityFactory(
            course=self.course7,
            rules={"eq": ["staff_leadership_date", {"to": "2020-06-01"}]}
        )

    def test_list(self):
        expected = {
            self.course1.id,  # курс без правил
            self.course2.id,  # пользователь попадает в период  (from/to)
            self.course4.id,  # пользователь попадает в начало периода (from)
            self.course7.id,  # пользователь попадает в конец периода (to)
        }

        self.client.force_login(user=self.user)
        with self.assertNumQueries(12):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), len(expected))
        results = response.data.get('results')

        self.assertSetEqual({result['id'] for result in results}, expected)


class StaffLeadershipPeriodRuleTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def setUp(self):
        self.user = UserWithStaffProfileFactory()
        self.staff_profile = self.user.staffprofile
        self.joined_at = datetime(2020, 5, 15, tzinfo=timezone.utc)
        self.staff_leadership = StaffLeadershipFactory(profile=self.staff_profile, joined_at=self.joined_at)

    def assertCourseVisibility(self, user, expected, today):  # noqa: N802
        self.client.force_login(user=user)
        with patch('lms.core.visibility.validators.date') as mocked_date:
            mocked_date.today.return_value = today
            with self.assertNumQueries(12):
                response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), len(expected))
        results = response.data.get('results')

        self.assertSetEqual({result['id'] for result in results}, expected, msg=expected)

    def generate_rules(self, period='d'):
        multiplier = {
            'd': 1,
            'w': 7,
            'm': 30,
            'y': 365,
        }
        today = (self.joined_at + timedelta(days=5 * multiplier[period])).date()

        public_course = CourseFactory(is_active=True)

        # right
        visibility1 = CourseVisibilityFactory(
            course__is_active=True,
            rules={"eq": ["staff_leadership_period", {"from": "4", "to": "6", "period": period}]}
        )

        # wrong
        _ = CourseVisibilityFactory(
            course__is_active=True,
            rules={"eq": ["staff_leadership_period", {"from": "8", "to": "10", "period": period}]}
        )

        # right
        visibility3 = CourseVisibilityFactory(
            course__is_active=True,
            rules={"eq": ["staff_leadership_period", {"from": "4", "period": period}]}
        )

        # wrong
        _ = CourseVisibilityFactory(
            course__is_active=True,
            rules={"eq": ["staff_leadership_period", {"from": "8", "period": period}]}
        )

        # right
        visibility5 = CourseVisibilityFactory(
            course__is_active=True,
            rules={"eq": ["staff_leadership_period", {"to": "6", "period": period}]}
        )

        # right
        _ = CourseVisibilityFactory(
            course__is_active=True,
            rules={"eq": ["staff_leadership_period", {"to": "3", "period": period}]}
        )

        expected = {
            public_course.id,
            visibility1.course_id,
            visibility3.course_id,
            visibility5.course_id,
        }

        self.assertCourseVisibility(self.user, expected, today)

    def test_days(self):
        self.generate_rules('d')

    def test_weeks(self):
        self.generate_rules('w')

    def test_months(self):
        self.generate_rules('m')

    def test_years(self):
        self.generate_rules('y')


class StaffCityRuleTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def setUp(self):
        self.city = StaffCityFactory()
        self.office = StaffOfficeFactory(city=self.city)
        self.user = UserWithStaffProfileFactory(staffprofile__office=self.office)

        self.course1 = CourseFactory(is_active=True, is_archive=False)
        self.course2 = CourseFactory(is_active=True, is_archive=False)
        self.course3 = CourseFactory(is_active=True, is_archive=False)

        self.course_visibility2 = CourseVisibilityFactory(
            course=self.course2,
            rules={"eq": ["staff_city", self.city.id]}
        )
        self.course_visibility3 = CourseVisibilityFactory(
            course=self.course3,
            rules={"ne": ["staff_city", self.city.id]}
        )

    def test_list(self):
        self.client.force_login(user=self.user)
        with self.assertNumQueries(11):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), 2)
        results = response.data.get('results')

        self.assertSetEqual({result['id'] for result in results}, {self.course1.id, self.course2.id})


class StaffOfficeRuleTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def setUp(self):
        self.office = StaffOfficeFactory()
        self.user = UserWithStaffProfileFactory(staffprofile__office=self.office)

        self.course1 = CourseFactory(is_active=True, is_archive=False)
        self.course2 = CourseFactory(is_active=True, is_archive=False)
        self.course3 = CourseFactory(is_active=True, is_archive=False)

        self.course_visibility2 = CourseVisibilityFactory(
            course=self.course2,
            rules={"eq": ["staff_office", self.office.id]}
        )
        self.course_visibility3 = CourseVisibilityFactory(
            course=self.course3,
            rules={"ne": ["staff_office", self.office.id]}
        )

    def test_list(self):
        self.client.force_login(user=self.user)
        with self.assertNumQueries(11):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), 2)
        results = response.data.get('results')

        self.assertSetEqual({result['id'] for result in results}, {self.course1.id, self.course2.id})


class StaffIsHeadRuleTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def setUp(self):
        self.user = UserWithStaffProfileFactory()
        self.staff_profile = self.user.staffprofile
        self.staff_leadership = StaffLeadershipFactory(profile=self.staff_profile, role=StaffLeadership.ROLE_CHIEF)

        self.course1 = CourseFactory(is_active=True, is_archive=False)
        self.course2 = CourseFactory(is_active=True, is_archive=False)
        self.course3 = CourseFactory(is_active=True, is_archive=False)

        self.course_visibility2 = CourseVisibilityFactory(
            course=self.course2,
            rules={"eq": ["staff_is_head", True]}
        )
        self.course_visibility3 = CourseVisibilityFactory(
            course=self.course3,
            rules={"ne": ["staff_is_head", True]}
        )

    def test_list(self):
        self.client.force_login(user=self.user)
        with self.assertNumQueries(12):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), 2)
        results = response.data.get('results')

        self.assertSetEqual({result['id'] for result in results}, {self.course1.id, self.course2.id})


class StaffIsDeputyRuleTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def setUp(self):
        self.user = UserWithStaffProfileFactory()
        self.staff_profile = self.user.staffprofile
        self.staff_leadership = StaffLeadershipFactory(profile=self.staff_profile, role=StaffLeadership.ROLE_DEPUTY)

        self.course1 = CourseFactory(is_active=True, is_archive=False)
        self.course2 = CourseFactory(is_active=True, is_archive=False)
        self.course3 = CourseFactory(is_active=True, is_archive=False)

        self.course_visibility2 = CourseVisibilityFactory(
            course=self.course2,
            rules={"eq": ["staff_is_deputy", True]}
        )
        self.course_visibility3 = CourseVisibilityFactory(
            course=self.course3,
            rules={"ne": ["staff_is_deputy", True]}
        )

    def build_expected(self, courses):
        return [{"id": course.id} for course in sorted(courses, key=lambda course: course.name)]

    def test_list(self):
        self.client.force_login(user=self.user)

        expected = self.build_expected([self.course1, self.course2])
        self.list_request(self.get_url(), num_queries=12, expected=expected, only_ids=True)


class StaffGroupRuleTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def test_list(self):
        staff_groups = StaffGroupFactory.create_batch(3, is_active=True)
        user = UserWithStaffProfileFactory(
            staffprofile__groups_tree=[staff_groups[1].pk, staff_groups[0].pk]
        )

        course1 = CourseFactory(is_active=True, is_archive=False)
        _ = CourseVisibilityFactory(
            course=course1,
            rules={"eq": ["staff_group", {"group_id": staff_groups[0].pk}]}
        )

        course2 = CourseFactory(is_active=True, is_archive=False)
        _ = CourseVisibilityFactory(
            course=course2,
            rules={"eq": ["staff_group", {"group_id": staff_groups[1].pk}]}
        )

        course3 = CourseFactory(is_active=True, is_archive=False)
        _ = CourseVisibilityFactory(
            course=course3,
            rules={"eq": ["staff_group", {"group_id": staff_groups[2].pk}]}
        )

        self.client.force_login(user=user)
        with self.assertNumQueries(11):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), 1)
        results = response.data.get('results')

        self.assertSetEqual({result['id'] for result in results}, {course1.id})

    def test_list_when_group_id_is_string(self):
        staff_groups = StaffGroupFactory.create_batch(3, is_active=True)
        user = UserWithStaffProfileFactory(
            staffprofile__groups_tree=[staff_groups[1].pk, staff_groups[0].pk]
        )

        course1 = CourseFactory(is_active=True, is_archive=False)
        _ = CourseVisibilityFactory(
            course=course1,
            rules={"eq": ["staff_group", {"group_id": str(staff_groups[0].pk)}]}
        )

        course2 = CourseFactory(is_active=True, is_archive=False)
        _ = CourseVisibilityFactory(
            course=course2,
            rules={"eq": ["staff_group", {"group_id": str(staff_groups[1].pk)}]}
        )

        course3 = CourseFactory(is_active=True, is_archive=False)
        _ = CourseVisibilityFactory(
            course=course3,
            rules={"eq": ["staff_group", {"group_id": str(staff_groups[2].pk)}]}
        )

        self.client.force_login(user=user)
        with self.assertNumQueries(11):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), 1)
        results = response.data.get('results')

        self.assertSetEqual({result['id'] for result in results}, {course1.id})

    def build_groups(self, levels: List[int]) -> List[List[StaffGroup]]:
        """
        Собирает симметричное дерево категорий

        Списк levels определяет кол-во категорий на каждом уровне, начиная с корневого.

        :param levels:
        :return:
        """
        level_groups = []
        parents = [None]

        for cnt in levels:
            children = []
            for _ in parents:
                children += StaffGroupFactory.create_batch(cnt)

            level_groups.append(children)
            parents = children

        return level_groups

    def test_nested_group(self):
        staff_group_levels = self.build_groups([1, 2, 6, 2, 2])
        user = UserWithStaffProfileFactory(
            staffprofile__groups_tree=[
                staff_group_levels[0][0].pk,
                staff_group_levels[1][0].pk,
                staff_group_levels[2][0].pk,
                staff_group_levels[3][0].pk,
            ],
        )

        course1 = CourseFactory(is_active=True, is_archive=False)
        CourseVisibilityFactory(
            course=course1,
            rules={"eq": ["staff_group", {"group_id": staff_group_levels[1][0].pk, "nested": True}]}
        )

        for n in range(1, 5):
            course = CourseFactory(is_active=True, is_archive=False)
            CourseVisibilityFactory(
                course=course,
                rules={"eq": ["staff_group", {"group_id": staff_group_levels[2][n].pk, "nested": True}]}
            )

        self.client.force_login(user=user)
        with self.assertNumQueries(11):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), 1)
        results = response.data.get('results')

        self.assertSetEqual({result['id'] for result in results}, {course1.id})

    def test_backward_compatibility(self):
        staff_groups = StaffGroupFactory.create_batch(3, is_active=True)
        user = UserWithStaffProfileFactory(
            staffprofile__groups_tree=[staff_groups[1].pk, staff_groups[0].pk]
        )

        course1 = CourseFactory(is_active=True, is_archive=False)
        _ = CourseVisibilityFactory(
            course=course1,
            rules={"eq": ["staff_group", staff_groups[0].pk]}
        )

        course2 = CourseFactory(is_active=True, is_archive=False)
        _ = CourseVisibilityFactory(
            course=course2,
            rules={"eq": ["staff_group", staff_groups[1].pk]}
        )

        course3 = CourseFactory(is_active=True, is_archive=False)
        _ = CourseVisibilityFactory(
            course=course3,
            rules={"eq": ["staff_group", staff_groups[2].pk]}
        )

        self.client.force_login(user=user)
        with self.assertNumQueries(11):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), 1)
        results = response.data.get('results')

        self.assertSetEqual({result['id'] for result in results}, {course1.id})


class CohortRuleTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def setUp(self):
        self.user_in_cohort, self.user_not_in_cohort = UserFactory.create_batch(2)
        self.cohort = CohortFactory(
            users=[self.user_in_cohort, UserFactory()],
            logins=[self.user_in_cohort.username],
        )
        self.other_cohorts_with_user_in_cohort = CohortFactory.create_batch(
            2,
            users=[self.user_in_cohort, UserFactory()],
            logins=[self.user_in_cohort.username],
        )
        self.other_cohorts_with_user_not_in_cohort = CohortFactory.create_batch(
            2,
            users=[self.user_not_in_cohort, UserFactory()],
            logins=[self.user_not_in_cohort.username],
        )
        self.other_cohorts = CohortFactory.create_batch(
            2,
            logins=[fake.word()],
        )
        self.other_cohorts_with_both_users = CohortFactory.create_batch(
            2,
            users=[self.user_in_cohort, self.user_not_in_cohort, UserFactory()],
            logins=[self.user_in_cohort.username, self.user_not_in_cohort.username],
        )

        self.courses = CourseFactory.create_batch(3, is_active=True, is_archive=False)

        rules = [
            None,
            {"eq": ["cohort", self.cohort.id]},
            {"ne": ["cohort", self.cohort.id]},
        ]

        self.course_visibilities = [
            CourseVisibilityFactory(course=course, rules=rule) if rule else None
            for course, rule in zip(self.courses, rules)
        ]

    def test_list_user_in_cohort(self):
        self.client.force_login(user=self.user_in_cohort)
        expected = [
            {
                'id': course.id,
            } for course in sorted([self.courses[0], self.courses[1]], key=lambda course: course.name)
        ]
        self.list_request(url=self.get_url(), expected=expected, num_queries=11, only_ids=True)

    def test_list_user_not_in_cohort(self):
        self.client.force_login(user=self.user_not_in_cohort)
        expected = [
            {
                'id': course.id,
            } for course in sorted([self.courses[0], self.courses[2]], key=lambda course: course.name)
        ]
        self.list_request(url=self.get_url(), expected=expected, num_queries=11, only_ids=True)


class CourseVisibilityPerformanceTestCase(TransactionTestCase):
    def setUp(self) -> None:
        self.user = UserWithStaffProfileFactory()

    def test_staff_group_rules(self):
        staff_groups = StaffGroupFactory.create_batch(3, is_active=True)
        staff_profile = self.user.staffprofile
        staff_profile.groups_tree = [staff_groups[1].pk, staff_groups[0].pk]

        rules = {"or": [
            {"eq": ["staff_group", {"group_id": staff_groups[0].pk, "nested": True}]},
            {"eq": ["staff_group", {"group_id": staff_groups[0].pk}]},
            {"eq": ["staff_group", {"group_id": staff_groups[1].pk}]},
            {"eq": ["staff_group", {"group_id": staff_groups[2].pk}]},
        ]}

        CourseVisibilityFactory(rules=rules)
        CourseVisibilityFactory(rules=rules)
        CourseVisibilityFactory(rules=rules)

        with self.assertNumQueries(1):
            CourseVisibility.objects.unavailable_for(self.user)

    def test_cohort_rules(self):
        cohort = CohortFactory(users=[self.user], logins=[self.user.username])

        rules = {"or": [
            {"eq": ["cohort", cohort.id]},
            {"eq": ["cohort", cohort.id]},
            {"eq": ["cohort", cohort.id]},
            {"eq": ["cohort", cohort.id]},
            {"eq": ["cohort", cohort.id]},
            {"eq": ["cohort", cohort.id]},
            {"eq": ["cohort", cohort.id]},
            {"eq": ["cohort", cohort.id]},
            {"eq": ["cohort", cohort.id]},
        ]}

        CourseVisibilityFactory(rules=rules)
        CourseVisibilityFactory(rules=rules)
        CourseVisibilityFactory(rules=rules)

        with self.assertNumQueries(2):
            CourseVisibility.objects.unavailable_for(self.user)

    def test_updated_cohort_rules(self):
        course = CourseFactory()

        with patch('lms.courses.receivers.process_pending_cohort_task.delay'):
            cohort = CohortFactory(users=[self.user], logins=[self.user.username], course=course)
            rule = {"and": [{"eq": ["cohort", cohort.id]}]}
            CourseVisibilityFactory(rules=rule, course=course)
            self.assertTrue(is_course_available_for_user(course, self.user))

            new_user = UserWithStaffProfileFactory()
            cohort.users.add(new_user)
            cohort.logins.append(new_user.username)
            cohort.save()
            self.assertTrue(is_course_available_for_user(course, new_user))


class CourseVisibilityInvalidTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def setUp(self):
        self.user = UserWithStaffProfileFactory(staffprofile__joined_at=datetime(2020, 5, 15, tzinfo=timezone.utc))

        (
            self.course1, self.course2, self.course3,
        ) = CourseFactory.create_batch(3, is_active=True, is_archive=False)

        self.course_visibility_invalid = CourseVisibilityFactory(
            course=self.course2,
            rules={"eq": ["staff_working_period", {"from": "qwerty", "to": "20"}]}
        )

        self.course_visibility_valid = CourseVisibilityFactory(
            course=self.course3,
            rules={"eq": ["staff_working_period", {"from": "10", "to": "20"}]}
        )

    def test_list(self):
        expected = {
            self.course1.id, self.course3.id,
        }

        self.client.force_login(user=self.user)
        with patch('lms.core.visibility.validators.date') as mocked_date:
            mocked_date.today.return_value = date(2020, 6, 1)
            with self.assertNumQueries(11):
                response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
        self.assertEqual(response.data.get('count'), len(expected))
        results = response.data.get('results')

        self.assertSetEqual({result['id'] for result in results}, expected)
