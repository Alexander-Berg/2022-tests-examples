"""Provides various utilities for testing Flask applications."""

from __future__ import unicode_literals

import json
import unittest

from werkzeug.datastructures import Headers

import flask.testing

from sepelib.mongo.mock import Database


class FlaskTestCase(unittest.TestCase):
    """Base class for all Flask test cases."""

    def __init__(self, *args, **kwargs):
        super(FlaskTestCase, self).__init__(*args, **kwargs)

        # It's not recommended to change test case constructor signature,
        # so we have to pass parameters via object attributes.

        # The `app` attribute must be set by a derived class.
        assert hasattr(self, "app")
        self.lightweight_db_mocking = getattr(self, "lightweight_db_mocking", False)

        self.db = None

    def setUp(self):
        super(FlaskTestCase, self).setUp()

        try:
            self.app.config["TESTING"] = True
            self.db = Database(lightweight=self.lightweight_db_mocking)
        except:
            FlaskTestCase.tearDown(self)
            raise

    def tearDown(self):
        try:
            self.app.config["TESTING"] = False

            if self.db is not None:
                self.db.close()
        finally:
            super(FlaskTestCase, self).tearDown()

    @property
    def client(self):
        """Returns a client to the application."""

        return self.app.test_client()

    @property
    def api_client(self):
        """Returns a client to the application which sends and accepts JSON by default."""

        class Response(self.app.response_class):
            @property
            def json(self):
                return json.loads(self.data)

        return _ApiClient(self.app, Response)


class _ApiClient(flask.testing.FlaskClient):
    """Overrides Flask's client to simplify working with API."""

    def open(self, *args, **kwargs):
        kwargs["headers"] = headers = Headers(kwargs.get("headers", {}))
        headers.setdefault("Accept", "application/json")

        if kwargs.get("method") in ("PUT", "PATCH", "POST", "DELETE") and isinstance(kwargs.get("data"), dict):
            content_type = headers.setdefault("Content-Type", "application/json")
            if content_type == "application/json":
                kwargs["data"] = json.dumps(kwargs["data"])

        return super(_ApiClient, self).open(*args, **kwargs)
