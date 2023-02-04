#!/usr/bin/perl -w

use strict;
use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use Time::HiRes qw/gettimeofday tv_interval/;
use Data::Dumper;

my $sst = [gettimeofday];

use lib '/opt/broadmatching/scripts/lib';
use Project;
my $proj = Project->new();
my $set = 1;
my $get = int (!$set );
my $ktclient = $proj->ktclient();

my @pack = ();
open (fF,"</opt/broadmatching/work/BannersExtended/banners-recategorized") || die $!;
my $lines = 0;

my $length = 0;
$proj->log("file started, set = $set, get: $get\n");

check_alive() or die("ERROR: server dead");

# SET pack
if ( $set ) {
$proj->log("we are here!");
while (<fF>) {
    chomp;
    $lines++;
    my ( $bid, @values ) = split /\t/;
    push @pack, [ $bid, Storable::freeze( \@values ), 3600 * 24 * 10 ]; # 10 days of storage
    if ( scalar(@pack) >= 100_000 ) {
	while ( @pack ) {
            my $result = $ktclient->set_multi( splice( @pack, 0, 10000 ) );
            if ( !$result ) { die("cannot connect") };
        }
    }
    $proj->log("done $lines lines") if $lines % 100_000 == 0;
}
} else {
while (<fF>) {
    chomp;
    $lines++;
    my ( $bid ) = split /\t/;
    push @pack, $bid;
    if ( scalar(@pack) >= 10 ) {
        my $hash = $ktclient->get_multi( @pack );
        for my $key ( keys %$hash ) {
            print $key . ":::" . join('##', @{Storable::thaw( $hash->{$key} )}) . "\n\n\n";
        }
	@pack = ();
    }
    last if $lines > 100;
}

}
close fF;

$proj->log(" all done in " . tv_interval($sst)) . "\n";

exit(0);

sub check_alive {
    $ktclient->set('ALIVE', 'YES');
    return $ktclient->get('ALIVE') eq 'YES' ? 1 : 0;
}

1;

