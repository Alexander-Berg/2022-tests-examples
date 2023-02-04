#!/usr/bin/perl -w
#проверка категориации с помощью ближайших соседей

use strict;

use utf8;
use open ":utf8";
use Data::Dumper;

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';
binmode STDOUT, ':utf8';

use FindBin;
use lib "$FindBin::Bin/../lib";
use lib "/home/yuryz/arcadia/rt-research/broadmatching/scripts/lib";
use Project;

my $proj = Project->new({
    load_dicts   => 1,
    load_minicategs_light => 1, 
});

my $head = <STDIN>; #"bid    title   body    outer_category" - заголовки столбцов таблицы
chomp $head;
print "$head\t9\t1\n";
my $num = 1; #порядковый номер
while (<STDIN>) { #bnrs_exp4b
    $num++;
    chomp;
    my ($bid, $title, $body, $ctgs) = split /\t/;

    my $bnr = $proj->bf->get_banner_by_id($bid);
    next unless $bnr;

    my @ctgs;
    my $cnt = 0; #число голосов

    my $h = $bnr->get_intent; #ссылка на хеш с ключом intent
    my $phr = $proj->phrase($$h{intent});
    if ($phr) {
        @ctgs = $phr->get_minicategs; #категоризация по intent
        next if @ctgs && join("/", sort @ctgs) ne $ctgs;
        $cnt++ if @ctgs;

        @ctgs = $phr->get_minicategs_snippets; #категоризация по intent
        next if @ctgs && join("/", sort @ctgs) ne $ctgs;
        $cnt++ if @ctgs;
    }

    my ($brand, $model) = $bnr->parse; #массив: (brand, model)
    my $br_mod;
    if ($brand && $model) {
        $br_mod = "$brand $model";
    } elsif ($brand) {
        $br_mod = $brand;
    } elsif ($model) {
        $br_mod = $model;
    } else {
        $br_mod = "";
    }
    $phr = $proj->phrase($br_mod);
    if ($phr) {
        @ctgs = $phr->get_minicategs_snippets; #категоризация по brand+model
        next if @ctgs && join("/", sort @ctgs) ne $ctgs;
        $cnt++ if @ctgs;
    }


    @ctgs = $bnr->get_categs_neighbors;
    next if @ctgs && join("/", sort @ctgs) ne $ctgs;
    $cnt++ if @ctgs;

    print "$bid\t$title\t$body\t$ctgs\t$cnt\t$num\n";
}
