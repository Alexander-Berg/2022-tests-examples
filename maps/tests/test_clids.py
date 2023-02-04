from maps.analyzer.pylibs.common.clids import get_user_clids_names, get_taxi_clids_names, get_auto_clids_names


def test_clids_names():
    users_clids_names = [
        'ru.yandex.yandexmaps', 'ru.yandex.traffic', 'ru.yandex.yandexnavi', 'ru.yandex.mobile.navigator'
    ]
    taxi_clids_names = [
        'ru.azerbaijan.taximeter', 'ru.yandex.taximeter', 'ru.yandex.taximeter.x', 'com.yango.driver'
    ]
    auto_clids_names = ['yandex.auto']
    assert sorted(get_user_clids_names()) == sorted(users_clids_names)
    assert sorted(get_taxi_clids_names()) == sorted(taxi_clids_names)
    assert sorted(get_auto_clids_names()) == sorted(auto_clids_names)
