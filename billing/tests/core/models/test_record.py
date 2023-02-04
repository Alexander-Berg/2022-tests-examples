from mdh.core.models import Record, Nested
from mdh.core.models.schema import DEFAULT_DISPLAY_ME
from mdh.core.models.aux_cache import RecordSerializationCache
from mdh.core.schemas.attributes import FieldRepresentation, SchemaField
from mdh.core.schemas.widgets import Localized


def test_basic(init_user, init_schema, init_reference, init_domain, init_node):

    user1 = init_user()

    domain = init_domain('domain1', user=user1)
    schema = init_schema('schema1', user=user1)
    schema.display['me'] = 'some: {{integer1}} -> %s' % DEFAULT_DISPLAY_ME
    assert schema.version == 1
    schema.save()
    assert schema.version == 1

    schema.save(bump_version=True)
    assert schema.version == 2

    reference = init_reference('reference1', user=user1)

    node_mdh = init_node('node1', user=user1)
    node_service = init_node('node2', user=user1)

    reference = domain.add_reference(reference)[0]

    resource_mdh = reference.resource_add(creator=user1, schema=schema, node=node_mdh)
    resource_service = reference.resource_add(creator=user1, schema=schema, node=node_service)

    # Добавление золотой записи в mdh (допустим через веб-интерфейс).
    record_mdh_1 = resource_mdh.record_add(attrs={'integer1': 1}, creator=user1, remote_id='3040')
    assert record_mdh_1.is_draft
    assert record_mdh_1.record_uid
    assert record_mdh_1.master_uid == record_mdh_1.record_uid
    assert not record_mdh_1.is_master
    assert record_mdh_1.version == 1
    val = f'some: 1 -> {record_mdh_1.master_uid}'
    assert record_mdh_1.me_dict == {'ru': val}
    assert record_mdh_1.me_localized == val
    assert record_mdh_1.status_title == 'Draft'
    assert record_mdh_1.remote_id == '3040'

    # Далее процесс согласования и публикации золота.
    record_mdh_1.set_master()
    record_mdh_1.mark_published()
    record_mdh_1.save(bump_version=True)
    assert record_mdh_1.is_master
    assert record_mdh_1.version == 2

    # Проверим адресацию по uid.
    picked = Record.get_by_uid(f'{record_mdh_1.master_uid}')
    assert picked.id == record_mdh_1.id

    # Добавляем новую запись.
    record_mdh_2 = resource_mdh.record_add(
        master_uid=record_mdh_1.record_uid,
        attrs={'integer1': 3},
        creator=user1,
    )
    assert record_mdh_2.record_uid != record_mdh_1.record_uid
    assert record_mdh_2.master_uid == record_mdh_1.master_uid
    assert not record_mdh_2.master_id
    assert record_mdh_2.remote_id == '3040'

    # Добавляем ещё одну.
    record_mdh_3 = resource_mdh.record_add(master_uid=record_mdh_1.record_uid, attrs={'integer1': 4}, creator=user1)

    # Проверим адресацию по uid.
    picked = Record.get_by_uid('12345678123456781234567812345678')
    assert picked is None

    picked = Record.get_by_uid(f'{record_mdh_2.master_uid}')
    assert picked.id == record_mdh_1.id

    Record.publish(record_mdh_2)

    # Предпочтение мастера.
    picked = Record.get_by_uid(f'{record_mdh_2.master_uid}')
    assert picked.id == record_mdh_2.id

    # Предпочтение конкретной записи.
    picked = Record.get_by_uid(f'{record_mdh_2.master_uid}', prefer_master=False)
    assert picked.id == record_mdh_1.id

    picked = Record.get_by_uid(f'{record_mdh_3.record_uid}')
    assert picked.id == record_mdh_3.id


def test_display_me(init_user, init_resource, init_schema_attr_dict):

    user1 = init_user()

    resource = init_resource(user=user1)

    schema = resource.schema
    schema.fields.extend((
        init_schema_attr_dict(alias='loc1', type='map'),
        init_schema_attr_dict(alias='loc2', type='map'),
    ))
    schema.display['me'] = '{{loc1}}-{{integer1}}-{{loc2}}'
    schema.display['widgets']['loc1'] = Localized().to_dict()
    schema.display['widgets']['loc2'] = Localized().to_dict()
    schema.save()

    record1 = resource.record_add(
        attrs={
            'integer1': 1,
            'loc1': {'ru': 'русский'},
            'loc2': {'en': 'english', 'ru': 'русиш'},
        },
        creator=user1)
    assert record1.me == {'ru': 'русский-1-русиш', 'en': '-1-english'}

    record2 = resource.record_add(
        attrs={'integer1': 33, 'loc1': {}, 'loc2': {}},
        creator=user1)
    assert record2.me == {'ru': '-33-'}

    # Проверим подстановку None
    record1.attrs['loc1'] = None
    represented = schema.represent_record(record1)
    assert represented == {'en': '-1-english', 'ru': '-1-русиш'}


