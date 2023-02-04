import logging
import os

import yatest.common
from library.python.testing.recipe import declare_recipe, set_env
from yatest.common import source_path
from yatest.common.network import PortManager


def start(argv):
    # находим свободный порт, на котором поднимем сервер,
    # и записываем в переменную окружения, которая будет доступна в тесте
    pm = PortManager()
    server_port = pm.get_port()
    set_env("ACCOUNTS_API_PORT", str(server_port))
    set_env("ACCOUNTS_BASE_URL", "http://localhost:" + str(server_port))

    binary_path = yatest.common.binary_path("billing/hot/accounts/cmd/api/api")
    logging.info('Binary path: ' + binary_path)
    execution = yatest.common.execute(
        [binary_path, "runserver",
         "-c", source_path() + "/billing/hot/accounts/configs/api/dev.yaml",
         "-s", source_path() + "/billing/hot/accounts/configs/settings/dev.yaml"],
        stdout=open(os.path.join(yatest.common.output_path(), 'accounts-api.out'), 'w'),
        stderr=open(os.path.join(yatest.common.output_path(), 'accounts-api.err'), 'w'),
        wait=False,
        env=dict(**os.environ.copy(), RECIPE_PORT=str(server_port))
    )

    with open("accounts_api.pid", "w") as f:
        f.write(str(execution.process.pid))


def stop(argv):
    with open("accounts_api.pid") as f:
        pid = f.read()
        logging.info("Found pid to stop: " + pid)
        os.kill(int(pid), 9)


if __name__ == "__main__":
    declare_recipe(start, stop)
