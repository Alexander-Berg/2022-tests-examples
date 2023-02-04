import json
from mock import patch
import pytest

from django.test import Client

from smarttv.droideka.proxy.player_detection import PlayerDetector
from smarttv.droideka.tests import mock as predefined_mock
from smarttv.droideka.proxy.player_detection import SearchRequestPlayerIdFiller
from smarttv.droideka.tests.mock import MockTvmClient
import mock

mock_tvm_client = MockTvmClient()
required_headers = {
    'HTTP_USER_AGENT': 'com.yandex.tv.home/1.2.773 (Realtek SmartTV; Android 7.1.1)',
    'HTTP_X_YAUUID': 'd68c1bca4efa403313837b12f5cdcd26',
    'HTTP_X_YADEVICEID': 'ca9b68da30474d5db0bbd1e8a25565bb',
}

client = Client(content_type='application.json')


@mock.patch('smarttv.droideka.proxy.views.card.CardDetailV6View.load_memento_configs',
            mock.Mock(return_value=None))
@mock.patch('smarttv.droideka.proxy.views.card.ThinCardDetailView.load_memento_configs',
            mock.Mock(return_value=None))
@mock.patch('smarttv.droideka.proxy.views.series.EpisodesView.load_memento_configs',
            mock.Mock(return_value=None))
@mock.patch('smarttv.droideka.proxy.tvm.tvm_client', mock.Mock(mock_tvm_client))
@mock.patch('smarttv.droideka.proxy.views.card.cinema_db.get_allowed_cinema_codes',
            mock.Mock(return_value=[]))
