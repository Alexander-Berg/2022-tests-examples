import json
import textwrap
import functools

import vh3
import pytest

from .. import project_operations as operations


def output2json(output) -> dict:
    return json.loads(output.result.as_string())


@pytest.mark.usefixtures('multiprocessing_pool_mock', 'tracker_mock')
class TestCreateTrackerCommentWithRelatedIssue:
    @pytest.fixture(autouse=True)
    def setup(self, vh3_test_env, default_id: int, default_uuid: str):
        self.env = vh3_test_env

        self.default_id = default_id
        self.default_uuid = default_uuid

        self.queue = 'ISSUE'
        self.issue_key = f'{self.queue}-1'
        self.related_issue_key = f'{self.queue}-7'

        self.token = '-'

    @functools.wraps(operations.create_tracker_comment_with_excel_attachments)
    def run(self, **params):
        kwargs = dict(
            issue=self.issue_key,
            tracker_token=self.token,
            related_issue_queue=self.queue,
        )
        kwargs.update(params)
        return operations.create_tracker_comment_with_related_issue_and_excel_attachments(**kwargs)

    @pytest.mark.parametrize(
        'issue,comment,tracker_token,related_issue_queue,related_issue_summary',
        (
            ('', 'text', 'token', 'queue', 'summary'),
            ('issue', '', 'token', 'queue', 'summary'),
            ('issue', 'text', '', 'queue', 'summary'),
            ('issue', 'text', 'token', '', 'summary'),
            ('issue', 'text', 'token', 'queue', ''),
        ),
        ids=('no-issue', 'no-text', 'no-token', 'no-queue', 'no-summary'),
    )
    def test_invalid_input(self, issue, comment, tracker_token, related_issue_queue, related_issue_summary):
        with self.env.build() as wi:
            self.run(
                issue=issue,
                comment=comment,
                tracker_token=tracker_token,
                related_issue_queue=related_issue_queue,
                related_issue_summary=related_issue_summary,
            )
        with pytest.raises(RuntimeError):
            wi.run()

    def test_no_related_comment(self):
        with self.env.build() as wi:
            out = self.run(
                comment='any',
                related_issue_summary='any',
            )
        wi.run()

        with pytest.raises(KeyError):
            output2json(out.related_comment)

    def test_with_context(self):
        with self.env.build() as wi:
            context = vh3.echo(json.dumps({'name': 'unknown'}), vh3.JSON)
            out = self.run(
                comment='Hello, {{ context.name }}! Working with issue: {{ issue.key }}',
                context=context,
                related_issue_summary='Related issue for {{ issue.key }}',
            )
        wi.run()

        result = output2json(out.comment)['text']
        expected = f'Hello, unknown! Working with issue: {self.related_issue_key}'
        assert result == expected

        result = output2json(out.related_issue)['summary']
        expected = f'Related issue for {self.issue_key}'
        assert result == expected

    def test_default_behaviour(self):
        with self.env.build() as wi:
            attachments = [
                vh3.echo(json.dumps([{'a': 1, 'b': 2}]), vh3.JSON),
                vh3.echo(json.dumps([{'a': 1, 'b': 2}, {'a': 3, 'b': 4}]), vh3.JSON),
            ]
            attachment_names = ("first.xlsx",)

            out = self.run(
                comment=textwrap.dedent(
                    """
                        Hello, I've uploaded two files:
                        - "first.xlsx" with {{ counts["first.xlsx"] }} items;
                        - "attachment2.xlsx" with {{ counts[1] }} items.

                        We will continue to investigate in issue {{ issue.key }}.
                    """
                ).strip(),
                attachments=attachments,
                attachment_names=attachment_names,
                summonees=['somebody-1'],
                related_issue_summary='Investigation results of the run in {{ issue.key }}',
                related_issue_description=textwrap.dedent(
                    """
                        Hello, I've uploaded two files:
                        - "first.xlsx" with {{ counts["first.xlsx"] }} items;
                        - "attachment2.xlsx" with {{ counts[1] }} items.

                        Original issue: {{ issue.key }}.
                    """
                ).strip(),
                related_issue_assignee='somebody-2',
                related_issue_comment="Please investigate what we found at {{ issue.key }}!",
                related_issue_summonees=['somebody-3'],
                related_issue_components=['component'],
                related_issue_tags=['tags'],
            )
        wi.run()

        result = output2json(out.comment)
        expected = {
            'self': '',
            'id': self.default_id,
            'longId': self.default_uuid,
            'createdBy': 'somebody',
            'summonees': ['somebody-1'],
            'text': textwrap.dedent(
                f"""
                    Hello, I've uploaded two files:
                    - "first.xlsx" with 1 items;
                    - "attachment2.xlsx" with 2 items.

                    We will continue to investigate in issue {self.related_issue_key}.
                """
            ).strip(),
        }
        assert result == expected

        result = output2json(out.related_issue)
        expected = {
            'self': '',
            'id': self.default_uuid,
            'key': self.related_issue_key,
            'queue': self.queue,
            'status': 'open',
            'priority': 'normal',
            'summary': f'Investigation results of the run in {self.issue_key}',
            'description': textwrap.dedent(
                f"""
                    Hello, I've uploaded two files:
                    - "first.xlsx" with 1 items;
                    - "attachment2.xlsx" with 2 items.

                    Original issue: {self.issue_key}.
                """
            ).strip(),
            'createdBy': 'somebody',
            'assignee': 'somebody-2',
            'followers': [],
            'components': ['component'],
            'tags': ['tags'],
        }
        assert result == expected

        result = output2json(out.related_comment)
        expected = {
            'self': '',
            'id': self.default_id,
            'longId': self.default_uuid,
            'createdBy': 'somebody',
            'text': f'Please investigate what we found at {self.issue_key}!',
            'summonees': ['somebody-3'],
        }
        assert result == expected
