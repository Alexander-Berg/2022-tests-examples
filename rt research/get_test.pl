#!/usr/bin/perl -w
#выбор данных для тестирования

use strict;
use utf8;
use open ':utf8';
no warnings 'utf8';

binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

=z
open F, "ksu_bids_public";
my %h;
while (<F>) {
    chomp;
    my ($mctgs, $bid) = split /\t/;
    $h{$bid} = $mctgs;
}
=cut

open F, "ksu_bids_private";
my %h;
while (<F>) {
    chomp;
    my ($bid) = split /\t/;
    $h{$bid} = 1;
}

open F, "dataset_sync";
my %d;
while (<F>) {
    chomp;
    my ($bid, $domain, $domain_bad) = split /\t/;
    $d{$bid} = $domain;
}

while (<STDIN>) { #raw_dataset.dat
    chomp;
    my @f = split /\t/;
    if ($h{$f[0]}) {
        my $bid = $f[0];
        my $title = $f[2];
        my $body = $f[4];
        my $mctgs_tag = $f[9];
        my $bnr_domain = $d{$f[0]} ? $d{$f[0]} : $f[6]; #корректировка домена
        my $mctgs = $f[9];
        print "$bid\t$title\t$body\t$mctgs_tag\t$bnr_domain\t$mctgs\n";
    }
}
