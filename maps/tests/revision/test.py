import logging
import psycopg2
from itertools import chain
import pytest
import six

from shapely.geometry import Polygon, LineString

import yatest.common

from yandex.maps.pgpool3 import PgPool, PoolConstants
from maps.pylibs.local_postgres import postgres_instance

from maps.wikimap.mapspro.libs.python import revision as rev
from maps.wikimap.mapspro.libs.python.revision import filters as rf

UID = 1

REVISION_SCHEMA_PATH = 'maps/wikimap/mapspro/libs/revision/sql/postgres_upgrade.sql'
REVISION_DB_SCHEMA = "revision"


def init_db(conn_params):
    conn = psycopg2.connect(conn_params.connection_string)
    cursor = conn.cursor()
    cursor.execute('CREATE EXTENSION postgis')
    cursor.execute('CREATE EXTENSION hstore')
    conn.commit()


@pytest.fixture(scope="module")
def conn_params():
    with postgres_instance() as params:
        init_db(params)
        yield params


@pytest.fixture
def clean_schema(conn_params):
    conn = psycopg2.connect(conn_params.connection_string)
    cursor = conn.cursor()
    cursor.execute("DROP SCHEMA IF EXISTS %s CASCADE" % REVISION_DB_SCHEMA)
    sql = yatest.common.source_path(REVISION_SCHEMA_PATH)
    print(sql)
    cursor.execute(open(sql).read())
    conn.commit()


@pytest.fixture
def pool(conn_params):
    poolConstants = PoolConstants(2, 2, 1, 1)
    conn_instance = ('localhost', conn_params.port)
    logger = logging.getLogger('pgpool3')
    logger.setLevel(logging.ERROR)

    return PgPool(
        conn_instance,
        [conn_instance],
        conn_params.connection_string,
        poolConstants,
        logger
    )


def ok_(expression):
    assert expression


def eq_(a, b):
    assert a == b


def create_commit(rgw, objects, commit_attrs=None):
    commit_data = [rev.ObjectData(rgw.acquire_object_id(), attrs=attrs, geom=geom)
                    for attrs, geom in objects]
    if commit_attrs is None:
        commit_attrs = rev.Attributes(dict(a="a"))
    commit = rgw.create_commit(commit_data, UID, commit_attrs)
    return commit, [rev.RevisionId(data.id.object_id, commit.id) for data in commit_data]


@pytest.mark.usefixtures('clean_schema')
def test_create_commit(pool):
    rgw = rev.RevisionsGateway(pool, rev.TRUNK_BRANCH_ID)
    geom_wkb = Polygon(([(0, 0), (10, 0), (10, 10), (0, 10)])).wkb
    if six.PY3:
        geom_wkb = geom_wkb.decode()
    oid = rgw.acquire_object_id()
    object_data = [rev.ObjectData(oid, attrs=dict(a="a"), geom=geom_wkb)]
    commit = rgw.create_commit(object_data, UID, rev.Attributes({'a': 'a'}))
    ok_(commit.id)
    eq_(commit.state, rev.CommitState.DRAFT)
    eq_(commit.created_by, UID)
    ok_(commit.created_at)
    eq_(commit.in_trunk, True)
    eq_(commit.in_stable, False)
    rgw.commit_txn()

    commit_revs = rgw.commit_revisions(commit.id)
    eq_(len(commit_revs), 1)

    rgw = rev.RevisionsGateway(pool, rev.TRUNK_BRANCH_ID)
    oid = rev.RevisionId(oid.object_id, commit.id)
    orev = rgw.load_revision(oid)
    eq_(orev.prev_id.object_id, 0)
    eq_(orev.next_trunk_id.object_id, 0)
    eq_(orev.deleted, False)
    eq_(orev.attrs["a"], "a")
    eq_(orev.geom, geom_wkb)
    eq_(orev.relation_data, None)

    rgw.create_commit(
        [rev.ObjectData(oid, geom='')],
        UID,
        rev.Attributes({'a': 'a'}))
    rgw.commit_txn()


@pytest.mark.usefixtures("clean_schema")
def test_filters(pool):
    rgw = rev.RevisionsGateway(pool, rev.TRUNK_BRANCH_ID)

    objects = [({"cat:rd": "1"}, None)]
    commit1, saved_oids1 = create_commit(rgw, objects)
    loaded_oids = rgw.revision_ids(rf.Attr("cat:rd").defined(), commit1.id)
    eq_(set(loaded_oids), set(saved_oids1))

    loaded_oids = rgw.revision_ids(rf.Geom.defined(), commit1.id)
    eq_(set(loaded_oids), set())

    objects = [({"cat:rd_el": "1"}, LineString([(0, 0), (10, 10)]).wkb),
               ({"cat:rd_el": "1"}, LineString([(20, 20), (40, 40)]).wkb)]

    commit2, saved_oids2 = create_commit(rgw, objects)

    loaded_oids = rgw.revision_ids(rf.Attr("cat:rd").defined(), commit2.id)
    eq_(set(loaded_oids), set(saved_oids1))

    loaded_oids = rgw.revision_ids(rf.Attr("cat:rd_el").defined(), commit2.id)
    eq_(set(loaded_oids), set(saved_oids2))

    loaded_oids = rgw.revision_ids(rf.Attr("cat:rd_el") == "1", commit2.id)
    eq_(set(loaded_oids), set(saved_oids2))

    loaded_oids = rgw.revision_ids(rf.Geom.defined(), commit2.id)
    eq_(set(loaded_oids), set(saved_oids2))

    loaded_oids = rgw.revision_ids(rf.Geom.intersects(0, 0, 100, 100), commit2.id)
    eq_(set(loaded_oids), set(saved_oids2))

    loaded_oids = rgw.revision_ids(rf.Geom.intersects(0, 0, 10, 10), commit2.id)
    eq_(set(loaded_oids), set([saved_oids2[0]]))

    loaded_oids = rgw.revision_ids(rf.or_(rf.Attr("cat:rd_el").defined(),
                                          rf.Attr("cat:rd").defined()), commit2.id)
    eq_(set(loaded_oids), set(chain(saved_oids2, saved_oids1)))

    loaded_oids = rgw.revision_ids(rf.or_(rf.Attr("cat:rd").defined(),
                                          rf.Geom.defined()), commit2.id)
    eq_(set(loaded_oids), set(chain(saved_oids2, saved_oids1)))

    loaded_oids = rgw.revision_ids(rf.and_(rf.ObjRevAttr.is_deleted(),
                                           rf.Attr("cat:rd").defined()), commit2.id)
    ok_(not loaded_oids)

    loaded_oids = rgw.revision_ids(rf.or_(
        rf.ObjRevAttr.object_id().in_([oid.object_id for oid in saved_oids2]),
        rf.ObjRevAttr.commit_id() == commit1.id), commit2.id)
    eq_(set(loaded_oids), set(chain(saved_oids2, saved_oids1)))
