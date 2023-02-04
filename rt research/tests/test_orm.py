# coding=utf-8
import string
import time

import yt.wrapper as yt

import pytest

from irt.multik.pylib.yt_orm import ObjectDoesNotExist
from irt.multik.pylib.yt_orm.registry import registry
from .tables import TestTable, Label, Index, IndexInt, BannerData, BannerBIDIndex, TestIndex


def test_registry():
    """Check that all tables register themselves in registry"""
    assert registry.tables == {
        Label._table_name: Label,
        Index._table_name: Index,
        IndexInt._table_name: IndexInt,
        BannerData._table_name: BannerData,
        TestTable._table_name: TestTable,
        BannerBIDIndex._table_name: BannerBIDIndex,
        '{}-pk:field_1,field_2'.format(TestTable._table_name): TestIndex
    }


@pytest.mark.linux
def test_create_table(local_yt_registry):
    manager = TestTable.objects
    store = manager.store
    assert not store.exists_table()
    manager.create_table()
    assert store.exists_table()
    store.drop_store()
    assert not store.exists_table()


@pytest.fixture()
def yt_tables(local_yt_registry):
    Label.objects.create_table()
    Index.objects.create_table()
    IndexInt.objects.create_table()
    BannerData.objects.create_table()
    yield
    Label.objects.store.drop_store()
    Index.objects.store.drop_store()
    IndexInt.objects.store.drop_store()
    BannerData.objects.store.drop_store()


def test_object_initialization(fake_registry):
    ll = Label()
    assert ll.tag == 'prod'  # default
    assert ll.name is None
    ll = Label(tag='test', name='name')
    assert ll.tag == 'test'
    assert ll.name == 'name'
    with pytest.raises(ValueError):
        Label(tag='test', name='name', unknown_field='123')


def test_update(fake_registry):
    ll = Label()
    assert ll.update(tag='test') is ll
    assert ll.tag == 'test'
    assert ll.name is None
    with pytest.raises(ValueError):
        ll.update(unknown_field='123')
    with pytest.raises(ValueError):
        ll.update(tag='prod', name='first', unknown_field='123')
    assert ll.tag == 'test'
    assert ll.name is None
    ll.update(tag='prod', name='second')
    assert ll.tag == 'prod'
    assert ll.name == 'second'


@pytest.mark.linux
def test_crud(yt_tables):
    now = int(time.time())
    lbl = Label(tag='prod', timestamp=now, user_id=0, id=0, name='name')
    lbl.save()
    assert len(Label.objects.all().list()) == 1
    assert Label.objects.get(id=0).name == lbl.name
    lbl.name = 'another_name'
    lbl.save()
    assert Label.objects.get(id=0).name == lbl.name
    lbl.delete()
    assert len(Label.objects.all().list()) == 0


@pytest.mark.linux
def test_crud_transaction(yt_tables):
    now = int(time.time())
    lbl = Label(tag='prod', timestamp=now, user_id=0, id=0, name='name')
    with pytest.raises(RuntimeError):
        with Label.objects.transaction():
            lbl.save()
            raise RuntimeError()
    assert len(Label.objects.all().list()) == 0
    with Label.objects.transaction():
        lbl.save()
    assert len(Label.objects.all().list()) == 1
    assert Label.objects.get(id=0).name == lbl.name
    lbl.name = 'another_name'
    with pytest.raises(RuntimeError):
        with Label.objects.transaction():
            lbl.save()
            raise RuntimeError()
    assert Label.objects.get(id=0).name != lbl.name
    with Label.objects.transaction():
        lbl.save()
    assert Label.objects.get(id=0).name == lbl.name
    with pytest.raises(RuntimeError):
        with Label.objects.transaction():
            lbl.delete()
            raise RuntimeError()
    assert len(Label.objects.all().list()) == 1
    with Label.objects.transaction():
        lbl.delete()
    assert len(Label.objects.all().list()) == 0


