# -*- coding: utf-8 -*-


import pytest
from idm.users import ranks

from idm.users.models import Group
from idm.core.constants.affiliation import AFFILIATION


pytestmark = pytest.mark.django_db


def test_user_heads(users_for_test):
    """Проверяем определение цепочки начальников и непосредственого руководителя"""
    (art, fantom, terran, admin) = users_for_test

    root = Group.objects.get_root('department')
    d1 = Group.objects.create(slug='d1', type='department', parent=root)
    d2 = Group.objects.create(slug='d2', parent=d1, type='department')
    d3 = Group.objects.create(slug='d3', parent=d2, type='department')
    d4 = Group.objects.create(slug='d4', parent=d3, type='department')
    d1.responsibilities.create(rank=ranks.HEAD, user=terran, is_active=True)
    d2.responsibilities.create(rank=ranks.HEAD, user=art, is_active=True)
    d3.responsibilities.create(rank=ranks.HEAD, user=fantom, is_active=True)
    d4.responsibilities.create(rank=ranks.HEAD, user=art, is_active=True)

    terran.department_group = d1
    terran.save()

    art.department_group = d4
    art.save()

    assert terran.all_heads == []
    assert terran.head is None

    assert art.all_heads == [fantom, terran]
    assert art.head == fantom


def test_user_head_if_it_is_not_direct(users_for_test):
    """Проверим случай, когда у некоторых подразделений в цепочке нет руководителя"""
    art, fantom, terran, admin = users_for_test

    root = Group.objects.get_root('department')
    d1 = Group.objects.create(slug='d1', parent=root)
    d2 = Group.objects.create(slug='d2', parent=d1)
    d3 = Group.objects.create(slug='d3', parent=d2)
    d4 = Group.objects.create(slug='d4', parent=d3)
    d1.responsibilities.create(rank=ranks.HEAD, user=terran, is_active=True)
    d3.responsibilities.create(rank=ranks.HEAD, user=fantom, is_active=True)

    terran.department_group = d1
    terran.save()

    art.department_group = d4
    art.save()

    assert terran.all_heads == []
    assert terran.head is None

    assert art.all_heads == [fantom, terran]
    assert art.head == fantom


def test_user_internal_heads(flat_arda_users):
    """Поиск ближайшего внутреннего руководителя"""
    users = (flat_arda_users.frodo, flat_arda_users.bilbo, flat_arda_users.sam,
             flat_arda_users.meriadoc, flat_arda_users.peregrin)
    frodo, bilbo, sam, meriadoc, peregrin = users

    root = Group.objects.get_root('department')
    d1 = Group.objects.create(slug='d1', parent=root)
    d2 = Group.objects.create(slug='d2', parent=d1)
    d3 = Group.objects.create(slug='d3', parent=d2)
    d1.responsibilities.create(rank=ranks.HEAD, user=frodo, is_active=True)
    d2.responsibilities.create(rank=ranks.HEAD, user=bilbo, is_active=True)
    d3.responsibilities.create(rank=ranks.HEAD, user=sam, is_active=True)

    frodo.department_group = d1
    frodo.save()

    bilbo.department_group = d2
    bilbo.save()

    meriadoc.department_group = d2
    meriadoc.affiliation = AFFILIATION.EXTERNAL
    meriadoc.save()

    sam.department_group = d3
    sam.affiliation = AFFILIATION.EXTERNAL
    sam.save()

    peregrin.department_group = d3
    peregrin.affiliation = AFFILIATION.EXTERNAL
    peregrin.save()

    correct_internal_heads = (None, frodo, bilbo, bilbo, bilbo)
    for user, answer in zip(users, correct_internal_heads):
        assert user.internal_head == answer

