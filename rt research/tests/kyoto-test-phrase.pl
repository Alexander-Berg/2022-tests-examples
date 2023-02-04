#!/usr/bin/perl -w

use strict;
use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use Time::HiRes qw/gettimeofday tv_interval/;
use Data::Dumper;

use lib '/opt/broadmatching/scripts/lib';
use Project;
my $proj = Project->new({load_dicts=>1});

my $phrase = $proj->phrase("zopa2");

$proj->log("\n");
$proj->log("====start");
$proj->log( $phrase->test_kyoto_cache_100() );
#$proj->log( $phrase->test_kyoto_cache_1000() );
$proj->log("====finish");
exit (0);
