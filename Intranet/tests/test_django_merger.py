# coding: utf-8
from __future__ import unicode_literals
import pytest

from sync_tools.datagenerators import DjangoGenerator
from sync_tools.diff_merger import DjangoDiffMerger
from .app.models import Article

pytestmark = pytest.mark.django_db


class DataGenerator(DjangoGenerator):
    model = Article
    sync_fields = ('slug',)
    diff_fields = ('name', 'score')

    def get_external_objects(self):
        return [
            {'slug': 'new', 'name': 'New!'},
            {'slug': 'both', 'name': 'New name', 'score': 5},
        ]


def test_django_merger():
    Article.objects.create(slug='old', name='Old', score=5)
    Article.objects.create(slug='both', name='Old name', score=3)
    assert Article.objects.count() == 2

    merger = DjangoDiffMerger(data_generator=DataGenerator(), create=True, delete=True, update=True)
    merger.execute()
    expected = {
        'updated': 1,
        'skipped': 0,
        'created': 1,
        'deleted': 1,
        'all': 3,
        'errors': 0
    }
    assert merger.results == expected
    assert Article.objects.count() == 2
    assert Article.objects.filter(slug='old').count() == 0
    assert Article.objects.filter(slug='both').count() == 1
    assert Article.objects.filter(slug='new').count() == 1

    both = Article.objects.get(slug='both')
    assert both.name == 'New name'
    assert both.score == 5

    new = Article.objects.get(slug='new')
    assert new.score == 0
    assert new.name == 'New!'
