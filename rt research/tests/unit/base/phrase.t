#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More;

use FindBin;
use lib "$FindBin::Bin/../../../lib";
use BaseProject;

my $proj = BaseProject->new({
    load_dicts                              => 1,
    load_minicategs_light                   => 1,
    use_comptrie_subphraser                 => 1,
    use_sandbox_categories_suppression_dict => 1,
});

my @phrases = (
    'купить свадебное платье',
    'купить наркотики',
);


sub test_get_minicategs {
    for my $case (@phrases) {
        my $phr = $proj->phrase($case);
        my @categs = $phr->get_minicategs;
        like(join('/', @categs), qr/\w+/, 'get_minicategs');
    }
}

sub test_norm {
    for my $case (@phrases) {
        my $phr = $proj->phrase($case);
        my $norm = $phr->norm_phr;
        like($norm, qr/\w+/, 'norm');
    }
}

sub test_snorm {
    for my $case (@phrases) {
        my $phr = $proj->phrase($case);
        my $norm = $phr->snorm_phr;
        like($norm, qr/\w+/, 'snorm');
    }
}

sub test_parse {
    for my $case (@phrases) {
        my $phr = $proj->phrase($case);
        my %h = $phr->parse;
        like($h{type}, qr/\w+/, 'parse');
    }
}

test_get_minicategs();
test_norm();
test_snorm();
test_parse();
done_testing();
