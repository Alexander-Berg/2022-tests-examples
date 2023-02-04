import json
import datetime

from infra.cauth.server.common.alchemy import Session
from infra.cauth.server.common.constants import USER_GROUP_TYPE
from infra.cauth.server.common.models import (
    User,
    Group,
    UserGroupRelation,
    Server,
    Role,
    Source,
    Access,
    ServerGroup,
)


DEFAULT_CLIENT_CAUTH_VERSION = 'CAUTH/yandex-cauth-1.3.2'


def save_object(obj):
    Session.add(obj)
    Session.flush()
    return obj


def add_user_to_group(user, group, until=None):
    while group is not None:
        membership = UserGroupRelation(uid=user.uid, gid=group.gid, until=until)
        Session.add(membership)
        group = group.parent

    Session.flush()


def create_user_group(name, gid, group_type=USER_GROUP_TYPE.DPT, parent=None):
    group = Group(
        gid=gid,
        parent_gid=parent.gid if parent is not None else None,
        type=group_type,
        name=name,
    )
    return save_object(group)


def get_or_create_source(name):
    source = Source.query.filter_by(name=name).first()
    if source is None:
        source = Source(
            name=name,
            is_default=False,
        )
        save_object(source)
    return source


def create_server(
        fqdn,
        client_version=DEFAULT_CLIENT_CAUTH_VERSION,
        source_name=None,
        groups=None,
        **kwargs
):
    flow = kwargs.pop('flow', None)
    server = Server(
        fqdn=fqdn,
        client_version=client_version,
        **kwargs
    )
    groups = groups or ''
    group_names = [name for name in groups.split(',') if name]
    if group_names:
        server_groups = [get_or_create_server_group(name=name) for name in group_names]
        server.groups.extend(server_groups)
        save_object(server)
    if flow:
        server.set_flow(flow)
    server = save_object(server)

    if source_name:
        source = get_or_create_source(source_name)
        server.sources.append(source)

    return server


def get_or_create_server_group(source_name=None, name=None, **kwargs):
    assert source_name or name, 'Some of source_name or name parameter must be specified'
    if name:
        if len(name.split('.')) != 2:
            name = '.'.join([source_name, name])
        assert len(name.split('.')) == 2, 'Invalid ServerGroup name {}'.format(name)
    if source_name:
        name = name or '{}.some_group'.format(source_name)
        assert name.split('.')[0] == source_name, 'Group name prefix must equal source_name'
    source_name = source_name or name.split('.')[0]

    source = get_or_create_source(source_name)
    server_group = ServerGroup.query.filter_by(name=name).first()
    if server_group:
        return server_group
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


class Response(object):
    """Эмулятор requests.response"""
    def __init__(self, content, status_code=200, headers=None, url=None):
        self.status_code = status_code
        self.content = content
        if isinstance(self.content, dict):
            self.content = json.dumps(self.content)
        self.headers = headers
        self.url = url

    def __repr__(self):
        return '<Response [%s]>' % (self.status_code)

    @property
    def text(self):
        """
        Возвращает unicode-объект с телом ответа (из предположения, что кодировка — utf-8).
        """
        return self.content.decode('utf-8')

    def json(self, **kwargs):
        return json.loads(self.text)


class AttrDict(dict):
    def __getattr__(self, item):
        return self[item]


def create_user(uid, username, first_name, last_name='', join_date=None, shell='/bin/bash',
                is_fired=False, is_robot=False):
    user = User.query.filter_by(login=username).first()
    if user is None:
        if join_date is None:
            join_date = datetime.date.today() - datetime.timedelta(days=3)
        user = User(
            login=username,
            uid=uid,
            gid=100,
            first_name=first_name,
            last_name=last_name,
            join_date=join_date,
            shell=shell,
            is_fired=is_fired,
            is_robot=is_robot,
        )
        Session.add(user)
        Session.flush()
    return user


def assert_contains(snippets_list, text):
    """Проверяет, что все кусочки текста из списка упоминаются в тексте."""
    not_in = [
        snippet
        for snippet in snippets_list
        if snippet not in text
    ]
    if not_in:
        raise AssertionError(
            'Эти фрагменты не найдены в тексте: ' + ', '.join(not_in)
        )
