import json
import mock
from django.test import TestCase
from rest_framework import status
from yaphone.advisor.common import localization_keys

from yaphone.advisor.advisor.models import check_mock
from yaphone.advisor.advisor.tests.views import BasicAdvisorViewTest
from yaphone.advisor.common.mocks.geobase import LookupMock
from yaphone.advisor.launcher.models.wallpapers import (WallpaperCategories, WallpapersFeed, WallpaperFeedback,
                                                        WallpaperStatus, WallpaperColors)
from yaphone.advisor.launcher.tests.base import StatelessViewMixin, RequiredParamsMixin
from yaphone.advisor.launcher.tests.fixtures import load_fixture, get_localization_fixture


# noinspection PyPep8Naming
class WallpapersMixin(object):
    def setUp(self):
        self.set_localization_key(localization_keys.WALLPAPERS, get_localization_fixture('wallpapers'))
        check_mock()
        load_fixture('wallpapers', WallpaperCategories)
        super(WallpapersMixin, self).setUp()

    def tearDown(self):
        check_mock()
        WallpaperCategories.objects.delete()
        WallpaperFeedback.objects.delete()
        WallpaperStatus.objects.delete()
        super(WallpapersMixin, self).tearDown()

    def test_count(self):
        response = self.request()
        content = json.loads(response.content)
        self.assertEqual(len(content), 1)


class WallpapersTest(WallpapersMixin, BasicAdvisorViewTest, TestCase):
    endpoint = '/api/v1/wallpapers'

    default_params = {}


@mock.patch('yaphone.utils.geo.geobase_lookuper', LookupMock())
class WallpapersV2Test(WallpapersMixin, StatelessViewMixin, TestCase):
    endpoint = '/api/v2/wallpapers'
    method = 'GET'
    required_params = 'screen_height', 'screen_width', 'dpi'

    default_params = {
        'screen_height': 1920,
        'screen_width': 1080,
        'dpi': 480,
    }


@mock.patch('yaphone.utils.geo.geobase_lookuper', LookupMock())
class WallpapersV3Test(WallpapersMixin, StatelessViewMixin, TestCase):
    endpoint = '/api/v3/wallpapers'
    method = 'GET'
    required_params = 'screen_height', 'screen_width', 'dpi'

    default_params = {
        'screen_height': 1920,
        'screen_width': 1080,
        'dpi': 480,
    }

    def test_composite_covers_scheme(self):
        request_data = self.request().data.pop()
        cover = request_data.get('cover')
        self.assertIsNotNone(cover)
        self.assertIn('title', cover)
        self.assertIn('subtitle', cover)
        self.assertIn('background', cover)


# noinspection PyPep8Naming
class GetWallpaperTest(RequiredParamsMixin, TestCase):
    endpoint = '/api/v2/get_wallpaper'
    method = 'GET'
    required_params = 'screen_height', 'id'

    default_params = {
        'screen_height': 1920,
        'id': 'wallpapers/images/05_6246e7fddc701d8733608259f764241b.jpg',
    }

    def get(self, params=None, follow=True, **kwargs):
        return self.client.get(
            follow=follow,
            path=self.endpoint,
            data=params or self.default_params,
            **kwargs
        )

    def test_ok(self, *args):
        self.assertEqual(self.request(follow=False).status_code, status.HTTP_302_FOUND)


