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
    { name => '',   price => '' },
    { name => '0',  price => '0' },
    { name => 'a1', price => '2' },
    { name => 'a2', price => '5', },
    { name => 'a3', price => '14.50', },
    { name => 'a4', price => '300.00', },
    { name => 'a5', price => '500.00', },
    { name => 'A5', price => '1500.00', },
];


# like tests
sub test_compare_condition_like_single {
    my $filter_condition = { 'name like' => 'a5', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a5', price => '500.00', },
        { name => 'A5', price => '1500.00', },
    ];
    is_deeply($filtered, $expected, 'test compare condition: like single condition')
}

sub test_compare_condition_like_array {
    my $filter_condition = { 'name like' => [ '5', 'a3' ], };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a3', price => '14.50', },
        { name => 'a5', price => '500.00', },
        { name => 'A5', price => '1500.00', },

    ];
    is_deeply($filtered, $expected, 'test compare condition: like array condition')
}

sub test_compare_condition_like_empty {
    my $filter_condition = { 'name like' => '', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = $data_sample;
    is_deeply($filtered, $expected, 'test compare condition: like empty string')
}

sub test_compare_condition_not_like_empty {
    my $filter_condition = { 'name NOT like' => '', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [];
    is_deeply($filtered, $expected, 'test compare condition: like empty string')
}

# ilike tests
sub test_compare_condition_ilike_single {
    my $filter_condition = { 'name ilike' => 'A5', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a5', price => '500.00', },
        { name => 'A5', price => '1500.00', },
    ];
    is_deeply($filtered, $expected, 'test compare condition: ilike single condition')
}

sub test_compare_condition_ilike_array {
    my $filter_condition = { 'name ilike' => [ 'A5', 'A3' ], };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a3', price => '14.50', },
        { name => 'a5', price => '500.00', },
        { name => 'A5', price => '1500.00', },
    ];
    is_deeply($filtered, $expected, 'test compare condition: ilike array condition')
}

sub test_compare_condition_ilike_empty {
    my $filter_condition = { 'name ilike' => '', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = $data_sample;
    is_deeply($filtered, $expected, 'test compare condition: ilike empty string')
}

# range tests
sub test_compare_condition_range_from_zero {
    my $filter_condition = { 'price <->' => '0-300', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => '',   price => '' },
        { name => '0',  price => '0' },
        { name => 'a1', price => '2' },
        { name => 'a2', price => '5', },
        { name => 'a3', price => '14.50', },
        { name => 'a4', price => '300.00', },
    ];
    is_deeply($filtered, $expected, 'test compare conditions: range from zero')
}

sub test_compare_condition_range_to_zero {
    my $filter_condition = { 'price <->' => '2-0', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [];
    is_deeply($filtered, $expected, 'test compare conditions: range')
}

sub test_compare_condition_range_single {
    my $filter_condition = { 'price <->' => '2.50-14.5', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a2', price => '5', },
        { name => 'a3', price => '14.50', },
    ];
    is_deeply($filtered, $expected, 'test compare conditions: range single')
}

sub test_compare_condition_range_array {
    my $filter_condition = { 'price <->' => [ '2.50-14.5', '200-300' ], };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a2', price => '5', },
        { name => 'a3', price => '14.50', },
        { name => 'a4', price => '300.00', },
    ];
    is_deeply($filtered, $expected, 'test compare conditions: range array')
}

sub test_compare_condition_not_range_array {
    my $filter_condition = { 'price NOT <->' => [ '2.50-14.5', '200-500' ], };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => '', price => '' },
        { name => '0', price => '0' },
        { name => 'a1', price => '2' },
        { name => 'A5', price => '1500.00', },

    ];
    is_deeply($filtered, $expected, 'test compare not conditions: range array')
}


# run tests
test_compare_condition_like_single;
test_compare_condition_like_array;
test_compare_condition_like_empty;
test_compare_condition_not_like_empty;

test_compare_condition_ilike_single;
test_compare_condition_ilike_array;
test_compare_condition_ilike_empty;

test_compare_condition_range_from_zero;
test_compare_condition_range_to_zero;
test_compare_condition_range_single;
test_compare_condition_range_array;
test_compare_condition_not_range_array;
