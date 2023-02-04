# flake8: noqa: E501

from django.conf import settings
from django.contrib.auth import get_user_model

from wiki import access as wiki_access
from wiki.notifications.models import PageEvent
from wiki.pages.access import get_raw_access, interpret_raw_access
from wiki.pages.api import save_page
from wiki.pages.cluster import Cluster, straighten_relative_links
from wiki.pages.constants import ReservedSupertagAction as Action
from wiki.pages.logic.move import move_clusters
from wiki.pages.models import Access, Page, PageWatch
from wiki.pages.reserved_supertags import is_reserved_supertag
from wiki.personalisation.user_cluster import create_personal_page
from wiki.users.logic import set_user_setting

from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase

User = get_user_model()


class ClusterMoveTest(BaseTestCase):
    def setUp(self):
        super(ClusterMoveTest, self).setUp()
        self.setGroupMembers()
        self.cluster = Cluster('thasonic')
        self.user = self.user_thasonic
        self.setPages()

    def setPages(self):
        self.create_page(tag='testinfo', supertag='testinfo')
        self.create_page(tag='testinfo/testinfogem', supertag='testinfo/testinfogem')
        self.create_page(tag='testinfo/bla', supertag='testinfo/bla')
        self.create_page(tag='destination/testinfo/testinfogem', supertag='destination/testinfo/testinfogem')
        self.create_page(tag='testinfo/redirectpage', supertag='testinfo/redirectpage')
        self.create_page(tag='search', supertag='search')
        redirect = Page.objects.get(supertag='testinfo/redirectpage')
        page = Page.objects.get(supertag='destination/testinfo/testinfogem')
        redirect.redirects_to = page
        redirect.save()

    def create_page(self, **kwargs):
        if 'body' not in kwargs:
            kwargs['body'] = 'example text'
        kwargs['authors_to_add'] = [self.user_chapson]
        return super(ClusterMoveTest, self).create_page(**kwargs)

    def testIntoItselt(self):
        r = self.cluster.move({'testinfo/bla': 'testinfo/bla', 'testinfo/testinfogem': 'testinfo/egg'})
        self.assertEqual(412, r[0])
        self.assertEqual('into_itself', r[1])
        self.assertEqual({'testinfo/bla': 'testinfo/bla'}, r[2])  # Error clusters must be different'

        r = self.cluster.move({'testinfo/testinfogem': 'testinfo/testinfogem/egg'})
        self.assertEqual(
            {'testinfo/testinfogem': 'testinfo/testinfogem/egg'}, r[2]
        )  # 'Error clusters must be different')
        self.assertEqual('into_itself', r[1])

    def testSameParent(self):
        r = self.cluster.move({'testinfo/testinfogem': 'settings/gem', 'settings': 'settings/gem'})
        self.assertNotEqual(r[0], 200)  # Expected error code
        self.assertEqual('different_source_parents', r[1])  # Expected some clusters to be of different parents

        r = self.cluster.move({'testinfo/testinfogem': 'settings/gem', 'testinfo/bla': 'testinfo/cool'})
        self.assertNotEqual(r[0], 200)  # Expected error code
        self.assertEqual('different_result_parents', r[1])  # Expected some clusters to be of different parents

    def testClustersAccessible(self):
        wiki_access.set_access(
            Page.objects.get(supertag='testinfo/testinfogem'), wiki_access.TYPES.OWNER, self.user_chapson
        )
        r = self.cluster.move({'testinfo/testinfogem': 'settings/gem'})
        self.assertTrue(r[0] == 403, 'Expecting access error')
        self.assertTrue(bool(r[1]), 'Expect error description')

    def testResultAccessible(self):
        Access.objects.filter(page=Page.objects.get(supertag='search')).delete()
        wiki_access.set_access(Page.objects.get(supertag='search'), wiki_access.TYPES.OWNER, self.user_chapson)

        r = self.cluster.move({'testinfo': 'search/testinfo'})
        self.assertEqual(r[0], 403)  # Expecting access error
        self.assertTrue(bool(r[1]), 'Expect error description')

        # если пользователь владелец переносимой страницы, то он
        # сохранит доступ всегда, поэтому проверку должен пройти
        page = Page.objects.get(supertag='testinfo')
        page.authors.clear()
        page.authors.add(self.user_thasonic)
        page.save()
        response = self.cluster.move({'testinfo': 'search/testinfo'})
        self.assertEqual(200, response[0])

    def testAllClustersExist(self):
        r = self.cluster.move({'testinfo/nonexistent': 'search/bla'})
        self.assertFalse(r[0] != 400, 'Expect 400 error')
        self.assertTrue(bool(r[1]), 'Expected error text')

    def testNamesConflict(self):
        # Error because of clusters testinfo/gem and destination/testinfo/gem
        r = self.cluster.move({'testinfo': 'destination/testinfo'})
        self.assertFalse(r[0] != 409, 'Expect 409 Conflict error')
        self.assertTrue(bool(r[1]), 'Expected error text')
        # error because cluster search exists
        r = self.cluster.move({'testinfo/testinfogem': 'search'})
        self.assertFalse(r[0] != 409, 'Expect 409 Conflict error')
        self.assertTrue(bool(r[1]), 'Expected error text')

    def test_move_common_access(self):
        r = self.cluster.move({'testinfo/redirectpage': 'nyanyanya'})
        self.assertEqual(r[0], 409, 'Expect 409 conflict error')
        self.assertTrue(r[1] != 'Expected error text')

    def test_redirect_created(self):
        result = self.cluster.move({'testinfo': 'nyanyanya'})
        page = Page.objects.get(supertag='testinfo')
        self.assertTrue(page.has_redirect(), 'Must have created redirect')
        page = Page.objects.filter(supertag='nyanyanya').count()
        self.assertEqual(page, 1, 'Page must have been created')

    def test_wiki_2217(self):
        """
                The cluster "testinfo/testinfogem" should get common access set explicitly
        The cluster "testinfo/testinfogem" inherits common access. It is moved under page "search"
        which has access set "only to owner" "testinfo/testinfogem" must get explicit access "common access"
        """
        search = Page.objects.get(supertag='search')
        wiki_access.set_access(search, wiki_access.TYPES.OWNER, self.user_chapson)
        cluster = Cluster('chapson')
        r = cluster.move({'testinfo/testinfogem': 'search/testinfogem'})
        self.assertEqual(r[0], 200)
        testinfogem = Page.objects.get(supertag='search/testinfogem')
        access = interpret_raw_access(get_raw_access(testinfogem.supertag))
        self.assertEqual(access['is_inherited'], False)
        self.assertEqual(access['is_common'], True)

    def test_page_watch(self):
        set_user_setting(self.user, 'new_subscriptions', False)

        page = Page.objects.get(supertag='search')
        watch = PageWatch(user=self.user.username, page=page, is_cluster=True)
        watch.save()
        moved_page = Page.objects.get(supertag='testinfo/testinfogem')
        before = PageWatch.objects.filter(page=moved_page).count()
        self.cluster.move({moved_page.supertag: 'search/nyanyanya'})
        after = PageWatch.objects.filter(page=moved_page).count()
        self.assertEqual(before + 1, after, 'Add 1 page watch')

    def test_move_page_without_children(self):
        to_move = self.create_page(supertag='moveme', tag='MoveMe')
        clash = 'nosubclash'
        possible_subclash = self.create_page(
            supertag=to_move.supertag + '/' + clash,
            tag='MoveMe/' + clash,
        )
        where_to_move = 'vacantplace'
        reason_for_subclash = self.create_page(
            supertag=where_to_move + '/' + clash,
            tag=where_to_move + '/' + clash,
        )
        result = self.cluster.move({to_move.supertag: where_to_move}, with_children=False)
        self.assertEqual(result[0], 200, 'A correct operation')

    def test_revision_is_created(self):
        from wiki.pages.models import Revision

        to_move = self.create_page(supertag='moveme', tag='MoveMe')
        where_to_move = 'vacantplace'
        self.cluster.move({to_move.supertag: where_to_move})
        self.assertEqual(Revision.objects.filter(page__supertag='moveme').count(), 1)

    def test_relative2absolute_with_children(self):
        """
        Переносим вместе с страницу testinfo/gem в metacluster/super/gem
        """
        self.client.login('thasonic')
        _from = 'testinfo/gem'
        destination = 'metacluster/super/gem'
        save_page(_from, EXAMPLE_TEXT, user=User.objects.get(username='thasonic'))
        status, _ = move_clusters(user=self.user_thasonic, clusters={_from: destination}, with_children=True)

        self.assertEqual(200, status)

        page = Page.objects.get(supertag=destination)

        body_lines = page.body.splitlines()
        move_lines = MOVE_WITH_CHILDREN.splitlines()
        self.assertEqual(len(body_lines), len(move_lines))

        for i in range(len(body_lines)):
            self.assertEqual(body_lines[i], move_lines[i])

    def test_relative2absolute_with_no_children(self):
        """
        Переносим вместе с страницу testinfo/gem в metacluster/super/gem
        """
        self.client.login('thasonic')
        _from = 'testinfo/gem'
        destination = 'metacluster/super/gem'
        save_page('testinfo/gem', EXAMPLE_TEXT, user=User.objects.get(username='thasonic'))
        status, _ = move_clusters(user=self.user_thasonic, clusters={_from: destination}, with_children=False)

        self.assertEqual(200, status)

        page = Page.objects.get(supertag=destination)

        body_lines = page.body.splitlines()
        move_lines = MOVE_WITHOUT_CHILDREN.splitlines()
        self.assertEqual(len(body_lines), len(move_lines))

        for i in range(len(body_lines)):
            self.assertEqual(body_lines[i], move_lines[i])

    def test_blackbox(self):
        """
        Тестируемый код использует START TRANSACTION и поэтому он СЛУЧАЙНО может недоудалять после себя данные.
        Имейте ввиду."""
        _from = 'testinfo'
        testinfo = Page.objects.get(supertag=_from)
        destination = 'metacluster/super/gem'
        self.client.login('chapson')
        self.assertEqual(PageEvent.objects.filter(page=testinfo).count(), 0)
        status, _ = move_clusters(user=self.user_chapson, clusters={_from: destination}, with_children=False)

        self.assertEqual(200, status)
        self.assertEqual(PageEvent.objects.filter(page=testinfo).count(), 1)
        event = PageEvent.objects.get(page=testinfo)
        self.assertEqual(event.meta['with_children'], False)
        self.assertEqual(event.meta['move_supertag_to_tag'], {_from: destination})

    def test_wiki_3073(self):
        """Must correctly move neighbour links"""
        top_cluster_from = 'arnoldsnegovojj/wikilinkstest/clustermove'
        top_cluster_to = 'arnoldsnegovojj/wikilinkstest/target/clustermove'
        _from = top_cluster_from + '/page1'
        destination = top_cluster_to + '/page1'
        test_sample = 'Текст ((page2 страница2)) and more ((../clustermove clustermove))'
        page = self.create_page(tag=_from, supertag=_from, body=test_sample)
        result, has_changes = straighten_relative_links(
            page.body,
            self.user_chapson,
            _from,
            destination,
            top_cluster_from,
            top_cluster_to,
            with_children=True,
        )
        expected_result = 'Текст ((/arnoldsnegovojj/wikilinkstest/target/clustermove/page2 страница2)) and more ((/arnoldsnegovojj/wikilinkstest/target/clustermove clustermove))'  # noqa
        self.assertEqual(expected_result, result)

    def test_tag_into_supertag_dljasapportov(self):
        """Page and subpage must be correctly moved into /support/
        Got two pages:
           1) tag="ДляСаппортов", supertag="dljasapportov"
           2) tag="dljasapportov/subpage", supertag="dljasapportov/subpage"
        The top page is moved to /support.
        The subpage must be moved, and it's tag must be "support/subpage" """
        top_page = self.create_page(tag='ДляСаппортов', supertag='dljasapportov')
        subpage = self.create_page(tag='dljasapportov/subpage', supertag='dljasapportov/subpage')
        self.client.login('thasonic')
        status, _ = move_clusters(user=self.user_thasonic, clusters={top_page.supertag: 'support'}, with_children=True)

        self.assertEqual(200, status)
        support = Page.objects.get(tag='support')
        qs = Page.objects.filter(tag='support/subpage')
        self.assertEqual(1, qs.count(), "There must be page: page.tag=='support/subpage' ")
        support_subpage = qs[0]

    def test_absolute_link_on_subpage(self):
        top_cluster = 'users/chapson/pagetomove'
        move_to = 'users/chapson/getsmoved/here'
        test_sample = '((/users/chapson/pagetomove/page1 link to another page))'
        must_be = '((/users/chapson/getsmoved/here/page1 link to another page))'
        self.create_page(tag=top_cluster)
        self.create_page(tag=top_cluster + '/page1')
        subpage_suffix = '/subpage'
        self.create_page(tag=top_cluster + subpage_suffix, body=test_sample)
        self.client.login('thasonic')
        status, _ = move_clusters(user=self.user_thasonic, clusters={top_cluster: move_to}, with_children=True)
        self.assertEqual(200, status)
        page = Page.objects.get(supertag=move_to + subpage_suffix)
        self.assertEqual(must_be, page.body)

    def test_wiki_3308(self):
        """Links /home/files  Links /home/filesLinks /home/files?get=afile.jpg -> file:afile.jpg"""
        TEXT_WITH_FILES = """text /dljasapportov/subpage/files?get=afile.jpg endtext"""
        top_page = self.create_page(tag='ДляСаппортов', supertag='dljasapportov')
        top_page.body = TEXT_WITH_FILES
        top_page.save()
        subpage = self.create_page(tag='dljasapportov/subpage', supertag='dljasapportov/subpage')
        subpage.body = TEXT_WITH_FILES
        subpage.save()
        self.client.login('thasonic')
        status, _ = move_clusters(user=self.user_thasonic, clusters={top_page.supertag: 'support'}, with_children=True)
        self.assertEqual(200, status)

    def test_reserved_tags(self):
        supertag_users_chapson = 'users/chapson'
        create_personal_page(self.user_chapson)

        self.client.login('chapson')
        self.assertTrue(is_reserved_supertag(supertag_users_chapson, Action.CREATE))
        self.assertTrue(is_reserved_supertag(supertag_users_chapson, Action.DELETE))
        status, _ = move_clusters(
            user=self.user_chapson, clusters={supertag_users_chapson: 'chapsonpage'}, with_children=True
        )
        self.assertEqual(409, status)


