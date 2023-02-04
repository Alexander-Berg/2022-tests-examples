from datetime import datetime, timedelta
from unittest.mock import MagicMock, patch

import faker

from django.contrib.auth.hashers import UnsaltedSHA1PasswordHasher
from django.core.exceptions import ValidationError
from django.test import TestCase, override_settings
from django.utils import timezone

from lms.enrollments.models import EnrolledUser, Enrollment
from lms.enrollments.tests.factories import EnrolledUserFactory, EnrollmentFactory
from lms.users.tests.factories import UserFactory

from ..models import Course, CourseGroup, CourseStudent
from ..services import update_occupancy
from .factories import (
    CourseBlockFactory, CourseFactory, CourseGroupFactory, CourseModuleFactory, CourseStudentFactory,
    CourseVisibilityFactory, LinkedCourseFactory,
)

fake = faker.Faker()


class CourseNumStudentsModelTestCase(TestCase):
    def test_num_participants(self):
        course = CourseFactory(is_active=True)
        enrollment = EnrollmentFactory(course=course, enroll_type=Enrollment.TYPE_MANUAL)
        group1, group2 = CourseGroupFactory.create_batch(2, is_active=True, course=course)
        enrolled_users_active_pending = []
        enrolled_users_rejected = []
        for group in [group1, group2]:
            enrolled_users = EnrolledUserFactory.create_batch(
                2,
                course=course,
                enrollment=enrollment,
                group=group,
                status=EnrolledUser.StatusChoices.ENROLLED,
            )
            enrolled_users_active_pending.extend(enrolled_users)
            enrolled_users = EnrolledUserFactory.create_batch(
                2,
                course=course,
                enrollment=enrollment,
                group=group,
                status=EnrolledUser.StatusChoices.PENDING,
            )
            enrolled_users_active_pending.extend(enrolled_users)
            enrolled_users_rejected.extend(
                EnrolledUserFactory.create_batch(
                    2,
                    course=course,
                    enrollment=enrollment,
                    group=group,
                    status=EnrolledUser.StatusChoices.REJECTED,
                )
            )
            group.refresh_from_db()
            self.assertEqual(group.num_participants, 4)
        course.refresh_from_db()
        self.assertEqual(course.occupancy.current, 8)

        enrolled_users_active_pending[0].delete()
        enrolled_users_active_pending[-1].delete()
        group1.refresh_from_db()
        group2.refresh_from_db()
        course.refresh_from_db()
        self.assertEqual(group1.num_participants, 3)
        self.assertEqual(group2.num_participants, 3)
        self.assertEqual(course.occupancy.current, 6)

        enrolled_users_rejected[0].delete()
        enrolled_users_rejected[-1].delete()
        group1.refresh_from_db()
        group2.refresh_from_db()
        course.refresh_from_db()
        self.assertEqual(group1.num_participants, 3)
        self.assertEqual(group2.num_participants, 3)
        self.assertEqual(course.occupancy.current, 6)


class CourseSlugValidateModelTestCase(TestCase):
    def test_invalid_slug(self):
        long_string = fake.pystr(min_chars=300, max_chars=500)
        for slug in ['new', 'archived', 'drafts', 'all', 'with spaces', 'неанглийский', long_string]:
            course = CourseFactory()
            course.slug = slug
            with self.assertRaises(ValidationError):
                course.full_clean()


class CourseGroupIsFullModelTestCase(TestCase):
    def test_is_full(self):
        course_group = CourseGroupFactory(max_participants=2)
        EnrolledUserFactory(course=course_group.course, group=course_group, status=EnrolledUser.StatusChoices.ENROLLED)
        EnrolledUserFactory(course=course_group.course, group=course_group, status=EnrolledUser.StatusChoices.PENDING)
        course_group.refresh_from_db()
        self.assertTrue(course_group.is_full)

    def test_is_not_full(self):
        course_group = CourseGroupFactory(max_participants=0)
        course_group.refresh_from_db()
        self.assertFalse(course_group.is_full)

        course_group = CourseGroupFactory(max_participants=3)
        EnrolledUserFactory(course=course_group.course, group=course_group, status=EnrolledUser.StatusChoices.ENROLLED)
        EnrolledUserFactory(course=course_group.course, group=course_group, status=EnrolledUser.StatusChoices.PENDING)
        course_group.refresh_from_db()
        self.assertFalse(course_group.is_full)


