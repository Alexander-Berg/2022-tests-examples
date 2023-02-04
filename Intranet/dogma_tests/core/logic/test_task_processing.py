import pytest

from intranet.dogma.dogma.core.models import Node, Clone, Source, Repo
from intranet.dogma.dogma.core.utils import get_current_node
from ..git.utils import GIT_ROOT
from intranet.dogma.dogma.core.logic.tasks_processing import (mark_object_as_failed,
                                               mark_object_as_successful,
                                               retry_operational_error,)
from intranet.dogma.dogma.core.errors.base import ConnectionError

from django.db import OperationalError
from mock import patch

pytestmark = pytest.mark.django_db(transaction=True)


@pytest.fixture
def source(transactional_db):
    return Source.objects.create(code='svn',
                                 web_type='websvn',
                                 vcs_type='svn',
                                 web_url='https://svn.yandex-team.ru/websvn/wsvn',
                                 host='svn.yandex-team.ru',
                                 web_auth='basic',
                                 vcs_protocol='https',
                                 rate=0.5)


@pytest.fixture
def repo(source):
    return Repo.objects.create(
        source=source,
        vcs_name='adv',
        name='adv',
        owner='root',
    )


@pytest.fixture
def clone(repo):
    return Clone.objects.create(
        repo=repo,
        path=GIT_ROOT,
        node=get_current_node(create_missing=True),
        status=Clone.STATUSES.active,
    )


@pytest.fixture
def node(transactional_db):
    return Node.objects.create(
        hostname='dogma.yandex.net',
        space_total=1000,
        space_available=700,
    )


def test_mark_object_as_failed_with_repo_success(repo):
    mark_object_as_failed(repo, trace='test', parsed_error=ConnectionError)


def test_mark_object_as_failed_with_source_success(source):
    mark_object_as_failed(source, trace='test', parsed_error=ConnectionError)


def test_mark_object_as_failed_with_clone_fail(clone):
    with pytest.raises(AttributeError) as err:
        mark_object_as_failed(clone, trace='test', parsed_error=ConnectionError)
    assert "'Clone' object has no attribute 'SYNC_STATUSES'" in str(err.value)


def test_mark_object_as_failed_with_node_fail(node):
    with pytest.raises(AttributeError) as err:
        mark_object_as_failed(node, trace='test', parsed_error=ConnectionError)
    assert "'Node' object has no attribute 'SYNC_STATUSES'" in str(err.value)


def test_mark_object_as_successful_with_repo_success(repo):
    mark_object_as_successful(repo)


def test_mark_object_as_successful_with_source_success(source):
    mark_object_as_successful(source)


def test_mark_object_as_successful_with_clone_fail(clone):
    with pytest.raises(AttributeError) as err:
        mark_object_as_successful(clone)
    assert "'Clone' object has no attribute 'SYNC_STATUSES'" in str(err.value)


def test_mark_object_as_successful_with_node_fail(node):
    with pytest.raises(AttributeError) as err:
        mark_object_as_successful(node)
    assert "'Node' object has no attribute 'SYNC_STATUSES'" in str(err.value)


def test_retry_operational_error_success():
    with patch('intranet.dogma.dogma.core.tasks.fetch_clone') as action:
        action.side_effect = [OperationalError, 'result']
        result = retry_operational_error(action, 'some_id')
        assert result == 'result'


def test_retry_operational_error_fail():
    with patch('intranet.dogma.dogma.core.tasks.fetch_clone') as action:
        action.side_effect = [OperationalError, OperationalError, 'result']
        result = retry_operational_error(action, 'some_id')
        assert not result


def test_retry_operational_error_fail_with_wrong_exception():
    with patch('intranet.dogma.dogma.core.tasks.fetch_clone') as action:
        action.side_effect = [Exception, 'result']
        pytest.raises(Exception, retry_operational_error, action, 'some_id')


