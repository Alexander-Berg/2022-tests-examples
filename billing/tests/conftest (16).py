from contextlib import contextmanager
from datetime import datetime, timedelta
from os.path import dirname, abspath, join

import pytest
from django.contrib.auth.models import User
from django.test import Client
from freezegun import freeze_time

try:
    import library.python
    import pkgutil

    ARCADIA_RUN = True

except ImportError:
    ARCADIA_RUN = False


@pytest.fixture
def read_fixture(dir_fixtures):

    def read_fixture_(filename):
        file_path = dir_fixtures(filename)

        if ARCADIA_RUN:
            data = pkgutil.get_data(__package__, file_path)

        else:
            with open(file_path, 'rb') as f:
                data = f.read()

        return data

    return read_fixture_


@pytest.fixture
def extract_fixture(read_fixture, tmpdir):
    """Изымает файл фикстуры данных во временную директорию.
    Может потребоваться для работы некоторых тестов как из бинарной сборки,
    так и без неё.

    """
    def extract_fixture_(filename, ensure=False):
        data = read_fixture(filename)
        tmp_filepath = tmpdir.join(filename)
        tmp_filepath.write_binary(data, ensure)
        return f'{tmp_filepath}'

    return extract_fixture_


@pytest.fixture
def dir_fixtures(dir_module):

    def dir_fixtures_(filename=None):
        path_chunks = [dir_module, 'fixtures']
        filename and path_chunks.append(filename)
        return join(*path_chunks)

    return dir_fixtures_


@pytest.fixture
def dir_module(request):
    filename = request.module.__file__
    if not ARCADIA_RUN:
        filename = abspath(filename)
    return dirname(filename)


@pytest.fixture
def time_forward():
    """Менеджер контекста. Позволяет передвинуть время вперёд на указанное количество секунд."""

    @contextmanager
    def time_forward_(seconds, *, utc=True):
        now_func = datetime.utcnow if utc else datetime.now

        with freeze_time(now_func() + timedelta(seconds=seconds)):
            yield

    return time_forward_


@pytest.fixture
def mock_solomon(monkeypatch):
    """Имитирует запрос на отправку данных в Соломон."""

    class RequestsMock():

        def post(self, *args, **kwargs):
            return

    monkeypatch.setattr('refs.core.notifiers.requests', RequestsMock())


@pytest.fixture
def mock_sleepy_sync(monkeypatch):
    """Имитирует сон при запуске засыпающих синхронизаций."""

    monkeypatch.setattr('refs.core.common.sleep', lambda _: None)


@pytest.fixture
@pytest.mark.django_db
def admin_client():
    cl = Client()

    user = User(username='a', is_superuser=True, is_active=True, is_staff=True)
    user.set_password('b')
    user.save()

    cl.login(username='a', password='b')

    return cl
