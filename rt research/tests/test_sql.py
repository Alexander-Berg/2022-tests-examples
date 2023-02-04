# coding=utf-8
import string

import pytest

from irt.multik.pylib.yt_orm import Q, BadLookup
from .tables import Label, Index, IndexInt, BannerData


def test_sql_all(fake_registry):
    expected = "* FROM [{}]".format(Label._table_path)
    assert Label.objects.all().as_sql() == expected
    assert Label.objects.filter().as_sql() == expected
    assert Label.objects.exclude().as_sql() == expected


def test_limit(fake_registry):
    expected = "* FROM [{}] LIMIT 10".format(Label._table_path)
    assert Label.objects.all().limit(10).as_sql() == expected


def test_sql_filter(fake_registry):
    sql = Label.objects.filter(id=5).as_sql()
    assert sql == "* FROM [{}] WHERE (id = 5)".format(Label._table_path)

    sql = Label.objects.filter(id__gte=5).as_sql()
    assert sql == "* FROM [{}] WHERE (id >= 5)".format(Label._table_path)

    sql = Label.objects.filter(id__lte=5, id__gte=5).as_sql()
    assert sql == "* FROM [{}] WHERE (id <= 5 AND id >= 5)".format(Label._table_path)

    sql = Label.objects.filter(Q(id__lte=5) | Q(id__gte=5)).as_sql()
    assert sql == "* FROM [{}] WHERE ((id <= 5 OR id >= 5))".format(Label._table_path)


def test_sql_in_lookup(fake_registry):
    sql = Label.objects.filter(id__in=[5, 10]).as_sql()
    assert sql == "* FROM [{}] WHERE (id IN (5, 10))".format(Label._table_path)


def test_list_contains(fake_registry):
    sql = Index.objects.filter(label_ids__list_contains='5').as_sql()
    assert sql == "* FROM [{}] WHERE (list_contains(label_ids, '5'))".format(Index._table_path)

    sql = IndexInt.objects.filter(label_ids__list_contains=5).as_sql()
    assert sql == "* FROM [{}] WHERE (list_contains(label_ids, 5))".format(IndexInt._table_path)


def test_sql_exclude(fake_registry):
    sql = Label.objects.exclude(id=5).as_sql()
    assert sql == "* FROM [{}] WHERE (NOT (id = 5))".format(Label._table_path)

    sql = Label.objects.exclude(id__gte=5).as_sql()
    assert sql == "* FROM [{}] WHERE (NOT (id >= 5))".format(Label._table_path)

    sql = Label.objects.exclude(id__lte=5, id__gte=5).as_sql()
    assert sql == "* FROM [{}] WHERE (NOT (id <= 5 AND id >= 5))".format(Label._table_path)

    sql = Label.objects.exclude(Q(id__lte=5) | Q(id__gte=5)).as_sql()
    assert sql == "* FROM [{}] WHERE (NOT ((id <= 5 OR id >= 5)))".format(Label._table_path)


def test_test_complex_filters(fake_registry):
    sql = Label.objects.filter(id__gte=0).exclude(Q(id__ne=2, id__lt=4) | Q(name='name')).as_sql()
    assert sql == "* FROM [{}] WHERE (id >= 0 AND NOT (((id != 2 AND id < 4) OR name = 'name')))".format(Label._table_path)


def test_join(fake_registry):
    query = Label.objects.filter(user_id=0) \
        .join(Index, (Label.tag, Label.user_id), (Index.tag, Index.user_id)) \
        .join(BannerData, Index.bannerhash, BannerData.bannerhash)
    expected = ("* FROM [{}] "
                "JOIN [{}] AS j0 ON (tag,user_id) = (j0.tag,j0.user_id) "
                "JOIN [{}] AS j1 ON (j0.bannerhash) = (j1.bannerhash) "
                "WHERE (user_id = 0)").format(Label._table_path, Index._table_path, BannerData._table_path)
    assert query.as_sql() == expected
    expected = ("* FROM [{}] "
                "JOIN [{}] AS j0 ON (tag,user_id) = (j0.tag,j0.user_id) "
                "JOIN [{}] AS j1 ON (j0.bannerhash) = (j1.bannerhash) "
                "WHERE (user_id = 0 AND name = 'foo')").format(Label._table_path, Index._table_path, BannerData._table_path)
    assert query.filter(name='foo').as_sql() == expected


