#!/usr/bin/perl -w
use strict;
use utf8;

use Getopt::Long;
use FindBin;
use lib "$FindBin::Bin/../lib";
use Utils::Common;
use Project;
use Data::Dumper;
use Time::HiRes qw/gettimeofday tv_interval/;

my %opt;
GetOptions(\%opt, 'verbose');

select STDERR; $| = 1;
select STDOUT; $| = 1;

binmode STDOUT, ':utf8';

my $proj = Project->new({
    load_dicts   => 0,
});

my @phrases = (
    'валенки опт',
    'заказ цветок',
    'такси москва',
    'такси петербург',
    'samsung n500',
    'sgh самсунг',
    'samsung 70',
    'samsung москва',
    'новый год',
    'продажа квартир',
    'холодильник',
    'телевизор',
    'пылесос',
);

my $advq = $proj->advqraw;
$advq->get_cache(0);
$advq->set_cache(0);
$advq->verbose(1) if $opt{verbose};
print "Using advq hosts: ".join(', ', @{$advq->{hosts}})."\n";

my $start = [ gettimeofday ];
for my $phrase ( @phrases ) {
    printf "%s [%d]\n", $phrase, $advq->get_count($phrase);
}
my $intv = tv_interval($start);
print "======\n";
printf "time: %.2f (rps: %d)\n", $intv, int(@phrases/$intv); 

exit(0);
