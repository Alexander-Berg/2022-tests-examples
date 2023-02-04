#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More tests => 8;

use FindBin;
use lib "$FindBin::Bin/../../../lib";
use BaseProject;

my $proj = BaseProject->new({});

my $data_sample = [
    { name => 'aa1', url => 'http://example.ru/aa1.html' },
    { name => 'aA1', url => 'http://example.ru/aA1.html' },
    { name => 'AA1', url => 'http://example.ru/AA1.html' },
    { name => 'a2', url => 'http://example.ru/a2.html', new_field => 'zzzX' },
    { name => '0', url => 'http://example.ru/0.html' },
    { name => '', url => 'http://example.ru/empty.html' },
    { name => 'a5', url => 'http://example.ru/a5.html', new_field => 'zzz1' },
    { name => 'a6', url => 'http://example.ru/a6.html', new_field => '' },
    { name => 'a7', url => 'http://example.ru/a7.html', new_field => 0 },
];



sub test_equal_conditions_lc {
    my $filter_condition = {
        'name eq'       => 'aa1',
    };
    my $filtered = $proj->filter({ filter => $filter_condition } )->filter($data_sample);
    my $expected = [
        { name => 'aa1', url => 'http://example.ru/aa1.html' },
        { name => 'aA1', url => 'http://example.ru/aA1.html' },
        { name => 'AA1', url => 'http://example.ru/AA1.html' },
    ];
    is_deeply($filtered, $expected, 'test equal conditions in lc')
}

sub test_equal_conditions_uc {
    my $filter_condition = {
        'name eq'       => 'AA1',
    };
    my $filtered = $proj->filter({ filter => $filter_condition } )->filter($data_sample);
    my $expected = [
        { name => 'aa1', url => 'http://example.ru/aa1.html' },
        { name => 'aA1', url => 'http://example.ru/aA1.html' },
        { name => 'AA1', url => 'http://example.ru/AA1.html' },
    ];
    is_deeply($filtered, $expected, 'test equal conditions in uc')
}

sub test_not_equal_conditions_lc {
    my $filter_condition = {
        'name not eq'       => 'aa1',
    };
    my $filtered = $proj->filter({ filter => $filter_condition } )->filter($data_sample);
    my $expected = [
        { name => 'a2', url => 'http://example.ru/a2.html', new_field => 'zzzX' },
        { name => '0', url => 'http://example.ru/0.html' },
        { name => '', url => 'http://example.ru/empty.html' },
        { name => 'a5', url => 'http://example.ru/a5.html', new_field => 'zzz1' },
        { name => 'a6', url => 'http://example.ru/a6.html', new_field => '' },
        { name => 'a7', url => 'http://example.ru/a7.html', new_field => 0 },
    ];
    is_deeply($filtered, $expected, 'test not equal conditions in lc')
}

sub test_not_equal_conditions_uc {
    my $filter_condition = {
        'name not eq'       => 'Aa1',
    };
    my $filtered = $proj->filter({ filter => $filter_condition } )->filter($data_sample);
    my $expected = [
        { name => 'a2', url => 'http://example.ru/a2.html', new_field => 'zzzX' },
        { name => '0', url => 'http://example.ru/0.html' },
        { name => '', url => 'http://example.ru/empty.html' },
        { name => 'a5', url => 'http://example.ru/a5.html', new_field => 'zzz1' },
        { name => 'a6', url => 'http://example.ru/a6.html', new_field => '' },
        { name => 'a7', url => 'http://example.ru/a7.html', new_field => 0 },
    ];
    is_deeply($filtered, $expected, 'test not equal conditions in uc')
}


sub test_in_array_conditions_single {
    my $filter_condition = {
        'name' => [ '', ],
    };
    my $filtered = $proj->filter({ filter => $filter_condition } )->filter($data_sample);
    my $expected = [{ name => '', url => 'http://example.ru/empty.html' },];
    is_deeply($filtered, $expected, 'test in array conditions single')
}

sub test_in_array_conditions{
    my $filter_condition = {
        'name' => [ 'aa1', ''],
    };
    my $filtered = $proj->filter({ filter => $filter_condition } )->filter($data_sample);
    my $expected = [
        { name => 'aa1', url => 'http://example.ru/aa1.html' },
        { name => 'aA1', url => 'http://example.ru/aA1.html' },
        { name => 'AA1', url => 'http://example.ru/AA1.html' },
        { name => '', url => 'http://example.ru/empty.html' },
    ];
    is_deeply($filtered, $expected, 'test in array conditions')
}

sub test_in_array_conditions_different_registers{
    my $filter_condition = {
        'name' => [ 'aa1', '', 'A2', 0],
    };
    my $filtered = $proj->filter({ filter => $filter_condition } )->filter($data_sample);
    my $expected = [
        { name => 'aa1', url => 'http://example.ru/aa1.html' },
        { name => 'aA1', url => 'http://example.ru/aA1.html' },
        { name => 'AA1', url => 'http://example.ru/AA1.html' },
        { name => 'a2', url => 'http://example.ru/a2.html', new_field => 'zzzX' },
        { name => '0', url => 'http://example.ru/0.html' },
        { name => '', url => 'http://example.ru/empty.html' },
    ];
    is_deeply($filtered, $expected, 'test in array conditions with different registers')
}

sub test_not_in_array_conditions{
    my $filter_condition = {
        'name NOT' => [ 'aa1', '', 0, 'a2', 'a5', 'a6'],
    };
    my $filtered = $proj->filter({ filter => $filter_condition } )->filter($data_sample);
    my $expected = [ { name => 'a7', url => 'http://example.ru/a7.html', new_field => 0 }, ];
    is_deeply($filtered, $expected, 'test not in array conditions')
}

# tests
test_equal_conditions_lc;
test_equal_conditions_uc;
test_not_equal_conditions_lc;
test_not_equal_conditions_uc;
test_in_array_conditions_single;
test_in_array_conditions;
test_in_array_conditions_different_registers;
test_not_in_array_conditions;
1;

