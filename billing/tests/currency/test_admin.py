import pytest


def decode(response):
    return response.content.decode()


class TestAdmin:

    def get_reponse(self, url, client):
        return decode(client.get(url, follow=True))

    @pytest.mark.django_db
    def test_syncmodel(self, admin_client, monkeypatch):
        url = '/admin/currency/syncmodel/'

        response = self.get_reponse(url, admin_client)
        assert 'Source ID:' in response

        response = decode(admin_client.post(url, data={
            'source': 'EUR',
            'date': '2020-03-03',
        }, follow=True))

        assert 'Источник EUR не поддерживает получение данных на определённую дату.' in response

        was_run = []

        def run_mock(*args, params=None, **kwargs):
            was_run.append(params)

        monkeypatch.setattr('refs.currency.admin.run_synchronizers', run_mock)

        response = decode(admin_client.post(url, data={
            'source': 'RUS',
            'date': '2020-03-03',
        }, follow=True))

        assert 'Задача выполнена' in response
        assert was_run == [{'scrapers': 'RUS', 'date': '2020-03-03'}]
