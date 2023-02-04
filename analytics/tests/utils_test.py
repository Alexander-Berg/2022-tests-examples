from analytics.plotter_lib.utils import parse_urlencoded
from analytics.collections.plotter_collections.plots.utils import (
    mongo_id_to_timestamp,
    mongo_id_to_datestr,
    get_card_id_from_url,
    get_user_login_board_slug_from_url,
)
from analytics.collections.plotter_collections.plots.prep_users import get_user_type


class Test_mongo_id():
    def test_id_to_timestamp(self):
        assert 1571314473 == mongo_id_to_timestamp("5da85b296a257ccae67fbbaf")

    def test_id_to_datetime(self):
        assert '2019-10-17' == mongo_id_to_datestr("5da85b296a257ccae67fbbaf")


class Test_card_id_from_url():
    def test_case1(self):
        assert get_card_id_from_url('https://yandex.ru/collections/card/5da85b296a257ccae67fbbaf/?qwe') == '5da85b296a257ccae67fbbaf'

    def test_case2(self):
        assert get_card_id_from_url('https://yandex.com/collections/card/erwerww12_3/?qwe') == 'erwerww12_3'

    def test_case3(self):
        assert get_card_id_from_url('https://yandex.ru/collections/card/erwerww12_3?qwe') == 'erwerww12_3'

    def test_case4(self):
        url = 'https://yandex.ru/collections/card/_url%2Fimage%2FYWVmNJNzM3NWU0NjZ8NzUwNzI0NTg0MjYyMzM2MjE3Mw%3D%3D/'
        card_id = parse_urlencoded('_url%2Fimage%2FYWVmNJNzM3NWU0NjZ8NzUwNzI0NTg0MjYyMzM2MjE3Mw%3D%3D')
        assert get_card_id_from_url(url) == card_id


class Test_user_login_board_slug_from_url():
    def test_case1(self):
        assert get_user_login_board_slug_from_url('https://yandex.ru/collections/user/user_1/board_1/werw') == ('user_1', 'board_1')

    def test_case2(self):
        assert get_user_login_board_slug_from_url('https://yandex.com/collections/user/user_2/board_2?qweqw') == ('user_2', 'board_2')

    def test_case3(self):
        assert get_user_login_board_slug_from_url('https://yandex.ru/collections/user/user_3')[0] == 'user_3'

    def test_case4(self):
        assert get_user_login_board_slug_from_url('https://yandex.ru/collections/user/user_3')[1] is None

    def test_case5(self):
        assert get_user_login_board_slug_from_url('https://yandex.ru/collections/user/')[0] is None


class Test_get_user_type():
    def test_case1(self):
        assert get_user_type(None, [], None) == 'organic'
        assert get_user_type(None, ['km', 'fotki'], None) == 'km'
        assert get_user_type(None, ['km', 'business'], None) == 'km'
        assert get_user_type(None, ['verified', 'business'], None) == 'company'
        assert get_user_type(None, ['fotki'], '5da85b296a257ccae67fbbaf') == 'toloker'
        assert get_user_type('redactor', ['fotki'], '5da85b296a257ccae67fbbaf') == 'redactor'
