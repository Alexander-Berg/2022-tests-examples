from datetime import timedelta
from itertools import chain

from faker import Faker
from guardian.shortcuts import assign_perm

from django.conf import settings
from django.contrib.auth.models import Permission
from django.utils import timezone

from rest_framework import serializers, status
from rest_framework.fields import DateTimeField
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.courses.tests.factories import (
    CourseCategoryFactory, CourseFactory, CourseGroupFactory, CourseStudentFactory, CourseTeamFactory,
    CourseVisibilityFactory, StudentCourseProgressFactory,
)
from lms.staff.tests.factories import StaffCityFactory
from lms.tracker.tests.factories import EnrolledUserTrackerIssueFactory
from lms.users.tests.factories import GroupFactory, PermissionPresetFactory, UserFactory
from lms.utils.tests.test_decorators import parameterized_expand_doc

from ..models import EnrolledUser, Enrollment, EnrollSurveyField
from .factories import EnrolledUserFactory, EnrollmentFactory, EnrollSurveyFactory, EnrollSurveyFieldFactory

fake = Faker()


class CourseEnrollmentListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-enrollments'

    def setUp(self):
        self.user = UserFactory(is_staff=True)
        self.course = CourseFactory.create(is_active=True)
        # self.other_enrollments = EnrollmentFactory.create_batch(2)

    def test_url(self):
        self.assertURLNameEqual('courses/{pk}/enrollments/', kwargs={"pk": 1}, base_url=settings.API_BASE_URL)

    def test_permission_denied(self):
        with self.assertNumQueries(0):
            response = self.client.get(self.get_url(1), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    # TODO куплю бутылку хорошего алкоголя тому, кто это переделает в читабельные тесты
    @parameterized_expand_doc(
        [
            (False, False, False, False, False, False, 3, False),
            (False, False, True, False, False, False, 3, False),
            (False, True, False, False, False, False, 5, False),
            (False, True, True, False, False, False, 6, True),
            (True, False, False, False, False, False, 4, True),
            (True, False, True, False, False, False, 4, True),
            (True, True, False, False, False, False, 6, True),
            (True, True, True, False, False, False, 6, True),
            (False, False, False, True, False, False, 3, False),
            (False, False, False, False, True, False, 3, False),
            (False, False, False, False, False, True, 3, False),
            (False, True, False, True, False, False, 6, True),
            (False, True, False, False, True, False, 6, True),
            (False, True, False, False, False, True, 6, True),
        ]
    )
    def test_list(
        self,
        is_active,
        is_preview_mode,
        user_is_author,
        user_has_permisssion,
        user_group_has_permission,
        user_in_course_team,
        num_queries,
        has_result
    ) -> None:
        course = CourseFactory.create(is_active=is_active, author=self.user if user_is_author else UserFactory())
        active_enrollments = EnrollmentFactory.create_batch(3, is_active=True, course=course)
        _ = EnrollmentFactory.create_batch(3, is_active=False, course=course)

        if user_has_permisssion:
            assign_perm('courses.view_course', self.user, course)
        if user_group_has_permission:
            group = GroupFactory()
            group.user_set.add(self.user)
            assign_perm('courses.view_course', group, course)
        if user_in_course_team:
            preset = PermissionPresetFactory()
            permission = Permission.objects.get(codename='view_course')
            preset.permissions.add(permission)
            course_team = CourseTeamFactory(permission_preset=preset)
            course_team.add_user(self.user.id)
            course.teams.add(course_team)
            self.assertTrue(self.user.has_perm('courses.view_course', course))

        url = self.get_url(course.id)
        if is_preview_mode:
            url += '?preview=true'

        self.client.force_login(user=self.user)

        with self.assertNumQueries(num_queries):
            response = self.client.get(url, format='json')

        response_status = status.HTTP_200_OK if has_result else status.HTTP_404_NOT_FOUND
        self.assertEqual(response.status_code, response_status)

        if has_result:
            response_data = response.data
            expected = [
                {
                    'id': enrollment.id,
                    'survey_id': enrollment.survey_id,
                    'enroll_type': enrollment.enroll_type,
                    'name': enrollment.name,
                    'summary': enrollment.summary,
                    'options': enrollment.options,
                    'is_default': enrollment.is_default,
                    'is_active': enrollment.is_active,
                    'created': serializers.DateTimeField().to_representation(enrollment.created),
                    'modified': serializers.DateTimeField().to_representation(enrollment.modified),
                } for enrollment in sorted(active_enrollments, key=lambda enrollment: enrollment.order)
            ]
            self.assertEqual(response_data, expected)

    def test_list_with_survey(self):
        survey = EnrollSurveyFactory()
        enrollments = EnrollmentFactory.create_batch(3, is_active=True, course=self.course, survey=survey)

        self.client.force_login(user=self.user)

        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(self.course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        response_data = response.data
        expected = [
            {
                'id': enrollment.id,
                'survey_id': survey.id,
                'enroll_type': enrollment.enroll_type,
                'name': enrollment.name,
                'summary': enrollment.summary,
                'options': enrollment.options,
                'is_default': enrollment.is_default,
                'is_active': enrollment.is_active,
                'created': serializers.DateTimeField().to_representation(enrollment.created),
                'modified': serializers.DateTimeField().to_representation(enrollment.modified),
            } for enrollment in sorted(enrollments, key=lambda enrollment: enrollment.order)
        ]
        self.assertEqual(response_data, expected)


class CourseEnrollmentDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:enrollment-detail'

    def setUp(self) -> None:
        self.user = UserFactory(is_staff=True)
        self.enrollment = EnrollmentFactory(is_active=True)

    def tearDown(self) -> None:
        self.enrollment.refresh_from_db()
        self.client.logout()

    def test_url(self):
        self.assertURLNameEqual(
            'enrollments/{pk}/', kwargs={'pk': 1}, base_url=settings.API_BASE_URL,
        )

    def test_disabled_enrollment(self):
        enrollment = EnrollmentFactory(is_active=False)
        self.client.force_login(user=self.user)
        self.detail_request(self.get_url(pk=enrollment.id), status_code=status.HTTP_404_NOT_FOUND, num_queries=3)

    def build_expected(self, enrollment):
        return {
            'id': enrollment.id,
            'course_id': enrollment.course_id,
            'survey_id': enrollment.survey_id,
            'enroll_type': enrollment.enroll_type,
            'name': enrollment.name,
            'summary': enrollment.summary,
            'options': enrollment.options,
            'is_default': enrollment.is_default,
            'created': DateTimeField().to_representation(enrollment.created),
            'modified': DateTimeField().to_representation(enrollment.modified),
        }

    def test_retrieve(self) -> None:
        self.client.force_login(user=self.user)

        expected = self.build_expected(self.enrollment)
        self.detail_request(self.get_url(pk=self.enrollment.id), expected=expected, num_queries=3)


class EnrolledUserCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-enroll'

    def setUp(self) -> None:
        self.user = UserFactory(is_active=True)
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual(
            'courses/{pk}/enroll/', kwargs={'pk': 1}, base_url=settings.API_BASE_URL,
        )

    def test_denied(self):
        data = {
            "enrollment_id": 1,
        }
        self.create_request(self.get_url(1), data=data, status_code=status.HTTP_403_FORBIDDEN, num_queries=0)

    def test_create(self):
        enrollment = EnrollmentFactory(
            course=self.course,
            enroll_type=Enrollment.TYPE_MANUAL,
            is_default=True,
            is_active=True,
        )
        self.client.force_login(user=self.user)

        def expected(response):
            return {
                'id': response.data['id'],
                'course_id': self.course.id,
                'enrollment': {
                    'id': enrollment.id,
                    'course_id': enrollment.course_id,
                    'survey_id': enrollment.survey_id,
                    'enroll_type': enrollment.enroll_type,
                    'name': enrollment.name,
                },
                'group_id': None,
                'survey_id': None,
                'status': EnrolledUser.StatusChoices.PENDING,
                'parameters': None,
                'created': serializers.DateTimeField().to_representation(response.data['created']),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        self.create_request(self.get_url(self.course.id), data={}, expected=expected, num_queries=21)

    def test_create_custom_enrollment(self):
        EnrollmentFactory(
            course=self.course,
            enroll_type=Enrollment.TYPE_MANUAL,
            is_default=True,
            is_active=True,
        )
        enrollment_custom = EnrollmentFactory(
            course=self.course,
            enroll_type=Enrollment.TYPE_MANUAL,
            is_default=False,
            is_active=True,
        )
        self.client.force_login(user=self.user)

        def expected(response):
            return {
                'id': response.data['id'],
                'course_id': self.course.id,
                'enrollment': {
                    'id': enrollment_custom.id,
                    'course_id': enrollment_custom.course_id,
                    'survey_id': enrollment_custom.survey_id,
                    'enroll_type': enrollment_custom.enroll_type,
                    'name': enrollment_custom.name,
                },
                'group_id': None,
                'survey_id': None,
                'status': EnrolledUser.StatusChoices.PENDING,
                'parameters': None,
                'created': serializers.DateTimeField().to_representation(response.data['created']),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        data = {
            'enrollment_id': enrollment_custom.id,
        }
        self.create_request(self.get_url(self.course.id), data=data, expected=expected, num_queries=21)

    def test_create_group(self):
        enrollment = EnrollmentFactory(
            course=self.course,
            enroll_type=Enrollment.TYPE_MANUAL,
            is_default=True,
            is_active=True,
        )
        course_group = CourseGroupFactory(
            course=self.course,
            is_active=True,
            can_join=True,
        )
        self.client.force_login(user=self.user)

        def expected(response):
            return {
                'id': response.data['id'],
                'course_id': self.course.id,
                'enrollment': {
                    'id': enrollment.id,
                    'course_id': enrollment.course_id,
                    'survey_id': enrollment.survey_id,
                    'enroll_type': enrollment.enroll_type,
                    'name': enrollment.name,
                },
                'group_id': course_group.id,
                'survey_id': None,
                'status': EnrolledUser.StatusChoices.PENDING,
                'parameters': None,
                'created': serializers.DateTimeField().to_representation(response.data['created']),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        data = {
            'group_id': course_group.id,
        }
        self.create_request(self.get_url(self.course.id), data=data, expected=expected, num_queries=26)

    def test_create_wrong_group(self):
        _ = EnrollmentFactory(
            course=self.course,
            enroll_type=Enrollment.TYPE_MANUAL,
            is_default=True,
            is_active=True,
        )
        _ = CourseGroupFactory(
            course=self.course,
            is_active=True,
            can_join=True,
        )
        wrong_group = CourseGroupFactory(is_active=True, can_join=True)
        self.client.force_login(user=self.user)

        data = {
            'group_id': wrong_group.id,
        }
        self.create_request(
            url=self.get_url(self.course.id),
            data=data,
            status_code=status.HTTP_400_BAD_REQUEST,
            num_queries=6,
            # TODO:
            # check_errors=True,
            # expected={
            #     'group': [{
            #         'code': 'invalid',
            #         '__str__': 'Группа не найдена в текущем курсе',
            #     }]
            # }
        )

    def test_not_available_for_enroll(self):
        EnrollmentFactory(
            course=self.course,
            enroll_type=Enrollment.TYPE_MANUAL,
            is_default=True,
            is_active=True,
        )
        self.group = CourseGroupFactory(
            course=self.course,
            is_active=True,
            can_join=False,
        )

        self.client.force_login(user=self.user)

        data = {
            'group_id': self.group.id,
        }
        self.create_request(
            url=self.get_url(self.course.id),
            data=data,
            status_code=status.HTTP_400_BAD_REQUEST,
            num_queries=4,
            # TODO
            # check_errors=True,
            # expected=[{
            #     'code': 'invalid',
            #     '__str__': 'курс недоступен для записи',
            # }],
        )

    def test_course_not_visible(self):
        EnrollmentFactory(
            course=self.course,
            enroll_type=Enrollment.TYPE_MANUAL,
            is_default=True,
            is_active=True,
        )
        self.group = CourseGroupFactory(
            course=self.course,
            is_active=True,
            can_join=True,
        )
        CourseVisibilityFactory(
            course=self.course,
            rules={"eq": ["staff_office", 1]}
        )

        self.client.force_login(user=self.user)

        data = {
            'group_id': self.group.id,
        }
        self.create_request(
            url=self.get_url(self.course.id),
            data=data,
            status_code=status.HTTP_400_BAD_REQUEST,
            num_queries=5,
            # TODO
            # check_errors=True,
            # expected=[{
            #     'code': 'invalid',
            #     '__str__': 'курс недоступен для пользователя'
            # }]
        )

    def test_create_group_required(self):
        EnrollmentFactory(
            course=self.course,
            enroll_type=Enrollment.TYPE_MANUAL,
            is_default=True,
            is_active=True,
        )
        CourseGroupFactory(
            course=self.course,
            is_active=True,
            can_join=True,
        )
        self.client.force_login(user=self.user)
        self.create_request(
            url=self.get_url(self.course.id),
            data={},
            status_code=status.HTTP_400_BAD_REQUEST,
            num_queries=5,
            # TODO
            # check_errors=True,
            # expected={
            #     'group': [{
            #         'code': 'invalid',
            #         '__str__': 'Для курса с группами должна быть указана группа',
            #     }]
            # }
        )
        self.create_request(
            url=self.get_url(self.course.id),
            data={},
            status_code=status.HTTP_400_BAD_REQUEST,
            num_queries=5,
            # TODO
            # check_errors=True,
            # expected={
            #     'group': [{
            #         'code': 'invalid',
            #         '__str__': 'Для курса с группами должна быть указана группа',
            #     }]
            # }
        )

    def test_create_not_available_by_visibility(self):
        EnrollmentFactory(
            course=self.course,
            enroll_type=Enrollment.TYPE_MANUAL,
            is_default=True,
            is_active=True,
        )
        city = StaffCityFactory()
        CourseVisibilityFactory(
            course=self.course,
            rules={"eq": ["staff_city", city.id]}
        )

        self.client.force_login(user=self.user)

        self.create_request(
            url=self.get_url(self.course.id),
            data={},
            status_code=status.HTTP_400_BAD_REQUEST,
            num_queries=4,
            # TODO
            # check_errors=True,
            # expected=[{
            #     'code': 'invalid',
            #     '__str__': 'курс недоступен для пользователя',
            # }],
        )


class EnrolledUserCreateWithSurveyTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-enroll'

    def setUp(self) -> None:
        self.student = UserFactory(is_active=True)
        self.course = CourseFactory(is_active=True)

    def test_create_survey(self):
        survey = EnrollSurveyFactory(is_active=True)
        survey_field_text = EnrollSurveyFieldFactory(
            survey=survey,
            field_type=EnrollSurveyField.TYPE_TEXT,
            parameters={'max_length': 255},
        )
        survey_field_textarea = EnrollSurveyFieldFactory(
            survey=survey,
            field_type=EnrollSurveyField.TYPE_TEXTAREA,
            parameters={'max_length': 255},
        )
        survey_field_email = EnrollSurveyFieldFactory(
            survey=survey,
            field_type=EnrollSurveyField.TYPE_EMAIL,
            parameters={'max_length': 255},
        )
        survey_field_url = EnrollSurveyFieldFactory(
            survey=survey,
            field_type=EnrollSurveyField.TYPE_URL,
            parameters={'max_length': 255},
        )
        survey_field_number = EnrollSurveyFieldFactory(
            survey=survey,
            field_type=EnrollSurveyField.TYPE_NUMBER,
            parameters={'min_value': 10, 'max_value': 20},
        )
        select_options = [
            {'value': 'qwerty', 'content': 'asdfg'},
            {'value': 'ytrewq', 'content': 'gfdsa'},
        ]
        select_options_keys = [i['value'] for i in select_options]
        survey_field_select = EnrollSurveyFieldFactory(
            survey=survey,
            field_type=EnrollSurveyField.TYPE_SELECT,
            parameters={'options': select_options},
        )
        multicheckbox_options = [
            [
                {'value': 'qwerty', 'content': 'asdfg'},
                {'value': 'ytrewq', 'content': 'gfdsa'},
            ],
            [
                {'value': 'uiop', 'content': 'hjkl'},
                {'value': 'poiu', 'content': 'lkjh'},
            ],
        ]
        multicheckbox_options_keys = list(chain(*[[i['value'] for i in j] for j in multicheckbox_options]))
        survey_field_multicheckbox = EnrollSurveyFieldFactory(
            survey=survey,
            field_type=EnrollSurveyField.TYPE_MULTICHECKBOX,
            parameters={'options': multicheckbox_options},
        )

        enrollment = EnrollmentFactory(
            course=self.course,
            survey=survey,
            enroll_type=Enrollment.TYPE_MANUAL,
        )
        self.client.force_login(user=self.student)

        data = {
            'survey_data': {
                survey_field_text.name: fake.pystr(max_chars=255),
                survey_field_textarea.name: fake.pystr(max_chars=255),
                survey_field_email.name: fake.email(),
                survey_field_url.name: fake.url(),
                survey_field_number.name: fake.pyint(min_value=10, max_value=20),
                survey_field_select.name: fake.random_element(elements=select_options_keys),
                survey_field_multicheckbox.name:
                    fake.random_elements(elements=multicheckbox_options_keys, length=2, unique=True),
            }
        }
        assign_perm('courses.change_course', self.student, self.course)
        with self.assertNumQueries(24):
            response = self.client.post(self.get_url(self.course.id), data=data, format='json')

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        enrolled_user = EnrolledUser.objects.get(
            user=self.student,
            course=self.course,
        )

        expected_data = data['survey_data']
        self.assertEqual(
            enrolled_user.survey_data,
            expected_data,
            msg=f'survey_data={enrolled_user.survey_data}, expected_data={expected_data}'
        )

        expected = {
            'id': enrolled_user.id,
            'course_id': self.course.id,
            'enrollment': {
                'id': enrollment.id,
                'course_id': enrollment.course_id,
                'survey_id': enrollment.survey_id,
                'enroll_type': enrollment.enroll_type,
                'name': enrollment.name,
            },
            'group_id': None,
            'survey_id': survey.id,
            'status': EnrolledUser.StatusChoices.PENDING,
            'parameters': None,
            'created': serializers.DateTimeField().to_representation(enrolled_user.created),
            'modified': serializers.DateTimeField().to_representation(enrolled_user.modified),
        }
        self.assertEqual(response.data, expected, msg=response.data)


class UserEnrollListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-enroll'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.url = self.get_url()

        self.category = CourseCategoryFactory()
        self.course = CourseFactory(is_active=True)
        self.course.categories.add(self.category)

        self.enrollment = EnrollmentFactory(course=self.course, enroll_type=Enrollment.TYPE_TRACKER)

        self.enrolled_user = EnrolledUserFactory(
            user=self.user,
            course=self.course,
            enrollment=self.enrollment,
        )
        self.enrolled_user.enroll()
        self.enrolled_user.refresh_from_db()

        self.default_issue = EnrolledUserTrackerIssueFactory(enrolled_user=self.enrolled_user, is_default=True)

    def test_url(self):
        self.assertURLNameEqual(
            'my/enroll/', base_url=settings.API_BASE_URL,
        )

    def build_expected(self, enrolled_user, issue, category):
        student = enrolled_user.course_student
        return [{
            'id': enrolled_user.id,
            'enrollment_id': enrolled_user.enrollment_id,
            'group_id': enrolled_user.group_id,
            'status': enrolled_user.course_status,
            'name': enrolled_user.course_name,
            'begin_date': enrolled_user.begin_date,
            'end_date': enrolled_user.end_date,
            'score': 0,
            'course_id': enrolled_user.course.id,
            'course': {
                'id': enrolled_user.course.id,
                'slug': enrolled_user.course.slug,
                'name': enrolled_user.course.name,
                'num_hours': enrolled_user.course.num_hours,
                'format': enrolled_user.course.format,
                'categories': [category.id],
                'type': enrolled_user.course.course_type,
                'structure': enrolled_user.course.structure,
                'is_active': enrolled_user.course.is_active,
                'is_archive': enrolled_user.course.is_archive,
            },
            'student': None if not student else {
                'id': student.id,
                'status': student.status,
                'is_passed': student.is_passed,
                'score': None if not student.course_progresses.all() else student.course_progresses.first().score,
            },
            'issues': [{
                'issue_key': issue.issue_key,
                'issue_status': issue.issue_status,
                'status': issue.status,
                'is_default': issue.is_default,
            }] if issue else [],
            'created': serializers.DateTimeField().to_representation(enrolled_user.created),
            'modified': serializers.DateTimeField().to_representation(enrolled_user.modified),
        }]

    def test_list(self):
        EnrolledUserTrackerIssueFactory(enrolled_user=self.enrolled_user, is_default=False)

        expected = self.build_expected(self.enrolled_user, self.default_issue, self.category)

        self.client.force_login(user=self.user)
        self.list_request(self.get_url(), expected=expected, num_queries=7)

    def test_score_no_course_student(self):
        StudentCourseProgressFactory.create_batch(11, course=self.course)
        user = UserFactory(is_active=True)
        enrolled_user = EnrolledUserFactory(user=user, course=self.course, enrollment=self.enrollment)
        default_issue = EnrolledUserTrackerIssueFactory(enrolled_user=enrolled_user, is_default=True)

        expected = self.build_expected(enrolled_user, default_issue, self.category)
        expected[0]['score'] = 0

        self.client.force_login(user=user)
        self.list_request(self.get_url(), expected=expected, num_queries=6)

    def test_score_no_student_course_progress(self):
        StudentCourseProgressFactory.create_batch(11, course=self.course)
        expected = self.build_expected(self.enrolled_user, self.default_issue, self.category)
        expected[0]['score'] = 0

        self.client.force_login(user=self.user)
        self.list_request(self.url, expected=expected, num_queries=7)

    def test_score_with_student_course_progress(self):
        StudentCourseProgressFactory(student=self.enrolled_user.course_student, course=self.course, score=27)
        StudentCourseProgressFactory.create_batch(11, course=self.course)
        expected = self.build_expected(self.enrolled_user, self.default_issue, self.category)
        expected[0]['score'] = 27

        self.client.force_login(user=self.user)
        self.list_request(self.url, expected=expected, num_queries=7)

    def test_ordering(self):
        user = UserFactory()
        now = timezone.now()

        enrolled_user_0h = EnrolledUserFactory(user=user, modified=now)
        enrolled_user_2h = EnrolledUserFactory(user=user, modified=now - timedelta(hours=2))

        course_student = CourseStudentFactory(user=user, modified=now - timedelta(hours=1))
        enrolled_user_with_student_1h = EnrolledUserFactory(
            user=user,
            course_student=course_student,
            course=course_student.course
        )

        course_student = CourseStudentFactory(user=user, modified=now - timedelta(hours=3))
        enrolled_user_with_student_3h = EnrolledUserFactory(
            user=user,
            course_student=course_student,
            course=course_student.course
        )

        enrolled_user_order = [
            enrolled_user_0h,
            enrolled_user_with_student_1h,
            enrolled_user_2h,
            enrolled_user_with_student_3h,
        ]

        expected = [
            {'id': enrolled_user.id} for enrolled_user in enrolled_user_order
        ]

        self.client.force_login(user=user)
        self.list_request(self.url, only_ids=True, expected=expected, num_queries=7)

    def test_empty(self):
        user = UserFactory()
        self.client.force_login(user=user)
        self.list_request(self.url, only_ids=True, expected=[], num_queries=3)

    def test_user_filtering(self):
        EnrolledUserFactory()

        self.client.force_login(user=self.user)
        self.list_request(self.url, only_ids=True, expected=[{"id": self.enrolled_user.id}], num_queries=7)


class MyCoursesListTestCase(UserEnrollListTestCase):
    URL_NAME = 'api:my-courses'

    def test_url(self):
        self.assertURLNameEqual(
            'my/courses/', base_url=settings.API_BASE_URL,
        )


class UserEnrollListFilterTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-enroll'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.url = self.get_url()

        self.pending = EnrolledUserFactory(user=self.user)

        self.active = EnrolledUserFactory(user=self.user)
        self.active.enroll()
        self.active.refresh_from_db()

        self.active_passed = EnrolledUserFactory(user=self.user)
        self.active_passed.enroll()
        self.active_passed.refresh_from_db()
        self.active_passed.course_student.is_passed = True
        self.active_passed.course_student.save()
        self.active_passed.refresh_from_db()

        self.rejected = EnrolledUserFactory(user=self.user)
        self.rejected.reject()
        self.rejected.refresh_from_db()

        self.completed = EnrolledUserFactory(user=self.user)
        self.completed.enroll()
        self.completed.refresh_from_db()
        self.completed.reject()
        self.completed.complete()
        self.completed.refresh_from_db()

        self.expelled = EnrolledUserFactory(user=self.user)
        self.expelled.enroll()
        self.expelled.refresh_from_db()
        self.expelled.reject()
        self.expelled.refresh_from_db()

    def build_expected(self, enrolled_users):
        return [{
            'id': eu.id,
            'enrollment_id': eu.enrollment_id,
            'group_id': eu.group_id,
            'status': eu.course_status,
            'name': eu.course_name,
            'begin_date': eu.begin_date,
            'end_date': eu.end_date,
            'score': 0,
            'course_id': eu.course.id,
            'course': {
                'id': eu.course.id,
                'slug': eu.course.slug,
                'name': eu.course.name,
                'num_hours': eu.course.num_hours,
                'format': eu.course.format,
                'categories': [],
                'type': eu.course.course_type,
                'structure': eu.course.structure,
                'is_active': eu.course.is_active,
                'is_archive': eu.course.is_archive,
            },
            'student': None if not eu.course_student else {
                'id': eu.course_student.id,
                'status': eu.course_student.status,
                'is_passed': eu.course_student.is_passed,
                'score': None,
            },
            'issues': [],
            'created': serializers.DateTimeField().to_representation(eu.created),
            'modified': serializers.DateTimeField().to_representation(eu.modified),
        } for eu in enrolled_users]

    def test_single_status_pending(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.pending])
        self.list_request(self.url + '?status=pending', expected=expected, num_queries=6)

    def test_single_status_rejected(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.rejected])
        self.list_request(self.url + '?status=rejected', expected=expected, num_queries=6)

    def test_single_status_active(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.active])
        self.list_request(self.url + '?status=active', expected=expected, num_queries=7)

    def test_single_status_enrolled(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.active_passed, self.active])
        self.list_request(self.url + '?status=enrolled', expected=expected, num_queries=7)

    def test_single_status_completed(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.completed, self.active_passed])
        self.list_request(self.url + '?status=completed', expected=expected, num_queries=7)

    def test_single_status_expelled(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.expelled])
        self.list_request(self.url + '?status=expelled', expected=expected, num_queries=7)

    def test_single_status_invalid(self):
        self.client.force_login(self.user)

        self.detail_request(
            self.url + '?status=invalid_status',
            status_code=status.HTTP_400_BAD_REQUEST,
            num_queries=2
        )

    def test_pending(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.pending])
        self.list_request(self.url + '?statuses=pending', expected=expected, num_queries=6)

    def test_rejected(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.rejected])
        self.list_request(self.url + '?statuses=rejected', expected=expected, num_queries=6)

    def test_active(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.active])
        self.list_request(self.url + '?statuses=active', expected=expected, num_queries=7)

    def test_enrolled(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.active_passed, self.active])
        self.list_request(self.url + '?statuses=enrolled', expected=expected, num_queries=7)

    def test_completed(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.completed, self.active_passed])
        self.list_request(self.url + '?statuses=completed', expected=expected, num_queries=7)

    def test_expelled(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.expelled])
        self.list_request(self.url + '?statuses=expelled', expected=expected, num_queries=7)

    def test_invalid_status(self):
        self.client.force_login(self.user)

        self.detail_request(
            self.url + '?statuses=invalid_status',
            status_code=status.HTTP_400_BAD_REQUEST,
            num_queries=2
        )

    def test_two_enrolled_user_statuses_no_rejected(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.active_passed, self.active, self.pending])
        self.list_request(self.url + '?statuses=pending&statuses=enrolled', expected=expected, num_queries=7)

    def test_enrolled_user_status_with_rejected(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.rejected, self.pending])
        self.list_request(self.url + '?statuses=pending&statuses=rejected', expected=expected, num_queries=6)

    def test_course_student_status_with_rejected(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.rejected, self.active])
        self.list_request(self.url + '?statuses=active&statuses=rejected', expected=expected, num_queries=7)

    def test_both_course_student_and_enrolled_user_statuses_no_rejected(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.active, self.pending])
        self.list_request(self.url + '?statuses=pending&statuses=active', expected=expected, num_queries=7)

    def test_both_course_student_and_enrolled_user_statuses_with_rejected(self):
        self.client.force_login(self.user)

        expected = self.build_expected([self.rejected, self.active, self.pending])
        self.list_request(
            self.url + '?statuses=pending&statuses=active&statuses=rejected',
            expected=expected,
            num_queries=7
        )


class MyCoursesDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-enroll-detail'

    def setUp(self) -> None:
        self.user = UserFactory(is_active=True)

        self.category = CourseCategoryFactory()
        self.course = CourseFactory(is_active=True)
        self.course.categories.add(self.category)

        self.enrollment = EnrollmentFactory(course=self.course, enroll_type=Enrollment.TYPE_INSTANT)
        self.course_student = CourseStudentFactory(course=self.course, user=self.user)
        self.enrolled_user = EnrolledUserFactory(
            user=self.user,
            course=self.course,
            enrollment=self.enrollment,
            course_student=self.course_student,
        )
        self.enrolled_user.enroll()
        self.enrolled_user.refresh_from_db()

    def test_url(self):
        self.assertURLNameEqual(
            'my/enroll/{pk}/', kwargs={'pk': 1}, base_url=settings.API_BASE_URL,
        )

    def test_anonymous(self):
        with self.assertNumQueries(0):
            response = self.client.get(self.get_url(pk=1), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def build_expected(self, enrolled_user: EnrolledUser):
        course = enrolled_user.course
        student = enrolled_user.course_student
        return {
            'id': enrolled_user.id,
            'enrollment_id': enrolled_user.enrollment_id,
            'group_id': enrolled_user.group_id,
            'status': enrolled_user.course_status,
            'name': enrolled_user.course_name,
            'begin_date': enrolled_user.begin_date,
            'end_date': enrolled_user.end_date,
            'score': 0,
            'course_id': course.id,
            'course': {
                'id': course.id,
                'slug': course.slug,
                'name': course.name,
                'num_hours': course.num_hours,
                'format': course.format,
                'categories': list(course.categories.values_list('pk', flat=True)),
                'type': course.course_type,
                'structure': course.structure,
                'is_active': course.is_active,
                'is_archive': course.is_archive,
            },
            'student': {
                'id': student.id,
                'status': student.status,
                'is_passed': student.is_passed,
                'score': None,
            },
            'issues': [],
            'created': serializers.DateTimeField().to_representation(enrolled_user.created),
            'modified': serializers.DateTimeField().to_representation(enrolled_user.modified),
        }

    def test_retrieve(self):
        expected = self.build_expected(self.enrolled_user)
        self.client.force_login(user=self.user)
        self.detail_request(self.get_url(self.enrolled_user.id), expected=expected, num_queries=6)
