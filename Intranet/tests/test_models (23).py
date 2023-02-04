from builtins import object, range, str
from datetime import timedelta

import pytest

from django.contrib.auth import get_user_model
from django.utils import timezone

from kelvin.courses.models import Course, CourseLessonLink, CourseStudent
from kelvin.lesson_assignments.models import LessonAssignment
from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.problems.models import Problem
from kelvin.result_stats.models import CourseLessonStat, ProblemAnswerStat
from kelvin.results.models import CourseLessonResult, CourseLessonSummary

User = get_user_model()


@pytest.mark.django_db
class TestCourseLessonStat(object):
    """
    Тесты групповой статистики по занятию
    """
    CORRECT_ANSWER = {
        'markers': {
            '1': {
                'mistakes': 0,
                'max_mistakes': 1,
            },
        },
        'mistakes': 0,
        'max_mistakes': 1,
        'completed': True,
        'spent_time': None,
        'points': 20,
        'max_points': 20,
    }
    INCORRECT_ANSWER = {
        'markers': {
            '1': {
                'mistakes': 1,
                'max_mistakes': 1,
            },
        },
        'mistakes': 1,
        'max_mistakes': 1,
        'completed': True,
        'spent_time': None,
        'points': 0,
        'max_points': 20,
    }

    def test_calculate(self, teacher, theme_model, subject_model):
        """
        Тест подсчета статистики
        """
        #  Вопросы, занятие
        problem1 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={},
        )
        problem2 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={},
        )
        problem3 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={},
        )
        problem4 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={},
        )
        lesson = Lesson.objects.create(owner=teacher, theme=theme_model,
                                       name=u'Урок')
        link1 = LessonProblemLink.objects.create(
            lesson=lesson, problem=problem1, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )
        link2 = LessonProblemLink.objects.create(
            lesson=lesson, problem=problem2, order=2,
            options={'max_attempts': 5, 'show_tips': True},
        )
        link3 = LessonProblemLink.objects.create(
            lesson=lesson, problem=problem3, order=3,
            options={'max_attempts': 5, 'show_tips': True},
        )
        link4 = LessonProblemLink.objects.create(
            lesson=lesson, problem=problem4, order=4,
            options={'max_attempts': 5, 'show_tips': True},
        )

        # Курс и курсозанятие
        course = Course.objects.create(
            name=u'Новый спец курс', subject=subject_model, owner=teacher)
        clesson = CourseLessonLink.objects.create(
            lesson=lesson, course=course, order=1,
            date_assignment=timezone.now(),
        )

        # ученики
        students = []
        for i in range(3):
            user = User.objects.create(username=u'{0}'.format(i))
            students.append(user)
            CourseStudent.objects.create(course=course, student=user)

        # Результат одному ученику - 3 правильных задачи
        summary1 = CourseLessonSummary.objects.create(
            clesson=clesson,
            student=students[0],
        )
        result1 = CourseLessonResult.objects.create(
            answers={
                str(link1.id): [self.INCORRECT_ANSWER],
                str(link2.id): [self.CORRECT_ANSWER],
                str(link3.id): [self.CORRECT_ANSWER],
                str(link4.id): [self.CORRECT_ANSWER],
            },
            points=1,
            max_points=100,
            summary=summary1,
        )

        # Объект статистики должен создаться селери
        stat, created = CourseLessonStat.objects.get_or_create(clesson=clesson)
        assert not created, u'Статистика должна быть создана селери-таском'

        # Без назначений, решено верно 3 из 12 - 25%
        # одна с ошибкой - 1 из 12 - 8%
        # попытка пока только от одного ученика, всего их трое
        assert stat.calculate() == (25, 8, 1, 3, 1, 100)

        # Назначаем второму ученику 3 задачи - тогда 3 из 11 - 27%
        assignment = LessonAssignment.objects.create(
            clesson=clesson, student=students[1],
            problems=[link2.id, link3.id, link4.id],
        )
        assert stat.calculate() == (27, 9, 1, 3, 1, 100)

        # "Решаем" второму ученику одну из задач, тогда 4 из 11 - 36%
        summary2 = CourseLessonSummary.objects.create(
            clesson=clesson,
            student=students[1],
        )
        result2 = CourseLessonResult.objects.create(
            answers={
                str(link2.id): [self.CORRECT_ANSWER],
            },
            points=4,
            max_points=101,
            summary=summary2,
        )
        assert stat.calculate() == (36, 9, 2, 3, 3, 101)

        # "Решаем" второму ученику три задачи. Должна быть учтена эта попытка
        result3 = CourseLessonResult.objects.create(
            answers={
                str(link2.id): [self.CORRECT_ANSWER],
                str(link3.id): [self.CORRECT_ANSWER],
                str(link4.id): [self.CORRECT_ANSWER],
            },
            points=10,
            max_points=50,
            summary=summary2,
        )
        # 6 из 11 - 55%
        assert stat.calculate() == (55, 9, 2, 3, 6, 75)

        # Убираем учеников из курса, тогда статистика отдает `None`
        CourseStudent.objects.filter(course=course).update(deleted=True)
        assert stat.calculate() == (55, 9, 2, 3, 6, 75)

    def test_calculate_before_assignment(self, teacher, theme_model,
                                         subject_model, student):
        """
        Тест подсчета статитстики при наличии ответов до даты выдачи
        """
        # Задача, занятие
        problem1 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={},
        )
        lesson = Lesson.objects.create(owner=teacher, theme=theme_model,
                                       name=u'Урок')
        link1 = LessonProblemLink.objects.create(
            lesson=lesson, problem=problem1, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )

        # Курс и курсозанятие
        course = Course.objects.create(
            name=u'Новый спец курс', subject=subject_model, owner=teacher)
        clesson = CourseLessonLink.objects.create(
            lesson=lesson, course=course, order=1,
            date_assignment=timezone.now() + timedelta(hours=1),
        )

        # Ученик
        CourseStudent.objects.create(course=course, student=student)

        # Результат
        summary1 = CourseLessonSummary.objects.create(
            clesson=clesson,
            student=student,
        )
        result1 = CourseLessonResult.objects.create(
            answers={
                str(link1.id): [self.CORRECT_ANSWER],
            },
            points=1,
            max_points=100,
            summary=summary1,
        )

        # Объект статистики должен создаться селери
        stat, created = CourseLessonStat.objects.get_or_create(clesson=clesson)
        assert not created, u'Статистика должна быть создана селери-таском'

        # Так как попытка создана до даты назначения, статистика должна быть
        # пустой
        assert stat.calculate() == (0, 0, 0, 0, 0, 0)

    def test_calculate_control_work(self, teacher, theme_model,
                                    subject_model):
        """
        Тест подсчета статистики для контрольной работы
        - статистика для контрольной должна считаться
        - неначатые задания должны считаться как неправильные
        """
        # Задача, занятие
        problem1 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={},
        )
        problem2 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={},
        )
        problem3 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={},
        )
        problem4 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={},
        )
        lesson = Lesson.objects.create(owner=teacher, theme=theme_model,
                                       name=u'Урок')
        link1 = LessonProblemLink.objects.create(
            lesson=lesson, problem=problem1, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )
        link2 = LessonProblemLink.objects.create(
            lesson=lesson, problem=problem2, order=2,
            options={'max_attempts': 5, 'show_tips': True},
        )
        link3 = LessonProblemLink.objects.create(
            lesson=lesson, problem=problem3, order=3,
            options={'max_attempts': 5, 'show_tips': True},
        )
        link4 = LessonProblemLink.objects.create(
            lesson=lesson, problem=problem4, order=4,
            options={'max_attempts': 5, 'show_tips': True},
        )

        # Курс и курсозанятие
        course = Course.objects.create(
            name=u'Новый спец курс', subject=subject_model, owner=teacher)
        now = timezone.now()
        clesson = CourseLessonLink.objects.create(
            lesson=lesson,
            course=course,
            order=1,
            mode=CourseLessonLink.CONTROL_WORK_MODE,
            date_assignment=now - timedelta(minutes=1),
            duration=1,
            finish_date=now,
            evaluation_date=now,
        )

        # 3 ученика
        students = []
        for i in range(3):
            user = User.objects.create(username=u'{0}'.format(i))
            students.append(user)
            CourseStudent.objects.create(course=course, student=user)

        # Решаем ученику 2 задачи правильно, 1 неправильно, 1 не решаем
        summary1 = CourseLessonSummary.objects.create(
            clesson=clesson,
            student=students[0],
        )
        result1 = CourseLessonResult.objects.create(
            answers={
                str(link1.id): [self.CORRECT_ANSWER],
                str(link3.id): [self.CORRECT_ANSWER],
                str(link4.id): [self.INCORRECT_ANSWER],
            },
            points=1,
            max_points=100,
            summary=summary1,
        )

        # Решаем другому ученику 1 правильно, 3 не решаем
        summary2 = CourseLessonSummary.objects.create(
            clesson=clesson,
            student=students[2],
        )
        result2 = CourseLessonResult.objects.create(
            answers={
                str(link1.id): [self.CORRECT_ANSWER],
            },
            points=1,
            max_points=100,
            summary=summary2,
        )

        # Другим ученикам задачи не решаем

        # Объект статистики должен создаться селери
        stat, created = CourseLessonStat.objects.get_or_create(clesson=clesson)
        assert not created, u'Статистика должна быть создана селери-таском'

        # Статистика должна посчитаться
        # Нерешенные должны засчитаться как неправильные
        assert stat.calculate() == (25, 75, 2, 3, 1, 100)

    def test_calculate_work_without_unsolved(self, teacher, theme_model,
                                             subject_model):
        """
        Тест подсчета статистики для работы, в которой нужно считать
        нерешенные задания как неправильные
        """
        # Задача, занятие
        problem1 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={},
        )
        problem2 = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={},
        )
        lesson = Lesson.objects.create(owner=teacher, theme=theme_model,
                                       name=u'Урок')
        link1 = LessonProblemLink.objects.create(
            lesson=lesson, problem=problem1, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )
        link2 = LessonProblemLink.objects.create(
            lesson=lesson, problem=problem2, order=2,
            options={'max_attempts': 5, 'show_tips': True},
        )

        # Курс и курсозанятие
        course = Course.objects.create(
            name=u'Новый спец курс', subject=subject_model, owner=teacher)
        clesson = CourseLessonLink.objects.create(
            lesson=lesson, course=course, order=1,
            date_assignment=timezone.now(),
        )

        # 3 ученика
        students = []
        for i in range(3):
            user = User.objects.create(username=u'{0}'.format(i))
            students.append(user)
            CourseStudent.objects.create(course=course, student=user)

        # Решаем одному ученику (из трех) только одну
        # задачу из 2 (итого решена 1 из 6)
        summary1 = CourseLessonSummary.objects.create(
            clesson=clesson,
            student=students[1],
        )
        result1 = CourseLessonResult.objects.create(
            answers={
                str(link2.id): [self.CORRECT_ANSWER],
            },
            points=1,
            max_points=100,
            summary=summary1,
        )

        # Объект статистики должен создаться селери
        stat, created = CourseLessonStat.objects.get_or_create(clesson=clesson)
        assert not created, u'Статистика должна быть создана селери-таском'

        # Нерешенные должны засчитаться как неправильные
        assert stat.calculate(allow_unsolved=False) == (17, 83, 1, 3, 1, 100)