EXAMPLE_TEXT = (
    """Это текст для теста.
Образцы ссылок с круглыми скобками ((!/DirectSubpage)) и еще ((!/aaa not in the link)), ((../PagezSubpagez)), ((Page2 a link))
Образцы ссылок с квадратными скобками [[!/DirectSubpage]] и еще [[!/aaa not in the link]], [[../PagezSubpagez]], [[Page2 a link]]

это абсолютные ссылки ((/Absolute/link an absolute link)) [[/Absolute/link an absolute link]]. Они остаются без изменений.

это ссылка в тексте !/DirectSubpage ../PagezSubpagez /Absolute/link

ссылка на файл с котиками file:funnycats.jpg и без котиков ((file:nocats.jpg без котиков))

Образец вызова экшена {{tree for="!/DirectSubpage" page='../PagezSubpagez' root="/testinfo/gem/path" and a few symbols left}}

Ссылки внешние http://yandex.ru ((http://yandex.ru/ Яндекс)) [[http://yandex.ru/ Яндекс]] ((http://yandex.ru/)) [[http://yandex.ru/]]
Ссылки на почтовые адреса ((mailto:%s tools))

((http Похожая на внешнюю ссылка))
"""
    % settings.SUPPORT_EMAIL
)

MOVE_WITHOUT_CHILDREN = (
    """Это текст для теста.
Образцы ссылок с круглыми скобками ((/testinfo/gem/DirectSubpage)) и еще ((/testinfo/gem/aaa not in the link)), ((/PagezSubpagez)), ((/testinfo/Page2 a link))
Образцы ссылок с квадратными скобками ((/testinfo/gem/DirectSubpage)) и еще ((/testinfo/gem/aaa not in the link)), ((/PagezSubpagez)), ((/testinfo/Page2 a link))

это абсолютные ссылки ((/Absolute/link an absolute link)) [[/Absolute/link an absolute link]]. Они остаются без изменений.

это ссылка в тексте !/DirectSubpage ../PagezSubpagez /Absolute/link

ссылка на файл с котиками file:funnycats.jpg и без котиков ((file:nocats.jpg без котиков))

Образец вызова экшена {{tree for="!/DirectSubpage" page='../PagezSubpagez' root="/testinfo/gem/path" and a few symbols left}}

Ссылки внешние http://yandex.ru ((http://yandex.ru/ Яндекс)) [[http://yandex.ru/ Яндекс]] ((http://yandex.ru/)) [[http://yandex.ru/]]
Ссылки на почтовые адреса ((mailto:%s tools))

((/testinfo/http Похожая на внешнюю ссылка))
"""
    % settings.SUPPORT_EMAIL
)

