# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

import pytest
import mock

from autodasha.core.api.tracker import Issue as BaseIssue
from autodasha.core.config import Config


class Issue(BaseIssue):
    def __init__(self, *args, **kwargs):
        super(Issue, self).__init__(*args, **kwargs)
        self._comments = []

    @property
    def comments(self):
        return self._comments

# TODO: тесты с приглашаемыми в комментарии


@pytest.fixture
def issue(session):
    Config(session)
    dt_ = dt.datetime.now().strftime('%Y-%m-%dT%H:%M:%S')

    res = Issue(mock.MagicMock(createdAt=dt_, updatedAt=dt_))
    res._comments = [
        {'id': 1, 'author': 'Олег', 'text': 'Как ныне сбирается вещий Олег'},
        {'id': 2, 'author': 'кудесник', 'text': 'Отмстить неразумным хозарам'},
        {'id': 3, 'author': 'Олег', 'text': 'Их селы и нивы за буйный набег'},
        {'id': 4, 'author': 'конь', 'text': 'Обрек он мечам и пожарам'},
        {'id': 5, 'author': 'конь', 'text': 'Из темного леса навстречу ему идет вдохновенный кудесник'},
        {'id': 6, 'author': 'кудесник', 'text': 'Покорный Перуну старик одному, заветов грядущего вестник'},
        {'id': 7, 'author': 'Олег', 'text': 'В мольбах и гаданьях проведший весь век'},
        {'id': 8, 'author': 'кудесник', 'text': 'И к мудрому старцу подъехал Олег'},
    ]

    return res


def test_get_single_author(issue):
    res = issue.get_comments('Олег')

    required_res = [
        {'id': 1, 'author': 'Олег', 'text': 'Как ныне сбирается вещий Олег'},
        {'id': 3, 'author': 'Олег', 'text': 'Их селы и нивы за буйный набег'},
        {'id': 7, 'author': 'Олег', 'text': 'В мольбах и гаданьях проведший весь век'},
    ]

    assert res == required_res


def test_get_single_author_list(issue):
    res = issue.get_comments(['Олег'])

    required_res = [
        {'id': 1, 'author': 'Олег', 'text': 'Как ныне сбирается вещий Олег'},
        {'id': 3, 'author': 'Олег', 'text': 'Их селы и нивы за буйный набег'},
        {'id': 7, 'author': 'Олег', 'text': 'В мольбах и гаданьях проведший весь век'},
    ]

    assert res == required_res


def test_get_author_fail(issue):
    res = issue.get_comments('Пушкин')

    required_res = []

    assert res == required_res


def test_get_multiple_authors(issue):
    res = issue.get_comments(['Олег', 'конь'])

    required_res = [
        {'id': 1, 'author': 'Олег', 'text': 'Как ныне сбирается вещий Олег'},
        {'id': 3, 'author': 'Олег', 'text': 'Их селы и нивы за буйный набег'},
        {'id': 4, 'author': 'конь', 'text': 'Обрек он мечам и пожарам'},
        {'id': 5, 'author': 'конь', 'text': 'Из темного леса навстречу ему идет вдохновенный кудесник'},
        {'id': 7, 'author': 'Олег', 'text': 'В мольбах и гаданьях проведший весь век'},
    ]

    assert res == required_res


def test_get_multiple_authors_fail(issue):
    res = issue.get_comments(['Олег', 'Пушкин'])

    required_res = [
        {'id': 1, 'author': 'Олег', 'text': 'Как ныне сбирается вещий Олег'},
        {'id': 3, 'author': 'Олег', 'text': 'Их селы и нивы за буйный набег'},
        {'id': 7, 'author': 'Олег', 'text': 'В мольбах и гаданьях проведший весь век'},
    ]

    assert res == required_res


def test_get_text_single_match(issue):
    res = issue.get_comments(text='Перуну')

    required_res = [
        {'id': 6, 'author': 'кудесник', 'text': 'Покорный Перуну старик одному, заветов грядущего вестник'},
    ]

    assert res == required_res


