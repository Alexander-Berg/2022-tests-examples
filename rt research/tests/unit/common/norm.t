#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More;

use FindBin;
use lib "$FindBin::Bin/../../../lib";
use Utils::Words qw(text2words);

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';

# costyl
my $builder = Test::More->builder;
binmode $builder->output,         ":encoding(utf8)";
binmode $builder->failure_output, ":encoding(utf8)";
binmode $builder->todo_output,    ":encoding(utf8)";


my @tokenize_test = (
    [ 'simple test'     => [ qw(simple test) ] ],
    [ 'UCASE CamelCase' => [ qw(ucase camelcase) ] ],
    [ 'а.б.в.г.д'       => [ qw(а б в г д) ] ],
    [ 'a+b'             => [ qw(a b) ] ],
    [ 'c++'             => [ 'c' ] ],
    [ 'a=b c--d'        => [ qw(a b c d) ] ],
    [ '+в москва'       => [ qw(+в москва) ] ],
    [ '!то генератор !то2'   => [ qw(!то генератор !то2) ] ],
    [ '"!то генератор"' => [ qw(!то генератор) ] ],
    #[ "'!то генератор'" => [ qw(!то генератор) ] ],  # непонятно, нужно ли
    [ '"+на столе"'     => [ qw(+на столе) ] ],
    [ 'ubuntu 16.04'    => [ qw(ubuntu 16.04) ] ],
    [ 'ubuntu "16.04"'    => [ qw(ubuntu 16.04) ] ],
    [ 'ubuntu "16 04"'  => [ qw(ubuntu 16 04) ] ],
    [ 'word -minus'     => [ qw(word -minus) ] ],
    [ 'word --minus'    => [ qw(word minus) ] ],
    [ 'word -!minus'    => [ qw(word -!minus) ] ],
    [ '! a + b - -! -+ c /d'    => [ qw(a b c d) ] ],
    [ 'странный -+минус'=> [ qw(странный -+минус) ] ],
    [ 'странный-+минус' => [ qw(странный минус) ] ],
    [ '!!not'           => [ qw(not) ] ],
    [ '!-not'           => [ qw(not) ] ],
    [ '!+not'           => [ qw(not) ] ],
    [ '-1 !2 +3 4 -5.6'           => [ qw(-1 !2 +3 4 -5.6) ] ],
    [ 'без хвостов ~0'           => [ qw(без хвостов 0) ] ],  # да, сейчас так, тильды обрабатываем до токенизатора
    [ '0!1@2#3$4%5^6&7*8(9)10₽11€12' => [ 0 .. 2, '3$4', 5 .. 9, '10₽11€12' ] ],
    [ '/11{12}[13];14:15`16\'17"18"~19	20>21★22' => [ 11 .. 22] ],
    [ 'c++ ++c = c'     => [ qw(c c c) ] ],

    [ 'санкт-петербург'     => [ qw(санкт-петербург) ] ],
    [ '-санкт-петербург'     => [ qw(-санкт-петербург) ] ],
    [ 'ростов-на-дону'     => [ qw(ростов-на-дону) ] ],
    [ 'санкт-+петербург'     => [ qw(санкт петербург) ] ],
    [ 'санкт-!петербург'     => [ qw(санкт петербург) ] ],
    [ '1-2+3 4+5-6 +7+8'     => [ qw(1-2 3 4 5-6 +7 8) ] ],
    [ 'санкт- петербург+ спб!'     => [ qw(санкт петербург спб) ] ],
);

for my $d (@tokenize_test) {
    my ($text, $words) = @$d;
    is_deeply([text2words($text)], $words, $text);
}

done_testing();
