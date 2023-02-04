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


my $proj = BM::BannersMaker::BannerLandProject->new({load_dicts => 1 });

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';

# costyl
my $builder = Test::More->builder;
binmode $builder->output,         ":encoding(utf8)";
binmode $builder->failure_output, ":encoding(utf8)";
binmode $builder->todo_output,    ":encoding(utf8)";

sub test_truncate_titles_runner {
    my ($cases, $max_length, $test_name) = @_;
    my $sub_test_num = 1;
    for my $case (@$cases) {
        my $input = $case->[0];
        my $expected = $case->[1];
        is($proj->dse_tools->truncate_title($input, $max_length), $expected, "[$test_name]: $sub_test_num");
        $sub_test_num += 1;
    }
}

sub test_truncate_titles_exp_runner {
    my ($cases, $max_length, $test_name) = @_;
    my $sub_test_num = 1;
    for my $case (@$cases) {
        my $input = $case->[0];
        my $expected = $case->[1];
        is($proj->dse_tools->truncate_title($input, $max_length, ignore_narrow => 1), $expected, "[$test_name]: $sub_test_num");
        $sub_test_num += 1;
    }
}

my @not_need_truncate_cases = (
    # input, expected result
    ['Купить телефон Samsung',        'Купить телефон Samsung'],
    ['Книга: "Perl Best Practices"', 'Книга: "Perl Best Practices"'],
);


my @need_truncate_cases = (
    # input, expected result
    ['Онлайн белая хлопковая туника из Китая',                 'Онлайн белая хлопковая туника'],
    ['Sbordoni Deco Мыльница настольная, цвет: никель',        'Sbordoni Deco Мыльница...'],
    ['JP Group 1187200280 Ручка открывания задней двери...',   'JP Group 1187200280 Ручка...'],
    ['Браслет из золота с аметистами и фианитами 540306',      'Браслет из золота с аметистами'],
    ['Браслет металлический со стразами 9 рядов серебряный 1', 'Браслет металлический'],
    ['Браслетик в стиле "Кантри" — купить',                    'Браслетик в стиле "Кантри...'],
    ['"Чиббо Давидофф Эспрессо" 250гр молотый (12...',         '"Чиббо Давидофф Эспрессо...'],
    ['Gutenberg бальза "альпийский", 250 мл в Москве...',      'Gutenberg бальза "альпийский...'],
    ['Gutenberg бальза "альпийский  ", 25',                    'Gutenberg бальза "альпийский...'],
    ['Кабель для телефона honor type-C за 1000 р.',            'Кабель для телефона honor...'],
    ['Кабель для телефона honor mate за 1000 р.',              'Кабель для телефона honor mate'],
    ['Кабель для телефона honor mate безопасный',              'Кабель для телефона honor mate...'],
    ['Kremlin Смеситель без д/о для установки',                'Kremlin Смеситель'],
    ['Брюки вязанные из мериноса и кашемира. Цвет черный',     'Брюки вязанные из мериноса'],
);


test_truncate_titles_runner(\@not_need_truncate_cases, 33, "Not need truncate");
test_truncate_titles_runner(\@need_truncate_cases, 33, "Need truncate");

my @not_need_truncate_cases_exp = (
    # input, expected result
    ['Купить телефон Samsung',        'Купить телефон Samsung'],
    ['Книга: "Perl Best Practices"', 'Книга: "Perl Best Practices"'],
    ['Клей для натурального камня "Pereli"!', 'Клей для натурального камня "Pereli"!'], # len = 37 with narrow symbols
    ['Клей для натурального камня Keramog', 'Клей для натурального камня Keramog'],     # len = 35
    ['Клей! Для натурального камня "Keramog"...', 'Клей! Для натурального камня "Keramog"...'], # len = 41 with narrow symbols
);

my @need_truncate_cases_exp = (
    # input, expected result
    ['Онлайн белая хлопковая туника из Китая',                 'Онлайн белая хлопковая туника'],
    ['Sbordoni Deco Мыльница настольная, цвет: никель',        'Sbordoni Deco Мыльница настольная...'],
    ['JP Group 1187200280 Ручка открывания задней двери...',   'JP Group 1187200280 Ручка...'],
    ['Браслет из золота с аметистами и фианитами 540306',      'Браслет из золота с аметистами'],
    ['Браслет металлический со стразами 9 рядов серебряный 1', 'Браслет металлический со стразами 9...'],
    ['Браслетик в стиле "Кантри" — купить',                    'Браслетик в стиле "Кантри" — купить'],
    ['"Чиббо Давидофф Эспрессо" 250гр молотый (12...',         '"Чиббо Давидофф Эспрессо" 250гр...'],
    ['Gutenberg бальза "альпийский", 250 мл в Москве...',      'Gutenberg бальза "альпийский", 250 мл'],
    ['Кабель для телефона honor type-C за 1000 р.',            'Кабель для телефона honor type-C'],
    ['Кабель для телефона honor mate за 1000 р.',              'Кабель для телефона honor mate'],
    ['Кабель для телефона honor mate безопасный',              'Кабель для телефона honor mate...'],
    ['Kremlin Смеситель без д/о для установки',                'Kremlin Смеситель'],
    ['Брюки вязанные из мериноса и кашемира. Цвет черный',     'Брюки вязанные из мериноса'],
    ['Брюки вязанные из мериноса и кашемира. Цвет черный - s-m - стандарт',     'Брюки вязанные из мериноса'],
);

test_truncate_titles_exp_runner(\@not_need_truncate_cases_exp, 35, "Not need truncate");
test_truncate_titles_exp_runner(\@need_truncate_cases_exp, 35, "Need truncate");

done_testing();
