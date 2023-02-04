# coding: utf-8


import pytest
pytestmark = pytest.mark.django_db(transaction=True)

from intranet.dogma.dogma.core.dao.file_statistics import insert_file_statistics_data
from intranet.dogma.dogma.core.models import UserFileStatistics, User
from dateutil import parser


@pytest.fixture
def date():
    return parser.parse('2018-01-01')


@pytest.fixture
def another_date():
    return parser.parse('2018-03-01')


@pytest.fixture
def user(transactional_db,):
    return User.objects.create(
        uid='123456',
        login='smosker',
        email='smosker@ya.ru',
        name='Anotherone',
        other_emails='smosker@ya.ru, email@email.com'
    )


@pytest.fixture
def py_file_statistics(user, date):
    return UserFileStatistics.objects.create(
        user=user,
        extension='py',
        lines_added=100,
        lines_deleted=50,
        period=date,
    )


def test_insert_file_statistics_data_blank_success(user, date, ):
    assert UserFileStatistics.objects.count() == 0
    data_to_insert = {
        '{}_py'.format(user.id): {'lines_added': 50, 'lines_deleted': 30, },
        '{}_txt'.format(user.id): {'lines_added': 120, 'lines_deleted': 40, },
        '{}_cc'.format(user.id): {'lines_added': 456, 'lines_deleted': 20, },
    }
    insert_file_statistics_data(data_to_insert, date, [user.id], )
    assert UserFileStatistics.objects.count() == 3
    py_file = UserFileStatistics.objects.get(user=user, extension='py')
    txt_file = UserFileStatistics.objects.get(user=user, extension='txt')
    cc_file = UserFileStatistics.objects.get(user=user, extension='cc')
    assert py_file.lines_added == 50
    assert py_file.lines_deleted == 30
    assert txt_file.lines_added == 120
    assert txt_file.lines_deleted == 40
    assert cc_file.lines_added == 456
    assert cc_file.lines_deleted == 20


def test_insert_file_statistics_data_existing_success(user, date, py_file_statistics, ):
    assert UserFileStatistics.objects.count() == 1
    data_to_insert = {
        '{}_py'.format(user.id): {'lines_added': 50, 'lines_deleted': 30, },
        '{}_txt'.format(user.id): {'lines_added': 120, 'lines_deleted': 40, },
    }
    insert_file_statistics_data(data_to_insert, date, [user.id], )
    assert UserFileStatistics.objects.count() == 2

    py_file = UserFileStatistics.objects.get(user=user, extension='py')
    txt_file = UserFileStatistics.objects.get(user=user, extension='txt')
    assert py_file.lines_added == py_file_statistics.lines_added + 50
    assert py_file.lines_deleted == py_file_statistics.lines_deleted +30
    assert txt_file.lines_added == 120
    assert txt_file.lines_deleted == 40


def test_insert_file_statistics_data_existing_another_date_success(user, date,
                                                                   another_date, py_file_statistics,
                                                                   ):
    assert UserFileStatistics.objects.count() == 1
    data_to_insert = {
        '{}_py'.format(user.id): {'lines_added': 50, 'lines_deleted': 30, },
        '{}_txt'.format(user.id): {'lines_added': 120, 'lines_deleted': 40, },
    }
    insert_file_statistics_data(data_to_insert, another_date, [user.id], )
    assert UserFileStatistics.objects.count() == 3

    txt_file = UserFileStatistics.objects.get(user=user, extension='txt')
    py_files = UserFileStatistics.objects.filter(user=user, extension='py')
    assert py_files.count() == 2
    py_file = py_files.get(period=another_date)
    assert py_file.lines_added == 50
    assert py_file.lines_deleted == 30
    py_file = py_files.get(period=date)
    assert py_file.lines_added == 100
    assert py_file.lines_deleted == 50
    assert txt_file.lines_added == 120
    assert txt_file.lines_deleted == 40
