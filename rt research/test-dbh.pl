#!/usr/bin/perl -w
use strict;
use utf8;

use JSON qw(to_json);
use Getopt::Long;

use FindBin;
use lib "$FindBin::Bin/../lib";
use Utils::Common;
use Project;
use Data::Dumper;
use Time::HiRes qw/gettimeofday tv_interval/;
use Utils::Sys qw( handle_errors );

handle_errors();

my %opt;
GetOptions(\%opt, 'help|h', 'cmd');
if ($opt{help}) {
    printf "Options:\n";
    printf "  --cmd  print command to connect to server\n";
}
my %par;
$par{cmd} = 1 if $opt{cmd};


my $proj = Project->new;

my @dbh_names = @ARGV or die "Defined dbh list!";

my $result = $proj->dbh_test( \@dbh_names, %par);

$proj->log("result: $result");

exit ($result ? 0 : 1);