# noinspection PyPep8Naming
class WallpapersAutochangeTest(StatelessViewMixin, TestCase):
    endpoint = '/api/v3/wallpapers/autochange'
    method = 'GET'
    default_params = {
        'screen_height': 1920,
        'screen_width': 1080,
        'dpi': 420,
        'mode': 'hourly',
    }
    required_params = 'mode', 'screen_height', 'screen_width', 'dpi'

    def setUp(self):
        self.set_localization_key(localization_keys.WALLPAPERS, get_localization_fixture('wallpapers'))
        check_mock()
        load_fixture('wallpapers_status', WallpaperStatus)
        load_fixture('wallpapers_autochange', WallpaperCategories)
        WallpaperFeedback(action='force_next', wallpaper='test', uuid='0' * 32).save()
        super(WallpapersAutochangeTest, self).setUp()

    def tearDown(self):
        check_mock()
        WallpaperCategories.objects.delete()
        WallpaperFeedback.objects.delete()
        WallpaperStatus.objects.delete()
        super(WallpapersAutochangeTest, self).tearDown()

    @staticmethod
    def get_wallpaper_keys(response):
        return [wallpaper['id'] for wallpaper in json.loads(response.content)['wallpapers']]

    def test_bandits(self):
        self.set_localization_key(localization_keys.WALLPAPERS_AUTOCHANGE_STRATEGY, 'bandits')
        response = self.request()
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_random(self):
        self.set_localization_key(localization_keys.WALLPAPERS_AUTOCHANGE_STRATEGY, 'random')
        response = self.request()
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_most_picked(self):
        self.set_localization_key(localization_keys.WALLPAPERS_AUTOCHANGE_STRATEGY, 'most_picked')
        response = self.request()
        wallpapers = self.get_wallpaper_keys(response)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(wallpapers, [
            '1_7c2dbe2d47323fb253cc47cb9224a117.jpg',
            '2_d551662a5b3df9123c7c5fd9ced9687b.jpg',
            '4_d732e849be58a67ed7a3efdfbb7fa4d3.jpg',
            '3_aa54b8cd693031098fc02c0c286457c9.jpg',
        ])

    def test_less_skipped(self):
        self.set_localization_key(localization_keys.WALLPAPERS_AUTOCHANGE_STRATEGY, 'less_skipped')
        response = self.request()
        wallpapers = self.get_wallpaper_keys(response)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(wallpapers, [
            '1_7c2dbe2d47323fb253cc47cb9224a117.jpg',
            '2_d551662a5b3df9123c7c5fd9ced9687b.jpg',
            '3_aa54b8cd693031098fc02c0c286457c9.jpg',
            '4_d732e849be58a67ed7a3efdfbb7fa4d3.jpg',
        ])


class WallpapersActionTest(RequiredParamsMixin, TestCase):
    endpoint = '/api/v3/wallpapers/action'
    method = 'POST'
    default_params = {
        'id': 'wallpapers/image/some_image.jpg',
        'action': 'force_next',
    }
    wallpaper_actions = {
        'force_next',
        'auto_next',
        'collection_pick',
    }

    def setUp(self):
        check_mock()
        load_fixture('wallpapers', WallpaperCategories)
        super(WallpapersActionTest, self).setUp()

    def tearDown(self):
        check_mock()
        WallpaperCategories.objects.delete()
        WallpaperFeedback.objects.delete()
        WallpaperStatus.objects.delete()
        super(WallpapersActionTest, self).tearDown()

    def test_feedback_and_status(self):
        self.request()
        wallpaper_key = self.default_params['id'].split('/')[-1]
        action = self.default_params['action']

        wallpaper_feedback = WallpaperFeedback.objects(wallpaper=wallpaper_key)[0]
        wallpaper_status = WallpaperStatus.objects.get(wallpaper=wallpaper_key)

        self.assertIsNotNone(wallpaper_feedback)
        self.assertIsNotNone(wallpaper_status)

        self.assertEqual(wallpaper_feedback.wallpaper, wallpaper_key)
        self.assertEqual(wallpaper_feedback.action, action)

        for wallpaper_action in self.wallpaper_actions:
            value = 1 if wallpaper_action == action else 0
            self.assertEqual(getattr(wallpaper_status, wallpaper_action + '_count'), value)

    def test_status_auto_next_rate(self):
        counts = {
            'force_next': 10,
            'auto_next': 3,
        }
        rate = 1. * counts['auto_next'] / (counts['auto_next'] + counts['force_next'])

        for action, count in counts.items():
            for _ in range(count):
                self.request(data={'id': self.default_params['id'], 'action': action})

        wallpaper_key = self.default_params['id'].split('/')[-1]
        wallpaper_status = WallpaperStatus.objects.get(wallpaper=wallpaper_key)
        self.assertAlmostEqual(wallpaper_status.auto_next_rate, rate)
        self.assertEqual(wallpaper_status.force_next_count, counts['force_next'])
        self.assertEqual(wallpaper_status.auto_next_count, counts['auto_next'])


def get_wallpapers_feed_mock(feed_id, color=None, offset=None, limit=None):
    wallpapers_feed = WallpapersFeed.objects(feed_id=feed_id, color=color).first()
    if wallpapers_feed and offset is not None:
        wallpapers_feed.wallpapers = wallpapers_feed.wallpapers[offset:offset + limit]
    return wallpapers_feed