class CourseGroupAvailableForEnrollModelTestCase(TestCase):
    def test_available_for_enroll(self):
        course_group = CourseGroupFactory(
            max_participants=0,
            enroll_begin=timezone.now() - timedelta(days=1),
            enroll_end=timezone.now() + timedelta(days=1),
        )
        self.assertTrue(course_group.available_for_enroll)

    def test_not_available_for_enroll(self):
        course_group = CourseGroupFactory(
            max_participants=0,
            enroll_end=timezone.now() - timedelta(days=1),
        )
        self.assertFalse(course_group.available_for_enroll)

        course_group = CourseGroupFactory(
            max_participants=0,
            enroll_begin=timezone.now() + timedelta(days=1),
        )
        self.assertFalse(course_group.available_for_enroll)

        course_group = CourseGroupFactory(
            max_participants=1,
            enroll_begin=timezone.now() - timedelta(days=1),
            enroll_end=timezone.now() + timedelta(days=1),
        )
        EnrolledUserFactory(course=course_group.course, group=course_group, status=EnrolledUser.StatusChoices.ENROLLED)
        course_group.refresh_from_db()
        self.assertEqual(course_group.num_participants, 1)
        self.assertFalse(course_group.available_for_enroll)

        course_group = CourseGroupFactory(
            max_participants=1,
            enroll_end=timezone.now() - timedelta(days=1),
        )
        with self.assertRaises(ValidationError):
            EnrolledUserFactory(
                course=course_group.course, group=course_group, status=EnrolledUser.StatusChoices.ENROLLED,
            )

        course_group.refresh_from_db()
        self.assertFalse(course_group.available_for_enroll)


class CourseWithGroupsModelTestCase(TestCase):
    def test_course_enable_groups(self):
        course = CourseFactory()
        self.assertFalse(course.enable_groups)

        group = CourseGroupFactory(course=course)
        course.refresh_from_db()
        self.assertTrue(course.enable_groups)

        group.delete()
        course.refresh_from_db()
        self.assertFalse(course.enable_groups)


class CourseEnrollmentFlagsModelTestCase(TestCase):
    def test_multi_enrollments(self):
        course = CourseFactory()
        course.enrollments_only = False
        course.multi_enrollments = True

        with self.assertRaises(ValidationError):
            course.save()

    def test_retries_allowed(self):
        course = CourseFactory()
        course.enrollments_only = True
        course.retries_allowed = True

        with self.assertRaises(ValidationError):
            course.save()


