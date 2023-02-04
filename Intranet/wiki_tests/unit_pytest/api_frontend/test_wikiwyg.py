import json

import pytest
from wiki.pages.models import Page


@pytest.mark.django_db
def test_create_simple_page(client, wiki_users, api_url):
    demo_json = {'widgets': [{'type': 'image'}]}
    demo_json_payload = json.dumps(demo_json)
    page_data = {'page_type': 'W', 'title': 'MegaTitle', 'body': demo_json_payload}

    tag = 'fancywikipage'
    client.login('thasonic')
    request_url = f'{api_url}/{tag}'
    response = client.post(request_url, data=page_data)
    assert response.status_code == 200

    page = Page.active.filter(supertag=tag).get()
    assert page.page_type == Page.TYPES.WYSIWYG
    assert page.tag == tag
