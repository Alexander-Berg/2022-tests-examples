# coding: utf-8

from datetime import datetime
import unittest

import pytest

import at.aux_.models
from at.common.groups import GroupType
from at.common import utils
from at.common import const
from at.aux_.entries import models
from at.aux_ import Mailing
from at.common.utils import get_connection

from tests.test_fixtures import posts_requests

pytestmark = [pytest.mark.skip('MYSQL specific'), pytest.mark.django_db]


class GetMailingTargetsTestCase(unittest.TestCase):
    """
    Тест методы GetMailingTargets, который собирает получателей нотификаций
    для поста или комента.
      * написал пост
        [x] в личном дневнике — фоловеры с настройкой «мгновенный дайджест»
        [x] в клубе — подписчики клуба, фоловеры клуба с настройкой
            «мгновенный дайджест», автор поста, если он подписчик или фоловер
      * написал комент
        [x] ответ на пост — получает автор поста (в зависимости от настройки)
            и подписчика поста (в зависимости от настройки)
        [x] ответ на комент — получает автор поста, автор комента-родителя
            и подписчики поста
        [x] в настройках клуба можно подписаться на все коменты ко всем постам
        [x] автор комента, если есть настройка self_mailing
    """

    users = [
        1120000000020758,  # robot-internal-1
        1120000000020760,  # robot-internal-2
        1120000000020761,  # robot-internal-2
        1120000000020762,  # robot-internal-4
    ]

    club = 4611686018427388702

    # TESTS
    def test_post_diary_no_followers(self):
        # дневник без фоловеров — никто не получает нотификации
        diary_post = self.get_or_create_diary_post()

        targets = self.get_targets(diary_post)

        expected = []
        self.assert_targets_are_expected(targets, expected)

    def test_post_diary_with_follower_no_instant_digest_setting(self):
        # есть фоловер, но у него не стоит настройка
        # «сразу же высылать каждый пост ваших друзей и клубов»
        # — ему не надо слать, он получит пост в дайджесте или нигде
        diary_post = self.get_or_create_diary_post()
        diary_follower = self.get_user(but_not=[diary_post.author_id])
        self.follow_diary(diary_follower, diary_post.feed_id)

        targets = self.get_targets(diary_post)

        expected = []
        self.assert_targets_are_expected(targets, expected)

    def test_post_diary_with_follower_with_instant_digest_setting(self):
        # есть фоловер с настройкой «сразу высылать друзей» — ему шлем
        diary_post = self.get_or_create_diary_post()
        follower = self.get_user(but_not=[diary_post.author_id])
        self.follow_diary(follower, diary_post.feed_id)
        self.enable_instant_digest(uid=follower)

        targets = self.get_targets(diary_post)

        expected = [follower]
        self.assert_targets_are_expected(targets, expected)

        expected = [follower]
        self.assert_targets_are_expected(targets, expected)

    def test_post_diary_with_several_followers(self):
        # есть нетерпеливый фоловер с настройкой «сразу высылать друзей»,
        # а второй без нее — первому шлем
        diary_post = self.get_or_create_diary_post()
        follower_impatient = self.get_user(but_not=[diary_post.author_id])
        follower_patient = self.get_user(
            but_not=[diary_post.author_id, follower_impatient])
        self.follow_diary(follower_patient, diary_post.feed_id)
        self.follow_diary(follower_impatient, diary_post.feed_id)
        self.enable_instant_digest(uid=follower_impatient)

        targets = self.get_targets(diary_post)

        expected = [follower_impatient]
        self.assert_targets_are_expected(targets, expected)

    def test_post_club_with_nobody(self):
        # у клуба нет ни фоловеров, ни подписчиков — никому не шлем
        post_author = self.get_user()
        club_post = self.get_or_create_club_post(author_uid=post_author)

        targets = self.get_targets(club_post)

        expected = []
        self.assert_targets_are_expected(targets, expected)

    def test_post_club_with_subscribers(self):
        # у клуба нет фоловеров, но есть подписчики — им шлем
        post_author = self.get_user()
        club_post = self.get_or_create_club_post(author_uid=post_author)
        subscriber = self.get_user(but_not=[post_author])
        self.subscribe_to_club_posts(uid=subscriber)

        targets = self.get_targets(club_post)

        expected = [subscriber]
        self.assert_targets_are_expected(targets, expected)

    def test_post_club_with_author_subscriber(self):
        # автор получает пост в клбуе как подписчик
        post_author = self.get_user()
        club_post = self.get_or_create_club_post(author_uid=post_author)
        self.subscribe_to_club_posts(uid=post_author)

        targets = self.get_targets(club_post)

        expected = [post_author]
        self.assert_targets_are_expected(targets, expected)

    def test_post_club_with_followers_no_setting(self):
        # у клуба нет подписчиков, но есть фоловер, но без настройки
        # «мгновенный дайджест» — не шлем
        post_author = self.get_user()
        club_post = self.get_or_create_club_post(author_uid=post_author)
        follower = self.get_user(but_not=[post_author])
        self.follow_club(uid=follower)

        targets = self.get_targets(club_post)

        expected = []
        self.assert_targets_are_expected(targets, expected)

    def test_post_club_with_followers_with_setting(self):
        # у клуба нет подписчиков, но есть фоловер c настройкой
        # «мгновенный дайджест» — не шлем
        post_author = self.get_user()
        club_post = self.get_or_create_club_post(author_uid=post_author)
        follower = self.get_user(but_not=[post_author])
        self.follow_club(uid=follower)
        self.enable_instant_digest(uid=follower)

        targets = self.get_targets(club_post)

        expected = [follower]
        self.assert_targets_are_expected(targets, expected)

    def test_post_club_with_followers_of_all_kinds(self):
        # у клуба есть подписчик, фоловер без настройки и фоловер с настройкой
        post_author = self.get_user()
        club_post = self.get_or_create_club_post(author_uid=post_author)
        subscriber = self.get_user(but_not=[post_author])
        follower_impatient = self.get_user(but_not=[post_author, subscriber])
        follower_patient = self.get_user(
            but_not=[post_author, follower_impatient, subscriber])

        self.subscribe_to_club_posts(uid=subscriber)
        self.follow_club(uid=follower_impatient)
        self.follow_club(uid=follower_patient)
        self.enable_instant_digest(uid=follower_impatient)

        targets = self.get_targets(club_post)

        expected = [follower_impatient, subscriber]
        self.assert_targets_are_expected(targets, expected)

    @pytest.mark.skip('MYSQL specific')
    def test_comment_diary_no_followers(self):
        # комент к посту в дневнике без фоловеров,
        # автор отписался от ответов — никто не получает
        diary_post = self.get_or_create_diary_post()
        commenter = self.get_user(but_not=[diary_post.author_id])
        comment = self.get_or_create_comment(
            post=diary_post, author_uid=commenter)

        targets = self.get_targets(comment)
        expected = []
        self.assert_targets_are_expected(targets, expected)

    def test_comment_diary_self_mailing_with_disabled_comments(self):
        # свой комент приходит, если self_mailing = 1 и
        # CommentsSourcesMode.all
        diary_post = self.get_or_create_diary_post()
        commenter = self.get_user(but_not=[diary_post.author_id])
        comment = self.get_or_create_comment(
            post=diary_post, author_uid=commenter)
        self.enable_self_mailing(commenter)

        targets = self.get_targets(comment)
        expected = []
        self.assert_targets_are_expected(targets, expected)

    def test_comment_diary_self_mailing_with_enabled_comments(self):
        # свой комент приходит, если self_mailing = 1
        # CommentsSourcesMode.all
        diary_post = self.get_or_create_diary_post()
        commenter = self.get_user(but_not=[diary_post.author_id])
        comment = self.get_or_create_comment(
            post=diary_post, author_uid=commenter)
        self.enable_all_comments_notify(commenter)
        self.enable_self_mailing(commenter)

        targets = self.get_targets(comment)
        expected = [commenter]
        self.assert_targets_are_expected(targets, expected)

    @pytest.mark.skip('MYSQL specific')
    def test_comment_diary_by_self_no_self_mailing_with_enabled_comments(self):
        # автору поста не должны приходить коменты к своему посту,
        # если self_mailing = 0 и CommentsSourcesMode.all
        diary_post = self.get_or_create_diary_post()
        comment = self.get_or_create_comment(
            post=diary_post, author_uid=diary_post.author_id)
        self.enable_all_comments_notify(diary_post.author_id)

        targets = self.get_targets(comment)
        expected = []
        self.assert_targets_are_expected(targets, expected)

    @pytest.mark.skip('MYSQL specific')
    def test_comment_diary_no_followers_author_from_all(self):
        # комент к посту в дневнике без фоловеров,
        # автор подписался на ответы — он получит
        diary_post = self.get_or_create_diary_post()
        self.enable_all_comments_notify(diary_post.author_id)

        commenter = self.get_user(but_not=[diary_post.author_id])
        comment = self.get_or_create_comment(
            post=diary_post, author_uid=commenter)

        targets = self.get_targets(comment)
        expected = [diary_post.author_id]
        self.assert_targets_are_expected(targets, expected)

    @pytest.mark.skip('MYSQL specific')
    def test_comment_diary_no_followers_author_from_not_friend(self):
        # комент к посту в дневнике без фоловеров,
        # автор подписался на ответы друзей, но написал враг
        diary_post = self.get_or_create_diary_post()
        self.enable_friends_comments_notify(diary_post.author_id)

        commenter = self.get_user(but_not=[diary_post.author_id])
        comment = self.get_or_create_comment(
            post=diary_post, author_uid=commenter)

        targets = self.get_targets(comment)
        expected = []
        self.assert_targets_are_expected(targets, expected)

    @pytest.mark.skip('MYSQL specific')
    def test_comment_diary_no_followers_author_from_friend(self):
        # комент к посту в дневнике без фоловеров,
        # автор подписался на ответы друзей и написал друг
        diary_post = self.get_or_create_diary_post()
        self.enable_friends_comments_notify(diary_post.author_id)

        commenter = self.get_user(but_not=[diary_post.author_id])
        comment = self.get_or_create_comment(
            post=diary_post, author_uid=commenter)
        self.add_friend(diary_post.author_id, commenter)

        targets = self.get_targets(comment)

        expected = [diary_post.author_id]
        self.assert_targets_are_expected(targets, expected)

    def test_comment_diary_subscribers_from_all(self):
        # комент к посту в дневнике получают подписавшиеся на пост в режиме
        # получать все посты
        diary_post = self.get_or_create_diary_post()
        subscriber = self.get_user(but_not=[diary_post.author_id])
        self.subscribe_to_post(uid=subscriber, post=diary_post)
        self.enable_all_comments_notify(subscriber)

        commenter = self.get_user(but_not=[diary_post.author_id, subscriber])
        comment = self.get_or_create_comment(
            post=diary_post, author_uid=commenter)

        targets = self.get_targets(comment)
        expected = [subscriber]
        self.assert_targets_are_expected(targets, expected)

    def test_comment_diary_subscribers_from_no_friend(self):
        # комент к посту в дневнике от не друга не получают подписавшиеся на пост
        # в режиме «получать ответы только от друзей»
        diary_post = self.get_or_create_diary_post()
        subscriber = self.get_user(but_not=[diary_post.author_id])
        self.subscribe_to_post(uid=subscriber, post=diary_post)
        self.enable_friends_comments_notify(subscriber)

        commenter = self.get_user(but_not=[diary_post.author_id, subscriber])
        comment = self.get_or_create_comment(
            post=diary_post, author_uid=commenter)

        targets = self.get_targets(comment)
        expected = []
        self.assert_targets_are_expected(targets, expected)

    def test_comment_diary_subscribers_from_friend(self):
        # комент к посту в дневнике от друга получают подписавшиеся на пост
        # в режиме «получать ответы только от друзей»
        diary_post = self.get_or_create_diary_post()
        subscriber = self.get_user(but_not=[diary_post.author_id])
        self.subscribe_to_post(uid=subscriber, post=diary_post)
        self.enable_friends_comments_notify(subscriber)

        commenter = self.get_user(but_not=[diary_post.author_id, subscriber])
        comment = self.get_or_create_comment(
            post=diary_post, author_uid=commenter)
        self.add_friend(subscriber, commenter)

        targets = self.get_targets(comment)
        expected = [subscriber]
        self.assert_targets_are_expected(targets, expected)

    def test_subcomment_diary_no_subscribers(self):
        # комент к коменту к посту без подписчиков,
        # получают автор и автор родительского комента
        # (включаем им получение всех ответов)
        diary_post = self.get_or_create_diary_post()
        commenter = self.get_user(but_not=[diary_post.author_id])
        comment = self.get_or_create_comment(
            post=diary_post, author_uid=commenter)
        subcommenter = self.get_user(but_not=[diary_post.author_id, commenter])
        subcomment = self.get_or_create_subcomment(
            parent_comment=comment, author_uid=subcommenter)

        self.enable_all_comments_notify(diary_post.author_id)
        self.enable_all_comments_notify(commenter)

        targets = self.get_targets(subcomment)
        expected = [diary_post.author_id, commenter]
        self.assert_targets_are_expected(targets, expected)

    def test_subcomment_diary_with_subscribers(self):
        # комент к коменту к посту c подписчиками,
        # получают автор и автор родительского комента и подписчики
        diary_post = self.get_or_create_diary_post()
        commenter = self.get_user(but_not=[diary_post.author_id])
        comment = self.get_or_create_comment(
            post=diary_post, author_uid=commenter)
        subcommenter = self.get_user(but_not=[diary_post.author_id, commenter])
        subcomment = self.get_or_create_subcomment(
            parent_comment=comment, author_uid=subcommenter)
        subscriber = self.get_user(
            but_not=[diary_post.author_id, commenter, subcommenter])
        self.subscribe_to_post(subscriber, diary_post)

        self.enable_all_comments_notify(diary_post.author_id)
        self.enable_all_comments_notify(commenter)
        self.enable_all_comments_notify(subscriber)

        targets = self.get_targets(subcomment)
        expected = [diary_post.author_id, commenter, subscriber]
        self.assert_targets_are_expected(targets, expected)

    def test_post_club_with_subscriber_to_all_comments(self):
        # подписался на все коменты всех постов клуба — получай
        post_author = self.get_user()
        club_post = self.get_or_create_club_post(author_uid=post_author)
        commenter = self.get_user(but_not=[post_author])
        comment = self.get_or_create_comment(
            post=club_post, author_uid=commenter)
        crazy_subscriber = self.get_user(but_not=[post_author, commenter])
        self.subscribe_to_club_posts(uid=crazy_subscriber, with_comments=True)
        self.enable_all_comments_notify(uid=crazy_subscriber)

        targets = self.get_targets(comment)

        expected = [crazy_subscriber]
        self.assert_targets_are_expected(targets, expected)

    # TESTS HELPERS
    def get_targets(self, entry, unfiltered_uids=False):
        return Mailing.GetMailingTargets(
            entry.feed_id,
            entry.item_no,
            entry.comment_id,
            unfiltered_uids=unfiltered_uids,
        )

    def assert_targets_are_expected(self, targets, expected, debug=None):
        targets = set([getattr(target, 'feed_id', target) for target in targets])
        expected = set(expected)
        msg = '\n'.join([
            'Targets: ' + str(targets),
            'Expected: ' + str(expected),
            'Debug: ' + str(debug),
        ])
        self.assertEqual(targets, expected, msg)

    # DATA HELPERS
    def setUp(self):
        """
        создаем пост, где первый автор
        создаем комент, где первый автор
        удалить возможные подписки второго на клуб и пост
        удалить возможные дружбы межу людьми и фидами
        """
        self.clean_related_db_data()

    @classmethod
    def clean_related_db_data(cls):
        """Перед каждым тестом все должно быть в предсказуемом состоянии"""
        for uid in cls.users:
            cls.drop_subscriptions(feed_id=uid)
            cls.drop_followers(feed_id=uid)
            cls.drop_friends(feed_id=uid)

        cls.drop_subscriptions(cls.club)
        cls.drop_followers(cls.club)

        cls.reset_mail_settings()

    @staticmethod
    def drop_subscriptions(feed_id):
        """Удалить всех подписки на feed_id"""
        sql = """
            DELETE FROM Subscriptions
            WHERE feed_id = :feed_id
        """
        with get_connection() as connection:
            connection.execute(sql, {'feed_id': feed_id})

    @staticmethod
    def drop_followers(feed_id):
        """Удалить всех фоловеров для feed_id"""
        # TODO: проверить не тут ли отношение наоборот
        sql = """
            DELETE FROM Followers
            WHERE person_id = :feed_id
        """
        with get_connection() as connection:
            connection.execute(sql, {'feed_id': feed_id})

    @staticmethod
    def drop_friends(feed_id):
        """Удалить всех друзей для feed_id"""
        sql = """
            DELETE FROM FriendGroupMember
            WHERE person_id = :feed_id
        """
        with get_connection() as connection:
            connection.execute(sql, {'feed_id': feed_id})

    @classmethod
    def reset_mail_settings(cls):
        """Поставить все настройки пользователей в не получать ничего"""
        for uid in cls.users:
            settings = Mailing.get_user_mail_settings(uid)
            settings.digest_mode = const.DIGEST_MODES.DISABLED
            settings.comment_mode = const.COMMENT_MODES.NONE
            settings.notify_self = False
            settings.save()

    @classmethod
    def set_mail_settings(cls, uid, **params):
        settings = Mailing.get_user_mail_settings(uid)
        for key, value in list(params.items()):
            setattr(settings, key, value)
        settings.save()

    @classmethod
    def enable_instant_digest(cls, uid):
        cls.set_mail_settings(uid, digest_mode=const.DIGEST_MODES.ALL)

    @classmethod
    def enable_self_mailing(cls, uid):
        cls.set_mail_settings(uid, notify_self=True)

    @classmethod
    def enable_all_comments_notify(cls, uid):
        cls.set_mail_settings(
            uid, comment_mode=const.COMMENT_MODES.ALL)

    @classmethod
    def enable_friends_comments_notify(cls, uid):
        cls.set_mail_settings(
            uid, comment_mode=const.COMMENT_MODES.FRIENDS)

    @staticmethod
    def follow_diary(uid, feed_id):
        with get_connection() as connection:
            connection.add(at.aux_.models.Follower(uid=uid, person_id=feed_id))

    @staticmethod
    def add_friend(uid, feed_id):
        # дружба – чёртово инвертированное отношение

        with get_connection() as connection:
            connection.add(at.aux_.models.Friend(uid=feed_id, person_id=uid))

    @classmethod
    def follow_club(cls, uid):
        with get_connection() as connection:
            connection.add(at.aux_.models.Follower(uid=uid, person_id=cls.club))

    @classmethod
    def subscribe_to_club_posts(cls, uid, with_comments=False):
        Mailing.Subscriptions.SubscribeToBlog(
            ai=utils.getAuthInfo(uid=uid),
            feed_id=cls.club,
            with_comments=with_comments,
        )

    @classmethod
    def subscribe_to_post(cls, uid, post, with_comments=True):
        Mailing.Subscriptions.SubscribeToPost(
            ai=utils.getAuthInfo(uid=uid),
            feed_id=post.feed_id,
            item_id=post.item_no,
            with_comments=with_comments,
        )

    @classmethod
    def get_user(cls, but_not=None):
        but_not = but_not or []
        return [user for user in cls.users if user not in but_not][0]

    @classmethod
    def get_or_create_diary_post(cls, author_uid=None):
        author_uid = author_uid or cls.get_user()
        post = cls.get_one_post(feed_id=author_uid, author_uid=author_uid)
        if post is None:
            post = cls.create_post(feed_id=author_uid, author_uid=author_uid)
        return post

    @classmethod
    def get_or_create_comment(cls, post, author_uid=None):
        author_uid = author_uid or cls.get_user()
        comment = cls.get_one_comment(
            feed_id=post.feed_id, item_no=post.item_no, author_uid=author_uid)
        if comment is None:
            comment = cls.create_comment(
                feed_id=post.feed_id, item_no=post.item_no, author_uid=author_uid)
        return comment

    @classmethod
    def get_or_create_subcomment(cls, parent_comment, author_uid=None):
        author_uid = author_uid or cls.get_user()
        comment = cls.get_one_subcomment(
            feed_id=parent_comment.feed_id,
            item_no=parent_comment.item_no,
            parent_comment_id=parent_comment.comment_id,
            author_uid=author_uid,
        )
        if comment is None:
            comment = cls.create_subcomment(
                feed_id=parent_comment.feed_id,
                item_no=parent_comment.item_no,
                parent_comment_id=parent_comment.comment_id,
                author_uid=author_uid,
            )
        return comment

    @classmethod
    def get_or_create_club_post(cls, author_uid=None):
        author_uid = author_uid or cls.get_user()
        post = cls.get_one_post(feed_id=cls.club, author_uid=author_uid)
        if post is None:
            post = cls.create_post(feed_id=cls.club, author_uid=author_uid)
        return post

    @staticmethod
    def get_one_post(feed_id, author_uid):
        sql = """
            SELECT p.person_id, p.post_no
            FROM Posts p
            JOIN EntryXmlContent x ON  -- skip post with no content
              p.person_id = x.feed_id AND
              p.post_no = x.post_no
            WHERE
              person_id = :feed_id
              AND deleted = 0
              AND author_uid = :author_uid
            LIMIT 1
        """

        with get_connection() as connection:
            cursor = connection.execute(sql, {
                'feed_id': feed_id,
                'author_uid': author_uid,
            })
            result = cursor.fetchone()
        if result:
            feed_id, item_no = result
            return models.load_entry(feed_id, item_no)

    @staticmethod
    def get_one_comment(feed_id, item_no, author_uid):
        sql = """
            SELECT c.person_id, c.post_no, c.comment_id
            FROM Comments c
            JOIN EntryXmlContent x ON  -- skip comment with no content
              c.person_id = x.feed_id AND
              c.post_no = x.post_no AND
              c.comment_id = x.comment_id
            WHERE
              c.person_id = :feed_id AND
              c.post_no = :item_no AND
              c.author_uid = :author_uid AND
              deleted = 0 AND
              c.comment_id != 0
            LIMIT 1
        """

        with get_connection() as connection:
            cursor = connection.execute(sql, {
                'feed_id': feed_id,
                'item_no': item_no,
                'author_uid': author_uid,
            })
            result = cursor.fetchone()
        if result:
            feed_id, item_no, comment_id = result
            return models.load_entry(feed_id, item_no, comment_id)

    @staticmethod
    def get_one_subcomment(feed_id, item_no, author_uid, parent_comment_id):
        sql = """
            SELECT c.person_id, c.post_no, c.comment_id
            FROM Comments c
            JOIN EntryXmlContent x ON  -- skip comment with no content
              c.person_id = x.feed_id AND
              c.post_no = x.post_no AND
              c.comment_id = x.comment_id
            WHERE
              c.person_id = :feed_id AND
              c.post_no = :item_no AND
              c.author_uid = :author_uid AND
              deleted = 0 AND
              c.comment_id != 0 AND
              c.parent_id = :parent_comment_id
            LIMIT 1
        """

        with get_connection() as connection:
            cursor = connection.execute(sql, {
                'feed_id': feed_id,
                'item_no': item_no,
                'author_uid': author_uid,
                'parent_comment_id': parent_comment_id,
            })
            result = cursor.fetchone()
        if result:
            feed_id, item_no, comment_id = result
            return models.load_entry(feed_id, item_no, comment_id)

    @classmethod
    def create_post(cls, feed_id, author_uid=None):
        author_uid = author_uid or cls.get_user()
        post = models.Text(feed_id=feed_id, item_no=None)
        post.update_fields({
            'is_comment': False,
            'author_id': author_uid,
            'store_time': datetime.now(),
            'item_time': datetime.now(),
            'block_comments': False,
            'rubric_id': 6,
            'access_group': GroupType.USER,
            'form_id': posts_requests.generate_form_id(),
            'body': 'THE POST',
        })
        post.save()
        return post

    @classmethod
    def create_comment(cls, feed_id, item_no, author_uid=None):
        author_uid = author_uid or cls.get_user()
        comment = models.Text(feed_id=feed_id, item_no=item_no)
        comment.update_fields({
            'is_comment': True,
            'author_id': author_uid,
            'store_time': datetime.now(),
            'parent_comment_id': 0,
            'body': 'THE COMMENT',
            'deleted': 0,
        })
        comment.save()
        return comment

    @classmethod
    def create_subcomment(cls, feed_id, item_no, parent_comment_id, author_uid=None):
        author_uid = author_uid or cls.get_user()
        comment = models.Text(feed_id=feed_id, item_no=item_no)
        comment.update_fields({
            'is_comment': True,
            'author_id': author_uid,
            'store_time': datetime.now(),
            'parent_comment_id': parent_comment_id,
            'body': 'THE COMMENT',
            'deleted': 0,
        })
        comment.save()
        return comment
