#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More;
use FindBin;
use lib "$FindBin::Bin/../../../lib";

use Project;
use BM::BannersMaker::BannerLandProject;
use BM::BannersMaker::DSETools;

my $proj = BM::BannersMaker::BannerLandProject->new({ load_dicts => 1 });

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';

# costyl
my $builder = Test::More->builder;
binmode $builder->output, ":encoding(utf8)";
binmode $builder->failure_output, ":encoding(utf8)";
binmode $builder->todo_output, ":encoding(utf8)";


{
    my @test_cases = (
        # input, expected result
        [ 'Купить - белую светодиодную ленту, свет - холодный',
          'Купить - белую светодиодную ленту, свет - холодный' ],
        [ ';. Как сэкономить железнодорожных билетах?',
          'Как сэкономить железнодорожных билетах?'],
        [ '; Граверы и мини-дрели',
          'Граверы и мини-дрели'],
        [ '; Пилы цепные:', 
          'Пилы цепные'],
        [ 'Чай и кофе в Москве - Быстрая доставка на Flowwow - ',
          'Чай и кофе в Москве - Быстрая доставка на Flowwow'],
    );

    my $sub_test_num = 1;

    for my $case (@test_cases) {
        my $input = $case->[0];
        my $expected = $case->[1];
        my $removed = $proj->dse_tools->remove_text_artifacts($input);
        my $title = ucfirst($proj->phrase($removed)->casecorrection->text);
        is($title, $expected, "remove_text_artifacts #$sub_test_num");
        $sub_test_num += 1;
    }
}

done_testing();