@pytest.mark.linux
def test_multiple_joins(yt_tables):
    now = int(time.time())
    then = now - 1
    joined_name = 'joined'
    title = 'other_title'
    Label(timestamp=now, user_id=1, id=0, name='not-joined').save()
    Label(timestamp=now, user_id=0, id=0, name=joined_name).save()
    Index(timestamp=now, user_id=0, bannerhash=10).save()
    Index(timestamp=then, user_id=1, bannerhash=10).save()
    BannerData(bannerhash=10, title=title).save()
    BannerData(bannerhash=11, title=title + '_other').save()

    labels = list(
        Label.objects.filter(user_id=0)
        .join(Index, (Label.tag, Label.user_id), (Index.tag, Index.user_id))
        .join(BannerData, Index.bannerhash, BannerData.bannerhash).eval(allow_join_without_index=True)
    )
    assert len(labels) == 1
    lbl = labels[0]
    assert lbl.name == joined_name
    assert lbl._extra['Index'].timestamp == now
    assert lbl._extra['BannerData'].title == title


@pytest.mark.linux
@pytest.mark.parametrize("name", [
    string.punctuation,
    "name with spaces",
    "Юникодное имя",
])
def test_string_escaping(yt_tables, name):
    """Make sure we can use punctuation in queries"""
    Label(tag='prod', timestamp=0, user_id=0, id=0, name=name).save()
    label = Label.objects.get(name=name)
    assert label.name == name


@pytest.mark.linux
def test_object_does_not_exist(yt_tables):
    with pytest.raises(ObjectDoesNotExist):
        Label.objects.get(id=-1)
    with pytest.raises(ObjectDoesNotExist):
        Index.objects.get(user_id=-1)


@pytest.mark.linux
def test_filters(yt_tables):
    for i in range(10):
        Label(timestamp=0, user_id=0, id=i, name='name').save()

    ids = [l.id for l in Label.objects.filter(id__gte=5)]
    assert ids == [5, 6, 7, 8, 9]
    ids = [l.id for l in Label.objects.filter(id__gte=5).exclude(id__gte=8)]
    assert ids == [5, 6, 7]

    ids = [l.id for l in Label.objects.filter(id__ne=5).filter(id__lte=6)]
    assert ids == [0, 1, 2, 3, 4, 6]


@pytest.mark.linux
def test_bulk_save(yt_tables):
    n_labels = 10
    assert len(Label.objects.all().list()) == 0

    labels = [Label(timestamp=0, user_id=0, id=0, name='name-0') for _ in range(n_labels)]
    Label.objects.bulk_save(labels)
    assert len(Label.objects.all().list()) == 1

    labels = [Label(timestamp=0, user_id=0, id=i, name='name-{}'.format(i)) for i in range(n_labels)]
    Label.objects.bulk_save(labels)
    assert len(Label.objects.all().list()) == n_labels
    assert list(sorted([l.id for l in Label.objects.all()])) == list(range(n_labels))


@pytest.mark.linux
def test_bulk_save_type_error(yt_tables):
    with pytest.raises(TypeError):
        Index.objects.bulk_save([Label(timestamp=0, user_id=0, id=0, name='name')])


@pytest.mark.linux
def test_list_contains(yt_tables):
    Index.objects.bulk_save([
        Index(bannerhash=1, label_ids=["3", "2", "1"]),
        Index(bannerhash=2, label_ids=["3", "2", "2"]),
        Index(bannerhash=3, label_ids=["3"]),
        Index(bannerhash=4, label_ids=[]),
    ])
    assert 3 == len(Index.objects.filter(label_ids__list_contains="3").list())
    assert 2 == len(Index.objects.filter(label_ids__list_contains="2").list())
    assert 1 == len(Index.objects.filter(label_ids__list_contains="1").list())


@pytest.mark.linux
def test_list_contains_int(yt_tables):
    IndexInt.objects.bulk_save([
        IndexInt(bannerhash=1, label_ids=[3, 2, 1]),
        IndexInt(bannerhash=2, label_ids=[3, 2, 2]),
        IndexInt(bannerhash=3, label_ids=[3]),
        IndexInt(bannerhash=4, label_ids=[]),
    ])
    assert 3 == len(IndexInt.objects.filter(label_ids__list_contains=3).list())
    assert 2 == len(IndexInt.objects.filter(label_ids__list_contains=2).list())
    assert 1 == len(IndexInt.objects.filter(label_ids__list_contains=1).list())


