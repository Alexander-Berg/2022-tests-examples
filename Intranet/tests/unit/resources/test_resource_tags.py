import pretend
import pytest
from django.core.urlresolvers import reverse

from plan.resources import models
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db, owner_role, staff_factory):
    staff = staff_factory()
    stranger = staff_factory()
    service = factories.ServiceFactory(owner=staff)
    factories.ServiceMemberFactory(service=service, role=owner_role, staff=staff)

    category = factories.ResourceTagCategoryFactory()
    tag = factories.ResourceTagFactory(service=service, category=category)

    return pretend.stub(
        staff=staff,
        stranger=stranger,
        service=service,
        category=category,
        tag=tag,
    )


def test_create_tag(client, data):
    client.login(data.staff.login)

    response = client.json.post(
        reverse('resources-api:tag-list'),
        {
            'name': {'ru': 'name', 'en': 'name_en'},
            'slug': 'slug',
            'type': 'external',
            'category': data.category.pk,
            'service': data.service.pk,
            'description': {'ru': 'описание', 'en': 'description'},
        }
    )

    assert response.status_code == 201

    assert models.ResourceTag.objects.count() == 2

    tag = models.ResourceTag.objects.order_by('-pk')[0]
    assert tag.slug == 'slug'
    assert tag.name == 'name'
    assert tag.name_en == 'name_en'
    assert tag.slug == 'slug'
    assert tag.slug == 'slug'
    assert tag.description == 'описание'
    assert tag.description_en == 'description'
    assert tag.service == data.service


def test_create_tag_empty_name(client, data):
    client.login(data.staff.login)

    response = client.json.post(
        reverse('resources-api:tag-list'),
        {
            'name': {'ru': '', 'en': ''},
            'slug': 'slug',
            'type': 'external',
            'service': data.service.pk,
        }
    )

    assert response.status_code == 400

    result = response.json()
    assert result['error']['code'] == 'validation_error'
    assert result['error']['extra']['name'] == ['This field is required.']


def test_create_tag_by_stranger(client, data):
    client.login(data.stranger.login)

    response = client.json.post(
        reverse('resources-api:tag-list'),
        {
            'name': {'ru': 'name', 'en': 'name_en'},
            'slug': 'slug',
            'type': 'external',
            'category': data.category.pk,
            'service': data.service.pk,
        }
    )

    assert response.status_code == 403
    assert response.json()['error']['message']['en'] == (
        'You are not allowed to add resource tags to this service')


def test_permissions_by_owner(client, data):
    client.login(data.staff.login)

    response = client.json.options(
        reverse('resources-api:tag-list'),
        {
            'service': data.service.pk,
        }
    )

    assert response.json() == {'permissions': ['can_create_tags', 'can_edit_tags']}


def test_permissions_by_stranger(client, data):
    client.login(data.stranger.login)

    response = client.json.options(
        reverse('resources-api:tag-list'),
        {
            'service': data.service.pk,
        }
    )

    assert response.json() == {'permissions': []}


def test_get_tag(client):
    tag = factories.ResourceTagFactory()

    response = client.json.get(
        reverse('resources-api:tag-detail', args=[tag.id]),
    )
    assert response.status_code == 200
    result = response.json()

    assert result['id'] == tag.id
    assert result['slug'] == tag.slug
    assert result['description'] == {'ru': tag.description, 'en': ''}


def test_order_by_category(client):
    cat1 = factories.ResourceTagCategoryFactory(order=1)
    cat2 = factories.ResourceTagCategoryFactory(order=3)
    cat3 = factories.ResourceTagCategoryFactory(order=2)

    tag1 = factories.ResourceTagFactory(category=cat1)
    tag2 = factories.ResourceTagFactory(category=cat2)
    tag3 = factories.ResourceTagFactory(category=cat3)

    response = client.json.get(
        reverse('resources-api:tag-list'),
    )
    assert [tag['id'] for tag in response.json()['results']] == [tag1.pk, tag3.pk, tag2.pk]


def check_tags(client, params, tags):
    response = client.json.get(
        reverse('resources-api:tag-list'),
        params,
    )

    assert response.status_code == 200

    expected_ids = {t.id for t in tags}
    real_ids = {t['id'] for t in response.json()['results']}
    assert real_ids == expected_ids


def test_filter_by_name(client):
    tag = factories.ResourceTagFactory()

    check_tags(
        client,
        {'name': tag.name},
        [tag],
    )

    check_tags(
        client,
        {'name': 'xx' + tag.name},
        [],
    )


def test_filter_by_slug(client):
    tag1 = factories.ResourceTagFactory()
    tag2 = factories.ResourceTagFactory()
    tag3 = factories.ResourceTagFactory()

    check_tags(
        client,
        {'slug': tag1.slug},
        [tag1],
    )

    check_tags(
        client,
        {'slug__in': f'{tag2.slug},{tag3.slug}'},
        [tag2, tag3],
    )


def test_filter_by_service(client):
    service1 = factories.ServiceFactory()
    service2 = factories.ServiceFactory()

    tag1 = factories.ResourceTagFactory(service=service1)
    tag2 = factories.ResourceTagFactory()

    check_tags(
        client,
        {'service': service1.pk},
        [tag1],
    )

    check_tags(
        client,
        {'service': service2.pk},
        [],
    )

    check_tags(
        client,
        {'service': (service1.pk, service2.pk)},
        [tag1],
    )

    check_tags(
        client,
        {'service__isnull': True},
        [tag2],
    )