@pytest.mark.django_db
class TestProblemAnswerStat(object):
    """
    Тесты статистики по ответу на задание
    """
    CORRECT_ANSWER = {
        'markers': {
            '1': {
                'user_answer': [1, 2],
                'mistakes': 0,
                'max_mistakes': 1,
                'answer_status': 1,
            },
        },
        'mistakes': 0,
        'max_mistakes': 1,
        'completed': True,
        'spent_time': None,
        'points': 20,
        'max_points': 20,
    }
    CORRECT_MARKERS_ANSWERS = {
        '1': {
            'user_answer': [1, 2],
        },
    }

    INCORRECT_ANSWER1 = {
        'markers': {
            '1': {
                'user_answer': [3, 4],
                'mistakes': 1,
                'max_mistakes': 1,
                'answer_status': 0,
            },
        },
        'mistakes': 1,
        'max_mistakes': 1,
        'completed': True,
        'spent_time': None,
        'points': 0,
        'max_points': 20,
    }
    INCORRECT_MARKERS_ANSWERS1 = {
        '1': {
            'user_answer': [3, 4],
        },
    }

    INCORRECT_ANSWER2 = {
        'markers': {
            '1': {
                'user_answer': [4, 5],
                'mistakes': 1,
                'max_mistakes': 1,
                'answer_status': 0,
            },
        },
        'mistakes': 1,
        'max_mistakes': 1,
        'completed': True,
        'spent_time': None,
        'points': 0,
        'max_points': 20,
    }
    INCORRECT_MARKERS_ANSWERS2 = {
        '1': {
            'user_answer': [4, 5],
        },
    }

    def test_calculate_stats(self, teacher, theme_model, subject_model):
        """
        Тест подсчета статистики
        """
        #  Вопросы, занятие
        problem = Problem.objects.create(
            owner=teacher, subject=subject_model, markup={},
        )
        lesson = Lesson.objects.create(owner=teacher, theme=theme_model,
                                       name=u'Урок')
        link = LessonProblemLink.objects.create(
            lesson=lesson, problem=problem, order=1,
            options={'max_attempts': 5, 'show_tips': True},
        )

        # Курс и курсозанятие
        course = Course.objects.create(
            name=u'Новый курс', subject=subject_model, owner=teacher)
        clesson = CourseLessonLink.objects.create(
            lesson=lesson, course=course, order=1)

        # Ученики
        students = []
        for i in range(2):
            user = User.objects.create(username=u'{0}'.format(i))
            students.append(user)
            CourseStudent.objects.create(course=course, student=user)

        # Результат первого ученика
        summary1 = CourseLessonSummary.objects.create(
            clesson=clesson,
            student=students[0],
        )
        CourseLessonResult.objects.create(
            answers={
                str(link.id): [self.INCORRECT_ANSWER2, self.CORRECT_ANSWER],
            },
            points=1,
            max_points=100,
            summary=summary1,
        )

        # Соотношение ответов должно быть 50%/50%
        ProblemAnswerStat.calculate_stats(problem.id)
        stats = ProblemAnswerStat.objects.all().order_by('is_correct')

        assert stats[0].percent == 50
        assert stats[0].count == 1
        assert stats[0].markers_answer == self.INCORRECT_MARKERS_ANSWERS2
        assert not stats[0].is_correct
        assert stats[1].percent == 50
        assert stats[1].count == 1
        assert stats[1].markers_answer == self.CORRECT_MARKERS_ANSWERS
        assert stats[1].is_correct
        assert len(stats) == 2

        # Результат второго ученика
        summary2 = CourseLessonSummary.objects.create(
            clesson=clesson,
            student=students[1],
        )
        CourseLessonResult.objects.create(
            answers={
                str(link.id): [
                    self.INCORRECT_ANSWER2,
                    self.INCORRECT_ANSWER1,
                    self.INCORRECT_ANSWER2,
                    self.CORRECT_ANSWER,
                ],
            },
            points=4,
            max_points=101,
            summary=summary2,
        )

        # Теперь должно быть 17%/33%/50%
        ProblemAnswerStat.calculate_stats(problem.id)
        stats = ProblemAnswerStat.objects.all().order_by('percent')

        assert stats[0].percent == 17
        assert stats[0].count == 1
        assert stats[0].markers_answer == self.INCORRECT_MARKERS_ANSWERS1
        assert not stats[0].is_correct
        assert stats[1].percent == 33
        assert stats[1].count == 2
        assert stats[1].markers_answer == self.CORRECT_MARKERS_ANSWERS
        assert stats[1].is_correct
        assert stats[2].percent == 50
        assert stats[2].count == 3
        assert stats[2].markers_answer == self.INCORRECT_MARKERS_ANSWERS2
        assert not stats[2].is_correct
        assert len(stats) == 3
