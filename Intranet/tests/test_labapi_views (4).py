from collections import defaultdict
from unittest.mock import MagicMock, patch

import faker
from guardian.shortcuts import assign_perm

from django.conf import settings
from django.db.models import Max
from django.utils.timezone import utc

from rest_framework import serializers, status
from rest_framework.fields import DateTimeField
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.courses.tests.factories import CourseFactory, CourseGroupFactory
from lms.enrollments.tests.factories import EnrolledUserFactory, TrackerEnrollmentFactory
from lms.staff.tests.factories import StaffGroupFactory, UserWithStaffProfileFactory
from lms.tracker.tests.factories import EnrolledUserTrackerIssueFactory, EnrollmentTrackerIssueFactory
from lms.users.tests.factories import LabUserFactory, UserFactory
from lms.utils.tests.test_decorators import parameterized_expand_doc

from ..models import EnrolledUser, Enrollment, EnrollSurvey
from .factories import EnrollmentFactory, EnrollSurveyFactory, EnrollSurveyFieldFactory

fake = faker.Faker()


class LabEnrollSurveyListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-survey-form-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.surveys = EnrollSurveyFactory.create_batch(3, is_active=True)
        self.inactive_surveys = EnrollSurveyFactory.create_batch(2, is_active=False)
        self.form_fields = defaultdict(list)
        for survey in self.surveys:
            self.form_fields[survey.id] = EnrollSurveyFieldFactory.create_batch(3, survey=survey)

    def test_url(self):
        self.assertURLNameEqual('enrollment_surveys/', base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self) -> None:
        self.client.force_login(user=self.user)
        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        response_data = response.data
        expected = [
            {
                'id': survey.id,
                'slug': survey.slug,
                'name': survey.name,
                'summary': survey.summary,
                'description': survey.description,
                'is_active': survey.is_active,
                'created_by': {
                    'id': survey.created_by.id,
                    'username': survey.created_by.username,
                    'first_name': survey.created_by.first_name,
                    'last_name': survey.created_by.last_name,
                },
            } for survey in sorted(self.surveys, key=lambda survey: survey.name)
        ]

        self.assertListEqual(expected, response_data)


class LabEnrollSurveyDetailTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-survey-form-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)

    def test_url(self):
        self.assertURLNameEqual(
            'enrollment_surveys/{pk}/', kwargs={'pk': 1}, base_url=settings.LABAPI_BASE_URL,
        )

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(pk=1), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_not_found(self):
        inactive_survey = EnrollSurveyFactory(is_active=False)
        self.client.force_login(user=self.user)
        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(pk=inactive_survey.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

        max_survey_id = EnrollSurvey.objects.all().aggregate(max_id=Max('id')).get('max_id', 0) or 0
        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(pk=max_survey_id + 1), format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_retrieve(self) -> None:
        survey = EnrollSurveyFactory(is_active=True)
        form_fields = EnrollSurveyFieldFactory.create_batch(3, survey=survey)

        self.client.force_login(user=self.user)
        with self.assertNumQueries(6):
            response = self.client.get(self.get_url(pk=survey.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        response_data = response.data
        expected = {
            'id': survey.id,
            'slug': survey.slug,
            'name': survey.name,
            'summary': survey.summary,
            'description': survey.description,
            'is_active': survey.is_active,
            'created_by': {
                'id': survey.created_by.id,
                'username': survey.created_by.username,
                'first_name': survey.created_by.first_name,
                'last_name': survey.created_by.last_name,
            },
            'fields': [
                {
                    'id': field.id,
                    'survey_id': field.survey_id,
                    'name': field.name,
                    'field_type': field.field_type,
                    'title': field.title,
                    'description': field.description,
                    'placeholder': field.placeholder,
                    'parameters': field.parameters,
                    'default_value': field.default_value,
                    'is_required': field.is_required,
                    'is_hidden': field.is_hidden,
                } for field in form_fields
            ]
        }

        self.assertDictEqual(expected, response_data)


class LabEnrollmentListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-enrollment-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)
        self.enrollment = EnrollmentFactory(course=self.course, enroll_type=Enrollment.TYPE_MANUAL)

    def test_url(self):
        self.assertURLNameEqual(
            'courses/{pk}/enrollments/', kwargs={'pk': 1}, base_url=settings.LABAPI_BASE_URL,
        )

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(pk=self.course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        assign_perm('courses.view_course', self.user, self.course)
        self.client.force_login(user=self.user)
        with self.assertNumQueries(9):
            response = self.client.get(self.get_url(pk=self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)

        expected = [{
            'id': e.id,
            'survey_id': e.survey_id,
            'enroll_type': e.enroll_type,
            'name': e.name,
            'summary': e.summary,
            'options': e.options,
            'is_active': e.is_active,
            'is_default': e.is_default,
            'created': serializers.DateTimeField().to_representation(e.created),
            'modified': serializers.DateTimeField().to_representation(e.modified)
        } for e in [self.enrollment]]
        self.assertEqual(response.data, expected)


class LabEnrollmentDetailTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)
        self.enrollment = EnrollmentFactory(course=self.course, enroll_type=Enrollment.TYPE_MANUAL)

    def test_url(self):
        self.assertURLNameEqual(
            'enrollments/{pk}/', kwargs={'pk': 1}, base_url=settings.LABAPI_BASE_URL,
        )

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(pk=self.enrollment.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_retrieve(self):
        assign_perm('courses.view_course', self.user, self.course)
        self.client.force_login(user=self.user)
        with self.assertNumQueries(9):
            response = self.client.get(self.get_url(pk=self.enrollment.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)


class LabEnrollmentCreateTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-create'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual(
            'enrollments/', base_url=settings.LABAPI_BASE_URL,
        )

    def test_create(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)
        new_enrollment = EnrollmentFactory.build(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_MANUAL)
        data = {
            'course_id': new_enrollment.course_id,
            'enroll_type': new_enrollment.enroll_type,
            'name': new_enrollment.name,
            'is_active': new_enrollment.is_active,
        }
        with self.assertNumQueries(15):
            response = self.client.post(self.get_url(), data=data, format='json')

        self.assertEqual(response.status_code, status.HTTP_201_CREATED, msg=response.data)

        response_data = response.data
        enrollment = Enrollment.objects.get(pk=response_data['id'])

        expected = {
            'id': enrollment.id,
            'course_id': enrollment.course_id,
            'survey_id': enrollment.survey_id,
            'enroll_type': enrollment.enroll_type,
            'name': enrollment.name,
            'summary': enrollment.summary,
            'options': enrollment.options,
            'is_active': enrollment.is_active,
            'is_default': enrollment.is_default,
            'created': serializers.DateTimeField().to_representation(enrollment.created),
            'modified': serializers.DateTimeField().to_representation(enrollment.modified),
        }
        self.assertEqual(response_data, expected)
        self.assertTrue(enrollment.is_default)


class LabEnrolledUserListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:enrolled-user-list'

    def setUp(self):
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)
        self.groups = CourseGroupFactory.create_batch(2, course=self.course)
        self.enrollment = TrackerEnrollmentFactory(course=self.course)
        self.users = UserWithStaffProfileFactory.create_batch(3)
        for user in self.users:
            staff_groups = StaffGroupFactory.create_batch(2)
            user.staffprofile.groups.add(*staff_groups)
            user.staff_groups = staff_groups
        self.other_staff_groups = StaffGroupFactory.create_batch(2)

        self.enrolled_users_with_groups = [
            EnrolledUserFactory(course=self.course, group=group, enrollment=self.enrollment, user=user)
            for user, group in zip(self.users[:2], self.groups)
        ]
        self.enrolled_users_without_groups = [
            EnrolledUserFactory(course=self.course, group=None, enrollment=self.enrollment, user=self.users[2])
        ]

        self.enrolled_users = self.enrolled_users_with_groups + self.enrolled_users_without_groups
        for enrolled_user in self.enrolled_users:
            enrolled_user._issues = EnrollmentTrackerIssueFactory.create_batch(2, enrolled_user=enrolled_user)
            enrolled_user._tracker_issues = [
                EnrolledUserTrackerIssueFactory(enrolled_user=enrolled_user, is_default=True),
                EnrolledUserTrackerIssueFactory(enrolled_user=enrolled_user, is_default=False),
            ]
        self.other_issues = EnrollmentTrackerIssueFactory.create_batch(
            2, enrolled_user__enrollment__enroll_type=Enrollment.TYPE_TRACKER,
        )
        self.other_tracker_issues = [
            EnrolledUserTrackerIssueFactory(
                enrolled_user__enrollment__enroll_type=Enrollment.TYPE_TRACKER, is_default=True,
            ),
            EnrolledUserTrackerIssueFactory(
                enrolled_user__enrollment__enroll_type=Enrollment.TYPE_TRACKER, is_default=True,
            ),
        ]
        self.enrolled_users_other_course = EnrolledUserFactory.create_batch(2)

    def build_expected(self, enrolled_users):
        return [
            {
                'id': enrolled_user.id,
                'course_id': self.course.id,
                'enrollment_id': enrolled_user.enrollment_id,
                'user': {
                    'id': enrolled_user.user_id,
                    'username': enrolled_user.user.username,
                    'first_name': enrolled_user.user.first_name,
                    'last_name': enrolled_user.user.last_name,
                },
                'group': {
                    'id': enrolled_user.group_id,
                    'name': enrolled_user.group.name,
                } if enrolled_user.group else None,
                'departments': [
                    {
                        'id': staff_group.id,
                        'name': staff_group.name,
                        'level': staff_group.level,
                    }
                    for staff_group in sorted(enrolled_user.user.staff_groups, key=lambda g: (g.level, g.name))
                ],
                'issues': [
                    {
                        'id': issue.id,
                        'queue_id': issue.queue_id,
                        'issue_number': issue.issue_number,
                        'got_status_from_startrek':
                            serializers.DateTimeField().to_representation(issue.got_status_from_startrek),
                        'status': issue.status,
                        'status_processed': issue.status_processed,
                        'issue': issue.issue,
                    }
                    for issue in enrolled_user._issues if issue.queue.is_default
                ],
                'tracker_issues': [
                    {
                        'id': issue.id,
                        'queue_id': issue.queue_id,
                        'issue_key': issue.issue_key,
                        'issue_status': str(issue.issue_status),
                        'status': str(issue.status),
                    } for issue in enrolled_user._tracker_issues if issue.is_default
                ],
                'enroll_date': serializers.DateTimeField().to_representation(enrolled_user.enroll_date),
                'completion_date': serializers.DateTimeField().to_representation(enrolled_user.completion_date),
                'status': enrolled_user.status,
                'created': serializers.DateTimeField().to_representation(enrolled_user.created),
                'modified': serializers.DateTimeField().to_representation(enrolled_user.modified),
            } for enrolled_user in enrolled_users
        ]

    def test_list(self):

        assign_perm('courses.view_course', self.user, self.course)
        self.client.force_login(user=self.user)

        expected = self.build_expected(
            sorted(self.enrolled_users, key=lambda enrolled_user: enrolled_user.created, reverse=True),
        )

        url = self.get_url(self.course.id)
        self.list_request(url, expected, num_queries=13)

    def test_list_with_group_filter(self):
        assign_perm('courses.view_course', self.user, self.course)
        self.client.force_login(user=self.user)

        url = self.get_url(self.course.id)

        expected = self.build_expected(
            sorted(
                filter(
                    lambda enrolled_user: enrolled_user.group_id == self.groups[0].id,
                    self.enrolled_users,
                ),
                key=lambda enrolled_user: enrolled_user.created,
                reverse=True,
            ),
        )

        self.list_request(url + f'?group_id={self.groups[0].id}', expected, num_queries=14)


class LabEnrolledUserDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:enrolled-user-detail'

    def setUp(self):
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)
        self.group = CourseGroupFactory(course=self.course)
        self.enrollment = TrackerEnrollmentFactory(course=self.course)

    @parameterized_expand_doc(
        [
            (EnrolledUser.StatusChoices.ENROLLED, EnrolledUser.StatusChoices.ENROLLED, 25),
            (EnrolledUser.StatusChoices.ENROLLED, EnrolledUser.StatusChoices.PENDING, 25),
            (EnrolledUser.StatusChoices.ENROLLED, EnrolledUser.StatusChoices.REJECTED, 32),
            (EnrolledUser.StatusChoices.ENROLLED, EnrolledUser.StatusChoices.COMPLETED, 32),

            (EnrolledUser.StatusChoices.PENDING, EnrolledUser.StatusChoices.ENROLLED, 32),
            (EnrolledUser.StatusChoices.PENDING, EnrolledUser.StatusChoices.PENDING, 23),
            (EnrolledUser.StatusChoices.PENDING, EnrolledUser.StatusChoices.REJECTED, 25),
            (EnrolledUser.StatusChoices.PENDING, EnrolledUser.StatusChoices.COMPLETED, 25),

            (EnrolledUser.StatusChoices.REJECTED, EnrolledUser.StatusChoices.ENROLLED, 37),
            (EnrolledUser.StatusChoices.REJECTED, EnrolledUser.StatusChoices.PENDING, 28),
            (EnrolledUser.StatusChoices.REJECTED, EnrolledUser.StatusChoices.REJECTED, 20),
            (EnrolledUser.StatusChoices.REJECTED, EnrolledUser.StatusChoices.COMPLETED, 20),

            (EnrolledUser.StatusChoices.COMPLETED, EnrolledUser.StatusChoices.ENROLLED, 37),
            (EnrolledUser.StatusChoices.COMPLETED, EnrolledUser.StatusChoices.PENDING, 28),
            (EnrolledUser.StatusChoices.COMPLETED, EnrolledUser.StatusChoices.REJECTED, 20),
            (EnrolledUser.StatusChoices.COMPLETED, EnrolledUser.StatusChoices.COMPLETED, 20),
        ]
    )
    def test_partial_update(self, old_status, new_status, num_queries: int):
        """
        Тестируется изменение статуса заявки между всеми возможными парами статусов
        Разное количество запросов, так как запускаются разные post-процедуры
        """
        self.enrolled_user = EnrolledUserFactory(
            course=self.course, group=self.group, enrollment=self.enrollment, status=old_status,
        )

        now_moment = fake.date_time(tzinfo=utc)
        mocked_now = MagicMock()
        mocked_now.return_value = now_moment

        data = {
            'status': new_status,
        }

        new_enroll_date = (
            now_moment
            if (
                new_status == EnrolledUser.StatusChoices.ENROLLED and
                old_status != EnrolledUser.StatusChoices.ENROLLED
            )
            else self.enrolled_user.enroll_date
        )
        new_completion_date = (
            now_moment
            if (
                new_status == EnrolledUser.StatusChoices.COMPLETED and
                old_status != EnrolledUser.StatusChoices.COMPLETED
            )
            else self.enrolled_user.completion_date
        )

        def expected(response):
            return {
                "id": self.enrolled_user.id,
                "course_id": self.enrolled_user.course_id,
                "enrollment_id": self.enrolled_user.enrollment_id,
                "user_id": self.enrolled_user.user_id,
                "group": {
                    'id': self.enrolled_user.group_id,
                    'name': self.enrolled_user.group.name,
                },
                "enroll_date": serializers.DateTimeField().to_representation(new_enroll_date),
                "completion_date": serializers.DateTimeField().to_representation(new_completion_date),
                "status": new_status.value,
                "created": serializers.DateTimeField().to_representation(self.enrolled_user.created),
                "modified": serializers.DateTimeField().to_representation(response.data['modified']),
            }

        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)
        url = self.get_url(self.enrolled_user.id)
        with patch('lms.enrollments.models.timezone.now', new=mocked_now):
            self.partial_update_request(url, data, expected=expected, num_queries=num_queries)


class CourseEnrollmentInstantDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-instant-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual(
            'enrollments/instant/{pk}/', kwargs={'pk': 1}, base_url=settings.LABAPI_BASE_URL,
        )

    def test_not_found(self) -> None:
        enrollment = EnrollmentFactory(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_MANUAL)

        self.client.force_login(user=self.user)
        assign_perm('courses.view_course', self.user, self.course)
        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(pk=enrollment.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_retrieve(self) -> None:
        enrollment = EnrollmentFactory(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_INSTANT)

        self.client.force_login(user=self.user)

        assign_perm('courses.view_course', self.user, self.course)

        expected = {
            'id': enrollment.id,
            'course_id': enrollment.course_id,
            'survey_id': enrollment.survey_id,
            'enroll_type': enrollment.enroll_type,
            'name': enrollment.name,
            'summary': enrollment.summary,
            'options': enrollment.options,
            'is_active': enrollment.is_active,
            'is_default': enrollment.is_default,
            'created': DateTimeField().to_representation(enrollment.created),
            'modified': DateTimeField().to_representation(enrollment.modified),
        }
        self.detail_request(url=self.get_url(pk=enrollment.id), expected=expected, num_queries=9)

    def test_partial_update(self):
        enrollment = EnrollmentFactory(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_INSTANT)
        new_enrollment = EnrollmentFactory.build(
            course=self.course, is_active=True, enroll_type=Enrollment.TYPE_INSTANT,
        )

        data = {
            'survey_id': new_enrollment.survey_id,
            'name': new_enrollment.name,
            'summary': new_enrollment.summary,
        }

        def make_request(response):
            return {
                **data,
                **{
                    'id': enrollment.id,
                    'course_id': enrollment.course_id,
                    'enroll_type': enrollment.enroll_type,
                    'options': enrollment.options,
                    'is_active': enrollment.is_active,
                    'is_default': enrollment.is_default,
                    'created': response.data['created'],
                    'modified': response.data['modified'],
                },
            }

        assign_perm('courses.change_course', self.user, self.course)

        self.client.force_login(user=self.user)

        self.partial_update_request(
            url=self.get_url(pk=enrollment.id), data=data, expected=make_request, num_queries=13,
        )

    def test_delete(self) -> None:
        enrollment = EnrollmentFactory(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_INSTANT)

        self.client.force_login(user=self.user)

        assign_perm('courses.change_course', self.user, self.course)

        self.delete_request(url=self.get_url(pk=enrollment.id), num_queries=16)


class CourseEnrollmentInstantCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-instant-create'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual(
            'enrollments/instant/', base_url=settings.LABAPI_BASE_URL,
        )

    def test_create(self) -> None:
        self.client.force_login(user=self.user)
        enrollment = EnrollmentFactory.build(course=self.course, is_default=True)

        assign_perm('courses.change_course', self.user, self.course)
        data = {
            'course_id': enrollment.course_id,
            'survey_id': enrollment.survey_id,
            "name": enrollment.name,
            "summary": enrollment.summary,
            "is_active": enrollment.is_active,
            "is_default": enrollment.is_default,
        }

        def make_expected(response):
            response_data = response.data
            return {
                **{
                    'id': response_data['id'],
                    'enroll_type': Enrollment.TYPE_INSTANT,
                    'options': {},
                    'created': DateTimeField().to_representation(response.data['created']),
                    'modified': DateTimeField().to_representation(response.data['modified']),
                },
                **data,
            }

        self.create_request(url=self.get_url(), data=data, expected=make_expected, num_queries=14)


class CourseEnrollmentManualDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-manual-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual(
            'enrollments/manual/{pk}/', kwargs={'pk': 1}, base_url=settings.LABAPI_BASE_URL,
        )

    def test_not_found(self) -> None:
        enrollment = EnrollmentFactory(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_INSTANT)

        self.client.force_login(user=self.user)
        assign_perm('courses.view_course', self.user, self.course)
        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(pk=enrollment.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_retrieve(self) -> None:
        enrollment = EnrollmentFactory(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_MANUAL)

        self.client.force_login(user=self.user)

        assign_perm('courses.view_course', self.user, self.course)

        expected = {
            'id': enrollment.id,
            'course_id': enrollment.course_id,
            'survey_id': enrollment.survey_id,
            'enroll_type': enrollment.enroll_type,
            'name': enrollment.name,
            'summary': enrollment.summary,
            'options': enrollment.options,
            'is_active': enrollment.is_active,
            'is_default': enrollment.is_default,
            'created': DateTimeField().to_representation(enrollment.created),
            'modified': DateTimeField().to_representation(enrollment.modified),
        }

        self.detail_request(url=self.get_url(pk=enrollment.id), expected=expected, num_queries=9)

    def test_partial_update(self):
        enrollment = EnrollmentFactory(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_MANUAL)
        new_enrollment = EnrollmentFactory.build(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_MANUAL)

        data = {
            'survey_id': new_enrollment.survey_id,
            'name': new_enrollment.name,
            'summary': new_enrollment.summary,
        }

        def make_request(response):
            return {
                **data,
                **{
                    'id': enrollment.id,
                    'course_id': enrollment.course_id,
                    'enroll_type': enrollment.enroll_type,
                    'options': enrollment.options,
                    'is_active': enrollment.is_active,
                    'is_default': enrollment.is_default,
                    'created': response.data['created'],
                    'modified': response.data['modified'],
                },
            }

        self.client.force_login(user=self.user)

        assign_perm('courses.change_course', self.user, self.course)

        self.partial_update_request(
            url=self.get_url(pk=enrollment.id), data=data, expected=make_request, num_queries=13,
        )

    def test_delete(self) -> None:
        enrollment = EnrollmentFactory(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_MANUAL)

        self.client.force_login(user=self.user)

        assign_perm('courses.change_course', self.user, self.course)

        self.delete_request(url=self.get_url(pk=enrollment.id), num_queries=16)


class CourseEnrollmentManualCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-manual-create'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual(
            'enrollments/manual/', base_url=settings.LABAPI_BASE_URL,
        )

    def test_create(self) -> None:
        self.client.force_login(user=self.user)
        enrollment = EnrollmentFactory.build(course=self.course, is_default=True)

        assign_perm('courses.change_course', self.user, self.course)
        data = {
            'course_id': enrollment.course_id,
            'survey_id': enrollment.survey_id,
            "name": enrollment.name,
            "summary": enrollment.summary,
            "is_active": enrollment.is_active,
            "is_default": enrollment.is_default,
        }

        def make_expected(response):
            response_data = response.data
            return {
                **{
                    'id': response_data['id'],
                    'enroll_type': Enrollment.TYPE_MANUAL,
                    'options': {},
                    'created': DateTimeField().to_representation(response.data['created']),
                    'modified': DateTimeField().to_representation(response.data['modified']),
                },
                **data,
            }

        self.create_request(url=self.get_url(), data=data, expected=make_expected, num_queries=14)
