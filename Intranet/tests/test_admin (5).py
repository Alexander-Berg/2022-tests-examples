from builtins import object

import pytest
from mock import MagicMock, call

from kelvin.problem_meta.admin import ProblemMetaAdmin
from kelvin.subjects.models import Theme


class TestProblemMetaAdmin(object):
    """
    Тесты для админки модели `ProblemMeta`
    """
    list_cases = (
        (
            ['1', 2, u'Строка', 1.4],
            u'<ul><li>1</li><li>2</li><li>Строка</li><li>1.4</li></ul>',
        ),
        (
            [],
            u'',
        ),
        (
            [Theme(id=1, name=u'Алгебра'), Theme(id=2, name=u'Геометрия')],
            u'<ul><li>Алгебра</li><li>Геометрия</li></ul>',
        )
    )

    @pytest.mark.parametrize('data,expected', list_cases)
    def test_get_manytomany_list(self, data, expected):
        """Тест метода `get_manytomany_list`"""
        admin = ProblemMetaAdmin(MagicMock(), MagicMock())
        assert admin.get_manytomany_list(data) == expected

    def test_get_skills(self, mocker):
        """Тест метода `get_skills`"""
        mocked_get_list = mocker.patch.object(ProblemMetaAdmin,
                                              'get_manytomany_list')
        mocked_get_list.return_value = u'Вызван'
        obj = MagicMock()
        obj.skills = MagicMock()
        obj.skills.all.return_value = [1, 2]

        ProblemMetaAdmin(MagicMock(), MagicMock()).get_skills(obj)
        assert obj.skills.all.called, u'Не был вызван список навыков'
        assert mocked_get_list.called, (
            u'Не был вызван метод `get_manytomany_list`')
        assert mocked_get_list.mock_calls == [call([1, 2])], (
            u'Метод `get_manytomany_list` был вызван с неверными аргументами'
        )

    def test_get_additional_themes(self, mocker):
        """Тест метода `get_additional_themes`"""
        mocked_get_list = mocker.patch.object(ProblemMetaAdmin,
                                              'get_manytomany_list')
        obj = MagicMock()
        obj.additional_themes = MagicMock()
        obj.additional_themes.all.return_value = [1, 2]
        ProblemMetaAdmin(MagicMock(), MagicMock()).get_additional_themes(obj)
        assert obj.additional_themes.all.called, u'Не был вызван список тем'
        assert mocked_get_list.called, (
            u'Не был вызван метод `get_manytomany_list`')
        assert mocked_get_list.mock_calls == [call([1, 2])], (
            u'Метод `get_manytomany_list` был вызван с неверными аргументами'
        )

    def test_get_exams(self, mocker):
        """Тест метода `get_exams`"""
        mocked_get_list = mocker.patch.object(ProblemMetaAdmin,
                                              'get_manytomany_list')
        obj = MagicMock()
        obj.exams = MagicMock()
        obj.exams.all.return_value = [1, 2]
        ProblemMetaAdmin(MagicMock(), MagicMock()).get_exams(obj)
        assert obj.exams.all.called, u'Не был вызван список тем'
        assert mocked_get_list.called, (
            u'Не был вызван метод `get_manytomany_list`')
        assert mocked_get_list.mock_calls == [call([1, 2])], (
            u'Метод `get_manytomany_list` был вызван с неверными аргументами'
        )
