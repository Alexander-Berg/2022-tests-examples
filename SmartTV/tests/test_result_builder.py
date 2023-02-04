import mock
import pytest

from rest_framework.exceptions import NotFound

from smarttv.droideka.proxy import result_builder


@mock.patch('smarttv.droideka.proxy.data_source.UnboundSeasonsDataSource.get_result', mock.Mock(return_value={}))
@mock.patch('smarttv.droideka.proxy.data_source.UnboundEpisodesDataSource.get_result', mock.Mock(return_value=[]))
@mock.patch('smarttv.droideka.proxy.data_source.UnboundEpisodesDataSource.get_result', mock.Mock(return_value=[]))
class TestUnboundEpisodesResultBuilder:

    fake_request_data = {'season_number': 0, 'backward_count': 0, 'forward_count': 0, 'stop_at_season_boundaries': 0,
                         'series_id': '13', 'pivot_episode_number': 0}

    @mock.patch('smarttv.droideka.utils.chaining.ResultBuilder.get_result', mock.Mock(return_value={
        'series_id': '1',
        'raw_episodes': [{'a': 'b'}]
    }))
    def test_ok(self):
        result = result_builder.UnboundEpisodesResultBuilder(self.fake_request_data).get_result()
        assert [{'a': 'b'}] == result

    @mock.patch('smarttv.droideka.utils.chaining.ResultBuilder.get_result', mock.Mock(return_value={'series_id': '1'}))
    def test_404(self):
        with pytest.raises(NotFound):
            result_builder.UnboundEpisodesResultBuilder(self.fake_request_data).get_result()


class TestKpMultiselectionsResultBuilder:

    @pytest.mark.parametrize('items, expected_ids', [
        ([{'id': '1', 'contentType': 'ott-movie'}], ('1',)),
        ([{'id': '1', 'contentType': 'tv-series'}], ('1',)),
        ([{'id': '1', 'type': 'SELECTION'}], ('1',)),
        ([{'id': '1', 'contentType': 'channel'}], None),
        ([{'id': '1', 'contentType': 'unknown'}], None),
        ([{'id': '1', 'contentType': 'channel'}, {'id': '2', 'contentType': 'ott-movie'}, {'id': '3', 'contentType': 'tv-series'}], ('2', '3')),
        ([{'id': '1', 'contentType': 'unknown'}, {'id': '2', 'contentType': 'ott-movie'}, {'id': '3', 'contentType': 'tv-series'}], ('2', '3')),
        ([], None),
        (None, None),
        ([{}], None),
        ([{'id': '1', 'contentType': 'tv-series'}, {'id': '2', 'contentType': 'tv-series'}, {}], ('1', '2')),
    ])
    def test_filter_unknown_items_filtered_correctly(self, items, expected_ids):
        carousel = result_builder.MultiSelectionsResultBuilder.get_carousel_with_allowed_items_only({'data': items})

        actual_ids = tuple(item['id'] for item in carousel['data']) if carousel else None
        assert expected_ids == actual_ids
