from unittest import mock

from infra.reconf_juggler.tools import jdump


def _run_dumper(resolver, *args):
    try:
        with mock.patch('sys.argv', ['jdump', *args]):
            jdump.main(resolver=resolver)
        return 0

    except SystemExit as e:
        return e.code


def test_dump_recursively(capsys, canonized_resolver, diff_canonized):
    code = _run_dumper(
        canonized_resolver,
        '--object-name', 'rtc_msk',
        '--service-name', 'ssh',
        '--keys-only', 'children',
        '--recursive', '1',
    )

    assert 0 == code

    captured = capsys.readouterr()
    diff_canonized(captured.out)
    assert '' == captured.err


def test_dump_nonrecursively(capsys, canonized_resolver, diff_canonized):
    code = _run_dumper(
        canonized_resolver,
        '--object-name', 'rtc_msk',
        '--service-name', 'ssh',
        '--keys-exclude', 'check_id,creation_time,modification_time,meta',
    )

    assert 0 == code

    captured = capsys.readouterr()
    diff_canonized(captured.out)
    assert '' == captured.err


def test_dump_html(capsys, canonized_resolver):
    code = _run_dumper(
        canonized_resolver,
        '--object-name', 'rtc_msk',
        '--service-name', 'ssh',
        '--keys-only', 'children',
        '--ofmt', 'html',
        '--recursive', '1',
    )

    assert 0 == code

    captured = capsys.readouterr()
    assert '<!DOCTYPE html>' in captured.out
    assert '' == captured.err


def test_dump_jsdk(capsys, canonized_resolver, diff_canonized):
    code = _run_dumper(
        canonized_resolver,
        '--object-name', 'rtc_msk',
        '--service-name', 'ssh',
        '--keys-only', 'children',
        '--ofmt', 'jsdk',
        '--recursive', '1',
    )

    assert 0 == code

    captured = capsys.readouterr()
    diff_canonized(captured.out)
    assert '' == captured.err
