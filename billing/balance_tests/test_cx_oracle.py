# coding=utf-8

import random
import datetime
import itertools
from decimal import Decimal

import pytest
import cx_Oracle
import sqlalchemy as sa

from balance import mapper

from tests import object_builder as ob


def test_nulls_in_bind_array(app):
    """
    Check that cx_Oracle supports nulls in bind arrays.
    """
    query_template = """
    declare
      type array_t is table of {type} index by binary_integer;
      array array_t := :array_bind;
    begin
      for i in 1..array.count loop
        if array(i) is null 
        then
            dbms_output.put_line('null');
        else
            dbms_output.put_line(array(i));
        end if;
      end loop;
    end;
    """

    cursor = app.dbhelper.create_raw_connection(backend_id='balance').cursor()
    cursor.callproc('dbms_output.enable')

    all_expected_values = []

    for type_, binds_array, expected_array in (
            ('varchar2(20 char)', ['value', None], ['value', 'null']),
            ('varchar2(20 char)', [None, 'value'], ['null', 'value']),
            ('varchar2(20 char)', ['', None], ['null', 'null']),
            ('varchar2(20 char)', [None, ''], ['null', 'null']),
            ('varchar2(20 char)', [None, None], ['null', 'null']),

            ('number', [Decimal(123), None], ['123', 'null']),
            ('number', [None, 123], ['null', '123']),

            # cannot infer type of array
            # ('number', [None, None], ['null', 'null']),
    ):
        query = query_template.format(type=type_)
        cursor.execute(query, array_bind=binds_array)
        all_expected_values.extend(expected_array)

    status_bind = cursor.var(cx_Oracle.NUMBER)
    line = cursor.var(cx_Oracle.STRING)
    for expected in all_expected_values:
        cursor.callproc('dbms_output.get_line', (line, status_bind))
        status = status_bind.getvalue()
        # check that dbms.output works (not a test case check)
        assert status == 0, status
        actual = line.getvalue()
        assert actual == expected, (expected, actual)


def test_negative_zero_handling(session):
    """
    Check that inserting negative zero results in inserting plain zero.
    See https://github.com/oracle/python-cx_Oracle/issues/274
    """
    order = ob.OrderBuilder.construct(session)
    order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: Decimal('-0')})
    session.flush()
    session.refresh(order.shipment)


def test_passing_boolean_to_integer_column(session):
    """
    Oracle does not have boolean column type, so all bool columns are declared as
    sa.Integer in out code (which translates to oracle number).
    SQLAlchemy declares such columns as int to cx_Oracle cursor. So cx_Oracle does not
    expect a bool value.
    This test checks if cx_Oracle has a patch, which converts bool to number in that case.
    """
    clients = sa.Table('t_client', sa.MetaData(),
                       sa.Column('id', sa.Integer, primary_key=True),
                       sa.Column('is_agency', sa.Integer))
    # noinspection PyPropertyAccess
    session.execute(clients.update().where(clients.c.id == -1).values(is_agency=True))


def test_boolean_converted_to_number_in_pl_sql_block(session):
    """
    Upstream cx_Oracle does not convert bool to int if it is passed to pl/sql block.
    But pl/sql block does not necessary mean, that all bools must be passed as pl/sql booleans.
    This test checks if cx_Oracle has as a patch, which makes it always converts bool to int.
    """
    session.execute("""
    begin
        update bo.t_client 
        set is_agency = :is_agency
        where id = -1;
    end;
    """, {'is_agency': True})


def test_long_numbers(session):
    """
    odpi recently reduced max number digits:
    https://github.com/oracle/odpi/commit/11c13863a9f038038a06acbf26c9f85941402172
    but in fact OCI and db works fine up to 40 digits (previous constant value).
    So we patched it back to support long numbers, because we rely on them in several places.
    """
    for x in (
        int('9' * 40),
        Decimal('-0.' + '9' * 40),
        Decimal('-9.' + '9' * 38),
    ):
        assert session.execute('select :x from dual', {'x': x}).scalar() == x


COMMON_NUMBER_FUNCS = [
    # int (smaller than C native 32-bit int)
    lambda: random.randrange(2**30),
    lambda: True
]

if int(cx_Oracle.version.split('.')[0]) >= 7:
    # pylong (larger than C long)
    # cx_Oracle 5.2.1 does not handle it properly: writes to (or reads from) db incorrect numbers.
    COMMON_NUMBER_FUNCS.append(lambda: random.randrange(2**64, 2**65))

ALL_INT_FUNCS = COMMON_NUMBER_FUNCS + [
    lambda: Decimal(random.randrange(2**64, 2**65)),
    # This is works right now (but is not supposed to) for sa.Integer columns
    # because of implementation details. Do not rely on this behavior.
    # lambda: str(random.randrange(2**30)),
    # lambda: unicode(random.randrange(2**30)),
]

ALL_DECIMAL_FUNCS = COMMON_NUMBER_FUNCS + [
    lambda: Decimal('%d.666' % random.randrange(2**64, 2**65)),
    # This is not supposed to work
    # lambda: '%d.666' % random.randrange(2**30),
    # lambda: u'%d.666' % random.randrange(2**30),
]