def test_short_join(fake_registry):
    expected = ("* FROM [{}] "
                "JOIN [{}] AS j0 ON (bannerhash) = (j0.bannerhash) "
                "WHERE (user_id = 0)").format(Index._table_path, BannerData._table_path)

    query = Index.objects.filter(user_id=0) \
        .join(BannerData, BannerData.bannerhash)
    assert query.as_sql() == expected

    with pytest.raises(ValueError):
        query = Index.objects.filter(user_id=0) \
            .join(BannerData, BannerData.href)


def test_order_by(fake_registry):
    query = Index.objects.all().order_by('tag')
    assert query.as_sql() == "* FROM [{}] ORDER BY tag".format(Index._table_path)
    query = Index.objects.all().order_by('-tag')
    assert query.as_sql() == "* FROM [{}] ORDER BY tag DESC".format(Index._table_path)
    query = Index.objects.all().order_by('tag', '-user_id', 'bannerhash')
    assert query.as_sql() == "* FROM [{}] ORDER BY tag, user_id DESC, bannerhash".format(Index._table_path)


def test_tuples(fake_registry):
    query = Index.objects.filter(tag__user_id__gte=("prod", 1))
    assert query.as_sql() == "* FROM [{}] WHERE ((tag, user_id) >= ('prod', 1))".format(Index._table_path)

    query = Index.objects.filter(user_id__tag__lte=(1, "prod"))
    assert query.as_sql() == "* FROM [{}] WHERE ((user_id, tag) <= (1, 'prod'))".format(Index._table_path)

    query = Index.objects.filter(tag__user_id__in=[("prod", 1), ("stage", 1)])
    assert query.as_sql() == "* FROM [{}] WHERE ((tag, user_id) IN (('prod', 1), ('stage', 1)))".format(Index._table_path)


def test_bad_lookup(fake_registry):
    with pytest.raises(BadLookup):
        Label.objects.get(bad_field_name=1)
    with pytest.raises(BadLookup):
        Label.objects.get(id__id=1)
    with pytest.raises(BadLookup):
        Label.objects.get(id__id__gte=1)
    with pytest.raises(BadLookup):
        Label.objects.get(id__name__gte=1)
    with pytest.raises(BadLookup):
        Label.objects.get(id__name__list_contains=1)
    with pytest.raises(BadLookup):
        Label.objects.get(id__name__in=1)
    with pytest.raises(BadLookup):
        Label.objects.get(id__name__in=[(1, 2), 3])


@pytest.mark.parametrize("name,expected", [
    (string.punctuation, ''.join('\\' + c for c in string.punctuation)),
    ("name with spaces", None),
    (u"Юникодное имя", None),
    (b"ascii", "ascii"),
])
def test_string_escaping(fake_registry, name, expected):
    """Make sure we can use punctuation in queries"""
    assert Label.objects.filter(name=name).as_sql() == u"* FROM [{}] WHERE (name = '{}')".format(
        Label._table_path,
        expected if expected is not None else name,
    )


def test_only_fields(fake_registry):
    sql = Label.objects.filter(id=5).as_sql('id')
    assert sql == "id FROM [{}] WHERE (id = 5)".format(Label._table_path)
    sql = Label.objects.filter(id=5).as_sql('id', 'user_id', 'tag')
    assert sql == "id,user_id,tag FROM [{}] WHERE (id = 5)".format(Label._table_path)
    with pytest.raises(ValueError):
        list(Label.objects.filter(id=5).only('non_existant'))
    with pytest.raises(ValueError):
        list(Label.objects.filter(id=5).only())