class CourseIsEnrollOpenModelTestCase(TestCase):
    def setUp(self) -> None:
        self.now = timezone.now()
        self.day_before_now = self.now - timedelta(days=1)
        self.day_after_now = self.now + timedelta(days=1)
        self.two_days_before_now = self.now - timedelta(days=2)
        self.two_days_after_now = self.now + timedelta(days=2)

    def test_course_without_groups(self):
        # зачисление всегда открыто
        course = CourseFactory(is_active=True, enroll_begin=None, enroll_end=None)
        self.assertTrue(course.is_enroll_open)

        # зачисление уже началось
        course = CourseFactory(is_active=True, enroll_begin=self.day_before_now, enroll_end=None)
        self.assertTrue(course.is_enroll_open)

        # зачисление еще не началось
        course = CourseFactory(is_active=True, enroll_begin=self.day_after_now, enroll_end=None)
        self.assertFalse(course.is_enroll_open)

        # зачисление уже завершено
        course = CourseFactory(is_active=True, enroll_begin=None, enroll_end=self.day_before_now)
        self.assertFalse(course.is_enroll_open)

        # зачисление еще не завершено
        course = CourseFactory(is_active=True, enroll_begin=None, enroll_end=self.day_after_now)
        self.assertTrue(course.is_enroll_open)

        # зачисление началось и уже завершилось
        course = CourseFactory(is_active=True, enroll_begin=self.two_days_before_now, enroll_end=self.day_before_now)
        self.assertFalse(course.is_enroll_open)

        # зачисление началось и еще не завершилось
        course = CourseFactory(is_active=True, enroll_begin=self.day_before_now, enroll_end=self.day_after_now)
        self.assertTrue(course.is_enroll_open)

        # зачисление еще не началось
        course = CourseFactory(is_active=True, enroll_begin=self.day_after_now, enroll_end=self.two_days_after_now)
        self.assertFalse(course.is_enroll_open)

    def test_course_with_groups(self):
        course = CourseFactory(is_active=True, enroll_begin=None, enroll_end=None)
        CourseGroupFactory(course=course, enroll_begin=None, enroll_end=self.day_before_now)
        CourseGroupFactory(course=course, enroll_begin=self.day_after_now, enroll_end=None)

        # зачисление в группе всегда открыто
        course = CourseFactory(is_active=True, enroll_begin=None, enroll_end=None)
        CourseGroupFactory(course=course, enroll_begin=None, enroll_end=None)
        course.refresh_from_db()
        self.assertTrue(course.is_enroll_open)

        # зачисление в группе уже открыто
        course = CourseFactory(is_active=True, enroll_begin=None, enroll_end=None)
        CourseGroupFactory(course=course, enroll_begin=self.day_before_now, enroll_end=None)
        course.refresh_from_db()
        self.assertTrue(course.is_enroll_open)

        # зачисление в группе уже открыто и еще не закрыто
        course = CourseFactory(is_active=True, enroll_begin=None, enroll_end=None)
        CourseGroupFactory(
            course=course, enroll_begin=self.day_before_now, enroll_end=self.day_after_now,
        )
        course.refresh_from_db()
        self.assertTrue(course.is_enroll_open)

        # зачисление в группе уже открыто
        course = CourseFactory(is_active=True, enroll_begin=None, enroll_end=None)
        CourseGroupFactory(course=course, enroll_begin=self.day_before_now, enroll_end=None)
        course.refresh_from_db()
        self.assertTrue(course.is_enroll_open)

        # зачисление в группе уже закрыто
        course = CourseFactory(is_active=True, enroll_begin=None, enroll_end=None)
        CourseGroupFactory(course=course, enroll_begin=None, enroll_end=self.day_before_now)
        course.refresh_from_db()
        self.assertFalse(course.is_enroll_open)

        # зачисление в группе открылось, но уже закрыто
        course = CourseFactory(is_active=True, enroll_begin=None, enroll_end=None)
        CourseGroupFactory(
            course=course, enroll_begin=self.two_days_before_now, enroll_end=self.day_before_now,
        )
        course.refresh_from_db()
        self.assertFalse(course.is_enroll_open)

        # зачисление в группе еще не открылось
        course = CourseFactory(is_active=True, enroll_begin=None, enroll_end=None)
        CourseGroupFactory(
            course=course, enroll_begin=self.day_after_now, enroll_end=self.two_days_after_now,
        )
        course.refresh_from_db()
        self.assertFalse(course.is_enroll_open)

        # зачисление в группе еще не открылось
        course = CourseFactory(is_active=True, enroll_begin=None, enroll_end=None)
        CourseGroupFactory(course=course, enroll_begin=self.day_after_now, enroll_end=None)
        course.refresh_from_db()
        self.assertFalse(course.is_enroll_open)

        # период регистрации в группах приоритетней периода в курсе
        course = CourseFactory(is_active=True, enable_groups=True, enroll_begin=None, enroll_end=self.day_before_now)
        CourseGroupFactory(course=course, enroll_begin=None, enroll_end=None)
        course.refresh_from_db()
        self.assertTrue(course.is_enroll_open)


class CourseHasOpenSeatsModelTestCase(TestCase):
    def setUp(self) -> None:
        self.now = timezone.now()
        self.day_before_now = self.now - timedelta(days=1)
        self.day_after_now = self.now + timedelta(days=1)

    def test_course_without_groups(self):
        course = CourseFactory(is_active=True)
        self.assertTrue(course.has_open_seats)

        # у курса не открыта регистрация
        course = CourseFactory(is_active=True, enroll_begin=self.day_after_now)
        self.assertTrue(course.has_open_seats)

        # у группы курса еще не открылась регистрация
        course = CourseFactory(is_active=True, enable_groups=True)
        CourseGroupFactory(course=course, enroll_begin=self.day_after_now)
        course.refresh_from_db()
        self.assertFalse(course.has_open_seats)

        # у группы курса нет мест
        course = CourseFactory(is_active=True, enable_groups=True)
        group = CourseGroupFactory(course=course, max_participants=1)

        enrollment = EnrollmentFactory(course=course, enroll_type=Enrollment.TYPE_MANUAL)
        EnrolledUserFactory(course=course, group=group, enrollment=enrollment)
        update_occupancy(course)
        course.refresh_from_db()

        self.assertEqual(course.occupancy.maximum, 1)
        self.assertEqual(course.occupancy.current, 1)
        self.assertFalse(course.has_open_seats)

        # в группе курса есть места
        course = CourseFactory(is_active=True, enable_groups=True)
        CourseGroupFactory(course=course, max_participants=5, num_participants=0)
        course.refresh_from_db()
        self.assertTrue(course.has_open_seats)

        # одна группа занята, у второй - не открыта регистрация
        course = CourseFactory(is_active=True, enable_groups=True)

        group = CourseGroupFactory(course=course, max_participants=1)
        enrollment = EnrollmentFactory(course=course, enroll_type=Enrollment.TYPE_MANUAL)
        EnrolledUserFactory(course=course, group=group, enrollment=enrollment)
        update_occupancy(course)

        CourseGroupFactory(course=course, max_participants=5, enroll_begin=self.day_after_now)
        course.refresh_from_db()

        self.assertFalse(course.has_open_seats)


