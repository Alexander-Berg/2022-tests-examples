import datetime

from sqlalchemy.inspection import inspect
from sqlalchemy.sql import text

import factory

from infra.cauth.server.common.alchemy import Session
from infra.cauth.server.common.constants import USER_GROUP_TYPE, CLIENT_SOURCE_REASON
from infra.cauth.server.common.models import (
    User,
    Group,
    UserGroupRelation,
    Server,
    ServerGroup,
    ServerResponsible,
    Source,
    Access,
    Role,
    PublicKey,
    ServerTrustedSourceRelation,
    ServerGroupTrustedSourceRelation,
)

DEFAULT_CLIENT_CAUTH_VERSION = 'CAUTH/yandex-cauth-1.3.2'


class BaseFactory(factory.alchemy.SQLAlchemyModelFactory):
    @classmethod
    def _setup_next_sequence(cls):
        pkey = inspect(cls._meta.model).primary_key[0].name
        last_obj = cls._meta.model.query.order_by(text('{} desc'.format(pkey))).first()
        return getattr(last_obj, pkey, 0) + 1


class GroupFactory(BaseFactory):
    class Meta:
        model = Group
        sqlalchemy_session = Session

    gid = factory.Sequence(lambda n: n)
    name = factory.Sequence(lambda n: 'group%d' % n)
    type = USER_GROUP_TYPE.DPT


class ServerFactory(BaseFactory):
    class Meta:
        model = Server
        sqlalchemy_session = Session

    fqdn = factory.Sequence(lambda n: 'example%s.yandex-team.ru' % n)
    client_version = DEFAULT_CLIENT_CAUTH_VERSION


class PublicKeyFactory(BaseFactory):
    class Meta:
        model = PublicKey
        sqlalchemy_session = Session

    key = factory.Sequence(lambda n: 'key %s' % n)


class ServerTrustedSourceRelationFactory(BaseFactory):
    class Meta:
        model = ServerTrustedSourceRelation
        sqlalchemy_session = Session

    reason = CLIENT_SOURCE_REASON.FROM_CLIENT


class ServerGroupTrustedSourceRelationFactory(BaseFactory):
    class Meta:
        model = ServerGroupTrustedSourceRelation
        sqlalchemy_session = Session

    reason = CLIENT_SOURCE_REASON.FROM_SOURCE


class UserFactory(BaseFactory):
    class Meta:
        model = User
        sqlalchemy_session = Session

    uid = factory.Sequence(lambda n: n)
    login = factory.Sequence(lambda n: 'login%d' % n)
    first_name = factory.Sequence(lambda n: 'name%d' % n)
    last_name = factory.Sequence(lambda n: 'lastname%d' % n)
    shell = '/bin/bash'
    home = factory.LazyAttribute(lambda o: '/home/%s' % o.login)


def save_object(obj):
    Session.add(obj)
    Session.flush()
    return obj


def add_user_to_group(user, group):
    while group is not None:
        membership = UserGroupRelation.query.filter_by(uid=user.uid, gid=group.gid).first()
        if not membership:
            membership = UserGroupRelation(uid=user.uid, gid=group.gid)
            Session.add(membership)
        group = group.parent

    Session.flush()


def create_user_group(parent=None, **kwargs):
    group = GroupFactory(
        parent_gid=parent.gid if parent is not None else None,
        **kwargs
    )
    return group


def create_user(group, **kwargs):
    user = User.query.filter_by(gid=group.gid, **kwargs).first() if 'login' in kwargs else None
    if not user:
        user = UserFactory(gid=group.gid, **kwargs)
    add_user_to_group(user, group)
    return user


def get_or_create_source(name, is_default=False):
    source = Source.query.filter_by(name=name).first()
    if source is None:
        source = Source(
            name=name,
            is_default=is_default,
        )
        save_object(source)
    return source


def create_server(source_names=None, **kwargs):
    server = ServerFactory(**kwargs)
    source_names = source_names or []
    for source in source_names:
        source = get_or_create_source(source)
        server.sources.append(source)
    return server


def create_server_group(name, source, **kwargs):
    server_group = ServerGroup(
        name=name,
        source=source,
        **kwargs
    )
    return save_object(server_group)


def create_sudo_role(spec):
    db_role = Session.query(Role).filter(Role.spec == spec).first()
    if db_role:
        return db_role

    role = Role(spec=spec)
    return save_object(role)


def create_access_rule(access_type, src_obj, dst_obj, is_root=False, nopasswd=False):
    src = None
    src_user = None
    src_group = None

    if isinstance(src_obj, User):
        src = src_obj.login
        src_user = src_obj
    elif isinstance(src_obj, Group):
        src = src_obj.name
        src_group = src_obj

    dst = None
    dst_server = None
    dst_group = None

    if isinstance(dst_obj, Server):
        dst = dst_obj.fqdn
        dst_server = dst_obj
    elif isinstance(dst_obj, ServerGroup):
        dst = dst_obj.name
        dst_group = dst_obj

    access = Access(
        type=access_type,
        old_id=0,
        src=src,
        src_user=src_user,
        src_group=src_group,
        dst=dst,
        dst_server=dst_server,
        dst_group=dst_group,
        ssh_is_admin=is_root,
        description='test',
        requester='test-login',
        approver='test-login',
        request_date=datetime.datetime.now(),
        approve_date=datetime.datetime.now(),
    )
    if access_type == 'sudo':
        cmdlist = 'ALL=(ALL) {}ALL'.format('NOPASSWD: ' if nopasswd else '')
        access.sudo_role = create_sudo_role(cmdlist)

    return save_object(access)


def create_public_key(user, **kwargs):
    key = PublicKeyFactory(uid=user.uid, **kwargs)
    return key


def create_server_trusted_source_relation(server, source, **kwargs):
    relation = ServerTrustedSourceRelationFactory(server_id=server.id, source_id=source.id, **kwargs)
    return relation


def create_servergroup_trusted_source_relation(servergroup, source, **kwargs):
    relation = ServerGroupTrustedSourceRelationFactory(servergroup_id=servergroup.id, source_id=source.id, **kwargs)
    return relation


def add_user_to_responsibles(server, user, source):
    Session.add(
        ServerResponsible(
            user=user,
            server=server,
            source=source,
        )
    )
    Session.flush()
