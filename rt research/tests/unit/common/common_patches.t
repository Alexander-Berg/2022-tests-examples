#!/usr/bin/env perl

use strict;
use warnings;
use utf8;

use Test::More;

use FindBin;
use lib "$FindBin::Bin/../../../lib";

use Utils::CommonPatches;

my @parse_patch_str_tests = (
    {
        comment => "one item",
        input_str => "some/long/path:value",
        expected_result => {
            some => { long => { path => "value" } },
        },
    },
    {
        comment => "two items",
        input_str => "some/long/path:value;short_path:other value",
        expected_result => {
            some => { long => { path => "value" } },
            short_path => "other value",
        },
    },
    {
        comment => "items that share parts of the path",
        input_str => "some/long/path:value;some/long/other/path:other value;some/third/path:third value",
        expected_result => {
            some => {
                long => {
                    path => "value",
                    other => { path => "other value" },
                },
                third => { path => "third value" },
            },
        },
    },
    {
        comment => "empty input str",
        input_str => "",
        expected_result => {},
    },
    {
        comment => "undefined input",
        input_str => undef,
        expected_result => {},
    },
);

for my $test (@parse_patch_str_tests) {
    my $result = Utils::CommonPatches::_parse_patch_str($test->{input_str});
    is_deeply($result, $test->{expected_result}, "Utils::CommonPatches::_parse_patch_str() tests: " . $test->{comment});
}


done_testing();
