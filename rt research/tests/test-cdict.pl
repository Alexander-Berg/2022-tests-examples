#!/usr/bin/perl -w

use strict;

use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");
use lib '../lib';

use POSIX ":sys_wait_h";

use Time::HiRes qw/gettimeofday tv_interval/;

my $tstart = [gettimeofday];

print STDERR "script started\n";

use Project;

my $timeout = 40;
my $pid = fork();

if ( $pid == 0 ) {
    # тестовый
    my $proj = Project->new({load_dicts => 1});
    $proj->log("sterted");
    my $phr = $proj->phrase("ремонт ваз 2114");
    my $count = $phr->get_search_count();
    if ( $count > 0 ) {
        print STDERR "TEST OK, count of (" . $phr->text() . ") is:" . $count . "\n";
    } else {
        print STDERR "TEST FAILED!\n";
    }
    exit(0);
} else {
    # основной процесс
    my $count = 0;
    my $kid;
    while ( $count++ < $timeout ) {
	$kid = waitpid(-1, WNOHANG);
	last if ( $kid == $pid );
	sleep(1);
    }
    if ( $kid != $pid ) {
	my $result = kill 9, $pid;
	print STDERR "TEST FAILED, ERROR: timeout(more than) . $timeout sec, result killing child:$result\n";
    }
}
print STDERR "script finished in " . tv_interval( $tstart ) . " sec\n";

exit(0);

1;

__END__
