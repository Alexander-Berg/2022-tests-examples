from awtest.api import comment_id, user


SECTASK_ID = 'SECTASK-XXX'


class MockComment(object):
    @staticmethod
    def create(*_, **__):
        return comment_id(id=1)

    @staticmethod
    def get_all(*_, **__):
        return []


class MockIssue(object):
    comments = MockComment()
    id = SECTASK_ID

    def __init__(self):
        self.followers = [user(login='robot')]  # not using class variable to protect from append()s in different tests
        self.access = [user(login='robot')]  # not using class variable to protect from append()s in different tests

    @staticmethod
    def update(*_, **__):
        return MockIssue()


class MockStartrekClient(object):
    issues = {SECTASK_ID: MockIssue()}
    users = {'not-a-robot': user(login='not-a-robot')}
