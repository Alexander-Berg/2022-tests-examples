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

my $stime = [gettimeofday];

my @phrases = (
	'валенки опт',
	'заказ цветок',
	'такси москва',
	'такси петербург',
	'wwwqqqsdafakjshdfjklaghsdflj',
	'wwwqqqsdafakjshdfjklaghsdfljasdfl;ashdfkljhaskljdfh',
	'wwwqqqsdafakjshdfjklaghsdfljasdfl;ashdfkljhaskljdfhasdf;jkahsdkl;fhlqwkjehrlkqjwhelrk',
);

my $tcount = 0;
for my $phrase ( @phrases ) {
	my $po = $proj->phrase($phrase);
	my $tstart = [gettimeofday];
	print "phrase:".$po->text().",advqcount:".$po->advqcount().".\n";
	$tcount += tv_interval( $tstart );
}

print "tcount:$tcount.\n";

exit(0);

1;