class CourseEnrollWillBeginModelTestCase(TestCase):
    def setUp(self) -> None:
        self.now = timezone.now()
        self.day_before_now = self.now - timedelta(days=1)
        self.day_after_now = self.now + timedelta(days=1)
        self.two_day_after_now = self.now + timedelta(days=2)

    def test_course_without_groups(self):
        # зачисление всегда открыто
        course = CourseFactory(is_active=True, enroll_begin=None)
        self.assertIsNone(course.enroll_will_begin)

        # зачисление уже открыто
        course = CourseFactory(is_active=True, enroll_begin=self.day_before_now)
        self.assertIsNone(course.enroll_will_begin)

        # зачисление откроется в будущем
        course = CourseFactory(is_active=True, enroll_begin=self.day_after_now)
        self.assertEqual(course.enroll_will_begin, self.day_after_now)

    def test_course_with_groups(self):
        # зачисление в группу всегда открыто
        course = CourseFactory(is_active=True, enable_groups=True)
        CourseGroupFactory(course=course, enroll_begin=None)
        course.refresh_from_db()
        self.assertIsNone(course.enroll_will_begin)

        # зачисление в группу уже открыто
        course = CourseFactory(is_active=True, enable_groups=True)
        CourseGroupFactory(course=course, enroll_begin=self.day_before_now)
        course.refresh_from_db()
        self.assertIsNone(course.enroll_will_begin)

        # зачисление в группу откроется в будущем
        course = CourseFactory(is_active=True, enable_groups=True)
        CourseGroupFactory(course=course, enroll_begin=self.day_after_now)
        course.refresh_from_db()
        self.assertEqual(course.enroll_will_begin, self.day_after_now)

        # зачисление в ближайшую группу откроется в будущем
        course = CourseFactory(is_active=True, enable_groups=True)
        CourseGroupFactory(course=course, enroll_begin=self.day_after_now)
        CourseGroupFactory(course=course, enroll_begin=self.two_day_after_now)
        course.refresh_from_db()
        self.assertEqual(course.enroll_will_begin, self.day_after_now)


class CourseGroupOrderingModelTestCase(TestCase):
    def test_ordering(self):
        equal_date = fake.date_time(end_datetime=datetime.max - timedelta(days=1))
        groups = sorted(
            (
                CourseGroupFactory.create_batch(
                    5, begin_date=fake.date_time(end_datetime=datetime.max - timedelta(days=1)),
                ) +
                CourseGroupFactory.create_batch(5) +
                CourseGroupFactory.create_batch(5, begin_date=equal_date)
            ),
            key=lambda group: (group.begin_date if group.begin_date else datetime.max, group.name),
        )

        sorted_ids = [group.id for group in groups]

        self.assertListEqual(
            sorted_ids,
            list(CourseGroup.objects.filter(id__in=sorted_ids).values_list('id', flat=True)),
        )


class CourseVisibilityModelTestCase(TestCase):
    def setUp(self):
        self.first_visibility_group_courses = CourseFactory.create_batch(3, is_active=True)
        self.first_visibility_group = [
            CourseVisibilityFactory(course=course, rules={"eq": ["staff_is_head", True]})
            for course in self.first_visibility_group_courses
        ]
        self.second_visibility_group_courses = CourseFactory.create_batch(5, is_active=True)
        self.second_visibility_group = [
            CourseVisibilityFactory(course=course, rules={"eq": ["staff_is_head", False]})
            for course in self.second_visibility_group_courses
        ]

    def test_hash(self):
        for group in (self.first_visibility_group, self.second_visibility_group):
            expected_hash = UnsaltedSHA1PasswordHasher().encode(str(group[0].rules), '')
            for visibility in group:
                self.assertEqual(expected_hash, visibility.rules_hash)