FIELD_TO_NUMBER_FUNCS_MAP = [
    ('payment_type', ALL_INT_FUNCS),
    ('click_price', ALL_DECIMAL_FUNCS)
]


# We need separate cases for an sa.Integer column,
# because SQLAlchemy declares it's type to cx_Oracle,
# and cx_Oracle handles it differently.
@pytest.mark.parametrize(
    ['num_field', 'permutation'],
    [(num_field_, list(permutation_), )
     for num_field_, number_funcs in FIELD_TO_NUMBER_FUNCS_MAP
     for combination in itertools.combinations(number_funcs, 2)
     for permutation_ in (itertools.permutations(combination))]
)
def test_executemany_orm(session, num_field, permutation):
    """
    These functions (see test_executemany_plain_sql below) check, that cx_Oracle does not fail
    if you pass rows with different numeric types in one column to executemany.
    For example:
    [{'id': 1, value_num: 1},
     {'id': 2, value_num: True}]
    Test checks all possible pairs (and its permutations) of types.

    This function works on orm level.
    test_executemany_plain_sql works on plain sql level.
    """
    expected_map = {}
    number_funcs_map = {}
    objects = []
    ids = []
    for number_func in permutation:
        number = number_func()
        id_ = random.randrange(2**32, 2**65)
        # Test insert
        obj = mapper.Page(page_id=id_, nds=0, dt=datetime.datetime.now())
        setattr(obj, num_field, number)
        expected_map[id_] = number
        number_funcs_map[id_] = number_func
        ids.append(id_)
        objects.append(obj)

    session.add_all(objects)
    session.flush()
    for obj in objects:
        session.expire(obj)
    objects = session.query(mapper.Page).filter(mapper.Page.page_id.in_(ids)).all()

    for obj in objects:
        id_ = obj.page_id
        actual = getattr(obj, num_field)
        expected = expected_map[id_]
        assert actual == Decimal(expected), '%s != %r' % (actual, expected)

        # Now test update
        number = number_funcs_map[id_]()
        expected_map[id_] = number
        setattr(obj, num_field, number)

    session.flush()
    for obj in objects:
        session.expire(obj)

    objects = session.query(mapper.Page).filter(mapper.Page.page_id.in_(ids)).all()
    for obj in objects:
        id_ = obj.page_id
        actual = getattr(obj, num_field)
        expected = expected_map[id_]
        assert actual == Decimal(expected), '%s != %r' % (actual, expected)


# There is not need to test with integer column, because from plain sql point of view,
# it does not differ from Oracle numeric column.
@pytest.mark.parametrize(
    ['permutation'],
    [(list(permutation_), )
     for combination in itertools.combinations(ALL_DECIMAL_FUNCS, 2)
     for permutation_ in (itertools.permutations(combination))]
)
def test_executemany_plain_sql(session, permutation):
    """
    See test_executemany_orm's docstring.
    """
    num_field = 'click_price'
    expected_map = {}
    number_funcs_map = {}
    binds = []
    ids = []
    for number_func in permutation:
        number = number_func()
        id_ = random.randrange(2**32, 2**65)
        # Test insert
        obj = dict(page_id=id_, nds=0, dt=datetime.datetime.now())
        obj[num_field] = number
        expected_map[id_] = number
        number_funcs_map[id_] = number_func
        ids.append(id_)
        binds.append(obj)

    columns = binds[0].keys()
    insert_query = 'insert into {table} ({columns}) values ({values})'.format(
        table=mapper.Page.__table__.fullname,
        columns=', '.join(columns),
        values=', '.join(':%s' % col for col in columns)
    )

    session.execute(insert_query, binds)
    binds = []
    objects = session.query(mapper.Page).filter(mapper.Page.page_id.in_(ids)).all()

    for obj in objects:
        id_ = obj.page_id
        actual = getattr(obj, num_field)
        expected = expected_map[id_]
        assert actual == Decimal(expected), '%s != %r' % (actual, expected)

        # Now test update
        number = number_funcs_map[id_]()
        expected_map[id_] = number
        binds.append({'page_id': id_, num_field: number})

    update_query = 'update {table} set {num_field} = :{num_field} where page_id = :page_id'.format(
        table=mapper.Page.__table__.fullname,
        num_field=num_field
    )
    session.execute(update_query, binds)
    for obj in objects:
        session.expire(obj)

    objects = session.query(mapper.Page).filter(mapper.Page.page_id.in_(ids)).all()
    for obj in objects:
        id_ = obj.page_id
        actual = getattr(obj, num_field)
        expected = expected_map[id_]
        assert actual == Decimal(expected), '%s != %r' % (actual, expected)


def test_regression_native_int_bug(session):
    """ BALANCE-32333 """
    num = 51202658330991905595474923094537872723
    query = "SELECT CAST('{}' AS NUMBER(*, 0)) FROM dual".format(num)
    res = session.execute(query).scalar()
    assert res == num


def test_orm_read_clob_is_utf8(session):
    """ BALANCE-32380 """
    string = u'Вот это дела'
    client = ob.ClientBuilder.construct(session)
    client.enqueue('OEBS', force=True)
    client.exports['OEBS'].traceback = string
    session.flush()
    session.refresh(client.exports['OEBS'])
    res = client.exports['OEBS'].traceback
    assert isinstance(res, str)
    assert res == string.encode('utf8')
