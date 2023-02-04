#!/usr/bin/perl -w

use strict;

#use FindBin;
#use lib "$FindBin::Bin/../../lib";
use POSIX qw[];


print "Content-type: text/html\n\n";

my $host = `hostname --fqdn`;
chomp $host;

my $time = POSIX::strftime("%Y-%m-%d %H:%M:%S", localtime);
print "time:($time) hostname:($host)\n";

#print STDERR "STDERR\n";

exit(0);

1;
