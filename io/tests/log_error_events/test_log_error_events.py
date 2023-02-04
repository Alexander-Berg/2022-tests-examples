"""
This test finds and canonizes all used log event names in source code.
Event names are concisely sorted into a single list to facilitate reviews for naming consistency.
"""

import glob
import json
import logging
import re
from collections import defaultdict

import yatest.common as yc

logger = logging.getLogger('global_ctors')

RE_LOG_MODULE = re.compile(r'YIO_DEFINE_LOG_MODULE(?:_IN_SCOPE)?\("([^"]*)"\)')
RE_LOG_ERROR_EVENT = re.compile(r'YIO_LOG_ERROR_EVENT\("([^"]*)"')
RE_MONITOR_THROTTLED_LOG_ERROR_EVENT = re.compile(r'THROTTLED_LOG_ERROR\(.*, "(.*)".*,')
RE_BUILD_LOG_ERROR_EVENT = re.compile(r'YIO_BUILD_LOG_EVENT_HEADER(?:_FMT)?_IMPL\([^,]*, *"([^"]*)", *"([^"]*)"\)')
RE_SK_LOG_EVENT = re.compile(r'SK_LOG_EVENT\("([^"]*)"')


def iter_error_events(lines):
    log_module = None
    for line in lines:
        m = RE_LOG_MODULE.search(line)
        if m:
            log_module = m.group(1)
            continue

        m = RE_LOG_ERROR_EVENT.search(line)
        if m:
            tmp_log_error_event = m.group(1)
            yield log_module, tmp_log_error_event
            continue

        m = RE_MONITOR_THROTTLED_LOG_ERROR_EVENT.search(line)
        if m:
            tmp_log_error_event = m.group(1)
            yield log_module, tmp_log_error_event
            continue

        m = RE_BUILD_LOG_ERROR_EVENT.search(line)
        if m:
            tmp_log_module = m.group(1)
            tmp_log_error_event = m.group(2)
            yield tmp_log_module, tmp_log_error_event
            continue

        m = RE_SK_LOG_EVENT.search(line)
        if m:
            tmp_log_error_event = m.group(1)
            yield 'speechkit', tmp_log_error_event
            continue


def test_event_names():
    yandexio_root = yc.source_path("yandex_io")
    speechkit_root = yc.source_path("speechkit")
    smart_devices_root = yc.source_path("smart_devices")

    paths = glob.glob(yandexio_root + '/**/*', recursive=True)
    paths += glob.glob(speechkit_root + '/**/*', recursive=True)
    paths += glob.glob(smart_devices_root + '/**/*', recursive=True)
    paths = [path for path in paths if path.endswith('.cpp') or path.endswith('.cc') or path.endswith('.h')]

    events = defaultdict(set)
    for path in paths:
        with open(path) as f:
            for log_module, log_error_event in iter_error_events(f):
                assert log_module is not None, f'in {path}'  # log module must be defined in source code
                events[log_module].add(log_error_event)

    events = {k: sorted(v) for k, v in events.items()}

    result_path = yc.test_output_path('log_error_events.json')
    with open(result_path, 'w') as f:
        f.write(json.dumps(events, indent=2, sort_keys=True))

    return yc.canonical_file(result_path, local=True)