def test_get_text_multiple_match(issue):
    res = issue.get_comments(text='Олег')

    required_res = [
        {'id': 1, 'author': 'Олег', 'text': 'Как ныне сбирается вещий Олег'},
        {'id': 8, 'author': 'кудесник', 'text': 'И к мудрому старцу подъехал Олег'},
    ]

    assert res == required_res


def test_get_text_case(issue):
    res = issue.get_comments(text='перуну')

    required_res = [
        {'id': 6, 'author': 'кудесник', 'text': 'Покорный Перуну старик одному, заветов грядущего вестник'},
    ]

    assert res == required_res


def test_get_text_fail(issue):
    res = issue.get_comments(text='СОТОНА')

    required_res = [
    ]

    assert res == required_res


def test_get_text_exactly(issue):
    res = issue.get_comments(text='Обрек он мечам и пожарам', exactly=True)

    required_res = [
        {'id': 4, 'author': 'конь', 'text': 'Обрек он мечам и пожарам'},
    ]

    assert res == required_res


def test_get_text_exactly_case(issue):
    res = issue.get_comments(text='обрек он мечам и пожарам', exactly=True)

    required_res = []

    assert res == required_res


def test_get_single_author_text(issue):
    res = issue.get_comments('Олег', 'Олег')

    required_res = [
        {'id': 1, 'author': 'Олег', 'text': 'Как ныне сбирается вещий Олег'},
    ]

    assert res == required_res


def test_get_multiple_authors_text(issue):
    res = issue.get_comments(['Олег', 'кудесник'], 'Олег')

    required_res = [
        {'id': 1, 'author': 'Олег', 'text': 'Как ныне сбирается вещий Олег'},
        {'id': 8, 'author': 'кудесник', 'text': 'И к мудрому старцу подъехал Олег'},
    ]

    assert res == required_res


def test_get_authors_text_mismatch(issue):
    res = issue.get_comments(['Олег', 'конь'], 'Перун')

    required_res = []

    assert res == required_res


def test_get_authors_text_exactly(issue):
    res = issue.get_comments(['Олег', 'кудесник'], 'И к мудрому старцу подъехал Олег')

    required_res = [
        {'id': 8, 'author': 'кудесник', 'text': 'И к мудрому старцу подъехал Олег'},
    ]

    assert res == required_res


def test_has_single_author(issue):
    res = issue.has_comments('Олег')

    assert res is True


def test_has_single_author_list(issue):
    res = issue.has_comments(['Олег'])
    assert res is True


def test_has_author_fail(issue):
    res = issue.has_comments('Пушкин')

    assert res is False


def test_has_multiple_authors(issue):
    res = issue.has_comments(['Олег', 'конь'])

    assert res is True


def test_has_multiple_authors_fail(issue):
    res = issue.has_comments(['Олег', 'конь'])

    assert res is True


def test_has_text_single_match(issue):
    res = issue.has_comments(text='Перуну')

    assert res is True


def test_has_text_multiple_match(issue):
    res = issue.has_comments(text='Олег')

    assert res is True


def test_has_text_case(issue):
    res = issue.has_comments(text='перуну')

    assert res is True


def test_has_text_fail(issue):
    res = issue.has_comments(text='СОТОНА')

    assert res is False


def test_has_text_exactly(issue):
    res = issue.has_comments(text='Обрек он мечам и пожарам', exactly=True)

    assert res is True


def test_has_text_exactly_case(issue):
    res = issue.has_comments(text='обрек он мечам и пожарам', exactly=True)

    assert res is False


def test_has_single_author_text(issue):
    res = issue.has_comments('Олег', 'Олег')

    assert res is True


def test_has_multiple_authors_text(issue):
    res = issue.has_comments(['Олег', 'кудесник'], 'Олег')

    assert res is True


def test_has_authors_text_mismatch(issue):
    res = issue.has_comments(['Олег', 'конь'], 'Перун')

    assert res is False


def test_has_authors_text_exactly(issue):
    res = issue.has_comments(['Олег', 'кудесник'], 'И к мудрому старцу подъехал Олег')

    assert res is True
