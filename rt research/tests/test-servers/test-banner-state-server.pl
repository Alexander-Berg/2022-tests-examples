#!/usr/bin/perl -w
use strict;
use utf8;

# тест баннер-сервера

# запускается из под крона
# после выполнения - в нужных местах (см. Utils::Common) лежат симпграфы

use FindBin;
use lib "$FindBin::Bin/../../lib";
use Utils::Common;
use Project;
use Data::Dumper;
use Time::HiRes qw/gettimeofday tv_interval/;

select STDERR; $| = 1;
select STDOUT; $| = 1;

print "start...\n";
my $tstart = [gettimeofday];

my $proj = Project->new({
        load_dicts   => 0,
});

my $stime = [gettimeofday];

open (fF,"</opt/broadmatching/work/BannersExtended/banners_extended") || die $!;
my $bcount = 0;
while (<fF>) {
	chomp;
	my $new_state = time . "good";
	my $bnr = $proj->banner_factory->matchtext2banner($_); 
	print "setting state($new_state) for banner(" . $bnr->id() . ")\n";
	print "current_state(" . $bnr->get_state() . ")\n";
	print "setting new state...\n";
	$bnr->set_state($new_state);
	print "state set\n";
	print "now state(" . $bnr->get_state() . ")\n";
	last;
}

print "script finished in " . tv_interval( $tstart ) . "\n";

exit(0);

1;

