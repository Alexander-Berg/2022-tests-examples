# coding: utf-8

import pytest

from temp.igogor.balance_objects import Client, Person, Contract, Order, RequestOrder, Line, Request, \
    Invoice, Act, Context

pytestmark = [pytest.mark.parametrize('clazz, defined_attr, optional_attr',
                                      [(Client, 'id', 'NAME'),
                                       (Person, 'type', 'ADDRESS'),
                                       (Contract, 'start_dt', 'DATE'),
                                       (Order, 'service', 'SOMETHING'),
                                       (RequestOrder, 'qty', 'ANYTHING'),
                                       (Request, 'client', 'JUST_THIS'),
                                       (Line, 'begin_dt', 'NO_IMAGINATION'),
                                       (Invoice, 'paysys', 'TIRED'),
                                       (Act, 'external_id', 'SO_TIRED'),
                                       (Context, 'name', 'FFFFFF')]
                                      )]


def test_init_attributes(clazz, defined_attr, optional_attr):
    objekt = clazz().new(**{defined_attr: 1111})
    assert getattr(objekt, defined_attr) == 1111

    objekt = clazz().new(**{optional_attr: 'Frederic'})
    assert objekt[optional_attr] == 'Frederic'
    assert objekt.params == {optional_attr: 'Frederic'}

    objekt = clazz().new(**{defined_attr: 1111, optional_attr: 'Frederic'})
    assert getattr(objekt, defined_attr) == 1111
    assert objekt[optional_attr] == 'Frederic'
    assert objekt.params == {optional_attr: 'Frederic'}


def test_change_attributes(clazz, defined_attr, optional_attr):
    objekt = clazz().new(**{defined_attr: 1111, optional_attr: 'Frederic'})

    objekt2 = objekt.new(**{defined_attr: 2222})
    assert getattr(objekt, defined_attr) == 1111
    assert objekt[optional_attr] == 'Frederic'
    assert getattr(objekt2, defined_attr) == 2222
    assert objekt2[optional_attr] == 'Frederic'

    objekt3 = objekt2.new(**{optional_attr: 'Johnny'})
    assert getattr(objekt2, defined_attr) == 2222
    assert objekt2[optional_attr] == 'Frederic'
    assert getattr(objekt3, defined_attr) == 2222
    assert objekt3[optional_attr] == 'Johnny'

    objekt4 = objekt3.new(**{defined_attr: 4444, optional_attr: 'Freddy'})
    assert getattr(objekt3, defined_attr) == 2222
    assert objekt3[optional_attr] == 'Johnny'
    assert getattr(objekt4, defined_attr) == 4444
    assert objekt4[optional_attr] == 'Freddy'

    setattr(objekt, defined_attr, 3333)
    assert getattr(objekt, defined_attr) == 3333
    assert objekt[optional_attr] == 'Frederic'

    objekt[optional_attr] = 'Ken'
    assert getattr(objekt, defined_attr) == 3333
    assert objekt[optional_attr] == 'Ken'


def test_params(clazz, defined_attr, optional_attr):
    objekt = clazz().new(**{defined_attr: 1111, optional_attr: 'Frederic'})
    assert objekt.params == {optional_attr: 'Frederic'}

    objekt = objekt.new(ANOTHER='Pfff', other='olololo')
    assert objekt.params == {optional_attr: 'Frederic', 'ANOTHER': 'Pfff', 'other': 'olololo'}

    objekt = objekt.new(**{defined_attr: 1111})
    assert objekt.params == {optional_attr: 'Frederic', 'ANOTHER': 'Pfff', 'other': 'olololo'}


def test_changes(clazz, defined_attr, optional_attr):
    objekt = clazz()
    assert (objekt.changes, objekt.params_changes) == ({}, {})

    objekt = clazz().new(**{defined_attr: 1111, optional_attr: 'Frederic'})
    assert (objekt.changes, objekt.params_changes) == ({defined_attr: 1111, optional_attr: 'Frederic'},
                                                       {optional_attr: 'Frederic'})

    objekt = clazz().new(**{defined_attr: 2222})
    assert (objekt.changes, objekt.params_changes) == ({defined_attr: 2222}, {})

    objekt = objekt.new(**{optional_attr: 'Johnny'})
    assert (objekt.changes, objekt.params_changes) == ({optional_attr: 'Johnny'}, {optional_attr: 'Johnny'})


def test_empty_object(clazz, defined_attr, optional_attr):
    objekt = clazz()
    assert objekt.params == {}
    assert objekt.changes == {}
    assert objekt.params_changes == {}


def test_object_equality(clazz, defined_attr, optional_attr):
    assert clazz() == clazz()
    # assert Client() == Client().new()  # todo-igogor спорно

    objekt1 = clazz().new(**{defined_attr: 1111, optional_attr: 'Frederic'})
    objekt2 = clazz().new(**{defined_attr: 1111, optional_attr: 'Frederic'})
    assert objekt1 == objekt2

    objekt3 = clazz().new(**{defined_attr: 1111}).new(**{optional_attr: 'Frederic'})
    assert objekt3 == objekt2

    objekt4 = clazz().new(**{optional_attr: 'Frederic'}).new(**{defined_attr: 1111})
    assert objekt4 == objekt3

    objekt5 = clazz().new(**{defined_attr: clazz().new(**{optional_attr: 'Frederic'})})
    objekt6 = clazz().new(**{defined_attr: clazz().new(**{optional_attr: 'Frederic'})})
    assert objekt5 == objekt6

    objekt7 = clazz().new(**{defined_attr: 1111})
    objekt8 = clazz().new(**{defined_attr: 2222})
    assert objekt7 != objekt8

    objekt9 = clazz().new(**{optional_attr: 'Frederic'})
    objekt10 = clazz().new(**{optional_attr: 'Johnny'})
    assert objekt9 != objekt10

    objekt11 = clazz().new(**{defined_attr: 1111})
    objekt12 = clazz().new(**{optional_attr: 1111})
    assert objekt11 != objekt12
