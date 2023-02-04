import binascii
import json
import os
import subprocess

import pytest
import responses
from click.testing import CliRunner

from billing.yandex_pay.tools.release.lib.commands.get_commits import cli
from billing.yandex_pay.tools.release.lib.common import GET_BRANCH_URL, GET_COMMITS_URL, GET_TIMELINE_URL


@pytest.fixture
def mocked_responses():
    with responses.RequestsMock(assert_all_requests_are_fired=False) as rsps:
        yield rsps


@pytest.fixture(autouse=True)
def mock_arc_call(mocker):
    mock_result = mocker.Mock(spec=subprocess.CompletedProcess)
    mock_result.stdout = b'fake_token'
    mocker.patch.object(subprocess, 'run', return_value=mock_result)


@pytest.fixture
def no_commits_response():
    return {
        "commits": [],
        "releaseCommits": [],
        "offset": {
            "total": {
                "value": "0"
            },
            "hasMore": False
        },
        "next": None
    }


@pytest.fixture
def one_commit_response():
    def _inner(hash_: str = None, seconds: int = None):  # noqa: E251
        hash_ = hash_ or '3c51325af2d1c81468546a49447a5d1692a571a3'
        seconds = str(seconds or 1619553690)
        return {
            "commits": [],
            "releaseCommits": [
                {
                    "cancelledReleases": [],
                    "branches": [],
                    "commit": {
                        "issues": ["YANDEXPAY-0"],
                        "cancelledReleases": [],
                        "revision": {
                            "hash": hash_,
                            "branch": "trunk",
                            "number": "2",
                            "pullRequestId": "0"
                        },
                        "date": {
                            "seconds": seconds,
                            "nanos": 0
                        },
                        "message": "Fake message",
                        "author": "d-deriabin",
                        "pullRequestId": 0
                    }
                }
            ],
            "offset": {
                "total": {
                    "value": "0"
                },
                "hasMore": False
            },
            "next": None
        }
    return _inner


@pytest.fixture
def one_branch_timeline_response():
    return {
        "items": [
            {
                "timelineBranch": {
                    "branch": {
                        "name": "releases/yandex_pay/release-0",
                        "createdBy": "d-deriabin",
                        "created": {
                            "seconds": "1619682232",
                            "nanos": 681849000
                        },
                        "baseRevisionHash": "3c51325af2d1c81468546a49447a5d1692a571a3",
                        "trunkCommitsCount": 1,
                        "branchCommitsCount": 0,
                        "activeLaunchesCount": 0,
                        "completedLaunchesCount": 0,
                        "cancelledLaunchesCount": 0,
                    },
                    "freeBranchCommitsCount": 0,
                    "lastBranchRelease": None,
                },
                "item": "timelineBranch"
            }
        ],
        "next": None,
        "offset": {
            "total": 1,
            "hasMore": False
        }
    }


@pytest.fixture
def two_branch_timeline_response():
    return {
        "items": [
            {
                "timelineBranch": {
                    "branch": {
                        "name": f"releases/yandex_pay/release-{count}",
                        "createdBy": "d-deriabin",
                        "created": {
                            "seconds": f"161968223{count}",
                            "nanos": 0
                        },
                        "baseRevisionHash": binascii.hexlify(os.urandom(20)).decode(),
                        "trunkCommitsCount": 1,
                        "branchCommitsCount": 0,
                        "activeLaunchesCount": 0,
                        "completedLaunchesCount": 0,
                        "cancelledLaunchesCount": 0,
                    },
                    "freeBranchCommitsCount": 0,
                    "lastBranchRelease": None,
                },
                "item": "timelineBranch",
            }
            for count in range(2, 0, -1)
        ],
        "next": None,
        "offset": {
            "total": 1,
            "hasMore": False
        }
    }


