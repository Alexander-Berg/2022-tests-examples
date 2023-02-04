# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime

import pytest

from balance import muzzle_util as ut

from autodasha.solver_cl.settings_creation.states_lib import *


class SampleState(ObjectState):
    NAME = 'Пример'
    TABLE_NAME = 'bo.t_sample_state'
    TABLE_PRIMARY_KEY = 'id'

    id = StateAttribute('ИД')
    bool = BoolAttribute('Булеан')
    string = StringAttribute('Стринг')
    date = DateAttribute('Дате')
    object = ObjectAttribute(
        'Обжект',
        lambda x: x.name,
        lambda x: x.id
    )
    list = ListAttribute(
        'Лист',
        lambda x: x.name,
        lambda x: x.id,
    )
    literal = LiteralAttribute(666)


def test_dml_attributes():
    state = SampleState(
        id=1,
        bool=True,
        string='аляляля',
        date=datetime.datetime(2666, 6, 6),
        object=ut.Struct(id=42, name='объект'),
    )

    assert state.dml.id == 1
    assert state.dml.bool == 1
    assert state.dml.string == "'аляляля'"
    assert state.dml.date == "date'2666-06-06'"
    assert state.dml.object == 42

    assert state.dml.keys() == ['id', 'bool', 'string', 'date', 'object', 'literal']
    assert state.dml.values() == [1, 1, "'аляляля'", "date'2666-06-06'", 42, 666]
    assert state.dml.items() == [
        ('id', 1),
        ('bool', 1),
        ('string', "'аляляля'"),
        ('date', "date'2666-06-06'"),
        ('object', 42),
        ('literal', 666),
    ]


def test_dml_attributes_null():
    state = SampleState()

    assert state.dml.id == 'null'
    assert state.dml.bool == 'null'
    assert state.dml.string == 'null'
    assert state.dml.date == 'null'
    assert state.dml.object == 'null'
    assert state.dml.literal == 666
    assert state.dml_list.list == []

    assert state.fmt.id == '<отсутствует>'
    assert state.fmt.bool == '<отсутствует>'
    assert state.fmt.string == '<отсутствует>'
    assert state.fmt.date == '<отсутствует>'
    assert state.fmt.object == '<отсутствует>'
    assert state.fmt.list == '<отсутствует>'

    assert state.dml.keys(skip_none=True) == ['literal']
    assert state.dml.values(skip_none=True) == [666]
    assert state.dml.items(skip_none=True) == [('literal', 666)]


def test_dml_object_attributes_null():
    state = SampleState(
        object=ut.Struct(id=None, name=None),
        list=[ut.Struct(id=None, name=None)]
    )

    assert state.dml.object == 'null'
    assert state.dml_list.list == ['null']
    assert state.fmt.object == '<отсутствует>'
    assert state.fmt.list == '<отсутствует>'


def test_dml_attributes_list():
    state = SampleState(
        list=[ut.Struct(id=42, name='список'), ut.Struct(id=24, name='другой список')]
    )

    with pytest.raises(AttributeError):
        v = state.dml.list
    assert state.dml_list.list == [42, 24]
    assert state.dml_list.keys() == ['list']
    assert state.dml_list.values() == [[42, 24]]
    assert state.dml_list.items() == [('list', [42, 24])]


def test_dml_format_insert():
    SampleState.DML_FORMAT = INSERT_FORMAT
    state = SampleState(
        id=1,
        bool=True,
        string='аляляля',
        date=datetime.datetime(2666, 6, 6),
        object=ut.Struct(id=42, name='объект'),
        list=[ut.Struct(id=42, name='список'), ut.Struct(id=24, name='другой список')]
    )

    assert state.get_full_dml() == '''
INSERT INTO bo.t_sample_state (id, bool, string, date, object, literal)
VALUES (1, 1, 'аляляля', date'2666-06-06', 42, 666)
    '''.strip()


def test_dml_format_merge():
    SampleState.DML_FORMAT = MERGE_FORMAT
    state = SampleState(
        id=1,
        bool=True,
        string='аляляля',
        date=datetime.datetime(2666, 6, 6),
        object=ut.Struct(id=42, name='объект'),
        list=[ut.Struct(id=42, name='список'), ut.Struct(id=24, name='другой список')]
    )

    assert state.get_full_dml() == '''
MERGE INTO bo.t_sample_state t
USING (
  SELECT
    1 id,
    1 bool,
    'аляляля' string,
    date'2666-06-06' date,
    42 object,
    666 literal
  FROM dual
) d
ON (t.id = d.id)
WHEN NOT MATCHED THEN
  INSERT (id, bool, string, date, object, literal)
  VALUES (d.id, d.bool, d.string, d.date, d.object, d.literal)
WHEN MATCHED THEN
  UPDATE SET
    t.bool = d.bool,
    t.string = d.string,
    t.date = d.date,
    t.object = d.object,
    t.literal = d.literal
'''.strip()


