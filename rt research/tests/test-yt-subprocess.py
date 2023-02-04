#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import os
import subprocess

import yt.wrapper as yt

bindir = os.path.abspath(os.path.dirname(sys.argv[0]))
sys.path.append(bindir + '/../pylib')

from yt_mappers.yt_subprocess_mapper import SubprocessMapper
from yt_mappers.yt_perl_mapper import PerlMapper

def test_subprocess():
    script = 'yt-subprocess-mapper-test.pl'
    with open(script, 'w') as fh:
        fh.write(r"""
            while (<>) {
                chomp;
                my @f = split /\t/, $_;
                my %h = map { split /=/, $_, 2 } @f;
                my %hh = map { $_ => uc($h{$_}) } keys %h;
                print join("\t", map { "$_=$hh{$_}" } keys %hh), "\n";
            }
        """)

    rows = [{"id": x, "name": "my_name_is_" + str(x)} for x in range(10000)]
    with yt.TempTable() as tmp_in, yt.TempTable() as tmp_out:
        yt.write_table(tmp_in, rows)
        my_mapper = SubprocessMapper(['perl', script],
            dst_fields=[{"id": int, "name": str}],
            process_count=4,
        )
        yt.run_map(my_mapper, tmp_in, tmp_out, local_files=[script])
        for row in yt.read_table(tmp_out):
            print('sub_out', row)

def test_perl():
    mapper_code = r"""
        $r->{name} .= $self->{magic_constant};
        my %lc = map { $_ => lc($r->{$_}) } keys %$r;
        my %uc = map { $_ => uc($r->{$_}) } keys %$r;
        yield(\%lc => LTABLE);
        yield(\%uc => UTABLE);
    """
    os.environ["BACKUP_GENERATED_PL"] = './generated-from-PerlMapper.pl'
    my_mapper = PerlMapper(
        mapper_code,
        begin="$self->{magic_constant} = '.MAGIC';""",
        dst_fields=[
            {"id": int, "name": str},
            {"id": int, "name": str},
        ],
        dst_names=['LTABLE','UTABLE'],
        process_count=4,
    )
    print(my_mapper.local_files)

    rows = [{"id": x, "name": "My_Name_Is_" + str(x)} for x in range(10000)]
    with yt.TempTable() as tmp_in, yt.TempTable() as tmp_out1, yt.TempTable() as tmp_out2:
        yt.write_table(tmp_in, rows)
        yt.run_map(
            my_mapper,
            tmp_in,
            [tmp_out1, tmp_out2],
            local_files=my_mapper.local_files,
        )
        for row in yt.read_table(tmp_out1):
            print('perl_out1', row)
        for row in yt.read_table(tmp_out2):
            print('perl_out2', row)


if __name__ == "__main__":
    test_subprocess()
    test_perl()
