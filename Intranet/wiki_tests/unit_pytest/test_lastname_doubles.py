import pprint
import pytest


@pytest.mark.django_db
def test_authors(client, wiki_users, page_cluster):
    # Из директории приходит first_name = last_name = "first_name last_name"
    name = 'Александр Покатилов Ибн Вики'
    wiki_users.thasonic.staff.first_name = name
    wiki_users.thasonic.staff.last_name = name
    wiki_users.thasonic.staff.save()

    client.login(wiki_users.thasonic)
    resp = client.get('/_api/frontend/root')
    pprint.pprint(resp.json()['data']['authors'][0]['display'] == name)
