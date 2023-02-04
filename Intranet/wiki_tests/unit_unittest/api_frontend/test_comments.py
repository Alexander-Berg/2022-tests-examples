from django.conf import settings

from wiki.pages.models import Comment
from wiki.pages.models.consts import COMMENTS_STATUS
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


def add_comment(page, user, **kwargs):
    data = {
        'page': page,
        'user': user,
        'body': 'comment body',
        'page_at': page.modified_at,
    }
    data.update(kwargs)
    return Comment.objects.create(**data)


class APICommentListTest(BaseApiTestCase):
    TEST_COMMENT_ONE = 'nice page, bro'
    TEST_COMMENT_TWO = 'thanks'
    handler = 'comments'

    def setUp(self):
        super(APICommentListTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

        self.page_one = self.create_page(tag='one', body='one body')
        self.page_two = self.create_page(tag='two', body='two body')
        self.default_page = self.page_one

    def test_get_comments_list(self):
        comment_one = add_comment(self.page_one, self.user_thasonic)
        comment_two = add_comment(self.page_one, self.user_thasonic)

        data = self.get(status=200)

        self.assertEqual(len(data), 2)
        comment_ids = [c['id'] for c in data]
        self.assertIn(comment_one.id, comment_ids)
        self.assertIn(comment_two.id, comment_ids)

    def test_get_comments_list_paginated(self):
        add_comment(self.page_one, self.user_thasonic)
        add_comment(self.page_one, self.user_thasonic)

        query_params = {'start': 0, 'limit': 1}
        assert_queries = 50 if not settings.WIKI_CODE == 'wiki' else 8
        with self.assertNumQueries(assert_queries):
            data = self.get(
                query_params=query_params,
                status=200,
            )

        self.assertEqual(len(data), 1)

        query_params = {'start': 1, 'limit': 1}
        data = self.get(
            query_params=query_params,
            status=200,
        )
        self.assertEqual(len(data), 1)

    def test_post_comment_no_parent(self):
        params = {'body': self.TEST_COMMENT_ONE}
        self.post(status=200, data=params)

        self.assertTrue(
            Comment.objects.filter(
                page=self.page_one,
                body=self.TEST_COMMENT_ONE,
                parent_id=None,
            ).exists()
        )

    def test_post_comment_with_parent(self):
        comment = add_comment(self.page_one, self.user_thasonic)

        params = {'body': self.TEST_COMMENT_TWO, 'parent': comment.id}
        self.post(status=200, data=params)

        self.assertTrue(
            Comment.objects.filter(
                page=self.page_one,
                body=self.TEST_COMMENT_TWO,
                parent=comment,
            ).exists()
        )

    def test_post_comment_with_unknown_parent(self):
        params = {'body': self.TEST_COMMENT_TWO, 'parent': 100500}
        error = self.post(status=409, data=params)

        self.assertIn('parent', error['errors'])

    def test_post_comment_with_parent_wrong_page(self):
        comment = add_comment(self.page_two, self.user_thasonic)

        params = {'body': self.TEST_COMMENT_TWO, 'parent': comment.id}
        error = self.post(status=409, data=params)

        self.assertIn('parent', error['errors'])


class APICommentsDetailTest(BaseApiTestCase):
    handler = 'comments/{}'

    def setUp(self):
        super(APICommentsDetailTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

        self.page_one = self.create_page(tag='one', body='one body')
        self.default_page = self.page_one

    def test_edit_comment(self):
        comment = add_comment(self.page_one, self.user_thasonic)

        params = {'body': 'comment_fixed'}
        self.post(placeholders=[comment.id], status=200, data=params)

        self.assertTrue(
            Comment.objects.filter(
                id=comment.id,
                page=self.page_one,
                body='comment_fixed',
            ).exists()
        )

    def test_delete_comment(self):
        comment = add_comment(self.page_one, self.user_thasonic)

        self.delete(placeholders=[comment.id], status=200)

        self.assertTrue(
            Comment.objects.filter(
                id=comment.id,
                page=self.page_one,
                status=False,
            ).exists()
        )


class APICommentsStatusTest(BaseApiTestCase):
    TEST_COMMENT_ONE = 'Hello, world'
    TEST_COMMENT_TWO = 'Hi, everybody'
    handler = 'comments/status'

    def setUp(self):
        super(APICommentsStatusTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

        self.page1 = self.create_page(tag='one', body='one body')
        self.page2 = self.create_page(tag='two', body='two body')
        self.page11 = self.create_page(tag='one/one', body='one/one body')
        self.page12 = self.create_page(tag='one/two', body='one/two body')
        self.default_page = self.page1

    def test_get_comments_status(self):
        data = self.get(status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.enabled])
        self.assertEqual(data['subpages_count'], 2)

    def test_disable_comments_status_for_page(self):
        params = {'disabled': True, 'for_cluster': False}
        self.post(status=200, data=params)

        data = self.get(status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.disabled_on_page])

        data = self.get(page=self.page11, status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.enabled])

        data = self.get(page=self.page2, status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.enabled])

    def test_disable_comments_status_for_cluster(self):
        params = {'disabled': True, 'for_cluster': True}
        self.post(status=200, data=params)

        data = self.get(status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.disabled_on_cluster])

        data = self.get(page=self.page11, status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.disabled_on_cluster])

        data = self.get(page=self.page12, status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.disabled_on_cluster])

        data = self.get(page=self.page2, status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.enabled])

    def test_enable_comments_status_for_page(self):
        params = {'disabled': True, 'for_cluster': False}
        self.post(status=200, data=params)

        data = self.get(status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.disabled_on_page])

        params = {'disabled': False, 'for_cluster': False}
        self.post(status=200, data=params)

        data = self.get(status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.enabled])

        data = self.get(page=self.page11, status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.enabled])

        data = self.get(page=self.page2, status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.enabled])

    def test_enable_comments_status_for_cluster(self):
        params = {'disabled': True, 'for_cluster': True}
        self.post(status=200, data=params)

        data = self.get(status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.disabled_on_cluster])

        params = {'disabled': False, 'for_cluster': True}
        self.post(status=200, data=params)

        data = self.get(status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.enabled])

        data = self.get(page=self.page11, status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.enabled])

        data = self.get(page=self.page12, status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.enabled])

        data = self.get(page=self.page2, status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.enabled])

    def test_try_add_comments_if_comments_are_disabled(self):
        self.handler = 'comments'
        params = {'body': self.TEST_COMMENT_ONE}
        self.post(status=200, data=params)

        self.handler = 'comments/status'
        params = {'disabled': True, 'for_cluster': False}
        self.post(status=200, data=params)

        self.handler = 'comments'
        params = {'body': self.TEST_COMMENT_TWO}
        self.post(status=409, data=params)

    def test_access_to_change_comments_status(self):
        # комментарии отключать может только владелец страницы
        self.client.login('chapson')
        params = {'disabled': True, 'for_cluster': False}
        self.post(status=403, data=params)

        # внутри кластера изменение статуса комментариев применяется только к страницам владельца кластера
        self._default_user = self.user_chapson
        page13 = self.create_page(user=self.user_chapson, tag='one/three', body='one/three body')

        self._default_user = self.user_thasonic
        self.client.login('thasonic')
        params = {'disabled': True, 'for_cluster': True}
        self.post(status=200, data=params)

        data = self.get(status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.disabled_on_cluster])

        data = self.get(page=self.page11, status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.disabled_on_cluster])

        data = self.get(page=self.page12, status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.disabled_on_cluster])

        data = self.get(page=page13, status=200)
        self.assertEqual(data['comments_status'], COMMENTS_STATUS[COMMENTS_STATUS.disabled_on_cluster])
