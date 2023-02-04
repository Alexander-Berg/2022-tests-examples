import os
import pytest
import yatest


@pytest.fixture(scope="session", autouse=True)
def migrate_db(request):
    if os.environ.get('NO_MIGRATE_DB'):
       return

    from intranet.yandex_directory.src.yandex_directory import app
    from intranet.yandex_directory.src.yandex_directory.setup import setup_app
    setup_app(app)

    from intranet.yandex_directory.src.yandex_directory.common.commands.migrate_database import migrate
    local_path = 'intranet/yandex_directory/src/yandex_directory/core/db/migrations'
    try:
        path = yatest.common.source_path(local_path)
    except AttributeError:
        # only for local pycharm tests
        path = os.path.join(os.environ["Y_PYTHON_SOURCE_ROOT"], local_path)

    with app.app_context():
        migrate(
            rebuild=False,
            verbose=True,
            path=path,
            admin_user=os.environ.get('PG_LOCAL_USER'),
        )
    os.environ['NO_MIGRATE_DB'] = '1'


@pytest.fixture(autouse=True)
def pycharm_connect():
    for name in ('REMOTE_DEBUG_EGG_PATH', 'REMOTE_DEBUG_PORT'):
        if name not in os.environ:
            print(f"{name} is not set, WILL NOT connect to PyCharm debugger")
            return
    egg_path = os.environ['REMOTE_DEBUG_EGG_PATH']
    import sys
    sys.path.append(egg_path)
    try:
        import pydevd_pycharm

        pydevd_pycharm.settrace(
            'localhost',
            port=int(os.environ['REMOTE_DEBUG_PORT']),
            stdoutToServer=False,
            stderrToServer=False
        )
        print('CONNECTED to pycharm remote debugger')
    except ImportError:
        print('%s not found' % egg_path)
