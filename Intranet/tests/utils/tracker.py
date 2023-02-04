from collections import namedtuple


FakeIssueCommentAuthor = namedtuple('FakeIssueCommentAuthor', ['id'])
FakeIssueCommentBase = namedtuple('FakeIssueCommentBase', ['author', 'text', 'createdAt'])


class FakeIssueComment(FakeIssueCommentBase):

    @property
    def id(self):
        return 1

    @property
    def createdBy(self):
        return FakeIssueCommentAuthor(self.author)
