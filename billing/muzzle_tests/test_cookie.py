import unittest
import zlib
import base64

import balance.cookie2 as cookie2


class TestCookieEncodeDecode(unittest.TestCase):
    def setUp(self):
        super(TestCookieEncodeDecode, self).setUp()

        self.test_dict = {'key1': 'val1', 'key2': 'val2'}
        self.test_sign_key = 'aroh7Moosoo8PeixChoob8ie'

    def testEncodeDecodeCorrectUnsigned(self):
        unsigned_encoded_map = cookie2.encode_map(self.test_dict)

        self.assertEqual(unsigned_encoded_map, 'eJzLTq00YihLzDFiyE6tNASxDAE8QgXf')

        unsigned_map = cookie2.parse(unsigned_encoded_map)

        self.assertDictEqual(self.test_dict, unsigned_map)

    def testEncodeDecodeCorrectSigned(self):
        signed_encoded_map = cookie2.encode_map(self.test_dict, self.test_sign_key)

        self.assertEqual(
            signed_encoded_map,
            'eJwVxkESgCAIBVBP1ACKxXHwCxtbN^Ptq82bt2JLefyWsmLzPz5QMaNpI/fIptbFagfsUyDdyBjInKStnhwpYCayMUgvmuIvAPQXUw=='
        )

        signed_map = cookie2.parse(signed_encoded_map, self.test_sign_key)

        self.assertDictEqual(self.test_dict, signed_map)

    def testDecodeIncorrectSourceUnsigned(self):
        with self.assertRaises(TypeError):
            cookie2.parse('bad source')

    def testEncodeIncorrectSourceUnsigned(self):
        with self.assertRaises(AttributeError):
            cookie2.encode_map({'key1': 0})

        with self.assertRaises(AttributeError):
            cookie2.encode_map('bad source')

    def testEncodeDecodeEmptySourceUnsigned(self):
        unsigned_encoded_map = cookie2.encode_map({})

        self.assertEqual(unsigned_encoded_map, '')

        unsigned_map = cookie2.parse(unsigned_encoded_map)

        self.assertDictEqual({}, unsigned_map)

    def testDecodeIncorrectKeySigned(self):
        signed_encoded_map = cookie2.encode_map(self.test_dict, self.test_sign_key)
        signed_map = cookie2.parse(signed_encoded_map, 'bad key')

        self.assertDictEqual({}, signed_map)

    def testDecodeIncorrectHmacSigned(self):
        signed_encoded_map = cookie2.encode_map(self.test_dict, self.test_sign_key)

        # Parse encoded value
        test = signed_encoded_map.replace(' ', '+').replace('^', '+').strip()
        test = zlib.decompress(base64.b64decode(test))

        self.assertTrue('val2' in test)

        test = test.replace('val2', 'val3')

        # Encode hacked value
        test = base64.b64encode(zlib.compress(test)).replace('+', '^')

        signed_map = cookie2.parse(test, self.test_sign_key)

        self.assertDictEqual({}, signed_map)
