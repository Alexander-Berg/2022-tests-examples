from datetime import datetime
from uuid import uuid4

import pytest
from mdh.core.models import Schema, Queue, STATUS_ARCHIVED
from mdh.core.schemas.base import DataModel
from mdh.core.toolbox.localized import LANG__DEFAULT
from pydantic import ValidationError, BaseModel


class DummySchema(DataModel):

    myint: int


def test_display(init_schema, init_user):

    user = init_user()

    with pytest.raises(ValidationError) as e:
        # Неизвестное поле в me.
        init_schema(alias='sch1', user=user, display={'me': '{{one}} - {{ two }}'})
    assert 'Display value' in e.value.errors()[0]['msg']

    with pytest.raises(ValidationError) as e:
        # Неизвестное поле в parent_field.
        init_schema(alias='sch1', user=user, display={'parent_field': 'bogus'})
    assert 'set as parent' in e.value.errors()[0]['msg']

    with pytest.raises(ValidationError) as e:
        # Неизвестное поле в widgets.
        init_schema(alias='sch1', user=user, display={'widgets': {'string1': {'alias': 'bobogus'}}})
    assert 'widget alias `bobogus`' in e.value.errors()[0]['msg']

    with pytest.raises(ValidationError) as e:
        # Неизвестное поле в constraints.
        init_schema(alias='sch1', user=user, constraints=[{'alias': 'abc', 'attrs': ['string1', 'bobobogus']}])
    assert '`bobobogus` used for constraint' in e.value.errors()[0]['msg']

    schema1 = init_schema(
        alias='sch1',
        user=user,
        constraints=[
            {'alias': 'abc', 'attrs': ['string1', 'integer1']},
            {'alias': 'a', 'attrs': []},  # будет подчищен
            {'alias': 'b', 'attrs': []},
        ],
        display={
            'me': '{{ _master_uid }} -- {{ string1 }}',
            'widgets': {
                'string1': {
                    'alias': 'textarea',
                }
            },
            'linked': [
                {'dom': 'domain1', 'ref': 'reference1', 'lnk': {'srcattr': 'dstattr'}}
            ],
        })

    assert f'{schema1}'
    assert 'widgets' in schema1.display
    assert 'linked' in schema1.display
    assert 'parent_field' in schema1.display
    constraints = schema1.constraints
    assert len(constraints) == 1
    constraint = constraints[0]
    assert constraint['attrs'] == ['string1', 'integer1']

    assert schema1.display_schema == (
        '{"alias": "sch1", "titles": {}, "hints": {}, "fields": '
        '[{"alias": "string1", "type": {"alias": "str", "params": {}}, "titles": {"en": "Текстовое поле"}, '
        '"hints": {"en": "Описание для текстового поля"}, "default": {"const": "myvalue1", "dyn": null}, '
        '"choices": {"const": [{"titles": {"en": "Значение 1"}, "val": "myvalue1"}, {"titles": {"en": "Значение 2"}, '
        '"val": "myvalue2"}, {"titles": {"en": "Значение 3"}, "val": "myvalue3"}], "dyn": null}, "validators": '
        '[{"alias": "strlen", "params": {"min": 1, "max": 30}}, {"alias": "re", "params": {"val": "myvalue*"}}]}, '
        '{"alias": "integer1", "type": {"alias": "int", "params": {}}, "titles": {"en": "Поле для целого"}, '
        '"hints": {"en": "Описание для поля с целым"}, "default": {"const": "⁇", "dyn": null}, '
        '"choices": {"const": [], "dyn": null}, "validators": []}], '
        '"display": {"me": "{{ _master_uid }} -- {{ string1 }}", '
        '"widgets": {"string1": {"alias": "textarea", "params": {}}, "integer1": {"alias": "number", "params": {}}}, '
        '"linked": [{"dom": "domain1", "ref": "reference1", "lnk": {"srcattr": "dstattr"}}], "parent_field": ""}, '
        '"version": 1, "version_master": 0}'
    )


