#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';


use Test::More;
use FindBin;
use lib "$FindBin::Bin/../../../lib";

use Project;
use BM::BannersMaker::Product;

my $proj = Project->new({load_dicts => 1 });

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';

# costyl
my $builder = Test::More->builder;
binmode $builder->output,         ":encoding(utf8)";
binmode $builder->failure_output, ":encoding(utf8)";
binmode $builder->todo_output,    ":encoding(utf8)";

my $pt = BM::BannersMaker::Product->new({ proj => $proj, () });

my $case_type = "has no duplicates";
{
    my @cases = (
        "Кровать 100х100",
        "Кровать 100 х 100",
        "Кровать 100 х 100...",
        "Кровать 100 на 100",
        "Кровать 100 на 100...",
        # joseph & joseph - brand
        "Joseph & joseph посуда",
        "Посуда Joseph & Joseph",
        "Посуда Joseph & Joseph...",
        "Joseph & joseph кровать 200 x 200",
        "Joseph & joseph куб 200 x 200 х 200",
        "Куб Joseph & joseph 200 x 200 х 200",
        "Брус 300 на 50 на 50",
    );
    for my $case (@cases) {
        ok(!$pt->is_title_has_dup_words($case), "Test: [$case] $case_type");
    }
}

$case_type = "has duplicates";
{
    my @cases = (
        "Кровать 200х100 кровать",
        "Кровать 200х100 кровать...",
        "Кровать... Кровать 100 х 100",
        "Кровать 200х100 200х100 кровать",
        "Кровать 100 х 100 100 х 100",
        "Кровать 100 х 100, 100 х 100",
        "Кровать 100 х 100, 100 х 100...",
    );
    for my $case (@cases) {
        ok($pt->is_title_has_dup_words($case, 1), "Test: [$case] $case_type");
    }
}


done_testing();
