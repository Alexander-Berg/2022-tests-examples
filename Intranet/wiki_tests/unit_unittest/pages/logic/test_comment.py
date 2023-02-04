
from wiki.pages.logic import comment as comment_logic
from wiki.pages.models import Comment
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class CommentLogicTestCase(BaseTestCase):
    def setUp(self):
        self.page = self.create_page()
        self.user = self.page.get_authors().first()

    def test_add_comment_no_parent(self):
        comment_logic.add_comment(user=self.user, page=self.page, body='wow')

        comment = Comment.objects.get(page=self.page)

        self.assertEqual(comment.user, self.user)
        self.assertEqual(comment.body, 'wow')
        self.assertEqual(comment.status, True)
        self.assertIsNone(comment.parent_id)

    def test_add_comment_parent(self):
        comment_logic.add_comment(user=self.user, page=self.page, body='wow')
        comment = Comment.objects.get(page=self.page)

        answer = comment_logic.add_comment(
            user=self.user,
            page=self.page,
            body='such page',
            parent_id=comment.id,
        )
        answer = self.refresh_objects(answer)

        self.assertEqual(answer.user, self.user)
        self.assertEqual(answer.body, 'such page')
        self.assertEqual(answer.status, True)
        self.assertEqual(answer.parent_id, comment.id)

    def test_edit_comment(self):
        comment_logic.add_comment(user=self.user, page=self.page, body='wow')
        comment = Comment.objects.get(page=self.page)

        comment_logic.edit_comment(comment, user=self.user, body='wow!')
        comment = self.refresh_objects(comment)

        self.assertEqual(comment.body, 'wow!')

    def test_delete_comment(self):
        comment_logic.add_comment(user=self.user, page=self.page, body='wow')
        comment = Comment.objects.get(page=self.page)

        comment_logic.delete_comment(comment, user=self.user)
        comment = self.refresh_objects(comment)

        self.assertEqual(comment.status, False)
