# coding: utf-8


import os

import datetime

import pytest
from celery.schedules import crontab
from django.utils import timezone
from django.utils.encoding import force_text
from django.core.management import get_commands


def assert_int(x):
    try:
        return int(x)
    except ValueError:
        raise AssertionError('"{}" is not a valid integer number'.format(x))


def to_crontab_format(parts):
    crontab = []
    for i in range(len(parts)):
        num = assert_int(parts[i])
        res = parts[i] if num >= 0 else '*/{}'.format(-num)
        crontab.append(res)

    # Не умеем парсить выполнение по дням недели, так что пусть будет будет каждый день
    if crontab[-1] in [str(x) for x in range(1, 8)]:
        crontab[-1] = '*'

    return crontab


def check_line(line):
    line = force_text(line).strip()
    if not line.startswith('cron = '):
        return
    line = line[len('cron = '):]

    parts = line.split(' ', 5)
    command = parts.pop()

    parts = to_crontab_format(parts)
    schedule = crontab(*parts)

    # обойдём конструкцию IF_PRODUCTION &&
    command = command.split('&&', 1)[-1]
    params = command.split(' ', 2)
    idm, management_command = params[:2]
    assert idm == 'idm'
    all_commands = get_commands()
    klass = all_commands[management_command]

    # все действия выполняются хотя бы раз в три дня
    days = 3
    assert schedule.remaining_estimate(timezone.now()) < datetime.timedelta(days=days)
    return klass


def test_invalid_time():
    with pytest.raises(ValueError):
        check_line('cron = 30 25 -1 -1 1 idm idm_send_roles_reminders')


# Диапазоны в uwsgi? Пффф
def test_invalid_interval():
    with pytest.raises(AssertionError):
        check_line('cron = 30 9 -1 -1 1-5 idm idm_send_roles_reminders')


# Никаких звёздочек
def test_invalid_stars():
    with pytest.raises(AssertionError):
        check_line('cron = 30 9 * * 3 idm idm_send_roles_reminders')


def test_invalid_command():
    with pytest.raises(KeyError) as exc:
        check_line('cron = 30 9 -1 -1 4 idm idm_nonsense')
    assert exc.value.args[0] == 'idm_nonsense'


def test_crontab_is_valid(settings):
    cron_path = os.path.normpath(
        os.path.join(settings.MODULE_ROOT, '..', 'uwsgi.ini')
    )
    with open(cron_path, 'rb') as crontab:
        lines = crontab.readlines()

    ilines = iter(lines)

    for line in ilines:
        check_line(line)
