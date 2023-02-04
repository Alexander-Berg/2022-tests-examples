#!/usr/bin/perl -w

use strict;

use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use Time::HiRes qw(gettimeofday tv_interval);
use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;
use Utils::Common;

for my $curr_process (1..5) {
    next if fork();

    my $proj = Project->new({ prjsrv => 1, nrmsrv => 1 });

    my $start = [gettimeofday];
    my $norm;
    for my $i (1..1000) {
        my $phr = $proj->phrase("куплю холодильники");
        $norm = $phr->norm_phr . "\n";
    }
    my $finish = [gettimeofday];
    my $dt = tv_interval($start, $finish);

    print "$norm $dt\n";
    exit(0);
}

while(wait() > -1) {}
