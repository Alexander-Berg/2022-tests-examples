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
    my $proj = Project->new({load_dicts => 0});
    $proj->log("started");
    $proj->do_sys_cmd_bash(q[ perl -e "for (\$f=0;\$f<10;\$f++) {print \$f.qq[\n]}" | head -n30 | LC_ALL=C sort -S2G -t$'\t' -r -n  | awk -F/7/ {'print $1'}]);
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
