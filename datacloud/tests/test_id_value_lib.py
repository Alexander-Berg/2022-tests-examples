import sys
import hashlib
import unittest
from datacloud.dev_utils.id_value import id_value_lib

VERSION = sys.version_info[0]


class TestIdValueLib(unittest.TestCase):
    def test_count_md5(self):
        self.assertEqual(
            id_value_lib.count_md5(id_value_lib.normalize_email('sample@yandex.ru')),
            '7416adeec1ace820b2bad2f846a3578e')
        self.assertEqual(
            id_value_lib.count_md5(id_value_lib.normalize_email('  sAmpLe@yandex.ru ')),
            '7416adeec1ace820b2bad2f846a3578e')
        self.assertEqual(
            id_value_lib.count_md5(id_value_lib.normalize_email('x-product@yandex-team.ru')),
            '7c8a1c75f84a335352c45bf1fbc1ab89')
        self.assertEqual(
            id_value_lib.count_md5(id_value_lib.normalize_phone('71234567890')),
            '5a893224859c93b3be4c1be4389dc58f')
        self.assertEqual(
            id_value_lib.count_md5(id_value_lib.normalize_phone('71234567891')),
            'b1bb37a4e1464a9f7535dad0dd456d34')
        self.assertEqual(
            id_value_lib.count_md5(id_value_lib.normalize_phone('+7(123)456-78-90')),
            '5a893224859c93b3be4c1be4389dc58f')
        self.assertEqual(
            id_value_lib.count_md5(id_value_lib.normalize_phone('8-1-2-3-4-5-67890 ')),
            '5a893224859c93b3be4c1be4389dc58f')

    def test_normalize_email(self):
        self.assertEqual(
            id_value_lib.normalize_email('sample@yandex.ru'), 'sample@yandex.ru')
        self.assertEqual(
            id_value_lib.normalize_email('  sAmpLe@yandex.ru '), 'sample@yandex.ru')
        self.assertEqual(
            id_value_lib.normalize_email('x-product@yandex-team.ru'), 'x-product@yandex-team.ru')

    def test_normalize_phone(self):
        self.assertEqual(id_value_lib.normalize_phone('71234567890'), '71234567890')
        self.assertEqual(id_value_lib.normalize_phone('71234567891'), '71234567891')
        self.assertEqual(id_value_lib.normalize_phone('+7(123)456-78-90'), '71234567890')
        self.assertEqual(id_value_lib.normalize_phone('8-1-2-3-4-5-67890 '), '71234567890')
        self.assertEqual(id_value_lib.normalize_phone('1-2-3-4-5-67890 \\n\\t\\r'), '71234567890')
        if VERSION == 2:
            self.assertEqual(id_value_lib.normalize_phone(u'+7(123)456-78-90'), u'71234567890')

    def test_encode_as_uint64(self):
        self.assertEqual(
            id_value_lib.encode_as_uint64(''), 7203772011789518145)
        self.assertEqual(
            id_value_lib.encode_as_uint64('na gorshke sidel korol'), 7676862522229347065)

    def test_encode_hexhash_as_uint64(self):
        self.assertEqual(
            id_value_lib.encode_hexhash_as_uint64(id_value_lib.count_md5('')), 7203772011789518145)
        self.assertEqual(
            id_value_lib.encode_hexhash_as_uint64(id_value_lib.count_md5('na gorshke sidel korol')), 7676862522229347065)

    def test_encode_hash_as_uint64(self):
        self.assertEqual(
            id_value_lib.encode_hash_as_uint64(hashlib.md5(''.encode()).digest()), 7203772011789518145)
        self.assertEqual(
            id_value_lib.encode_hash_as_uint64(hashlib.md5('na gorshke sidel korol'.encode()).digest()), 7676862522229347065)

    def test_normalize_crypta_phone(self):
        self.assertEqual(id_value_lib.normalize_crypta_phone('71234567890'),  '71234567890')
        self.assertEqual(id_value_lib.normalize_crypta_phone('+71234567890'),  '71234567890')