def test_attributes(init_schema_attr_dict, init_user):

    schema_fields = [
        init_schema_attr_dict(
            alias='str1',
            type='str',
            default={'const': 'somestr1'},
            choices={
                'const': [
                    {'val': 'somestr1', 'titles': {LANG__DEFAULT: 'opt1'}},
                    {'val': 'somestr111', 'titles': {LANG__DEFAULT: 'opt2'}},
                ],
                'dyn': None,
            },
        ),

        init_schema_attr_dict(
            alias='str2',
            type='str',
            validators=[
                {'alias': 'strlen', 'params': {'min': 1, 'max': 1}},
                {'alias': 're', 'params': {'val': 'Y'}},
            ],
        ),

        init_schema_attr_dict(
            alias='int1',
            type='int',
        ),

        init_schema_attr_dict(
            alias='fk1',
            type='fk',
            type_params={'ref': 2},
        ),

        init_schema_attr_dict(
            alias='map1',
            type='map',
            type_params={},
        ),

        init_schema_attr_dict(
            alias='dt1',
            type='datetime',
            default={'const': '2021-05-20T10:30'},
        ),
    ]

    schema1 = Schema(
        alias='schema1',
        fields=schema_fields,
        creator=init_user(),
    )
    schema1.save()
    schema1.refresh_from_db()  # чтобы проверить данные в полях после сериализации при сохрании

    attrs = schema1.field_objects

    # Проверка локализуемых полей.
    attr_str1 = attrs['str1']
    assert attr_str1.alias == 'str1'

    assert attr_str1.get_title() == 'str1_title'
    assert attr_str1.get_hint() == 'str1_hint'

    attr_str1.set_title('new', lang='ja')
    assert attr_str1.data['titles'] == {'en': 'str1_title', 'ja': 'new'}

    # Проверка материализатора схемы.
    materializer = schema1.materializer
    schema_model = materializer.run()

    assert issubclass(schema_model, BaseModel)

    # Далее попытки приведения данных к схеме.

    common_uuid = uuid4()

    # Невалидные данные.
    with pytest.raises(ValidationError) as e:
        schema1.apply_to({
            'str2': 'bogus',
            'fk1': str(common_uuid),
            'map1': 'aa',
        })

    assert e.value.errors() == [
        {'ctx': {'limit_value': 1},
         'loc': ('str2',), 'msg': 'ensure this value has at most 1 characters',
         'type': 'value_error.any_str.max_length'},
        {'loc': ('int1',), 'msg': 'field required', 'type': 'value_error.missing'},
        {'loc': ('map1',), 'msg': 'value is not a valid dict', 'type': 'type_error.dict'}
    ]

    # Валидные данные разными типами.
    result = schema1.apply_to({
        'str1': 10, 'str2': 'Y', 'int1': '20', 'fk1': f'{common_uuid}', 'map1': {'a': 'b'},
        'dt1': '2021-05-18T11:20',
    })
    assert result.dict() == {
        'int1': 20, 'str1': '10', 'str2': 'Y', 'fk1': common_uuid, 'map1': {'a': 'b'},
        'dt1': datetime(2021, 5, 18, 11, 20)
    }


def test_attr_default(init_schema_attr_dict):

    def check(data, *, default):

        schema = Schema(alias='schema', fields=[init_schema_attr_dict(
            alias='a', type='str', default=default,
        )])
        result = schema.apply_to(data)
        return result.dict()

    # Пропущено необязательное.
    assert check({}, default={'const': 'some'}) == {'a': 'some'}

    # Установлено необязательное.
    assert check({'a': 'other'}, default={'const': 'some'}) == {'a': 'other'}

    # Пропущено необязательное с динамическим умолчанием.
    assert '-' in str(check({}, default={'dyn': {'alias': 'uuid4'}})['a'])

    # Неизвестная функция для динамического умолчания.
    with pytest.raises(ValueError):
        check({}, default={'dyn': {'alias': 'unknown'}})


def test_startrek_issue(
        init_user, init_schema, django_assert_num_queries, assert_startrek_create_update, response_mock):

    user = init_user()

    schema_0 = init_schema(alias='sch0', user=user, issue='TESTMDH-7')
    assert schema_0.issue
    assert '0]' in schema_0.startrek_title

    Schema.publish(schema_0)
    assert '1]' in schema_0.startrek_title

    assert schema_0.is_master
    assert schema_0.is_published

    schema_1 = init_schema(alias='sch1', user=user, master_uid=schema_0.master_uid)

    assert not schema_1.issue

    with django_assert_num_queries(3) as _:
        assert schema_1.startrek_title
        descr = schema_1.startrek_description
        assert '    "widgets": {' in descr
        assert 'будет затронуто записей: 0' in descr
        assert schema_1.startrek_queue == 'TESTMDH'

    assert_startrek_create_update(obj=schema_1, issue_key='TESTMDH-8', bypass=False)


