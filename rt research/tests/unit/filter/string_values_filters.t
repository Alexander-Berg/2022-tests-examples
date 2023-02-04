#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More tests => 13;

use FindBin;
use lib "$FindBin::Bin/../../../lib";
use BaseProject;


my $proj = BaseProject->new({});
my $data_sample = [
    { name => '', },
    { name => 'автомат', },
    { name => 'автомобиль', },
    { name => 'Автомобиль', },
    { name => 'арбуз', },
    { name => 'барабан', },
    { name => 'бублик', },
    { name => 'вагон', },
    { name => 'велосипед', },
];


sub test_compare_condition_greater_than {
    my $filter_condition = { 'name gt' => 'ваг', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'вагон', },
        { name => 'велосипед', },
    ];
    is_deeply($filtered, $expected, 'test compare condition: greater than condition')
}

sub test_compare_condition_not_greater_than {
    my $filter_condition = { 'name NOT gt' => 'авто', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => '', },
    ];
    is_deeply($filtered, $expected, 'test compare condition: not greater than condition')
}

sub test_compare_condition_greater_than_or_equal {
    my $filter_condition = { 'name ge' => 'ваг', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'вагон', },
        { name => 'велосипед', },
    ];
    is_deeply($filtered, $expected, 'test compare condition: greater than or equal condition')
}

sub test_compare_condition_not_greater_than_or_equal {
    my $filter_condition = { 'name NOT ge' => 'автомобиль', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => '', },
        { name => 'автомат', },
    ];
    is_deeply($filtered, $expected, 'test compare condition: not greater or equal than condition')
}


sub test_compare_condition_less_than {
    my $filter_condition = { 'name lt' => 'Автомобиль', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => '', },
        { name => 'автомат', },
    ];
    is_deeply($filtered, $expected, 'test compare condition: less than condition')
}

sub test_compare_condition_not_less_than {
    my $filter_condition = { 'name NOT lt' => 'вагон', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'вагон', },
        { name => 'велосипед', },
    ];
    is_deeply($filtered, $expected, 'test compare condition: less than condition')
}

sub test_compare_condition_less_than_or_equal {
    my $filter_condition = { 'name le' => 'автомобиль', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => '', },
        { name => 'автомат',},
        { name => 'автомобиль',},
        { name => 'Автомобиль',},
    ];
    is_deeply($filtered, $expected, 'test compare condition: less than or equal condition')
}

sub test_compare_condition_not_less_than_or_equal {
    my $filter_condition = { 'name NOT le' => 'вагон', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'велосипед', },
    ];
    is_deeply($filtered, $expected, 'test compare condition: not less than or equal condition')
}

sub test_compare_condition_equal {
    my $filter_condition = { 'name eq' => 'бублик', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'бублик',},
    ];
    is_deeply($filtered, $expected, 'test compare condition: equal');

    $filter_condition = { 'name eq' => 'Бублик', };
    $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    $expected = [
        { name => 'бублик',},

    ];
    is_deeply($filtered, $expected, 'test compare condition: equal uc')
}

sub test_compare_condition_not_equal {
    my $filter_condition = { 'name NOT eq' => 'бублик', };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => '', },
        { name => 'автомат', },
        { name => 'автомобиль', },
        { name => 'Автомобиль', },
        { name => 'арбуз', },
        { name => 'барабан', },
        { name => 'вагон', },
        { name => 'велосипед', },
    ];
    is_deeply($filtered, $expected, 'test compare condition: not equal')
}

sub test_compare_condition_equal_array {
    my $filter_condition = {
        'name eq' => ['автомобиль', 'бублик'],
    };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'автомобиль',},
        { name => 'Автомобиль',},
        { name => 'бублик', },
    ];
    is_deeply($filtered, $expected, 'test compare condition: equal array')
}

sub test_compare_condition_not_equal_array {
    my $filter_condition = {
        'name NOT eq' => ['автомобиль', 'арбуз', 'бублик', 'вагон'],
    };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => '', },
        { name => 'автомат', },
        { name => 'барабан', },
        { name => 'велосипед', },
    ];
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
