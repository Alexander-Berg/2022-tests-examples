from unittest.mock import MagicMock, patch

import faker

from django.test import TestCase, TransactionTestCase
from django.utils.timezone import utc

from lms.assignments.tests.factories import AssignmentFactory
from lms.tags.services import normalize_tag_name
from lms.tags.tests.factories import TagFactory

from ..models import CourseModule, StudentCourseProgress, StudentModuleProgress
from ..services import (
    recalculate_all_course_progresses, update_course_progress, update_course_tags, update_module_progress,
)
from .factories import (
    CourseFactory, CourseModuleFactory, CourseStudentFactory, ModuleTypeFactory, StudentCourseProgressFactory,
    StudentModuleProgressFactory,
)

fake = faker.Faker()


class UpdateCourseProgressServiceTestCase(TestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.student = CourseStudentFactory(course=self.course)
        module_type = ModuleTypeFactory(app_label="dummies", model="dummy")
        self.modules = CourseModuleFactory.create_batch(100, course=self.course, module_type=module_type, weight=1)

    def create_module_progress(self, scaled_scores):
        """
        "дробный" прогресс необходим, чтобы протестировать округление. ранее логика была завязана на score_scaled.
        но так как score_scaled в текущей имплементации не используется, а score - это целое число от 0 до 100,
        то создаём 100 равновесных модулей (см. setUp) с ненулевым весом.
        и тогда score (от 0 до 100) прогресса по i-му модулю будет давать вклад в прогесс по курсу от 0.00 до 1.00
        """
        progress_list = []
        for i, score_scaled in enumerate(scaled_scores):
            score = int(score_scaled * 100)
            progress = StudentModuleProgressFactory(
                course=self.course, module=self.modules[i], student=self.student, score=score
            )
            progress_list.append(progress)
        return progress_list

    def test_update_course_progress_round_up(self):
        module_scaled_scores = (0.06, 0.20, 0.20, 0.24, 0.30, 0.50)
        self.create_module_progress(module_scaled_scores)
        course_progress = StudentCourseProgressFactory(course=self.course, student=self.student)
        with self.assertNumQueries(7):
            update_course_progress(self.student)
        course_progress.refresh_from_db()
        self.assertEqual(course_progress.score, 2)

    def test_update_course_progress_round_down(self):
        module_scaled_scores = (0.02, 0.49, 0.49, 0.49)
        self.create_module_progress(module_scaled_scores)
        course_progress = StudentCourseProgressFactory(course=self.course, student=self.student)
        with self.assertNumQueries(7):
            update_course_progress(self.student)
        course_progress.refresh_from_db()
        self.assertEqual(course_progress.score, 1)

    def test_update_course_progress_no_module_progress(self):
        course_progress = StudentCourseProgressFactory(course=self.course, student=self.student, score=37)
        with self.assertNumQueries(7):
            update_course_progress(self.student)
        course_progress.refresh_from_db()
        self.assertEqual(course_progress.score, 0)

    def test_update_course_progress_no_progress(self):
        module_scaled_scores = (0.30, 0.30, 0.40)
        self.create_module_progress(module_scaled_scores)
        with self.assertNumQueries(7):
            update_course_progress(self.student)
        course_progress = StudentCourseProgress.objects.get(course=self.course, student=self.student)
        self.assertEqual(course_progress.score, 1)

    def test_update_course_student_modified_field(self):
        now_moment = fake.date_time(tzinfo=utc)
        mocked_now = MagicMock()
        mocked_now.return_value = now_moment

        with patch('lms.courses.services.now', new=mocked_now):
            update_course_progress(self.student)
            self.student.refresh_from_db()
            self.assertEqual(self.student.modified, now_moment)


class CourseProgressCalculationTestCase(TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.student = CourseStudentFactory(course=self.course)
        # модуль Assignment используется потому что там есть сигналы на пересчёт кэша весов модулей
        self.modules = AssignmentFactory.create_batch(4, course=self.course, weight=1)
        self.progress = StudentCourseProgressFactory(course=self.course, student=self.student, score=0)

    def assert_course_progress(self, expected_score):
        self.progress.refresh_from_db()
        self.assertEqual(self.progress.score, expected_score)

    def test_progress_after_module_creation(self):
        StudentModuleProgressFactory(course=self.course, student=self.student, module=self.modules[0], score=100)
        self.assert_course_progress(25)

        AssignmentFactory(course=self.course, weight=1)
        StudentModuleProgressFactory(course=self.course, student=self.student, module=self.modules[1], score=100)
        self.assert_course_progress(40)

    def test_progress_after_module_deletion(self):
        StudentModuleProgressFactory(course=self.course, student=self.student, module=self.modules[0], score=100)
        self.assert_course_progress(25)

        self.modules[-1].delete()
        StudentModuleProgressFactory(course=self.course, student=self.student, module=self.modules[1], score=100)
        self.assert_course_progress(67)

    def test_progress_after_module_weight_change(self):
        StudentModuleProgressFactory(course=self.course, student=self.student, module=self.modules[0], score=100)
        self.assert_course_progress(25)

        self.modules[-1].weight = 2
        self.modules[-1].save()
        StudentModuleProgressFactory(course=self.course, student=self.student, module=self.modules[1], score=100)
        self.assert_course_progress(40)

    def test_progress_after_module_hiding(self):
        StudentModuleProgressFactory(course=self.course, student=self.student, module=self.modules[0], score=100)
        self.assert_course_progress(25)

        self.modules[-1].is_active = False
        self.modules[-1].save()
        StudentModuleProgressFactory(course=self.course, student=self.student, module=self.modules[1], score=100)
        self.assert_course_progress(67)

    def test_progress_after_module_activating(self):
        self.modules[-1].is_active = False
        self.modules[-1].save()
        StudentModuleProgressFactory(course=self.course, student=self.student, module=self.modules[0], score=100)
        self.assert_course_progress(33)

        self.modules[-1].is_active = True
        self.modules[-1].save()
        StudentModuleProgressFactory(course=self.course, student=self.student, module=self.modules[1], score=100)
        self.assert_course_progress(50)


class UpdateModuleProgressServiceTestCase(TestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.student = CourseStudentFactory(course=self.course)
        self.module_type = ModuleTypeFactory(app_label="dummies", model="dummy")
        self.module = CourseModuleFactory(course=self.course, module_type=self.module_type, weight=1)
        self.zero_weight_module = CourseModuleFactory(course=self.course, module_type=self.module_type, weight=0)

    def test_update_module_progress_no_progress(self):
        with self.assertNumQueries(8):
            update_module_progress(module=self.module, student=self.student, value=50)
        module_progress = StudentModuleProgress.objects.get(module=self.module, student=self.student)
        self.assertEqual(module_progress.score, 50)

    def test_update_module_progress_existing_progress(self):
        module_progress = StudentModuleProgressFactory(
            course=self.course, student=self.student, module=self.module, score=0
        )
        with self.assertNumQueries(10):
            update_module_progress(module=self.module, student=self.student, value=50)
        module_progress.refresh_from_db()
        self.assertEqual(module_progress.score, 50)

    def test_update_module_progress_max_value(self):
        with self.assertNumQueries(8):
            update_module_progress(module=self.module, student=self.student, value=666)
        module_progress = StudentModuleProgress.objects.get(module=self.module, student=self.student)
        self.assertEqual(module_progress.score, 100)

    def test_update_module_progress_min_value_force(self):
        module_progress = StudentModuleProgressFactory(
            course=self.course, student=self.student, module=self.module, score=50
        )
        with self.assertNumQueries(10):
            update_module_progress(module=self.module, student=self.student, value=-273, force=True)
        module_progress.refresh_from_db()
        self.assertEqual(module_progress.score, 0)

    def test_update_module_progress_min_value(self):
        module_progress = StudentModuleProgressFactory(
            course=self.course, student=self.student, module=self.module, score=50
        )
        with self.assertNumQueries(3):
            update_module_progress(module=self.module, student=self.student, value=-273)
        module_progress.refresh_from_db()
        self.assertEqual(module_progress.score, 50)

    def test_update_module_progress_zero_weight(self):
        with self.assertNumQueries(8):
            update_module_progress(module=self.zero_weight_module, student=self.student, value=50)
        module_progress = StudentModuleProgress.objects.get(module=self.zero_weight_module, student=self.student)
        self.assertEqual(module_progress.score, 50)

    def test_update_module_progress_same_value(self):
        update_module_progress(module=self.module, student=self.student, value=100)
        with self.assertNumQueries(10):
            update_module_progress(module=self.module, student=self.student, value=72, force=True)
        with self.assertNumQueries(3):
            update_module_progress(module=self.module, student=self.student, value=72, force=True)
        with self.assertNumQueries(3):
            update_module_progress(module=self.module, student=self.student, value=72)


class RecalculateAllCourseProgressesServiceTestCase(TestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.students = CourseStudentFactory.create_batch(3, course=self.course)
        self.student_without_progress = self.students[2]
        self.module_type = ModuleTypeFactory(app_label="dummies", model="dummy")

        # создаём два равновесных модуля и прогресс по одному из модулей для двух студентов
        # при сохранении прогресса в pre_save считается score_scaled, который будет 50.00 = 100 * (1 / (1 + 1))
        self.modules = CourseModuleFactory.create_batch(2, course=self.course, module_type=self.module_type, weight=1)
        StudentModuleProgressFactory(student=self.students[0], course=self.course, module=self.modules[0], score=100)
        StudentModuleProgressFactory(student=self.students[1], course=self.course, module=self.modules[0], score=100)

        # создаём ещё один модуль такого же веса и прогресс по ещё одному модулю для одного студента
        # при сохранении прогресса в pre_save считается score_scaled, который будет 33.33 = 100 * (1 / (1 + 1 + 1))
        CourseModuleFactory(course=self.course, module_type=self.module_type, weight=1)
        StudentModuleProgressFactory(student=self.students[1], course=self.course, module=self.modules[1], score=100)

        # прогрессы по курсу создаются таской асинхронно, поэтому для TestCase теста создаём их самостоятельно
        # важно, чтобы значение score не равнялось ожидаемому в тесте
        self.progress1 = StudentCourseProgressFactory(student=self.students[0], course=self.course, score=1)
        self.progress2 = StudentCourseProgressFactory(student=self.students[1], course=self.course, score=1)

    def assert_course_progress(self, progress: StudentCourseProgress, expected_score: int):
        progress.refresh_from_db()
        self.assertEqual(progress.score, expected_score)

    def assert_student_has_no_course_progress(self, student):
        self.assertIsNone(StudentCourseProgress.objects.filter(student=student).first())

    def test_recalculate_all_course_progresses(self):
        with self.assertNumQueries(14):
            recalculate_all_course_progresses(course_id=self.course.id)

        self.assert_course_progress(progress=self.progress1, expected_score=33)
        self.assert_course_progress(progress=self.progress2, expected_score=67)
        self.assert_student_has_no_course_progress(student=self.student_without_progress)

    def test_recalculate_all_course_progresses_inactive_module(self):
        self.modules[0].is_active = False
        self.modules[0].save()

        with self.assertNumQueries(14):
            recalculate_all_course_progresses(course_id=self.course.id)

        self.assert_course_progress(progress=self.progress1, expected_score=0)
        self.assert_course_progress(progress=self.progress2, expected_score=50)
        self.assert_student_has_no_course_progress(student=self.student_without_progress)

    def test_recalculate_all_course_progresses_zero_weight_module(self):
        self.modules[0].weight = 0
        self.modules[0].save()

        with self.assertNumQueries(14):
            recalculate_all_course_progresses(course_id=self.course.id)

        self.assert_course_progress(progress=self.progress1, expected_score=0)
        self.assert_course_progress(progress=self.progress2, expected_score=50)
        self.assert_student_has_no_course_progress(student=self.student_without_progress)

    def test_recalculate_all_course_progresses_all_modules_inactive(self):
        CourseModule.objects.filter(course_id=self.course.id).update(is_active=False)

        with self.assertNumQueries(14):
            recalculate_all_course_progresses(course_id=self.course.id)

        self.assert_course_progress(progress=self.progress1, expected_score=0)
        self.assert_course_progress(progress=self.progress2, expected_score=0)
        self.assert_student_has_no_course_progress(student=self.student_without_progress)

    def test_recalculate_all_course_progresses_all_modules_zero_weight(self):
        CourseModule.objects.filter(course_id=self.course.id).update(weight=0)

        with self.assertNumQueries(14):
            recalculate_all_course_progresses(course_id=self.course.id)

        self.assert_course_progress(progress=self.progress1, expected_score=0)
        self.assert_course_progress(progress=self.progress2, expected_score=0)
        self.assert_student_has_no_course_progress(student=self.student_without_progress)


class UpdateCourseTagsServiceTestCase(TestCase):
    def setUp(self):
        self.course = CourseFactory()

    def test_add_new_tags(self):
        tag_names = [fake.pystr() for _ in range(5)]
        with self.assertNumQueries(9):
            update_course_tags(self.course, tag_names)
        course_tags = set(self.course.tags.values_list('normalized_name', flat=True))
        expected_tags = {normalize_tag_name(tag) for tag in tag_names}
        self.assertEqual(expected_tags, course_tags)

    def test_add_existing_tags(self):
        tags = [TagFactory(name=fake.pystr()) for _ in range(5)]
        tag_names = [tag.name for tag in tags[:3]]
        with self.assertNumQueries(5):
            update_course_tags(self.course, tag_names)
        course_tags = set(self.course.tags.values_list('normalized_name', flat=True))
        expected_tags = {normalize_tag_name(tag) for tag in tag_names}
        self.assertEqual(expected_tags, course_tags)

    def test_update_tags(self):
        tags = [TagFactory(name=fake.pystr()) for _ in range(8)]
        self.course.tags.set(tags[:4])
        tag_names = [tag.name for tag in tags[2:6]]
        tag_names.extend(fake.pystr() for _ in range(3))
        with self.assertNumQueries(10):
            update_course_tags(self.course, tag_names)
        course_tags = set(self.course.tags.values_list('normalized_name', flat=True))
        expected_tags = {normalize_tag_name(tag) for tag in tag_names}
        self.assertEqual(expected_tags, course_tags)

    def test_clear_tags(self):
        tags = [TagFactory(name=fake.pystr()) for _ in range(5)]
        self.course.tags.set(tags)
        with self.assertNumQueries(3):
            update_course_tags(self.course, [])
        self.assertEqual(len(self.course.tags.all()), 0)
