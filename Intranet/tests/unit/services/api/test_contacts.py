from functools import partial

import pretend
import pytest
from django.core.urlresolvers import reverse

from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def get_contacts(client):
    return partial(
        client.json.get,
        reverse('services-api:contact-list')
    )


@pytest.fixture
def data(db):
    service = factories.ServiceFactory()
    contact_type = factories.ContactTypeFactory(
        code='url_sitelink',
        name='Тип контакта',
        name_en='Contact type',
    )
    contact1 = factories.ServiceContactFactory(
        type=contact_type,
        service=service,
        content='foo'
    )
    contact2 = factories.ServiceContactFactory(
        type=contact_type,
        service=service,
        content='bar'
    )
    contact3 = factories.ServiceContactFactory(
        content='xxx'
    )
    return pretend.stub(
        contact_type=contact_type,
        service=service,
        contact1=contact1,
        contact2=contact2,
        contact3=contact3,
    )


def test_filter_by_service(get_contacts, data):
    response = get_contacts({'service': data.service.pk})

    json = response.json()
    assert len(json['results']) == 2

    service2 = factories.ServiceFactory()
    response = get_contacts({'service': [data.service.pk, service2.pk]})

    json = response.json()
    assert len(json['results']) == 2

    service2 = factories.ServiceFactory()
    response = get_contacts({'service__in': '%s,%s' % (data.service.pk, service2.pk)})

    json = response.json()
    assert len(json['results']) == 2


def test_filter_by_service_slug(get_contacts, data):
    response = get_contacts({'service__slug': data.service.slug})

    json = response.json()
    assert len(json['results']) == 2

    service2 = factories.ServiceFactory()
    response = get_contacts({'service__slug__in': '%s,%s' % (data.service.slug, service2.slug)})

    json = response.json()
    assert len(json['results']) == 2


@pytest.mark.skip
def test_filter_by_service_slug_is_case_insensitive(get_contacts, data):
    response = get_contacts({'service__slug': data.service.slug.upper()})

    json = response.json()
    assert len(json['results']) == 2


def test_filter_by_absent_content(get_contacts, data):
    response = get_contacts({'content__contains': 'hello/world'})

    assert response.status_code == 200
    json = response.json()
    assert not json['results']


def test_filter_by_content(get_contacts, data):
    response = get_contacts({'content__contains': 'foo'})

    json = response.json()
    assert len(json['results']) == 1
    assert json['results'][0]['content'] == 'foo'
    assert json['results'][0]['type']['name']['ru'] == 'Тип контакта'


def test_filter_by_contact_type(get_contacts, data):
    response = get_contacts({'contact_type': data.contact_type.pk})

    json = response.json()
    assert len(json['results']) == 2

    ct = factories.ContactTypeFactory()
    response = get_contacts({'contact_type': [data.contact_type.pk, ct.pk]})

    json = response.json()
    assert len(json['results']) == 2


def test_filter_by_type(get_contacts, data):
    response = get_contacts({'type__code': data.contact_type.code})

    json = response.json()
    assert len(json['results']) == 2

    response = get_contacts({'type__code': [data.contact_type.code]})

    json = response.json()
    assert len(json['results']) == 2


def test_filter_by_title(get_contacts, data):
    response = get_contacts({'title': data.contact1.title})

    json = response.json()
    assert len(json['results']) == 1

    response = get_contacts({'title__in': '%s,%s' % (data.contact1.title, data.contact2.title)})

    json = response.json()
    assert len(json['results']) == 2

    response = get_contacts({'title__contains': data.contact1.title[-3:]})

    json = response.json()
    assert len(json['results']) == 1


def test_filter_by_exportable(client, get_contacts, data):
    results = get_contacts().json()['results']
    assert len(results) == 3

    results = get_contacts({'service__is_exportable': 'true'}).json()['results']
    assert len(results) == 3

    data.contact3.service.is_exportable = False
    data.contact3.service.save()

    results = get_contacts().json()['results']
    assert len(results) == 2
    assert {results[0]['id'], results[1]['id']} == {data.contact1.id, data.contact2.id}

    # при этом во frontend api логика другая и контакты отдаются
    results = client.json.get(reverse('api-frontend:service-contact-list')).json()['results']
    assert len(results) == 3

    results = get_contacts({'service__is_exportable': 'false'}).json()['results']
    assert len(results) == 1
    assert results[0]['id'] == data.contact3.id

    results = get_contacts({'service__is_exportable__in': 'true,false'}).json()['results']
    assert len(results) == 3


@pytest.mark.parametrize('title', ['smth', {'ru': 'smth'}])
def test_create_contact(client, data, title):
    contact_content = 'some data'
    create_data = {
        'service': data.service.id,
        'type': data.contact_type.id,
        'content': contact_content,
        'title': title,
    }
    response = client.json.post(
        reverse('api-v4:service-contact-list'),
        data=create_data,
    )
    assert response.status_code == 201
    response = response.json()
    assert response['content'] == contact_content
    assert response['type']['code'] == data.contact_type.code
    assert response['title'] == {'ru': 'smth', 'en': ''}


def test_get_url_contacts(client, django_assert_num_queries):
    contact_type = factories.ContactTypeFactory(
        code='tracker',
        name='Тип контакта',
        name_en='Contact type',
        validator='STARTREK',
    )
    for i in range(20):
        factories.ServiceContactFactory(
            type=contact_type,
            content='ABC',
            title='some',
            title_en='some_en'
        )
    with django_assert_num_queries(5):
        # 1 - пользователь из базы
        # 1 - пермишены
        # 1 - запрос за данными контактов
        # 1 - pg_is_in_recovery
        # 1 - waffle_switch
        response = client.json.get(
            reverse('api-v4:service-contact-list'),
            {'fields': 'service,content,type,url,title'}
        )

    assert response.status_code == 200
    data = response.json()['results']
    assert len(data) == 20
    assert data[0]['content'] == 'ABC'
    assert data[0]['url'] == 'https://st.yandex-team.ru/ABC'
    assert data[0]['title'] == {'ru': 'some', 'en': 'some_en'}


def test_get_contacts_types(client, django_assert_num_queries):
    contact_type = factories.ContactTypeFactory(name='some', name_en='some_en')
    with django_assert_num_queries(5):
        # 1 - пользователь из базы
        # 1 - пермишены
        # 1 - запрос за данными контактов
        # 1 - pg_is_in_recovery
        # 1 - waffle_switch
        response = client.json.get(
            reverse('api-frontend:service-contact-type-list'),
        )
    assert response.status_code == 200, response.content
    data = response.json()
    assert len(data) == 1
    assert data[0]['name'] == {
        'en': contact_type.name_en,
        'ru': contact_type.name,
    }
