# encoding: utf-8
from dbmock import ObjectsDbMock
import nose
from nose.tools import eq_
import os

from shapely import wkb
from shapely.geometry import Point, Polygon

from ytools.config import Config
from yandex.maps.wikimap.generalization.gen_info import MAX_ZOOM
from yandex.maps.wikimap.generalization import IconGeneralizer
import yandex.maps.coverage5 as coveragelib
import yandex.maps.geolib3 as geolib
from yandex.maps.tileutils5 import scale2resolution

config = Config(config_filename=
        'tests/generalization/configs/generalizer-test.cfg')

class TestSingleObject:
    @classmethod
    def setup_class(self):
        self.db_mock = ObjectsDbMock()
        self.db_mock.add_object(1, category_id='other',
                                   state=1,
                                   center=wkb.dumps(Point(0, 0)),
                                   geometry=wkb.dumps(Point(0, 0)),
                                   geometry_type='POINT',
                                   area=0.0,
                                   revision_id=1,
                                   icon_zmin=MAX_ZOOM + 1)
        self.db_mock.add_object(2, category_id='cov-klejfewlfkj',
                                   state=0,
                                   center=wkb.dumps(Point(1, 0)),
                                   geometry=wkb.dumps(Point(1, 0)),
                                   geometry_type='POINT',
                                   area=0.0,
                                   revision_id=2,
                                   icon_zmin=MAX_ZOOM + 1)
        self.db_mock.add_object(3, category_id='normal',
                                   state=1,
                                   center=wkb.dumps(Point(1, 10001)),
                                   geometry=wkb.dumps(Polygon([(0, 10000),
                                                               (2, 10000),
                                                               (2, 10002)])),
                                   geometry_type='POLYGON',
                                   area=4.0,
                                   revision_id=3,
                                   icon_zmin=MAX_ZOOM + 1)
        self.db_mock.add_object(4, category_id='town-town',
                                   state=1,
                                   center=wkb.dumps(Point(1, 1)),
                                   geometry=wkb.dumps(Point(1, 1)),
                                   geometry_type='POINT',
                                   area=0.0,
                                   revision_id=4,
                                   icon_zmin=MAX_ZOOM + 1)
        self.db_mock.add_object(5, category_id='normal',
                                   state=3,
                                   center=wkb.dumps(Point(1, 10001)),
                                   geometry=wkb.dumps(Point(1, 10001)),
                                   geometry_type='POINT',
                                   area=0.0,
                                   revision_id=5,
                                   icon_zmin=MAX_ZOOM + 1)
        self.db_mock.add_object(6, category_id='nocov-artsarta',
                                   state=1,
                                   center=wkb.dumps(Point(0, 1)),
                                   geometry=wkb.dumps(Point(0, 1)),
                                   geometry_type='POINT',
                                   area=0.0,
                                   revision_id=6,
                                   icon_zmin=MAX_ZOOM + 1)
        self.generalizer = IconGeneralizer(config, self.db_mock)

        coverage_layer = config.get('core/coverage/layer', 'sat');
        coverage_path = config.get('core/coverage/path',
                                   '/usr/share/yandex/maps/coverage5/' +
                                        coverage_layer +
                                        '.mms.1')

        coverage = coveragelib.Coverage(coverage_path)[coverage_layer];
        geo_point = geolib.mercator_to_geo(geolib.Point2(1, 0))
        self.max_zoom = coverage.max_zoom(geo_point.lon,
                                         geo_point.lat)

    def tearDown(self):
        for i in xrange(1, 7):
            self.db_mock.set_icon_zmin(i, MAX_ZOOM + 1, i)

    def test_correct(self):
        self.generalizer.expand_cluster(1)
        objects_updated = self.generalizer.generalize()
        assert(len(objects_updated) == 0)
        eq_(self.db_mock.object_info(1)['icon_zmin'], MAX_ZOOM + 1)

    def test_nocov(self):
        self.generalizer.expand_cluster(6)
        objects_updated = self.generalizer.generalize()
        assert(len(objects_updated) == 1)
        eq_(self.db_mock.object_info(6)['icon_zmin'], MAX_ZOOM)

    def test_correct_cov(self):
        self.db_mock.set_icon_zmin(2, self.max_zoom, 2)
        self.generalizer.expand_cluster(2)
        objects_updated = self.generalizer.generalize()
        assert(len(objects_updated) == 0)
        eq_(self.db_mock.object_info(2)['icon_zmin'], self.max_zoom)

    def test_high(self):
        self.db_mock.set_icon_zmin(1, 2, 1)
        self.db_mock.set_icon_zmin(3, 3, 3)
        self.generalizer.expand_cluster(1)
        self.generalizer.expand_cluster(3)
        objects_updated = self.generalizer.generalize()
        assert(len(objects_updated) == 2)
        eq_(self.db_mock.object_info(1)['icon_zmin'], MAX_ZOOM + 1)
        assert(1 in objects_updated)
        eq_(self.db_mock.object_info(3)['icon_zmin'], 7)
        assert(3 in objects_updated)

    def test_high_cov(self):
        self.db_mock.set_icon_zmin(2, 0, 2)
        self.generalizer.expand_cluster(2)
        objects_updated = self.generalizer.generalize()
        assert(len(objects_updated) == 1)
        eq_(self.db_mock.object_info(2)['icon_zmin'], self.max_zoom)
        assert(2 in objects_updated)

    def test_low(self):
        self.generalizer.expand_cluster(3)
        objects_updated = self.generalizer.generalize()
        assert(len(objects_updated) == 1)
        eq_(self.db_mock.object_info(3)['icon_zmin'], 7)
        assert(3 in objects_updated)

    def test_low_cov(self):
        self.generalizer.expand_cluster(2)
        objects_updated = self.generalizer.generalize()
        assert(len(objects_updated) == 1)
        eq_(self.db_mock.object_info(2)['icon_zmin'], self.max_zoom)
        assert(2 in objects_updated)

    def test_rev_change(self):
        self.generalizer.expand_cluster(3)
        self.db_mock.set_revision(3, 30)
        objects_updated = self.generalizer.generalize()
        assert(len(objects_updated) == 1)
        eq_(self.db_mock.object_info(3)['icon_zmin'], MAX_ZOOM + 1)
        assert(3 in objects_updated)
        self.db_mock.set_revision(3, 3)

    def test_town_direct(self):
        self.generalizer.expand_cluster(4)
        objects_updated = self.generalizer.generalize()
        assert(len(objects_updated) == 0)
        eq_(self.db_mock.object_info(4)['icon_zmin'], MAX_ZOOM + 1)

    def test_deleted(self):
        self.db_mock.set_icon_zmin(3, 10, 3)
        self.generalizer.expand_cluster(5)
        objects_updated = self.generalizer.generalize()
        assert(len(objects_updated) == 1)
        eq_(self.db_mock.object_info(3)['icon_zmin'], 7)
        assert(3 in objects_updated)

    def test_commit(self):
        self.generalizer.expand_cluster(3)
        objects_updated = self.generalizer.generalize()
        eq_(self.db_mock.object_info(3)['icon_zmin'], 7)
        self.db_mock.rollback()
        eq_(self.db_mock.object_info(3)['icon_zmin'], 7)

    def test_rollback(self):
        self.generalizer.expand_cluster(3)
        objects_updated = self.generalizer.generalize_wo_commit()
        eq_(self.db_mock.object_info(3)['icon_zmin'], 7)
        self.db_mock.rollback()
        eq_(self.db_mock.object_info(3)['icon_zmin'], MAX_ZOOM + 1)

