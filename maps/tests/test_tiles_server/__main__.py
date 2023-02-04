#!/usr/bin/env python

import os
import sys
from flask import Flask, request, make_response

app = Flask(__name__)


@app.route('/', methods=['GET'])
def is_available():
    return make_response("running", 200)


def is_valid_args(args):
    params = {"l", "x", "y", "z"}
    for param in params:
        if param not in args.keys():
            return False
    if args["l"] != "sat":
        return False
    if args["x"].isdigit() and args["y"].isdigit() and args["z"].isdigit():
        return True
    else:
        return False


BUILD_ROOT = os.environ.get('ARCADIA_BUILD_ROOT', '../../../../../../../../../..')
TILES_DIR = 'maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/tests/test_tiles_server/tiles'


@app.route('/tiles', methods=['GET'])
def get_tile():
    args = request.args.to_dict()
    if not is_valid_args(args):
        return make_response('Bad Request', 400)
    name = args['x'] + '_' + args['y'] + '_' + args['z'] + '.jpg'
    path = os.path.join(BUILD_ROOT, TILES_DIR, name)
    if not os.path.isfile(path):
        return make_response('Not Found', 404)
    with open(path, 'rb') as f:
        return make_response(f.read(), 200)


def main(argv=sys.argv):
    app.run(sys.argv[1], sys.argv[2])


if __name__ == "__main__":
    main()
