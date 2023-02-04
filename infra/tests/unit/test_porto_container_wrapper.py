from __future__ import unicode_literals

from instancectl.lib.process import porto_container


def test_cast_command_to_str_porto():
    cases = [
        (["/bin/sleep", "1000"], "'/bin/sleep' '1000'"),
        (
            ["/bin/bash", "-c", "echo ${SOME_VAR} >> tmp.txt && D=$(date +%s)"],
            "'/bin/bash' '-c' 'echo ${SOME_VAR} >> tmp.txt && D=$(date +%s)'"
        ),
        (["/sbin/init"], "/sbin/init"),
        (["/bin/true"], "'/bin/true'"),
        (
            ["sky", "run", "grep '2019-01-01 10:05' /db/www/logs/log.log"],
            "'sky' 'run' 'grep '\\''2019-01-01 10:05'\\'' /db/www/logs/log.log'"
        ),
    ]
    for test, output in cases:
        assert porto_container.cast_command_to_str(test) == output





