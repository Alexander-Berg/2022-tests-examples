import unittest
from datacloud.dev_utils.json import json_utils


class TestJsonUtils(unittest.TestCase):
    def test_json_utils(self):
        self.assertEqual(json_utils.json_loads_byteified('{"Hello": "World"}'), {'Hello': 'World'})
        self.assertEqual(json_utils.json_loads_byteified('"I am a top-level string"'), 'I am a top-level string')
        self.assertEqual(json_utils.json_loads_byteified('7'), 7)
        self.assertEqual(json_utils.json_loads_byteified('["I am inside a list"]'), ['I am inside a list'])
        self.assertEqual(
            json_utils.json_loads_byteified('[[[[[[[["I am inside a big nest of lists"]]]]]]]]'),
            [[[[[[[['I am inside a big nest of lists']]]]]]]])
        self.assertEqual(
            json_utils.json_loads_byteified('{"foo": "bar", "things": [7, {"qux": "baz", "moo": {"cow": ["milk"]}}]}'),
            {'things': [7, {'qux': 'baz', 'moo': {'cow': ['milk']}}], 'foo': 'bar'})

    def test_byteify(self):
        self.assertEqual(json_utils._byteify(u'hello world \xf1'), 'hello world \xc3\xb1')
        self.assertEqual(json_utils._byteify('hello world'), 'hello world')
        self.assertEqual(json_utils._byteify([[u'string \xf1']]), [['string \xc3\xb1']])
        self.assertEqual(json_utils._byteify({u'sample \xf1': u'test \xf1'}), {'sample \xc3\xb1': 'test \xc3\xb1'})
        self.assertEqual(json_utils._byteify({u'sample \xf1': u'test \xf1'}, ignore_dicts=True), {u'sample \xf1': u'test \xf1'})
        self.assertEqual(json_utils._byteify(set([1, 2, 3])), set([1, 2, 3]))
