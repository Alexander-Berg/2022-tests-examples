from codecs import decode
from shapely import (
    wkb,
)
import maps.tools.easyview.pylib.draw as ev
from maps.wikimap.feedback.pushes.entrances.prepare_entrances.tests.lib import (
    data,
)
import argparse
import sys


OPACITY = 0.4


def _parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--resfile',
        help='Path to result file')
    return parser.parse_args()


def generate(args):
    for i, shape_hex in data.SHAPES.items():
        if shape_hex is None:
            continue
        shape = wkb.loads(decode(shape_hex, "hex"))
        yield ev.polygon(
            shape.exterior.coords[:-1],
            data={"bld_id": i},
            style=ev.polygonstyle(
                fill=ev.rgba(0, 0, 0, 0),
                outline=ev.rgba(0, 0, 0, OPACITY),
            ),
        )
    for node in data.NODE:
        yield ev.point(
            node["x"],
            node["y"],
            data={"node_id": node["node_id"]},
            style=ev.pointstyle(
                fill=ev.rgba(0, 0, 1, OPACITY),
                outline=ev.rgba(0, 0, 0, 0),
                radius=2,
            ),
        )


def main():
    args = _parse_args()
    ev.dump(
        generate(args),
        open(args.resfile, "wb") if args.resfile is not None else sys.stdout.buffer,
    )
