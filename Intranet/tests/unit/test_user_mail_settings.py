# coding: utf-8

from django.conf import settings
import pytest
from pretend import stub

from at.common import const
from at.common import models
from at.aux_ import Mailing

from tests import helpers


def test_tmp_no_record_default_mail_settings(client, test_person):
    response = client.get(
        path='/api/frontend/get_mailing_settings_xml/',
    )
    assert response.status_code == 200

    expected = """
        <UserMailSettings>
            <feed_id>{feed_id}</feed_id>
            <comments_notification_mode>1</comments_notification_mode>
            <digest_mode>0</digest_mode>
            <summons>1</summons>
            <invites>1</invites>
            <self_mailing>0</self_mailing>
            <moderation>1</moderation>
        </UserMailSettings>
    """.format(feed_id=settings.YAUTH_TEST_USER['uid'])
    diff = helpers.xml_diff(
        response.content,
        expected,
        consider_order=False,
        ignore_attrs=['hostname'],
    )
    assert not diff, (diff.content_diff, diff.structure_diff, response.content)


@pytest.mark.django_db(transaction=True)
def test_ui_write_and_read_methods(client, test_person):
    models.MailSettings.objects.create(person=test_person)
    response = client.post(
        path='/api/frontend/set_mailing_settings/',
        data={
            'comments_notification_mode': '2',
            'digest_mode': '2',
            'summons': '0',
            'invites': '0',
            'self_mailing': '1',
            'moderation': '0',
        },
    )
    assert response.status_code == 200

    response = client.get(
        path='/api/frontend/get_mailing_settings_xml/',
    )
    assert response.status_code == 200

    expected = """
        <UserMailSettings>
            <feed_id>{feed_id}</feed_id>
            <comments_notification_mode>2</comments_notification_mode>
            <digest_mode>2</digest_mode>
            <summons>0</summons>
            <invites>0</invites>
            <self_mailing>1</self_mailing>
            <moderation>0</moderation>
        </UserMailSettings>
    """.format(feed_id=settings.YAUTH_TEST_USER['uid'])

    diff = helpers.xml_diff(
        response.content,
        expected,
        consider_order=False,
        ignore_attrs=['hostname'],
    )
    assert not diff, (diff.content_diff, diff.structure_diff, response.content)


def test_get_digest_users(person_builder, mail_settings_builder):
    person_one = person_builder(has_access=False)
    person_two = person_builder()
    person_three = person_builder()
    mail_settings_builder(
        person=person_one,
        digest_mode=const.DIGEST_MODES.POPULAR,
    )
    mail_settings_builder(
        person=person_two,
        digest_mode=const.DIGEST_MODES.POPULAR,
    )
    mail_settings_builder(
        person=person_three,
        digest_mode=const.DIGEST_MODES.FULL,
    )

    assert set(Mailing.get_digest_users()) == {
        (person_two.person_id, const.DIGEST_MODES.POPULAR),
        (person_three.person_id, const.DIGEST_MODES.FULL),
    }


def test_get_user_mail_settings(mail_settings):
    assert Mailing.get_user_mail_settings(mail_settings.person) == mail_settings
    assert Mailing.get_user_mail_settings(mail_settings.person_id) == mail_settings


MODES = const.COMMENT_MODES


@pytest.mark.parametrize('comment_mode, is_friend, expected', [
    (MODES.NONE, False, False),
    (MODES.NONE, True, False),
    (MODES.FRIENDS, False, False),
    (MODES.FRIENDS, True, True),
    (MODES.ALL, False, True),
    (MODES.ALL, True, True),
])
def test_filter_persons_for_notify(
    person,
    mail_settings_builder,
    friend_builder,
    comment_mode,
    is_friend,
    expected,
):
    mail_settings = mail_settings_builder(comment_mode=comment_mode)
    entry = stub(author_id=person.person_id, comment_id=100500)
    if is_friend:
        friend_builder(
            who=mail_settings.person,
            whom=person,
        )
    is_notified = mail_settings.person_id in Mailing.filter_persons_for_notify(
        person_ids={mail_settings.person_id},
        entry=entry,
    )
    assert is_notified == expected


def test_is_person_should_be_notified_by_post_(
    person,
    mail_settings_builder
):
    entry = stub(author_id=person.person_id, comment_id=None)
    mail_settings = mail_settings_builder(
        comment_mode=const.COMMENT_MODES.ALL
    )
    assert mail_settings.person_id in Mailing.filter_persons_for_notify(
        person_ids={mail_settings.person_id},
        entry=entry,
    )


def test_user_mail_settings_migration(
    person_builder,
):
    person_without_settings = person_builder()
    person_with_mongo_digest_settings = person_builder()
    person_with_mongo_other_settings = person_builder()

    from at.aux_ import MongoStorage
    mongo_storage = MongoStorage.Storage(
        collection_name='UserMailSettings_storage'
    )
    mongo_storage.connect()
    mongo_storage.collection.insert_one({
        'feed_id': person_with_mongo_digest_settings.person_id,
        'dict_data': {
            'digest_mode': 3,
            'email': 'wow@yandex-team.ru',
        }
    })
    mongo_storage.collection.insert_one({
        'feed_id': person_with_mongo_other_settings.person_id,
        'dict_data': {
            'self_mailing': 1,
            'email': 'wow2@yandex-team.ru',
        }
    })

    Mailing.tmp_migrate_mail_settings()

    assert models.MailSettings.objects.filter(
        person=person_without_settings,
        digest_mode=const.DIGEST_MODES.DISABLED,
    ).exists()
    assert models.MailSettings.objects.filter(
        person=person_with_mongo_digest_settings,
        digest_mode=const.DIGEST_MODES.ALL,
    ).exists()
    assert models.MailSettings.objects.filter(
        person=person_with_mongo_other_settings,
        digest_mode=const.DIGEST_MODES.DISABLED,
        notify_self=True,
    ).exists()
