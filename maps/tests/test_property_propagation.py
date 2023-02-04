import unittest
import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from maps.garden.libs_server.common.errors import PropagatePropertiesError

from maps.garden.libs_server.build.module_builds_manager import _check_build_properties_intersection
from maps.garden.libs_server.build.module_builds_manager import _propagate_source_to_properties


class AttrDict(dict):
    def __init__(self, *args, **kwargs):
        super(AttrDict, self).__init__(*args, **kwargs)
        self.__dict__ = self


def sources_with_same_properties():
    source1 = AttrDict(properties=AttrDict(shipping_date='1'), name='ymapsdf_src')
    source2 = AttrDict(
        properties=AttrDict(shipping_date='1'), name='backa_export')
    return [source1, source2]


def sources_with_different_properties():
    source1 = AttrDict(properties=AttrDict(shipping_date='1'), name='ymapsdf_src')
    source2 = AttrDict(
        properties=AttrDict(shipping_date='22'), name='backa_export')
    return [source1, source2]


class TestPropagateAllProperties(unittest.TestCase):
    def setUp(self):
        self.traits = AttrDict(
            build_properties_from_sources=None,
            build_properties_from_sources_mapping=None)

    def test_same_properties(self):
        sources = sources_with_same_properties()

        props = _propagate_source_to_properties(sources, self.traits)

        self.assertEqual(len(props), 1)
        self.assertEqual(props['shipping_date'], '1')

    def test_different_properties(self):
        sources = sources_with_different_properties()

        props = _propagate_source_to_properties(sources, self.traits)

        self.assertEqual(len(props), 0)


class TestPropagateSourceProperties(unittest.TestCase):
    def setUp(self):
        self.traits = AttrDict(
            build_properties_from_sources=['shipping_date'],
            build_properties_from_sources_mapping=None)

    def test_same_properties(self):
        sources = sources_with_same_properties()

        props = _propagate_source_to_properties(sources, self.traits)

        self.assertEqual(len(props), 1)
        self.assertEqual(props['shipping_date'], '1')

    def test_different_properties(self):
        sources = sources_with_different_properties()

        self.assertRaises(
            PropagatePropertiesError, _propagate_source_to_properties,
            sources, self.traits)


class TestPropagateSourceMappingProperties(unittest.TestCase):
    def setUp(self):
        self.traits = AttrDict(
            build_properties_from_sources=None,
            build_properties_from_sources_mapping={'shipping_date': 'ymapsdf_src'})

    def test_same_properties(self):
        sources = sources_with_different_properties()

        props = _propagate_source_to_properties(sources, self.traits)

        self.assertEqual(len(props), 1)
        self.assertEqual(props['shipping_date'], '1')

    def test_different_properties(self):
        sources = sources_with_different_properties()
        props = _propagate_source_to_properties(sources, self.traits)

        self.assertEqual(len(props), 1)
        self.assertEqual(props['shipping_date'], '1')

    def test_two_sources_same_properites(self):
        source1 = AttrDict(properties=AttrDict(shipping_date='1'), name='ymapsdf_src')
        source2 = AttrDict(properties=AttrDict(shipping_date='1'), name='ymapsdf_src')
        sources = [source1, source2]
        props = _propagate_source_to_properties(sources, self.traits)
        self.assertEqual(len(props), 1)
        self.assertEqual(props['shipping_date'], '1')

    def test_two_sources_different_properites(self):
        source1 = AttrDict(properties=AttrDict(shipping_date='1'), name='ymapsdf_src')
        source2 = AttrDict(properties=AttrDict(shipping_date='2'), name='ymapsdf_src')
        sources = [source1, source2]
        self.assertRaises(
            PropagatePropertiesError, _propagate_source_to_properties,
            sources, self.traits)

    def test_non_existing_property(self):
        source1 = AttrDict(properties=AttrDict(non_existing='1'), name='ymapsdf_src')
        sources = [source1]
        self.assertRaises(
            PropagatePropertiesError, _propagate_source_to_properties,
            sources, self.traits)

    def test_unhashable_property_value(self):
        sources = [AttrDict(properties=AttrDict(shipping_date=['1']), name='ymapsdf_src')]
        props = _propagate_source_to_properties(sources, self.traits)
        self.assertEqual(len(props), 1)
        self.assertEqual(props['shipping_date'], ['1'])


class TestPropagateProperties(unittest.TestCase):
    def test_intersection(self):
        self.assertRaises(
            PropagatePropertiesError,
            _check_build_properties_intersection,
            ['shipping_date'],
            {'shipping_date': 'ymapsdf_src'},
        )


if __name__ == '__main__':
    unittest.main()
