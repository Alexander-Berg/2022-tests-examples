import datetime
import json
import mock
from django.test import TestCase
from rest_framework import status

from yaphone.advisor.advisor.models import check_mock
from yaphone.advisor.common.mocks.geobase import LookupMock
from yaphone.advisor.launcher.models import db
from yaphone.advisor.launcher.tests.base import StatelessViewMixin


class MusicWallpapersMixin(object):
    def setUp(self):
        check_mock()
        db.music_wallpapers.insert_many([
            {
                u'image': {
                    u'key': u'music-wallpaper-10.jpg',
                    u'hash': u'8ee8bc400e863cddc5703c651cc41b50',
                    u'width': 1,
                    u'height': 2,
                },
                u'updated_at': datetime.datetime(2018, 8, 23, 15, 57, 6, 828000)
            },
            {
                u'image': {
                    u'key': u'music-wallpaper-11.jpg',
                    u'hash': u'8a3015e0d92bbc5acb967ecdd86c9449',
                    u'width': 1,
                    u'height': 2,
                },
                u'updated_at': datetime.datetime(2018, 8, 24, 9, 13, 27, 980000)
            }
        ])
        super(MusicWallpapersMixin, self).setUp()

    def tearDown(self):
        check_mock()
        db.music_wallpapers.delete_many({})
        super(MusicWallpapersMixin, self).tearDown()


@mock.patch('yaphone.utils.geo.geobase_lookuper', LookupMock())
class MusicWallpapersTest(MusicWallpapersMixin, StatelessViewMixin, TestCase):
    endpoint = '/api/v1/music_wallpapers'

    def setUp(self):
        super(MusicWallpapersTest, self).setUp()
        self.client.defaults['HTTP_HOST'] = 'lockscreen'

    def test_ok(self):
        self.assertEqual(self.get().status_code, status.HTTP_200_OK)

    def test_files(self):
        self.assertEqual(len(json.loads(self.get().content)), 2)
