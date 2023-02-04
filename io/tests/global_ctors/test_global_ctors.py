import json
import linecache
import logging
import re
from collections import defaultdict

import yatest.common as yc

logger = logging.getLogger('global_ctors')


def iter_global_ctors(lines):
    re_file = re.compile(r'File (.+):')
    re_source = re.compile(r'([0-9]+):(.*)')

    current_file = None
    for line in lines:
        file_match = re_file.match(line)
        if file_match:
            current_file = file_match.group(1)
            continue

        if current_file is None:
            continue

        source_match = re_source.match(line)
        if source_match:
            current_lineno = int(source_match.group(1))
            current_source = source_match.group(2)
            yield current_file, current_lineno, current_source


def get_global_ctors(program_path):
    proc = yc.execute(
        [
            yc.gdb_path(),
            '--batch',
            '--eval-command',
            'info functions __cxx_global_var_init',
            yc.build_path(program_path),
        ]
    )
    gdb_lines = proc.std_out.decode('utf-8').splitlines()

    file_ctors = defaultdict(set)
    build_root = yc.build_path() + '/'
    source_root = yc.source_path() + '/'
    for source_file, source_lineno, source_line in iter_global_ctors(gdb_lines):
        assert source_file.startswith('/-'), (
            "Global path found in debug info, " "rerun with `-D FORCE_CONSISTENT_DEBUG`: " + source_file
        )

        real_source_file = source_file.replace('/-S/', source_root).replace('/-B/', build_root)
        real_source_line = linecache.getline(real_source_file, source_lineno)
        logger.info(
            'path=%r, real_path=%r, line=%r, line=%r, real_line=%r',
            source_file,
            real_source_file,
            source_lineno,
            source_line,
            real_source_line,
        )

        if source_file.startswith('/-S/'):
            assert real_source_line, "Source path is not found, " "adjust DATA() prefixes in ya.make: " + source_file

        file_ctors[source_file].add((real_source_line or source_line).strip())

    return {k: sorted(v) for k, v in file_ctors.items()}


def test_sample_app_global_ctors():
    ctors = get_global_ctors('yandex_io/sample_app/linux/sample_app')

    result_path = yc.test_output_path('global_ctors.json')
    with open(result_path, 'w') as f:
        f.write(json.dumps(ctors, indent=2, sort_keys=True))

    return yc.canonical_file(result_path, local=True)
