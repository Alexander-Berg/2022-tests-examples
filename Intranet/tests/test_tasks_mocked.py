import pytest
from mock import MagicMock, call

from django.conf import settings

from kelvin.courses.journal import CourseGroupJournal
from kelvin.courses.models import Course, CourseLessonLink, CourseStudent
from kelvin.result_stats.models import CourseLessonStat, ProblemAnswerStat, ProblemStat, StudentCourseStat
from kelvin.result_stats.tasks import (
    calculate_course_journal, recalculate_courselessonstat, recalculate_problem_answers_stat, recalculate_problemstat,
    recalculate_studentcoursestat, recalculate_studentcoursestat_by_result,
)
from kelvin.results.models import CourseLessonResult, CourseLessonSummary


def test_recalculate_courselessonstat(mocker):
    """
    Тест задачи пересчета статистики
    """
    mocked_stat_manager = mocker.patch.object(CourseLessonStat, 'objects')
    mocked_stat_instance = MagicMock()
    mocked_stat_manager.get_or_create.return_value = (
        mocked_stat_instance, True)

    # Случай, когда подсчет возвращает `None`
    mocked_stat_instance.calculate.return_value = None

    recalculate_courselessonstat(1111)

    assert mocked_stat_manager.mock_calls == [
        call.get_or_create(
            clesson_id=1111,
        ),
    ], u'Неправильное получение/создание объекта статистики'
    assert mocked_stat_instance.mock_calls == [
        call.calculate(), call.delete(),
    ], u'Нужно посчитать статистику и при возвращенном `None` удалить объект'

    # Случай, когда подсчет возвращает три значения
    mocked_stat_manager.reset_mock()
    mocked_stat_instance.reset_mock()

    mocked_stat_instance.calculate.return_value = 1, 2, 3, 4, 5, 6
    recalculate_courselessonstat(2222)
    assert mocked_stat_manager.mock_calls == [
        call.get_or_create(
            clesson_id=2222,
        ),
    ], u'Неправильное получение/создание объекта статистики'
    assert mocked_stat_instance.mock_calls == [
        call.calculate(), call.save()], (
        u'Нужно подсчитать статистику и, если вернулись '
        u'значения, сохранить объект'
    )

    assert mocked_stat_instance.percent_complete == 1, (
        u'Неправильный процент выполнения')
    assert mocked_stat_instance.percent_fail == 2, (
        u'Неправильный процент выполнения')
    assert mocked_stat_instance.results_count == 3, (
        u'Неправильный процент выполнения')
    assert mocked_stat_instance.max_results_count == 4, (
        u'Неправильный процент выполнения')
    assert mocked_stat_instance.average_points == 5, (
        u'Неправильный средний балл')
    assert mocked_stat_instance.average_max_points == 6, (
        u'Неправильный средний максимум баллов')


def test_recalculate_problemstat(mocker):
    """
    Тест пересчета статистики по задаче
    """
    mocked_problemstat_manager = mocker.patch.object(ProblemStat, 'objects')
    mocked_problemstat_instance = MagicMock()
    mocked_problemstat_manager.get_or_create.return_value = (
        mocked_problemstat_instance, True)
    mocked_problemstat_instance.calculate.return_value = 1, 2, 3, {'1': 2}

    recalculate_problemstat(1234)

    assert mocked_problemstat_instance.correct_number == 1, (
        u'Неправильное количество верных решений')
    assert mocked_problemstat_instance.incorrect_number == 2, (
        u'Неправильное количество неверных решений')
    assert mocked_problemstat_instance.correct_percent == 3, (
        u'Неправильный процент верных решений')
    assert mocked_problemstat_instance.marker_stats == {'1': 2}, (
        u'Неправильная статистика по маркерам')
    assert mocked_problemstat_manager.mock_calls == [
        call.get_or_create(
            problem_id=1234,
            defaults=dict(marker_stats=[]),
        )
    ], u'Не было вызова get_or_create'
    assert mocked_problemstat_instance.mock_calls == [
        call.calculate(),
        call.save(),
    ], u'У объекта статистики должны быть вызваны подсчет и сохранение'


def test_recalculate_problem_answers_stat(mocker):
    """
    Тест задачи пересчета статистики по ответам на задачу
    """
    calculate_stats = mocker.patch.object(ProblemAnswerStat, 'calculate_stats')
    recalculate_problem_answers_stat(42)

    assert calculate_stats.mock_calls == [call(42)]


def test_recalculate_studentcoursestat(mocker):
    """
    Тест задачи полного пересчета статистики ученика по курсу
    """
    mocked_atomic = mocker.patch('kelvin.result_stats.tasks.transaction')
    mocked_stat_manager = mocker.patch.object(StudentCourseStat, 'objects')
    mocked_stat_instance = MagicMock()
    (mocked_stat_manager.select_for_update.return_value
     .get_or_create.return_value) = mocked_stat_instance, True

    recalculate_studentcoursestat(1, 11)

    assert mocked_stat_manager.mock_calls == [
        call.select_for_update(),
        call.select_for_update().get_or_create(student_id=1, course_id=11),
    ], u'Нужно создать или получить объект с локом'
    assert mocked_stat_instance.mock_calls == [
        call.calculate(),
        call.save(),
    ], u'Нужно вызвать подсчет, затем сохранить объект'


