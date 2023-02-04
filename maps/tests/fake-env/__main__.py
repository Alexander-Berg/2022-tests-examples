from gevent.pywsgi import WSGIServer
from flask import Flask, jsonify

import logging
import sys

from logging import StreamHandler
from maps.automotive.libs.large_tests.fake_env import blackbox
from maps.automotive.libs.large_tests.fake_env import datasync
from maps.automotive.libs.large_tests.fake_env import mdb
from maps.automotive.libs.large_tests.fake_env import payment

from . import alfred
from . import startrek
from . import tariff
from . import trust


app = Flask('fake-env')
handler = StreamHandler(stream=sys.stderr)
handler.setLevel(logging.DEBUG)
app.logger.addHandler(handler)

app.register_blueprint(alfred.blueprint, url_prefix='/alfred')
app.register_blueprint(blackbox.blueprint, url_prefix='/blackbox')
app.register_blueprint(datasync.blueprint, url_prefix='/datasync')
app.register_blueprint(mdb.blueprint, url_prefix='/mdb')
app.register_blueprint(payment.blueprint, url_prefix='/payment')
app.register_blueprint(startrek.blueprint, url_prefix='/startrek')
app.register_blueprint(tariff.blueprint, url_prefix='/tariff')
app.register_blueprint(trust.blueprint, url_prefix='/trust')


@app.route('/reset', methods=['POST'])
def reset():
    alfred.reset()
    blackbox.reset()
    datasync.reset()
    mdb.reset()
    payment.reset()
    startrek.reset()
    tariff.reset()
    trust.reset()

    return jsonify({})


http_server = WSGIServer(('', 80), app)
http_server.serve_forever()