class TestManyObjects:
    @classmethod
    def setup_class(self):
        self.db_mock = ObjectsDbMock()
        self.db_mock.add_object(1, category_id='normal',
                                   state=1,
                                   center=wkb.dumps(Point(0, 0)),
                                   geometry=wkb.dumps(Point(0, 0)),
                                   geometry_type='POINT',
                                   area=0.0,
                                   revision_id=1,
                                   icon_zmin=MAX_ZOOM + 1)
        self.db_mock.add_object(2, category_id='nocoll',
                                   state=1,
                                   center=wkb.dumps(Point(1, 24470)),
                                   geometry=wkb.dumps(Point(1, 24470)),
                                   geometry_type='POINT',
                                   area=0.0,
                                   revision_id=2,
                                   icon_zmin=MAX_ZOOM + 1)
        self.db_mock.add_object(3, category_id='normal',
                                   state=0,
                                   center=wkb.dumps(Point(2, 2)),
                                   geometry=wkb.dumps(Polygon([(1, 1),
                                                               (3, 0),
                                                               (3, 3)])),
                                   geometry_type='POLYGON',
                                   area=4.0,
                                   is_building=True,
                                   revision_id=3,
                                   icon_zmin=MAX_ZOOM + 1)
        self.db_mock.add_object(4, category_id='nocoll',
                                   state=0,
                                   center=wkb.dumps(Point(0, 24470)),
                                   geometry=wkb.dumps(Point(0, 24470)),
                                   geometry_type='POINT',
                                   area=0.0,
                                   revision_id=4,
                                   icon_zmin=MAX_ZOOM + 1)
        self.db_mock.add_object(5, category_id='normal',
                                   state=1,
                                   center=wkb.dumps(Point(48930, 0)),
                                   geometry=wkb.dumps(Point(48930, 0)),
                                   geometry_type='POINT',
                                   area=0.0,
                                   revision_id=5,
                                   icon_zmin=MAX_ZOOM + 1)
        self.db_mock.add_object(6, category_id='normal',
                                   state=1,
                                   center=wkb.dumps(Point(2, 2)),
                                   geometry=wkb.dumps(Polygon([(0, 0),
                                                               (4, 0),
                                                               (4, 4)])),
                                   geometry_type='POLYGON',
                                   area=16.0,
                                   revision_id=6,
                                   icon_zmin=MAX_ZOOM + 1)
        self.db_mock.add_object(7, category_id='town-town',
                                   state=0,
                                   center=wkb.dumps(Point(48930, 1)),
                                   geometry=wkb.dumps(Point(48930, 1)),
                                   geometry_type='POINT',
                                   area=0.0,
                                   revision_id=7,
                                   icon_zmin=MAX_ZOOM + 1)
        self.db_mock.add_object(8, category_id='normal',
                                   state=2,
                                   center=wkb.dumps(Point(2, 2)),
                                   geometry=wkb.dumps(Polygon([(0, 0),
                                                               (4, 0),
                                                               (4, 4)])),
                                   geometry_type='POLYGON',
                                   area=16.0,
                                   revision_id=8,
                                   icon_zmin=MAX_ZOOM + 1)
        self.generalizer = IconGeneralizer(config, self.db_mock)

    def tearDown(self):
        for i in xrange(1, 8):
            self.db_mock.set_icon_zmin(i, MAX_ZOOM + 1, i)

    def test_all(self):
        for i in range(1, 8):
            self.generalizer.expand_cluster(i)
        objects_updated = self.generalizer.generalize()
        assert(len(objects_updated) == 5)
        eq_(self.db_mock.object_info(1)['icon_zmin'], 18)
        assert(1 in objects_updated)
        eq_(self.db_mock.object_info(2)['icon_zmin'], 7)
        assert(2 in objects_updated)
        eq_(self.db_mock.object_info(3)['icon_zmin'], 8)
        assert(3 in objects_updated)
        eq_(self.db_mock.object_info(4)['icon_zmin'], 7)
        assert(4 in objects_updated)
        eq_(self.db_mock.object_info(5)['icon_zmin'], 7)
        assert(5 in objects_updated)
        eq_(self.db_mock.object_info(6)['icon_zmin'], MAX_ZOOM + 1)
        eq_(self.db_mock.object_info(7)['icon_zmin'], MAX_ZOOM + 1)

    def test_cluster(self):
        for i in range(1, 8):
            self.db_mock.set_icon_zmin(i, 15, i)
        self.generalizer.expand_cluster(3)
        objects_updated = self.generalizer.generalize()
        assert(len(objects_updated) == 5)
        eq_(self.db_mock.object_info(1)['icon_zmin'], 18)
        assert(1 in objects_updated)
        eq_(self.db_mock.object_info(2)['icon_zmin'], 7)
        assert(2 in objects_updated)
        eq_(self.db_mock.object_info(3)['icon_zmin'], 8)
        assert(3 in objects_updated)
        eq_(self.db_mock.object_info(4)['icon_zmin'], 7)
        assert(4 in objects_updated)
        eq_(self.db_mock.object_info(5)['icon_zmin'], 15)
        eq_(self.db_mock.object_info(6)['icon_zmin'], MAX_ZOOM + 1)
        assert(6 in objects_updated)
        eq_(self.db_mock.object_info(7)['icon_zmin'], 15)

    def test_far(self):
        for i in range(1, 8):
            self.db_mock.set_icon_zmin(i, 10, i)
        self.generalizer.expand_cluster(5)
        objects_updated = self.generalizer.generalize()
        eq_(self.db_mock.object_info(1)['icon_zmin'], 10)
        assert(1 not in objects_updated)
        eq_(self.db_mock.object_info(3)['icon_zmin'], 10)
        assert(3 not in objects_updated)
        eq_(self.db_mock.object_info(5)['icon_zmin'], 7)
        assert(5 in objects_updated)
        eq_(self.db_mock.object_info(6)['icon_zmin'], 10)
        assert(6 not in objects_updated)
        eq_(self.db_mock.object_info(7)['icon_zmin'], 10)
        assert(7 not in objects_updated)
        assert(8 not in objects_updated)

class TestMaxObjects:
    @classmethod
    def setup_class(self):
        self.db_mock = ObjectsDbMock()
        x = 0.0
        for i in range(MAX_ZOOM):
            self.db_mock.add_object(i, category_id='highest',
                                       state=1,
                                       center=wkb.dumps(Point(x, 0)),
                                       geometry=wkb.dumps(Point(x, 0)),
                                       geometry_type='POINT',
                                       area=0.0,
                                       revision_id=i,
                                       icon_zmin=0)
            x += scale2resolution(i) + 1e-8
        self.generalizer = IconGeneralizer(config, self.db_mock)

    def test(self):
        self.generalizer.expand_cluster(MAX_ZOOM - 1)
        objects_updated = self.generalizer.generalize()
        print len(objects_updated)
        assert(len(objects_updated) == MAX_ZOOM - 1)
        for i in range(MAX_ZOOM):
            eq_(self.db_mock.object_info(i)['icon_zmin'], min(i, 20))
            assert(i in objects_updated or i == 0)
