import argparse
import logging
import os
import time
from urllib import parse

import requests
import yatest.common
from library.python.testing.recipe import declare_recipe, set_env
from yatest.common import source_path
from yatest.common.network import PortManager

PID_FILE = "processor-runserver.pid"
READY_PROBES = 10  # attempts


def start(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--configs', action='store',
        default=','.join([
            os.path.join("billing", "hot", "processor", "configs", "dev", name)
            for name in ['common.yml', 'manifests.yml', 'yt.yml']
        ])
    )
    args = parser.parse_args(argv)

    pm = PortManager()
    server_port = pm.get_port()
    base_url = "http://localhost:" + str(server_port)
    set_env("PROCESSOR_BASE_URL", base_url)
    set_env("PROCESSOR_RUNSERVER_PORT", str(server_port))

    binary_path = yatest.common.binary_path("billing/hot/processor/processor")
    logging.info('Binary path: ' + binary_path)
    execution = yatest.common.execute(
        [
            binary_path,
            "-c", ','.join([os.path.join(source_path(), name) for name in args.configs.split(',')]),
            'runserver'
        ],
        stdout=open(os.path.join(yatest.common.output_path(), 'processor-api.out'), 'w'),
        stderr=open(os.path.join(yatest.common.output_path(), 'processor-api.err'), 'w'),
        wait=False,
        env=dict(**os.environ.copy(), TVM_SRC='NO TVM', RECIPE_PORT=str(server_port)),
    )

    with open(PID_FILE, "w") as f:
        f.write(str(execution.process.pid))

    is_ready = False
    for _ in range(READY_PROBES):
        try:
            res = requests.get(parse.urljoin(base_url, 'ping'))
            res.raise_for_status()

            is_ready = True
            break
        except requests.RequestException as e:
            logging.warning(e)
            time.sleep(1)

    if not is_ready:
        raise Exception('processor not ready')


def stop(argv):
    with open(PID_FILE) as f:
        pid = f.read()
        logging.info("Found pid to stop: " + pid)
        os.kill(int(pid), 9)


if __name__ == "__main__":
    declare_recipe(start, stop)
