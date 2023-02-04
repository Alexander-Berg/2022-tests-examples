import pytest
import copy
import mock

from rest_framework.exceptions import NotFound

from smarttv.droideka.proxy.views.series import SeasonsView, EpisodesView, FIELD_MORE, FIELD_EPISODES, FIELD_SEASONS
from smarttv.droideka.utils import PlatformInfo, PlatformType
from smarttv.droideka.tests.mock import RequestStub, series_seasons_big_seasons_response, \
    series_seasons_medium_seasons_response, series_seasons_small_seasons_response
from smarttv.droideka.proxy import data_source


class TestFillMoreUrl:
    test_platform_info = PlatformInfo(PlatformType.ANDROID, '7.1.1', '1.2', None, None)

    FILL_MORE_URL_GENERAL_INFO = {'offset': 0, 'limit': 10, 'request': RequestStub(platform_info=test_platform_info)}

    @pytest.mark.parametrize('func, kwargs', [
        (SeasonsView.fill_more_seasons_url, dict({'series_id': 'test_id'}, **FILL_MORE_URL_GENERAL_INFO)),
        (EpisodesView.fill_more_episodes_url, dict({'season_id': 'test_id'}, **FILL_MORE_URL_GENERAL_INFO))
    ])
    def test_fill_more_url_with_no_data_field(self, func, kwargs):
        response = {}
        func(response=response, **kwargs)

        assert FIELD_MORE not in response

    def test_fill_more_url_items_less_than_limit(self):
        """
        Pass response with actual items less than limit
        """
        response = {FIELD_EPISODES: [{}]}
        SeasonsView.fill_more_seasons_url(response=response, **dict(
            {'series_id': 'test_id'}, **self.FILL_MORE_URL_GENERAL_INFO))

        assert FIELD_MORE not in response

    def fill_more_url_for_episodes_if_items_less_limit(self):
        response = {FIELD_EPISODES: [{}]}

        EpisodesView.fill_more_episodes_url(response=response, **dict(
            {'season_id': 'test_id'}, **self.FILL_MORE_URL_GENERAL_INFO))

        assert FIELD_MORE in response

    @pytest.mark.parametrize('func, kwargs, response', [
        (
            SeasonsView.fill_more_seasons_url,
            dict({'series_id': 'test_id'}, **FILL_MORE_URL_GENERAL_INFO),
            {FIELD_SEASONS: []}
        ),
        (
            SeasonsView.fill_more_seasons_url,
            dict({'series_id': 'test_id'}, **FILL_MORE_URL_GENERAL_INFO),
            {}
        ),
        (
            EpisodesView.fill_more_episodes_url,
            dict({'season_id': 'test_id'}, **FILL_MORE_URL_GENERAL_INFO),
            {FIELD_EPISODES: []}
        ),
        (
            EpisodesView.fill_more_episodes_url,
            dict({'season_id': 'test_id'}, **FILL_MORE_URL_GENERAL_INFO),
            {}
        )
    ])
    def test_not_fill_more_url_when_zero_items(self, func, kwargs, response):
        response = copy.deepcopy(response)
        func(response=response, **kwargs)

        assert FIELD_MORE not in response


class TestUnboundSeasonsQueryConfig:

    @pytest.mark.parametrize(
        'season_number, backward_episodes_count, forward_episodes_count, stop_at_season_boundaries, expected_offset',
        [
            (1, 5, 5, False, 0),
            (1, 5, 5, True, 0),
            (6, 5, 5, False, 1),
            (6, 5, 5, True, 5),
            (7, 5, 5, True, 6),
        ])
    def test_seasons_offset_ok(
            self, season_number, backward_episodes_count, forward_episodes_count, stop_at_season_boundaries,
            expected_offset):
        config = data_source.UnboundSeasonsQueryConfig(season_number, backward_episodes_count,
                                                       forward_episodes_count, stop_at_season_boundaries)
        assert expected_offset == config.seasons_offset

    @pytest.mark.parametrize(
        'season_number, backward_episodes_count, forward_episodes_count, stop_at_season_boundaries, expected_limit',
        [
            (1, 5, 5, False, 6),
            (1, 5, 5, True, 1),
            (6, 5, 5, False, 11),
            (6, 5, 5, True, 1),
        ])
    def test_seasons_limit(
            self, season_number, backward_episodes_count, forward_episodes_count, stop_at_season_boundaries,
            expected_limit):
        config = data_source.UnboundSeasonsQueryConfig(season_number, backward_episodes_count,
                                                       forward_episodes_count, stop_at_season_boundaries)
        assert expected_limit == config.seasons_limit


