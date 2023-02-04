# coding: utf-8
import pytest

from itertools import islice

from mock import patch
from ..utils import test_vcr

from intranet.dogma.dogma.core.models import Source, OrganisationsToClone
from intranet.dogma.dogma.core.crawlers import get_crawler


pytestmark = pytest.mark.django_db(transaction=True)


def test_hgweb_crawler():
    source = Source.objects.create(code='hg', web_type='hgweb', web_url='https://hg.yandex-team.ru')

    with test_vcr.use_cassette('hgweb.yaml'):
        crawler = get_crawler(source)

        repos = {repo.name: repo for repo in islice(crawler.get_repos(), 20)}

        assert 'baibik' in repos
        assert 'balance-inflector' in repos

        assert 'balance' not in repos

        repo = repos['baibik']

        assert repo.owner == 'root'
        assert repo.vcs_name == 'baibik'


def test_gitweb_crawler():
    source = Source.objects.create(code='git', web_type='gitweb', web_url='https://git.yandex-team.ru/gitweb')

    with test_vcr.use_cassette('gitweb.yaml'):
        crawler = get_crawler(source)

        repos = {repo.name: repo for repo in islice(crawler.get_repos(), 20)}

        assert 'aandrosov-mail-app' in repos

        repo = repos['aandrosov-mail-app']

        assert repo.owner == 'root'
        assert repo.vcs_name == 'aandrosov/mail-app.git'


def test_stash_crawler():
    source = Source.objects.create(code='stash-desktop', web_type='stash', web_url='https://stash.desktop.dev.yandex.net',
                                   web_auth='basic')

    with test_vcr.use_cassette('stash.yaml'):
        crawler = get_crawler(source)

        repos = {repo.name: repo for repo in islice(crawler.get_repos(), 50)}

        assert 'browser-foreman' in repos

        repo = repos['browser-foreman']

        assert repo.owner == 'autotest'
        assert repo.vcs_name == 'autotest/browser-foreman.git'
        assert repo.default_branch == 'develop'

        repo = repos['browser-android-old']

        assert repo.owner == 'abro'


def test_bb_is_public():
    source = Source.objects.create(code='stash-desktop', web_type='bitbucket_yateam',
                                   web_url='https://stash.desktop.dev.yandex.net',
                                   web_auth='basic')

    with test_vcr.use_cassette('test_bb_is_public.yaml'):
        crawler = get_crawler(source)
        is_public = crawler.is_repo_public('metro', {'name': 'repo_name'})
        assert is_public is True

        is_public = crawler.is_repo_public('metro', {'name': 'repo_name_2'})
        assert is_public is False


def test_gitlab_crawler():
    source = Source.objects.create(code='Gitlab', web_type='gitlab',
                                   web_url='http://git.adfox.ru',
                                   web_auth='none', extra_info={}, )

    with test_vcr.use_cassette('gitlab.yaml'):
        crawler = get_crawler(source)

        repos = {repo.name: repo for repo in islice(crawler.get_repos(), 3)}

        assert 'sources' in repos

        repo = repos['sources']

        assert repo.owner == 'adfox'
        assert repo.vcs_name == 'adfox/sources'
        assert repo.default_branch == 'pre-release'


def test_github_crawler_organisations():
    source = Source.objects.create(code='Github', web_type='github',
                                   web_url='https://github.com',
                                   web_auth='none')
    with test_vcr.use_cassette('github_organisations.yaml'):
        crawler = get_crawler(source)

        owners = [
            OrganisationsToClone.objects.create(name='yandex', source=source),
        ]

        with patch('intranet.dogma.dogma.core.crawlers.github.GithubCrawler._owners_to_copy', owners):
            repos = {repo.name: repo for repo in list(crawler.get_repos())}

        assert 'yandex-tank' in repos

        repo = repos['yandex-tank']

        assert repo.owner == 'yandex'
        assert repo.vcs_name == 'yandex/yandex-tank'
        assert repo.default_branch == 'master'
        assert repo.organisation == owners[0]
