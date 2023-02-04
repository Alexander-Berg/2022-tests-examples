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
    set_env("TEMPLATE_PROJECT_RUNSERVER_PORT", str(server_port))
    os.environ["RECIPE_PORT"] = str(server_port)

    binary_path = yatest.common.binary_path("billing/template-project/cmd/runserver/runserver")
    logging.info('Binary path: ' + binary_path)
    execution = yatest.common.execute(
        [binary_path, "-c", source_path() + "/billing/template-project/config/dev.yaml"],
        stdout=open(yatest.common.output_path() + '/runserver.out', 'w'),
        stderr=open(yatest.common.output_path() + '/runserver.err', 'w'),
        wait=False,
    )

    with open("template-project_runserver.pid", "w") as f:
        f.write(str(execution.process.pid))


def stop(argv):
    with open("template-project_runserver.pid") as f:
        pid = f.read()
        logging.info("Found pid to stop: " + pid)
        os.kill(int(pid), 9)


if __name__ == "__main__":
    declare_recipe(start, stop)