def test_fmt_attributes():
    state = SampleState(
        id=1,
        bool=True,
        string='аляляля',
        date=datetime.datetime(2666, 6, 6),
        object=ut.Struct(id=42, name='объект'),
        list=[ut.Struct(id=42, name='список'), ut.Struct(id=24, name='другой список')]
    )

    assert state.fmt.id == 1
    assert state.fmt.bool == 'Да'
    assert state.fmt.string == 'аляляля'
    assert state.fmt.date == '2666-06-06'
    assert state.fmt.object == 'объект'
    assert state.fmt.list == 'список, другой список'

    assert state.fmt.keys() == ['ИД', 'Булеан', 'Стринг', 'Дате', 'Обжект', 'Лист']
    assert state.fmt.values() == [1, 'Да', 'аляляля', '2666-06-06', 'объект', 'список, другой список']
    assert state.fmt.items() == [
        ('ИД', 1),
        ('Булеан', 'Да'),
        ('Стринг', 'аляляля'),
        ('Дате', '2666-06-06'),
        ('Обжект', 'объект'),
        ('Лист', 'список, другой список')
    ]


def test_fmt_format():
    state = SampleState(
        id=1,
        bool=True,
        string='аляляля',
        date=datetime.datetime(2666, 6, 6),
        object=ut.Struct(id=42, name='объект'),
        list=[ut.Struct(id=42, name='список'), ut.Struct(id=24, name='другой список')]
    )

    assert state.get_description() == '''
Пример:
* ИД: 1
* Булеан: Да
* Стринг: аляляля
* Дате: 2666-06-06
* Обжект: объект
* Лист: список, другой список
'''.strip()


def test_fmt_skip_getter():
    class SampleSkippedGetterState(ObjectState):
        NAME = 'Пример'
        TABLE_NAME = 'bo.t_sample_state'
        TABLE_PRIMARY_KEY = 'id'

        id = StateAttribute('ИД')
        other_id = StateAttribute('ОЗЕР ИД', SKIP_GETTER)

    state = SampleSkippedGetterState(id=1, other_id=666)

    with pytest.raises(AttributeError):
        v = state.fmt.other_id
    assert state.fmt.keys() == ['ИД']
    assert state.fmt.values() == [1]
    assert state.fmt.items() == [('ИД', 1)]


def test_query_parameter():
    state = SampleState(
        id=QueryParameter('id'),
        object=QueryParameter('object_id'),
        list=[
            QueryParameter('list_1'),
            QueryParameter('list_2')
        ]
    )

    assert state.dml.id == ':id'
    assert state.dml.object == ':object_id'
    assert state.dml_list.list == [':list_1', ':list_2']

    assert state.fmt.id == '<требуется указать>'
    assert state.fmt.object == '<требуется указать>'
    assert state.fmt.list == '<требуется указать>, <требуется указать>'

    state.id.value = 666
    state.object.value = ut.Struct(id=667, name='Объект')
    for idx, l_el in enumerate(state.list):
        l_el.value = ut.Struct(id=idx, name='Список %s' % idx)

    assert state.dml.id == 666
    assert state.dml.object == 667
    assert state.dml_list.list == [0, 1]

    assert state.fmt.id == 666
    assert state.fmt.object == 'Объект'
    assert state.fmt.list == 'Список 0, Список 1'


def test_object_query_parameter():
    state = SampleState(
        object=ut.Struct(id=QueryParameter('object_id'), name='Объект'),
        list=[
            ut.Struct(id=QueryParameter('list_1'), name='Список 1'),
            ut.Struct(id=QueryParameter('list_2'), name='Список 2'),
        ]
    )

    assert state.dml.object == ':object_id'
    assert state.dml_list.list == [':list_1', ':list_2']
    assert state.fmt.object == 'Объект'
    assert state.fmt.list == 'Список 1, Список 2'

    state.object.id.value = 666
    for idx, l_el in enumerate(state.list):
        l_el.id.value = idx

    assert state.dml.object == 666
    assert state.dml_list.list == [0, 1]
    assert state.fmt.object == 'Объект'
    assert state.fmt.list == 'Список 1, Список 2'


def test_related_query_parameter():
    class RelatedSampleState(ObjectState):
        id = StateAttribute('ID')
        name = StringAttribute('Имя')

    rel_state_obj = RelatedSampleState(id=QueryParameter('id'), name='имя объекта')
    rel_state_list_1 = RelatedSampleState(id=QueryParameter('list_id_1'), name='списка 1')
    rel_state_list_2 = RelatedSampleState(id=QueryParameter('list_id_2'), name='списка 2')

    state = SampleState(
        object=rel_state_obj,
        list=[rel_state_list_1, rel_state_list_2]
    )

    assert state.dml.object == ':id'
    assert state.dml_list.list == [':list_id_1', ':list_id_2']
    assert state.fmt.object == 'имя объекта'
    assert state.fmt.list == 'списка 1, списка 2'

    rel_state_obj.id.value = 666
    rel_state_list_1.id.value = 667
    rel_state_list_2.id.value = 668

    assert state.dml.object == 666
    assert state.dml_list.list == [667, 668]
