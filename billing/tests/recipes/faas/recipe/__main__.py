import argparse
import json
import logging
import os
import time
import yaml
from urllib import parse

import requests
import yatest.common
from library.python.testing.recipe import declare_recipe, set_env
from yatest.common.network import PortManager

PID_FILE = "faas-runserver.pid"
READY_PROBES = 10  # attempts


class ManifestParser:
    MANIFEST = "billing/hot/processor/configs/dev/manifests.yml"

    def __init__(self, namespaces: list[str]):
        self.namespaces = set(namespaces)
        self.functions = self.parse_functions()
        self.url_prefix = self.functions[0]['url_prefix'] if self.functions else ''

    @staticmethod
    def method_to_prefix_name(method: str) -> dict[str, str]:
        parts = method.split('/')
        prefix, name = '/' + parts[1], parts[3]
        return {'url_prefix': prefix, 'name': name}

    def parse_functions(self) -> list[dict]:
        with open(yatest.common.source_path(self.MANIFEST)) as yaml_stream:
            all_manifests = yaml.safe_load(yaml_stream)
        manifests = [
            manifest
            for manifest in all_manifests['manifests']
            if manifest['namespace'] in self.namespaces
        ]

        functions = [
            endpoint['faas'] | self.method_to_prefix_name(endpoint['calculator_method'])
            for manifest in manifests for endpoint in manifest['endpoints'].values()
        ]

        return functions


def start(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--bin-path", action="store", default="billing/hot/faas/python/bin"
    )
    parser.add_argument(
        "--namespaces", nargs='+', default=[]
    )
    args = parser.parse_args(argv)

    if args.namespaces:
        manifest_parser = ManifestParser(args.namespaces)
        os.environ['FUNCTION'] = json.dumps(manifest_parser.functions)
        os.environ['URL_PREFIX'] = manifest_parser.url_prefix

    pm = PortManager()
    server_port = pm.get_port()
    base_url = "http://localhost:" + str(server_port)
    set_env("FAAS_RUNSERVER_PORT", str(server_port))
    set_env("FAAS_BASE_URL", base_url)

    binary_path = yatest.common.binary_path(os.path.join(args.bin_path, "faas"))
    logging.info("Binary path: " + binary_path)
    logging.info("FUNCTION = " + str(os.environ.get("FUNCTION")))

    execution = yatest.common.execute(
        [binary_path, "runserver", "--port", str(server_port)],
        stdout=open(
            os.path.join(yatest.common.output_path(), "faas-runserver.out"), "w"
        ),
        stderr=open(
            os.path.join(yatest.common.output_path(), "faas-runserver.err"), "w"
        ),
        wait=False,
    )

    with open(PID_FILE, "w") as f:
        f.write(str(execution.process.pid))

    is_ready = False
    for _ in range(READY_PROBES):
        try:
            res = requests.get(parse.urljoin(base_url, "ping"))
            res.raise_for_status()

            is_ready = True
            break
        except requests.RequestException as e:
            logging.warning(e)
            time.sleep(1)

    if not is_ready:
        raise Exception("faas not ready")


def stop(argv):
    with open(PID_FILE) as f:
        pid = f.read()
        logging.info("Found pid to stop: " + pid)
        os.kill(int(pid), 9)


if __name__ == "__main__":
    declare_recipe(start, stop)
