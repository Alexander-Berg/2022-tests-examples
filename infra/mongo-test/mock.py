"""Mocks MongoDB database for Unit Tests.

Note: this module is not thread-safe.
"""

from __future__ import unicode_literals

import json
import os

import mongoengine
import mongoengine.connection

from sepelib.mongo.util import get_registered_models, IS_PYMONGO_2


_MONGODB = None
_START_TIMEOUT = 30
_POLLING_INTERVAL = 0.01
_TERMINATION_TIMEOUT = 10


class ObjectMocker(object):
    """
    A handy object for mocking MongoEngine objects which remembers all created objects and have assertion methods to
    ensure that the database has been changed that way you expected.
    """

    def __init__(self, cls, defaults=None):
        self.__cls = cls
        self.__defaults = defaults or {}
        self.__objects = []

        cls.ensure_indexes()

    def add(self, obj):
        """Adds an object to the list of tracked objects."""

        self.__objects.append(obj)

    def remove(self, obj):
        """Stops tracking the specified object."""

        self.__objects = [other for other in self.__objects if other is not obj]

    @property
    def objects(self):
        """Returns the list of tracked objects."""

        return self.__objects

    def mock(self, overrides=None, add=True, save=True):
        """Mocks an object."""

        if not overrides:
            overrides = {}
        kwargs = self.__defaults.copy()
        kwargs.update(overrides)

        obj = self.__cls(**kwargs)
        if save:
            obj.save(force_insert=True)
        if add:
            self.add(obj)

        return obj

    def assert_equal(self, ignore_fields=None):
        """Asserts that the controlled objects are equal to the objects in the database."""

        if not ignore_fields:
            ignore_fields = []

        def obj_to_dict(objects):
            objs = dict()

            for obj in objects:
                obj = json.loads(obj.to_json())

                for field in ignore_fields:
                    if field in obj:
                        del obj[field]

                key = frozenset(obj["_id"].items()) if isinstance(obj["_id"], dict) else obj["_id"]
                objs[key] = obj

            return objs

        objects = obj_to_dict(self.objects)
        db_objects = obj_to_dict(self.__cls.objects)

        assert objects == db_objects


class _MongoDb(object):
    """Mocks MongoDB database."""

    def __init__(self):
        self.connected = False
        self.empty = True
        self.dirty = False

        self.__host = "localhost"
        self.__port = os.getenv("RECIPE_MONGO_PORT")

        if not self.__port:
            raise RuntimeError("RECIPE_MONGO_PORT is undefined")

    @property
    def host(self):
        return "{}:{}".format(self.__host, self.__port)


class Database(object):
    """Represents a mocked database."""

    _system_databases = ("admin", "local")

    def __init__(self, lightweight=False, alias=None):
        self.__lightweight = lightweight
        self.__mongodb = mongodb = _mock_mongodb()

        if not lightweight:
            self.__disconnect()

        params = {"alias": alias or mongoengine.DEFAULT_CONNECTION_NAME, "host": mongodb.host}
        if IS_PYMONGO_2:
            params["use_greenlets"] = True

        self.connection = mongoengine.connect("mock", **params)
        mongodb.connected = True

        try:
            if lightweight:
                if mongodb.dirty:
                    self.__erase()
            else:
                if not mongodb.empty:
                    self.__drop_databases()

            mongodb.empty = False
            mongodb.dirty = True
        except:
            self.close()
            raise

    def close(self):
        if not self.__lightweight:
            self.__drop_databases()
            self.__disconnect()

    def __erase(self):
        for db_name in self.connection.database_names():
            if db_name in self._system_databases:
                continue

            db = self.connection[db_name]
            for collection_name in db.collection_names(include_system_collections=False):
                if IS_PYMONGO_2:
                    db[collection_name].remove({})
                else:
                    db[collection_name].delete_many({})

        self.__mongodb.dirty = False

    def __drop_databases(self):
        for db_name in self.connection.database_names():
            if db_name not in self._system_databases:
                self.connection.drop_database(db_name)

        self.__mongodb.dirty = False
        self.__mongodb.empty = True

    def __disconnect(self):
        disconnect()
        self.__mongodb.connected = False


def disconnect():
    """Disconnects from mocked database."""

    # MongoEngine caches a lot of data. Try to purge as much caches as we can.

    for model in get_registered_models():
        try:
            del model._collection
        except AttributeError:
            pass

    for alias in list(mongoengine.connection._connections.keys()):
        mongoengine.connection.disconnect(alias)

    mongoengine.connection._connection_settings.clear()


def _mock_mongodb():
    global _MONGODB

    if _MONGODB is None:
        _MONGODB = _MongoDb()

    return _MONGODB
