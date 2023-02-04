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
        [ 'Купить - белую светодиодную ленту, свет - холодный',    'Купить - белую светодиодную ленту, свет - холодный' ],
        [ 'Купить 1 комнатную квартиру дом 54 в Ждановском...',    'Купить 1 комнатную квартиру дом 54 в Ждановском...' ],
        [ 'Купить запчасти по низкой цене в России',               'Купить запчасти по низкой цене в России' ],
        [ 'Купить варочную панель Домино по низкой цене в Москве', 'Купить варочную панель Домино по низкой цене в Москве'],
        [ 'Купить нижнее белье, мужскую одежду',                   'Купить нижнее белье, мужскую одежду'],
        [ 'Купить нижнее белье и мужскую одежду',                  'Купить нижнее белье и мужскую одежду'],

        [ 'Купить блок предохранителей на Saab 9-5...',                'Блок предохранителей на Saab 9-5...' ],
        [ 'Купить 1 комнатная квартира 11-й микрорайон дом 50...',     '1 комнатная квартира 11-й микрорайон дом 50...' ],
        [ 'Купить Gutenberg бальзам "альпийский", 250 мл в Москве...', 'Gutenberg бальзам "альпийский", 250 мл в Москве...' ],
        [ 'Купить оптом Mist Fixer Спрей-фиксатор для макияжа',        'Оптом Mist Fixer спрей-фиксатор для макияжа' ],
        [ 'Купить очки виртуальной реальности',                        'Очки виртуальной реальности' ],
        [ 'Купить очки виртуальной реальности и геймпады',             'Очки виртуальной реальности и геймпады' ],
        [ 'Купить очки виртуальной реальности и геймпад',              'Очки виртуальной реальности и геймпад' ],
        [ 'Купить очки виртуальной реальности в Москве',               'Очки виртуальной реальности в Москве' ],
        [ 'Купить красные ножницы',                                    'Красные ножницы' ],
        [ 'Купить лыжные очки',                                        'Лыжные очки' ],
        [ 'Купить встраиваемые светильники по низким ценам',           'Встраиваемые светильники по низким ценам' ],
        [ 'Купить красивые красные',                                   'Красивые красные' ],
        [ 'Купить межкомнатные двери с гарантией в Москве',            'Межкомнатные двери с гарантией в Москве' ],
        [ 'Купить МФУ в Казахстане',                                   'МФУ в Казахстане' ],
        [ 'Купить стремянки по низкой цене Лестница.ру',               'Стремянки по низкой цене Лестница.ру'],
    );

    my $sub_test_num = 1;

    for my $case (@test_cases) {
        my $input = $case->[0];
        my $expected = $case->[1];
        my $removed = $proj->dse_tools->remove_unnecessary_words($input);
        my $title = ucfirst($proj->phrase($removed)->casecorrection->text);
        is($title, $expected, "remove_unnecessary_words #$sub_test_num");
        $sub_test_num += 1;
    }
}

{
    my @test_cases = (
        ['Фильм.', 'Фильм'],
        ['Фильм. фильм. фильм.', 'Фильм'],
        ['Фильм! фильм! фильм!', 'Фильм'],
        ['Фильм? фильм? фильм?', 'Фильм'],
    );
    my $sub_test_num = 1;
    for my $case (@test_cases) {
        my $input = $case->[0];
        my $expected = $case->[1];
        my $context = BM::BannersMaker::DSETools::_get_first_sentence($input);
        is($context, $expected, "_get_first_sentence #$sub_test_num");
        $sub_test_num += 1;
    }
}

{
    my @test_cases = (
        ['Кроссовки',  'Кроссовки'],
        ['Кроссовки из замши',  'Кроссовки'],
        ['Кроссовки без шнурков', 'Кроссовки'],
    );
    my $sub_test_num = 1;
    for my $case (@test_cases) {
        my $input = $case->[0];
        my $expected = $case->[1];
        my $context = BM::BannersMaker::DSETools::_get_text_first_part($input);
        is($context, $expected, "_get_text_first_part #$sub_test_num");
        $sub_test_num += 1;
    }
}

{
    my @test_cases_ok = (
        'Межкомнатные двери',
        'Ботинки',
        'Очки',
    );

    my $sub_test_num = 1;
    for my $case (@test_cases_ok) {
        ok($proj->dse_tools->_is_possible_to_remove_unnecessary_words($case), "is possible to remove #$sub_test_num ($case)");
        $sub_test_num += 1;
    }

    my @test_cases_not_ok = (
        'Квартиру',
        'Машину',
    );
    for my $case (@test_cases_not_ok) {
        ok(!$proj->dse_tools->_is_possible_to_remove_unnecessary_words($case), "is not possible to remove #$sub_test_num ($case)");
        $sub_test_num += 1;
    }
}

done_testing();
