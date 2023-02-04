from library.python import resource
from maps.analyzer.services.eta_comparison.lib.logbroker.parser import (
    RouterParser,
    RouterDecompressor,
)


def test_router_parser():
    decompressor = RouterDecompressor()
    parser = RouterParser()
    for d in decompressor(bytes(resource.find('router_entry'))):
        parser.parse(d)
