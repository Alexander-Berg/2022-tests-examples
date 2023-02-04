from operator import itemgetter
from unittest.mock import patch

from django.test import TestCase

from photoalbum.files.models import Location, Tag, create_tags


class CreateTagTestCase(TestCase):
    def test_path_root(self):
        tags = create_tags("disk:/file.jpg", dict())
        self.assertEqual(tags, [])

    def test_path_short(self):
        tags = create_tags("disk:/на/для/от/и/file.jpg", dict())
        self.assertEqual(tags, [])

    def test_path_correct(self):
        cached_tags = dict()
        tag_ids = create_tags(
            "disk:/Конференция 100% nAtUral от 08/01_2017 до 15.03.17/file.jpg",
            cached_tags,
        )
        tags = list(Tag.objects.filter(id__in=tag_ids).values_list("name", flat=True))
        self.assertEqual(
            tags,
            ["date:08/01/2017", "date:15/03/2017", "конференция", "100", "natural"],
        )

    def test_path_short_and_correct(self):
        cached_tags = dict()
        tag_ids = create_tags(
            "disk:/лЕкцИя; 2010:01/09_. для яндекс_практикум, /file.jpg", cached_tags
        )
        tags = list(Tag.objects.filter(id__in=tag_ids).values_list("name", flat=True))
        self.assertEqual(tags, ["date:09/01/2010", "лекция", "яндекс", "практикум"])

    def test_two_different_paths(self):
        cached_tags = dict()
        for path, dirs in zip(
            ["disk:/dir1/dir2/w/file1.jpg", "disk:/dir3/q/dir4/file2.jpg"],
            [["dir1", "dir2"], ["dir3", "dir4"]],
        ):
            tags = create_tags(path, cached_tags)
            self.assertEqual(tuple(tags), itemgetter(*dirs)(cached_tags))
        self.assertEqual(list(cached_tags.keys()), ["dir1", "dir2", "dir3", "dir4"])

    def test_two_similar_paths(self):
        cached_tags = dict()
        dirs = ["dir1", "dir2", "dir3"]
        for filename in ["file1.jpg", "file2.jpg"]:
            tags = create_tags("disk:/dir1/q/dir2/w/dir3/" + filename, cached_tags)
            self.assertEqual(tuple(tags), itemgetter(*dirs)(cached_tags))
        self.assertEqual(list(cached_tags.keys()), dirs)

    def test_already_created_tags(self):
        cached_tags = dict()
        dirs = ["dir1", "dir2", "dir3"]
        for name in ["dir1", "dir2", "dir3"]:
            tag = Tag.objects.create(name=name)
            cached_tags[name] = tag.pk
        tags = create_tags("disk:/dir1/q/dir2/w/dir3/file.jpg", cached_tags)
        self.assertEqual(tuple(tags), itemgetter(*dirs)(cached_tags))


def mock_get_location(coords):
    return GetLocationTestCase.location_map.get(coords).split(", ", 2)


class GetLocationTestCase(TestCase):
    location_map = {
        "(59.958611, 30.405313)": "Россия, Санкт-Петербург, Пискарёвский проспект, 2к2Щ",
        "(55.733816, 37.588229)": "Россия, Москва, улица Льва Толстого, 16",
        "(55.751096, 37.534182)": "Россия, Москва, 1-й Красногвардейский проезд, 22с1",
    }

    @patch("photoalbum.files.tasks.get_location", side_effect=mock_get_location)
    def test_location_split(self, get_location):
        country, city, address = get_location("(59.958611, 30.405313)")
        self.assertEqual(country, "Россия")
        self.assertEqual(city, "Санкт-Петербург")
        self.assertEqual(address, "Пискарёвский проспект, 2к2Щ")

    @patch("photoalbum.files.tasks.get_location", side_effect=mock_get_location)
    def test_equal_locations(self, get_location):
        country1, city1, address1 = get_location("(55.733816, 37.588229)")
        country2, city2, address2 = get_location("(55.751096, 37.534182)")
        self.assertEqual(country1, country2)
        self.assertEqual(city1, city2)
        self.assertNotEqual(address1, address2)

    @patch("photoalbum.files.tasks.get_location", side_effect=mock_get_location)
    def test_different_locations(self, get_location):
        for geo_coords in (
            "(59.958611, 30.405313)",
            "(55.733816, 37.588229)",
            "(55.751096, 37.534182)",
        ):
            country, city, address = get_location(geo_coords)
            Location.objects.create(
                geo_coords=geo_coords,
                country=country,
                city=city,
                address=address,
            )

        self.assertEqual(Location.objects.filter(country="Россия").count(), 3)
        self.assertEqual(Location.objects.filter(city="Москва").count(), 2)
        self.assertEqual(Location.objects.filter(city="Санкт-Петербург").count(), 1)
