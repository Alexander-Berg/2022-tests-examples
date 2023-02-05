#!/usr/bin/env python

from easyview import \
    mercator_to_pixel, pixel_to_mercator, \
    line_tiles, geodetic_to_pixel, \
    create_primitive, coord_pairs
from nose.tools import eq_, ok_, assert_raises
import sys, os

"""
    Tests are quite weak here. It is assumed one can use './easyview.py -f example'
    and make sure everything is working correctly by looking at web page.
    Though, addition of more clever tests is welcomed. :)
"""


def test_mercator_conversions():
    # no need to test conversions to/from geodetic
    # as they are just calls of geolib methods
    merc_x, merc_y = 1234567, 5500
    eq_(mercator_to_pixel(merc_x, merc_y), (1139898064, 1073447097))
    merc = pixel_to_mercator(1139898064, 1073447097)
    ok_(abs(merc[0] - merc_x) < 0.01)
    ok_(abs(merc[1] - merc_y) < 0.01)


def test_line_tiles():
    tiles = line_tiles(
        line_start=(1297955818, 673440595),
        line_end=(1297964170, 673434780),
        start_tile=(158441, 82207),
        end_tile=(158442, 82206),
        reverse_scale=5)
    eq_(frozenset(tiles), frozenset([(158441, 82207), (158442, 82207), (158442, 82206)]))


def test_coord_pairs():
    eq_(list(coord_pairs([37.55, 55.37, 37.01, 55.01], geodetic_to_pixel)),
        [(1297736298, 677264114), (1294515072, 681017762)])

    assert_raises(AssertionError, coord_pairs, [1, 2, 3], geodetic_to_pixel)


class Quieter(object):
    class DevNull(object):
        def write(self, data):
            pass

    def __enter__(self):
        self._old_stderr = sys.stderr
        sys.stderr = self.DevNull()

    def __exit__(self, type, value, traceback):
        sys.stderr = self._old_stderr


def test_create_primitive():
    style = {
        "point-radius": 4,
        "point-outline-color": "yellow",
        "point-fill-color": "red",
        "line-width": 2,
        "line-color": "blue",
    }
    point = create_primitive("37.55 55.37".split(), geodetic_to_pixel, style)
    eq_(len(point.coords), 1)
    eq_(point.label, "<no label>")
    style["point-radius"] = 6
    eq_(point.style["point-radius"], 4)

    labeled_point = create_primitive("37.37 55.11 usefultext".split(), geodetic_to_pixel, style)
    eq_(len(labeled_point.coords), 1)
    eq_(labeled_point.style["point-radius"], 6)
    eq_(labeled_point.label, "usefultext")

    polyline = create_primitive("38.01 57.77 37.11 55.22 37.77 54.54".split(), geodetic_to_pixel, style)
    eq_(len(polyline.coords), 3)
    eq_(polyline.label, "<no label>")
    style["line-width"] = 4
    eq_(polyline.style["line-width"], 2)

    labeled_polyline = create_primitive("38.01 57.77 37.11 55.22 Smile!".split(), geodetic_to_pixel, style)
    eq_(len(labeled_polyline.coords), 2)
    ok_(labeled_polyline.label, "Smile!")
    eq_(labeled_polyline.style["line-width"], 4)

    # suppress stderr output 'Failed to create primitive ...'
    with Quieter():
        ok_(create_primitive([], geodetic_to_pixel, style) is None)
        ok_(create_primitive("37.37 55.5 text1 text2".split(), geodetic_to_pixel, style) is None)
