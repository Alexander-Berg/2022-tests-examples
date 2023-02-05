#!/usr/bin/python
from __future__ import with_statement

from contextlib import closing
from lxml import objectify
from subprocess import check_call
from StringIO import StringIO
from tempfile import NamedTemporaryFile
import unittest
from yml import Yml
from yandex.maps.geolib3 import Point2


class TestYml(unittest.TestCase):
    NAMESPACES = {
        "gml":   "http://www.opengis.net/gml",
        "ymaps": "http://maps.yandex.ru/ymaps/1.x",
        "repr":  "http://maps.yandex.ru/representation/1.x",
    }
    GEO_OBJECT_COLLECTION_PREFIX = "/ymaps:ymaps/ymaps:GeoObjectCollection"
    GEO_OBJECT_PREFIX = GEO_OBJECT_COLLECTION_PREFIX + "/gml:featureMember/ymaps:GeoObject"
    REPR_PREFIX = "/ymaps:ymaps/repr:Representation"

    def validate(self, xml_string):
        with closing(NamedTemporaryFile(bufsize=0)) as test_xml:
            test_xml.write(xml_string)
            self.assertEqual(0,
                    check_call(["xmllint", "--noout",
                        "--schema", "/usr/share/yandex/maps/schemas/ymaps/1.x/ymaps.xsd",
                        "--schema", "/usr/share/yandex/maps/schemas/ymaps/1.x/gml.xsd",
                        "--schema", "/usr/share/yandex/maps/schemas/representation/1.x/representation.xsd",
                        test_xml.name]),
                    "invalid xml")

    def xpath(self, xpath, tree):
        return tree.xpath(xpath, namespaces=self.NAMESPACES)

    def xpath_len(self, xpath, tree):
        return len(self.xpath(xpath, tree))

    def xpath_text(self, xpath, tree):
        return self.xpath(xpath, tree)[0].text.strip()

    def test_empty(self):
        string_io = StringIO()
        with Yml(string_io, add_view_envelope=False):
            pass
        xml_string = string_io.getvalue()
        self.validate(xml_string)
        tree = objectify.fromstring(xml_string)
        self.assertEquals(1,
            self.xpath_len(self.GEO_OBJECT_COLLECTION_PREFIX + "/gml:featureMember", tree))
        self.assertEquals(1, self.xpath_len(self.REPR_PREFIX, tree))
        self.assertEquals(0, self.xpath_len(self.REPR_PREFIX + "/*", tree))


    def test_non_empty(self):
        for add_view_envelope in [True, False]:
            string_io = StringIO()
            with Yml(string_io, add_view_envelope=add_view_envelope) as yml:
                yml.add_style("style", "000000", 10)
                points = [Point2(0, 0), Point2(1, 1)]
                yml.open_geometry("style", "description").add_points(points)
            xml_string = string_io.getvalue()
            self.validate(xml_string)

            tree = objectify.fromstring(xml_string)
            self.assertEquals("0.00000000 0.00000000 1.00000000 1.00000000",
                self.xpath_text(self.GEO_OBJECT_PREFIX + "/gml:LineString/gml:posList", tree))
            self.assertEquals("#style",
                self.xpath_text(self.GEO_OBJECT_PREFIX + "/ymaps:style", tree))
            self.assertEquals("description",
                self.xpath_text(self.GEO_OBJECT_PREFIX + "/gml:description", tree))
            self.assertEquals("style",
                str(self.xpath(self.REPR_PREFIX + "/repr:Style/@gml:id", tree)[0]))
            self.assertEquals("000000",
                self.xpath_text(self.REPR_PREFIX + "/repr:Style/repr:lineStyle/repr:strokeColor", tree))
            self.assertEquals("10",
                self.xpath_text(self.REPR_PREFIX + "/repr:Style/repr:lineStyle/repr:strokeWidth", tree))
            if add_view_envelope:
                self.assertEquals("0.000000 0.000000",
                    self.xpath_text(self.REPR_PREFIX + "/repr:View/gml:boundedBy/gml:Envelope/gml:lowerCorner", tree))
                self.assertEquals("1.000000 1.000000",
                    self.xpath_text(self.REPR_PREFIX + "/repr:View/gml:boundedBy/gml:Envelope/gml:upperCorner", tree))

if __name__ == "__main__":
    unittest.main()

