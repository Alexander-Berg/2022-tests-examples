#!/usr/bin/env python

import logging
import os
import subprocess
from logging import FileHandler

from flask import Flask, request

app = Flask(__name__)

maindir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'main'))
rootdir = os.path.dirname(os.path.abspath(__file__))


def get_file_main(fname):
    with open(os.path.join(maindir, fname), 'r') as f:
        return f.read()


def get_file_local(fname):
    with open(os.path.join(rootdir, fname), 'r') as f:
        return f.read()


@app.route('/')
def hello_world():
    return 'Hello World!'


@app.route('/oops-agent/oops-recipe.py')
def get_recipe():
    return get_file_main('oops-recipe.py')


@app.route('/bundles/oops-agent.jar')
def get_agent():
    return get_file_local('agent.zip')


@app.route('/api/agent/configs/<fqdn>')
def get_config(fqdn):
    if os.path.isfile('config'):
        return get_file_local('config')
    else:
        return '{}'


@app.route('/api/agent/feedback')
def feedback():
    open(os.path.join(rootdir, 'god_feedback'), 'w').close()
    return ''


@app.route('/api/agent/commands/ack/<fqdn>/<command>', methods=['POST'])
def got_zip(fqdn, command):
    logging.info('got ack for command %s', command)
    logging.info('content-type: %s', request.content_type)
    with open('recv.zip', 'wb') as f:
        f.write(request.stream.read())
    return ''


@app.route('/api/agent/configs/<fqdn>', methods=['POST'])
def configs(fqdn):
    if os.path.isfile(get_file_local('config')):
        return get_file_local('config')
    else:
        return '{}'


def start_server():
    log = logging.getLogger()
    log.setLevel('INFO')
    log.addHandler(FileHandler(filename='server.log'))
    os.chdir(maindir)
    subprocess.call(('zip -qr %s oops_agent.py oops-bootstrap.py context.json agent modules' % os.path.join(
        rootdir,
        'agent.zip'
    )).split())
    os.chdir(rootdir)
    app.run()


if __name__ == '__main__':
    start_server()