@staticmethod
def shuffle_wallpapers_mock(wallpapers, sigma=5):
    """just don't shuffle"""
    return wallpapers


# noinspection PyPep8Naming
class WallpapersFeedTest(StatelessViewMixin, TestCase):
    endpoint = '/api/v3/wallpapers/feed'
    method = 'GET'
    default_params = {
        'screen_height': 1920,
        'screen_width': 1080,
        'dpi': 420,
    }
    required_params = 'screen_height', 'screen_width', 'dpi'

    def setUp(self):
        self.set_localization_key(localization_keys.WALLPAPERS, get_localization_fixture('wallpapers'))
        self.set_localization_key(localization_keys.WALLPAPERS_FEED_COLOR_GROUP, 'first')
        check_mock()
        load_fixture('wallpapers_status', WallpaperStatus)
        load_fixture('wallpapers_autochange', WallpaperCategories)
        load_fixture('wallpaper_colors', WallpaperColors)
        super(WallpapersFeedTest, self).setUp()

    def tearDown(self):
        check_mock()
        WallpaperCategories.objects.delete()
        WallpaperFeedback.objects.delete()
        WallpaperStatus.objects.delete()
        WallpaperColors.objects.delete()
        super(WallpapersFeedTest, self).tearDown()

    @staticmethod
    def get_wallpaper_keys(response):
        return [wallpaper['id'] for wallpaper in json.loads(response.content)['wallpapers']]

    def test_colors(self):
        pass

    @mock.patch('yaphone.advisor.launcher.wallpapers.NewWallpapersFeedLoader.shuffle_wallpapers', shuffle_wallpapers_mock)
    def test_most_picked(self):
        response = self.request()
        wallpapers = self.get_wallpaper_keys(response)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(wallpapers, [
            'wallpapers/images/1_7c2dbe2d47323fb253cc47cb9224a117.jpg',
            'wallpapers/images/2_d551662a5b3df9123c7c5fd9ced9687b.jpg',
            'wallpapers/images/4_d732e849be58a67ed7a3efdfbb7fa4d3.jpg',
            'wallpapers/images/3_aa54b8cd693031098fc02c0c286457c9.jpg',
        ])

    @mock.patch('yaphone.advisor.launcher.wallpapers.get_wallpapers_feed', get_wallpapers_feed_mock)
    @mock.patch('yaphone.advisor.launcher.wallpapers.NewWallpapersFeedLoader.shuffle_wallpapers', shuffle_wallpapers_mock)
    def test_sequentially(self):
        wallpapers_initial = [
            'wallpapers/images/1_7c2dbe2d47323fb253cc47cb9224a117.jpg',
            'wallpapers/images/2_d551662a5b3df9123c7c5fd9ced9687b.jpg',
            'wallpapers/images/4_d732e849be58a67ed7a3efdfbb7fa4d3.jpg',
            'wallpapers/images/3_aa54b8cd693031098fc02c0c286457c9.jpg',
        ]
        wallpapers_limited = [
            'wallpapers/images/1_7c2dbe2d47323fb253cc47cb9224a117.jpg',
            'wallpapers/images/2_d551662a5b3df9123c7c5fd9ced9687b.jpg',
        ]
        wallpapers_colored = [
            'wallpapers/images/1_7c2dbe2d47323fb253cc47cb9224a117.jpg',
            'wallpapers/images/3_aa54b8cd693031098fc02c0c286457c9.jpg',
        ]
        response = self.request()
        wallpapers = self.get_wallpaper_keys(response)
        feed_id = json.loads(response.content)['feed_id']
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(wallpapers, wallpapers_initial)

        params = self.default_params.copy()
        params['feed_id'] = feed_id
        response = self.request(data=params)
        wallpapers = self.get_wallpaper_keys(response)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(json.loads(response.content)['feed_id'], feed_id)
        self.assertEqual(wallpapers, wallpapers_initial)

        params['limit'] = 2
        response = self.request(data=params)
        wallpapers = self.get_wallpaper_keys(response)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(json.loads(response.content)['feed_id'], feed_id)
        self.assertEqual(wallpapers, wallpapers_limited)

        del params['limit']
        params['color'] = 'first/red'
        response = self.request(data=params)
        wallpapers = self.get_wallpaper_keys(response)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(json.loads(response.content)['feed_id'], feed_id)
        self.assertEqual(wallpapers, wallpapers_colored)
