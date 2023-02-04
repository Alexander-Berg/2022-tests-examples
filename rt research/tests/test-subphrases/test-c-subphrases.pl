#!/usr/bin/perl -w
use strict;
use utf8;

use FindBin;
use lib "$FindBin::Bin/../../lib";
use Utils::Common;
use Project;
use Data::Dumper;
use Time::HiRes qw/gettimeofday tv_interval/;
use Utils::Words;

select STDERR; $| = 1;
select STDOUT; $| = 1;

print "start...\n";

my $proj = Project->new({
        load_dicts   => 0,
        load_minicategs_light => 0,
});

open (fF,"<test_file_1000") or $proj->log("ERROR: cannot open file, $!") and exit(0);
while (<fF>) {
    chomp;
    my ( $query, @dummy ) = split /\t/;
    my @subq = Utils::Words::lmr->get_subphrases($query);
    for ( my $index = 0; $index < $#subq; $index++ ) {
        print join("\t", $subq[$index], $dummy[0], $dummy[2], 0 ) . "\n";
    }
    print join("\t", $query, $dummy[0], $dummy[2], $dummy[2] ) . "\n";
}
close fF;