class TestUnboundSeasonsDataSource:
    GENERAL_SERIES_ID = '1'

    general_mock_data = {'season_number': 8, 'backward_count': 4, 'forward_count': 5, 'stop_at_season_boundaries': False, 'series_id': GENERAL_SERIES_ID, 'pivot_episode_number': 18}

    seasons_request_big = {'season_number': 3, 'pivot_episode_number': 13, 'backward_count': 5, 'forward_count': 5, 'stop_at_season_boundaries': False, 'series_id': GENERAL_SERIES_ID}
    seasons_request_big_stop = {'season_number': 3, 'pivot_episode_number': 13, 'backward_count': 5, 'forward_count': 5, 'stop_at_season_boundaries': True, 'series_id': GENERAL_SERIES_ID}

    seasons_request_medium = {'season_number': 3, 'pivot_episode_number': 2, 'backward_count': 5, 'forward_count': 5, 'stop_at_season_boundaries': False, 'series_id': GENERAL_SERIES_ID}
    seasons_request_medium_stop = {'season_number': 3, 'pivot_episode_number': 2, 'backward_count': 5, 'forward_count': 5, 'stop_at_season_boundaries': True, 'series_id': GENERAL_SERIES_ID}

    seasons_request_small = {'season_number': 3, 'pivot_episode_number': 1, 'backward_count': 5, 'forward_count': 5, 'stop_at_season_boundaries': False, 'series_id': GENERAL_SERIES_ID}
    seasons_request_small_stop = {'season_number': 3, 'pivot_episode_number': 1, 'backward_count': 5, 'forward_count': 5, 'stop_at_season_boundaries': True, 'series_id': GENERAL_SERIES_ID}

    get_seasons_ok_test_data = [
        ({'season_number': 3, 'pivot_episode_number': 13, 'backward_count': 5, 'forward_count': 5,
          'stop_at_season_boundaries': False, 'series_id': GENERAL_SERIES_ID}, [
            {'offset': 7, 'limit': 8, 'season_id': '3'}, {'offset': 0, 'limit': 3, 'season_id': '4'}],
         series_seasons_big_seasons_response),
        ({'season_number': 3, 'pivot_episode_number': 13, 'backward_count': 5, 'forward_count': 5,
          'stop_at_season_boundaries': True, 'series_id': GENERAL_SERIES_ID}, [
            {'offset': 7, 'limit': 8, 'season_id': '3'}], series_seasons_big_seasons_response),
        ({'season_number': 3, 'pivot_episode_number': 2, 'backward_count': 5, 'forward_count': 5,
          'stop_at_season_boundaries': False, 'series_id': GENERAL_SERIES_ID}, [
            {'offset': 0, 'limit': 2, 'season_id': '1'}, {'offset': 0, 'limit': 2, 'season_id': '2'},
            {'offset': 0, 'limit': 2, 'season_id': '3'}, {'offset': 0, 'limit': 2, 'season_id': '4'},
            {'offset': 0, 'limit': 2, 'season_id': '5'}, {'offset': 0, 'limit': 1, 'season_id': '6'}],
         series_seasons_medium_seasons_response),
        ({'season_number': 3, 'pivot_episode_number': 2, 'backward_count': 5, 'forward_count': 5,
          'stop_at_season_boundaries': True, 'series_id': GENERAL_SERIES_ID}, [
            {'offset': 0, 'limit': 2, 'season_id': '3'}], series_seasons_medium_seasons_response),
        ({'season_number': 3, 'pivot_episode_number': 1, 'backward_count': 5, 'forward_count': 5,
          'stop_at_season_boundaries': False, 'series_id': GENERAL_SERIES_ID}, [
            {'offset': 0, 'limit': 1, 'season_id': '1'}, {'offset': 0, 'limit': 1, 'season_id': '2'},
            {'offset': 0, 'limit': 1, 'season_id': '3'}, {'offset': 0, 'limit': 1, 'season_id': '4'},
            {'offset': 0, 'limit': 1, 'season_id': '5'}, {'offset': 0, 'limit': 1, 'season_id': '6'},
            {'offset': 0, 'limit': 1, 'season_id': '7'}, {'offset': 0, 'limit': 1, 'season_id': '8'}],
         series_seasons_small_seasons_response),
        ({'season_number': 3, 'pivot_episode_number': 1, 'backward_count': 5, 'forward_count': 5,
          'stop_at_season_boundaries': True, 'series_id': GENERAL_SERIES_ID}, [
            {'offset': 0, 'limit': 1, 'season_id': '3'}], series_seasons_small_seasons_response),
    ]

    @pytest.mark.parametrize('input, output', [
        ([{'season_id': 1, 'offset': 0, 'limit': 2}, {'season_id': 1, 'offset': 2, 'limit': 3}], [{'season_id': 1, 'offset': 0, 'limit': 5}]),
        ([{'season_id': 1, 'offset': 0, 'limit': 2}, {'season_id': 2, 'offset': 2, 'limit': 3}], [{'season_id': 1, 'offset': 0, 'limit': 2}, {'season_id': 2, 'offset': 2, 'limit': 3}])
    ])
    def test_merge_same_seasons_ok(self, input, output):
        assert output == data_source.UnboundSeasonsDataSource.merge_same_seasons(input)

    @pytest.mark.parametrize('fill_direction, expected_rest_episode_count', [
        (data_source.FillDirection.BACKWARD, 4),
        (data_source.FillDirection.FORWARD, 6)
    ])
    def test_get_rest_episodes_ok(self, fill_direction, expected_rest_episode_count):
        assert expected_rest_episode_count == data_source.UnboundSeasonsDataSource(self.general_mock_data).\
            _get_rest_episodes_count(fill_direction)

    def test_get_raw_seasons_ok(self):
        seasons = [{'season_id': 1}]
        seasons_response = {'set': seasons}

        assert seasons == data_source.UnboundSeasonsDataSource(self.general_mock_data)._get_raw_seasons(seasons_response)

    @pytest.mark.parametrize('seasons_response', [
        None,
        {},
        {'set': None},
        {'set': []},
    ])
    def test_get_raw_seasons_404(self, seasons_response):
        with pytest.raises(NotFound):
            data_source.UnboundSeasonsDataSource(self.general_mock_data)._get_raw_seasons(seasons_response)

    @pytest.mark.parametrize('seasons, season_number, expected_index', [
        ([{'season_number': 1}, {'season_number': 2}, {'season_number': 3}], 2, 1),
        ([{'season_number': 3}, {'season_number': 4}, {'season_number': 5}], 4, 1),
        ([{'season_number': 3}], 3, 0),
    ])
    def test_get_season_ok(self, seasons, season_number, expected_index):
        index, _ = data_source.UnboundSeasonsDataSource(self.general_mock_data)._get_season(seasons,
                                                                                            season_number)
        assert expected_index == index

    @pytest.mark.parametrize('season_list, season_number', [
        ([{'season_number': 3}], 4),
        ([{'season_number': 2}, {'season_number': 3}], 4),
        ([], 4),
        (None, 4),
    ])
    def test_get_season_404(self, season_list, season_number):
        with pytest.raises(NotFound):
            data_source.UnboundSeasonsDataSource(self.general_mock_data)._get_season(season_list, season_number)

    def test_get_seasons_404(self):
        seasons = [{'season_number': 8, 'episodes_count': 15}]
        with pytest.raises(NotFound):
            data_source.UnboundSeasonsDataSource(self.general_mock_data)._get_seasons(seasons)

    @pytest.mark.parametrize('seasons_request, seasons_response, mock_vh_response', get_seasons_ok_test_data)
    def test_get_seasons_ok(self, seasons_request, seasons_response, mock_vh_response):
        with mock.patch('smarttv.droideka.proxy.api.vh.client.seasons',
                        mock.Mock(return_value=mock_vh_response)):
            result = data_source.UnboundSeasonsDataSource(seasons_request).get_result()

            assert seasons_response == result['seasons']
