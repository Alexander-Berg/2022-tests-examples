# coding: utf-8
from __future__ import unicode_literals

from django.conf import settings
from jinja2 import Environment, PackageLoader

env = Environment(loader=PackageLoader(package_name='static_api'))
env.globals['settings'] = settings


def test_docs_index():
    template = env.get_template('static_api/docs/index.html')

    rendered = template.render({
        'static': lambda *a, **kw: 'static'
    })

    assert rendered
