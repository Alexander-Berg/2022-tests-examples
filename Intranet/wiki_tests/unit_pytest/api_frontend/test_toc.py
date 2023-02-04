import pytest

from ujson import loads

pytestmark = [
    pytest.mark.django_db,
]


def test_toc(client, wiki_users, test_page, api_url):
    client.login('thasonic')
    test_page.body = '''
== Глава 1
=== Подглава 1.1
== Глава2
=== Подглава 2.1
== Глава3
=== Подглава 3.1
'''
    test_page.save()
    request_url = f'{api_url}/{test_page.supertag}/.toc'
    response = client.get(request_url)

    assert response.status_code == 200
    response_data = loads(response.content.decode())['data']
    expected = {'toc': {'block': 'wiki-doc',
                   'content': [{'block': 'wiki-toc',
                                'content': [{'block': 'wiki-tocitem',
                                             'wiki-attrs': {'anchor': 'glava1',
                                                            'level': 1,
                                                            'txt': 'Глава 1'}},
                                            {'block': 'wiki-tocitem',
                                             'wiki-attrs': {'anchor': 'podglava1.1',
                                                            'level': 2,
                                                            'txt': 'Подглава 1.1'}},
                                            {'block': 'wiki-tocitem',
                                             'wiki-attrs': {'anchor': 'glava2',
                                                            'level': 1,
                                                            'txt': 'Глава2'}},
                                            {'block': 'wiki-tocitem',
                                             'wiki-attrs': {'anchor': 'podglava2.1',
                                                            'level': 2,
                                                            'txt': 'Подглава 2.1'}},
                                            {'block': 'wiki-tocitem',
                                             'wiki-attrs': {'anchor': 'glava3',
                                                            'level': 1,
                                                            'txt': 'Глава3'}},
                                            {'block': 'wiki-tocitem',
                                             'wiki-attrs': {'anchor': 'podglava3.1',
                                                            'level': 2,
                                                            'txt': 'Подглава 3.1'}}],
                                'wiki-attrs': {'page': '/testpage'}}]
                   }}
    assert expected == response_data
