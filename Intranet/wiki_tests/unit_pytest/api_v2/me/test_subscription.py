import pytest

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json
from intranet.wiki.tests.wiki_tests.common.skips import only_biz
from wiki.api_v2.public.me.subscriptions.schemas import CreateSubscriptionSchema, DeleteSubscriptionsSchema
from wiki.api_v2.public.pages.page_identity import PageIdentity
from wiki.subscriptions.logic import get_page_subscriptions
from wiki.subscriptions.models import Subscription, SubscriptionType

pytestmark = [pytest.mark.django_db]


def create_subscription(user, page, type_=SubscriptionType.MY, is_cluster=True) -> Subscription:
    subscr = Subscription(user=user, page=page, type=type_, is_cluster=is_cluster)
    subscr.save()
    return subscr


def test_get_subscriptions(client, wiki_users, test_page):
    client.login(wiki_users.asm)
    subscr = create_subscription(client.user, test_page, is_cluster=False)

    response = client.get('/api/v2/public/me/subscriptions')
    subscriptions = response.json()['results']

    assert len(subscriptions) == 1
    assert subscriptions[0]['id'] == subscr.id
    assert subscriptions[0]['is_cluster'] == subscr.is_cluster
    assert subscriptions[0]['page']['id'] == subscr.page.id
    assert subscriptions[0]['page']['title'] == subscr.page.title
    assert subscriptions[0]['page']['slug'] == subscr.page.slug
    assert subscriptions[0]['page']['owner']['username'] == subscr.page.owner.username


