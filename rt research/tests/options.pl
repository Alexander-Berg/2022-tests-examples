#!/usr/bin/perl -w
use strict;

use utf8;
use open ":utf8";

use Getopt::Long;
use JSON;

use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;

use Utils::Common;


my %opt;
GetOptions(\%opt, 'help|h', 'pretty');
if ($opt{help}) {
    printf "Usage: options.pl [Options] opt_name\n";
    printf "Print value for \$Utils::Common::options->{opt_name}.\n";
    printf "Options:\n";
    printf "  --pretty  pprint for JSON\n";
    exit(0);
}

for my $opt (@ARGV) {
    my $val = $Utils::Common::options->{$opt};
    my $val_str;
    if (!defined $val) {
        $val_str = "undefined!";
    } elsif (!ref($val)) {
        $val_str = $val;
    } else {
        my %par;
        $par{pretty} = 1 if $opt{pretty};
        $val_str = to_json($val, \%par);
    }
    print "Option $opt => $val_str\n";
}
