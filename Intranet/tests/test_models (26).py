from builtins import object

import pytest
from mock.mock import call

from kelvin.subjects.models import Subject, Theme, get_default_subject


class TestSubjectModel(object):
    """
    Тесты для модели предметов
    """
    @pytest.mark.parametrize(
        'initial,expected',
        [
            ({'name': 'Math', 'slug': 'math'}, 'Math'),
            ({'name': u'Математика', 'slug': 'math'}, u'Математика'),
        ])
    def test_unicode(self, initial, expected):
        """Тест `Subject.__str__`"""
        subject = Subject(**initial)
        assert subject.__str__() == expected


class TestThemeModel(object):
    """
    Тесты для модели тем
    """
    @pytest.mark.parametrize(
        'initial,expected',
        [
            (
                {
                    'subject': Subject(id=1), 'parent': None,
                    'code': '123', 'name': 'Multiplying',
                },
                u'Multiplying',
            ),
            (
                {
                    'subject': Subject(id=1), 'parent': Theme(id=1),
                    'code': '234', 'name': u'Умножение',
                },
                u'Умножение'),
        ]
    )
    def test_unicode(self, mocker, initial, expected):
        """Тест `Theme.__str__()`"""
        mocker.patch.object(Theme, 'parent')
        mocker.patch.object(Theme, 'subject')
        parent = initial.pop('parent')
        subject = initial.pop('subject')
        theme = Theme(**initial)
        theme.parent = parent
        theme.subject = subject
        assert theme.__str__() == expected


def test_get_default_subject(mocker):
    """Тест получения дефолтного предмета"""
    mocked_subject = mocker.patch('kelvin.subjects.models.Subject')
    mocked_subject.objects.get_or_create.return_value = (1, True)
    assert get_default_subject() == 1
    assert mocked_subject.mock_calls == [call.objects.get_or_create(
        name=u'Математика', slug='mathematics')]
