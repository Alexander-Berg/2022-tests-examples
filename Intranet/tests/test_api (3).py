from decimal import Decimal
from json import loads

import pytest
from django.core.urlresolvers import reverse

from staff.kudos.controllers import AddKudoController
from staff.lib.testing import StaffFactory
from .factories.model import StatFactory, LogFactory


def preprocess_response(response):

    data = loads(response.content)

    if data['success']:
        assert 'errors' not in data
    else:
        assert 'errors' in data

    return data


class TestApiAdd(object):
    """Проверки, связанные с начислением благодарностей."""

    def get_response(self, client, json):
        return preprocess_response(client.post(reverse('kudos:add'), json, content_type='application/json'))

    @pytest.mark.django_db()
    def test_recipients_filled(self, client):
        # Не заполнены получатели благодарностей.
        response = self.get_response(client, '{}')
        assert not response['success']
        assert len(response['errors']) == 1
        assert response['errors'][0] == "Field 'users' containing users login list is mandatory"

    @pytest.mark.django_db()
    def test_recipients_bypass_unknown(self, client):
        # В получателях не список. В получателях неизвестный пользователь. Просто пропустим.
        response = self.get_response(client, '{"users": "bogus"}')
        assert response['success']

    @pytest.mark.django_db()
    def test_recipients_bypass_wrong(self, client, bootstrap_users):
        tester, taker1, taker2 = bootstrap_users()

        StaffFactory(login='udismissed', is_dismissed=True)

        # В получателях уволенный пользователь. В получателях дающий. Просто пропустим.
        response = self.get_response(client, '{"users": ["udismissed", "tester"]}')
        assert response['success']
        assert not tester.kudos_taken.exists()

    @pytest.mark.django_db()
    def test_recipients_division(self, client, bootstrap_users):
        tester, taker1, taker2 = bootstrap_users()

        # Выдаёт от тесте благодарность двоим, которая будет поделена.
        response = self.get_response(
            client, '{"users": ["taker1", "taker2"], "message": "well done!", "notify": true}')

        assert response['success']

        taker1_taken = taker1.kudos_taken.all()
        assert len(taker1_taken) == 1
        assert taker1_taken[0].message == 'well done!'
        assert taker1.kudos.score == Decimal('0.5000')
        assert taker2.kudos.score == Decimal('0.5000')

    @pytest.mark.django_db()
    def test_delay(self, client, bootstrap_users):
        tester, taker1, taker2 = bootstrap_users()

        response = self.get_response(client, '{"users": ["taker1"]}')
        assert response['success']

        response = self.get_response(client, '{"users": ["taker1"]}')
        assert response['success']

        assert taker1.kudos_given.count() == 0
        # Вторая благодарность не зачислится, потому что сработает отсрочка.
        assert taker1.kudos_taken.count() == 1

    @pytest.mark.django_db()
    def test_kudos_accumulated(self, client, bootstrap_users):
        tester, taker1, taker2 = bootstrap_users()

        # Тестеру кто-то отсыпал благодарностей, сила увеличилась.
        StatFactory(person=tester)
        tester.kudos.score = Decimal('1.5')
        tester.kudos.save()

        response = self.get_response(client, '{"users": ["taker1"]}')
        assert response['success']

        assert taker1.kudos_taken.count() == 1
        assert taker1.kudos.score == Decimal('1.5850')
        assert tester.kudos_given.count() == 1

    @pytest.mark.django_db()
    def test_kudos_sum_diff(self, client, bootstrap_users):
        tester, taker1, taker2 = bootstrap_users()

        StatFactory(person=taker1, score=Decimal('1.25'))
        StatFactory(person=taker2, score=Decimal('0.1'))

        # Проверим различные суммы начисления двоим.
        response = self.get_response(client, '{"users": ["taker1", "taker2"]}')
        assert response['success']

        taker1.kudos.refresh_from_db()
        taker2.kudos.refresh_from_db()
        assert taker1.kudos.score == Decimal('1.7500')
        assert taker2.kudos.score == Decimal('0.6000')


class TestApiView(object):
    """Проверки, связанные с просмотром благодарностей."""

    def get_response(self, client, users):
        return preprocess_response(client.get(reverse('kudos:view') + '?' + users))

    @pytest.mark.django_db()
    def test_users_filled(self, client):
        # Не заполнены пользователи.
        response = self.get_response(client, '')
        assert not response['success']
        assert len(response['errors']) == 1
        assert response['errors'][0] == "Field 'users' containing users login list is mandatory"

    @pytest.mark.django_db()
    def test_users_bypass_wrong(self, client):
        # Срока вместо списка.
        response = self.get_response(client, 'users=tester')
        assert response['success']
        assert response['data']['stats']['tester']['score'] == '0'

    @pytest.mark.django_db()
    def test_positive(self, client, bootstrap_users):
        tester, _, _ = bootstrap_users()

        AddKudoController.add_kudo(issuer=tester, recipients=['taker1'])

        # Пробелы в параметрах. И проверка присутствия всех запрошенных в ответе.
        response = self.get_response(client, 'users=[tester, taker1,taker2]')
        assert response['success']
        stats = response['data']['stats']
        assert stats['tester']['score'] == '0'
        assert stats['taker1']['score'] == '1.0000'
        assert stats['taker2']['score'] == '0'

    @pytest.mark.django_db()
    def test_positive_journals(self, client, bootstrap_users):
        tester, taker1, _ = bootstrap_users()

        LogFactory(issuer=tester, recipient=taker1, power=Decimal('0.2'))
        LogFactory(issuer=tester, recipient=taker1, power=Decimal('0.3'))

        # Подвязка данных журнала.
        response = self.get_response(client, 'users=[tester,taker1]&logTaken=true&logGiven=true')
        assert response['success']
        stats = response['data']['stats']
        assert len(stats['taker1']['logGiven']) == 0
        log_taken = stats['taker1']['logTaken']
        assert len(log_taken) == 2
        assert log_taken[0]['issuer'] == 'tester'
        assert log_taken[0]['power'] == '0.3000'
        assert len(stats['tester']['logGiven']) == 2
        assert len(stats['tester']['logTaken']) == 0

    @pytest.mark.django_db()
    def test_positive_available(self, client, bootstrap_users):
        tester, taker1, _ = bootstrap_users()

        LogFactory(issuer=tester, recipient=taker1, power=Decimal('0.2'))

        # Подвязка данных доступности благодарения.
        response = self.get_response(client, 'users=[tester,taker1,taker2]&available=true')
        assert response['data']['available'] == ['taker2']
