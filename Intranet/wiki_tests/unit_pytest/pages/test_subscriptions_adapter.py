import pytest

from wiki.api_frontend.serializers.user_identity import UserIdentity
from wiki.api_v2.public.pages.subscribers.schemas import AddSubscribersSchema
from wiki.pages.logic.subscription import create_watch
from wiki.pages.models import PageWatch
from wiki.subscriptions.consts import SubscriptionType
from wiki.subscriptions.logic import WATCHES_ID_INCREMENT
from wiki.subscriptions.models import Subscription
from wiki.users.logic.settings import set_user_setting
from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only

pytestmark = [pytest.mark.django_db]


def create_subscription(user, page, type_=SubscriptionType.MY, is_cluster=True) -> Subscription:
    subscr = Subscription(user=user, page=page, type=type_, is_cluster=is_cluster)
    subscr.save()
    return subscr


def test_api_frontend_page(client, wiki_users, test_page, legacy_subscr_favor):
    user = wiki_users.thasonic

    create_subscription(user, test_page)

    client.login(user)
    response = client.get(f'/_api/frontend/{test_page.slug}')
    assert response.status_code == 200

    response_data = response.json()['data']
    assert response_data['current_user_subscription'] == 'none'

    set_user_setting(user, 'new_subscriptions', True)

    response = client.get(f'/_api/frontend/{test_page.slug}')
    assert response.status_code == 200

    response_data = response.json()['data']
    assert response_data['current_user_subscription'] == 'cluster'


def test_page_watchers(client, wiki_users, page_cluster, legacy_subscr_favor):
    # У thasonic старый вариант подписки у chapson новый и
    # включена настройка использовать новую схему, в ответе
    # должны придти оба в обеих ручках, старой и новой
    page = page_cluster['root/a/ad']
    create_watch(page, wiki_users.thasonic, True)
    create_subscription(wiki_users.chapson, page)
    set_user_setting(wiki_users.chapson, 'new_subscriptions', True)

    client.login(wiki_users.thasonic)
    response = client.get(f'/_api/frontend/{page.slug}/.watchers')
    assert response.status_code == 200

    response_data = response.json()['data']
    assert len(response_data) == 2
    assert response_data[0]['login'] == wiki_users.thasonic.username
    assert response_data[1]['login'] == wiki_users.chapson.username

    response = client.get(f'/api/v2/public/pages/{page.id}/subscribers')
    assert response.status_code == 200

    response_data = response.json()['results']
    assert len(response_data) == 2
    assert response_data[0]['user']['username'] == wiki_users.chapson.username
    assert response_data[0]['is_cluster'] is True
    assert response_data[1]['user']['username'] == wiki_users.thasonic.username
    assert response_data[1]['is_cluster'] is True


def test_page_watchers_empty(client, wiki_users, page_cluster, legacy_subscr_favor):
    # thasonic теперь использует новую схему, но у него только
    # старые подписки, а chapson использует старую, но у
    # него только новые, должен вернуться пустой список
    page = page_cluster['root/a/ad']
    create_watch(page, wiki_users.thasonic, True)
    create_subscription(wiki_users.chapson, page)

    set_user_setting(wiki_users.thasonic, 'new_subscriptions', True)

    client.login(wiki_users.thasonic)
    response = client.get(f'/_api/frontend/{page.slug}/.watchers')
    assert response.status_code == 200

    response_data = response.json()['data']
    assert len(response_data) == 0

    response = client.get(f'/api/v2/public/pages/{page.id}/subscribers')
    assert response.status_code == 200

    response_data = response.json()['results']
    assert len(response_data) == 0


def test_page_watch(client, wiki_users, page_cluster):
    set_user_setting(wiki_users.thasonic, 'new_subscriptions', True)
    page = page_cluster['root/a']

    assert Subscription.objects.filter(user=wiki_users.thasonic, page=page).count() == 0

    client.login(wiki_users.thasonic)
    response = client.post(f'/_api/frontend/{page.slug}/.watch')
    assert response.status_code == 200
    assert response.json()['data']['pages_count'] == 1
    assert Subscription.objects.filter(user=wiki_users.thasonic, page=page).count() == 1


def test_page_unwatch(client, wiki_users, page_cluster):
    set_user_setting(wiki_users.thasonic, 'new_subscriptions', True)
    page = page_cluster['root/a']

    create_subscription(wiki_users.thasonic, page)

    client.login(wiki_users.thasonic)
    response = client.post(f'/_api/frontend/{page.slug}/.unwatch')
    assert response.status_code == 200
    assert Subscription.objects.filter(user=wiki_users.thasonic, page=page).count() == 0


