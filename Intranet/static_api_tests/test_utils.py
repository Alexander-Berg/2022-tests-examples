# coding: utf-8
from __future__ import unicode_literals

from datetime import datetime
from bson import ObjectId


def test_encoder():
    from static_api.views.utils import MongoDocumentEncoder

    o = datetime(2013, 05, 17, 20, 47, 28)
    assert MongoDocumentEncoder().default(o) == '2013-05-17T20:47:28'

    o = ObjectId()
    assert MongoDocumentEncoder().default(o) == str(o)


def test_make_production_link(rf):
    from static_api.views.utils import make_production_link

    request = rf.get('persons', {'_debug': 1, '_fields': 'official'})
    link = next(make_production_link(request, {'official.is_robot': 1, 'official.is_ext': 1, '_id': 0}))
    assert link == ('production_uri', 'http://testserver/persons?_fields=official.is_ext%2Cofficial.is_robot')
