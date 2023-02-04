from datetime import timedelta
from unittest.mock import MagicMock, patch

import faker

from django.contrib.auth import get_user_model
from django.core.exceptions import ValidationError
from django.test import TestCase, TransactionTestCase
from django.utils import timezone

from lms.courses.models import Course, CourseGroup, CourseStudent
from lms.courses.tests.factories import CourseFactory, CourseGroupFactory
from lms.tracker.models import EnrollmentTrackerIssue
from lms.tracker.tests.factories import EnrollmentTrackerQueueFactory
from lms.users.tests.factories import UserFactory

from ..models import EnrolledUser, Enrollment, EnrollSurveyField
from .factories import (
    EnrolledUserFactory, EnrollmentFactory, EnrollSurveyFactory, EnrollSurveyFieldFactory, TrackerEnrollmentFactory,
)

User = get_user_model()

fake = faker.Faker()


class EnrolledUserStatusModelTestCase(TransactionTestCase):
    def setUp(self) -> None:
        self.user: User = UserFactory()
        self.author = UserFactory()
        self.course = CourseFactory(author=self.author)

    def test_instant(self):
        self.assertFalse(self.user.enrolled_to.exists())
        enrollment = EnrollmentFactory(
            course=self.course,
            enroll_type=Enrollment.TYPE_INSTANT,
        )
        with self.assertNumQueries(20):
            enrolled_user = EnrolledUserFactory(
                course=self.course,
                enrollment=enrollment,
                user=self.user,
            )

        self.assertEqual(enrolled_user.status, EnrolledUser.StatusChoices.ENROLLED)

    def test_manual(self):
        self.assertFalse(self.user.enrolled_to.exists())
        enrollment = EnrollmentFactory(
            course=self.course,
            enroll_type=Enrollment.TYPE_MANUAL,
        )
        with self.assertNumQueries(10):
            enrolled_user = EnrolledUserFactory(
                course=self.course,
                enrollment=enrollment,
                user=self.user,
            )

        self.assertEqual(enrolled_user.status, EnrolledUser.StatusChoices.PENDING)

    def test_tracker(self):
        self.assertFalse(self.user.enrolled_to.exists())
        enrollment = TrackerEnrollmentFactory(course=self.course)
        queue1 = EnrollmentTrackerQueueFactory(enrollment=enrollment)

        mocked_issues = MagicMock()
        create = MagicMock()
        create.key = f'{queue1.name}-42'
        create.status.key = 'opened'
        mocked_issues.create.return_value = create
        mocked_issues.find.return_value = []
        with patch('lms.tracker.services.startrek_api.issues', new=mocked_issues):
            with self.assertNumQueries(20):
                enrolled_user = EnrolledUser(
                    course=self.course,
                    enrollment=enrollment,
                    user=self.user,
                )
                enrolled_user.save()

        self.assertEqual(enrolled_user.status, EnrolledUser.StatusChoices.PENDING)

        self.assertEqual(
            EnrollmentTrackerIssue.objects.filter(
                queue=queue1,
                enrolled_user=enrolled_user,
                status='opened',
                issue_number=42,
            ).count(),
            1,
        )


