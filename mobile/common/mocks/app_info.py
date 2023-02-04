APP_NOT_FOUND = 'some_app_not_found'
APP_FOUND = 'some_found_app'


def get_answer(package_name):
    return dict(
        package_name=package_name,
        name=u'App title',
        description=u'app description',
        rating=4.55,
        rating_count=9000,
        icon_url=u'http://yandex.ru',
        file_size=250000,
        categories=[u'PERSONALIZATION'],
        downloads=1000000,
        adult=u'Everyone',
        is_free=True,
        screenshots=[
            u'http://avatars.mds.yandex.net/get-google-play-app-screens/119703/aebd4f3bbe76cdb3f7b6b7ffabe73266/orig',
            u'http://avatars.mds.yandex.net/get-google-play-app-screens/43519/10cd98dc191cb3a12a5db9d781fd4459/orig',
        ],
        screenshots_info=[
            {u'meta': {u'orig-size': {u'x': 1920, u'y': 1080}}},
            {u'meta': {u'orig-size': {u'x': 768, u'y': 1024}}},
        ],
        genres=[u'Personalization'],
        publisher=u'publisher',
        html_description=u'app description',
        release_date=1491512400,
        content_rating='12+',
        short_title='app',
    )


def dyntable_lookup_mock(self, table, input_stream, format):
    for item in input_stream:
        package_name = item['package_name']
        if package_name != APP_NOT_FOUND:
            answer = get_answer(package_name)
            answer['language'] = item['language']
            yield answer


def get_dyntable_lookup_mock(answer_update_func=None):
    def lookup_mock(*args, **kwargs):
        for answer in dyntable_lookup_mock(*args, **kwargs):
            answer.update(answer_update_func(answer['package_name'], answer['language']))
            yield answer

    return lookup_mock
