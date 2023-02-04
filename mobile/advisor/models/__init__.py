import logging

import mongoengine
from django.conf import settings
# noinspection PyProtectedMember
from mongoengine.connection import _connection_settings
from mongoengine.errors import OperationError
from pymongo import ReadPreference

logger = logging.getLogger(__name__)

# noinspection PyBroadException
try:
    logger.debug('Connecting to mongo with mongoengine')
    mongoengine.connect(
        connect=False,
        db=settings.MONGO_DB_NAME,
        host=settings.MONGO_URI,
        read_preference=ReadPreference.NEAREST,
        **settings.MONGO_TIMEOUT_PARAMS
    )
    mongoengine.connect(
        connect=False,
        alias='primary_only',
        db=settings.MONGO_DB_NAME,
        host=settings.MONGO_URI,
        read_preference=ReadPreference.PRIMARY,
        **settings.MONGO_TIMEOUT_PARAMS
    )
    mongoengine.connect(
        connect=False,
        alias='localization',
        db=settings.LOCALIZATION_MONGO_DBNAME,
        host=settings.LOCALIZATION_MONGO_URI,
        read_preference=ReadPreference.NEAREST,
        **settings.MONGO_TIMEOUT_PARAMS
    )
except:
    logger.exception('Cant connect to mongo with mongoengine')


# noinspection PyProtectedMember
def is_ready():
    try:
        check_is_not_mock()
    except AssertionError:
        return True
    for alias in mongoengine.connection._connections:
        db = mongoengine.connection.get_db(alias)
        if not db.command('ping').get('ok'):
            return False
    return True


def check_is_not_mock():
    for alias, connection in _connection_settings.iteritems():
        assert not connection.get('is_mock'), "You can not save fixtures from mocked database."


def check_mock():
    for alias, connection in _connection_settings.iteritems():
        assert connection.get('is_mock'), "You can load fixtures only in mocked database. " \
                                          "Please check how do you run tests"


def get_all_models():
    # noinspection PyProtectedMember
    from mongoengine.base.common import _document_registry

    return _document_registry.itervalues()


# noinspection PyProtectedMember
def ensure_indexes():
    for model in get_all_models():
        try:
            logger.info("Ensuring index for %s collection", model._get_collection_name())
            model.ensure_indexes()
        except OperationError:
            logger.error("Failed to create index for %s collection", model._get_collection_name())


# noinspection PyProtectedMember
def compare_indexes():
    for model in get_all_models():
        collection = model._get_collection_name()
        compare_result = model.compare_indexes()
        yield collection, compare_result['extra'], compare_result['missing']
