#!/usr/bin/perl -w
#выбор фраз из Каталогии для поиска дублей (чтобы иметь фиксированную базу на момент обработки)

use strict;
use utf8;
use open ':utf8';
no warnings 'utf8';
use Data::Dumper;

binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

while (<STDIN>) { #caddphr_web_ru
    chomp;

    my ($ctg, $phrs) = split /\t/;
    next unless $ctg =~ /^(Андрология|Урология)$/; #####
    my $id = $ctg eq "Андрология" ? 200013863 : 200014501;

    my @ctg_phrs = split /,/, $phrs;
    @ctg_phrs = sort {lc($a) cmp lc($b)} @ctg_phrs; #сортировка без учета регистра

    if (@ctg_phrs) {
        my $phr_num = 0; #порядковый номер фразы внутри категории
        for my $ctg_phr (@ctg_phrs) {
            $phr_num++;
            print "$ctg_phr\t$id\t$phr_num\n";
        }
    }
}
