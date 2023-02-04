#!/usr/bin/perl -w
use strict;

use utf8;
use open ':utf8';

use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;
use Utils::Sys qw(save_json load_json fork_tasks);

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';

my $proj = Project->new;
my $tm = $proj->get_new_timer;
my $cnt = 1000;
my $bid_str = $proj->random_banners_client->k_random_banners($cnt, {active_flag=>[1]}); 
my @bids = split /,/, $bid_str;
@bids = grep { $proj->bid2banner($_) } @bids;

my $tmp_old = $proj->get_tempfile('categs', UNLINK => 1);
my $tmp_new = $proj->get_tempfile('categs', UNLINK => 1);

fork_tasks([
    {
        name => 't1',
        func => (sub {
            my $proj_old = Project->new({load_dicts=>1,load_minicategs_light=>1});
            test($proj_old, \@bids, $tmp_old),
        }),
        max_mem => '4G',
        max_time => 3600,
    },
    {
        name => 't2',
        func => (sub {
            my $proj_new = Project->new({load_dicts=>1,load_minicategs_light=>1,use_comptrie_subphraser=>1});
            test($proj_new, \@bids, $tmp_new),
        }),
        max_mem => '4G',
        max_time => 3600,
    }
    ],
    { max_proc => 2, max_mem => '15G' },
);

my $diff = 0;
my $res_old = load_json($tmp_old);
my $res_new = load_json($tmp_new);
for my $bid (@bids) {
    my @old = @{$res_old->{$bid} // []};
    my @new = @{$res_new->{$bid} // []};
    
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


sub test {
    my $proj = shift;
    my $bids = shift;
    my $file = shift;

    my %ctg;
    for my $bid (@$bids) {
        $ctg{$bid} = [ $proj->bid2banner($bid)->get_minicategs ];
    }
    save_json(\%ctg, $file);
}
