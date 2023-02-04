# coding: utf-8



import hashlib
import unittest
from datetime import datetime

import pytest

from at.common.utils import get_connection

pytestmark = pytest.mark.django_db

from at.aux_.entries import models
from at.common import utils


class BaseRepoTestCase(unittest.TestCase):
    good_ezhik_login = 'robot-good-ezhik'
    good_ezhik_uid = 1120000000023943
    good_ezhik_ai = utils.getAuthInfo(
        uid=good_ezhik_uid,
        login=good_ezhik_login,
    )
    furry_ezhik_login = 'robot-furry-ezhik'
    furry_ezhik_uid = 1120000000023944
    furry_ezhik_ai = utils.getAuthInfo(
        uid=furry_ezhik_uid,
        login=furry_ezhik_login,
    )
    last_item_no = 999
    item_no = last_item_no + 1
    first_comment_id = item_no + 1
    second_comment_id = first_comment_id + 1
    third_comment_id = second_comment_id + 1

    entry_defaults = {
        'type': 'text',  # TODO: другие типы фейлятся на сериализации
        'feed_id': good_ezhik_uid,
        'store_time': datetime(2020, 1, 1, 10, 15, 20, 350),
        'item_time': datetime(2020, 2, 2, 11, 16, 21, 450),
        'access_type': 'public',
        # TODO: убрать form_id из модели — ее нужно использовать
        # на уровне Entries
        'form_id': 'xxxxxx',
        'author_id': good_ezhik_uid,
        'body': '**Hello world**',
        'content_type': 'text/wiki',
        'deleted': False,
        'block_comments': False,
        'children_count': 0,
        'on_moderation': False,
    }
    # store_time_month_year — через sql, а как он используется?

    entry_new_values = {
        'item_time': datetime(2030, 1, 1, 10, 15, 20, 250),
        'access_type': 'all_friends',
        'block_comments': True,
    }

    def setUp(self):
        self._set_last_item_no(feed_id=self.good_ezhik_uid, db='at')
        self._create_tags(feed_id=self.good_ezhik_uid, db='at')

    def tearDown(self):
        db = 'at'
        self._delete_last_item_no(feed_id=self.good_ezhik_uid, db=db)

        post_params = {
            'feed_id': self.good_ezhik_uid,
            'item_no': self.item_no,
        }
        self._delete_post_meta(db=db, **post_params)
        self._delete_post_content(db=db, **post_params)
        self._delete_post_store_data(
            db=db, form_id=self.entry_defaults['form_id'], **post_params)
        self._delete_post_accesses(db=db, **post_params)
        self._delete_post_tags(db=db, **post_params)
        self._delete_comment_meta(db=db, **post_params)
        self._delete_comment_content(db=db, **post_params)
        self._delete_comment_children_count(db=db, **post_params)
        self._delete_comment_full_parents(db=db, **post_params)
        self._delete_comment_trackback_info(db=db, **post_params)

    def _set_last_item_no(self, feed_id, item_no=None, db='at'):
        item_no = item_no or self.last_item_no
        query = """
            INSERT INTO ItemsBulcaIncrementer
            (
                feed_id,
                last_item_no
            )
            VALUES
            (
                :feed_id,
                :item_no
            )
        """
        with get_connection() as connection:
            connection.execute(query, {
                'feed_id': feed_id,
                'item_no': item_no,
            })

    def _create_tags(self, feed_id, db='at'):
        query = """
            INSERT INTO PostCategory
            (
                person_id,
                title_tag
            )
            VALUES
            (
                :feed_id,
                :tag
            )
        """
        with get_connection() as connection:
            connection.execute(query, {
                'feed_id': feed_id,
                'tag': '\'test_tag_one\'',
            })
            connection.execute(query, {
                'feed_id': feed_id,
                'tag': '\'test_tag_two\'',
            })

    def _get_tags_ids(self):
        from at.aux_ import Tags
        tags_ctl = Tags.Tags()
        tag_one_id = tags_ctl.getTagByName(self.good_ezhik_uid, '\'test_tag_one\'')
        tag_two_id = tags_ctl.getTagByName(self.good_ezhik_uid, '\'test_tag_two\'')
        return [tag_one_id, tag_two_id]

    def _delete_last_item_no(self, feed_id, db='at'):
        query = """
            DELETE FROM ItemsBulcaIncrementer
            WHERE feed_id = :feed_id
        """
        with get_connection() as connection:
            connection.execute(query, {
                'feed_id': feed_id,
            })

    def _delete_post_meta(self, feed_id, item_no, db='at'):
        query = """
            DELETE FROM Posts
            WHERE
                person_id = :feed_id AND
                post_no = :item_no
        """
        with get_connection() as connection:
            connection.execute(query, {
                'feed_id': feed_id,
                'item_no': item_no,
            })

    def _delete_post_content(self, feed_id, item_no, db='at'):
        query = """
            DELETE FROM EntryXmlContent
            WHERE
                feed_id = :feed_id AND
                post_no = :item_no
        """
        with get_connection() as connection:
            connection.execute(query, {
                'feed_id': feed_id,
                'item_no': item_no,
            })

    def _delete_post_store_data(self, feed_id, item_no, form_id, db='at'):
        query = """
            DELETE FROM PostStoreData
            WHERE
                feed_id = \':feed_id \' AND
                (item_no = \':item_no\' OR link_md5 = \':link_md5\')
        """

        with get_connection() as connection:
            connection.execute(query, {
                'feed_id': feed_id,
                'item_no': item_no,
                'link_md5': str(hashlib.md5(form_id.encode('utf-8')).digest(), 'utf16')
            })

    def _delete_post_accesses(self, feed_id, item_no, db='at'):
        query = """
            DELETE FROM PostAccesses
            WHERE
                feed_id = :feed_id AND
                post_no = :item_no
        """
        with get_connection() as connection:
            connection.execute(query, {
                'feed_id': feed_id,
                'item_no': item_no,
            })

    def _delete_post_tags(self, feed_id, item_no, db='at'):
        query = """
            DELETE FROM PostCategories
            WHERE
                feed_id = :feed_id AND
                post_no = :item_no
        """
        with get_connection() as connection:
            connection.execute(query, {
                'feed_id': feed_id,
                'item_no': item_no,
            })

    def _delete_comment_meta(self, feed_id, item_no, db='at'):
        query = """
            DELETE FROM Comments
            WHERE
                person_id = :feed_id AND
                post_no = :item_no
        """
        with get_connection() as connection:
            connection.execute(query, {
                'feed_id': feed_id,
                'item_no': item_no,
            })

    def _delete_comment_content(self, feed_id, item_no, db='at'):
        query = """
            DELETE FROM EntryXmlContent
            WHERE
                feed_id = :feed_id AND
                post_no = :item_no
        """
        with get_connection() as connection:
            connection.execute(query, {
                'feed_id': feed_id,
                'item_no': item_no,
            })

    def _delete_comment_full_parents(self, feed_id, item_no, db='at'):
        query = """
            DELETE FROM CommentsFullParents
            WHERE
                person_id = :feed_id AND
                post_no = :item_no
        """
        with get_connection() as connection:
            connection.execute(query, {
                'feed_id': feed_id,
                'item_no': item_no,
            })

    def _delete_comment_children_count(self, feed_id, item_no, db='at'):
        query = """
            DELETE FROM CommentsFullParents
            WHERE
                person_id = :feed_id AND
                post_no = :item_no
        """
        with get_connection() as connection:
            connection.execute(query, {
                'feed_id': feed_id,
                'item_no': item_no,
            })

    def _delete_comment_trackback_info(self, feed_id, item_no, db='at'):
        query = """
            DELETE FROM Trackbacks
            WHERE
                person_id = :feed_id AND
                post_no = :item_no
                OR
                trackback_person_id = :feed_id AND
                trackback_post_no = :item_no
        """
        with get_connection() as connection:
            connection.execute(query, {
                'feed_id': feed_id,
                'item_no': item_no,
            })

    def _get_build_params(self, **params):
        fields = self.entry_defaults.copy()
        fields['tags'] = self._get_tags_ids()
        fields.update(params)
        return fields

    def _build_entry(self, **params):
        fields = self._get_build_params(**params)

        model_cls = models.get_post_class(fields['type'])
        # ai = fields.pop('ai', self.good_ezhik_ai)
        model = model_cls(
            feed_id=fields.pop('feed_id'),
            item_no=fields.pop('item_no', None),
            comment_id=fields.pop('comment_id', 0),
        )
        # fields['author_id'] = self.good_ezhik_ai.uid
        for attr, value in list(fields.items()):
            setattr(model, attr, value)
        return model

    def _save(self, model, using='mysql'):
        model.save(using=using)

    def _load(self, feed_id, item_no, comment_id=0, using='mysql'):
        return models.load_entry(feed_id, item_no, comment_id, using=using)

    def _assert_fields(self, obj, expected):
        for field, expected_value in list(expected.items()):
            obj_field_value = getattr(obj, field)
            msg = 'Field `%s` in obj: %s  Expected: %s' % (
                field, obj_field_value, expected_value
            )
            self.assertEqual(obj_field_value, expected_value, msg)
