#!/usr/bin/perl -w
#добавление поля CategoryNames к hint_Test

use strict;
use utf8;
use open ':utf8';
no warnings 'utf8';
use Data::Dumper;

binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

my %h;
open F, "hint_dataset";
while (<F>) {
    chomp;
    my ($BannerID, $CategoryNames) = split /\t/;
    $h{$BannerID} = $CategoryNames;
}

my %c;
open F, "ztest_private_ctg_1";
while (<F>) {
    chomp;
    my ($BannerID, $stub1, $stub2, $stub3, $stub4, $CoreCategoryNames) = split /\t/;
    $c{$BannerID} = $CoreCategoryNames unless $c{$BannerID};
}

while (<STDIN>) { #hint_Test
    chomp;
    my ($Body, $Title, $AutoCategoryNames, $Domain, $BannerID) = split /\t/;
    print "$AutoCategoryNames\t",  $h{$BannerID}, "\t[", $c{$BannerID}, "]\t$BannerID\t$Title\t$Body\t$Domain\n";
}
