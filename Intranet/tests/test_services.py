from unittest.mock import patch

import faker

from django.test import TestCase

from lms.courses.tests.factories import CourseFactory, CourseStudentFactory

from ..models import AddAchievementEvent, CourseTrigger
from ..services import create_add_achivement_event, start_course_passed_trigger_actions
from .factories import CourseTriggerFactory

fake = faker.Faker()


class AddAchievementFromTriggerServiceTestCase(TestCase):
    def test_add_achievement_from_trigger(self):
        achievement_id = fake.pyint()
        course = CourseFactory()
        student = CourseStudentFactory(course=course)
        trigger = CourseTriggerFactory(
            course=course,
            action_type=CourseTrigger.ActionType.ADD_ACHIEVEMENT,
            trigger_type=CourseTrigger.TriggerType.COURSE_PASSED,
            parameters={'achievement_id': achievement_id},
        )

        with self.assertNumQueries(3):
            create_add_achivement_event(
                course_trigger=trigger,
                student_id=student.id
            )

        assert AddAchievementEvent.objects.filter(
            achievement_id=achievement_id,
            user_id=student.user_id,
            course_id=course.id,
        ).exists()


class StartCoursePassedTriggerActionsServiceTestCase(TestCase):
    def test_course_trigger_start_called(self):
        course = CourseFactory()
        student = CourseStudentFactory(course_id=course.id)

        with patch.object(CourseTrigger, 'start') as mock:
            # целевые триггеры
            CourseTriggerFactory.create_batch(
                size=3,
                course_id=course.id,
                action_type=CourseTrigger.ActionType.ADD_ACHIEVEMENT,
                trigger_type=CourseTrigger.TriggerType.COURSE_PASSED,
                parameters={'achievement_id': 1},
            )
            # неактивные триггеры
            CourseTriggerFactory.create_batch(
                size=5,
                course_id=course.id,
                action_type=CourseTrigger.ActionType.ADD_ACHIEVEMENT,
                trigger_type=CourseTrigger.TriggerType.COURSE_PASSED,
                parameters={'achievement_id': 1},
                is_active=False,
            )
            # триггеры другого курса
            another_course = CourseFactory()
            CourseTriggerFactory.create_batch(
                size=4,
                course_id=another_course.id,
                action_type=CourseTrigger.ActionType.ADD_ACHIEVEMENT,
                trigger_type=CourseTrigger.TriggerType.COURSE_PASSED,
                parameters={'achievement_id': 1},
            )
            start_course_passed_trigger_actions(course_id=course.id, student_id=student.id)
            assert mock.call_count == 3
            mock.assert_called_with(student_id=student.id)