class CourseStudentModelTestCase(TestCase):
    def test_set_completion_date(self):
        student = CourseStudentFactory(completion_date=None, status=CourseStudent.StatusChoices.ACTIVE)
        now_moment = fake.date_time(tzinfo=timezone.utc)
        mocked_now = MagicMock()
        mocked_now.return_value = now_moment
        with patch('lms.courses.models.timezone.now', new=mocked_now):
            student.complete()
        student.refresh_from_db()

        self.assertEqual(student.completion_date, mocked_now())


class CourseStudentCleanTestCase(TestCase):
    def test_already_has_active_students(self):
        course = CourseFactory()
        user = UserFactory()
        student1 = CourseStudentFactory(course=course, user=user)

        with self.assertRaises(ValidationError):
            CourseStudentFactory(course=course, user=user)

        student1.complete()

        with self.assertRaises(ValidationError):
            CourseStudentFactory(course=course, user=user)

    def test_multi_enrollments_when_has_active_student(self):
        course = CourseFactory(enrollments_only=True, multi_enrollments=True)
        user = UserFactory()

        CourseStudentFactory(course=course, user=user, status=CourseStudent.StatusChoices.ACTIVE)

        try:
            CourseStudentFactory(course=course, user=user)
        except Exception as e:
            raise self.fail("DID RAISE {0}".format(e))

    def test_multi_enrollments_when_has_expelled_student(self):
        course = CourseFactory(enrollments_only=True, multi_enrollments=True)
        user = UserFactory()

        CourseStudentFactory(course=course, user=user, status=CourseStudent.StatusChoices.EXPELLED)

        try:
            CourseStudentFactory(course=course, user=user)
        except Exception as e:
            raise self.fail("DID RAISE {0}".format(e))

    def test_multi_enrollments_when_has_completed_student(self):
        course = CourseFactory(enrollments_only=True, multi_enrollments=True)
        user = UserFactory()

        CourseStudentFactory(course=course, user=user, status=CourseStudent.StatusChoices.COMPLETED)

        try:
            CourseStudentFactory(course=course, user=user)
        except Exception as e:
            raise self.fail("DID RAISE {0}".format(e))

    def test_retries_allowed(self):
        course = CourseFactory(retries_allowed=True)
        user = UserFactory()
        student1 = CourseStudentFactory(course=course, user=user)

        with self.assertRaises(ValidationError):
            CourseStudentFactory(course=course, user=user)

        student1.complete()

        CourseStudentFactory(course=course, user=user)


class CourseModuleModelTestCase(TestCase):
    def test_block_with_invalid_course(self):
        course = CourseFactory()
        block = CourseBlockFactory()
        with self.assertRaises(ValidationError):
            CourseModuleFactory(course=course, block=block)

    @override_settings(COURSE_MODULE_DEFAULT_WEIGHT=27)
    def test_default_model_weight(self):
        module = CourseModuleFactory(weight=11)
        self.assertEqual(module.weight, 27)

    @override_settings(COURSE_MODULE_DEFAULT_WEIGHT=27)
    def test_update_model_weight(self):
        module = CourseModuleFactory()
        module.weight = 13
        module.save()
        module.refresh_from_db()
        self.assertEqual(module.weight, 13)

    @override_settings(COURSE_MODULE_DEFAULT_WEIGHT=-20)
    def test_default_model_weight_negative(self):
        module = CourseModuleFactory(weight=11)
        self.assertEqual(module.weight, 11)

    @override_settings(COURSE_MODULE_DEFAULT_WEIGHT=0)
    def test_default_model_weight_zero(self):
        module = CourseModuleFactory(weight=11)
        self.assertEqual(module.weight, 0)

    @override_settings(COURSE_MODULE_DEFAULT_WEIGHT=1337)
    def test_default_model_weight_very_big(self):
        module = CourseModuleFactory(weight=11)
        self.assertEqual(module.weight, 100)

    def test_default_model_weight_not_set(self):
        module = CourseModuleFactory(weight=72)
        self.assertEqual(module.weight, 72)


class LinkedCourseModelTestCase(TestCase):
    def test_course_type_is_not_track(self):
        course = CourseFactory(course_type=Course.TypeChoices.COURSE.value)
        with self.assertRaises(ValidationError):
            LinkedCourseFactory(course=course)

    def test_linked_course_type_is_not_course(self):
        linked_course = CourseFactory(course_type=Course.TypeChoices.TRACK.value)
        with self.assertRaises(ValidationError):
            LinkedCourseFactory(linked_course=linked_course)