@pytest.fixture(autouse=True)
def mock_get_branch(mocked_responses):
    mocked_responses.add(
        responses.POST,
        GET_BRANCH_URL,
        json={'branch': {'completedLaunchesCount': 0}},
        status=200,
    )


@pytest.mark.parametrize(
    'cli_args,error_message,num_requests',
    [
        ([], 'Error: No free commits for branch trunk.', 1),
        (['--branch', 'fake_branch'], 'Error: No free commits for branch fake_branch.', 1),
        # 4 = GetBranch, GetTimeline, GetCommits (free), GetCommits (release)
        (['--release', '0'], 'Error: No commits found in release 0.', 4),
    ]
)
def test_no_commits_found(
    mocked_responses, no_commits_response, cli_args, error_message, one_branch_timeline_response, num_requests
):
    mocked_responses.add(
        responses.POST,
        GET_COMMITS_URL,
        json=no_commits_response,
        status=200,
    )
    mocked_responses.add(
        responses.POST,
        GET_TIMELINE_URL,
        json=one_branch_timeline_response,
        status=200,
    )

    runner = CliRunner()
    response = runner.invoke(cli, cli_args)
    assert response.exit_code != 0
    assert response.output.strip() == error_message
    assert len(mocked_responses.calls) == num_requests


def test_one_commit_returned_for_branch(mocked_responses, one_commit_response):
    mocked_responses.add(
        responses.POST,
        GET_COMMITS_URL,
        json=one_commit_response(),
        status=200,
    )

    runner = CliRunner()
    response = runner.invoke(cli)
    expected_output = (
        '#|\n|| # | Message | Issues | PR | Revision | Author ||\n'
        '|| 1 | Fake message | YANDEXPAY-0 |  | '
        '((https://a.yandex-team.ru/arc_vcs/commit/3c51325af2d1c81468546a49447a5d1692a571a3 3c51325)) | '
        'кто:d-deriabin ||\n|#'
    )

    assert response.exit_code == 0
    assert response.output.strip() == expected_output


def test_one_commit_returned_for_release(
    mocked_responses, one_commit_response, one_branch_timeline_response
):
    mocked_responses.add(
        responses.POST,
        GET_COMMITS_URL,
        json=one_commit_response(),
        status=200,
    )
    mocked_responses.add(
        responses.POST,
        GET_TIMELINE_URL,
        json=one_branch_timeline_response,
        status=200,
    )

    runner = CliRunner()
    response = runner.invoke(cli, ['--release', '0'])
    expected_output = (
        '#|\n|| # | Message | Issues | PR | Revision | Author ||\n'
        '|| 1 | Fake message | YANDEXPAY-0 |  | '
        '((https://a.yandex-team.ru/arc_vcs/commit/3c51325af2d1c81468546a49447a5d1692a571a3 3c51325)) | '
        'кто:d-deriabin ||\n|#'
    )
    assert response.exit_code == 0
    assert response.output.strip() == expected_output