def test_startrek_get_represented_str(init_schema_attr_dict, init_resource, init_user):

    def get_repr(field_dict, value):
        return Record.startrek_get_represented_str(
            FieldRepresentation(field=SchemaField(init_schema_attr_dict(**field_dict)), value=value))

    assert (
        get_repr({'alias': 'repr1', 'type': 'int'}, value=1) ==
        get_repr({'alias': 'repr2', 'type': 'int'}, value=1)
    )

    user = init_user()
    resource = init_resource(user=user)
    record1 = resource.record_add(
        creator=user, status=Record.STATUS_ON_REVIEW, issue='TESTMDH-1',
        attrs={'integer1': 30, 'fk1': None})

    record_repr = get_repr({'alias': 'repr3', 'type': 'fk'}, value=record1)
    assert '??---TESTMDH-1??' in record_repr
    assert str(record1.master_uid) in record_repr
    assert Record.startrek_get_represented_str(None) == ''

    assert get_repr({'alias': 'repr4', 'type': 'bool'}, value=False) == 'no ---!!(сер)False!!'
    assert (
        get_repr({'alias': 'repr5', 'type': 'datetime'}, value='2016-09-29T00:00:00') ==
        'Sept. 29, 2016, midnight ---!!(сер)2016-09-29T00:00:00!!')


def test_startrek_issue(
        init_resource_fk, django_assert_num_queries, assert_startrek_create_update, response_mock):

    resource = init_resource_fk()
    user = resource.creator

    record_fk = resource.record_add(creator=user, attrs={'integer1': 30, 'fk1': None})
    Record.publish(record_fk)

    record_base = resource.record_add(
        creator=user,
        attrs={'integer1': 30, 'fk1': None},
    )
    Record.publish(record_base)
    description = record_base.startrek_description
    assert '<unspecified>' in description

    record_0 = resource.record_add(
        attrs={'string1': 'myvalue1', 'integer1': 10, 'fk1': record_fk.master_uid},
        issue='TESTMDH-6', creator=user)
    assert '0]' in record_0.startrek_title

    with django_assert_num_queries(5) as _:
        # Среди sql есть начало транзакции и выяснение типа content type для linked.
        Record.publish(record_0)

    assert '1]' in record_0.startrek_title

    assert record_0.is_master
    assert record_0.is_published

    record_1 = resource.record_add(
        attrs={'string1': 'myvalue3', 'integer1': 30, 'fk1': record_base.master_uid},
        creator=user,
        master_uid=record_0.master_uid
    )

    assert not record_1.issue

    with django_assert_num_queries(3) as _:
        title = record_1.startrek_title
        assert record_1.reference.title in title
        assert f'{record_1.version_master}' in title
        assert record_1.me_localized == f'tstme-{record_1.master_uid}'
        description = record_1.startrek_description
        assert f'{record_fk.master_uid}))--' in description  # ссылка на старую внешнюю запись
        assert 'Значение 3' in description
        assert 'myvalue3' in description
        assert 'tstme-' in description  # me для связанной записи (внешнего ключа)
        assert record_1.startrek_queue == 'TESTMDH'

    assert_startrek_create_update(obj=record_1, issue_key='TESTMDH-9', bypass=False)


def test_publishing(init_resource_fk):

    res = init_resource_fk()
    user = res.creator

    # Базовая опубликованная.
    record_base_v1 = res.record_add(
        creator=user,
        attrs={'integer1': 30, 'fk1': None},
    )
    Record.publish(record_base_v1)
    assert not record_base_v1.foreigns.exists()
    assert not record_base_v1.used_initially.exists()
    assert not record_base_v1.used_currently.exists()

    # Базовая опубликованная с неопубликованной версией.
    record_base_alt_pub = res.record_add(creator=user, attrs={'integer1': 70})
    Record.publish(record_base_alt_pub)
    record_base_alt_unpub = res.record_add(
        creator=user, attrs={'integer1': 70}, master_uid=record_base_alt_pub.master_uid)

    # Базовая неопубликованная.
    record_base_unpublished = res.record_add(
        creator=user,
        attrs={'integer1': 80},
    )

    def get_foreign_str(foreigns):
        return '|'.join(f'{foreign}' for foreign in foreigns)

    # Дочерняя запись.
    record_slave_v1 = res.record_add(
        creator=user,
        attrs={
            'integer1': 50,
            'fk1': record_base_v1.master_uid,
            'fk2': record_base_alt_pub.master_uid,
            'fk3': record_base_unpublished.master_uid
        },
    )

    Record.publish(record_slave_v1)
    foreigns = list(record_slave_v1.foreigns.all())

    assert (
        get_foreign_str(foreigns) ==
        f'fk1: {record_slave_v1.id} -> {record_base_v1.id}|'
        f'fk2: {record_slave_v1.id} -> {record_base_alt_pub.id}|'
        f'fk3: {record_slave_v1.id} -> {record_base_unpublished.id}'
    )

    foreign = foreigns[0]
    assert foreign.foreign_initial_id == record_base_v1.id
    assert foreign.foreign_current_id == record_base_v1.id
    assert not record_slave_v1.used_initially.exists()
    assert not record_slave_v1.used_currently.exists()

    # Публикуются внешние, ранее неопубликованные.
    Record.publish(record_base_unpublished)
    Record.publish(record_base_alt_unpub)

    assert (
        get_foreign_str(record_slave_v1.foreigns.all()) ==
        f'fk1: {record_slave_v1.id} -> {record_base_v1.id}|'
        f'fk2: {record_slave_v1.id} -> {record_base_alt_unpub.id}|'
        f'fk3: {record_slave_v1.id} -> {record_base_unpublished.id}'
    )

    # Новая версия мастер записи 1.
    record_slave_v2 = res.record_add(
        creator=user,
        attrs={'integer1': 60, 'fk1': record_base_v1.master_uid},
        master_uid=record_slave_v1.master_uid,
    )
    Record.publish(record_slave_v2)
    foreign = record_slave_v2.foreigns.all()[0]
    assert foreign.foreign_initial_id == record_base_v1.id
    assert foreign.foreign_current_id == record_base_v1.id
    assert not record_slave_v2.used_initially.exists()
    assert not record_slave_v2.used_currently.exists()

    # Проверяем, что связи не затронуты добавлением нового золота.
    record_slave_v1.refresh_from_db()
    assert record_slave_v1.is_archived
    assert record_slave_v1.master_id == record_slave_v2.id
    foreign = record_slave_v1.foreigns.all()[0]
    assert foreign.foreign_initial_id == record_base_v1.id
    assert foreign.foreign_current_id == record_base_v1.id

    # Изменяется запись, на которую ссылаются.
    record_base_v2 = res.record_add(
        creator=user, attrs={'integer1': 5, 'fk1': None},
        master_uid=record_base_v1.master_uid,
    )
    Record.publish(record_base_v2)
    assert not record_base_v2.foreigns.exists()
    assert not record_base_v2.used_initially.exists()
    # Текущие ссылки обновились в двух записях (и в старой и в новой).
    assert record_base_v2.used_currently.count() == 2


