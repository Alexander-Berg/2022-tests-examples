#!/usr/bin/env python

from flask import Flask, make_response
import sys

app = Flask(__name__)


@app.route('/ping', methods=['GET'])
def ping_server():
    return make_response('Ok', 200)


def main(argv=sys.argv):
    app.run(sys.argv[1], sys.argv[2])


if __name__ == "__main__":
    main()