def test_two_release_branches(
    mocked_responses, one_commit_response, two_branch_timeline_response
):
    count = 0

    fake_commits = (
        ('6b86b273ff34fce19d6b804eff5a3f5747ada4ea', 1619553699),  # free commits, release 2
        ('d4735e3a265e16eee03f59718b9b5d03019c07d8', 1619553698),  # release commits, release 2
        ('4e07408562bedb8b60ce05c1decfe3ad16b72230', 1619553697),  # free commits, release 1
    )

    def commits_request_callback(request):
        nonlocal count

        resp_body = one_commit_response(*fake_commits[count])
        count += 1
        return 200, {}, json.dumps(resp_body)

    def timeline_request_callback(request):
        body = json.loads(request.body)
        if 'branch' in body:
            resp_body = {'items': [{'release': {'id': {'number': 2}}, 'item': 'release'}]}
        else:
            resp_body = two_branch_timeline_response
        return 200, {}, json.dumps(resp_body)

    mocked_responses.add_callback(
        responses.POST,
        GET_COMMITS_URL,
        callback=commits_request_callback,
        content_type='application/json',
    )
    mocked_responses.add_callback(
        responses.POST,
        GET_TIMELINE_URL,
        callback=timeline_request_callback,
        content_type='application/json',
    )

    runner = CliRunner()
    response = runner.invoke(cli, ['--release', '2'])
    expected_output = (
        '#|\n'
        '|| # | Message | Issues | PR | Revision | Author ||\n'
        '|| 1 | Fake message | YANDEXPAY-0 |  | '
        '((https://a.yandex-team.ru/arc_vcs/commit/4e07408562bedb8b60ce05c1decfe3ad16b72230 4e07408)) '
        '| кто:d-deriabin ||\n'
        '|| 2 | Fake message | YANDEXPAY-0 |  | '
        '((https://a.yandex-team.ru/arc_vcs/commit/d4735e3a265e16eee03f59718b9b5d03019c07d8 d4735e3)) '
        '| кто:d-deriabin ||\n'
        '|| 3 | Fake message | YANDEXPAY-0 |  | '
        '((https://a.yandex-team.ru/arc_vcs/commit/6b86b273ff34fce19d6b804eff5a3f5747ada4ea 6b86b27)) '
        '| кто:d-deriabin ||\n'
        '|#'
    )
    assert response.exit_code == 0
    assert response.output.strip() == expected_output


def test_paginated_response(mocked_responses, one_commit_response):
    count = 0
    fake_commits = (
        ('6b86b273ff34fce19d6b804eff5a3f5747ada4ea', 1619553699),
        ('d4735e3a265e16eee03f59718b9b5d03019c07d8', 1619553698),
        ('4e07408562bedb8b60ce05c1decfe3ad16b72230', 1619553697),
    )

    def request_callback(request):
        nonlocal count

        if count == 2:
            offset = {"total": {"value": "2"}, "hasMore": False}
            next_ = None
        else:
            offset = {"total": {"value": "2"}, "hasMore": True}
            next_ = {"branch": "fake", "number": "2"}
        resp_body = one_commit_response(*fake_commits[count])
        resp_body.update(offset=offset, next=next_)
        count += 1
        return 200, {}, json.dumps(resp_body)

    mocked_responses.add_callback(
        responses.POST,
        GET_COMMITS_URL,
        callback=request_callback,
        content_type='application/json',
    )

    runner = CliRunner()
    response = runner.invoke(cli)
    assert response.exit_code == 0
    assert len(mocked_responses.calls) == 3

    expected_output = (
        '#|\n'
        '|| # | Message | Issues | PR | Revision | Author ||\n'
        '|| 1 | Fake message | YANDEXPAY-0 |  | '
        '((https://a.yandex-team.ru/arc_vcs/commit/4e07408562bedb8b60ce05c1decfe3ad16b72230 4e07408)) '
        '| кто:d-deriabin ||\n'
        '|| 2 | Fake message | YANDEXPAY-0 |  | '
        '((https://a.yandex-team.ru/arc_vcs/commit/d4735e3a265e16eee03f59718b9b5d03019c07d8 d4735e3)) '
        '| кто:d-deriabin ||\n'
        '|| 3 | Fake message | YANDEXPAY-0 |  | '
        '((https://a.yandex-team.ru/arc_vcs/commit/6b86b273ff34fce19d6b804eff5a3f5747ada4ea 6b86b27)) '
        '| кто:d-deriabin ||\n'
        '|#'
    )
    assert response.output.strip() == expected_output


def test_api_error_response(mocked_responses):
    error_message = 'fake error'
    mocked_responses.add(
        responses.POST,
        GET_COMMITS_URL,
        json={'error': {'details': error_message}},
        status=400,
    )

    runner = CliRunner()
    response = runner.invoke(cli)
    assert response.exit_code != 0
    assert len(mocked_responses.calls) == 1
    assert response.output.strip() == f'Error: {error_message}'