@pytest.mark.linux
def test_tuple_filters(yt_tables):
    def _strip_idx(indices):
        return [(i.tag, i.user_id, i.bannerhash) for i in indices]

    Index.objects.bulk_save([
        Index(tag="1", user_id=1, bannerhash=1),
        Index(tag="1", user_id=1, bannerhash=2),
        Index(tag="1", user_id=2, bannerhash=1),
        Index(tag="1", user_id=2, bannerhash=2),

        Index(tag="2", user_id=1, bannerhash=1),
        Index(tag="2", user_id=1, bannerhash=2),
        Index(tag="2", user_id=2, bannerhash=1),
        Index(tag="2", user_id=2, bannerhash=2),

        Index(tag="3", user_id=1, bannerhash=1),
    ])
    assert 9 == len(Index.objects.all().list())

    objects = Index.objects.all().order_by('tag', 'user_id', 'bannerhash').limit(20)
    idx = objects.filter(tag__user_id__gt=(1, 1))
    assert _strip_idx(idx) == [
        ("1", 2, 1),
        ("1", 2, 2),
        ("2", 1, 1),
        ("2", 1, 2),
        ("2", 2, 1),
        ("2", 2, 2),
        ("3", 1, 1),
    ]

    idx = objects.filter(tag__user_id__gt=(1, 1)).limit(3)
    assert _strip_idx(idx) == [
        ("1", 2, 1),
        ("1", 2, 2),
        ("2", 1, 1),
    ]

    # exlude, i.e. negation also works
    idx = objects.exclude(tag__user_id__gt=(1, 1))
    assert _strip_idx(idx) == [
        ("1", 1, 1),
        ("1", 1, 2),
    ]
    idx = objects.filter(tag__user_id__in=[(1, 2)])
    assert _strip_idx(idx) == [
        ("1", 2, 1),
        ("1", 2, 2),
    ]
    idx = objects.filter(tag__user_id__gt=(1, 2)).filter(tag__user_id__lt=(2, 2))
    assert _strip_idx(idx) == [
        ("2", 1, 1),
        ("2", 1, 2),
    ]


@pytest.mark.linux
def test_only_fields(yt_tables):
    for i in range(10):
        Label(timestamp=0, user_id=0, id=i, name='name').save()

    objs = [l for l in Label.objects.filter(id__gte=5).only('id')]
    assert objs == [
        {"id": 5},
        {"id": 6},
        {"id": 7},
        {"id": 8},
        {"id": 9},
    ]
    objs = [l for l in Label.objects.filter(id__gte=5).exclude(id__gte=8).only('id', 'user_id')]
    assert objs == [
        {"id": 5, "user_id": 0},
        {"id": 6, "user_id": 0},
        {"id": 7, "user_id": 0},
    ]


def test_schema():
    assert Label._schema.schema == [
        {'name': 'tag', 'type': 'string', 'sort_order': 'ascending'},
        {'name': 'user_id', 'type': 'int64', 'sort_order': 'ascending'},
        {'name': 'id', 'type': 'int64', 'sort_order': 'ascending', 'required': True},

        {'name': 'name', 'type': 'string'},
        {'name': 'action', 'type': 'string'},
        {'name': 'parent_id', 'type': 'int64'},
        {'name': 'timestamp', 'type': 'int64'},
    ]

    assert Label._schema.simple == [
        {'name': 'tag', 'type': 'string'},
        {'name': 'user_id', 'type': 'int64'},
        {'name': 'id', 'type': 'int64'},

        {'name': 'name', 'type': 'string'},
        {'name': 'action', 'type': 'string'},
        {'name': 'parent_id', 'type': 'int64'},
        {'name': 'timestamp', 'type': 'int64'},
    ]

    yt_attributes = Label._schema.yt_attributes
    assert yt_attributes['dynamic'] is True
    assert isinstance(yt_attributes['schema'], yt.yson.YsonList)
    assert yt_attributes['schema'].attributes['unique_keys'] is True
    assert yt_attributes['schema'].attributes['strict'] is True


def test_secondary_index():
    assert list(registry.get_secondary_indexes(Label)) == []
    assert list(registry.get_secondary_indexes(BannerData)) == [BannerBIDIndex]
    assert registry.get_secondary_index(BannerData, ('bid', )) == BannerBIDIndex
    assert registry.get_secondary_index(BannerData, ('bannerhash',)) is None
    assert registry.get_secondary_index(BannerBIDIndex, ('bannerhash',)) is None

    assert list(BannerBIDIndex._fields.key) == ['bid', 'bannerhash']
    assert list(BannerBIDIndex._fields.namespace) == []
    assert list(BannerBIDIndex._fields.data) == ['_stub']
