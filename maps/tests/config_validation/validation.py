import json
import unittest
import yatest.common
from pathlib import Path
from maps.pylibs.infrastructure_api.awacs import config_files

# from infra.awacs.proto
from infra.awacs.proto import model_pb2
from google.protobuf import text_format


def read_content(file_name: Path) -> str:
    with open(str(file_name)) as f:
        return f.read()


class Tests(unittest.TestCase):
    root_path = Path(yatest.common.source_path('maps/config/balancer'))

    def fqdns_list(self):
        return [dir.name for dir in self.root_path.iterdir() if not dir.is_file()]

    def orders(self, fqdn):
        order_file = Path(self.root_path, fqdn, config_files.UPSTREAMS_ORDER_FILE_NAME)
        return json.loads(read_content(order_file))

    def backends(self, fqdn):
        backends_dir = Path(self.root_path, fqdn, config_files.BACKENDS_FOLDER)
        if backends_dir.exists():
            return [backend.stem for backend in backends_dir.iterdir()]
        else:
            return []

    def test_orders(self):
        order_len_problems = set()
        missing_upstream_file_problem = set()
        missing_order_problem = set()
        for fqdn in self.fqdns_list():
            orders = self.orders(fqdn)
            for upstream, order in orders.items():
                if len(order) != 8:
                    order_len_problems.add(f"{fqdn}, {upstream}: {order}")

                if not Path(self.root_path, fqdn, f"{upstream}.{config_files.UPSTREAM_FILE_EXTENSION}").is_file():
                    missing_upstream_file_problem.add(f"{fqdn}/{upstream}.{config_files.UPSTREAM_FILE_EXTENSION}")

            for upstream_file in Path(self.root_path, fqdn).iterdir():
                if upstream_file.name.endswith(f'.{config_files.UPSTREAM_FILE_EXTENSION}'):
                    upstream_id = upstream_file.stem
                    if upstream_id not in orders:
                        missing_order_problem.add(f"{fqdn}/{upstream_id}.{config_files.UPSTREAM_FILE_EXTENSION}")

        if order_len_problems:
            raise self.failureException(f"Order len != 8 in {order_len_problems}")

        if missing_upstream_file_problem:
            raise self.failureException(f"Order exists, but upstream not found {missing_upstream_file_problem}")

        if missing_order_problem:
            raise self.failureException(f"Upstream exists, but order not found {missing_order_problem}")

    def test_backends(self):
        problems = set()
        for fqdn in self.fqdns_list():
            backends_dir = Path(self.root_path, fqdn, config_files.BACKENDS_FOLDER)
            for backend in self.backends(fqdn):
                try:
                    selector = model_pb2.BackendSelector()
                    text_format.Parse(read_content(Path(backends_dir, f"{backend}.{config_files.BACKEND_FILE_EXTENSION}")), selector)
                except text_format.ParseError as e:
                    problems.add(f"Failed to parse backend {fqdn}:{backend} with msg: {e}")
        if problems:
            raise self.failureException(problems)
