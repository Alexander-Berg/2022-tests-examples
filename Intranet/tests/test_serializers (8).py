from builtins import object

from kelvin.result_stats.models import StudentCourseStat
from kelvin.result_stats.serializers import StudentCourseStatInJournalSerializer


class TestStudentCourseStatInJournalSerializer(object):
    """
    Тесты журнального сериализатора курсовой статистики ученика
    """

    def test_get_lessons(self, mocker):
        """
        Тест получения упорядоченного по урокам списка эффективности ученика
        """
        stat = StudentCourseStat(
            student_id=1,
            course_id=11,
            clesson_data={
                '101': {
                    'max_points': 40,
                    'points': 10,
                    'progress': 100,
                },
                '202': {
                    'max_points': 20,
                    'points': 15,
                    'progress': 75,
                },
                '303': {
                    'max_points': 100,
                    'points': 48,
                    'progress': 20,
                },
                '505': {
                    'max_points': 25,
                    'points': 10,
                    'progress': 33,
                },
            },
        )
        serializer = StudentCourseStatInJournalSerializer(
            context={'clessons': [101, 202, 303, 404, 505]},
        )

        assert serializer.get_lessons(stat) == {
            101: {
                'max_points': 40,
                'points': 10,
                'progress': 100,
            },
            202: {
                'max_points': 20,
                'points': 15,
                'progress': 75,
            },
            303: {
                'max_points': 100,
                'points': 48,
                'progress': 20,
            },
            404: {
                'max_points': None,
                'points': None,
                'progress': None,
            },
            505: {
                'max_points': 25,
                'points': 10,
                'progress': 33,
            },
        }, u'Неправильно сформирован маппинг эффективности по занятиям'
