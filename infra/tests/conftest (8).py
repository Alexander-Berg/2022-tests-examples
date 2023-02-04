import boto3
import contextlib

import django
import pytest
from django.conf import settings
from django.utils import timezone

from infra.cauth.server.common.alchemy import create_cauth_engine, engine as cauth_engine, Session
from infra.cauth.server.common.models import BaseModel, Source
from infra.cauth.server.master.importers.servers.sources import BotAbcGroups
from infra.cauth.server.master.importers.servers.sources import HdAbcGroups
from __tests__.client import CauthClient
from __tests__.utils import create_user, AttrDict


DEFAULT_DB_NAME = 'postgres'
TEST_DB_NAME = settings.DATABASES['default']['NAME']


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


@pytest.fixture()
def client(request, settings):
    client = CauthClient()

    def login(username):
        settings.YAUTH_TEST_USER = {'login': username}

    client.login = login
    return client


@pytest.fixture()
def users(rollback_transaction):
    frodo = create_user(1, 'frodo', first_name='Frodo', last_name='Baggins')
    legolas = create_user(2, 'legolas', first_name='Legolas')
    gandalf = create_user(3, 'gandalf', first_name='Gandalf')
    idm_robot = create_user(4, settings.IDM_ROBOT_LOGIN, first_name=settings.IDM_ROBOT_LOGIN)

    return AttrDict({
        'frodo': frodo,
        'legolas': legolas,
        'gandalf': gandalf,
        'idm_robot': idm_robot,
    })


@pytest.fixture()
def default_source(rollback_transaction):
    now = timezone.now()
    source = Source(
        id=1,
        name='default',
        is_default=True,
        last_update=now,
    )
    Session.add(source)
    return source


@pytest.fixture()
def sources(settings, rollback_transaction):
    now = timezone.now()
    data = settings.CERT_ACCESS_REGISTRY['servers']
    for index, source_dict in enumerate(data):
        source = Source(
            name=source_dict['source'],
            is_default=False,
            last_update=now,
            is_modern=source_dict.get('is_modern', False)
        )
        Session.add(source)
        Session.flush()


@pytest.fixture
def mock_bot_https(monkeypatch):
    def mock(bot_answer, abc_answer):
        def mocked_bot_answer(*a, **kw):
            return bot_answer

        def mocked_abc_answer(*a, **kw):
            for item in abc_answer:
                yield item

        monkeypatch.setattr(BotAbcGroups, '_get_servers_from_bot', mocked_bot_answer)
        monkeypatch.setattr(BotAbcGroups, '_iter_abc', mocked_abc_answer)
    return mock


@pytest.fixture
def mock_hd_https(monkeypatch):
    def mock(hd_answer, abc_answer, consumers_answer):
        def mocked_hd_answer(*a, **kw):
            return hd_answer

        def mocked_abc_answer(*a, **kw):
            for item in abc_answer:
                yield item

        def mocked_consumers(*a, **kw):
            for item in consumers_answer:
                yield item

        monkeypatch.setattr(HdAbcGroups, '_fetch_hd_data', mocked_hd_answer)
        monkeypatch.setattr(HdAbcGroups, '_iter_abc', mocked_abc_answer)
        monkeypatch.setattr(HdAbcGroups, '_iter_consumers', mocked_consumers)
    return mock


@pytest.yield_fixture(autouse=True, scope='session')
def s3_bucket():
    client = boto3.resource(
        's3',
        endpoint_url=settings.AWS_S3_ENDPOINT_URL,
        aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
        aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
    )
    bucket = client.create_bucket(Bucket=settings.AWS_STORAGE_BUCKET_NAME)
    yield
    bucket.objects.all().delete()
    bucket.delete()
