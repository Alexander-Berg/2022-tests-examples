import pytest
import mock
from typing import Iterable
from copy import deepcopy

from smarttv.droideka.proxy.api.vh import ChannelIdFaker
from smarttv.droideka.proxy.transform import Channels, ChannelInfo
from smarttv.droideka.proxy.views.channels import ChannelsView, ChannelsViewV8

HIDDEN_CHANNEL_ID = '4062006f5228bf7d8380fd631687d533'
SUBSCRIPTION_CHANNEL_ID = '4fec709cf581ef3c94a9aa39a8564724'
FILTERED_CHANNEL_ID = '446006da0f7554fa8e820812181c45f4'
ALLOWED_CHANNEL_ID = '4dc538ead8ef64b1827217c0b7d5c442'
WITHOUT_SMARTTV_NUMBER_CHANNEL_ID = '4ab538ead8ef64b1827217c0b7d5c000'


class FilterChannelsTestData:

    def __init__(self, channels: Iterable):
        self.channels = list(deepcopy(channels))

    @property
    def show_hidden(self) -> bool:
        return True

    @property
    def filter_subscription_channels(self) -> bool:
        return False

    @property
    def filter_channel_ids(self) -> Iterable:
        return []

    @property
    def expected_filtered_channels_id(self) -> Iterable:
        return (WITHOUT_SMARTTV_NUMBER_CHANNEL_ID, )

    @property
    def args(self) -> dict:
        return {
            'channels': self.channels,
            'show_hidden': self.show_hidden,
            'filter_subscription_channels': self.filter_subscription_channels,
            'filter_channel_ids': self.filter_channel_ids,
        }

    @property
    def expected_result(self):
        return list(filter(lambda channel: channel['content_id'] not in self.expected_filtered_channels_id,
                           self.channels))


class FilterHiddenChannelsTestData(FilterChannelsTestData):

    @property
    def show_hidden(self) -> bool:
        return False

    @property
    def expected_filtered_channels_id(self) -> Iterable:
        return HIDDEN_CHANNEL_ID, WITHOUT_SMARTTV_NUMBER_CHANNEL_ID


class FilterSubscriptionChannelsTestData(FilterChannelsTestData):

    @property
    def filter_subscription_channels(self) -> bool:
        return True

    @property
    def expected_filtered_channels_id(self) -> Iterable:
        return SUBSCRIPTION_CHANNEL_ID, WITHOUT_SMARTTV_NUMBER_CHANNEL_ID


class FilterChannelIdsTestData(FilterChannelsTestData):

    @property
    def filter_channel_ids(self) -> Iterable:
        return [FILTERED_CHANNEL_ID]

    @property
    def expected_filtered_channels_id(self) -> Iterable:
        return FILTERED_CHANNEL_ID, WITHOUT_SMARTTV_NUMBER_CHANNEL_ID


class AllFiltersAppliedTestData(FilterChannelsTestData):
    @property
    def show_hidden(self) -> bool:
        return False

    @property
    def filter_subscription_channels(self) -> bool:
        return True

    @property
    def filter_channel_ids(self) -> Iterable:
        return [FILTERED_CHANNEL_ID]

    @property
    def expected_filtered_channels_id(self) -> Iterable:
        return HIDDEN_CHANNEL_ID, SUBSCRIPTION_CHANNEL_ID, FILTERED_CHANNEL_ID, WITHOUT_SMARTTV_NUMBER_CHANNEL_ID


class TestFilterChannels:
    view = ChannelsView()

    channels = (
        {
            'content_id': HIDDEN_CHANNEL_ID,
            'auto_fields': {'channel_smarttv_number': 1},
            'title': 'Волгоград 1',
            'status': [
                'has_vod',
                'auto_announce',
                'has_schedule',
                'published',
                'hidden'
            ],

        },
        {
            'content_id': SUBSCRIPTION_CHANNEL_ID,
            'auto_fields': {'channel_smarttv_number': 1},
            'title': 'Univer TV',
            'ottParams': {
                'monetizationModel': 'SVOD',
                'serviceName': 'ya-main',
                'contentTypeID': 2,
                'licenses': [
                    {
                        'monetizationModel': 'SVOD',
                        'offerText': '30 дней бесплатно',
                        'offerSubText': 'далее 199 ₽ в месяц',
                        'primary': True,
                        'purchaseTag': 'plus'
                    }
                ],
                'puid': '493032576',
                'reqid': '1599465008665736-9909909731231498769',
                'yandexuid': '1640136451583854875',
                'uuid': '4f07beabb7066d659f88eb802a2ada68',
                'from': 'unknown',
                'subscriptionType': ''
            }
        },
        {
            'content_id': FILTERED_CHANNEL_ID,
            'auto_fields': {'channel_smarttv_number': 1},
            'title': 'Вся Уфа',
        },
        {
            'content_id': ALLOWED_CHANNEL_ID,
            'auto_fields': {'channel_smarttv_number': 1},
            'title': 'Успех',
        },
        {
            'content_id': WITHOUT_SMARTTV_NUMBER_CHANNEL_ID,
            'title': 'Успех',
        },
    )

    test_data = [
        FilterChannelsTestData(channels),
        FilterHiddenChannelsTestData(channels),
        FilterSubscriptionChannelsTestData(channels),
        FilterChannelIdsTestData(channels),
        AllFiltersAppliedTestData(channels),
    ]

    @pytest.mark.parametrize('data', test_data)
    def test_channels_filtered(self, data):
        actual_result = self.view.filter_channels(**data.args)

        assert data.expected_result == actual_result


