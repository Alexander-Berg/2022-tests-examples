# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from infra.swatlib.util.fs import makedirs_ignore, remove_ignore, atomic_write, read_file_last_lines


def test_makedirs_ignore(tmpdir):
    target = tmpdir.join('a', 'b', 'c')
    makedirs_ignore(target.strpath)
    assert target.exists()
    makedirs_ignore(target.strpath)


def test_remove_ignore(tmpdir):
    target = tmpdir.join('target_file').ensure()
    remove_ignore(target.strpath)
    assert not target.exists()
    remove_ignore(target.strpath)


def test_atomic_write(tmpdir):
    contents = 'hello'
    target = tmpdir.join('target_file')
    atomic_write(target.strpath, contents)
    assert target.read() == contents


def test_read_file_last_lines(tmpdir):
    line = 'hello, my length is 23\n'
    temp_file = tmpdir.join('some_file')
    temp_file.write(1000 * line)
    result = read_file_last_lines(temp_file.strpath, lines_to_read=500)
    assert result.content == line * 500
    assert result.start_offset == len(line) * 500


def test_read_file_last_lines_short_file(tmpdir):
    line = 'hello, my length is 23\n'
    temp_file = tmpdir.join('some_file')
    temp_file.write(line * 10)
    result = read_file_last_lines(temp_file.strpath, lines_to_read=20)
    assert result.content == line * 10
    assert result.start_offset == 0


def test_read_file_last_lines_non_ascii(tmpdir):
    line = 'некоторая тестовая строка\n'
    temp_file = tmpdir.join('some_file')
    temp_file.write_text(line * 1000, encoding='utf8')
    result = read_file_last_lines(temp_file.strpath, lines_to_read=500)
    assert result.content == line * 500
    assert result.start_offset == len(line.encode('utf8')) * 500


def test_read_file_last_lines_no_last_line_end(tmpdir):
    line = 'hello, my length is 23\n'
    temp_file = tmpdir.join('some_file')
    temp_file.write((1000 * line).rstrip())
    result = read_file_last_lines(temp_file.strpath, lines_to_read=500)
    assert result.content == (line * 500).rstrip()
    assert result.start_offset == len(line) * 500


def test_read_file_las_lines_offset(tmpdir):
    line = 'hello, my length is 23\n'
    temp_file = tmpdir.join('some_file')
    temp_file.write(10 * line)
    result = read_file_last_lines(temp_file.strpath, lines_to_read=500, offset=len(line) * 5)
    assert result.content == line * 5
    assert result.start_offset == 0


def test_read_file_las_lines_byte_limit(tmpdir):
    line = 'hello, my length is 23\n'
    temp_file = tmpdir.join('some_file')
    temp_file.write(1000 * line)
    result = read_file_last_lines(temp_file.strpath, lines_to_read=500, byte_limit=230)
    assert result.content == line * 10
