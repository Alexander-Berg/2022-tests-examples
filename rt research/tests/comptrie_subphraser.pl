#!/usr/bin/perl -w
use strict;

use utf8;
use open ':utf8';

use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';

my $proj = Project->new;
my $proj_old = Project->new({load_dicts=>1,load_minicategs_light=>1});
my $proj_new = Project->new({load_dicts=>1,load_minicategs_light=>1,use_comptrie_subphraser=>1});

my $tm = $proj->get_new_timer;
my $cnt = 200;
my $diff = 0;
my $bid_str = $proj->random_banners_client->k_random_banners($cnt, {active_flag=>[1]}); 
my @bids = split /,/, $bid_str;

for my $bid (@bids) {
    next if !$proj->bid2banner($bid);
    my @old = $proj_old->bid2banner($bid)->get_minicategs;
    my @new = $proj_new->bid2banner($bid)->get_minicategs;

    my %old = map { $_ => 1 } @old;
    my %new = map { $_ => 1 } @new;

    my @gone = grep { !$new{$_} } @old;
    my @came = grep { !$old{$_} } @new;

    if (!@gone and !@came) {
        print "$bid: OK\n\n";
    } else {
        $diff++;
        print "$bid: diff:\n";
        print "< ".join('/', @gone), "\n";
        print "> ".join('/', @came), "\n";
        print "\n";
    }
}

printf "Diff: %d of %d (%.1f %%)\n", $diff, $cnt, 100 * $diff/$cnt;
