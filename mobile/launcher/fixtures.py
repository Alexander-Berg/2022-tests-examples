import json
import os
from glob import glob

import yatest.common as yc

from yaphone.advisor.launcher.views.browser_icons import extra_relative_icon_urls


dir_path = yc.source_path(os.path.join('yaphone/advisor/launcher/tests/fixtures'))


def make_fixtures_path(filename):
    path = os.path.join(dir_path, filename)
    return path


def do_mock(mock, address, data):
    mock.get(address, content=data)
    mock.head(address, status_code=200)


def load_requests_fixtures(mock):
    for item in glob(os.path.join(dir_path, '*')):
        if os.path.isdir(item):
            host = os.path.basename(item)

            for extra_icon in extra_relative_icon_urls:
                address = 'http://%s/%s' % (host, os.path.basename(extra_icon))
                mock.head(address, status_code=404)

            with open(os.path.join(item, 'main.html')) as main:
                do_mock(mock, 'http://%s/' % host, main.read())

            icons = glob(os.path.join(item, '*.png'))
            icons.extend(glob(os.path.join(item, '*.ico')))

            for icon in icons:
                with open(icon, 'rb') as icon_file:
                    do_mock(mock, 'http://%s/%s' % (host, os.path.basename(icon)), icon_file.read())


def load_fixture(name, model_class):
    path = make_fixtures_path('{}.json'.format(name))

    with open(path, 'r') as f:
        objects = json.load(f)

    for object in objects:
        model_class.from_json(json.dumps(object)).save(force_insert=True)


def get_localization_fixture(name):
    path = make_fixtures_path('{}_l10n.json'.format(name))
    with open(path, 'r') as f:
        return f.read()
