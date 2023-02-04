#!/usr/bin/perl -w
use strict;

use utf8;
use open ":utf8";

use JSON qw(to_json);
use Getopt::Long;

use FindBin;
use lib "$FindBin::Bin/../lib";
use Utils::Hosts;

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';

my %opt;
GetOptions(\%opt, 'help|h', 'host=s');
if ($opt{help}) {
    printf "Usage: $0 [Options]\n";
    printf "Print host info\n";
    printf "Options:\n";
    printf "  --host=H   specify host (fqdn)\n";
    exit(0);
}

print "Host info:\n";
my $host = $opt{host} // Utils::Hosts::get_curr_host();
my $info = Utils::Hosts::get_host_info($host);
print to_json($info, { pretty => 1 }), "\n";
