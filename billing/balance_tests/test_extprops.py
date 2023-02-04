# -*- coding:utf-8 -*-
import pytest
import datetime
import sqlalchemy as sa
import json
from decimal import Decimal

from balance.mapper.mapper_base import DeclarativeObject, ExtPropertyMapper, ExtPropertyHistoryMapper
from balance.mapper.extprops import *
from balance.scheme_meta import BalanceJSONEncoder
from billing.contract_iface.contract_json import JSONExtprop

meta = sa.MetaData()
client_table = sa.Table('t_test_extprops', meta,
                        sa.Column('id', sa.Integer, sa.Sequence('s_extprops_test_id', metadata=meta), primary_key=True),
                        sa.Column('info', sa.String(128)),
                        )


class Client(DeclarativeObject):
    """Test case mapper.
    Из-за наличия партиций на таблице тестовый маппер назван как один из оригинальных
    """
    __table__ = client_table

    attrstr = Str(default='')
    attrnum = Number(default=0, need_history=True)
    attrdate = DateTime(default=datetime.date(2018, 04, 12))
    attrlist = List(Str)
    attrdict = Dict(Str)
    attrdictint = Dict(Int)
    attrset = Set(Str)
    attrdictdt = Dict(DateTime)
    attrbool = Bool()
    attrjson = Json({'12312': 423, 'vc': ['43', None, 3]})
    attrpickle = Pickle()
    attrint = Int()
    attrsetint = Set(Int)
    attrsetdate = Set(DateTime)


def _create_empty_clients(session, qty):
    clients = [Client(info='text %i' % client_id) for client_id in range(qty)]
    session.add_all(clients)
    session.flush()
    return clients


@pytest.fixture()
def client(session):
    return _create_empty_clients(session, 1)[0]


