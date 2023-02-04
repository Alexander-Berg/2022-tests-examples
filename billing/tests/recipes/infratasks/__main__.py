import logging
import os
import requests
import time
import yatest.common
from library.python.testing.recipe import declare_recipe
from urllib import parse
from yatest.common import source_path
from yatest.common.network import PortManager

PID_FILE = "configshop-infratasks.pid"
READY_PROBES = 10  # attempts


def start(argv):
    # находим свободный порт, на котором поднимем сервер,
    # и записываем в переменную окружения, которая будет доступна в тесте
    pm = PortManager()
    server_port = pm.get_port()

    binary_path = yatest.common.binary_path("billing/configshop/cmd/runinfratasks/runinfratasks")
    logging.info('Binary path: ' + binary_path)
    execution = yatest.common.execute(
        [binary_path, "-c", source_path() + "/billing/configshop/configs/core/dev.yaml",
            "--processor-config", source_path() + "/billing/configshop/configs/processor-infra/dev.yaml"],
        stdout=open(os.path.join(yatest.common.output_path(), 'configshop-infratasks.out'), 'w'),
        stderr=open(os.path.join(yatest.common.output_path(), 'configshop-infratasks.err'), 'w'),
        wait=False,
        env=dict(**os.environ.copy(), TVM_SRC='NO TVM', RECIPE_PORT=str(server_port)),
    )

    with open(PID_FILE, "w") as f:
        f.write(str(execution.process.pid))

    is_ready = False
    for _ in range(READY_PROBES):
        try:
            res = requests.get(parse.urljoin('http://localhost:'+str(server_port), 'ping'))
            res.raise_for_status()

            is_ready = True
            break
        except requests.RequestException as e:
            logging.warning(e)
            time.sleep(1)

    if not is_ready:
        raise Exception('infratasks not ready')


def stop(argv):
    with open(PID_FILE) as f:
        pid = f.read()
        logging.info("Found pid to stop: " + pid)
        os.kill(int(pid), 9)


if __name__ == "__main__":
    declare_recipe(start, stop)
