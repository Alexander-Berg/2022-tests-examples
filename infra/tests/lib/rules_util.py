class MockCheckLimitResult:
    def __init__(self, result, message):
        self._result = result
        self.info = message

    def __bool__(self):
        return self._result

    def __nonzero__(self):
        return self.__bool__()


limit_breached = MockCheckLimitResult(False, "limits msg mock")
limit_not_breached = MockCheckLimitResult(True, None)
