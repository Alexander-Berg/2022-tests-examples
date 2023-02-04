# coding=utf-8
import os
import sys
import django
import unittest
from catalogue.settings import BASE_DIR

os.environ["DJANGO_SETTINGS_MODULE"] = "settings"
sys.path.append(BASE_DIR + "/catalogue")
django.setup()

from django_dbq.models import Job
from analytics.pdb.parts_loader import PartsLoader
from parts.models import PartInfo


class PartsDBTest(unittest.TestCase):
    parts_loader = PartsLoader(bulk_save=False)
    parts_loader_bulk = PartsLoader(bulk_save=True)

    def create_test_part(self, bulk_load=False, **kwargs):
        raw_title = kwargs.get("raw_title", "test")
        number = kwargs.get("number", "testnumber")
        brand_id = kwargs.get("brand_id", 3030)
        brand_model_id = kwargs.get("brand_model_id")
        category_id = kwargs.get("category_id", 6806)
        properties = kwargs.get("properties", [])
        images = kwargs.get(
            "images",
            [
                "http://avatars.mdst.yandex.net/get-autoparts-admin/3881/9c4587dce45beb0e4dabba4d812b91fd",
                "https://avatars.mds.yandex.net/get-autoparts-admin/1373104/1538581565633476.png",
            ],
        )
        compatibilities = kwargs.get("compatibilities", [])
        analogs = kwargs.get("analogs", [])
        source = kwargs.get("source", "test")
        if not bulk_load:
            self.parts_loader.save_one(
                raw_title,
                number,
                brand_id,
                brand_model_id,
                category_id,
                properties,
                images,
                compatibilities,
                analogs,
                source,
                do_save_image=kwargs.get("do_save_image", False),
            )
        else:
            self.parts_loader_bulk.save_one(
                raw_title,
                number,
                brand_id,
                brand_model_id,
                category_id,
                properties,
                images,
                compatibilities,
                analogs,
                source,
                do_save_image=kwargs.get("do_save_image", False),
            )
        return number, brand_id

    def test_do_not_save_image(self):
        number, brand_id = self.create_test_part()
        part = PartInfo.get_by_number_and_brand(number, brand_id)
        self.assertFalse(not part)
        self.assertEqual(2, len(part.images.all()))
        self.assertEqual(0, len(part.compatibilities.all()))
        part.hard_delete()
        part = PartInfo.get_by_number_and_brand(number, brand_id)
        self.assertTrue(not part)

    def test_do_image_save(self):
        Job.objects.all().delete()
        number, brand_id = self.create_test_part(
            do_save_image=True,
            images=["https://avatars.mds.yandex.net/get-autoparts-feed/1327601/vrl1teehi9t8eq.815043986"],
        )
        part = PartInfo.get_by_number_and_brand(number, brand_id)
        self.assertFalse(not part)
        self.assertEqual(1, len(part.images.all()))
        self.assertEqual(0, len(part.compatibilities.all()))
        part.hard_delete()
        part = PartInfo.get_by_number_and_brand(number, brand_id)
        self.assertTrue(not part)

    def test_do_not_save_image_update(self):
        self.create_test_part()
        number, brand_id = self.create_test_part(
            images=[
                "https://avatars.mds.yandex.net/get-autoparts-admin/1373104/8111ccd7315a06d8dfee0a09f08127b4",
                "https://avatars.mds.yandex.net/get-autoparts-admin/1373104/1538581565633476.png",
            ]
        )
        part = PartInfo.get_by_number_and_brand(number, brand_id)
        self.assertFalse(not part)
        self.assertEqual(3, len(part.images.all()))
        self.assertEqual(0, len(part.compatibilities.all()))
        part.hard_delete()
        part = PartInfo.get_by_number_and_brand(number, brand_id)
        self.assertTrue(not part)

    def test_do_no_images(self):
        number, brand_id = self.create_test_part(images=[])
        part = PartInfo.get_by_number_and_brand(number, brand_id)
        self.assertFalse(not part)
        self.assertEqual(0, len(part.images.all()))
        self.assertEqual(0, len(part.compatibilities.all()))
        part.hard_delete()
        part = PartInfo.get_by_number_and_brand(number, brand_id)
        self.assertTrue(not part)

    def test_bulk_save(self):
        number, brand_id = self.create_test_part(bulk_load=True)
        self.parts_loader_bulk.bulk_save_parts()
        part = PartInfo.get_by_number_and_brand(number, brand_id)
        self.assertFalse(not part)
        self.assertEqual(2, len(part.images.all()))
        self.assertEqual(0, len(part.compatibilities.all()))
        part.hard_delete()
        part = PartInfo.get_by_number_and_brand(number, brand_id)
        self.assertTrue(not part)

    def test_part_deletion(self):
        PartInfo.objects.filter(norm_part_number="testnumber", brand_id=3030).hard_delete()


if __name__ == "__main__":
    unittest.main()
