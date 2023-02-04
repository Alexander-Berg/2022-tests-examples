import logging
import shutil
import sys
import tempfile
import threading

import yatest.common as yc
from library.python import cores

logger = logging.getLogger(__name__)


class BoostTestFailure(Exception):
    def __init__(self, stdout_path, stderr_path, execution_result):
        self.stdout_path = stdout_path
        self.stderr_path = stderr_path
        self.execution_result = execution_result
        try:
            self.extracted_errors = self._extract_errors()
        except Exception as e:
            self.extracted_errors = f'<Failed to extract errors: {e}>'

    def __str__(self):
        message_lines = [
            f'Execution has failed with code {self.execution_result.exit_code}:',
            '',
            f'[[rst]]{self.extracted_errors}[[bad]]',
            '',
            f'STDOUT: {self.stdout_path}',
            f'STDERR: {self.stderr_path}',
        ]

        if self.execution_result.backtrace:
            colorized_backtrace = cores.colorize_backtrace(self.execution_result.backtrace)
            message_lines += [
                '',
                'Backtrace:',
                f'[[rst]]{colorized_backtrace}[[bad]]',
            ]

        return '\n'.join(message_lines)

    def _extract_errors(self):
        std_out = self.execution_result.std_out.decode('utf-8', errors='surrogateescape')
        lines = [line for line in std_out.splitlines() if 'error:' in line]

        std_err = self.execution_result.std_err.decode('utf-8', errors='surrogateescape')
        if '==ERROR:' in std_err or '==WARNING:' in std_err:
            # Sanitizer errors detected
            lines += std_err.splitlines()
        else:
            lines += [line for line in std_err.splitlines() if ' Assertion ' in line]

        return '\n'.join(lines)


def run_test_binary(program, *args, tag, tests_data, intercept_failure=False):
    tag = tag.replace('/', '__')  # Make hierarchical tags path-safe

    data_path = yc.build_path(tests_data)

    with tempfile.TemporaryDirectory(prefix=tag) as temp_dir:
        # Copy test data to a mutable directory.
        # Isolate tests from each other's mutations
        cwd = temp_dir + '/wd'
        shutil.copytree(data_path, cwd)
        stdout_path = yc.output_path('tests.' + tag + '.out')
        stderr_path = yc.output_path('tests.' + tag + '.err')
        try:
            return yc.execute(
                [program, '--color_output=no', '--catch_system_errors=no'] + list(args),
                stdout=stdout_path,
                stderr=stderr_path,
                cwd=cwd,
            )
        except yc.ExecutionError as err:
            if intercept_failure:
                # drop original exception: it is too verbose and clutters test report
                raise BoostTestFailure(stdout_path, stderr_path, err.execution_result) from None
            raise


def _get_indent(line):
    count = 0
    for c in line:
        if c == ' ':
            count += 1
        else:
            break
    return count


def _iter_hrf_test_list(text):
    parents = []
    for line in text.strip().splitlines():
        line_level = _get_indent(line) // 4
        name = line.lstrip(' ').rstrip('* ')
        yield parents[:line_level] + [name]
        parents[line_level:] = [name]


def _build_test_tree(tests):
    root = {}
    for test_path in tests:
        node = root
        for path_part in test_path:
            node = node.setdefault(path_part, {})
    return root


def _iter_test_tree_leafs(test_tree):
    for path_part, subtree in test_tree.items():
        if not subtree:
            yield [path_part]
        else:
            for subpath in _iter_test_tree_leafs(subtree):
                yield [path_part] + subpath


def list_tests(program, tests_data, suites=None):
    process = run_test_binary(program, '--list_content', tag='list_tests', tests_data=tests_data)
    hrf_test_list = process.std_err.decode('utf-8')
    test_tree = _build_test_tree(_iter_hrf_test_list(hrf_test_list))

    if suites is not None:
        test_tree = {suite: subtree for suite, subtree in test_tree.items() if suite in suites}

    return ['/'.join(path) for path in _iter_test_tree_leafs(test_tree)]


def run_test(program, test_name, tests_data):
    iterations = int(yc.get_param('rerun-count', '1'))
    for i in range(iterations):
        logger.info("Running test %s: rerun %s/%s", test_name, i, iterations)
        process = run_test_binary(
            program,
            f'--run_test={test_name}',
            tag=f'{test_name}_{i}',
            tests_data=tests_data,
            intercept_failure=True,
        )

        logger.info('STDOUT:\n%s', process.std_out)
        logger.info('STDERR:\n%s', process.std_err)


class DiscoverTests:
    def __init__(self, program, suites=None, tests_data='yandex_io/tests/data'):
        self.program = program
        self.suites = suites
        self.lock = threading.Lock()

        self.test_list = None
        self.test_discovery_exception = None

        self.tests_data = tests_data

        self._discover_tests()

    def check_success(self):
        if self.test_discovery_exception is None:
            return

        # Re-raise exception recorded at discovery
        exc_type, exc_value, exc_traceback = self.test_discovery_exception
        raise exc_type.with_traceback(exc_value, exc_traceback)

    def _discover_tests(self):
        if self.test_list is not None:
            return

        try:
            self.test_list = list_tests(self.program, suites=self.suites, tests_data=self.tests_data)
        except Exception:
            self.test_list = []
            self.test_discovery_exception = sys.exc_info()

        self.test_list.sort()

    def run_test(self, test_name):
        run_test(self.program, test_name, self.tests_data)
