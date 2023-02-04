#!/usr/bin/perl -w
#выбор списка баннеров для тестирования в интерфейсе

use strict;
use utf8;
use open ':utf8';
no warnings 'utf8';
use Data::Dumper;

binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

my $NUM = 500; #число баннеров для тестирования
my $ORD = 1; #порядковый номер в категории

my %bnrs;
my $cnt = 0;

my %ctgs_bad = (
    "Русский язык" => 1,
    "Опт _ Автомобили" => 1,
    "#ЕГЭ" => 1,
    "Монтаж _ Системы вентиляции и кондиционирования" => 1,
);

print "bid\ttitle\tbody\touter_category\n"; #заголовки столбцов таблицы
while (<STDIN>) { #zzz
    chomp;

    my ($bid, $title, $body, $outer_category, $nctgs_phrs, $ent, $rank, $sim, $ctg_siz, $wrd_num) = split /\t/;
    next if $ctgs_bad{$outer_category};

    next if $outer_category eq "Гостиницы России" && "$title $body" =~ /Украин/;
    next if $outer_category eq "Рассчетно-кассовое обслуживание" && "$title $body" !~ /(ООО|ИП)/;

    $bnrs{$outer_category}++;
    next if $bnrs{$outer_category} != $ORD;

    print "$bid\t$title\t$body\t$outer_category\n";
    #print "$bid\t$title\t$body\t$outer_category\t$ctg_siz\n";
    last if ++$cnt == $NUM;
}
