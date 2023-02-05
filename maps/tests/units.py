import unittest
from tests.common import TransactionalTestCase, MockPumper
from ytools.xml import AssertXML, ET
from WikimapPy import units

class UnitsConvertionTest(unittest.TestCase):

    def testAreaUnits(self):
        for val, (unit, sval) in ((1, ('1', 'm2')),
                                  (1000,('1000', 'm2')),
                                  (10001, ('1001', 'hectare')),
                                  (pow(10, 6)+10., ('100001', 'km2'))):
            AssertXML(ET.tostring(unit.area_to_ET(val, 'area')))\
                .equal('/area/@unit', unit)\
                .equal('/area', sval)


    def testLengthUnits(self):
        for val, (unit, sval) in ((1, ('1', 'm')),
                                  (1000,('1,000', 'm')),
                                  (10001, ('10.001', 'km')),
                                  (pow(10, 6), ('1000000', 'km'))):
            AssertXML(ET.tostring(unit.length_to_ET(val, 'length')))\
                .equal('/length/@unit', unit)\
                .equal('/length', sval)
