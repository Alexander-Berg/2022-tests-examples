from functools import partial
from unittest import TestCase

from yaphone.advisor.advisor.impression_id import ImpressionID
from yaphone.advisor.advisor.models.client import Client
from yaphone.advisor.advisor.models.lbs import LBSInfo
from yaphone.advisor.advisor.models.profile import Profile
from yaphone.advisor.project.version import VERSION

IMPRESSION_ID_TEMPLATE = 'launcher:goro;1:{obj_id}:0;1;2:1;2;3:{ext_rec}:{adv_version};' \
                         ':::{country};{current_country}:{content_type}'


# noinspection PyArgumentList
class ImpressionIDTest(TestCase):
    def setUp(self):
        self.impression_id = ImpressionID(
            app='launcher',
            experiment='goro',
            algorithm_code='1',
            position=(0, 1, 2),
            promo_enforcement_fields=(1, 2, 3),
            external_recommenders=[],
        )
        self.impression_id_template = partial(
            str.format, IMPRESSION_ID_TEMPLATE,
            adv_version=VERSION,
            obj_id=self.impression_id.view_id,
            country='',
            current_country='',
            ext_rec='0',
            content_type='apps'
        )

    def test_countries(self):
        self.impression_id.client = Client(profile=Profile(lbs_info=LBSInfo(country='DE', country_init='RU')))
        self.assertEqual(
            str(self.impression_id),
            self.impression_id_template(country='RU', current_country='DE')
        )

    def test_str_impression_id_with_one_external_recommender(self):
        self.impression_id.external_recommenders = ['facebook']
        self.assertEqual(
            str(self.impression_id),
            self.impression_id_template(ext_rec='1')
        )

    def test_str_impression_id_with_empty_external_recommenders(self):
        self.impression_id.external_recommenders = []
        self.assertEqual(
            str(self.impression_id),
            self.impression_id_template(ext_rec='0')
        )

    def test_str_impression_id_with_many_external_recommenders(self):
        self.impression_id.external_recommenders = ['facebook', 'direct']
        self.assertEqual(
            str(self.impression_id),
            self.impression_id_template(ext_rec='1;2')
        )

    def test_str_impression_id_keeps_order_of_external_recommenders(self):
        self.impression_id.external_recommenders = ['facebook', 'direct']
        self.assertEqual(
            str(self.impression_id),
            self.impression_id_template(ext_rec='1;2')
        )

        self.impression_id.external_recommenders = self.impression_id.external_recommenders[::-1]
        self.assertEqual(
            str(self.impression_id),
            self.impression_id_template(ext_rec='2;1')
        )

    def test_str_impression_id_unique_external_recommenders(self):
        self.impression_id.external_recommenders = ['facebook', 'facebook', 'direct']
        self.assertEqual(
            str(self.impression_id),
            self.impression_id_template(ext_rec='1;2')
        )

    def test_str_impression_id_skips_unknown_external_recommenders(self):
        self.impression_id.external_recommenders = ['facebook', 'unknown', 'direct']
        self.assertEqual(
            str(self.impression_id),
            self.impression_id_template(ext_rec='1;2')
        )

        self.impression_id.external_recommenders = ['unknown']
        self.assertEqual(
            str(self.impression_id),
            self.impression_id_template(ext_rec='0')
        )

    def test_from_string_impression_id_with_empty_external_recommender(self):
        impression_is_str = self.impression_id_template(ext_rec='0')
        impression_id = ImpressionID.from_string(impression_is_str)
        self.assertListEqual(impression_id.external_recommenders, [])

    def test_from_string_impression_id_with_one_external_recommender(self):
        impression_is_str = self.impression_id_template(ext_rec='1')
        impression_id = ImpressionID.from_string(impression_is_str)
        self.assertListEqual(impression_id.external_recommenders, ['facebook'])

    def test_from_string_impression_id_with_many_external_recommender(self):
        impression_is_str = self.impression_id_template(ext_rec='1;2')
        impression_id = ImpressionID.from_string(impression_is_str)
        self.assertListEqual(impression_id.external_recommenders, ['facebook', 'direct'])

    def test_str_impression_id_with_content_type(self):
        self.impression_id.content_type = 'apps'
        self.assertEqual(
            str(self.impression_id),
            self.impression_id_template(content_type='apps')
        )
