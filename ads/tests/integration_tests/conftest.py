import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from maps.pylibs.local_postgres import postgres_instance

import ads.watchman.timeline.api.tests.helpers.db_helpers as db_helpers
from ads.watchman.timeline.api.lib.modules.events import db


@pytest.fixture(scope="module")
def postgres():
    with postgres_instance() as pg:
        yield pg


@pytest.fixture(scope="module")
def migrator():
    with db_helpers.MigratorWrapper() as migrator:
        yield migrator


@pytest.fixture(scope="module")
def engine(postgres, migrator):
    migrator.run_migrations(postgres)
    db_engine = create_engine(postgres.db_url)
    yield db_engine
    db.Base.metadata.reflect(bind=db_engine)
    db.Base.metadata.drop_all(bind=db_engine)


@pytest.fixture(scope="function")
def db_session(engine):
    session = sessionmaker(bind=engine)()
    try:
        yield session
    except Exception:
        session.rollback()
        raise
    finally:
        db.remove_all_objects(session)
        session.close()
