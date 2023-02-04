import pytest


def pytest_generate_tests(metafunc):
    if 'dbpath' in metafunc.fixturenames:
        metafunc.parametrize('dbpath', (':memory:', 'test.db'), ids=('memory', 'file'))


@pytest.fixture(scope='session')
def dbclass():
    from ya.skynet.services.copier.rbtorrent.db import Database
    return Database


@pytest.fixture()
def dbfile(dbclass, tmpdir):
    db = dbclass(path=tmpdir.join('test.db').strpath, lockTimeout=1)
    db.open()
    return db


@pytest.fixture(scope='function')
def dbmem(dbclass):
    db = dbclass(path=':memory:', lockTimeout=1)
    db.open()
    return db


def test_open(dbclass, dbpath, tmpdir):
    if dbpath != ':memory:':
        dbpath = tmpdir.join(dbpath)

    db = dbclass(path=str(dbpath), lockTimeout=1)
    db.open()
    db.set_debug()
    assert db.query_col('pragma user_version') == 0
    assert db.query_col('pragma main.journal_mode') == 'memory' if dbpath == ':memory:' else 'wal'
    assert db.query_col('pragma synchronous') == 0
    assert db.query_col('pragma foreign_keys') == 1
    assert db.query_col('pragma page_size') == 4096

    # :mem: db always has auto_vacuum == OFF
    assert db.query_col('pragma auto_vacuum') == 0 if dbpath == ':memory:' else 1


@pytest.mark.parametrize('retry', (False, True), ids=('', 'retry'))
def test_open_fail(dbclass, caplog, retry):
    db = dbclass(path='/fake')
    with pytest.raises(db.Error):
        db.open(force=retry)
    assert 'Failed to open database /fake: CantOpenError' in caplog.text()
    assert ('will cleanup and try again' in caplog.text()) == retry


@pytest.mark.parametrize('retry', (False, True), ids=('', 'retry'))
def test_open_force(dbclass, tmpdir, caplog, retry):
    dbfile = tmpdir.join('test.db')
    db = dbclass(path=dbfile.strpath)
    dbfile.write('some weird content')
    try:
        db.open(force=retry)
    except db.Error:
        assert not retry, 'open(force=True) didnt opened bad file'
    else:
        assert retry, 'open(force=False) opened bad file'

    assert 'Failed to open database' in caplog.text()
    assert ('will cleanup and try again' in caplog.text()) == retry


def test_close_fail(dbmem, caplog):
    dbmem._db = None
    assert not dbmem.close()
    rec = caplog.records()[-1]
    assert rec.levelname == 'WARNING'
    assert rec.message.startswith('Unable to close db properly:')


def test_dbobj_repr(dbclass):
    db = dbclass(':memory:')
    assert repr(db) == '<Database>'
    db.open()
    assert repr(db) == '<Database (opened)>'
    db.close()
    assert repr(db) == '<Database>'


def test_child_logging(dbclass, caplog):
    import logging
    logger = logging.getLogger('test')
    db = dbclass(':memory:', logger=logger)

    logger.debug('abcmarker1')
    logger.getChild('db').debug('abcmarker2')
    db._log.debug('abcmarker3')
    db._log_sql.debug('abcmarker4')

    records = caplog.records()
    assert len(records) == 4

    for _ in range(4):
        assert records[_].message == 'abcmarker%d' % (_ + 1)

    assert records[0].name == 'test'
    assert records[1].name == 'test.db'
    assert records[2].name == 'test.db'
    assert records[3].name == 'test.db.sql'


def test_warning_log(dbmem, caplog):
    dbmem.set_warning_threshold(0)
    dbmem.query('pragma user_version')

    rec = caplog.records()[-1]
    assert rec.message.endswith('pragma user_version  []')
    assert rec.levelname == 'WARNING'
    assert rec.name == 'db.sql'


@pytest.mark.parametrize('retry', (False, True), ids=('', 'force'))
def test_corruptdb(dbclass, tmpdir, retry):
    path = tmpdir.join('test.db')
    path.write('SQLite format 3\x00\x04\x00\x02\x02\x00@  \x00\x00', mode='wb')

    db = dbclass(path=path.strpath)
    try:
        db.open(force=retry)
    except db.Error:
        assert not retry, 'Failed to open corrupt db with force=True'
    else:
        assert retry, 'Opened corrupt db with force=False'

    # Close db if it was successfully opened
    db.close()
    path.remove()

    db.check = lambda quick=True: False
    try:
        db.open(force=retry)
    except db.Error as ex:
        if 'Check failed' not in str(ex):
            raise
        assert not retry, 'Failed to open corrupt db (quick check fail) with force=True'
    else:
        assert retry, 'Opened corrupt db (quick check fail) with force=False'


