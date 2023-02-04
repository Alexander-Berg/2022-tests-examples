#!/usr/bin/env python3
"""
All this things exist just for tests speedup, see https://clubs.at.yandex-team.ru/arcadia/24883
Example:
this
> run_test.py -tt ./infra/walle/server/tests/ -F test_fsm.py::test_handling
will be converted to
> ya make -tt ./infra/walle/server/tests/ -F test_fsm.py::test_handling --test-filename test_fsm.py -D NO_FORK_TESTS=yes
"""

import os
import sys


TARGET_TEST_OPTIONS = {
    "-F",
    "--test-filter",
}


def add_more_test_options(args):
    test_filter_option_gen = ((i, a) for i, a in enumerate(args) if any(a.startswith(t) for t in TARGET_TEST_OPTIONS))
    option_name_token_pair = next(test_filter_option_gen, None)
    if option_name_token_pair:
        index, option = option_name_token_pair
        if option in TARGET_TEST_OPTIONS:
            test_name = args[index + 1]
        else:
            test_name = option[option.index("=") + 1 :]
        first_part = test_name[: test_name.index(":")]
        # NOTE(rocco66): rename modules to filename path 'dmc.test_event_handling.py' -> ' dmc/test_event_handling.py'
        filename_tokens = first_part.rsplit(".", 1)
        filename_tokens[0] = filename_tokens[0].replace(".", "/")
        args += ["--test-filename", ".".join(filename_tokens)]


def main():
    new_args = sys.argv[1:][:]
    add_more_test_options(new_args)
    new_args += ["-D", "NO_FORK_TESTS=yes"]
    os.system(" ".join(["ya", "make"] + new_args))


if __name__ == "__main__":
    main()
