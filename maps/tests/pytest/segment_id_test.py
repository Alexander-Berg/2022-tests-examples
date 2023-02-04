import unittest

from maps.analyzer.libs.data.lib.segment_id import SegmentId


class SegmentIdTest(unittest.TestCase):
    def test_segment(self):
        segment = SegmentId(1, 2)
        self.assertEqual(segment.edge_id, 1)
        self.assertEqual(segment.segment_index, 2)
        self.assertEqual(str(segment), "1 2")
        self.assertEqual(segment, SegmentId.from_string("1 2"))
        self.assertNotEqual(hash(SegmentId(1, 2)), hash(SegmentId(2, 1)))

if __name__ == "__main__":
    unittest.main()
