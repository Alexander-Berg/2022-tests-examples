import pytest

import bannerland.video_creative


def test_order():
    assert not bannerland.video_creative.check_order(1)
    assert bannerland.video_creative.check_order(171097532)


def test_get_video_no_order():
    with pytest.raises(KeyError):
        bannerland.video_creative.get_creative(1, '')


@pytest.mark.parametrize(
    ('url', 'creative_id'),
    [
        ('', None),
        (',kjhkugjyhtf', None),
        ('https://,kjhkugjyhtf', None),
        ('hps://,kjhkugjyhtf', None),
        ('https://sokolov.ru/jewelry-catalog/product/1021954-3', None),
        ('https://sokolov.ru/jewelry-catalog/product/1021954-3/', 1121335065),
        ('https://sokolov.ru/jewelry-catalog/product/1021954-3/?', 1121335065),
        ('https://sokolov.ru/jewelry-catalog/product/1021954-3/?abc', 1121335065),
        ('https://sokolov.ru/jewelry-catalog/product/1021954-3/?utm_source', 1121335065),
        ('https://sokolov.ru/jewelry-catalog/product/1021954-3/?utm_source=yande', 1121335065),
        ('https://sokolov.ru/jewelry-catalog/product/1021954-3/?utm_source=yandex&utm_medium=cpc&trading_network_id=3d0fdcba-a2f8-11e9-80d4-40f2e9a197bd', 1121335065),
    ]
)
def test_get_video(url, creative_id):
    assert bannerland.video_creative.get_creative(171097532, url) == creative_id
