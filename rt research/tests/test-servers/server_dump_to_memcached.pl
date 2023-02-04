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
use Utils::Sys qw/md5int/;

select STDERR; $| = 1;
select STDOUT; $| = 1;

print "start...\n";


my $proj = Project->new({});
my $clh = $proj->memclient();

my @lines = ();
my $total = 0;
while (<>) {
	chomp;
	my $string = $_;
	my ( $phrase, $values ) = split /\t/, $string;
	my $res = {};
	for my $value ( split /\,/,$values ) {
		next if !$value;
		my ( $type, $time, $val ) = split /\:/, $value;
		$res->{$type} = $val if $type;
	}
	push @lines, [ md5int($phrase), $res ];
	if ( scalar(@lines) >= 1_000 ) {
		$clh->set_multi( @lines );
		@lines = ();
	}
	$total++;
	print "[" . localtime() . "] done $total lines\n" if $total % 1_000_000 == 0
}

if ( @lines ) {
	$clh->set_multi( @lines );
	@lines = ();
}

print "finish...\n";

__END__

print "time:$total_time.\n";

exit(0);

1;

