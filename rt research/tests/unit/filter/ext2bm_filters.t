#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';
use Test::More tests => 22;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use BaseProject;


# Не тестируем фильтры, которые ходят в интернет - contains или titlecontains


my $proj = BaseProject->new({});
my $data_sample = [
    { 'url' => 'http://Example.com/dogs', title => 'Примеры собак' },
    { 'url' => 'http://example.com/dogs/1', title => 'Собака 1' },
    { 'url' => 'http://example.com/cats', title => 'Примеры кошек' },
    { 'url' => 'http://example.ru/cats/1', title => 'Кошка 1' },
    { 'url' => 'http://example.fi/cats/about', title => 'О кошках' },
];


sub test_domainlike_condition {
    my $filter_condition = [
        { 'kind' => 'exact', value => 'example.com', type => 'domain' }, ];
    my $filter = BM::Filter::ext2bm($filter_condition);
    my $filter_expected = { 'url domainlike' => [ 'example.com' ] };
    subtest filter_ok => sub {
        plan tests => 1;
        is_deeply($filter, $filter_expected);
    };

    my $filtered = $proj->filter({ filter => $filter })->filter($data_sample);
    my $expected = [
        { 'url' => 'http://Example.com/dogs', title => 'Примеры собак' },
        { 'url' => 'http://example.com/dogs/1', title => 'Собака 1' },
        { 'url' => 'http://example.com/cats', title => 'Примеры кошек' },

    ];
    is_deeply($filtered, $expected, 'test domainlike condition')
}

sub test_domainlike_condition_uc {
    my $filter_condition = [
        { 'kind' => 'exact', value => 'Example.com', type => 'domain' }, ];
    my $filter = BM::Filter::ext2bm($filter_condition);
    my $filter_expected = { 'url domainlike' => [ 'Example.com' ] };
    subtest filter_ok => sub {
        plan tests => 1;
        is_deeply($filter, $filter_expected);
    };

    my $filtered = $proj->filter({ filter => $filter })->filter($data_sample);
    my $expected = [
        { 'url' => 'http://Example.com/dogs', title => 'Примеры собак' },
        { 'url' => 'http://example.com/dogs/1', title => 'Собака 1' },
        { 'url' => 'http://example.com/cats', title => 'Примеры кошек' },

    ];
    is_deeply($filtered, $expected, 'test domainlike condition uc')
}

sub test_not_domainlike_condition {
    my $filter_condition = [
        { 'kind' => 'not_exact', value => 'example.com', type => 'domain' },
    ];
    my $filter = BM::Filter::ext2bm($filter_condition);
    my $filter_expected = { 'url NOT domainlike' => [ 'example.com' ] };
    subtest filter_ok => sub {
        plan tests => 1;
        is_deeply($filter, $filter_expected);
    };

    my $filtered = $proj->filter({ filter => $filter })->filter($data_sample);
    my $expected = [
        { 'url' => 'http://example.ru/cats/1', title => 'Кошка 1' },
        { 'url' => 'http://example.fi/cats/about', title => 'О кошках' },
    ];
    is_deeply($filtered, $expected, 'test not domainlike condition')
}

sub test_url_ilike_condition {
    my $filter_condition = [
        { 'kind' => 'exact', value => 'cats', type => 'url' },
    ];
    my $filter = BM::Filter::ext2bm($filter_condition);
    my $filter_expected = { 'url ilike' => [ 'cats' ] };
    subtest filter_ok => sub {
        plan tests => 1;
        is_deeply($filter, $filter_expected);
    };

    my $filtered = $proj->filter({ filter => $filter })->filter($data_sample);
    my $expected = [
        { 'url' => 'http://example.com/cats', title => 'Примеры кошек' },
        { 'url' => 'http://example.ru/cats/1', title => 'Кошка 1' },
        { 'url' => 'http://example.fi/cats/about', title => 'О кошках' },
    ];
    is_deeply($filtered, $expected, 'test url ilike condition')
}

sub test_url_not_ilike_condition {
    my $filter_condition = [
        { 'kind' => 'not_exact', value => 'cats', type => 'url' },
    ];
    my $filter = BM::Filter::ext2bm($filter_condition);
    my $filter_expected = { 'url NOT ilike' => [ 'cats' ] };
    subtest filter_ok => sub {
        plan tests => 1;
        is_deeply($filter, $filter_expected);
    };

    my $filtered = $proj->filter({ filter => $filter })->filter($data_sample);
    my $expected = [
        { 'url' => 'http://Example.com/dogs', title => 'Примеры собак' },
        { 'url' => 'http://example.com/dogs/1', title => 'Собака 1' },
    ];
    is_deeply($filtered, $expected, 'test url not ilike condition')
}