def test_closedb(dbfile, caplog):
    assert dbfile.close()
    rec = caplog.records()[-1]
    assert rec.message == 'Closed db'
    assert rec.levelname == 'INFO'

    assert not dbfile.close()  # close if not opened

    dbfile.open()


def test_migrate(dbmem, tmpdir):
    m1 = tmpdir.join('001_M')
    m1.ensure(file=1).write('CREATE TABLE test (a int)')
    m2 = tmpdir.join('002_M')
    m2.ensure(file=1).write('INSERT INTO test VALUES (1)')

    assert dbmem.migrate({1: m1.strpath, 2: m2.strpath}, {})
    assert dbmem.query_col('SELECT * FROM test')
    assert dbmem.query_col('pragma user_version') == 2


def test_migrate_back_fail(dbmem, caplog):
    dbmem.query('pragma user_version = 1')
    with pytest.raises(dbmem.Error):
        dbmem.migrate({}, {})
    assert 'Dont know how to degrade db version' in caplog.text()


def test_vacuum(dbmem):
    assert dbmem.vacuum()


def test_transactions(dbmem):
    dbmem.query('CREATE TABLE test (a INT)')
    dbmem.begin()
    dbmem.query('INSERT INTO test VALUES (1)')
    dbmem.rollback()
    assert dbmem.query_col('SELECT COUNT(*) FROM test') == 0

    dbmem.begin()
    dbmem.query('INSERT INTO test VALUES (2)')
    dbmem.commit()
    assert dbmem.query_col('SELECT COUNT(*) FROM test') == 1


def test_transactions_context_manager(dbmem):
    dbmem.query('CREATE TABLE test (a INT)')

    class MyEx(Exception):
        pass

    with pytest.raises(MyEx):
        with dbmem:
            dbmem.query('INSERT INTO test VALUES (1)')
            raise MyEx()

    assert dbmem.query_col('SELECT COUNT(*) FROM test') == 0

    with dbmem:
        dbmem.query('INSERT INTO test VALUES (2)')

    assert dbmem.query_col('SELECT COUNT(*) FROM test') == 1


def test_transactions_nested(dbmem):
    dbmem.query('CREATE TABLE test (a INT)')

    class MyEx(Exception):
        pass

    with dbmem:
        dbmem.query('INSERT INTO test VALUES (1)')
        with pytest.raises(MyEx):
            with dbmem:
                assert dbmem.query_col('SELECT COUNT(*) FROM test') == 1
                dbmem.query('INSERT INTO test VALUES (2)')
                raise MyEx()

    assert dbmem.query('SELECT * FROM test') == [(1, )]
    dbmem.query('DELETE FROM test')

    with pytest.raises(MyEx):
        with dbmem:
            dbmem.query('INSERT INTO test VALUES (1)')
            with dbmem:
                dbmem.query('INSERT INTO test VALUES (2)')
            raise MyEx

    assert dbmem.query_col('SELECT COUNT(*) FROM test') == 0

    with dbmem:
        dbmem.query('INSERT INTO test VALUES (1)')
        with dbmem:
            dbmem.query('INSERT INTO test VALUES (2)')

    assert dbmem.query_col('SELECT COUNT(*) FROM test') == 2


@pytest.mark.parametrize(
    ('sql', 'transactions'), (
        (False, False),
        (True, False),
        (False, True),
        (True, True)
    ), ids=(
        ('no debug'),
        ('sql but not transactions'),
        ('transactions but not sql'),
        ('full debug')
    )
)
def test_transactions_logging(dbmem, sql, transactions, caplog):
    dbmem.set_debug(sql=sql, transactions=transactions)
    dbmem.begin()
    dbmem.rollback()
    dbmem.begin()
    dbmem.commit()

    shouldBe = any((sql, transactions))
    assert ('BEGIN' in caplog.text()) is shouldBe
    assert ('ROLLBACK' in caplog.text()) is shouldBe
    assert ('COMMIT' in caplog.text()) is shouldBe

    if shouldBe:
        assert caplog.text().count('BEGIN') == 2
        assert caplog.text().count('ROLLBACK') == 1
        assert caplog.text().count('COMMIT') == 1


