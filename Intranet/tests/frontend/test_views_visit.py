# coding: utf-8

from __future__ import unicode_literals

import pytest
import json

from tests import helpers

from easymeeting.core import models


@pytest.mark.django_db
def test_get_last_visit_view_has_recently_answer(client):
    models.Visit.objects.create(
        uid='1120000000073516',
        params=json.dumps({'data': 'first visit data'}),
        is_helpful=True,
    )
    models.Visit.objects.create(
        uid='1120000000073516',
        params=json.dumps({'data': 'second visit data'}),
        is_helpful=None,
    )
    actual = helpers.get_json(
        client=client,
        path='/frontend/visit/'
    )
    assert actual == {}


@pytest.mark.django_db
def test_get_last_visit_view_filter_by_uid(client):
    models.Visit.objects.create(
        uid='1120000000073516',
        params=json.dumps({'data': 'first visit data'}),
        is_helpful=None,
    )
    models.Visit.objects.create(
        uid='2222222222222222',
        params=json.dumps({'data': 'other visit data'}),
        is_helpful=None,
    )
    actual = helpers.get_json(
        client=client,
        path='/frontend/visit/'
    )
    assert actual['params'] == {'data': 'first visit data'}


@pytest.mark.django_db
def test_get_last_visit_view_sorted_by_created_at(client):
    models.Visit.objects.create(
        uid='1120000000073516',
        params=json.dumps({'data': 'first visit data'}),
        is_helpful=None,
    )
    models.Visit.objects.create(
        uid='1120000000073516',
        params=json.dumps({'data': 'second visit data'}),
        is_helpful=None,
    )
    actual = helpers.get_json(
        client=client,
        path='/frontend/visit/'
    )
    assert actual['params'] == {'data': 'second visit data'}


@pytest.mark.django_db
def test_get_empty_last_visit_view(client):
    actual = helpers.get_json(
        client=client,
        path='/frontend/visit/'
    )
    assert actual == {}


@pytest.mark.django_db
def test_mark_visit(client):
    models.Visit.objects.create(
        id=2,
        uid='1120000000073516',
        params='{}',
        is_helpful=None,
    )
    helpers.post_json(
        client=client,
        path='/frontend/visit/',
        json_data={'id': 2, 'isHelpful': False},
        expect_status=204,
        json_response=False,
    )
    actual = models.Visit.objects.get(id=2)
    assert actual.is_helpful is False
