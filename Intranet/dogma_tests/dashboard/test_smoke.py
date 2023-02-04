# coding: utf-8



import pytest

from django.core.urlresolvers import reverse
from django.test.client import Client
from django.test import override_settings

from .factories import CloneFactory, RepoFactory, SourceFactory


pytestmark = pytest.mark.django_db(transaction=True)


@pytest.fixture
def fx_repo(transactional_db,):
    return RepoFactory()


@pytest.fixture
def fx_clone(transactional_db,):
    return CloneFactory()


@pytest.fixture
def fx_source(transactional_db,):
    return SourceFactory()


def _test_impl(url_name, reverse_args=None, reverse_kwargs=None,
               expect_code=200):
    client = Client()
    with override_settings(YAUTH_TEST_USER='testuser'):
        response = client.get(
            reverse(url_name, args=reverse_args, kwargs=reverse_kwargs)
        )
        assert response.status_code == expect_code


def test_index():
    _test_impl('dashboard:index', expect_code=302)


def test_sources_list():
    _test_impl('dashboard:sources_list')


def test_source_details(fx_source):
    _test_impl('dashboard:source_details', [fx_source.id], expect_code=405)


def test_repos_list():
    _test_impl('dashboard:repos_list')


def test_repo_details(fx_repo):
    _test_impl('dashboard:repo_details', [fx_repo.id])


def test_nodes_list():
    _test_impl('dashboard:nodes_list')


def test_clone_details(fx_clone):
    _test_impl('dashboard:clone_details', [fx_clone.id])