def test_get_subscriptions_filter_by_type(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    my_subscr = create_subscription(client.user, page_cluster['root/a'], type_=SubscriptionType.MY)
    other_subscr = create_subscription(client.user, page_cluster['root/b'], type_=SubscriptionType.OTHER)

    # only my
    response = client.get('/api/v2/public/me/subscriptions?type=my')
    subscriptions = response.json()['results']
    assert len(subscriptions) == 1
    assert subscriptions[0]['type'] == my_subscr.type

    # only other
    response = client.get('/api/v2/public/me/subscriptions?type=other')
    subscriptions = response.json()['results']
    assert len(subscriptions) == 1
    assert subscriptions[0]['type'] == other_subscr.type

    # both
    response = client.get('/api/v2/public/me/subscriptions')
    assert len(response.json()['results']) == 2


def test_get_subscriptions_filter_by_q(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)
    page_cluster['root/a'].title = 'foo bar'
    page_cluster['root/b'].title = 'foo baz'
    page_cluster['root/c'].title = 'foo buzz'
    page_cluster['root/a'].save()
    page_cluster['root/b'].save()
    page_cluster['root/c'].save()

    create_subscription(client.user, page_cluster['root/c'], type_=SubscriptionType.MY)
    create_subscription(client.user, page_cluster['root/a'], type_=SubscriptionType.MY)
    create_subscription(client.user, page_cluster['root/b'], type_=SubscriptionType.MY)

    # only my
    response = client.get('/api/v2/public/me/subscriptions?q=foo')

    # ordering by created_at in descending order
    assert_json(
        response.json(),
        {
            'results': [
                {'page': {'slug': 'root/b'}},
                {'page': {'slug': 'root/a'}},
                {'page': {'slug': 'root/c'}},
            ]
        },
    )

    response = client.get('/api/v2/public/me/subscriptions?q=Baz')

    assert_json(
        response.json(),
        {
            'results': [
                {'page': {'slug': 'root/b'}},
            ]
        },
    )


def test_get_subscriptions_filter_user(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    create_subscription(wiki_users.volozh, test_page)
    response = client.get('/api/v2/public/me/subscriptions')
    assert len(response.json()['results']) == 0

    create_subscription(client.user, test_page)
    response = client.get('/api/v2/public/me/subscriptions')
    assert len(response.json()['results']) == 1


def test_get_subscriptions_ordered_by_creation_time(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_slugs = ['root/a/ac/bd', 'root/a', 'root/a/aa', 'root/b', 'root/a/ad']
    for slug in page_slugs:
        create_subscription(client.user, page_cluster[slug], is_cluster=False)

    response = client.get('/api/v2/public/me/subscriptions')
    slugs = [subscr['page']['slug'] for subscr in response.json()['results']]

    assert slugs == page_slugs[::-1]


def test_get_subscriptions_pagination(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_one, page_two, page_three = page_cluster['root/a'], page_cluster['root/a/aa'], page_cluster['root/b']
    create_subscription(client.user, page_one, is_cluster=False)
    create_subscription(client.user, page_two, is_cluster=False)
    create_subscription(client.user, page_three, is_cluster=False)

    # all
    response = client.get('/api/v2/public/me/subscriptions')
    res = response.json()
    assert len(res['results']) == 3
    assert not res['has_next']

    # page one
    response = client.get('/api/v2/public/me/subscriptions?page_size=1')
    res = response.json()
    assert len(res['results']) == 1
    assert res['has_next']
    assert res['results'][0]['page']['slug'] == page_three.slug

    # last page
    response = client.get('/api/v2/public/me/subscriptions?page_size=1&page_id=3')
    res = response.json()
    assert len(res['results']) == 1
    assert not res['has_next']
    assert res['results'][0]['page']['slug'] == page_one.slug


def test_get_subscriptions__cursor(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_one, page_two, page_three = page_cluster['root/a'], page_cluster['root/a/aa'], page_cluster['root/b']
    create_subscription(client.user, page_one, is_cluster=False)
    create_subscription(client.user, page_two, is_cluster=False)
    create_subscription(client.user, page_three, is_cluster=False)

    # all
    response = client.get('/api/v2/public/me/subscriptions')
    res = response.json()
    assert len(res['results']) == 3
    assert res['prev_cursor'] is None and res['next_cursor'] is None

    # page one
    response = client.get('/api/v2/public/me/subscriptions?page_size=1')
    res = response.json()
    assert len(res['results']) == 1
    assert res['prev_cursor'] is None and res['next_cursor']
    assert res['results'][0]['page']['slug'] == page_three.slug

    # next_cursor
    response = client.get(f'/api/v2/public/me/subscriptions?page_size=1&cursor={res["next_cursor"]}')
    res = response.json()
    assert len(res['results']) == 1
    assert res['prev_cursor'] and res['next_cursor']
    assert res['results'][0]['page']['slug'] == page_two.slug

    # last
    response = client.get(f'/api/v2/public/me/subscriptions?page_size=1&cursor={res["next_cursor"]}')
    res = response.json()
    assert len(res['results']) == 1
    assert res['prev_cursor'] and res['next_cursor'] is None
    assert res['results'][0]['page']['slug'] == page_one.slug


@only_biz
def test_get_subscriptions_filter_org(client, wiki_users, test_page, organizations):
    client.login(wiki_users.asm)
    create_subscription(client.user, test_page)

    response = client.get('/api/v2/public/me/subscriptions')
    assert len(response.json()['results']) == 1

    test_page.org = organizations.org_21  # так как wiki_users.asm из 42 организации
    test_page.save()

    response = client.get('/api/v2/public/me/subscriptions')
    assert len(response.json()['results']) == 0


def test_create_subscription(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    data = CreateSubscriptionSchema(page=PageIdentity(slug=test_page.slug), is_cluster=True)
    response = client.post('/api/v2/public/me/subscriptions', data=data.json())

    subscription = response.json()
    subscription_db = Subscription.objects.get(id=subscription['id'])

    assert subscription['type'] == subscription_db.type == SubscriptionType.MY
    assert subscription['is_cluster'] == subscription_db.is_cluster == data.is_cluster
    assert subscription['page']['id'] == subscription_db.page.id == test_page.id
    assert subscription['page']['slug'] == subscription_db.page.slug == test_page.slug


def test_create_subscription_is_cluster(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)
    for is_cluster, slug in zip([False, True], page_cluster):
        data = CreateSubscriptionSchema(page=PageIdentity(slug=slug), is_cluster=is_cluster)
        response = client.post('/api/v2/public/me/subscriptions', data=data.json())

        subscription = response.json()
        subscription_db = Subscription.objects.get(id=subscription['id'])
        assert subscription['is_cluster'] == subscription_db.is_cluster == is_cluster


def test_create_subscription_sql_query(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    data = CreateSubscriptionSchema(page=PageIdentity(slug=page_cluster['root'].slug), is_cluster=True)
    client.post('/api/v2/public/me/subscriptions', data=data.json())
    u = get_page_subscriptions(page_cluster['root/a/ad/bc'])
    assert len(u) == 1
    assert u[0].user == wiki_users.asm


def test_create_subscription_on_cluster_and_delete_nested(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_a, page_a_ad, page_b = page_cluster['root/a'], page_cluster['root/a/ad'], page_cluster['root/b']
    create_subscription(client.user, page_a_ad)
    create_subscription(client.user, page_b)
    create_subscription(wiki_users.thasonic, page_a_ad)

    data = CreateSubscriptionSchema(page=PageIdentity(slug=page_a.slug), is_cluster=True)
    client.post('/api/v2/public/me/subscriptions', data=data.json())

    assert Subscription.objects.all().count() == 3
    assert Subscription.objects.filter(user=client.user, page=page_a).exists()
    assert Subscription.objects.filter(user=client.user, page=page_b).exists()
    assert not Subscription.objects.filter(user=client.user, page=page_a_ad).exists()
    assert Subscription.objects.filter(user=wiki_users.thasonic, page=page_a_ad).exists()


@only_biz
def test_create_subscription_on_cluster_and_delete_nested_by_org(client, wiki_users, page_cluster, organizations):
    client.login(wiki_users.asm)

    page_a, page_a_ad = page_cluster['root/a'], page_cluster['root/a/ad']
    page_a_ad.org = organizations.org_21
    page_a_ad.save()

    create_subscription(client.user, page_a_ad)

    data = CreateSubscriptionSchema(page=PageIdentity(slug=page_a.slug), is_cluster=True)
    client.post('/api/v2/public/me/subscriptions', data=data.json())

    assert Subscription.objects.all().count() == 2
    assert Subscription.objects.filter(user=client.user, page=page_a).exists()
    assert Subscription.objects.filter(user=client.user, page=page_a_ad).exists()


def test_create_subscription_on_subscribed_cluster_above(client, wiki_users, page_cluster):
    """Создать подписку на кластер, на который уже подписан выше"""
    client.login(wiki_users.asm)

    create_subscription(client.user, page_cluster['root/a'], is_cluster=False)
    create_subscription(client.user, page_cluster['root/b'], is_cluster=False)
    create_subscription(wiki_users.thasonic, page_cluster['root'], is_cluster=True)

    data = CreateSubscriptionSchema(page=PageIdentity(slug=page_cluster['root/a/ad'].slug), is_cluster=True)
    response = client.post('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 200

    data = CreateSubscriptionSchema(page=PageIdentity(slug=page_cluster['root/a/ad/bc'].slug), is_cluster=True)
    response = client.post('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 400  # the user already has a cluster subscription to the `root/a/ad` page


@only_biz
def test_create_subscription_on_subscribed_cluster_above_by_org(client, wiki_users, page_cluster, organizations):
    client.login(wiki_users.asm)

    page_a, page_a_ad = page_cluster['root/a'], page_cluster['root/a/ad']
    page_a.org = organizations.org_21
    page_a.save()

    create_subscription(client.user, page_a, is_cluster=True)

    data = CreateSubscriptionSchema(page=PageIdentity(slug=page_a_ad.slug), is_cluster=True)
    response = client.post('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 200


def test_create_subscription_twice(client, wiki_users, test_page):
    client.login(wiki_users.asm)
    data = CreateSubscriptionSchema(page=PageIdentity(slug=test_page.slug), is_cluster=True)

    assert Subscription.objects.all().count() == 0

    response = client.post('/api/v2/public/me/subscriptions', data=data.json())
    first_id = response.json()['id']

    assert response.status_code == 200
    assert Subscription.objects.all().count() == 1

    response = client.post('/api/v2/public/me/subscriptions', data=data.json())
    second_id = response.json()['id']

    assert response.status_code == 200
    assert Subscription.objects.all().count() == 1

    assert first_id != second_id  # recreated subscription


def test_create_subscription_invalid_page(client, wiki_users):
    client.login(wiki_users.asm)

    data = CreateSubscriptionSchema(page=PageIdentity(slug='invalid slug'), is_cluster=True)
    response = client.post('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 400


def test_create_subscription_no_accessible_by_acl_page(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    set_access_author_only(test_page)  # only thasonic

    data = CreateSubscriptionSchema(page=PageIdentity(slug=test_page.slug), is_cluster=True)
    response = client.post('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 403


@only_biz
def test_create_subscription_page_another_org(client, wiki_users, test_page, organizations):
    client.login(wiki_users.asm)
    data = CreateSubscriptionSchema(page=PageIdentity(id=test_page.id), is_cluster=True)

    test_page.org = organizations.org_21  # так как wiki_users.asm из 42 организации
    test_page.save()
    response = client.post('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 400

    test_page.org = organizations.org_42  # так как wiki_users.asm из 42 организации
    test_page.save()
    response = client.post('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 200


def test_delete_subscription(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_a, page_b = page_cluster['root/a'], page_cluster['root/b']
    create_subscription(client.user, page_a)
    create_subscription(client.user, page_b)
    assert Subscription.objects.all().count() == 2

    data = DeleteSubscriptionsSchema(pages=[PageIdentity(id=page_a.id), PageIdentity(slug=page_b.slug)])
    response = client.delete('/api/v2/public/me/subscriptions', data=data.json())

    assert response.status_code == 204
    assert Subscription.objects.all().count() == 0


def test_delete_subscription_invalid_page(client, wiki_users):
    client.login(wiki_users.asm)

    data = DeleteSubscriptionsSchema(pages=[PageIdentity(slug='invalid slug')])
    response = client.delete('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 400


def test_delete_subscription_no_accessible_by_acl_page(client, wiki_users, test_page):
    """В случае, если подписан на страницу, к которой у тебя нет доступа. Но удалить подписку пользователь может"""
    client.login(wiki_users.asm)

    create_subscription(client.user, test_page)
    set_access_author_only(test_page)  # only thasonic

    data = DeleteSubscriptionsSchema(pages=[PageIdentity(slug=test_page.slug)])
    response = client.delete('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 204


def test_delete_subscription_no_subscribed_page(client, wiki_users, page_cluster):
    """Удалить подписку на страницу, на которую нет подписки. Возвращает как обычно"""
    client.login(wiki_users.asm)

    page_have, page_no = page_cluster['root/a'], page_cluster['root/b']
    create_subscription(client.user, page_have)
    assert Subscription.objects.all().count() == 1

    data = DeleteSubscriptionsSchema(strict=True, pages=[PageIdentity(id=page_have.id), PageIdentity(id=page_no.id)])
    response = client.delete('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 404
    assert Subscription.objects.all().count() == 1

    data = DeleteSubscriptionsSchema(strict=True, pages=[PageIdentity(id=page_no.id)])
    response = client.delete('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 404
    assert Subscription.objects.all().count() == 1

    data = DeleteSubscriptionsSchema(strict=True, pages=[PageIdentity(id=page_have.id)])
    response = client.delete('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 204
    assert Subscription.objects.all().count() == 0


def test_delete_subscription_no_subscribed_page__unstrict(client, wiki_users, page_cluster):
    """Удалить подписку на страницу, на которую нет подписки. Возвращает как обычно"""
    client.login(wiki_users.asm)

    page_have, page_no = page_cluster['root/a'], page_cluster['root/b']
    create_subscription(client.user, page_have)
    assert Subscription.objects.all().count() == 1

    data = DeleteSubscriptionsSchema(strict=False, pages=[PageIdentity(id=page_have.id), PageIdentity(id=page_no.id)])
    response = client.delete('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 204
    assert Subscription.objects.all().count() == 0


@only_biz
def test_delete_subscription_page_another_org(client, wiki_users, test_page, organizations):
    client.login(wiki_users.asm)
    create_subscription(client.user, test_page)
    data = DeleteSubscriptionsSchema(pages=[PageIdentity(id=test_page.id)])

    test_page.org = organizations.org_21  # так как wiki_users.asm из 42 организации
    test_page.save()
    response = client.delete('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 400
    assert Subscription.objects.all().count() == 1

    test_page.org = organizations.org_42  # так как wiki_users.asm из 42 организации
    test_page.save()
    response = client.delete('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 204
    assert Subscription.objects.all().count() == 0


def test_delete_subscription_current_user(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    create_subscription(client.user, test_page)
    create_subscription(wiki_users.thasonic, test_page)
    create_subscription(wiki_users.volozh, test_page)
    assert Subscription.objects.all().count() == 3

    data = DeleteSubscriptionsSchema(pages=[PageIdentity(slug=test_page.slug)])
    response = client.delete('/api/v2/public/me/subscriptions', data=data.json())
    assert response.status_code == 204
    assert Subscription.objects.all().count() == 2


def test_delete_nested_page_subscription_from_cluster(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_a, page_a_ad, page_b = page_cluster['root/a'], page_cluster['root/a/ad'], page_cluster['root/b']
    create_subscription(client.user, page_a_ad)
    create_subscription(client.user, page_b)

    data = CreateSubscriptionSchema(page=PageIdentity(slug=page_a.slug), is_cluster=True)
    client.post('/api/v2/public/me/subscriptions', data=data.json())

    assert Subscription.objects.all().count() == 2
    assert Subscription.objects.filter(user=client.user, page=page_a).exists()
    assert Subscription.objects.filter(user=client.user, page=page_b).exists()
    assert not Subscription.objects.filter(user=client.user, page=page_a_ad).exists()

    unsubscribe_data = DeleteSubscriptionsSchema(strict=False, pages=[PageIdentity(id=page_a_ad.id)])
    response = client.delete('/api/v2/public/me/subscriptions', data=unsubscribe_data.json())

    assert response.status_code == 204
    assert Subscription.objects.all().count() == 1


def test_delete_nested_page_subscription_not_cluster(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_a, page_a_ad, page_b = page_cluster['root/a'], page_cluster['root/a/ad'], page_cluster['root/b']
    create_subscription(client.user, page_a_ad)
    create_subscription(client.user, page_b)
    create_subscription(client.user, page_a, is_cluster=False)

    assert Subscription.objects.all().count() == 3
    assert Subscription.objects.filter(user=client.user, page=page_a).exists()
    assert Subscription.objects.filter(user=client.user, page=page_b).exists()
    assert Subscription.objects.filter(user=client.user, page=page_a_ad).exists()

    unsubscribe_data = DeleteSubscriptionsSchema(strict=False, pages=[PageIdentity(id=page_a_ad.id)])
    response = client.delete('/api/v2/public/me/subscriptions', data=unsubscribe_data.json())

    assert response.status_code == 204
    assert Subscription.objects.all().count() == 2


def test_delete_deep_nested_page_subscription_from_cluster(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_a, page_a_ad, page_b = page_cluster['root/a'], page_cluster['root/a/ad'], page_cluster['root/b']
    page_a_ad_bc = page_cluster['root/a/ad/bc']
    page_a_ac_bd = page_cluster['root/a/ac/bd']

    create_subscription(client.user, page_a, is_cluster=False)
    create_subscription(client.user, page_a_ad_bc, is_cluster=False)
    create_subscription(client.user, page_b)
    create_subscription(client.user, page_a_ac_bd)

    data = CreateSubscriptionSchema(page=PageIdentity(slug=page_a_ad.slug), is_cluster=True)
    client.post('/api/v2/public/me/subscriptions', data=data.json())

    assert Subscription.objects.all().count() == 4
    assert Subscription.objects.filter(user=client.user, page=page_a).exists()
    assert Subscription.objects.filter(user=client.user, page=page_b).exists()
    assert Subscription.objects.filter(user=client.user, page=page_a_ad).exists()
    assert not Subscription.objects.filter(user=client.user, page=page_a_ad_bc).exists()


    unsubscribe_data = DeleteSubscriptionsSchema(strict=False, pages=[PageIdentity(id=page_a_ad_bc.id)])
    response = client.delete('/api/v2/public/me/subscriptions', data=unsubscribe_data.json())

    assert response.status_code == 204
    assert Subscription.objects.all().count() == 3
    assert not Subscription.objects.filter(user=client.user, page=page_a_ad).exists()



def test_delete_multiple_nested_pages_subscription_from_cluster(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_a, page_b = page_cluster['root/a'], page_cluster['root/b']
    page_a_ad_bc, page_a_ac_bd = page_cluster['root/a/ad/bc'], page_cluster['root/a/ac/bd']
    page_b_bd, page_a_ad = page_cluster['root/b/bd'], page_cluster['root/a/ad']

    create_subscription(client.user, page_a, is_cluster=False)
    create_subscription(client.user, page_a_ad_bc, is_cluster=False)
    create_subscription(client.user, page_b_bd)
    create_subscription(client.user, page_a_ac_bd)
    create_subscription(client.user, page_a_ad)

    data = CreateSubscriptionSchema(page=PageIdentity(slug=page_a_ad.slug), is_cluster=True)
    client.post('/api/v2/public/me/subscriptions', data=data.json())
    data = CreateSubscriptionSchema(page=PageIdentity(slug=page_b.slug), is_cluster=True)
    client.post('/api/v2/public/me/subscriptions', data=data.json())

    assert Subscription.objects.all().count() == 4
    assert Subscription.objects.filter(user=client.user, page=page_a).exists()
    assert Subscription.objects.filter(user=client.user, page=page_b).exists()
    assert Subscription.objects.filter(user=client.user, page=page_a_ad).exists()
    assert not Subscription.objects.filter(user=client.user, page=page_a_ad_bc).exists()


    unsubscribe_data = DeleteSubscriptionsSchema(
        strict=False,
        pages=[PageIdentity(id=page_a_ad_bc.id), PageIdentity(id=page_b.id)]
    )
    response = client.delete('/api/v2/public/me/subscriptions', data=unsubscribe_data.json())

    assert response.status_code == 204
    assert Subscription.objects.all().count() == 2