class TestCategories:

    def get_category_title(self, category_name):
        return Channels.categories_mapping.get(category_name)

    def test_channels_order_inside_category(self):
        """
        Убедимся, что get_categorized возвращает каналы, отсортированные по
        channel_smarttv_number внутри категории
        """
        channels = [
            ChannelInfo(id='second', categories=['sport'], number=2),
            ChannelInfo(id='first', categories=['sport'], number=1),
            ChannelInfo(id='third', categories=['sport'], number=3),
        ]
        categories = Channels.get_categorized(channels)
        assert categories[0]['name'] == self.get_category_title('sport')
        assert categories[0]['channel_ids'] == ['first', 'second', 'third']

    def test_categories_order(self):
        """
        Категории ближе к началу содержат каналы с меньшими номерами
        """
        channels = [
            ChannelInfo(id='sport1', categories=['sport'], number=21),
            ChannelInfo(id='sport2', categories=['sport'], number=22),
            ChannelInfo(id='info1', categories=['inform'], number=11),
            ChannelInfo(id='info2', categories=['inform'], number=12),
            ChannelInfo(id='films1', categories=['films'], number=31),
            ChannelInfo(id='films2', categories=['films'], number=32),
        ]

        categories = Channels.get_categorized(channels)
        # категория inform идет перед спортом, потому что в ней номера каналов меньше
        assert categories[0]['name'] == self.get_category_title('inform')
        assert categories[1]['name'] == self.get_category_title('sport')
        assert categories[2]['name'] == self.get_category_title('films')


class TestChannelOffsetVHClient():
    OFFSET = 1000

    @mock.patch('smarttv.droideka.proxy.api.vh.FrontendVHApi.channels_regions')
    def test_offset_disabled(self, mocked_channels_regions):
        mocked_channels_regions.return_value = {'set': [{'channel_id': 1}]}

        client = ChannelIdFaker(offset=0, url='', timeout=None, retries=None)
        answer = client.channels_regions(None, None)
        assert answer['set'][0]['channel_id'] == 1

    @mock.patch('smarttv.droideka.proxy.api.vh.FrontendVHApi.channels_regions')
    def test_offset_working(self, mocked_channels_regions):
        mocked_channels_regions.return_value = {'set': [{'channel_id': 1}]}

        client = ChannelIdFaker(offset=self.OFFSET, url='', timeout=None, retries=None)
        answer = client.channels_regions(None, None)
        assert answer['set'][0]['channel_id'] == 1 + self.OFFSET

    @mock.patch('smarttv.droideka.proxy.api.vh.FrontendVHApi.channels_regions')
    def test_items_without_channel_id(self, mocked_channels_regions):
        mocked_channels_regions.return_value = {'set': [{}, {}, {}]}
        client = ChannelIdFaker(offset=self.OFFSET, url='', timeout=None, retries=None)
        answer = client.channels_regions(None, None)
        assert len(answer['set']) == 3  # ошибок не райзится

    @mock.patch('smarttv.droideka.proxy.api.vh.FrontendVHApi.channels_regions')
    def test_strange_responses(self, mocked_channels_regions):
        mocked_channels_regions.return_value = {'set': [
            {'channel_id': 0},
            {'channel_id': None},
            {'channel_id': 's'},
            {'channel_id': b'som'},
        ]}
        # если из vh приходят некорректные айдишники, не трогаем их
        client = ChannelIdFaker(offset=self.OFFSET, url='', timeout=None, retries=None)
        answer = client.channels_regions(None, None)
        assert len(answer['set']) == 4  # ошибок не райзится
        assert answer['set'][0]['channel_id'] == 0
        assert answer['set'][1]['channel_id'] is None
        assert answer['set'][2]['channel_id'] == 's'
        assert answer['set'][3]['channel_id'] == b'som'


class TestPaidChannelDetection:
    """
    Кейс проверяет, что по разным доступным лицензиям в ответе ottParams определяется
    платный канал. А платные каналы мы не показываем клиентам < v1.4
    """
    avod = {'monetizationModel': 'AVOD'}
    svod = {'monetizationModel': 'SVOD'}
    othervod = {'monetizationModel': 'OTHERVOD'}
    test_data = [
        (None, False),  # no ottParamas means free channel
        ({}, False),  # no licenses is free
        ({'licenses': [avod]}, False),
        ({'licenses': [svod]}, True),
        ({'licenses': [othervod]}, True),
        ({'licenses': [avod, svod]}, False),
        ({'licenses': [avod, othervod]}, False),
    ]

    @pytest.mark.parametrize('input,result', test_data)
    def test_is_paid_cases(self, input, result):
        view = ChannelsView()
        assert view.is_paid_channel(input) is result


class TestUserRegionDetector:
    """
    Проверяет детектор страны (нужно, чтобы смотрешка была доступна только из России)
    """

    @pytest.fixture
    def geobase(self, mocker):
        return mocker.patch('smarttv.droideka.proxy.views.channels.geobase')

    def test(self, geobase):
        request = mock.Mock()
        geobase.get_region_by_ip.return_value = {'id': 100}
        geobase.get_region_by_id.return_value = {'iso_name': 'RU'}
        request.headers = {'X-Real-IP': '127.0.0.1'}
        view = ChannelsView()
        assert view.get_user_region(request=request) == (100, 'RU')
        geobase.get_region_by_ip.assert_called_with('127.0.0.1')


class TestIsSmotreshkaEnabled:

    @pytest.fixture
    def shared_pref(self, mocker):
        mocker.patch(
            'smarttv.droideka.proxy.views.channels.SharedPreferences',
            mock.Mock(get_int=lambda x: 1)
        )

    @pytest.mark.parametrize('country,expected', [
        ('RU', True),
        ('', False),
        (None, False),
    ])
    def test_function(self, country, expected, shared_pref):
        view = ChannelsViewV8()
        assert view.is_smotreshka_enabled(country) == expected
