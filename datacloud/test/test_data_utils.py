# -*- coding: utf-8 -*-
import numpy as np
import unittest
from datacloud.dev_utils.data import data_utils as du


class TestNumpyVersion(unittest.TestCase):
    def test_numpy_mask_behaviour(self):
        """
        In numpy 1.11.1
        import numpy as np
        mask = [True, True, True, True]
        a = np.array([1,2,3,4])
        a[mask]
        get: array([2, 2, 2, 2])

        # # In modern numpy
        import numpy as np
        mask = [True, True, True, True]
        a[mask]
        get: array([1, 2, 3, 4])
        """
        mask = [True, True, True, True]
        arr = np.array([1, 2, 3, 4])
        self.assertListEqual([1, 2, 3, 4], arr[mask].tolist())
        self.assertListEqual([1, 2, 3, 4], arr[np.array(mask).astype(bool)].tolist())


class TestDataUtils(unittest.TestCase):

    def setUp(self):
        print('Test: {}'.format(self._testMethodName))

    def test_empty_array_to_string(self):
        converted = du.array_tostring([])
        expected = b''
        self.assertEqual(expected, converted)

    def test_array_to_string(self):
        converted = du.array_tostring([1, 2, 3, 4, 5])
        expected = b'\x00\x00\x80?\x00\x00\x00@\x00\x00@@\x00\x00\x80@\x00\x00\xa0@'
        self.assertEqual(expected, converted)

    def test_np_array_to_string(self):
        converted = du.array_tostring(np.array([1, 2, 3, 4, 5]))
        expected = b'\x00\x00\x80?\x00\x00\x00@\x00\x00@@\x00\x00\x80@\x00\x00\xa0@'
        self.assertEqual(expected, converted)

    def test_double_array_to_string(self):
        converted = du.array_tostring(np.array([-1.5, 2.2, -3.1, 4.4, -5.1]))
        expected = b'\x00\x00\xc0\xbf\xcd\xcc\x0c@ffF\xc0\xcd\xcc\x8c@33\xa3\xc0'
        self.assertEqual(expected, converted)

    def test_array_from_string(self):
        converted = du.array_fromstring(b'\x00\x00\x80?\x00\x00\x00@\x00\x00@@\x00\x00\x80@\x00\x00\xa0@')
        expected = [1, 2, 3, 4, 5]
        self.assertTrue(np.array_equal(expected, converted))
        self.assertTrue(isinstance(converted, np.ndarray))

    def test_array_from_ampty_string(self):
        converted = du.array_fromstring('')
        expected = []
        self.assertTrue(np.array_equal(expected, converted))
        self.assertTrue(isinstance(converted, np.ndarray))

    def test_double_array_from_string(self):
        converted = du.array_fromstring(b'\x00\x00\xc0\xbf\xcd\xcc\x0c@ffF\xc0\xcd\xcc\x8c@33\xa3\xc0')
        expected = [-1.5, 2.2, -3.1, 4.4, -5.1]
        self.assertTrue(np.allclose(converted, expected))
        self.assertTrue(isinstance(converted, np.ndarray))

    def test_combine_features(self):
        combined = du.combine_features([[1, 2, 3], [6, 7, 8]])
        expected = [1, 2, 3, 6, 7, 8]
        self.assertTrue(np.array_equal(combined, expected))
        self.assertTrue(isinstance(combined, np.ndarray))

    def test_combine_with_empty(self):
        combined = du.combine_features([[], [], [1, 2, 3]])
        expected = [1, 2, 3]
        self.assertTrue(np.array_equal(combined, expected))
        self.assertTrue(isinstance(combined, np.ndarray))
