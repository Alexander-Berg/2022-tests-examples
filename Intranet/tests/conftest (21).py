import logging

import pytest
from mock import Mock

from sqlalchemy import create_engine
from sqlalchemy.orm import scoped_session


logging.getLogger("factory").setLevel(logging.CRITICAL)

pytest_plugins = [
    'fixtures.staff_info',
]


@pytest.fixture
def config(monkeypatch):
    monkeypatch.setenv('TVM2_ASYNC', 'true')
    monkeypatch.setenv('QLOUD_TVM_TOKEN', 'test_token')
    monkeypatch.setenv('TVM2_USE_QLOUD', 'true')
    from stackbot.config import settings
    return settings


@pytest.fixture(scope='session')
def session_scope_session(db_session):
    return scoped_session(db_session.cached_sessionmaker)


@pytest.fixture
def scope_session(db_session):
    return scoped_session(db_session.cached_sessionmaker)


@pytest.fixture
def sqlalchemy_session(config, scope_session):
    engine = create_engine(config.database_url)
    scope_session.configure(bind=engine)
    session = scope_session()
    yield session
    session.rollback()
    scope_session.remove()


@pytest.fixture(scope='function')
def db_session():
    from stackbot.db.base import get_cached_engine, get_cached_sessionmaker
    dbsession = Mock()
    dbsession.cached_engine = get_cached_engine()
    dbsession.cached_sessionmaker = get_cached_sessionmaker(dbsession.cached_engine)

    return dbsession


@pytest.fixture(scope='function', autouse=True)
def create_test_database(db_session):
    from stackbot.db import BaseModel

    engine = db_session.cached_engine
    BaseModel.metadata.create_all(engine)
    yield
    db_session.cached_sessionmaker.close_all()
    BaseModel.metadata.drop_all(engine)
