# coding: utf-8

import pytest

from at.common import exceptions
from at.aux_.entries.repositories import mysql_repository

from tests import base

# pytestmark = pytest.mark.skip('outdated, needs fixing')
pytestmark = [pytest.mark.skip('MYSQL specific'), pytest.mark.django_db]


class TestMysqlRepo(base.BaseRepoTestCase):
    """
    Тестируем сохранение и редактирование постов и комментариев.
    Тесты используют MysqlRepository напрямую, вызывая save и
    передавая в него инстанс модели.
    Это ближе к юнит-тестам, но они используют базу, иначе мало что
    получится проверить.

    Нулевая итерация (пока не имплеменчен load)
        (x) новый пост создается в базе без эксепшна
        (x) новый обычный комент без родителя создается без эксепшна
        (x) новый трэкбек комент без родителя создается без эксепшна
        (x) существующий пост редактируется без эксепшна
        (x) существующий коммент редактируется без эксепшна
        (x) новый комент с родителем создается без ошибок
        (x) комент с родителелем редактируется без ошибок
        (x) пост удаляется без ошибок (просто deleted=True)
        (x) комент без родителя удаляется без ошибок (deleted=True)
        (x) комент с родителем удаляется без ошибок (пересчет счетчиков)
        (x) не забываем удалять созданное после теста

        TODO:  Warning: Data truncated for column 'store_time_month_year' at row 1

    Первая итерация
        ( ) в модели созданного поста есть все поля с ожидаемыми значениями
        ( ) при редактировании поста изменяются нужные поля, остальные какие были
        (x) при запросе удаленного поста load кидает NotFound
        ( ) в модели созданного комента есть все поля с ожидаемыми значениями
    """

    def test_create_post(self):
        post = self._build_entry()

        self._save(post)
        self.assertTrue(post.item_no)

        loaded_post = self._load(post.feed_id, post.item_no)

        expected = self.entry_defaults.copy()
        del expected['form_id']
        del expected['body']
        # expected['body_original'] = expected.pop('body')
        expected.update(
            item_no=self.item_no,
            store_time=self.entry_defaults['store_time'].replace(microsecond=0),
            # edit_time=self.entry_defaults['store_time'].replace(microsecond=0),
            item_time=self.entry_defaults['item_time'].replace(microsecond=0),
        )

        self._assert_fields(obj=loaded_post, expected=expected)

    def test_edit_post(self):
        # неясно как проще сохранить записи про пост в базе
        post = self._build_entry()
        self._save(post)

        post.item_time = self.entry_new_values['item_time']
        post.access_type = self.entry_new_values['access_type']
        post.rubric_id = self.entry_new_values['rubric_id']
        post.block_comments = self.entry_new_values['block_comments']
        self._save(post)

        loaded_post = self._load(post.feed_id, post.item_no)

        expected = self.entry_defaults.copy()
        del expected['form_id']

        del expected['body']
        # expected['body_original'] = expected.pop('body')
        expected.update(self.entry_new_values)
        expected.update(
            item_no=self.item_no,
            store_time=self.entry_defaults['store_time'].replace(microsecond=0),
            item_time=self.entry_new_values['item_time'].replace(microsecond=0),
            # edit_time=self.entry_defaults['store_time'].replace(microsecond=0),
        )
        self._assert_fields(obj=loaded_post, expected=expected)

        # TODO: check edited tags
        # TODO: check edited xml?

    def test_delete_post(self):
        post = self._build_entry()
        self._save(post)

        post.mark_deleted()
        self.assertTrue(post.is_deleted_manually())

        self._save(post)

        self.assertRaises(exceptions.NotFound, self._load, post.feed_id, post.item_no)

    def test_create_simple_first_comment(self):
        post = self._build_entry()
        self._save(post)

        comment = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=post.item_no,
            
        )
        self._save(comment)

    def test_create_trackback_first_comment(self):
        post = self._build_entry()
        self._save(post)

        comment = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=post.item_no,
            
            do_trackback=True,
            tb_feed_id=self.furry_ezhik_uid,
            tb_rubric_id=5,
        )
        self._save(comment)

    def test_edit_comment(self):
        post = self._build_entry()
        self._save(post)

        comment = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=post.item_no,
            
        )
        self._save(comment)

        comment.type = 'status'
        self._save(comment)

    def test_edit_trackback_post(self):
        post = self._build_entry()
        self._save(post)

        comment = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=post.item_no,
            
            do_trackback=True,
            tb_feed_id=self.furry_ezhik_uid,
            tb_rubric_id=5,
        )
        self._save(comment)

        tb_post = self._build_entry(
            feed_id=self.furry_ezhik_uid,
            item_no=comment.tb_item_no,
            reply_to_feed_id=comment.feed_id,
            reply_to_item_no=comment.item_no,
            reply_to_comment_id=comment.comment_id
        )
        self._save(tb_post)

    def test_create_child_comment(self):
        post = self._build_entry()
        self._save(post)

        comment = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=post.item_no,
            
        )
        self._save(comment)

        child_comment = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=comment.comment_id,
            
        )
        self._save(child_comment)

    def test_edit_child_comment(self):
        post = self._build_entry()
        self._save(post)

        comment = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=post.item_no,
            
        )
        self._save(comment)

        child_comment = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=comment.comment_id,
            
        )
        self._save(child_comment)

        child_comment.comment_type = 'status'
        self._save(child_comment)

    def test_delete_comment(self):
        post = self._build_entry()
        self._save(post)

        comment = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=post.item_no,
            
        )
        self._save(comment)

        comment.is_deleted = True
        self._save(comment)

    def test_delete_child_comment(self):
        post = self._build_entry()
        self._save(post)

        comment = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=post.item_no,
            
        )
        self._save(comment)

        child_comment = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=comment.comment_id,
            
        )
        self._save(child_comment)

        child_comment.is_deleted = True
        self._save(child_comment)

    def test_load_comment_if_not_exists(self):
        repo = mysql_repository.MysqlRepository()
        self.assertRaises(
            exceptions.NotFound,
            lambda : repo.load(
                feed_id=0,
                item_no=0,
                comment_id=0
            )
        )

    def test_load_comment(self):
        post = self._build_entry()
        self._save(post)

        comment = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=0,
            
        )
        self._save(comment)

        repo = mysql_repository.MysqlRepository()
        try:
            comment_model = repo.load(
                feed_id=self.good_ezhik_uid,
                item_no=post.item_no,
                comment_id=comment.comment_id,
            )
        except exceptions.NotFound as exc:
            self.assertTrue(False, 'not found this comment model ' + repr(exc))

        self.assertEqual(comment_model.feed_id, self.good_ezhik_uid)
        self.assertEqual(comment_model.item_no, self.item_no)
        self.assertEqual(comment_model.comment_id, self.first_comment_id)

    def _create_comment_tree(self):
        """

        @return: post, parent_comment, it's child1, it's child2
        """
        post = self._build_entry()
        self._save(post)

        """
        Вот что я загружаю:
        post
         |___ comment
               |______comment1
               |______comment2
        """
        parent_comment = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=post.item_no,
            
        )
        self._save(parent_comment)
        child_comment_1 = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=parent_comment.comment_id,
            
        )
        self._save(child_comment_1)
        child_comment_2 = self._build_entry(
            item_no=post.item_no,
            parent_comment_id=parent_comment.comment_id,
            
        )
        self._save(child_comment_2)
        return post, parent_comment, child_comment_1, child_comment_2

    def test_load_post(self):
        post = self._build_entry()
        self._save(post)

        repo = mysql_repository.MysqlRepository()
        try:
            post_model = repo.load(
                feed_id=self.good_ezhik_uid,
                item_no=post.item_no,
            )
        except exceptions.NotFound as exc:
            self.assertTrue(False, 'not found this comment model ' + repr(exc))
        self.assertEqual(post_model, post)
