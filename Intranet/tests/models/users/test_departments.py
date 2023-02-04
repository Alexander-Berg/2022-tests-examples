# coding: utf-8


import pytest

from idm.users.models import Department
from idm.tests.utils import create_user

pytestmark = [pytest.mark.django_db, pytest.mark.robotless]


def test_subdepartments():
    assert Department.objects.count() == 0

    yandex = Department.objects.create(
        id=3,
        name='Яндекс',
    )
    akadem = Department.objects.create(
        id=392,
        name='Отдел по работе с академическими программами',
        parent=yandex,
    )
    Department.objects.create(
        id=450,
        name='Группа практики школы анализа данных',
        parent=akadem,
    )

    assert Department.objects.count() == 3


def test_bind_user():
    user = create_user('art')
    yandex = Department.objects.create(name='Яндекс')

    assert yandex.users2.count() == 0
    yandex.users2.add(user)
    assert yandex.users2.count() == 1
