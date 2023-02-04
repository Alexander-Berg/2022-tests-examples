from builtins import object, str

import pytest

from kelvin.problem_meta.models import Exam, ProblemMeta, ProblemMetaExam, Skill
from kelvin.subjects.models import Subject, Theme


class TestProblemMeta(object):
    """
    Тесты модели метаинформации задачи
    """
    cases = (
        (
            {'difficulty': 1, 'tags': [], 'themes': []},
            u'Id: None, Difficulty: Простой, Tags: <None>',
        ),
        (
            {
                'id': 1,
                'difficulty': 2,
                'tags': ['new'],
                'themes': [Theme(id=1)],
            },
            u'Id: 1, Difficulty: Средний, Tags: new',
        ),
        (
            {
                'id': 1,
                'tags': [u'новый', 'math'],
                'themes': [Theme(id=2, name=u'тема')],
            },
            u'Id: 1, Difficulty: None, Tags: новый, math',
        ),
    )

    @pytest.mark.parametrize('data,expected', cases)
    def test_unicode(self, mocker, data, expected):
        """
        Тесты `ProblemMeta.__str__`
        """
        # патчим поле `themes`, чтобы можно было задать значение поля
        mocker.patch.object(ProblemMeta, 'additional_themes')
        themes = data.pop('themes')
        meta = ProblemMeta(**data)
        meta.additional_themes = themes
        assert meta.__str__() == expected, (
            u'Неправильное строковое представление метаинформации')


class TestSkill(object):
    """
    Тесты для модели Навыки
    """
    @pytest.mark.parametrize(
        'skill,subject,expected',
        (
            (
                {'name': u'Решение интегралов в уме'},
                {'name': u'Математика', 'id': 1},
                u'Математика: Решение интегралов в уме',
            ),
            (
                {'name': u''},
                {'name': u'Математика', 'id': 1},
                u'Математика: ',
            ),
            (
                {'name': u'Решение интегралов в уме'},
                {'name': u'Русский', 'slug': u'russian', 'id': 1},
                u'Русский: Решение интегралов в уме',
            ),
            (
                {'name': u'Решение интегралов в уме'},
                {'name': u'', 'slug': u'russian', 'id': 1},
                u': Решение интегралов в уме',
            ),
        ),
    )
    def test_unicode(self, skill, subject, expected):
        """
        Тестирование метода `__str__`
        """
        skill_instance = Skill(**skill)
        skill_instance.subject = Subject(**subject)
        assert skill_instance.__str__() == expected


class TestProblemMetaExam(object):
    """
    Тесты для модели связи метаинформации задач и экзаменов
    """
    @pytest.mark.parametrize(
        'meta,exam,expected',
        (
            (
                {'number': u'A2'},
                {'name': u'ЕГЭ', 'year': 2015, 'id': 1},
                u'ЕГЭ (2015) №A2',
            ),
            (
                {'number': u'6'},
                {'name': u'ГИА', 'year': 2013, 'id': 1},
                u'ГИА (2013) №6',
            ),
        ),
    )
    def test_unicode(self, meta, exam, expected):
        """
        Тестирование метода `__str__`
        """
        obj = ProblemMetaExam(**meta)
        obj.exam = Exam(**exam)
        obj.problem_meta = ProblemMeta(id=2)

        assert obj.__str__() == expected


class TestExamModel(object):
    """
    Тесты модели экзамена
    """
    cases = (
        (
            {'name': u'ЕГЭ', 'year': 2015},
            u'ЕГЭ (2015)',
        ),
        (
            {'name': u'ГИА', 'year': 1995},
            u'ГИА (1995)',
        ),
        (
            {'name': u"ВНО", 'year': 2014},
            u'ВНО (2014)',
        ),
    )

    @pytest.mark.parametrize('data,expected', cases)
    def test_unicode(self, data, expected):
        exam = Exam(**data)
        assert str(exam) == expected, (
            u'Неправильное строковое представление экзамена')
