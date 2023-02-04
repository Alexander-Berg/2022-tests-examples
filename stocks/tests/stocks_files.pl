#!/usr/bin/perl

use strict;
use warnings;
use File::Find;

use constant crit_second => 3000; #equal 50min = 3 fail checks
use constant warn_second => 2000; #equal 33min = 2 fail checks
my @PATH = qw(/opt/www/stocks/data/xml /opt/www/stocks/data/xmlhist /opt/www/stocks/data/export/stocks /opt/www/stocks/data/export/lg);

$0 =~ m~/([^/]+?)\.[^/]+$~;
my $ME   = $1;
my @err  = ();
my @warn = ();


find(\&push_arrays, @PATH);

sub push_arrays {
  return unless ( -f $File::Find::name );
  my $delta_time = time - (stat($File::Find::name))[9];
  if ( $delta_time > crit_second ) {
    push(@err, $File::Find::name);
    return;
  } elsif ( $delta_time > warn_second ) {
    push(@warn, $File::Find::name);
    return;
  }
}

if (scalar @err) {
  print "PASSIVE-CHECK:$ME;2;" . join(', ', @err) . "\n";
  exit 0;
} elsif (scalar @warn) {
  print "PASSIVE-CHECK:$ME;1;" . join(', ', @warn) . "\n";
  exit 0;
}
print "PASSIVE-CHECK:$ME;0;Ok\n";