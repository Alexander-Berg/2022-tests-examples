# coding: utf-8


import pytest

import datetime
from json import loads

from django.core.urlresolvers import reverse
from intranet.dogma.dogma.core.models import UserFileStatistics

pytestmark = pytest.mark.django_db(transaction=True)


@pytest.fixture
def file_statistics(another_user):
    return UserFileStatistics.objects.create(
        user=another_user,
        extension='py',
        lines_added=1000,
        lines_deleted=0,
        period=datetime.date(2018, 3, 1)
    )


@pytest.fixture
def another_file_statistics(another_user):
    return UserFileStatistics.objects.create(
        user=another_user,
        extension='txt',
        lines_added=0,
        lines_deleted=42,
        period=datetime.date(2018, 3, 1)
    )


@pytest.fixture
def anotherone_file_statistics(user):
    return UserFileStatistics.objects.create(
        user=user,
        extension='changelog',
        lines_added=450,
        lines_deleted=34,
        period=datetime.date(2018, 4, 1)
    )


def test_get_changed_files_by_author_success(client, user, another_user,
                                             file_statistics, another_file_statistics,
                                             anotherone_file_statistics,
                                             ):
    client.login(username='vasya')

    response = loads(client.get('{}?{}'.format(reverse('api:file_statistics'),
                                               'uid=123456')
                                ).content)
    assert response == {'previous': None, 'results': [
        {'lines_added': 1000, 'lines_deleted': 0,
         'user': {'login': 'smosker', 'id': another_user.id,
                  'uid': '123456', 'email': 'smosker@ya.ru',
                  },
         'extension': 'py', 'period': '03.2018'
         },
        {'lines_added': 0, 'lines_deleted': 42,
         'user': {'login': 'smosker', 'id': another_user.id,
                  'uid': '123456', 'email': 'smosker@ya.ru',
                  },
         'extension': 'txt', 'period': '03.2018'}
    ], 'next': None}


def test_get_changed_files_by_many_author_success(client, user, another_user,
                                             file_statistics, another_file_statistics,
                                             anotherone_file_statistics, django_assert_num_queries,
                                                  ):
    client.login(username='vasya')
    with django_assert_num_queries(9):
        response = loads(client.get('{}?{}'.format(reverse('api:file_statistics'),
                                                   'uid=123456,12345')
                                    ).content)
    assert response['results'] == [
        {'lines_added': 1000, 'lines_deleted': 0,
         'user': {'login': 'smosker', 'id': another_user.id,
                  'uid': '123456', 'email': 'smosker@ya.ru',
                  },
         'extension': 'py', 'period': '03.2018'
         },
        {'lines_added': 0, 'lines_deleted': 42,
         'user': {'login': 'smosker', 'id': another_user.id,
                  'uid': '123456', 'email': 'smosker@ya.ru',
                  },
         'extension': 'txt', 'period': '03.2018'},
        {'lines_added': 450, 'lines_deleted': 34,
         'user': {'login': 'vsem_privet', 'id': user.id,
                  'email': 'test@ya.ru', 'uid': '12345',
                  },
         'extension': 'changelog', 'period': '04.2018',
         },
    ]


def test_get_changed_files_by_month_ahead_success(client, user, another_user,
                                                  file_statistics, another_file_statistics,
                                                  anotherone_file_statistics,
                                                  ):
    client.login(username='vasya')

    response = loads(client.get('{}?{}'.format(reverse('api:file_statistics'),
                                               'period=2018-04-01,2018-05-01')
                                ).content)
    assert response == {'previous': None, 'results': [
        {'lines_added': 450, 'lines_deleted': 34,
         'user': {'login': 'vsem_privet', 'id': user.id,
                  'email': 'test@ya.ru', 'uid': '12345',
                  },
         'extension': 'changelog', 'period': '04.2018',
         },
    ], 'next': None}


def test_get_changed_files_by_month_success(client, user, another_user,
                                            file_statistics, another_file_statistics,
                                            anotherone_file_statistics,
                                            ):
    client.login(username='vasya')

    response = loads(client.get('{}?{}'.format(reverse('api:file_statistics'),
                                               'period=2018-03-01,2018-03-31')
                                ).content)
    assert response == {'previous': None, 'results': [
        {'lines_added': 1000, 'lines_deleted': 0,
         'user': {'login': 'smosker', 'id': another_user.id,
                  'uid': '123456', 'email': 'smosker@ya.ru',
                  },
         'extension': 'py', 'period': '03.2018'
         },
        {'lines_added': 0, 'lines_deleted': 42,
         'user': {'login': 'smosker', 'id': another_user.id,
                  'uid': '123456', 'email': 'smosker@ya.ru',
                  },
         'extension': 'txt', 'period': '03.2018'}
    ], 'next': None}


def test_get_changed_files_by_month_only_from_1st_success(client, user, another_user,
                                                          file_statistics, another_file_statistics,
                                                          anotherone_file_statistics,
                                                          ):
    client.login(username='vasya')

    response = loads(client.get('{}?{}'.format(reverse('api:file_statistics'),
                                               'period=2018-04-30,2018-05-16')
                                ).content)
    assert response == {'previous': None, 'results': [
        {'lines_added': 450, 'lines_deleted': 34,
         'user': {'login': 'vsem_privet', 'id': user.id,
                  'email': 'test@ya.ru', 'uid': '12345',
                  },
         'extension': 'changelog', 'period': '04.2018',
         },
    ], 'next': None}


def test_get_changed_files_should_raise_without_args(client):
    client.login(username='vasya')

    response = client.get(reverse('api:file_statistics'))
    assert loads(response.content) == {'message': 'You have to specify uid or period'}
    assert response.status_code == 500


def test_get_changed_files_should_raise_with_wrong_date(client):
    client.login(username='vasya')

    response = client.get(reverse('api:file_statistics'), {'period': '02.2018'})
    assert loads(response.content) == {
        'message': 'Invalid period format: 02.2018, should be &period=YYYY-MM-DD,YYYY-MM-DD',
    }
    assert response.status_code == 500
