import unittest

from analytics.collections.plotter_collections.plots.all_services_shows import get_content_freshness_threshhold
from analytics.collections.plotter_collections.plots.utils import get_card_id_from_url, get_user_login_board_slug_from_url
from analytics.collections.plotter_collections.plots.collections_shows import extract_track_objects_item_data


def test_get_card_id_from_url():
    assert '123' == get_card_id_from_url('https://yandex.ru/collections/card/123')
    assert '5b4465bebfe06a5539b55a45' == get_card_id_from_url('https://yandex.ru/collections/card/5b4465bebfe06a5539b55a45/')
    assert '5b4465bebfe06a5539b55a45' == get_card_id_from_url('https://yandex.ru/collections/card/5b4465bebfe06a5539b55a45')
    assert get_card_id_from_url('https://yandex.ru/collections/user/alena-antipenko/vybor-fotokamery-dlia-nachinaiushchego-fotografa/') is None


def test_get_image_card_id_from_url():
    url1 = 'https://yandex.ru/collections/card/_url%2Fimage%2FM2Y2NzQ1ZjctNDI3MDU5ZGItZjRjMTM4ZjEtODY4MTRkYjl8ZTk2ZWUzYzVmNjViZjMxZjQ0N2JhYWVhY2EwZmMyYWN8NDYyNTU1NzE4NTUwNDAzNTk1OA%3D%3D/'
    assert get_card_id_from_url(url1) == '_url/image/M2Y2NzQ1ZjctNDI3MDU5ZGItZjRjMTM4ZjEtODY4MTRkYjl8ZTk2ZWUzYzVmNjViZjMxZjQ0N2JhYWVhY2EwZmMyYWN8NDYyNTU1NzE4NTUwNDAzNTk1OA=='


def test_get_user_login_board_slug_from_url():
    assert ('NNN', None) == get_user_login_board_slug_from_url('https://yandex.ru/collections/user/NNN/')
    assert ('NNN', None) == get_user_login_board_slug_from_url('https://yandex.ru/collections/user/NNN')
    assert ('NNN', 'board') == get_user_login_board_slug_from_url('https://yandex.ru/collections/user/NNN/board/')
    assert ('NNN', 'board') == get_user_login_board_slug_from_url('https://yandex.ru/collections/user/NNN/board')
    assert ('NNN', 'board') == get_user_login_board_slug_from_url('https://yandex.ru/collections/user/NNN/board/?qwe=qwe')