def test_recalculate_studentcoursestat_by_result(mocker):
    """
    Тест задачи частичного пересчета статистики ученика по курсу
    """
    mocked_atomic = mocker.patch('kelvin.result_stats.tasks.transaction')
    mocked_stat_manager = mocker.patch.object(StudentCourseStat, 'objects')
    mocked_result_manager = mocker.patch.object(CourseLessonResult, 'objects')
    mocked_stat_instance = MagicMock()

    # Случай, когда нет результата
    mocked_result_manager.select_related.return_value.get.side_effect = (
        CourseLessonResult.DoesNotExist())

    recalculate_studentcoursestat_by_result(111)

    assert mocked_result_manager.mock_calls == [
        call.select_related('summary', 'summary__clesson'),
        call.select_related().get(id=111, summary__student__isnull=False),
    ], u'Нужно получить результат'
    assert mocked_stat_manager.mock_calls == [], (
        u'Не нужно получать статистику')
    assert mocked_stat_instance.mock_calls == [], (
        u'Не нужно работать со статистикой')

    # Есть результат
    mocked_result_manager.reset_mock()
    mocked_result_manager.select_related.return_value.get.side_effect = None
    result = (
        CourseLessonResult(
            id=111,
            answers={},
            summary=CourseLessonSummary(
                student_id=1,
                clesson=CourseLessonLink(
                    course_id=11,
                )
            )
        )
    )
    mocked_result_manager.select_related.return_value.get.return_value = result

    # Объект создается с нуля - должен быть полный пересчет
    (mocked_stat_manager.select_for_update.return_value
     .get_or_create.return_value) = mocked_stat_instance, True

    recalculate_studentcoursestat_by_result(111)

    assert mocked_result_manager.mock_calls == [
        call.select_related('summary', 'summary__clesson'),
        call.select_related().get(id=111, summary__student__isnull=False),
    ], u'Нужно получить результат'
    assert mocked_stat_manager.mock_calls == [
        call.select_for_update(),
        call.select_for_update().get_or_create(student_id=1, course_id=11),
    ], u'Нужно создать или получить объект статистики с локом'
    assert mocked_stat_instance.mock_calls == [
        call.calculate(),
        call.save(),
    ], u'Нужно провести полный пересчет и сохранить объект'

    # Объект уже существует - должен быть частичный пересчет
    mocked_result_manager.reset_mock()
    mocked_stat_instance.reset_mock()
    mocked_stat_manager.reset_mock()
    (mocked_stat_manager.select_for_update.return_value
     .get_or_create.return_value) = mocked_stat_instance, False

    recalculate_studentcoursestat_by_result(111)

    assert mocked_result_manager.mock_calls == [
        call.select_related('summary', 'summary__clesson'),
        call.select_related().get(id=111, summary__student__isnull=False),
    ], u'Нужно получить результат'
    assert mocked_stat_manager.mock_calls == [
        call.select_for_update(),
        call.select_for_update().get_or_create(student_id=1, course_id=11),
    ], u'Нужно создать или получить объект статистики с локом'
    assert mocked_stat_instance.mock_calls == [
        call.recalculate_by_result(result),
        call.save(),
    ], u'Нужно провести частичный пересчет и сохранить объект'


@pytest.mark.skip
@pytest.mark.django_db
def test_calculate_course_journal(mocker):
    """
    Тест задачи пересчета журнала по курсу
    """
    mocked_csv = mocker.patch.object(CourseGroupJournal, 'table')
    mocked_result_manager = mocker.patch.object(CourseLessonResult, 'objects')
    mocked_student_manager = mocker.patch.object(CourseStudent, 'objects')
    mocked_course_manager = mocker.patch.object(Course, 'objects')

    mocked_csv.return_value = "ret"
    mocked_course = mocked_course_manager.get()
    mocked_student_manager.filter().count.return_value = 0

    mocked_save_journal = mocker.patch(
        'kelvin.result_stats.tasks.save_journal_table_to_model'
    )

    calculate_course_journal(1)

    assert mocked_course_manager.mock_calls == [
        call.get(), call.get(id=1),
    ], u'Нужно достать курс по course_id'
    assert mocked_csv.call_count == 0, (
        u'При малом количестве студентов не должен создаваться csv'
    )
    assert mocked_student_manager.mock_calls == [
        call.filter(),
        call.filter(course=mocked_course),
        call.filter().count(),
    ], u'Нужно посчитать количество студентов'

    mocked_student_manager.filter().count.return_value = (
        settings.MAX_COURSE_JOURNAL_STUDENTS + 1)

    calculate_course_journal(1)
    assert mocked_csv.call_count == 1, (
        u'При количестве студентов > 300 должен создаваться csv'
    )