class EnrolledUserCleanTestCase(TestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.user = UserFactory()
        self.enrollment = EnrollmentFactory(course=self.course)

    def test_group_not_required_for_course_without_groups(self):
        enrolled_user = EnrolledUserFactory.build(course=self.course, user=self.user, enrollment=self.enrollment)
        enrolled_user.save()

    def test_group_required_for_course_with_groups(self):
        group = CourseGroupFactory(course=self.course)
        self.course.refresh_from_db()
        enrolled_user = EnrolledUserFactory.build(course=self.course, user=self.user, enrollment=self.enrollment)

        with self.assertRaises(ValidationError):
            enrolled_user.save()

        enrolled_user.group = group
        enrolled_user.save()

    def test_group_from_the_same_group(self):
        group = CourseGroupFactory(course=self.course)
        other_group = CourseGroupFactory()
        enrolled_user = EnrolledUserFactory.build(
            course=self.course, user=self.user, enrollment=self.enrollment, group=other_group,
        )

        with self.assertRaises(ValidationError):
            enrolled_user.save()

        enrolled_user.group = group
        enrolled_user.save()

    def test_already_exists_without_groups(self):
        enrolled_user0 = EnrolledUserFactory(course=self.course, user=self.user, enrollment=self.enrollment)
        enrolled_user1 = EnrolledUserFactory.build(course=self.course, user=self.user, enrollment=self.enrollment)
        with self.assertRaises(ValidationError):
            enrolled_user1.save()

        enrolled_user0.delete()
        enrolled_user1.save()

    def test_already_exists_with_groups(self):
        group1 = CourseGroupFactory(course=self.course)
        EnrolledUserFactory(course=self.course, user=self.user, enrollment=self.enrollment, group=group1)

        enrolled_user1 = EnrolledUserFactory.build(
            course=self.course, user=self.user, enrollment=self.enrollment, group=group1,
        )
        with self.assertRaises(ValidationError):
            enrolled_user1.save()

        group2 = CourseGroupFactory(course=self.course)
        enrolled_user2 = EnrolledUserFactory.build(
            course=self.course, user=self.user, enrollment=self.enrollment, group=group2,
        )
        with self.assertRaises(ValidationError):
            enrolled_user2.save()

    def test_course_registration_not_started(self):
        now_monent = timezone.now()
        course = CourseFactory(enroll_begin=now_monent + timedelta(days=1))
        with self.assertRaises(ValidationError):
            EnrolledUserFactory(course=course)

    def test_group_registration_not_started(self):
        now_monent = timezone.now()
        course = CourseFactory(enroll_begin=now_monent - timedelta(days=1))
        group = CourseGroupFactory(course=course, enroll_begin=now_monent + timedelta(days=1))
        with self.assertRaises(ValidationError):
            EnrolledUserFactory(course=course, group=group)

    def test_course_registration_has_finished(self):
        now_monent = timezone.now()
        course = CourseFactory(enroll_end=now_monent - timedelta(days=1))
        with self.assertRaises(ValidationError):
            EnrolledUserFactory(course=course)

    def test_group_registration_has_finished(self):
        now_monent = timezone.now()
        course = CourseFactory(enroll_end=now_monent + timedelta(days=1))
        group = CourseGroupFactory(course=course, enroll_end=now_monent - timedelta(days=1))
        with self.assertRaises(ValidationError):
            EnrolledUserFactory(course=course, group=group)

    def test_group_is_full(self):
        group = CourseGroupFactory(course=self.course, max_participants=1)
        EnrolledUserFactory(course_id=self.course.id, enrollment_id=self.enrollment.id, group_id=group.id)
        with self.assertRaises(ValidationError):
            EnrolledUserFactory(course_id=self.course.id, group_id=group.id)

    def test_group_not_available(self):
        group = CourseGroupFactory(course=self.course, can_join=False)
        with self.assertRaises(ValidationError):
            EnrolledUserFactory(course=self.course, group=group)

    def test_simultaneous_enrollment(self):
        self.course.multi_enrollments = True
        group = CourseGroupFactory(course=self.course, max_participants=2)
        with self.assertRaises(ValidationError):
            EnrolledUserFactory.create_batch(
                size=5,
                course=self.course,
                user=self.user,
                enrollment=self.enrollment,
                group=group,
            )
        group.refresh_from_db()
        self.assertEqual(group.num_participants, 2)

    def test_multiple_enrollments(self):
        course = CourseFactory(course_type=Course.TypeChoices.TRACK)
        enrollment = EnrollmentFactory(course=course)
        with self.assertRaises(ValidationError):
            EnrolledUserFactory.create_batch(
                size=2,
                course=course,
                user=self.user,
                enrollment=enrollment,
            )

        self.assertEqual(
            EnrolledUser.objects.filter(
                course=course,
                user=self.user,
                enrollment=enrollment,
            ).count(),
            1,
        )


class EnrollSurveyFieldValidationModelTestCase(TestCase):
    def setUp(self):
        self.survey = EnrollSurveyFactory()

    def assert_valid(self, field_type, params):
        survey_field = EnrollSurveyFieldFactory.build(survey=self.survey, field_type=field_type)
        survey_field.parameters = params
        with self.assertNumQueries(2):
            survey_field.save()

    def assert_invalid(self, field_type, params):
        survey_field = EnrollSurveyFieldFactory.build(survey=self.survey, field_type=field_type)
        survey_field.parameters = params
        with self.assertNumQueries(0):
            with self.assertRaises(ValidationError):
                survey_field.save()

    def test_max_length_validation(self):
        invalid_parameters = [
            None,
            [],
            {'max_length': '255'}
        ]

        valid_parameters = [
            {},
            {'max_length': 255},
        ]

        field_types = [
            EnrollSurveyField.TYPE_TEXT,
            EnrollSurveyField.TYPE_TEXTAREA,
            EnrollSurveyField.TYPE_URL,
            EnrollSurveyField.TYPE_EMAIL,
        ]

        for field_type in field_types:
            for invalid_parameters_item in invalid_parameters:
                self.assert_invalid(field_type, invalid_parameters_item)
            for valid_parameters_item in valid_parameters:
                self.assert_valid(field_type, valid_parameters_item)

    def test_min_max_value_validation(self):
        invalid_parameters = [
            None,
            [],
            {'min_value': '255', 'max_value': '255'},
            {'min_value': 2, 'max_value': 1},
        ]

        valid_parameters = [
            {},
            {'min_value': 1},
            {'max_value': 2},
            {'min_value': 1, 'max_value': 2},
        ]

        field_type = EnrollSurveyField.TYPE_NUMBER
        for invalid_parameters_item in invalid_parameters:
            self.assert_invalid(field_type, invalid_parameters_item)
        for valid_parameters_item in valid_parameters:
            self.assert_valid(field_type, valid_parameters_item)

    def test_select_validation(self):
        invalid_parameters = [
            None,
            [],
            {},
            {'options': []},
            {'options': [{"value": "qwerty", "content": "asdfgh"}]},
            {'options': [{"value": "qwerty"}, {"value": "qwerty", "content": "asdfgh"}]},
            {'options': [{"content": "asdfgh"}, {"value": "qwerty", "content": "asdfgh"}]},
            {'options': [{"value": "qwerty", "content": 1}, {"value": "qwerty", "content": "asdfgh"}]},
            {'options': [{"value": 1, "content": "asdfgh"}, {"value": "qwerty", "content": "asdfgh"}]},
        ]

        valid_parameters = [
            {"options": [
                {"value": "qwerty", "content": "asdfg"},
                {"value": "ytrewq", "content": "gfdsa"},
            ]},
            {'options': [
                {"value": "qwerty", "content": "asdfgh"},
                {"value": "uiop", "content": "jkl"},
                {"value": "zxcv", "content": "bnm"},
            ]},
        ]

        field_type = EnrollSurveyField.TYPE_SELECT
        for invalid_parameters_item in invalid_parameters:
            self.assert_invalid(field_type, invalid_parameters_item)
        for valid_parameters_item in valid_parameters:
            self.assert_valid(field_type, valid_parameters_item)

    def test_multicheckbox_validation(self):
        invalid_parameters = [
            None,
            [],
            {},
            {'options': []},
            {'options': [[]]},
            {'options': [[{"value": "qwerty", "content": "asdfgh"}]]},
            {'options': [[], []]},
            {'options': [[{"value": "qwerty", "content": "asdfgh"}], []]},
            {'options': [[{"value": "qwerty", "content": "asdfgh"}], [{"value": "qwerty"}]]},
            {'options': [[{"value": "qwerty", "content": "asdfgh"}], [{"content": "asdfgh"}]]},
            {'options': [[{"value": "qwerty", "content": "asdfgh"}], [{"value": "qwerty", "content": 1}]]},
            {'options': [[{"value": "qwerty", "content": "asdfgh"}], [{"value": 1, "content": "asdfgh"}]]},
        ]

        valid_parameters = [
            {"options": [
                [{"value": "qwerty", "content": "asdfg"}],
                [{"value": "ytrewq", "content": "gfdsa"}],
            ]},
            {'options': [
                [{"value": "qwerty", "content": "asdfgh"}, {"value": "ytrewq", "content": "jhgfdsa"}],
                [{"value": "uiop", "content": "jkl"}, {"value": "poiu", "content": "lkj"}],
                [{"value": "zxcv", "content": "bnm"}, {"value": "vcxz", "content": "mnb"}],
            ]},
        ]

        field_type = EnrollSurveyField.TYPE_MULTICHECKBOX
        for invalid_parameters_item in invalid_parameters:
            self.assert_invalid(field_type, invalid_parameters_item)
        for valid_parameters_item in valid_parameters:
            self.assert_valid(field_type, valid_parameters_item)


class EnrolledUserCourseGroupParticipantsTestCase(TestCase):
    def setUp(self) -> None:
        self.user = UserFactory()
        self.course: Course = CourseFactory(is_active=True, enable_groups=True, enrollments_only=True)

        with self.assertNumQueries(10):
            self.course_group: CourseGroup = CourseGroupFactory(course=self.course, max_participants=3)

        self.enrollment = EnrollmentFactory(course=self.course, enroll_type=Enrollment.TYPE_MANUAL)
        self.course.refresh_from_db()

    def refresh_data_from_db(self):
        self.course.refresh_from_db()
        self.course_group.refresh_from_db()

    def test_reject_enrolled_user(self):
        self.assertEqual(self.course_group.num_participants, 0)
        self.assertEqual(self.course.occupancy.maximum, 3)
        self.assertEqual(self.course.occupancy.current, 0)

        with self.assertNumQueries(23):
            enrolled_user = EnrolledUserFactory(course=self.course, user=self.user, group=self.course_group)
        self.refresh_data_from_db()

        self.assertEqual(self.course_group.num_participants, 1)
        self.assertEqual(self.course.occupancy.maximum, 3)
        self.assertEqual(self.course.occupancy.current, 1)

        with self.assertNumQueries(13):
            enrolled_user.status = EnrolledUser.StatusChoices.REJECTED
            enrolled_user.save()

        self.refresh_data_from_db()

        self.assertEqual(self.course_group.num_participants, 0)
        self.assertEqual(self.course.occupancy.maximum, 3)
        self.assertEqual(self.course.occupancy.current, 0)

    def test_delete_enrolled_user(self):
        self.assertEqual(self.course_group.num_participants, 0)
        self.assertEqual(self.course.occupancy.maximum, 3)
        self.assertEqual(self.course.occupancy.current, 0)

        with self.assertNumQueries(23):
            enrolled_user = EnrolledUserFactory(course=self.course, user=self.user, group=self.course_group)
        self.refresh_data_from_db()

        self.assertEqual(self.course_group.num_participants, 1)
        self.assertEqual(self.course.occupancy.maximum, 3)
        self.assertEqual(self.course.occupancy.current, 1)

        with self.assertNumQueries(9):
            enrolled_user.delete()

        self.refresh_data_from_db()

        self.assertEqual(self.course_group.num_participants, 0)
        self.assertEqual(self.course.occupancy.maximum, 3)
        self.assertEqual(self.course.occupancy.current, 0)


class EnrolledUserCourseFlagsModelTestCase(TestCase):
    def test_not_enrollments_only(self):
        course = CourseFactory(enrollments_only=False)
        enrollment = EnrollmentFactory(course=course, enroll_type=Enrollment.TYPE_INSTANT)
        enrolled_user = EnrolledUserFactory(course=course, enrollment=enrollment)
        enrolled_user.refresh_from_db()

        self.assertEqual(
            CourseStudent.objects.get(
                course=enrolled_user.course,
                group=enrolled_user.group,
                user=enrolled_user.user,
                status=CourseStudent.StatusChoices.ACTIVE,
            ),
            enrolled_user.course_student,
        )

    def test_enrollments_only(self):
        course = CourseFactory(enrollments_only=True)
        enrollment = EnrollmentFactory(course=course, enroll_type=Enrollment.TYPE_INSTANT)
        enrolled_user = EnrolledUserFactory(course=course, enrollment=enrollment)
        enrolled_user.refresh_from_db()

        self.assertEqual(
            CourseStudent.objects.get(
                course=enrolled_user.course,
                group=enrolled_user.group,
                user=enrolled_user.user,
                status=CourseStudent.StatusChoices.ACTIVE,
            ),
            enrolled_user.course_student,
        )

    def test_not_multi_enrollments(self):
        course = CourseFactory(enrollments_only=True, multi_enrollments=False)
        enrollment = EnrollmentFactory(course=course, enroll_type=Enrollment.TYPE_INSTANT)
        user = UserFactory()
        EnrolledUserFactory(course=course, enrollment=enrollment, user=user)

        with self.assertRaises(ValidationError):
            EnrolledUserFactory(course=course, enrollment=enrollment, user=user)

    def test_multi_enrollments(self):
        user = UserFactory()
        course = CourseFactory(enrollments_only=True, multi_enrollments=True)
        enrollment = EnrollmentFactory(course=course, enroll_type=Enrollment.TYPE_INSTANT)

        enrolled_user1 = EnrolledUserFactory(course=course, user=user, enrollment=enrollment)
        enrolled_user1.refresh_from_db()
        self.assertIsNotNone(enrolled_user1.course_student)

        enrolled_user2 = EnrolledUserFactory(course=course, user=user, enrollment=enrollment)
        enrolled_user2.refresh_from_db()
        self.assertNotEqual(enrolled_user1.course_student.id, enrolled_user2.course_student.id)

    def test_multi_enrollments_with_groups(self):
        user = UserFactory()
        course = CourseFactory(enrollments_only=True, multi_enrollments=True)
        enrollment = EnrollmentFactory(course=course, enroll_type=Enrollment.TYPE_INSTANT)

        group1 = CourseGroupFactory(course=course)
        enrolled_user1 = EnrolledUserFactory(course=course, user=user, enrollment=enrollment, group=group1)
        enrolled_user1.refresh_from_db()
        self.assertIsNotNone(enrolled_user1.course_student)

        # Можно записаться в ту же группу
        enrolled_user2 = EnrolledUserFactory(course=course, user=user, enrollment=enrollment, group=group1)
        enrolled_user2.refresh_from_db()
        self.assertNotEqual(enrolled_user1.course_student.id, enrolled_user2.course_student.id)

        # Можно записаться в другую группу
        group2 = CourseGroupFactory(course=course)
        enrolled_user3 = EnrolledUserFactory(course=course, user=user, enrollment=enrollment, group=group2)
        enrolled_user3.refresh_from_db()
        self.assertNotEqual(enrolled_user1.course_student.id, enrolled_user3.course_student.id)
        self.assertNotEqual(enrolled_user2.course_student.id, enrolled_user3.course_student.id)

    def test_retries_not_allowed(self):
        user = UserFactory()
        course = CourseFactory(retries_allowed=False)
        enrollment = EnrollmentFactory(course=course, enroll_type=Enrollment.TYPE_INSTANT)

        enrolled_user1 = EnrolledUserFactory(course=course, enrollment=enrollment, user=user)
        enrolled_user1.refresh_from_db()
        enrolled_user1.complete()

        with self.assertRaises(ValidationError):
            EnrolledUserFactory(course=course, enrollment=enrollment, user=user)

    def test_retries_allowed(self):
        user = UserFactory()
        course = CourseFactory(retries_allowed=True)
        enrollment = EnrollmentFactory(course=course)
        enrolled_user1 = EnrolledUserFactory(course=course, user=user, enrollment=enrollment)

        enrolled_user2 = EnrolledUserFactory.build(course=course, user=user, enrollment=enrollment)
        with self.assertRaises(ValidationError):
            enrolled_user2.save()

        enrolled_user1.enroll()
        enrolled_user1.refresh_from_db()

        with self.assertRaises(ValidationError):
            enrolled_user2.save()

        enrolled_user1.complete()
        enrolled_user2.save()

    def test_retries_allowed_with_groups(self):
        user = UserFactory()
        course = CourseFactory(retries_allowed=True)
        enrollment = EnrollmentFactory(course=course)
        group1 = CourseGroupFactory(course=course)
        group2 = CourseGroupFactory(course=course)
        enrolled_user1 = EnrolledUserFactory(course=course, user=user, enrollment=enrollment, group=group1)

        # Пытаемся записаться в ту же группу
        enrolled_user2 = EnrolledUserFactory.build(course=course, user=user, enrollment=enrollment, group=group1)
        with self.assertRaises(ValidationError):
            enrolled_user2.save()

        enrolled_user1.enroll()
        enrolled_user1.refresh_from_db()

        with self.assertRaises(ValidationError):
            enrolled_user2.save()

        enrolled_user1.complete()
        enrolled_user2.save()

        # Пытаемся записаться в другую группу
        enrolled_user3 = EnrolledUserFactory.build(course=course, user=user, enrollment=enrollment, group=group2)
        with self.assertRaises(ValidationError):
            enrolled_user3.save()

        enrolled_user2.enroll()
        enrolled_user2.refresh_from_db()

        with self.assertRaises(ValidationError):
            enrolled_user3.save()

        enrolled_user2.complete()
        enrolled_user3.save()


class EnrolledUserBeginDateTestCase(TestCase):
    def setUp(self) -> None:
        self.now = timezone.now()
        self.yesterday = self.now - timedelta(days=1)

    def test_group_date(self):
        course = CourseFactory(begin_date=self.yesterday)
        group = CourseGroupFactory(course=course, begin_date=self.now)
        enrolled_user = EnrolledUserFactory(course=course, group=group)

        self.assertEqual(enrolled_user.begin_date, self.now)

    def test_course_date(self):
        course = CourseFactory(begin_date=self.now)
        group = CourseGroupFactory(course=course)
        enrolled_user = EnrolledUserFactory(course=course, group=group)

        self.assertEqual(enrolled_user.begin_date, self.now)

    def test_no_group(self):
        course = CourseFactory(begin_date=self.now)
        enrolled_user = EnrolledUserFactory(course=course)

        self.assertEqual(enrolled_user.begin_date, self.now)

    def test_no_begin_date(self):
        course = CourseFactory()
        enrolled_user = EnrolledUserFactory(course=course)

        self.assertIsNone(enrolled_user.begin_date)


class EnrolledUserMyCourseStatusTestCase(TestCase):
    def test_status_pending(self):
        enrolled_user = EnrolledUserFactory()

        self.assertEqual(enrolled_user.course_status, 'pending')

    def test_status_rejected(self):
        enrolled_user = EnrolledUserFactory()

        enrolled_user.reject()
        enrolled_user.refresh_from_db()

        self.assertEqual(enrolled_user.course_status, 'rejected')

    def test_status_active(self):
        enrolled_user = EnrolledUserFactory()

        enrolled_user.enroll()
        enrolled_user.refresh_from_db()

        self.assertEqual(enrolled_user.course_status, 'active')

    def test_status_completed(self):
        enrolled_user = EnrolledUserFactory()

        enrolled_user.enroll()
        enrolled_user.refresh_from_db()

        enrolled_user.complete()
        enrolled_user.refresh_from_db()

        self.assertEqual(enrolled_user.course_status, 'completed')

    def test_status_expelled(self):
        enrolled_user = EnrolledUserFactory()

        enrolled_user.enroll()
        enrolled_user.refresh_from_db()

        enrolled_user.reject()
        enrolled_user.refresh_from_db()

        self.assertEqual(enrolled_user.course_status, 'expelled')


class EnrolledUserCourseNameTemplateTestCase(TestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.user = UserFactory()

    def test_empty_template(self):
        enrollment = EnrollmentFactory(course=self.course)
        enrolled_user = EnrolledUserFactory(course=self.course, user=self.user, enrollment=enrollment)
        self.assertEqual(enrolled_user.generate_name(), '')
        self.assertEqual(enrolled_user.custom_course_name, '')
        self.assertEqual(enrolled_user.course_name, self.course.name)

    def test_simple_text(self):
        text = fake.word()
        enrollment = EnrollmentFactory(course=self.course, course_name_template=text)
        enrolled_user = EnrolledUserFactory(course=self.course, user=self.user, enrollment=enrollment)
        self.assertEqual(enrolled_user.generate_name(), text)
        self.assertEqual(enrolled_user.custom_course_name, text)
        self.assertEqual(enrolled_user.course_name, text)

    def test_valid_template(self):
        text = fake.word()
        group = CourseGroupFactory(course=self.course)
        survey_data = {fake.word(): fake.word() for _ in range(3)}

        expected = ", ".join((
            text, str(self.course.id), str(self.course.name), str(self.course.slug),
            str(group.id), str(group.name), *(str(survey_data[x]) for x in survey_data)
        ))
        template_fields = [
            "course.id", "course.name", "course.slug", "group.id", "group.name",
        ]
        template_fields.extend((f"survey.{x}" for x in survey_data))
        template = ", ".join((text, *[f"{{{{ {x} }}}}" for x in template_fields]))

        enrollment = EnrollmentFactory(course=self.course, course_name_template=template)
        enrolled_user = EnrolledUserFactory(course=self.course, user=self.user, enrollment=enrollment,
                                            group=group, survey_data=survey_data)
        self.assertEqual(enrolled_user.generate_name(), expected)
        self.assertEqual(enrolled_user.custom_course_name, expected)
        self.assertEqual(enrolled_user.course_name, expected)
