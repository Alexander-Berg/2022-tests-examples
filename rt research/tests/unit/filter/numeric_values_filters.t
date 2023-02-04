#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More tests => 12;

use FindBin;
use lib "$FindBin::Bin/../../../lib";
use BaseProject;

my $proj = BaseProject->new({});

my $data_sample = [
    { name => '',     price => '' },
    { name => '0',    price => 0 },
    { name => 'a1',   price => 2 },
    { name => 'a1.0', price => 2.00 },
    { name => 'a2',   price => 5, },
    { name => 'a3',   price => 14.50, },
    { name => 'a4',   price => 300.00, },
    { name => 'a5',   price => 500.30, },
];


sub test_compare_condition_greater_than {
    my $filter_condition = { 'price >' => '300', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a5', price => 500.30, },
    ];
    is_deeply($filtered, $expected, 'test compare condition: greater than condition')
}

sub test_compare_condition_not_greater_than {
    my $filter_condition = { 'price NOT >' => '2', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => '',     price => '' },
        { name => '0',    price => 0 },
        { name => 'a1',   price => 2 },
        { name => 'a1.0', price => 2.00 },
    ];
    is_deeply($filtered, $expected, 'test compare condition: not greater than condition')
}

sub test_compare_condition_greater_than_or_equal {
    my $filter_condition = { 'price >=' => '300', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a4', price => 300.00, },
        { name => 'a5', price => 500.30, },
    ];
    is_deeply($filtered, $expected, 'test compare condition: greater than or equal condition')
}

sub test_compare_condition_not_greater_than_or_equal {
    my $filter_condition = { 'price NOT >=' => '2', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => '',  price => '' },
        { name => '0', price => 0 },
    ];
    is_deeply($filtered, $expected, 'test compare condition: not greater than or equal condition')
}

sub test_compare_condition_less_than {
    my $filter_condition = { 'price <' => 2, };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => '',  price => '' },
        { name => '0', price => 0 },
    ];
    is_deeply($filtered, $expected, 'test compare condition: less than condition')
}

sub test_compare_condition_not_less_than {
    my $filter_condition = { 'price NOT <' => 300, };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a4', price => 300.00, },
        { name => 'a5', price => 500.30, },
    ];
    is_deeply($filtered, $expected, 'test compare condition: less than condition')
}

sub test_compare_condition_less_than_or_equal {
    my $filter_condition = { 'price <=' => 2, };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => '',     price => '' },
        { name => '0',    price => 0 },
        { name => 'a1',   price => 2 },
        { name => 'a1.0', price => 2.00 },
    ];
    is_deeply($filtered, $expected, 'test compare condition: less than or equal condition')
}

sub test_compare_condition_not_less_than_or_equal {
    my $filter_condition = { 'price NOT <=' => 300, };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a5', price => 500.30, },
    ];
    is_deeply($filtered, $expected, 'test compare condition: not less than or equal condition')
}

sub test_compare_condition_equal {
    my $filter_condition = { 'price ==' => 2, };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a1',   price => 2 },
        { name => 'a1.0', price => 2.00 },
    ];
    is_deeply($filtered, $expected, 'test compare condition: equal')
}

sub test_compare_condition_not_equal {
    my $filter_condition = { 'price NOT ==' => 2, };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => '',   price => '' },
        { name => '0',  price => 0 },
        { name => 'a2', price => 5, },
        { name => 'a3', price => 14.50, },
        { name => 'a4', price => 300.00, },
        { name => 'a5', price => 500.30, },
    ];
    is_deeply($filtered, $expected, 'test compare condition: not equal')
}

sub test_compare_condition_equal_array {
    my $filter_condition = { 'price ==' => [ 2, 5.00 ], };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a1', price => 2 },
        { name => 'a1.0', price => 2.00 },
        { name => 'a2', price => 5, },
    ];
    is_deeply($filtered, $expected, 'test compare condition: equal array')
}

sub test_compare_condition_not_equal_array {
    my $filter_condition = {
        'price NOT ==' => [ 2, 5.00, 14.5, 300, 500.3, 0 ],
    };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [];
    is_deeply($filtered, $expected, 'test compare condition: not equal array')
}


# run tests
test_compare_condition_greater_than;
test_compare_condition_not_greater_than;
test_compare_condition_greater_than_or_equal;
test_compare_condition_not_greater_than_or_equal;

test_compare_condition_less_than;
test_compare_condition_not_less_than;
test_compare_condition_less_than_or_equal;
test_compare_condition_not_less_than_or_equal;

test_compare_condition_equal;
test_compare_condition_not_equal;
test_compare_condition_equal_array;
test_compare_condition_not_equal_array;
