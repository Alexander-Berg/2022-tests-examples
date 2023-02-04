import logging
import os

import yatest.common
from library.python.testing.recipe import declare_recipe, set_env
from yatest.common import source_path
from yatest.common.network import PortManager


def start(argv):
    logging.info("SQS port is: %s", os.environ["SQS_PORT"])

    # находим свободный порт, на котором поднимем сервер,
    # и записываем в переменную окружения, которая будет доступна в тесте
    pm = PortManager()
    server_port = pm.get_port()
    set_env("FES_RUNSERVER_PORT", str(server_port))
    os.environ["RECIPE_PORT"] = str(server_port)

    aws_keys = {"AWS_ACCESS_KEY_ID": "my_user", "AWS_SESSION_TOKEN": "", "AWS_SECRET_ACCESS_KEY": "unused"}
    for k, v in aws_keys.items():
        set_env(k, v)  # Required by test
        os.environ[k] = v  # Required by runserver

    binary_path = yatest.common.binary_path("payplatform/fes/fes/cmd/runserver/runserver")
    logging.info('Binary path: ' + binary_path)
    execution = yatest.common.execute(
        [binary_path, "-c", source_path() + "/payplatform/fes/fes/config/dev/dev.yaml"],
        stdout=open(yatest.common.output_path() + '/runserver.out', 'w'),
        stderr=open(yatest.common.output_path() + '/runserver.err', 'w'),
        wait=False,
    )

    with open("fes_runserver.pid", "w") as f:
        f.write(str(execution.process.pid))


def stop(argv):
    with open("fes_runserver.pid") as f:
        pid = f.read()
        logging.info("Found pid to stop: " + pid)
        os.kill(int(pid), 9)


if __name__ == "__main__":
    declare_recipe(start, stop)
