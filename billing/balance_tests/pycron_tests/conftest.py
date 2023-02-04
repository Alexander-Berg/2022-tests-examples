from contextlib import contextmanager
from importlib import import_module

import pytest
import sqlalchemy.exc

from butils.application import getApplication


@pytest.fixture()
def mock_logbroker_app_base(mock_logbroker, session):
    @contextmanager
    def _get_app(module_name, app_name, config=None, messages=None):
        with mock_logbroker(messages):
            module = import_module(module_name)
            try:
                reload(module)
            except sqlalchemy.exc.SAWarning:
                pass
            app = getattr(module, app_name)
            if config is not None:
                session.config.set(
                    app.CFG_NAME, config, column_name="value_json", can_create=True
                )
                session.flush()
            yield app(getApplication())

    return _get_app
