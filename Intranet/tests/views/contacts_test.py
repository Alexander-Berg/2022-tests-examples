import json
import pytest
import factory

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory
from staff.person.models import VALIDATION_TYPES


class ContactTypeFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'person.ContactType'
    url_pattern = '%s'


@pytest.mark.django_db
def test_contacts_returns_is_private(client):
    test_person = StaffFactory(
        login=settings.AUTH_TEST_USER,
        home_email='init@home.ru',
        jabber='init@jabber.com',
        icq='111111',
        login_skype='init',
        login_lj='http://lj.com/init',
        home_page='http://init.ru',
    )

    ct_email = ContactTypeFactory(id=1, validation_type=VALIDATION_TYPES.EMAIL)

    data = {
        'contacts': [
            {
                'id': '',
                'contact_type': str(ct_email.id),
                'account_id': 'test@test.com',
                'private': 'true',
            }
        ]
    }

    response = client.post(
        reverse('profile:edit-contacts', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 200, response.content

    response = client.get(
        reverse('profile:contacts', kwargs={'login': test_person.login})
    )

    answer = json.loads(response.content)

    assert 'private' in answer['target']['contacts'][0]
