#!/usr/bin/perl
use strict;
use utf8;
use v5.10;

use FindBin;
use lib "$FindBin::Bin/../../lib";
use Project;

my $proj = Project->new;
my $yt = $proj->yt_client;

$yt->do_map(
    "//home/direct/export/bm/bm_banners",
    "//home/broadmatching/tmp/serkh/YandexBanners",
    sub {
        my $rec = shift;
        my @out;
        if ($rec->{body} =~ /Яндекс/){
            push @out, {map {$_ => $rec->{$_}} qw(bid title body)};
        }
        return \@out;
    }
)