class TestExtprops(object):

    # не создает таблицу при запуске тестов в teamcity
    # @pytest.fixture(scope='session', autouse=True)
    # def create_table(self, app, request):
    #     engine = app.dbhelper.engines[0]
    #     meta.create_all(engine)
    #
    #     def tear_down():
    #         meta.drop_all(engine)
    #
    #     request.addfinalizer(tear_down)

    def test_create_extprops(self, session, client):
        text = 'test classname'
        client.attrstr = text
        session.flush()
        attr = session.query(ExtPropertyMapper).getone(classname=client.__class__.__name__,
                                                       attrname='attrstr',
                                                       object_id=client.id)
        assert client.attrstr == attr.value_str
        assert attr.classname == Client.__name__

    def test_extprops_history(self, session, client):
        assert client.attrnum == 0
        client.attrnum = 5
        session.flush()
        assert client.attrnum == 5
        client.attrnum = 10
        session.flush()
        assert client.attrnum == 10
        history_attr = session.query(ExtPropertyHistoryMapper).getone(classname=client.__class__.__name__,
                                                                      attrname='attrnum',
                                                                      object_id=client.id)
        assert history_attr.value_num == 5
        assert history_attr.classname == Client.__name__

    @pytest.mark.parametrize('attr_name, value',
                             [
                                 ('info', 'text 0'),
                                 ('attrstr', ''),
                                 ('attrnum', 0),
                                 ('attrdate', datetime.date(2018, 04, 12)),
                                 ('attrlist', []),
                                 ('attrdict', {}),
                                 ('attrset', set()),
                                 ('attrdictdt', {}),
                                 ('attrbool', None),
                                 ('attrjson', {'12312': 423, 'vc': ['43', None, 3]}),
                                 ('attrpickle', None),
                                 ('attrint', None),
                                 ('attrsetint', set()),
                             ],
                             )
    def test_client_default_attributes(self, attr_name, value, client):
        assert getattr(client, attr_name) == value

    @pytest.mark.parametrize('attrname, value, extprop_attrname',
                             [
                                 ('attrstr', 'super text', 'value_str'),
                                 ('attrnum', Decimal('5.67'), 'value_num'),
                                 ('attrdate', datetime.datetime.now(), 'value_dt'),
                                 ('attrbool', True, 'value_num'),
                                 ('attrint', 5, 'value_num'),
                             ],
                             )
    def test_extprops_attrs(self, session, attrname, value, extprop_attrname, client):
        setattr(client, attrname, value)
        session.flush()
        attr = session.query(ExtPropertyMapper).getone(classname=client.__class__.__name__,
                                                       attrname=attrname,
                                                       object_id=client.id)
        assert getattr(client, attrname) == getattr(attr, extprop_attrname)

    @pytest.mark.parametrize('attrname, value, error',
                             [
                                 ('attrlist', 56, TypeError),
                                 ('attrset', 56, TypeError),
                                 ('attrint', 'abc', ValueError),
                                 ('attrsetint', set(['a', 'b']), ValueError),
                             ],
                             )
    def test_extprops_errors(self, session, attrname, value, error, client):
        with pytest.raises(error):
            setattr(client, attrname, value)
            session.flush()

    @pytest.mark.parametrize('attrname, value',
                             [
                                 ('attrlist', [1, 2, 3, 'a', 'b', 'c']),
                                 ('attrset', set(['a', 'b', 'c'])),
                                 ('attrsetint', set([1, 2, 3])),
                             ]
                             )
    def test_extprops_sequences(self, session, attrname, value, client):
        setattr(client, attrname, value)
        session.flush()
        attrs = session.query(ExtPropertyMapper).filter_by(classname=client.__class__.__name__,
                                                           attrname=attrname,
                                                           object_id=client.id).all()
        assert sorted(list(value)) == sorted([i.value_num or i.value_str for i in attrs])

    @pytest.mark.parametrize('attrname, value, func',
                             [
                                 ('attrdict', {'1': 'a', '2': 'b'}, lambda attrs: {i.key: i.value_str for i in attrs}),
                                 ('attrdictint', {'1': 5, '2': 6}, lambda attrs: {i.key: i.value_num for i in attrs}),
                                 ('attrdictdt', {'1': datetime.date(2018, 5, 4),
                                                 '2': datetime.date(2018, 5, 5)},
                                  lambda attrs: {i.key: i.value_dt for i in attrs}),
                             ]
                             )
    def test_extprops_dict(self, session, attrname, value, func, client):
        setattr(client, attrname, value)
        session.flush()
        attrs = session.query(ExtPropertyMapper).filter_by(classname=client.__class__.__name__,
                                                           attrname=attrname,
                                                           object_id=client.id).all()
        assert value == func(attrs)

    def test_extprops_json(self, session, client):
        value = {'12': 567, '34': ['ab', None, 5]}
        client.attrjson = value
        session.flush()
        attr = session.query(ExtPropertyMapper).getone(classname=client.__class__.__name__,
                                                       attrname='attrjson',
                                                       object_id=client.id)
        assert value == json.loads(attr.value_clob)

    @pytest.mark.parametrize('attrname, value', [
        ('attrstr', 'text'),
        ('attrnum', 10),
        ('attrdate', datetime.datetime.now()),
        ('attrlist', [1, 'text', None]),
        ('attrdict', {1: 'a', 'a': 1}),
        ('attrset', {1, 2, 3, 'a'}),
        ('attrdictdt', {'a': datetime.datetime.now(), 1: datetime.datetime.now()}),
        ('attrbool', True),
        ('attrjson', {'12312': 423, 'vc': ['43', None, 3]}),
        ('attrpickle', None),
        ('attrint', 12),
        ('attrsetint', {1, 2, 3, 4}),
        ('attrsetdate', {datetime.datetime.now()}),
    ])
    def test_serialization(self, session, client, attrname, value):
        setattr(client, attrname, value)
        session.flush()
        serialized = client.extprops_dict[attrname].serialize_to_primitive(client)
        assert JSONExtprop(attrname, **json.loads(json.dumps(serialized, cls=BalanceJSONEncoder))).get_value() == getattr(client, attrname)