def test_page_masswatch(client, wiki_users, page_cluster, legacy_subscr_favor):
    set_user_setting(wiki_users.thasonic, 'new_subscriptions', True)
    set_user_setting(wiki_users.asm, 'new_subscriptions', True)
    page = page_cluster['root/a']

    assert Subscription.objects.filter(user=wiki_users.thasonic, page=page, is_cluster=True).count() == 0
    assert Subscription.objects.filter(user=wiki_users.asm, page=page, is_cluster=True).count() == 0
    assert Subscription.objects.filter(user=wiki_users.chapson, page=page, is_cluster=True).count() == 0
    assert PageWatch.objects.filter(user=wiki_users.chapson.username, page=page).count() == 0

    client.login(wiki_users.thasonic)
    response = client.post(
        f'/_api/frontend/{page.slug}/.masswatch',
        data={'uids': [wiki_users.thasonic.staff.uid, wiki_users.chapson.staff.uid, wiki_users.asm.staff.uid]},
    )
    assert response.status_code == 200
    assert response.json()['data']['pages_count'] > 2
    assert Subscription.objects.filter(user=wiki_users.thasonic, page=page, is_cluster=True).count() == 1
    assert Subscription.objects.filter(user=wiki_users.asm, page=page, is_cluster=True).count() == 1
    assert Subscription.objects.filter(user=wiki_users.chapson, page=page, is_cluster=True).count() == 0
    assert PageWatch.objects.filter(user=wiki_users.chapson.username, page=page).count() == 1


def test_page_massunwatch(client, wiki_users, page_cluster):
    set_user_setting(wiki_users.thasonic, 'new_subscriptions', True)
    page = page_cluster['root/a']

    create_subscription(wiki_users.thasonic, page_cluster['root'])
    assert Subscription.objects.filter(user=wiki_users.thasonic).count() == 1

    client.login(wiki_users.thasonic)
    response = client.post(f'/_api/frontend/{page.slug}/.massunwatch')
    assert response.status_code == 200
    assert Subscription.objects.filter(user=wiki_users.thasonic).count() == 0


def test_add_subscribers(client, wiki_users, page_cluster, legacy_subscr_favor):
    set_user_setting(wiki_users.thasonic, 'new_subscriptions', True)
    set_user_setting(wiki_users.asm, 'new_subscriptions', True)
    page = page_cluster['root/a']

    data = AddSubscribersSchema(
        users=[
            UserIdentity.from_user(wiki_users.asm),
            UserIdentity.from_user(wiki_users.chapson),
            UserIdentity.from_user(wiki_users.thasonic),
        ],
        is_cluster=True,
    )

    assert Subscription.objects.filter(user=wiki_users.thasonic, page=page, is_cluster=True).count() == 0
    assert Subscription.objects.filter(user=wiki_users.asm, page=page, is_cluster=True).count() == 0
    assert Subscription.objects.filter(user=wiki_users.chapson, page=page, is_cluster=True).count() == 0
    assert PageWatch.objects.filter(user=wiki_users.chapson.username).count() == 0

    client.login(wiki_users.thasonic)
    response = client.post(f'/api/v2/public/pages/{page.id}/subscribers', data=data.json())
    assert response.status_code == 200

    assert len(response.json()['results']) == 3
    assert Subscription.objects.filter(user=wiki_users.thasonic, page=page, is_cluster=True).count() == 1
    assert Subscription.objects.filter(user=wiki_users.asm, page=page, is_cluster=True).count() == 1
    assert Subscription.objects.filter(user=wiki_users.chapson).count() == 0
    assert PageWatch.objects.filter(user=wiki_users.chapson.username).count() == 5


def test_add_subscribers__no_access(client, wiki_users, test_page):
    set_user_setting(wiki_users.asm, 'new_subscriptions', False)
    set_access_author_only(test_page, new_authors=[wiki_users.thasonic])

    data = AddSubscribersSchema(users=[UserIdentity.from_user(wiki_users.asm)], is_cluster=True)

    client.login(wiki_users.thasonic)
    response = client.post(f'/api/v2/public/pages/{test_page.id}/subscribers', data=data.json())
    assert response.status_code == 403


def test_remove_subscribers(client, wiki_users, page_cluster):
    page = page_cluster['root/a/ad']

    create_watch(page, wiki_users.thasonic, False)
    watch = PageWatch.objects.get(page=page, user=wiki_users.thasonic.username)
    watch_id = watch.id + WATCHES_ID_INCREMENT

    assert PageWatch.objects.filter(user=wiki_users.thasonic.username).count() == 1

    client.login(wiki_users.thasonic)
    response = client.delete(f'/api/v2/public/pages/{page.id}/subscribers/{watch_id}')
    assert response.status_code == 204
    assert PageWatch.objects.filter(user=wiki_users.thasonic.username).count() == 0


def test_subscribe_nested_page(client, wiki_users, page_cluster):
    root, page = page_cluster['root'], page_cluster['root/a']

    # old
    client.login(wiki_users.asm)
    response = client.post(f'/_api/frontend/{root.slug}/.masswatch')
    assert response.status_code == 200

    response = client.post(f'/_api/frontend/{page.slug}/.watch')
    assert response.status_code == 200
    assert response.json()['data']['pages_count'] == 0

    response = client.post(f'/_api/frontend/{page.slug}/.masswatch')
    assert response.status_code == 200
    assert response.json()['data']['pages_count'] == 0

    # new
    set_user_setting(wiki_users.thasonic, 'new_subscriptions', True)
    create_subscription(wiki_users.thasonic, root, is_cluster=True)

    client.login(wiki_users.thasonic)
    response = client.post(f'/_api/frontend/{page.slug}/.watch')
    assert response.status_code == 200
    assert response.json()['data']['pages_count'] == 0

    response = client.post(f'/_api/frontend/{page.slug}/.masswatch')
    assert response.status_code == 200
    assert response.json()['data']['pages_count'] == 0
