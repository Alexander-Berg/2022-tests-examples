from copy import copy, deepcopy
from datetime import timedelta

from django.contrib.auth import get_user_model

from wiki.pages.models import Comment, Page
from wiki.utils import timezone
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase

# Тестовое дерево комментариев
# Структура:
#
#                      с_2_5 -- c_3_4(последний по времени)
#                    /
# с_0_11  -- c_1_10  -- c_6_7
#                    \
#                     c_8_9
#
# с_12_15 -- c_13_14
#
#
# c_16_17

test_comments_struct = [
    {
        'id': 20,
        'comments': [
            {
                'id': 21,
                'comments': [
                    {
                        'id': 22,
                        'comments': [{'id': 23, 'comments': [], 'created_at': timezone.now() + timedelta(minutes=1)}],
                    },
                    {'id': 26, 'comments': []},
                    {'id': 28, 'comments': []},
                ],
            }
        ],
    },
    {'id': 32, 'comments': [{'id': 33, 'comments': []}]},
    {'id': 36, 'comments': []},
]


class CommentsTest(BaseTestCase):
    """
    Тесты для комментариев
    """

    test_comment_data = {'body': 'test comment, ((yandex.ru yandex))'}

    def setUp(self):
        super(CommentsTest, self).setUp()
        self.user = self.login('thasonic')
        self.create_page(tag='testpage', body='test page')

    def _add_comment(self, parent=None, username='thasonic', **kwargs):
        data = copy(self.test_comment_data)
        data['page'] = Page.objects.get(supertag='testpage')
        data['user'] = get_user_model().objects.get(username=username)
        data['page_at'] = data['page'].modified_at

        if parent:
            data['parent'] = parent

        data.update(kwargs)

        comment = Comment(**data)
        comment.save()

        return comment

    def _add_test_comments_tree(self, nodes, parent_id=None):

        for node in nodes:
            children = node['comments']
            del node['comments']
            node['parent_id'] = parent_id

            self._add_comment(**node)
            self._add_test_comments_tree(children, parent_id=node['id'])

    def _prepage_comments_tree(self):
        self._add_test_comments_tree(deepcopy(test_comments_struct))

        page = Page.objects.get(supertag='testpage')
        comments = Comment.get_page_comments(page)

        return Comment.make_tree(comments[:])

    def _check_comments_tree(self, children=None, parent=None, full_list=None):
        """
        Рекурсивно пробегая по дереву проверяет, что все дети ссылаются на родителя через parent_id
        Детьми считаются объекты из списка в поле comments объекта
        """
        self.assertEqual(len([child for child in children if child.parent_id != (parent.id if parent else None)]), 0)

        for child in children:
            self.assertTrue(
                child in full_list, msg='comment id={0} must be in comments list {1}'.format(child.id, full_list)
            )
            full_list.remove(child)
            self._check_comments_tree(child.comments, child, full_list=full_list)

    def test_getting_page_comments(self):
        comm1 = self._add_comment()
        comm2 = self._add_comment(parent=comm1)
        comm3 = self._add_comment()

        comm3.status = False
        comm3.save()

        page = Page.objects.get(supertag='testpage')
        comments_list = Comment.get_page_comments(page)

        # сначала комментарии без родителя, затем - ребенок
        self.assertEqual((comm1, comm3, comm2), tuple(comments_list))

    def test_set_comments_deletion_flag(self):
        # 1e два коммента от засоника
        comm1 = self._add_comment()
        self._add_comment(comm1)

        # 3й коммент создан чапсоном
        self.login('chapson')
        self._add_comment(username='chapson')
        page = Page.objects.get(supertag='testpage')

        comments = Comment.get_page_comments(page)

        Comment.populate_comments_deletion_flag(list(comments), self.user)

        # можно удалять лишь свои комменты, если не админ
        self.assertEqual([True, False, True], [c.may_be_deleted for c in comments])

        # admin mode
        Comment.populate_comments_deletion_flag(list(comments), self.user, True)
        self.assertEqual([True, True, True], [c.may_be_deleted for c in comments])

    def test_update_comments_count(self):

        # 2 коммента от засоника
        comm1 = self._add_comment()
        comm2 = self._add_comment(comm1)

        page = Page.objects.get(supertag='testpage')

        self.assertEqual(page.comments, 2)

        comm2.status = False
        comm2.save()

        page = Page.objects.get(supertag='testpage')
        # остался один комментарий
        self.assertEqual(page.comments, 1)

        # удалили корневой коммент
        comm1.status = False
        comm1.save()

        comm2.status = True
        comm2.save()

        page = Page.objects.get(supertag='testpage')
        # все равно активных комментариев один - хотя в верстке и покажется сначала "комментарий удален",
        # а за ним 2й коммент
        self.assertEqual(page.comments, 1)