def test_filter_by_hidden(client):
    cat1 = factories.ResourceTagCategoryFactory()
    cat2 = factories.ResourceTagCategoryFactory(is_hidden=True)

    tag1 = factories.ResourceTagFactory(category=cat1)
    tag2 = factories.ResourceTagFactory(category=cat2)

    check_tags(
        client,
        {},
        [tag1],
    )

    check_tags(
        client,
        {'category__is_hidden': True},
        [tag2],
    )

    check_tags(
        client,
        {'category__is_hidden': False},
        [tag1],
    )


def test_tag_with_no_category(client):
    cat1 = factories.ResourceTagCategoryFactory()
    cat2 = factories.ResourceTagCategoryFactory(is_hidden=True)

    shown_tag = factories.ResourceTagFactory(category=cat1)
    hidden_tag = factories.ResourceTagFactory(category=cat2)
    categoryless_tag = factories.ResourceTagFactory(category=None)

    check_tags(
        client,
        {},
        [shown_tag, categoryless_tag]
    )

    check_tags(
        client,
        {'category__is_hidden': False},
        [shown_tag]
    )

    check_tags(
        client,
        {'category__is_hidden': True},
        [hidden_tag]
    )


def test_filter_by_resource_type(client):
    tag = factories.ResourceTagFactory(service=factories.ServiceFactory())
    tag2 = factories.ResourceTagFactory(service=factories.ServiceFactory())
    factories.ResourceTagFactory(service=None)
    resource_type = factories.ResourceTypeFactory(supplier=tag2.service)
    resource_type.tags.add(tag)

    check_tags(
        client,
        {'resource_type': resource_type.id},
        [tag],
    )


def test_filter_by_resource_type_global(client):
    factories.ResourceTagFactory(service=factories.ServiceFactory())
    tag2 = factories.ResourceTagFactory(service=factories.ServiceFactory())
    global_tag = factories.ResourceTagFactory(service=None)
    resource_type = factories.ResourceTypeFactory(supplier=tag2.service)

    check_tags(
        client,
        {'resource_type': resource_type.id},
        [global_tag, tag2],
    )


def test_update_tag(client, data):
    client.login(data.staff.user.username)
    tag_category = factories.ResourceTagCategoryFactory()

    new_data = {
        'slug': 'mynewtag',
        'type': models.ResourceTag.EXTERNAL,
        'name': {'ru': 'Хеллоу ворлд', 'en': 'Hello world'},
        'description': {'ru': 'Тестовый тег', 'en': 'Test tag'},
        'category': tag_category.id
    }

    response = client.json.put(
        reverse('resources-api:tag-detail', args=[data.tag.id]),
        new_data
    )
    assert response.status_code == 200

    data.tag.refresh_from_db()
    assert data.tag.slug == new_data['slug']
    assert data.tag.type == new_data['type']
    assert data.tag.name == new_data['name']['ru']
    assert data.tag.name_en == new_data['name']['en']
    assert data.tag.description == new_data['description']['ru']
    assert data.tag.description_en == new_data['description']['en']
    assert data.tag.category.id == tag_category.id


def test_partial_update_tag(client, data):
    client.login(data.staff.user.username)
    tag_category = factories.ResourceTagCategoryFactory()

    new_data = {
        'name': {'ru': 'Хеллоу ворлд', 'en': 'Hello world'},
        'category': tag_category.id
    }

    response = client.json.patch(
        reverse('resources-api:tag-detail', args=[data.tag.id]),
        new_data
    )
    assert response.status_code == 200

    data.tag.refresh_from_db()
    assert data.tag.name == new_data['name']['ru']
    assert data.tag.name_en == new_data['name']['en']
    assert data.tag.category.id == tag_category.id


def test_drop_category(client, data):
    client.login(data.staff.user.username)
    tag_category = factories.ResourceTagCategoryFactory()
    data.tag.category = tag_category
    data.tag.save()

    response = client.json.patch(
        reverse('resources-api:tag-detail', args=[data.tag.id]),
        {
            'category': None,
        }
    )
    assert response.status_code == 200

    data.tag.refresh_from_db()
    assert data.tag.category is None


def test_update_tag_by_stranger(client, data):
    client.login(data.stranger.user.username)

    response = client.json.patch(
        reverse('resources-api:tag-detail', args=[data.tag.id]),
        {'slug': 'derp'}
    )
    assert response.status_code == 403


def test_tag_validation(client, data):
    client.login(data.staff.user.username)

    base_data = {
        'name': {'ru': 'name', 'en': 'name_en'},
        'type': 'external',
        'service': data.service.pk,
        'description': {'ru': 'описание', 'en': 'description'},
    }

    response = client.json.post(
        reverse('resources-api:tag-list'),
        dict(slug=data.tag.slug, category="", **base_data)
    )

    assert response.status_code == 400
    assert response.json()['error']['code'] == 'validation_error'


def test_tag_deletion(client, data):
    client.login(data.stranger.user.username)
    response = client.json.delete(reverse('resources-api:tag-detail', args=(data.tag.id,)))
    assert response.status_code == 403

    client.login(data.staff.user.username)
    response = client.json.delete(reverse('resources-api:tag-detail', args=(data.tag.id,)))
    assert response.status_code == 204

    response = client.json.delete(reverse('resources-api:tag-detail', args=(data.tag.id,)))
    assert response.status_code == 404
