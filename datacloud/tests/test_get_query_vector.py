import unittest
from datacloud.features.dssm import dot_product
from datacloud.dev_utils.testing.testing_utils import RecordsGenerator


class TestGetQueryVector(unittest.TestCase):
    def test_get_vector(self):
        query2vectors = dot_product.get_query_vectors()
        self.assertEqual(len(query2vectors), 400)
        self.assertEqual(len(query2vectors[0]), 300)

    def test_get_vector_v2(self):
        query2vectors = dot_product.get_query_vectors(for_cpp=False)
        self.assertEqual(len(query2vectors), 300)
        self.assertEqual(len(query2vectors[0]), 400)
