#!/usr/bin/perl -w
use strict;
use FindBin;
use lib "$FindBin::Bin/../lib";
use Data::Dumper;
use Time::HiRes qw/gettimeofday tv_interval/;


use Project;
use BM::Filter;

# utf8
use utf8;
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");
no warnings 'utf8';
# /utf8

my $proj = Project->new({load_dicts => 0});

my $filters_init_hash = { 
   44 => {  "price >=" => '20000', } 
};


# приготовим исходный массив
my $input = "";
open (fF,"<//tmp/testfeed") or die $!;
while ( <fF> ) {
    $input .= $_;
}
close fF;

my $start = [gettimeofday];
my $output = $proj->filters( $filters_init_hash )->filter_tskv_count( $input );

print $output . "\n";

$proj->log("done in " . tv_interval($start));

#print $output;


exit(0);
