import unittest

from maps.analyzer.libs.data.lib.track_id import TrackId, Time


class TrackIdTest(unittest.TestCase):
    def test_all(self):
        track = TrackId.from_string("clid uuid 20101010T000000")
        self.assertEqual(track, TrackId("clid", "uuid", Time(2010, 10, 10)))

    def test_hashability(self):
        tracks = [TrackId.from_string("clid uuid 20101010T000000")
                  for _ in range(2)]
        tracks += [
            TrackId.from_string("clid2 uuid2 20101010T000000") for _ in range(2)
        ]
        tracks_set = set(tracks)
        self.assertEqual(len(tracks_set), 2)


if __name__ == "__main__":
    unittest.main()
