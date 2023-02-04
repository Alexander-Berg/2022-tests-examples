#!/usr/bin/perl -w

use strict;

use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use IO::Socket;
use IO::Handle;
use Time::HiRes qw(gettimeofday tv_interval);

use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;

my $proj = Project->new({ load_dicts => 1 });
$proj->log("ready!");

while(defined(my $text = STDIN->getline)) {
    chomp $text;

    my $phr = $proj->phrase($text);

    my $start = [gettimeofday];
    my @ids = $proj->banners_bender->find_ids($phr, 10000);
    my $finish = [gettimeofday];

    print scalar(@ids)." banners for ".tv_interval($start, $finish)."\n";
    #print join(",", @ids)."\n";
}
