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

print "start...\n";

my $log_dir = $Utils::Common::options->{dirs}{'log'};
my $proj = Project->new({
        load_dicts   => 1,
});

my $clh = $proj->commonclient();

my $stime = [gettimeofday];

my @phrases = (
	'валенки опт',
	'заказ цветок112',
	'такси москва',
	'такси петербург112',
);

my $phraselist = $proj->phrase_list({
	phrases_arr => \@phrases,	
});

$phraselist->get_phrases_advqcount();
for my $phrase ( $phraselist->phrases() ) {
	print "phrase:".$phrase->norm_phr().",advqcount:" . $phrase->advqcount() . ".\n";
}

exit(0);

1;

