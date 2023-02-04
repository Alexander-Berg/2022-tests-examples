#!/usr/bin/perl -w
#тестирование результатов снятия омонимии

use strict;
use utf8;
use open ":utf8";
use Data::Dumper;

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';
binmode STDERR, ':utf8';

my %dict; # словарь омонимов со снятой омонимией
open F, "disamb_uniq";
while (<F>) {
    chomp;
    my ($hom, $val) = split /\t/;
    $dict{$hom} = $val;
}

while (<STDIN>) { #../data/bnrs_10kk.camp.disamb
    chomp;
    my ($id, $bnr, $hom, $val) = split /\t/;
    next unless $dict{$hom};
    print "$id\t$bnr\t$hom\t$dict{$hom}\n";
}