sub test_match_condition {
    my $filter_condition = [
        { 'kind' => 'match', value => 'кошка', type => 'title' },
    ];
    my $filter = BM::Filter::ext2bm($filter_condition);
    my $filter_expected = { 'title normlike' => [ 'кошка' ] };
    subtest filter_ok => sub {
        plan tests => 1;
        is_deeply($filter, $filter_expected);
    };

    my $filtered = $proj->filter({ filter => $filter })->filter($data_sample);
    my $expected = [
        { 'url' => 'http://example.com/cats', title => 'Примеры кошек' },
        { 'url' => 'http://example.ru/cats/1', title => 'Кошка 1' },
        { 'url' => 'http://example.fi/cats/about', title => 'О кошках' },
    ];
    is_deeply($filtered, $expected, 'test match condition')
}

sub test_not_match_condition {
    my $filter_condition = [
        { 'kind' => 'not_match', value => 'кошка', type => 'title' },
    ];
    my $filter = BM::Filter::ext2bm($filter_condition);
    my $filter_expected = { 'title NOT normlike' => [ 'кошка' ] };
    subtest filter_ok => sub {
        plan tests => 1;
        is_deeply($filter, $filter_expected);
    };

    my $filtered = $proj->filter({ filter => $filter })->filter($data_sample);
    my $expected = [
        { 'url' => 'http://Example.com/dogs', title => 'Примеры собак' },
        { 'url' => 'http://example.com/dogs/1', title => 'Собака 1' },
    ];
    is_deeply($filtered, $expected, 'test not match condition')
}

sub test_not_match_and_condition {
    my $filter_condition = [
        { 'kind' => 'not_match', value => 'кошка', type => 'title' },
        { 'kind' => 'not_match', value => 'Примеры', type => 'title' },
    ];
    my $filter = BM::Filter::ext2bm($filter_condition);
    my $filter_expected = { 'title NOT normlike' => [ 'кошка' ], 'title NOT normlike 1' => [ 'Примеры' ] };
    subtest filter_ok => sub {
        plan tests => 1;
        is_deeply($filter, $filter_expected);
    };

    my $filtered = $proj->filter({ filter => $filter })->filter($data_sample);
    my $expected = [
        { 'url' => 'http://example.com/dogs/1', title => 'Собака 1' },
    ];
    is_deeply($filtered, $expected, 'test not match and condition')
}

sub test_match_and_condition {
    my $filter_condition = [
        { 'kind' => 'match', value => 'кошка', type => 'title' },
        { 'kind' => 'match', value => 'Примеры', type => 'title' },
    ];
    my $filter = BM::Filter::ext2bm($filter_condition);
    my $filter_expected = { 'title normlike' => [ 'кошка' ], 'title normlike 1' => [ 'Примеры' ] };
    subtest filter_ok => sub {
        plan tests => 1;
        is_deeply($filter, $filter_expected);
    };

    my $filtered = $proj->filter({ filter => $filter })->filter($data_sample);
    my $expected = [
        { 'url' => 'http://example.com/cats', title => 'Примеры кошек' },
    ];
    is_deeply($filtered, $expected, 'test match and condition')
}

sub test_not_match_or_condition {
    my $filter_condition = [
        { 'kind' => 'not_match', value => ['кошка', 'Примеры'], type => 'title' },
    ];
    my $filter = BM::Filter::ext2bm($filter_condition);
    my $filter_expected = { 'title NOT normlike' => [ 'кошка', 'Примеры' ] };
    subtest filter_ok => sub {
        plan tests => 1;
        is_deeply($filter, $filter_expected);
    };

    my $filtered = $proj->filter({ filter => $filter })->filter($data_sample);
    my $expected = [
        { 'url' => 'http://Example.com/dogs', title => 'Примеры собак' },
        { 'url' => 'http://example.com/dogs/1', title => 'Собака 1' },
        { 'url' => 'http://example.ru/cats/1', title => 'Кошка 1' },
        { 'url' => 'http://example.fi/cats/about', title => 'О кошках' },
    ];
    is_deeply($filtered, $expected, 'test not match or condition')
}

sub test_match_or_condition {
    my $filter_condition = [
        { 'kind' => 'match', value => ['кошка', 'Примеры'], type => 'title' },
    ];
    my $filter = BM::Filter::ext2bm($filter_condition);
    my $filter_expected = { 'title normlike' => [ 'кошка', 'Примеры' ] };
    subtest filter_ok => sub {
        plan tests => 1;
        is_deeply($filter, $filter_expected);
    };

    my $filtered = $proj->filter({ filter => $filter })->filter($data_sample);
    my $expected = [
        { 'url' => 'http://Example.com/dogs', title => 'Примеры собак' },
        { 'url' => 'http://example.com/cats', title => 'Примеры кошек' },
        { 'url' => 'http://example.ru/cats/1', title => 'Кошка 1' },
        { 'url' => 'http://example.fi/cats/about', title => 'О кошках' },
    ];
    is_deeply($filtered, $expected, 'test match or condition')
}


# run tests
test_domainlike_condition;
test_domainlike_condition_uc;
test_not_domainlike_condition;

test_url_ilike_condition;
test_url_not_ilike_condition;

test_match_condition;
test_not_match_condition;

test_match_and_condition;
test_match_or_condition;

test_not_match_and_condition;
test_not_match_or_condition;
