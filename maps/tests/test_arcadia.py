import typing as tp
from collections.abc import Callable
from unittest.mock import Mock, call
from pathlib import PurePath, Path, PosixPath

import pytest

from maps.infra.sedem.sandbox.config_applier.lib.arcadia import Arcadia


@pytest.fixture
def mock_arc() -> Callable[..., Mock]:
    def factory(revision: int = 123,
                author: str = 'nobody',
                message: str = '<unknown>',
                commit_changes: tp.Iterator[list[str]] = None) -> Mock:
        mock = Mock()
        mock.show.return_value = [{
            'commits': [{
                'revision': revision,
                'author': author,
                'message': message,
            }],
            'names': [
                {
                    'path': changed_file,
                    'status': 'modified',
                }
                for changed_file in (commit_changes or [])
            ],
        }]
        return mock
    return factory


def test_root(mock_arc) -> None:
    arcadia = Arcadia(
        arc=mock_arc(),
        arcadia_root='/fake-root',
    )
    assert arcadia.root() == Path('/fake-root')


def test_revision(mock_arc) -> None:
    arcadia = Arcadia(
        arc=mock_arc(revision=42),
        arcadia_root='/fake-root',
    )
    assert arcadia.revision() == 42


def test_head_commit_description(mock_arc) -> None:
    arcadia = Arcadia(
        arc=mock_arc(
            revision=42,
            author='john-doe',
            message='feature',
        ),
        arcadia_root='/fake-root',
    )
    assert arcadia.head_commit_description() == 'r42: (@john-doe) feature'


def test_iter_commited_changes(mock_arc) -> None:
    arcadia = Arcadia(
        arc=mock_arc(
            commit_changes=[
                'maps/infra/service',
                'devtools/service',
            ]
        ),
        arcadia_root='/fake-root',
    )
    assert sorted(arcadia.iter_commited_changes()) == [
        PurePath('devtools/service'),
        PurePath('maps/infra/service'),
    ]


def test_iter_unstaged_changes(mock_arc) -> None:
    arc = mock_arc()
    arc.status.return_value = {'status': {
        'changed': [{
            'path': 'maps/infra/service',
            'status': 'modified',
        }],
        'untracked': [{
            'path': 'maps/infra/new_service',
            'status': 'untracked',
        }],
    }}
    arcadia = Arcadia(
        arc=arc,
        arcadia_root='/fake-root',
    )
    assert sorted(arcadia.iter_unstaged_changes()) == [
        PurePath('maps/infra/new_service'),
        PurePath('maps/infra/service'),
    ]


def test_create_review(mock_arc) -> None:
    arc = mock_arc()
    arc.user_info.return_value = {'effective_login': 'john-doe'}
    arc.pr_status.return_value = {'id': 42}
    arcadia = Arcadia(
        arc=arc,
        arcadia_root='/fake-root',
    )

    review_id = arcadia.create_review(
        changes=[
            PurePath('maps/infra/service'),
            PurePath('maps/infra/another_service'),
        ],
        message='new pr',
        branch='new_pr_branch',
    )
    assert review_id == 42

    assert arc.mock_calls == [
        call.show(mount_point=PosixPath('/fake-root'), commit='HEAD', as_dict=True, name_status=True),

        call.add(mount_point=PosixPath('/fake-root'), path='/fake-root/maps/infra/service'),
        call.add(mount_point=PosixPath('/fake-root'), path='/fake-root/maps/infra/another_service'),
        call.checkout(mount_point=PosixPath('/fake-root'), create_branch=True, branch='new_pr_branch'),
        call.commit(mount_point=PosixPath('/fake-root'), message='new pr'),
        call.user_info(mount_point=PosixPath('/fake-root'), as_dict=True),
        call.push(mount_point=PosixPath('/fake-root'), upstream='users/john-doe/new_pr_branch'),
        call.pr_create(mount_point=PosixPath('/fake-root'), message='new pr', publish=True, auto=False, no_commits=True),
        call.pr_status(mount_point=PosixPath('/fake-root'), as_dict=True),
        call.checkout(mount_point=PosixPath('/fake-root'), branch='-'),
    ]
