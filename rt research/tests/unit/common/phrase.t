#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More;

use FindBin;
use lib "$FindBin::Bin/../../../lib";

use Project;
my $proj = Project->new({
    load_dicts                              => 1,
    load_minicategs_light                   => 1,
    use_comptrie_subphraser                 => 1,
    use_sandbox_categories_suppression_dict => 1,
});

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';

# costyl
my Test::Builder $builder = Test::More->builder;
binmode $builder->output, ":encoding(utf8)";
binmode $builder->failure_output, ":encoding(utf8)";
binmode $builder->todo_output, ":encoding(utf8)";

sub test_get_geobase_region_ids {
    #copied from tests/geo/test_geo_region_id.pl
    my @test_data = (
        {
            param     => 'купить валенки в москве и санкт-петербурге',
            result    => [ 2, 213 ],
            test_name => 'get_geobase_region_ids msk+spb',
        },
        {
            param     => 'продать телевизор в Нижнем Новгороде',
            result    => [ 47 ],
            test_name => 'get_geobase_region_ids nn',
        },
    );
    for my $h (@test_data) {
        my BM::Phrase $phr = $proj->phrase($h->{param});
        my $res_actual = [ $phr->get_geobase_region_ids() ];
        is_deeply($res_actual, $h->{result}, $h->{test_name});
    }
}

sub test_is_porno_phrase {
    # https://st.yandex-team.ru/IRT-1275
    my @test_data = (
        { param => 'всё через жопу', result => 1, test_name => 'Porno phrase, no syntax', },
        { param => 'всё через +жопу', result => 1, test_name => 'Porno phrase, plusword', },
        { param => 'всё через !жопу', result => 1, test_name => 'Porno phrase, escaped', },
        { param => 'всё через -жопу', result => 0, test_name => 'Porno minusword doesn\'t make porno phrase', },
        { param => 'всё через счастье', result => 0, test_name => 'Not porno phrase, no syntax', },
        { param => 'всё через +счастье', result => 0, test_name => 'Not porno phrase, plusword', },
        { param => 'всё через !счастье', result => 0, test_name => 'Not porno phrase, escaped', },
        { param => 'всё через -счастье', result => 0, test_name => 'Not porno phrase, minusword', },
        { param => 'как приготовить единорога', result => 0, test_name => 'как is not porn', },
        { param => '+как приготовить единорога', result => 0, test_name => '+как is not porn', },
        { param => 'кака это вообще-то футболист', result => 1, test_name => 'кака is porn', },
    );

    for my $h (@test_data) {
        my $phr = $proj->phrase($h->{param});
        my $res = $phr->is_porno_phrase ? 1 : 0;
        is($res, $h->{result}, $h->{test_name});
    }
}

test_new();
test_get_geobase_region_ids();
test_is_porno_phrase();

done_testing();
