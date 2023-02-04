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

my @phraselists = (
    [ 'купить свадебное платье', 'купить наркотики' ],
);


sub test_norm_phrase_list {
    for my $case (@phraselists) {
        my $phl = $proj->phrase_list($case);
        my $result = $phl->norm_phrase_list;
        like(join(',', @$result), qr/\w+/, 'norm_phrase_list');
    }
}

sub test_get_wide_phrases {
    for my $case (@phraselists) {
        my $phl = $proj->phrase_list($case);
        my $result = $phl->get_wide_phrases;
        like(join(',', @$result), qr/\w+/, 'get_wide_phrases');
    }
}

sub test_parse {
    for my $case (@phraselists) {
        my $phl = $proj->phrase_list($case);
        my $result = $phl->parse;
        like(join(',', @$result), qr/\w+/, 'parse');
    }
}
test_norm_phrase_list();
test_get_wide_phrases();
test_parse();
done_testing();

