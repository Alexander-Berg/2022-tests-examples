import faker

from django.core.exceptions import ValidationError
from django.test import TestCase

from lms.courses.tests.factories import CourseStudentFactory

from ..models import AddAchievementEvent, CourseTrigger
from .factories import CourseTriggerFactory

fake = faker.Faker()


class CourseTriggerModelTestCase(TestCase):
    def test_action_handler_map(self):
        course_trigger = CourseTriggerFactory(
            action_type=CourseTrigger.ActionType.ADD_ACHIEVEMENT,
            trigger_type=CourseTrigger.TriggerType.COURSE_PASSED,
            parameters={'achievement_id': fake.pyint()}
        )
        action_handler_map = course_trigger.action_handler_map
        unregistered = [x for x in CourseTrigger.ActionType.values if x not in action_handler_map]

        assert not unregistered

    def test_add_achievement_action_type_start(self):
        achievement_id = fake.pyint()
        student = CourseStudentFactory()
        course_trigger = CourseTriggerFactory(
            course_id=student.course_id,
            action_type=CourseTrigger.ActionType.ADD_ACHIEVEMENT,
            trigger_type=CourseTrigger.TriggerType.COURSE_PASSED,
            parameters={'achievement_id': achievement_id}
        )

        course_trigger.start(student_id=student.id)

        assert AddAchievementEvent.objects.filter(
            achievement_id=achievement_id,
            user_id=student.user_id,
            course_id=student.course_id,
        ).exists()

    def test_missing_achievement_id_in_achievement_action_trigger(self):
        with self.assertRaises(ValidationError):
            CourseTriggerFactory(
                action_type=CourseTrigger.ActionType.ADD_ACHIEVEMENT,
                trigger_type=CourseTrigger.TriggerType.COURSE_PASSED,
                parameters={'another_field': 'test'}
            )

    def test_achievement_id_wrong_format_in_achievement_action_trigger(self):
        with self.assertRaises(ValidationError):
            CourseTriggerFactory(
                action_type=CourseTrigger.ActionType.ADD_ACHIEVEMENT,
                trigger_type=CourseTrigger.TriggerType.COURSE_PASSED,
                parameters={'achievement_id': 'test'}
            )