def test_create_function(dbmem):
    def fake(a, b):
        return a + '.fake.' + b

    dbmem.createFunction(fake, 'fake', 2)
    assert dbmem.query_col('SELECT fake("x", "z")') == 'x.fake.z'


def test_query_error(dbmem, caplog):
    with pytest.raises(dbmem.Error):
        dbmem.query('SELECT ABRACADABRA')
    assert caplog.records()[-1].levelname == 'WARNING'


def test_query_multiple(dbmem):
    assert dbmem.query('SELECT 1; SELECT 2') == [(1, ), (2, )]


def test_query_list(dbmem, caplog):
    dbmem.set_debug()
    assert dbmem.query('SELECT "X" WHERE 42 IN (??)', (range(100), ), paramsHint='ints') == [('X', )]
    rec = caplog.records()[-1]
    assert rec.message.endswith('SELECT "X" WHERE 42 IN (??)  [ints]')


def test_query_list_big(dbmem, caplog):
    lim = dbmem.LIMIT_VARIABLE_NUMBER
    assert lim == 999

    dbmem.set_debug()
    assert dbmem.query('SELECT "Y" WHERE 4442 IN (??)', (range(lim * 10), ), paramsHint='ints') == [('Y', )]

    rec = caplog.records()[-1]
    assert rec.message.endswith('SELECT "Y" WHERE 4442 IN (??)  [ints]')


def test_query_list_big_fail(dbmem):
    with pytest.raises(AssertionError):
        dbmem.query('SELECT ??, ??', (range(2048), range(2048)))


def test_query_as_dict(dbmem):
    assert dbmem.query('SELECT 1', asDict=True) == ({'1': 1}, )
    dbmem.query('CREATE TABLE test (a int, b text)')
    dbmem.executeMany('INSERT INTO test VALUES (?, ?)', [(1, 'a'), (2, 'b')], None)
    assert dbmem.query('SELECT a, b FROM test', asDict=True) == (
        {'a': 1, 'b': 'a'},
        {'a': 2, 'b': 'b'},
    )
    assert dbmem.query_one('SELECT a, b FROM test WHERE a == 2', asDict=True) == {
        'a': 2, 'b': 'b'
    }


def test_query_last_row_id(dbmem):
    dbmem.query('CREATE TABLE test (a int primary key, b int)')
    dbmem.query('INSERT INTO test (b) VALUES (1)', getLastId=True) == 1
    assert dbmem.query('INSERT INTO test (b) VALUES (2)', getLastId=True) == 2


def test_querycol_no_results(dbmem):
    assert dbmem.query_col('SELECT 1 WHERE 1 = 0') is None


def test_iquery(dbmem):
    gen = dbmem.iquery('SELECT 1')
    assert gen.next() == (1, )
    with pytest.raises(StopIteration):
        gen.next()


def test_iquery_dict(dbmem):
    gen = dbmem.iquery('SELECT 1 as key', asDict=True)
    assert gen.next() == {'key': 1}
    with pytest.raises(StopIteration):
        gen.next()


def test_query_cursor_close_if_error(dbmem, cov):
    dbmem.set_warning_threshold(set())
    with pytest.raises(TypeError):
        dbmem.query('SELECT 1')


def test_execute_many(dbmem, caplog):
    dbmem.set_debug()
    dbmem.query('CREATE TABLE test (a int)')
    dbmem.executeMany('INSERT INTO test VALUES (?)', [[1], [2], [3]], 'ints')

    assert caplog.records()[-1].message.endswith('INSERT INTO test VALUES (?)  [(ints,) x 3]')
    assert dbmem.query_col('SELECT COUNT(*) FROM test') == 3

    dbmem.executeMany('INSERT INTO test VALUES (?)', [[1], [2]], None)
    assert caplog.records()[-1].message.endswith('INSERT INTO test VALUES (?)  [<?> x 2]')
    assert dbmem.query_col('SELECT COUNT(*) FROM test') == 5

    assert dbmem.query('SELECT a FROM test') == [(x, ) for x in (1, 2, 3, 1, 2)]
