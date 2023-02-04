import json
import textwrap
import functools

import vh3
import pytest

from .. import project_operations as operations


@pytest.mark.usefixtures('multiprocessing_pool_mock', 'tracker_mock')
class TestCreateTrackerCommentWithExcelAttachments:
    @pytest.fixture(autouse=True)
    def setup(self, vh3_test_env):
        self.env = vh3_test_env

        self.issue_key = 'ISSUE-1'
        self.token = '-'

    @staticmethod
    def value_from_output(operation, key='text') -> str:
        return json.loads(operation.result.as_string())[key]

    @functools.wraps(operations.create_tracker_comment_with_excel_attachments)
    def run(self, **params):
        kwargs = dict(
            issue=self.issue_key,
            tracker_token=self.token,
        )
        kwargs.update(params)
        return operations.create_tracker_comment_with_excel_attachments(**kwargs)

    @pytest.mark.parametrize(
        'issue,comment,tracker_token',
        (('', 'text', 'token'), ('issue', '', 'token'), ('issue', 'text', '')),
        ids=('no-issue', 'no-text', 'no-token'),
    )
    def test_invalid_input(self, issue, comment, tracker_token):
        with self.env.build() as wi:
            self.run(issue=issue, comment=comment, tracker_token=tracker_token)
        with pytest.raises(RuntimeError):
            wi.run()

    def test_simple(self):
        with self.env.build() as wi:
            comment = 'Text message'
            op = self.run(comment=comment)
        wi.run()

        result = self.value_from_output(op)
        assert result == comment

    def test_with_context(self):
        with self.env.build() as wi:
            context = vh3.echo(json.dumps({'name': 'unknown'}), vh3.JSON)
            comment = 'Hello, {{ context.name }}!'
            op = self.run(comment=comment, context=context)
        wi.run()

        result = self.value_from_output(op)
        assert result == 'Hello, unknown!'

        result = self.value_from_output(op, key='createdBy')
        assert result == 'somebody'

    def test_with_attachments_and_summonees(self):
        with self.env.build() as wi:
            attachments = [
                vh3.echo(json.dumps([{'a': 1, 'b': 2}]), vh3.JSON),
                vh3.echo(json.dumps([{'a': 1, 'b': 2}, {'a': 3, 'b': 4}]), vh3.JSON),
            ]
            attachment_names = ('first.xlsx',)
            summonees = ('login1', 'login2')

            comment = textwrap.dedent(
                """
                    Hello! I've uploaded two files:
                    - "first.xlsx" with {{ counts[0] }} items;
                    - "attachment2.xlsx" with {{ counts["attachment2.xlsx"] }} items.
                """
            ).strip()
            op = self.run(
                comment=comment,
                attachments=attachments,
                attachment_names=attachment_names,
                summonees=summonees,
            )
        wi.run()

        result = self.value_from_output(op)
        expected = textwrap.dedent(
            """
                Hello! I've uploaded two files:
                - "first.xlsx" with 1 items;
                - "attachment2.xlsx" with 2 items.
            """
        ).strip()
        assert result == expected

        result = self.value_from_output(op, key='summonees')
        expected = ['login1', 'login2']
        assert result == expected
