#!/usr/bin/env perl
use strict;
use warnings;

use utf8;

use Test::More;
use FindBin;
use lib "$FindBin::Bin/../../../lib";
use Utils::UniDecode::UniDecode;

my $decoder = Utils::UniDecode::UniDecode->new();

my @cases = (
    ['', ''],
    ['Купить слона', 'Купить слона'],
    ['Купить        слона', 'Купить слона'],
    ['Купить | слона ', 'Купить - слона'],
    ['北亰', 'Bei Jing'],
    ['Комнатная температура 20 ℃   |   площадь комнаты 20 м²', 'Комнатная температура 20 °C - площадь комнаты 20 м²'],
    ['grzegorz brzęczyszczykiewicz', 'grzegorz brzeczyszczykiewicz'],
);

for my $case(@cases) {
    is($decoder->Decode($case->[0]), $case->[1]);
}

done_testing();

1;
