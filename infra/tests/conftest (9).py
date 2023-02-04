import itertools
import contextlib
import pytest

import django
from django.conf import settings
from sqlalchemy.orm import sessionmaker

from infra.cauth.server.common.alchemy import create_cauth_engine, engine as cauth_engine, Session
from infra.cauth.server.common.models import BaseModel, ServerResponsible

from infra.cauth.server.public.constants import SOURCE_NAME

from __tests__.utils.client import CauthPublicClient
from __tests__.utils.create import (
    create_user_group,
    create_user,
    create_server_group,
    create_server,
    create_access_rule,
    get_or_create_source,
    create_public_key,
)


DEFAULT_DB_NAME = 'cauth'
TEST_DB_NAME = settings.DATABASES['default']['NAME']


def transaction_execute(engine, command):
    with contextlib.closing(engine.connect()):
        with contextlib.closing(sessionmaker(bind=engine)()) as session:
            session.execute(command)


@pytest.fixture(scope='session', autouse=True)
def django_setup():
    django.setup()


@pytest.yield_fixture(scope='session')
def create_db():
    default_params = settings.DATABASES['default'].copy()
    default_params['NAME'] = DEFAULT_DB_NAME

    default_engine = create_cauth_engine(settings.PSYCOPG2_DSN_PATTERN, default_params)

    BaseModel.metadata.create_all(cauth_engine)
    yield

    cauth_engine.dispose()
    default_engine.dispose()


@pytest.yield_fixture(autouse=True)
def rollback_transaction(create_db):
    Session.remove()

    with contextlib.closing(cauth_engine.connect()) as connection:
        transaction = connection.begin()
        Session.configure(bind=connection)
        yield
        transaction.rollback()

    Session.remove()
    Session.configure(bind=cauth_engine)


@pytest.fixture
def client():
    return CauthPublicClient()


@pytest.fixture
def default_user_group():
    group = create_user_group(name='default_user_group', gid=1)
    Session.commit()
    return group


@pytest.fixture(autouse=True)
def sources(rollback_transaction):
    result = {}
    for name in SOURCE_NAME.choices():
        is_default = name == SOURCE_NAME.DEFAULT
        result[name] = get_or_create_source(name, is_default=is_default)
    return result


@pytest.fixture
def server_groups(sources):
    groups_data = {
        'group_cms': SOURCE_NAME.CMS,
        'group_conductor': SOURCE_NAME.CONDUCTOR,
    }

    result = {}
    for name, source_name in list(groups_data.items()):
        result[name] = create_server_group(name, sources[source_name])
    return result


@pytest.fixture
def users(default_user_group, server_groups):
    simple_logins = ['user_golem', 'user_ssh', 'user_ssh_root', 'user_ssh_2']
    cms_logins = ['user_cms_1', 'user_cms_2']
    conductor_logins = ['user_conductor_1', 'user_conductor_2']
    super_logins = ['user_super']

    logins = itertools.chain(simple_logins, cms_logins, conductor_logins, super_logins)
    result_users = {}
    for login in logins:
        result_users[login] = create_user(login=login, group=default_user_group)

    group_responsibilities = {
        server_groups['group_cms']: itertools.chain(cms_logins, super_logins),
        server_groups['group_conductor']: itertools.chain(conductor_logins, super_logins),
    }
    for server_group, logins in group_responsibilities.items():
        for login in logins:
            server_group.responsible_users.append(result_users[login])

    Session.flush()

    return result_users


@pytest.fixture
def server(sources, server_groups, users):
    result_server = create_server(fqdn='test.yandex-team.ru')
    for group in list(server_groups.values()):
        group.servers.append(result_server)

    responsible = ServerResponsible(
        server=result_server,
        user=users['user_golem'],
        source=sources[SOURCE_NAME.GOLEM],
    )
    result_server.responsibles.append(responsible)
    Session.flush()

    create_access_rule('ssh', users['user_ssh'], result_server)
    create_access_rule('sudo', users['user_ssh'], result_server)
    create_access_rule('ssh', users['user_ssh_root'], result_server, is_root=True)
    create_access_rule('ssh', users['user_ssh_2'], server_groups['group_conductor'], is_root=True)
    create_access_rule('ssh', users['user_cms_1'], server_groups['group_cms'], is_root=True)

    return result_server


@pytest.fixture
def public_keys(users):
    keys = {}
    for login, user in users.items():
        keys[login] = create_public_key(user)
    return keys
