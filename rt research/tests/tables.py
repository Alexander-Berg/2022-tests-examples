import pytest

from irt.multik.pylib.yt_orm import base, fields


class TestTable(base.Table):
    id = fields.IntegerField(key=True)
    non_id = fields.CharField()
    _ignore_rogue_fields = ['rogue']
    _table_prefix = "//home/"


class Label(base.Table):
    _table_prefix = "//home/"

    tag = fields.CharField(namespace=True, default='prod')
    user_id = fields.IntegerField(namespace=True)
    id = fields.IntegerField(key=True)

    name = fields.CharField()
    action = fields.CharField()
    parent_id = fields.IntegerField()
    timestamp = fields.IntegerField()


class Index(base.Table):
    _table_prefix = "//home/"

    tag = fields.CharField(namespace=True, default='prod')
    user_id = fields.IntegerField(namespace=True)
    bannerhash = fields.IntegerField(key=True)
    label_ids = fields.ListField(child_field=fields.CharField())
    action = fields.CharField()
    timestamp = fields.IntegerField()


class IndexInt(base.Table):
    _table_prefix = "//home/"

    tag = fields.CharField(namespace=True, default='prod')
    user_id = fields.IntegerField(namespace=True)
    bannerhash = fields.IntegerField(key=True)
    label_ids = fields.ListField(child_field=fields.IntegerField())
    action = fields.CharField()
    timestamp = fields.IntegerField()


class BannerData(base.Table):
    _table_prefix = "//home/"
    bannerhash = fields.IntegerField(key=True)
    title = fields.CharField()
    body = fields.CharField()
    href = fields.CharField()


with pytest.raises(ValueError):
    class SchemaErrorTestTable(base.Table):
        _table_prefix = "//home"
        user_id = fields.IntegerField(namespace=True)
        name = fields.CharField()
        id = fields.IntegerField(key=True)


class BannerBIDIndex(base.SecondaryIndexTable):
    _table_prefix = "//home/"
    _destination_table = BannerData
    bid = fields.IntegerField(key=True)


class TestIndex(base.SecondaryIndexTable):
    _destination_table = TestTable
    field_1 = fields.IntegerField(key=True)
    field_2 = fields.IntegerField(key=True)


with pytest.raises(RuntimeError):
    class BannerBIDIndex1(base.SecondaryIndexTable):
        _table_prefix = "//home/"
        _destination_table = BannerData
        bid = fields.IntegerField(key=True)


with pytest.raises(TypeError):
    class BannerIndex1(base.SecondaryIndexTable):
        _table_prefix = "//home/"
        cid = fields.IntegerField(key=True)


with pytest.raises(TypeError):
    class BannerIndex2(base.SecondaryIndexTable):
        _table_prefix = "//home/"
        _destination_table = dict
        cid = fields.IntegerField(key=True)


with pytest.raises(TypeError):
    class BannerIndex3(base.SecondaryIndexTable):
        _table_prefix = "//home/"
        _destination_table = BannerData
        cid = fields.IntegerField(key=True)
        bannerhash = fields.IntegerField(key=True)


with pytest.raises(TypeError):
    class BannerIndex4(base.SecondaryIndexTable):
        _destination_table = BannerData
        _table_prefix = "//home/"
        bannerhash = fields.IntegerField(key=True)


with pytest.raises(TypeError):
    class BannerIndex5(base.SecondaryIndexTable):
        _destination_table = BannerData
        _table_prefix = "//home/"
        cid = fields.IntegerField()


with pytest.raises(TypeError):
    class BannerIndex6(base.SecondaryIndexTable):
        _destination_table = BannerData
        _table_prefix = "//home/"
        cid = fields.IntegerField(key=True)
        _stub = fields.IntegerField()


with pytest.raises(TypeError):
    class BannerIndex7(base.SecondaryIndexTable):
        _destination_table = BannerData
        _table_prefix = "//home/"
        cid = fields.IntegerField(key=True)
        _pk = None