def test_remote_id(init_user, init_resource,):

    user = init_user()
    resource = init_resource(user=user)

    record1 = resource.record_add(
        creator=user, status=Record.STATUS_ON_REVIEW, issue='TESTMDH-1',
        attrs={'integer1': 30, 'fk1': None})

    record1.register_remote_id('1020', resource=resource)

    assert resource.remote_ids.count() == 1


def test_nesting(init_resource_fk, init_user, django_assert_num_queries):
    user = init_user()

    res_parent = init_resource_fk(alias_postfix='parent', user=user)
    res_child_1 = init_resource_fk(alias_postfix='child1', user=user, outer_ref=res_parent.reference.alias)
    res_child_2 = init_resource_fk(alias_postfix='child2', user=user, outer_ref=res_parent.reference.alias)

    Nested(parent=res_parent.reference, child=res_child_1.reference, child_attr='fk_outer').save()
    Nested(parent=res_parent.reference, child=res_child_2.reference, child_attr='fk_outer').save()

    def make_parent():

        record_parent = res_parent.record_add(
            creator=user,
            attrs={'integer1': 10, 'fk1': None},
        )
        Record.publish(record_parent)

        record_parent.refresh_from_db()
        parent_update_dt = record_parent.dt_upd

        return record_parent, parent_update_dt

    record_parent, parent_update_dt = make_parent()
    record_parent_2, parent_2_update_dt = make_parent()

    record_child_1 = res_child_1.record_add(
        creator=user, attrs={'integer1': 30, 'fk_outer': record_parent.master_uid},
    )

    with django_assert_num_queries(6) as _:
        Record.publish(record_child_1)

    record_child_2 = res_child_2.record_add(
        creator=user, attrs={'integer1': 40, 'fk_outer': record_parent.master_uid},
    )
    Record.publish(record_child_2)

    # Проверяем, что обновилась дата обновления записи-родителя.
    record_parent.refresh_from_db()
    assert record_parent.dt_upd > parent_update_dt
    # Проверяем, что не обновилась другая запись того же справочника.
    record_parent_2.refresh_from_db()
    assert record_parent_2.dt_upd == parent_2_update_dt

    cache = RecordSerializationCache()

    # Проверяем вложенность при сериализации.
    with django_assert_num_queries(0) as _:
        serialized = record_child_1.serialize(depth_fk=0, depth_nested=0, cache=cache)

    assert 'foreign' not in serialized
    assert 'nested' not in serialized

    with django_assert_num_queries(2) as _:
        serialized = record_child_1.serialize(depth_fk=1, depth_nested=1, cache=cache)

    outer = serialized['foreign']['fk_outer']
    assert 'nested' not in serialized
    assert 'nested' not in outer

    with django_assert_num_queries(6) as _:
        serialized = record_parent.serialize(depth_fk=1, depth_nested=1, cache=cache)

    assert 'foreign' not in serialized
    nested = serialized['nested']
    nested_ref1 = nested[res_child_1.reference.alias]
    nested_ref2 = nested[res_child_2.reference.alias]
    assert len(nested_ref1) == 1
    assert len(nested_ref2) == 1