MOVE_WITH_CHILDREN = (
    """Это текст для теста.
Образцы ссылок с круглыми скобками ((/metacluster/super/gem/DirectSubpage)) и еще ((/metacluster/super/gem/aaa not in the link)), ((/PagezSubpagez)), ((/testinfo/Page2 a link))
Образцы ссылок с квадратными скобками ((/metacluster/super/gem/DirectSubpage)) и еще ((/metacluster/super/gem/aaa not in the link)), ((/PagezSubpagez)), ((/testinfo/Page2 a link))

это абсолютные ссылки ((/Absolute/link an absolute link)) [[/Absolute/link an absolute link]]. Они остаются без изменений.

это ссылка в тексте !/DirectSubpage ../PagezSubpagez /Absolute/link

ссылка на файл с котиками file:funnycats.jpg и без котиков ((file:nocats.jpg без котиков))

Образец вызова экшена {{tree for="!/DirectSubpage" page='../PagezSubpagez' root="/testinfo/gem/path" and a few symbols left}}

Ссылки внешние http://yandex.ru ((http://yandex.ru/ Яндекс)) [[http://yandex.ru/ Яндекс]] ((http://yandex.ru/)) [[http://yandex.ru/]]
Ссылки на почтовые адреса ((mailto:%s tools))

((/testinfo/http Похожая на внешнюю ссылка))
"""
    % settings.SUPPORT_EMAIL
)