class TestExtractItemData(unittest.TestCase):
    def test_basic(self):
        source = {'id': '5df386a8874e97ed6c7df7f6'}
        result = {'card_id': '5df386a8874e97ed6c7df7f6', 'content_type': 'card'}
        self.assertDictEqual(extract_track_objects_item_data(source), result)

    def test_position(self):
        source = {'id': '5df386a8874e97ed6c7df7f6', 'pos': 11}
        result = {'card_id': '5df386a8874e97ed6c7df7f6', 'content_type': 'card', 'content_position': 11}
        self.assertDictEqual(extract_track_objects_item_data(source), result)

    def test_empty(self):
        source = {'id': 'incut-15', 'pos': 11}
        assert extract_track_objects_item_data(source) is None

        source = {'id': 'feedRecommendations/incut-2', 'type': 'feedRecommendations'}
        assert extract_track_objects_item_data(source) is None

        source = {'id': 'user_recent', 'type': 'userCards'}
        assert extract_track_objects_item_data(source) is None

        source = {'id': 'auth_cards', 'type': 'user'}
        assert extract_track_objects_item_data(source) is None

        source = {'id': 'auth_likes', 'type': 'user'}
        assert extract_track_objects_item_data(source) is None

        source = {'id': 'incut-1', 'type': 'editorsChoice'}
        assert extract_track_objects_item_data(source) is None

        source = {'id': 'adv', 'type': 'advCards'}
        assert extract_track_objects_item_data(source) is None

    def is_subtype_matches_to_type(self, subtype, content_type):
        source = {'id': 'NNNNNNNNNNNNNNNNNNNNNNNN', 'type': subtype}

        if content_type == 'card':
            result = {'card_id': 'NNNNNNNNNNNNNNNNNNNNNNNN', 'content_type': content_type, 'content_subtype': subtype}
            self.assertDictEqual(extract_track_objects_item_data(source), result)
        elif content_type == 'board':
            result = {'board_id': 'NNNNNNNNNNNNNNNNNNNNNNNN', 'content_type': content_type, 'content_subtype': subtype}
            self.assertDictEqual(extract_track_objects_item_data(source), result)
        elif content_type is None:
            assert extract_track_objects_item_data(source) is None

    def test_cards_subtype(self):
        self.is_subtype_matches_to_type('feedCards', 'card')
        self.is_subtype_matches_to_type('boardCards', 'card')
        self.is_subtype_matches_to_type('viewerBoardCards', 'card')
        self.is_subtype_matches_to_type('channelCards', 'card')
        self.is_subtype_matches_to_type('searchCards', 'card')
        self.is_subtype_matches_to_type('cardSimilar', 'card')
        self.is_subtype_matches_to_type('cardSimilarToCard', 'card')
        self.is_subtype_matches_to_type('searchCards', 'card')
        self.is_subtype_matches_to_type('ballotsCards', 'card')

    def test_boards_subtype(self):
        self.is_subtype_matches_to_type('boardRecommendations', 'board')
        self.is_subtype_matches_to_type('boardSimilar', 'board')
        self.is_subtype_matches_to_type('boardBoards', 'board')
        self.is_subtype_matches_to_type('userBoards', 'board')
        self.is_subtype_matches_to_type('userBoardsSidebar', 'board')
        self.is_subtype_matches_to_type('searchBoards', 'board')
        self.is_subtype_matches_to_type('boardsRecommendations', 'board')
        self.is_subtype_matches_to_type('feedBoards', 'board')

    def test_none_subtype(self):
        self.is_subtype_matches_to_type('advCards', None)
        self.is_subtype_matches_to_type('userCards', None)
        self.is_subtype_matches_to_type('user', None)
        self.is_subtype_matches_to_type('channelRelated', None)
        self.is_subtype_matches_to_type('favoritesCards', None)
        self.is_subtype_matches_to_type('editorsChoice', None)

    def test_subtype(self):
        source = {'id': '5a9d4028cff35f35426e7d4b', 'type': 'feedBoards', 'pos': 8}
        result = {'board_id': '5a9d4028cff35f35426e7d4b', 'content_type': 'board', 'content_position': 8, 'content_subtype': 'feedBoards'}
        self.assertDictEqual(extract_track_objects_item_data(source), result)

        source = {'id': '5a9d4028cff35f35426e7d4b', 'type': 'BoardSimilar'}
        result = {'board_id': '5a9d4028cff35f35426e7d4b', 'content_type': 'board', 'content_subtype': 'BoardSimilar'}
        self.assertDictEqual(extract_track_objects_item_data(source), result)

        source = {'id': 'footer/5b3b348424e06c77b92d87f1', 'type': 'footerBoardSimilar'}
        result = {'board_id': '5b3b348424e06c77b92d87f1', 'content_type': 'board', 'content_subtype': 'footerBoardSimilar'}
        self.assertDictEqual(extract_track_objects_item_data(source), result)


def test_get_content_freshness_threshhold():
    assert get_content_freshness_threshhold('2019-01-05', '2019-01-01') == '<=1w'
    assert get_content_freshness_threshhold('2019-01-10', '2019-01-01') == '<=2w'
    assert get_content_freshness_threshhold('2019-01-30', '2019-01-01') == '<=1m'
    assert get_content_freshness_threshhold('2019-03-10', '2019-01-01') == '<=3m'
    assert get_content_freshness_threshhold('2019-05-10', '2019-01-01') == '<=6m'
    assert get_content_freshness_threshhold('2019-10-10', '2019-01-01') == '<=1y'
    assert get_content_freshness_threshhold('2020-10-10', '2019-01-01') == '>1y'
