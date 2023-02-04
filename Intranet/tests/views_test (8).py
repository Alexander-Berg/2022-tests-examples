import pytest
import factory

from json import loads

from staff.django_api_auth.models import Token


class MailListFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'maillists.List'


@pytest.fixture
def ml():
    Token(token='killa', ips='127.0.0.1', hostnames='localhost').save()
    MailListFactory(name='killa', email='killa@yandex-team.ru', info='Oi-oi, killa!', is_open=True)
    MailListFactory(name='gorilla', email='', info='Oi-oi, gorila!', is_open=False)
    MailListFactory(name='godzilla', email=None, info='Bah-bah!', is_open=False)


@pytest.mark.django_db
def test_ViewAutocomleteMaillists(client, ml):
    r = client.get('/center/api/autocomplete/maillists/', {'q': 'ki'})
    assert r.status_code == 403

    r = client.get('/center/api/autocomplete/maillists/', {'q': 'ki'}, HTTP_REFERER='http://chukigek.yandex-team.ru/')
    assert r.status_code, 200
    data = loads(r.content)
    assert len(data) == 1
    assert data[0]['name'], 'killa'
