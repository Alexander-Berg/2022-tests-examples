#!/usr/bin/perl -w
use strict;
use utf8;

# Построение симп-графов

# запускается из под крона
# после выполнения - в нужных местах (см. Utils::Common) лежат симпграфы

use FindBin;
use lib "$FindBin::Bin/../lib";
use Utils::Common;
use Project;
use Data::Dumper;
use Time::HiRes qw/gettimeofday tv_interval/;

select STDERR; $| = 1;
select STDOUT; $| = 1;

print "[" . localtime() . "] started\n";
my $script_start = [gettimeofday];

my $log_dir = $Utils::Common::options->{dirs}{'log'};
my $proj = Project->new({load_dicts   => 0});

my $assigner = $proj->bmclient;
my $total_queries = 100;
my $stime = [gettimeofday];

print Dumper($assigner->AssignCampaignStat([999812])).".\n";

print "finished in " . tv_interval($script_start) . " sec\n";

exit(0);
