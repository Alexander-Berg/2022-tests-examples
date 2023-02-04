import pytest

from ujson import loads

pytestmark = [
    pytest.mark.django_db,
]

page_body = '''=== Глава один
текст 1
==== Раздел 1
==== Раздел 2
=== Глава два
текст 2
==== Раздел 1
=== Глава три
текст 3
==== Раздел 1
'''


@pytest.mark.parametrize('test_page', [{'page_body': page_body}], indirect=True)
class TestPageSection:
    def test_get_first_page_section(self, client, test_page, wiki_users, api_url):
        client.login('thasonic')
        response = client.get(f'{api_url}/{test_page.supertag}/.raw?section_id=1')
        assert response.status_code == 200
        raw_data = loads(response.content)['data']
        expected_body = '''=== Глава один
текст 1
==== Раздел 1
==== Раздел 2
'''
        assert raw_data['body'] == expected_body

    def test_get_page_section(self, client, test_page, wiki_users, api_url):
        client.login('thasonic')
        response = client.get(f'{api_url}/{test_page.supertag}/.raw?section_id=5')
        assert response.status_code == 200
        raw_data = loads(response.content)['data']
        expected_body = '==== Раздел 1\n'
        assert raw_data['body'] == expected_body

    def test_edit_page_section(self, client, test_page, wiki_users, api_url):
        section_text = '=== Новая глава два\ntext 2'

        page_data = {
            'body': section_text,
            'section_id': '4',
        }
        client.login('thasonic')
        response = client.post(f'{api_url}/{test_page.supertag}', data=page_data)
        assert response.status_code == 200
        page_data = loads(response.content)['data']
        expected_body = '''=== Глава один
текст 1
==== Раздел 1
==== Раздел 2
=== Новая глава два
text 2
=== Глава три
текст 3
==== Раздел 1
'''
        assert page_data['body'] == expected_body

    def test_edit_last_section(self, client, test_page, wiki_users, api_url):
        section_text = '==== Глава три раздел 1\n.'

        page_data = {
            'body': section_text,
            'section_id': '7',
        }
        client.login('thasonic')
        response = client.post(f'{api_url}/{test_page.supertag}', data=page_data)
        assert response.status_code == 200
        page_data = loads(response.content)['data']
        expected_body = '''=== Глава один
текст 1
==== Раздел 1
==== Раздел 2
=== Глава два
текст 2
==== Раздел 1
=== Глава три
текст 3
==== Глава три раздел 1
.
'''
        assert page_data['body'] == expected_body

    def test_edit_nonexistent_section(self, client, test_page, wiki_users, api_url):
        page_data = {
            'body': '...........',
            'section_id': '999',
        }
        client.login('thasonic')
        response = client.post(f'{api_url}/{test_page.supertag}', data=page_data)
        assert response.status_code == 200
        page_data = loads(response.content)['data']

        # Нет секции - нет изменений
        assert page_data['body'] == page_body