class TestPlayerId:
    base_card_detail_query_params = {'content_id': 'some_content_id'}
    base_series_episodes_query_params = {'season_id': '123'}

    mocks = predefined_mock.PlayerIdTestData()

    def _get_result_query_params(self, content_type: str):
        params = self.base_card_detail_query_params.copy()
        if content_type:
            params.update({'content_type': content_type})
        return params

    @pytest.mark.parametrize('result', mocks.vh_card_detail)
    @patch('smarttv.droideka.proxy.result_builder.ContentDetailV4ResultBuilder.get_result')
    def test_card_detail_has_player_id_vh(self, mock_method, result):
        mock_method.return_value = result
        response = client.get(
            path='/api/v6/card_detail',
            data=self.base_card_detail_query_params,
            **required_headers
        )

        assert json.loads(response.content)[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.VH_PLAYER

    @pytest.mark.parametrize('content_type, result', mocks.kinopoisk_card_detail)
    @patch('smarttv.droideka.proxy.result_builder.ContentDetailV4ResultBuilder.get_result')
    def test_card_detail_has_player_id_kinoposik(self, mock_method, content_type, result):
        mock_method.return_value = result
        response = client.get(
            path='/api/v6/card_detail',
            data=self._get_result_query_params(content_type),
            **required_headers
        )

        assert json.loads(response.content)[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.KINOPOISK_PLAYER

    @pytest.mark.parametrize('content_type, result', mocks.ott_card_details)
    @patch('smarttv.droideka.proxy.result_builder.ContentDetailV4ResultBuilder.get_result')
    def test_card_detail_has_player_id_ott(self, mock_method, content_type, result):
        mock_method.return_value = result
        response = client.get(
            path='/api/v6/card_detail',
            data=self._get_result_query_params(content_type),
            **required_headers
        )
        assert json.loads(response.content)[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.OTT_PLAYER

    @pytest.mark.parametrize('content_type, result', mocks.web_card_details)
    @patch('smarttv.droideka.proxy.result_builder.ContentDetailV4ResultBuilder.get_result')
    def test_card_detail_has_player_id_web(self, mock_method, content_type, result):
        mock_method.return_value = result
        response = client.get(
            path='/api/v6/card_detail',
            data=self._get_result_query_params(content_type),
            **required_headers
        )
        assert json.loads(response.content)[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.WEB_VIEW_PLAYER

    @pytest.mark.parametrize('result', mocks.kinopoisk_thin_card_detail)
    @patch('smarttv.droideka.proxy.result_builder.ContentDetailV4ResultBuilder.get_result')
    def test_thin_card_detail_kinopoisk(self, mock_method, result):
        mock_method.return_value = result
        response = client.get(
            path='/api/v6/card_detail/thin',
            data=self.base_card_detail_query_params,
            **required_headers
        )
        assert json.loads(response.content)[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.KINOPOISK_PLAYER

    @pytest.mark.parametrize('result', mocks.vh_thin_card_detail)
    @patch('smarttv.droideka.proxy.result_builder.ContentDetailV4ResultBuilder.get_result')
    def test_thin_card_detail_vh(self, mock_method, result):
        mock_method.return_value = result
        response = client.get(
            path='/api/v6/card_detail/thin',
            data=self.base_card_detail_query_params,
            **required_headers
        )
        assert json.loads(response.content)[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.VH_PLAYER

    @pytest.mark.parametrize('result', mocks.ott_thin_card_detail)
    @patch('smarttv.droideka.proxy.result_builder.ContentDetailV4ResultBuilder.get_result')
    def test_thin_card_detail_ott(self, mock_method, result):
        mock_method.return_value = result
        response = client.get(
            path='/api/v6/card_detail/thin',
            data=self.base_card_detail_query_params,
            **required_headers
        )
        assert json.loads(response.content)[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.OTT_PLAYER

    @pytest.mark.parametrize('result', mocks.web_thin_card_detail)
    @patch('smarttv.droideka.proxy.result_builder.ContentDetailV4ResultBuilder.get_result')
    def test_thin_card_detail_web(self, mock_method, result):
        mock_method.return_value = result
        response = client.get(
            path='/api/v6/card_detail/thin',
            data=self.base_card_detail_query_params,
            **required_headers
        )
        assert json.loads(response.content)[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.WEB_VIEW_PLAYER

    @pytest.mark.parametrize('result', mocks.series_episodes_kinopoisk)
    @patch('smarttv.droideka.proxy.result_builder.SeriesEpisodesV6ResultBuilder.get_result')
    def test_series_episodes_kinopoisk(self, mock_method, result):
        mock_method.return_value = result
        response = client.get(
            path='/api/v6/series/episodes',
            data=self.base_series_episodes_query_params,
            **required_headers
        )

        result = json.loads(response.content)
        assert result['episodes'][0][PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.KINOPOISK_PLAYER

    @pytest.mark.parametrize('result', mocks.series_episodes_ott)
    @patch('smarttv.droideka.proxy.result_builder.SeriesEpisodesV6ResultBuilder.get_result')
    def test_series_episodes_ott(self, mock_method, result):
        mock_method.return_value = result
        response = client.get(
            path='/api/v6/series/episodes',
            data=self.base_series_episodes_query_params,
            **required_headers
        )

        result = json.loads(response.content)
        assert result['episodes'][0][PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.OTT_PLAYER


class TestSearchPlayerIdFiller:

    mocks = predefined_mock.SearchPlayerIdFillerTestData()

    @pytest.mark.parametrize('clip', mocks.web_clips)
    def test_web_player_id_for_clips(self, clip):
        assert PlayerDetector.KEY_PLAYER_ID not in clip
        SearchRequestPlayerIdFiller.fill_player_id_for_clips([clip])
        assert clip[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.WEB_VIEW_PLAYER

    @pytest.mark.parametrize('clip', mocks.ott_clips)
    def test_ott_player_id_for_clips(self, clip):
        assert PlayerDetector.KEY_PLAYER_ID not in clip
        SearchRequestPlayerIdFiller.fill_player_id_for_clips([clip])
        assert clip[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.OTT_PLAYER

    @pytest.mark.parametrize('assoc', mocks.ott_assoc)
    def test_assoc_player_id_ott(self, assoc):
        assert PlayerDetector.KEY_PLAYER_ID not in assoc
        SearchRequestPlayerIdFiller.fill_player_id_for_assocs([assoc])
        assert assoc[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.OTT_PLAYER

    @pytest.mark.parametrize('assoc', mocks.kinopoisk_assoc)
    def test_assoc_player_id_kinopoisk(self, assoc):
        assert PlayerDetector.KEY_PLAYER_ID not in assoc
        SearchRequestPlayerIdFiller.fill_player_id_for_assocs([assoc])
        assert assoc[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.KINOPOISK_PLAYER

    @pytest.mark.parametrize('parent_collection_obj', mocks.kinopoisk_parent_collection)
    def test_parent_collection_player_id_kinopoisk(self, parent_collection_obj):
        assert PlayerDetector.KEY_PLAYER_ID not in parent_collection_obj
        SearchRequestPlayerIdFiller.fill_player_id_for_parent_collection([parent_collection_obj])
        assert parent_collection_obj[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.KINOPOISK_PLAYER

    @pytest.mark.parametrize('parent_collection_obj', mocks.ott_parent_collection)
    def test_parent_collection_player_id_ott(self, parent_collection_obj):
        assert PlayerDetector.KEY_PLAYER_ID not in parent_collection_obj
        SearchRequestPlayerIdFiller.fill_player_id_for_parent_collection([parent_collection_obj])
        assert parent_collection_obj[PlayerDetector.KEY_PLAYER_ID] == PlayerDetector.OTT_PLAYER
