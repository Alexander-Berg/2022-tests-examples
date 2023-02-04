from multiprocessing.managers import BaseManager, BaseProxy


class SharedCallableMock:
    MISSING = object()

    def __init__(self, **kwargs):
        self._call_args = []
        self._enter_called = False
        self._exit_call_args = None
        self.return_value = kwargs.get("return_value", self.MISSING)
        self.side_effect = kwargs.get("side_effect", self.MISSING)
        self._cur_seq_pos = 0

    def __call__(self, *args, **kwargs):
        self._call_args.append((args, kwargs))

        if self.side_effect is not self.MISSING:
            return self._exec_side_effect()
        elif self.return_value is not self.MISSING:
            return self.return_value
        else:
            return self

    def __enter__(self):
        self._enter_called = True
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self._exit_call_args = (exc_type, exc_val, exc_tb)

    @property
    def mock_calls(self):
        return self._call_args.copy()

    @property
    def call_args(self):
        try:
            return self._call_args[-1]
        except IndexError:
            return None

    @property
    def call_count(self):
        return len(self._call_args)

    @property
    def called(self):
        return self.call_count > 0

    def assert_called(self):
        assert self.called

    def assert_not_called(self):
        assert not self.called

    def assert_called_with(self, *args, **kwargs):
        if not self.called:
            raise AssertionError("Not called")

        if (args, kwargs) != self._call_args[-1]:
            raise AssertionError("No call with these args")

    def assert_any_call(self, *args, **kwargs):
        if not self.called:
            raise AssertionError("Not called")

        if not (args, kwargs) in self._call_args:
            raise AssertionError("No call with these args")

    def assert_enter_called(self):
        assert self._enter_called, "Not called"

    def assert_exit_called(self):
        assert self._exit_call_args is not None, "Not called"

    def assert_exit_called_without_exception(self):
        if self._exit_call_args is None:
            raise AssertionError("Not called")

        assert self._exit_call_args[0] is None, "Called with exception"

    def assert_exit_called_with_exception(self):
        if self._exit_call_args is None:
            raise AssertionError("Not called")

        assert self._exit_call_args[0] is not None, "Called without exception"

    def _exec_side_effect(self):
        if isinstance(self.side_effect, (list, tuple)):
            cur_pos = self._cur_seq_pos
            self._cur_seq_pos += 1
            side_effect = self.side_effect[cur_pos]
        else:
            side_effect = self.side_effect

        return self.__interpret_side_effect(side_effect)

    @staticmethod
    def __interpret_side_effect(side_effect):
        if (
            isinstance(side_effect, Exception)
            or isinstance(side_effect, type)
            and issubclass(side_effect, BaseException)
        ):
            raise side_effect
        elif callable(side_effect):
            return side_effect()
        else:
            return side_effect

    def _get_prop(self, name):
        return getattr(self, name)

    def _set_prop(self, name, value):
        setattr(self, name, value)


class SharedCallableMockProxy(BaseProxy):
    _exposed_ = (
        "__call__",
        "__enter__",
        "__exit__",
        "assert_called",
        "assert_not_called",
        "assert_called_with",
        "assert_any_call",
        "assert_enter_called",
        "assert_exit_called",
        "assert_exit_called_without_exception",
        "assert_exit_called_with_exception",
        "_set_return_value",
        "_set_side_effect",
        "_get_prop",
        "_set_prop",
    )

    _exposed_props_get = ("called", "call_count", "call_args", "mock_calls")
    _exposed_props_set = ("return_value", "side_effect")

    def __call__(self, *args, **kwargs):
        return self._callmethod("__call__", args, kwargs)

    def __enter__(self):
        return self._callmethod("__enter__")

    def __exit__(self, exc_type, exc_val, exc_tb):
        # NOTE: exc_tb is always empty because it can't be pickled
        return self._callmethod("__exit__", (exc_type, exc_val, ""))

    def assert_called(self):
        return self._callmethod("assert_called")

    def assert_not_called(self):
        return self._callmethod("assert_not_called")

    def assert_called_with(self, *args, **kwargs):
        return self._callmethod("assert_called_with", args, kwargs)

    def assert_any_call(self, *args, **kwargs):
        return self._callmethod("assert_any_call", args, kwargs)

    def assert_enter_called(self):
        return self._callmethod("assert_enter_called")

    def assert_exit_called(self):
        return self._callmethod("assert_exit_called")

    def assert_exit_called_without_exception(self):
        return self._callmethod("assert_exit_called_without_exception")

    def assert_exit_called_with_exception(self):
        return self._callmethod("assert_exit_called_with_exception")

    def __getattr__(self, name):
        if name in self._exposed_props_get:
            return self._callmethod("_get_prop", (name,))
        else:
            raise AttributeError(name)

    def __setattr__(self, name, value):
        if name in self._exposed_props_set:
            self._callmethod("_set_prop", (name, value))
        else:
            super().__setattr__(name, value)


class SharedCallableMockManager(BaseManager):
    pass


SharedCallableMockManager.register(
    "SharedCallableMock", SharedCallableMock, SharedCallableMockProxy
)


class YqlRequestMockProxy(SharedCallableMockProxy):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self._exposed_props_get = self._exposed_props_get + ("run", "get_results")
        self._exposed_props_set = self._exposed_props_set + ("run", "get_results")


class YqlRequestResultsMockProxy(SharedCallableMockProxy):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self._exposed_props_get = self._exposed_props_get + ("status", "text", "table")
        self._exposed_props_set = self._exposed_props_set + ("status", "text", "table")


class YqlResultTableMockProxy(SharedCallableMockProxy):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self._exposed_props_get = self._exposed_props_get + ("get_iterator",)
        self._exposed_props_set = self._exposed_props_set + ("get_iterator",)


SharedCallableMockManager.register(
    "YqlRequestMock", SharedCallableMock, YqlRequestMockProxy
)
SharedCallableMockManager.register(
    "YqlRequestResultsMock", SharedCallableMock, YqlRequestResultsMockProxy
)
SharedCallableMockManager.register(
    "YqlResultTableMock", SharedCallableMock, YqlResultTableMockProxy
)
