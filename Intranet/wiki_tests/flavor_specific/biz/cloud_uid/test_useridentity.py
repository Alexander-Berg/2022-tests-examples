import pytest
from rest_framework import serializers
from wiki.api_frontend.serializers.user_identity import UserIdentity, UserIdentityListField
from wiki.users.dao import get_users_by_identity, get_user_by_identity


class DummySerializer(serializers.Serializer):
    users = UserIdentityListField()


def test_useridentityfield():
    # старый стиль, когда мы передавали только строки
    s1 = DummySerializer(data={'users': ['100', '200', '300']})
    assert s1.is_valid()
    assert s1.validated_data['users'] == [UserIdentity(uid='100'), UserIdentity(uid='200'), UserIdentity(uid='300')]

    # старый стиль, когда мы передавали только строки
    s2 = DummySerializer(data={'users': [100, 200, 300]})
    assert s2.is_valid()
    assert s2.validated_data['users'] == [UserIdentity(uid='100'), UserIdentity(uid='200'), UserIdentity(uid='300')]

    # новый формат
    s3 = DummySerializer(data={'users': [{'uid': '100'}, {'cloud_uid': '200'}]})
    assert s3.is_valid()
    assert s3.validated_data['users'] == [UserIdentity(uid='100'), UserIdentity(cloud_uid='200')]


@pytest.mark.django_db
def test_useridentity(wiki_users, test_org_ctx):
    user_ids = [UserIdentity.from_user(u) for u in [wiki_users.thasonic, wiki_users.thasonic, wiki_users.kolomeetz]]
    # hash & eq

    user_ids_set = set(user_ids)
    assert len(user_ids_set) == 2
    asm_identity = UserIdentity.from_user(wiki_users.asm)
    volozh_identity = UserIdentity.from_user(wiki_users.volozh)
    assert asm_identity == UserIdentity(uid=asm_identity.uid, cloud_uid=asm_identity.cloud_uid)

    # lookup
    users = get_users_by_identity([asm_identity, volozh_identity], prefetch_staff=True, panic_if_missing=True)
    assert set(users) == {wiki_users.asm, wiki_users.volozh}

    assert wiki_users.volozh == get_user_by_identity(UserIdentity(cloud_uid=wiki_users.volozh.cloud_uid))


@pytest.mark.django_db
def test_useridentity_without_ctx(wiki_users, test_org):
    user_ids = [UserIdentity.from_user(u) for u in [wiki_users.thasonic, wiki_users.thasonic, wiki_users.kolomeetz]]
    # hash & eq

    user_ids_set = set(user_ids)
    assert len(user_ids_set) == 2
    asm_identity = UserIdentity.from_user(wiki_users.asm)
    volozh_identity = UserIdentity.from_user(wiki_users.volozh)
    assert asm_identity == UserIdentity(uid=asm_identity.uid, cloud_uid=asm_identity.cloud_uid)

    # lookup
    users = get_users_by_identity(
        [asm_identity, volozh_identity], prefetch_staff=True, panic_if_missing=True, organization=test_org
    )
    assert set(users) == {wiki_users.asm, wiki_users.volozh}

    assert wiki_users.volozh == get_user_by_identity(
        UserIdentity(cloud_uid=wiki_users.volozh.cloud_uid), organization=test_org
    )
