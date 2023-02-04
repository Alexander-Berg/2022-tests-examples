import os

from library.python.testing.recipe import declare_recipe, set_env
from yatest.common.network import PortManager
from yatest.common import execute, build_path, source_path


def run(port):
    execute(
        [
            build_path("contrib/tools/fluent-bit-minimal/bin/fluent-bit"),
            '-c',
            source_path("yandex_io/tests/fluent_bit/data/test.cfg"),
        ],
        env={
            'FB_SERVER_PORT': str(port),
            'FB_TARGET_FILE': source_path("yandex_io/tests/fluent_bit/data/test.log"),
        },
    )


def start(argv):
    port = PortManager().get_port()
    set_env("FB_SERVER_PORT", str(port))

    pid = os.fork()
    if pid == 0:
        run(port)
    else:
        with open("recipe.pid", "w") as f:
            f.write(str(pid))


def stop(argv):
    with open("recipe.pid") as f:
        pid = int(f.read())
        os.kill(pid, 9)


if __name__ == "__main__":
    declare_recipe(start, stop)
