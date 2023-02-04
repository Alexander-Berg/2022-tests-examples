import pytest
from smarttv.droideka.proxy.avatars_image_generator import AVATARS_MDS_REGEX, fill_images_from_pattern


class TestAvatarsMdsRegex:
    matching_candidates = [
        # https
        {
            'url': 'https://avatars.mds.yandex.net/get-ott/1/2/orig',
            'bucket': 'get-ott',
        },
        {
            'url': 'https://avatars.mds.yandex.net/get-entity_search/1/2/orig',
            'bucket': 'get-ott',
        },
        {
            'url': 'https://avatars.mds.yandex.net/get-vh/1/2/orig',
            'bucket': 'get-ott',
        },
        # http
        {
            'url': 'http://avatars.mds.yandex.net/get-ott/1/2/orig',
            'bucket': 'get-ott',
        },
        {
            'url': 'http://avatars.mds.yandex.net/get-entity_search/1/2/orig',
            'bucket': 'get-ott',
        },
        {
            'url': 'http://avatars.mds.yandex.net/get-vh/1/2/orig',
            'bucket': 'get-ott',
        },
    ]

    @pytest.mark.parametrize('candidate', [item['url'] for item in matching_candidates])
    def test_regex_matches(self, candidate: str):
        match = AVATARS_MDS_REGEX.match(candidate)
        assert match


class TestMusicImagesGenerator:
    def test_https_added(self):
        pattern = 'avatars.yandex.net/get-music-user-playlist/1/2/%%?1617203735672'
        expected = 'https://avatars.yandex.net/get-music-user-playlist/1/2/400x400?1617203735672'
        result = {}
        fill_images_from_pattern(pattern, 'thumbs', result, 'music')
        result['thumbs']['400x400'] == expected
        assert result['thumbs']['400x400'] == expected

    def test_without_percent_sign(self):
        # patterns without percent sign is okay too - we can use it if we need to put
        # music cover stun on s3
        image = 'http://androidtv.s3.yandex.net/music/nocover.png'
        result = {}
        fill_images_from_pattern(image, 'thumbs', result, 'music')
        assert result['thumbs']['400x400'] == image