def test_publish_error(init_user, init_resource, monkeypatch, run_task):

    user = init_user()

    resource = init_resource(user=user, publish=True)
    schema = resource.schema

    def get_clone(base):
        new = schema.prepare_clone(editor=user)
        new.set_master(to=base)
        new.save()
        return new

    def patched(*args, **kwargs):
        DummySchema(dum=1)

    schema_new = get_clone(schema)
    Schema.publish(schema_new)

    # Проверим собственно ошибку.
    monkeypatch.setattr(Schema, 'publish_', patched)

    schema_new = get_clone(schema_new)
    schema_new.status = schema_new.STATUS_APPROVED
    schema_new.save()

    with pytest.raises(ValidationError) as e:
        run_task('publish_schema')

    schema_new.refresh_from_db()
    assert schema_new.is_on_review

    queue = list(Queue.objects.all())
    assert len(queue) == 3
    error = queue[2]

    assert error.action_object.params.err == (
        '[\n  {\n    "loc": [\n      "myint"\n    ],\n    "msg": "field required",'
        '\n    "type": "value_error.missing"\n  }\n]')


def test_schema_rollout(init_user, init_schema, init_resource, django_assert_num_queries):

    user = init_user()

    resource = init_resource(user=user, publish=True)
    resource_schema = resource.schema_id
    reference_schema = resource.reference.schema_id

    record_0 = resource.record_add(attrs={'integer1': 1}, creator=user)
    record_0.publish(record_0)
    record_0_dt_upd = record_0.dt_upd

    record_1 = resource.record_add(attrs={'integer1': 2}, creator=user)
    record_1.publish(record_1)

    # Архивная запись при раскатке новой схемы остаётся использовать старую.
    record_2 = resource.record_add(attrs={'integer1': 0}, creator=user, status=STATUS_ARCHIVED)

    schema_v0 = resource.schema
    assert schema_v0.is_published

    # Проверка удачного обновления связанных сущностей.
    schema_v1 = init_schema(alias='sch1', user=user, master_uid=schema_v0.master_uid)
    schema_v1.fields[1]['type']['alias'] = 'str'  # обновялем поле 'integer1'
    schema_v1.save()

    with django_assert_num_queries(2) as _:
        check_result = schema_v1.check_records()

    assert check_result.total == 2  # здесь нет третьей записи.
    assert check_result.success == 2
    assert not check_result.errors
    assert check_result.src == schema_v0
    assert check_result.dst == schema_v1

    schema_v1.publish(schema_v1)

    # Обновилась схема ресурса.
    resource.refresh_from_db()
    assert resource.schema_id > resource_schema
    resource_schema = resource.schema_id

    # Обновилась схема справочника.
    resource.reference.refresh_from_db()
    assert resource.reference.schema_id > reference_schema

    # Архивная запись не была затронута.
    record_0.refresh_from_db()
    assert record_0.schema_id == schema_v1.id

    record_1.refresh_from_db()
    assert record_1.schema_id == schema_v1.id

    record_2.refresh_from_db()
    assert record_2.schema_id == schema_v0.id

    # Целое транфсормировалось в строку.
    record_0.refresh_from_db()
    assert record_0.dt_upd > record_0_dt_upd
    assert record_0.attrs['integer1'] == '1'

    # Проверка неудачного обновления связанных сущностей.
    with pytest.raises(ValueError):
        schema_v2 = init_schema(alias='sch1', user=user, master_uid=schema_v0.master_uid)
        schema_v2.fields[1]['type']['alias'] = 'bool'
        schema_v2.save()

        with django_assert_num_queries(2) as _:
            check_result = schema_v2.check_records()
        assert check_result.total == 2
        assert check_result.success == 1

        schema_v2.publish(schema_v2)

    # Проверим, что данные о сбойных записях есть в описании для трекера.
    with django_assert_num_queries(3) as _:
        descr = schema_v2.startrek_description
        assert '1 шт.: %%integer1:: value could not be' in descr
        assert 'неприменимо к: 1' in descr
        assert f'{record_1.record_uid}' in descr

    # Проверяем, что публикация откатилась.
    resource.refresh_from_db()
    assert resource.schema_id == resource_schema
