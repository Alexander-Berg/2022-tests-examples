import pytest

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only, set_access_everyone
from intranet.wiki.tests.wiki_tests.common.skips import only_biz
from wiki.pages.models import PageWatch
from wiki.subscriptions.models import Subscription, SubscriptionType

pytestmark = [pytest.mark.django_db]


def test_one_non_cluster(client, wiki_users, test_page):
    client.login(wiki_users.asm)
    page_watch = PageWatch.objects.create(page=test_page, is_cluster=False, user=client.user.username)

    client.post('/api/v2/public/me/subscriptions/migrate')
    assert Subscription.objects.count() == 1

    subscr = Subscription.objects.first()
    assert subscr.created_at == page_watch.created_at
    assert subscr.page == test_page
    assert subscr.is_cluster is False
    assert len(subscr.exclude['exclude']) == 0


def test_one_cluster_collapse(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    pages = ['root/a', 'root/a/aa', 'root/a/ad', 'root/a/ad/bc', 'root/a/ac/bd']
    for slug in pages:
        PageWatch(page=page_cluster[slug], is_cluster=True, user=client.user.username).save()

    response = client.post('/api/v2/public/me/subscriptions/migrate')
    assert response.status_code == 200
    assert Subscription.objects.count() == 1

    subscr = Subscription.objects.first()
    assert subscr.is_cluster is True
    assert subscr.page == page_cluster['root/a']
    assert len(subscr.exclude['exclude']) == 0


def test_many_subscriptions(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    clusters = {'root/b', 'root/a/aa'}
    for slug in clusters:
        PageWatch(page=page_cluster[slug], is_cluster=True, user=client.user.username).save()

    ones = {'root'}
    for slug in ones:
        PageWatch(page=page_cluster[slug], is_cluster=False, user=client.user.username).save()

    client.post('/api/v2/public/me/subscriptions/migrate')
    assert Subscription.objects.count() == len(clusters | ones)
    assert {subscr.page.supertag for subscr in Subscription.objects.all()} == clusters | ones


def test_exclude_one(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    for slug in ['root/a', 'root/a/aa', 'root/a/ad', 'root/a/ad/bc']:  # pass 'root/a/ac/bd':
        PageWatch(page=page_cluster[slug], is_cluster=True, user=client.user.username).save()

    client.post('/api/v2/public/me/subscriptions/migrate')
    assert Subscription.objects.count() == 1

    subscr = Subscription.objects.first()
    assert subscr.page == page_cluster['root/a']
    assert subscr.exclude['exclude'] == ['root/a/ac/bd']


def test_exclude_one_nested(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    for slug in ['root/a', 'root/a/aa', 'root/a/ad', 'root/a/ac/bd']:  # pass 'root/a/ad/bc':
        PageWatch(page=page_cluster[slug], is_cluster=True, user=client.user.username).save()

    client.post('/api/v2/public/me/subscriptions/migrate')
    assert Subscription.objects.count() == 1

    subscr = Subscription.objects.first()
    assert subscr.page == page_cluster['root/a']
    assert subscr.exclude['exclude'] == ['root/a/ad/bc']


def test_exclude_sub_cluster(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    for slug in ['root/a', 'root/a/aa', 'root/a/ac/bd']:  # exclude ['root/a/ad', 'root/a/ad/bc']:
        PageWatch(page=page_cluster[slug], is_cluster=True, user=client.user.username).save()

    client.post('/api/v2/public/me/subscriptions/migrate')
    assert Subscription.objects.count() == 1

    subscr = Subscription.objects.first()
    assert subscr.page == page_cluster['root/a']
    assert subscr.exclude['exclude'] == ['root/a/ad']


def test_subscribe_to_sub_cluster_as_page(client, wiki_users, page_cluster):
    """
    Подписался на кластер 'root'.
    Сменил тип подписки 'root/a' как на страницу, и отписался от этой подветки.
    Подписался на кластер 'root/a/ad'.
    Отписался от подветки 'root/a/ac/bd'.
    """
    client.login(wiki_users.asm)

    for slug in ['root', 'root/b', 'root/a/ad']:  # pass ['root/a', 'root/a/aa', 'root/a/ad/bc', 'root/a/ac/bd']
        PageWatch(page=page_cluster[slug], is_cluster=True, user=client.user.username).save()
    PageWatch(page=page_cluster['root/a'], is_cluster=False, user=client.user.username).save()

    client.post('/api/v2/public/me/subscriptions/migrate')
    assert Subscription.objects.count() == 3

    root_subscr = Subscription.objects.filter(page__supertag='root', is_cluster=True).first()
    assert root_subscr.exclude == {'exclude': ['root/a', 'root/b/bd', 'root/c']}

    one_subscr = Subscription.objects.filter(page__supertag='root/a', is_cluster=False).first()
    assert one_subscr.exclude == {'exclude': []}

    nested_subscr = Subscription.objects.filter(page__supertag='root/a/ad', is_cluster=True).first()
    assert nested_subscr.exclude == {'exclude': ['root/a/ad/bc']}


def test_exclude_ignore_non_accessible(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_root, page_nested = page_cluster['root/a/ad'], page_cluster['root/a/ad/bc']
    PageWatch(page=page_root, is_cluster=True, user=client.user.username).save()

    set_access_author_only(page=page_nested, new_authors=[wiki_users.thasonic])

    client.post('/api/v2/public/me/subscriptions/migrate')
    assert Subscription.objects.count() == 1

    subscr = Subscription.objects.first()
    assert subscr.page == page_root
    assert subscr.exclude['exclude'] == []


def test_exclude_ignore_non_accessible_nested(client, wiki_users, page_cluster):
    """Есть доступ только к корню и вложенной странице. Но подписан только на корень как кластер"""
    client.login(wiki_users.asm)

    for page in page_cluster.values():
        set_access_author_only(page=page, new_authors=[wiki_users.thasonic])

    root, nested = page_cluster['root'], page_cluster['root/a/ad/bc']
    for page in [root, nested]:
        set_access_everyone(page)

    PageWatch(page=page_cluster['root'], is_cluster=True, user=client.user.username).save()

    client.post('/api/v2/public/me/subscriptions/migrate')
    assert Subscription.objects.count() == 1

    subscr = Subscription.objects.first()
    assert subscr.page == root
    assert subscr.exclude['exclude'] == [nested.supertag]


def test_subscription_to_non_accessible_cluster(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    for page in page_cluster['root/a/ad'], page_cluster['root/a/ad/bc']:
        PageWatch(page=page, is_cluster=True, user=client.user.username).save()
        set_access_author_only(page=page, new_authors=[wiki_users.thasonic])

    client.post('/api/v2/public/me/subscriptions/migrate')
    assert Subscription.objects.count() == 1

    subscr = Subscription.objects.first()
    assert subscr.page == page_cluster['root/a/ad']
    assert subscr.exclude['exclude'] == []


def test_subscription_in_exclude_cluster(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    for slug in ['root', 'root/b']:  # exclude ['root/a']
        PageWatch(page=page_cluster[slug], is_cluster=True, user=client.user.username).save()
    for slug in ['root/a/ad', 'root/a/ad/bc']:
        PageWatch(page=page_cluster[slug], is_cluster=True, user=client.user.username).save()

    client.post('/api/v2/public/me/subscriptions/migrate')
    assert Subscription.objects.count() == 2

    root = Subscription.objects.filter(page__supertag='root', is_cluster=True).first()
    assert root.exclude['exclude'] == ['root/a', 'root/b/bd', 'root/c']

    nested = Subscription.objects.filter(page__supertag='root/a/ad', is_cluster=True).first()
    assert nested.exclude['exclude'] == []


def test_re_migration(client, wiki_users, test_page):
    client.login(wiki_users.asm)

    PageWatch(page=test_page, is_cluster=False, user=client.user.username).save()

    subscr = Subscription(user=client.user, page=test_page, is_cluster=False, type=SubscriptionType.MY)
    subscr.save()

    data = {'force': False}
    response = client.post('/api/v2/public/me/subscriptions/migrate', data=data)
    assert response.status_code == 400
    assert Subscription.objects.count() == 1
    assert Subscription.objects.filter(id=subscr.id).exists()

    data = {'force': True}
    response = client.post('/api/v2/public/me/subscriptions/migrate', data=data)
    assert response.status_code == 200
    assert Subscription.objects.count() == 1
    assert Subscription.objects.filter(id=subscr.id).exists() is False


@only_biz
def test_re_migration_filter_by_org(client, wiki_users, page_cluster, organizations):
    client.login(wiki_users.asm)
    current, other = page_cluster['root/a'], page_cluster['root/b']

    current_subscr = Subscription(user=client.user, page=current, is_cluster=False, type=SubscriptionType.MY)
    other_subscr = Subscription(user=client.user, page=other, is_cluster=False, type=SubscriptionType.MY)
    current_subscr.save()
    other_subscr.save()

    other.org = organizations.org_21
    other.save()

    response = client.post('/api/v2/public/me/subscriptions/migrate', data={'force': True})
    assert response.status_code == 200
    assert set(Subscription.objects.all()) == {other_subscr}


@only_biz
def test_filter_by_org(client, wiki_users, page_cluster, organizations):
    client.login(wiki_users.asm)

    curr, other = page_cluster['root/a/ad'], page_cluster['root/a/ad/bc']

    for page in (curr, other):
        PageWatch(page=page, is_cluster=False, user=client.user.username).save()

    other.org = organizations.org_21
    other.save()

    client.post('/api/v2/public/me/subscriptions/migrate')
    assert Subscription.objects.filter(page=curr).exists()
    assert not Subscription.objects.filter(page=other).exists()


@only_biz
def test_exclude_filter_by_org(client, wiki_users, page_cluster, organizations):
    client.login(wiki_users.asm)

    curr, other = page_cluster['root/a/ad'], page_cluster['root/a/ad/bc']

    PageWatch(page=curr, is_cluster=True, user=client.user.username).save()

    other.org = organizations.org_21
    other.save()

    client.post('/api/v2/public/me/subscriptions/migrate')
    assert Subscription.objects.count() == 1

    subscr = Subscription.objects.first()
    assert subscr.page == curr
    assert subscr.exclude['exclude'] == []
