#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More;
use Data::Dumper;

use FindBin;
use lib "$FindBin::Bin/../../../lib";
use Utils::Sys qw(get_tempfile);
use ObjLib::FileIter;

my $filename = get_tempfile("test_file_iter", UNLINK => 1);
my $key_field = 'url';

sub do_test {
    my $kv_arr = shift;
    my $check_keys_arr = shift;

    open my $fh, '>', $filename;
    for my $kv (@$kv_arr) {
        print $fh "$key_field=$kv->[0]\tcount=$kv->[1]\n";
    }
    close $fh;

    my @check_keys = sort @$check_keys_arr;

    my %expected_result;
    for my $k (@check_keys) {
        my @vs = map { $_->[1] } grep { $_->[0] eq $k } @$kv_arr;
        $expected_result{$k} = [ map { "$key_field=$k\tcount=$_" } @vs ];
    }

    my $ith = ObjLib::FileIter->new({filename => $filename, tskv_key_field => $key_field});

    print Dumper(\%expected_result);

    my %got_result = map { $_ => $ith->get_lines_by_key($_) // [] } @check_keys;
    is_deeply(\%got_result, \%expected_result, 'get_lines_by_key');

    my $last_key = 'яяяяяя';
    $ith->get_lines_by_key($last_key);
    my $very_last_key = "$last_key.";
    my $last_res = $ith->get_lines_by_key($very_last_key);  # должен быть undef!
    is_deeply($last_res, undef, 'get_lines_by_key_finish');
}

# стандартный тест
do_test([
        ['facebook.com' => 50],
        ['google.com' => 200],
        ['rbc.ru' => 25],
        ['rbc.ru' => 45],
        ['rbc.ru' => 35],
        ['yandex.ru' => 100],
        ['яндекс.рф' => 1000],
    ], [qw(avito.ru facebook.com google.com rbc.ru яндекс.рф)]
);

# ищем отсутствующий ключ
do_test([ ['a','zz'],['c','uu'] ], ['b']);

# ищем отсутствующий ключ, он больше остальных
do_test([ ['a','zz'],['b','uu'] ], ['c']);

# ищем отсутствующий ключ, он меньше остальных
do_test([ ['b','zz'],['c','uu'] ], ['a']);

# ключ в конце файла
do_test([ ['a','zz'],['b','uu'],['b','vv']], ['b']);

# повторы
do_test([ ['a','zz'],['a','zz'], ['b','uu'],['b','uu']], ['0','a','b','c']);

# совпадение из last_line + конец файла
do_test([ ['a','zz'],['c','uu'] ], ['b','c']);
do_test([ ['a','zz'],['c','uu'],['c','zz'] ], ['b','c']);

# duplicate key
do_test([ ['a','zz'],['b','uu'],['b','vv']], ['a','b','b']);

done_testing();
