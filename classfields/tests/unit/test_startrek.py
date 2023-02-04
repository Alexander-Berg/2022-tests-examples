#!/usr/bin/env python3
from api._startrek import StarTrek
import unittest

TEST_ISSUE_ID = "TEST_ID"


class TestStringMethods(unittest.TestCase):
    def test_should_transit_issue_into_testing_status(self):
        transition = StubTransition()
        client = StubStartrekClient(
            {TEST_ISSUE_ID: StubIssue({"readyForTest": transition}, status = StubStatus(""))}
        )
        st = StarTrek(client)
        st.send_ticket_to_testing(TEST_ISSUE_ID, "HELLO")
        self.assertTrue(transition.executed)

    def test_should_transit_issue_into_testing_status_if_resolved(self):
        transition = StubTransition()
        client = StubStartrekClient(
            {TEST_ISSUE_ID: StubIssue({"readyForTest": transition}, status = StubStatus("resolved"))}
        )
        st = StarTrek(client)
        st.send_ticket_to_testing(TEST_ISSUE_ID, "HELLO")
        self.assertTrue(transition.executed)

    def test_should_create_comment_with_text(self):
        issue = StubIssue({})
        client = StubStartrekClient(
            {TEST_ISSUE_ID: issue}
        )
        st = StarTrek(client)
        result = st.send_ticket_to_testing(TEST_ISSUE_ID, "HELLO")
        self.assertIs(issue.comments.msg, "HELLO")
        self.assertTrue(result)

    def test_should_not_create_comment_with_text(self):
        issue = StubIssue({}, comments = StubFailedComments())
        client = StubStartrekClient(
            {TEST_ISSUE_ID: issue}
        )
        st = StarTrek(client)
        result = st.send_ticket_to_testing(TEST_ISSUE_ID, "HELLO")
        self.assertFalse(result)

    def test_should_not_execute_transition_if_already_testing(self):
        transition = StubTransition()
        client = StubStartrekClient(
            {TEST_ISSUE_ID: StubIssue({"readyForTest": transition}, status=StubStatus("testing"))}
        )
        st = StarTrek(client)
        st.send_ticket_to_testing(TEST_ISSUE_ID, "HELLO")
        self.assertFalse(transition.executed)

    def test_should_not_execute_transition_if_already_ready_for_test(self):
        transition = StubTransition()
        client = StubStartrekClient(
            {TEST_ISSUE_ID: StubIssue({"readyForTest": transition}, status=StubStatus("readyForTest"))}
        )
        st = StarTrek(client)
        st.send_ticket_to_testing(TEST_ISSUE_ID, "HELLO")
        self.assertFalse(transition.executed)

    def test_should_not_execute_transition_if_already_ready_for_release(self):
        transition = StubTransition()
        client = StubStartrekClient(
            {TEST_ISSUE_ID: StubIssue({"readyForTest": transition}, status=StubStatus("readyForRelease"))}
        )
        st = StarTrek(client)
        st.send_ticket_to_testing(TEST_ISSUE_ID, "HELLO")
        self.assertFalse(transition.executed)

    def test_should_not_execute_transition_if_already_ready_for_merge(self):
        transition = StubTransition()
        client = StubStartrekClient(
            {TEST_ISSUE_ID: StubIssue({"readyForTest": transition}, status=StubStatus("readyForMerge"))}
        )
        st = StarTrek(client)
        st.send_ticket_to_testing(TEST_ISSUE_ID, "HELLO")
        self.assertFalse(transition.executed)

    def test_should_not_execute_transition_if_already_closed(self):
        transition = StubTransition()
        client = StubStartrekClient(
            {TEST_ISSUE_ID: StubIssue({"readyForTest": transition}, status=StubStatus("closed"))}
        )
        st = StarTrek(client)
        st.send_ticket_to_testing(TEST_ISSUE_ID, "HELLO")
        self.assertFalse(transition.executed)

class StubStatus:
    def __init__(self, key):
        self.key = key

class StubFailedComments:
    def create(self, text):
        raise Exception

class StubComments:
    def create(self, text):
        self.msg = text

class StubIssue:
    def __init__(self, transitions, comments = StubComments(), status = None):
        self.transitions = transitions
        self.comments = comments
        self.status = status


class StubTransition:
    executed = False

    def execute(self):
        self.executed = True


class StubStartrekClient:
    def __init__(self, issues):
        self.issues = issues


if __name__ == "__main__":
    unittest.main()
