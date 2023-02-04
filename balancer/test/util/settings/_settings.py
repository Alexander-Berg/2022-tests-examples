# -*- coding: utf-8 -*-
import os


def get_resource(py_path, ya_path):
    # TODO: replace with try-except ImportError after full ya make support
    if os.path.exists(py_path):
        with open(py_path, 'r') as f:
            return f.read()
    else:
        import run_import_hook
        return run_import_hook.importer.get_data(ya_path)


def get_data(py_path, ya_path):
    # TODO: replace with try-except ImportError after full ya make support
    if os.path.exists(py_path):
        return py_path
    else:
        import yatest.common
        return yatest.common.source_path(ya_path)


class Tool(object):
    def __init__(self,
                 pytest_option_name=None,
                 tool_file_name=None,
                 yatest_option_name=None,
                 yatest_path=None,
                 system_file_name=None,
                 skip_undefined=False,
                 ):
        super(Tool, self).__init__()
        self.__pytest_option_name = pytest_option_name
        self.__tool_file_name = tool_file_name
        self.__yatest_option_name = yatest_option_name
        self.__yatest_path = yatest_path
        self.__system_file_name = system_file_name
        self.__skip_undefined = skip_undefined

    @property
    def pytest_option_name(self):
        return self.__pytest_option_name

    @property
    def tool_file_name(self):
        return self.__tool_file_name

    @property
    def yatest_option_name(self):
        return self.__yatest_option_name

    @property
    def yatest_path(self):
        return self.__yatest_path

    @property
    def system_file_name(self):
        return self.__system_file_name


class TestTools(object):
    def __init__(self, tools_fs, request_config, settings):
        super(TestTools, self).__init__()
        self.__tools_fs = tools_fs
        self.__request_config = request_config
        self.__root_fs = settings.root_fs
        if settings.yatest:
            import yatest.common
            self.__yatest_common = yatest.common
        else:
            self.__yatest_common = None

    def get_tool(self, tool):
        return self.__choose(
            tool,
            [
                self.__pytest_option,
                self.__tools_dir,
                self.__yatest_option,
                self.__yatest_binary,
                self.__system_file,
            ]
        )

    def __pytest_option(self, tool):
        if tool.pytest_option_name is None:
            return None
        option_value = self.__request_config.getoption(tool.pytest_option_name)
        if option_value is None:
            return None
        return self.__root_fs.abs_path(option_value)

    def __tools_dir(self, tool):
        if tool.tool_file_name is None:
            return None
        if self.__tools_fs is None:
            return None
        path = self.__tools_fs.abs_path(tool.tool_file_name)
        if not os.path.exists(path):
            return None
        return path

    def __yatest_option(self, tool):
        if tool.yatest_option_name is None:
            return None
        if self.__yatest_common is None:
            return None
        option_value = self.__yatest_common.get_param(tool.yatest_option_name)
        if option_value is None:
            return None
        return self.__root_fs.abs_path(option_value)

    def __yatest_binary(self, tool):
        if tool.yatest_path is None:
            return None
        if self.__yatest_common is None:
            return None
        return self.__yatest_common.binary_path(tool.yatest_path)

    def __system_file(self, tool):
        if tool.system_file_name is None:
            return None
        return tool.system_file_name

    def __choose(self, tool, func_list):
        for func in func_list:
            result = func(tool)
            if result is not None:
                return result


class Settings(object):
    def __init__(self, request, sandbox, yatest, root_fs, build_vars):
        super(Settings, self).__init__()
        self.__request = request
        self.__sandbox = sandbox
        self.__yatest = yatest
        self.__root_fs = root_fs
        self.__build_vars = build_vars

    @property
    def sandbox(self):
        return self.__sandbox

    @property
    def yatest(self):
        return self.__yatest

    @property
    def root_fs(self):
        return self.__root_fs

    def get_build_var(self, name):
        return self.__build_vars[name]

    def get_param(self, name):
        if self.yatest:
            import yatest.common
            return yatest.common.get_param(name)
        else:
            return self.__request.config.getoption(name)

try:
    import yatest  # noqa
    YATEST = True
except ImportError:
    YATEST = False
