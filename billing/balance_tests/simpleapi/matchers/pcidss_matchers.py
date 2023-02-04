from hamcrest.core.base_matcher import BaseMatcher


def keyapi_status_is(expected):
    return _IsKeyApiSuccess(expected)


def keykeeper_status_is(expected):
    return _IsKeyKeeperSuccess(expected)


def confpatch_status_is(expected):
    return _IsConfPatchSuccess(expected)


class _IsKeyApiSuccess(BaseMatcher):
    def __init__(self, expected):
        self.expected = expected
        self.actual = None

    def _matches(self, item):
        from simpleapi.steps.pcidss_steps import KeyApi
        return KeyApi.get_status(item) == self.expected

    def describe_to(self, description):
        description.append_text("Response from KeyApi status check")


class _IsKeyKeeperSuccess(BaseMatcher):
    def __init__(self, expected):
        self.expected = expected
        self.actual = None

    def _matches(self, item):
        from simpleapi.steps.pcidss_steps import KeyKeeper
        return KeyKeeper.get_status(item) == self.expected

    def describe_to(self, description):
        description.append_text("Response from KeyKeeper status check")


class _IsConfPatchSuccess(BaseMatcher):
    def __init__(self, expected):
        self.expected = expected
        self.actual = None

    def _matches(self, item):
        from simpleapi.steps.pcidss_steps import ConfPatch
        return ConfPatch.get_status(item) == self.expected

    def describe_to(self, description):
        description.append_text("Response from KeyKeeper status check")
