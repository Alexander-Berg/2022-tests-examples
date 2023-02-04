import logging
import os
import requests
import time
import yatest.common
from library.python.testing.recipe import declare_recipe, set_env
from urllib import parse
from yatest.common.network import PortManager

PID_FILE = "configshop-testproxy.pid"
READY_PROBES = 10  # attempts


def start(argv):
    pm = PortManager()
    server_port = pm.get_port()
    base_url = "http://localhost:" + str(server_port)

    set_env("CONFIGSHOP_TEST_PROXY_URL", base_url)

    binary_path = yatest.common.binary_path("billing/configshop/cmd/testproxy/testproxy")
    logging.info('Binary path: ' + binary_path)
    execution = yatest.common.execute(
        [binary_path],
        stdout=open(os.path.join(yatest.common.output_path(), 'configshop-testproxy.out'), 'w'),
        stderr=open(os.path.join(yatest.common.output_path(), 'configshop-testproxy.err'), 'w'),
        wait=False,
        env=dict(**os.environ.copy(), RECIPE_PORT=str(server_port)),
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
        raise Exception('testproxy not ready')


def stop(argv):
    with open(PID_FILE) as f:
        pid = f.read()
        logging.info("Found pid to stop: " + pid)
        os.kill(int(pid), 9)


if __name__ == "__main__":
    declare_recipe(start, stop)
