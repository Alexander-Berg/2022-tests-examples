#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More tests => 2;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use BaseProject;

my $proj = BaseProject->new({});
my $data_sample = [
    { name => 'a1', url => 'http://example/1.html' },
    { name => 'a2', url => 'http://example/2.html', new_field => 'value' },
    { name => '0',  url => 'http://example/3.html' },
    { name => '',   url => 'http://example/4.html' },
    { name => 'a5', url => 'http://example/5.html', new_field => '1' },
    { name => 'a6', url => 'http://example/6.html', new_field => '' },
    { name => 'a7', url => 'http://example/7.html', new_field => 0 },
    { name => 'a8', url => 'http://example/7.html', New_field => 8 },
];


sub test_exists_condition {
    my $filter_condition = {
        'new_field exists' => 1,
    };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a2', url => 'http://example/2.html', new_field => 'value' },
        { name => 'a5', url => 'http://example/5.html', new_field => '1' },
        { name => 'a6', url => 'http://example/6.html', new_field => '' },
        { name => 'a7', url => 'http://example/7.html', new_field => 0 },
    ];
    is_deeply($filtered, $expected)
}

sub test_not_exists_condition {
    my $filter_condition = {
        'new_field NOT exists' => 1,
    };
    my $filtered = $proj->filter({ filter => $filter_condition })->filter($data_sample);
    my $expected = [
        { name => 'a1', url => 'http://example/1.html' },
        { name => '0', url => 'http://example/3.html' },
        { name => '', url => 'http://example/4.html' },
        { name => 'a8', url => 'http://example/7.html', New_field => 8 },
    ];
    is_deeply($filtered, $expected)
}


# run tests
test_exists_condition;
test_not_exists_condition;
