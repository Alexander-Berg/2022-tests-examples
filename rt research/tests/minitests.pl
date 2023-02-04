#!/usr/bin/perl -w
use strict;

use utf8;
use open ':utf8';

use List::Util qw(sum);
use Getopt::Long;
use JSON qw(to_json from_json);
use Data::Dumper;

use FindBin;
use lib "$FindBin::Bin/../lib";
use Utils::Common;
use Utils::Words;
use Utils::Sys qw[ handle_errors ];
use Utils::Funcs qw(ratio_str);
use Utils::Hosts;
use Project;
use ObjLib::Timer;

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';
binmode STDERR, ':utf8';

select STDERR; $|++;
select STDOUT; $|++;


handle_errors();

if (!@ARGV or (@ARGV == 1 and $ARGV[0] eq '--help')) {
    printf "Usage: minitests.pl test_name [args]\n";
    printf "Run sub test_name with given args\n";
    exit(0);
}

# common data
my $timer = ObjLib::Timer->new;
my $data_dir = $Utils::Common::options->{dirs}{scripts}.'/tests/data';


my $func_name = shift @ARGV;
my $func_ref = $main::{$func_name}
    or die "Function `$func_name' not defined!\n";
print "Calling sub `$func_name' ...\n";
$func_ref->(@ARGV);


sub csyns {
    my %opt = (
        name => 'precise',
        data => 'phrases',
    );
    GetOptions(\%opt, 'help', 'name=s', 'data=s', 'max_count=i', 'timelog');
    if ($opt{help}) {
        printf "Options for csyns test:\n";
        printf "  --name=N         csyns dict name (default: precise)\n";
        printf "  --data=D         data type: 'phrases' (default) or 'banners'\n";
        printf "  --max_count=C    read C lines from source\n";
        printf "  --timelog        turn on time logging\n";
        exit(0);
    }

    my $syn_name = $opt{name};
    printf "Testing csyns dict `%s' ...\n", $syn_name;

    my $proj = Project->new({
        load_dicts => 1,
        load_minicategs_light => 1,
        ($opt{timelog} ? (timelogpackages => [ _get_timelogpackages() ]) : ()),
    });
    $timer->time('load');
    my $mem_start = Utils::Sys::mem_usage();
    my $csyn = $proj->context_syns->{$syn_name};
    $csyn->_load;
    my $dict_mem = Utils::Sys::mem_usage() - $mem_start;

    $timer->time('get_data');
    $proj->log("get data from $opt{data} ...");
    my @src_data;
    if ($opt{data} eq 'phrases') {
        $opt{max_count} //= 10_000;
        open my $fh, "zcat $data_dir/phrases-100k.gz |";
        while (<$fh>) {
            chomp;
            my $phr = $proj->phrase($_);
            my $phl = $proj->phrase_list([$phr]);
            push @src_data, [ undef, $phl ];
            last if $. > $opt{max_count};
        }
        close $fh;
    } else {
        $opt{max_count} //= 1_000;
        open my $fh, "zcat $data_dir/banners-100k.gz |";
        while (<$fh>) {
            my $bnr = $proj->text2banner($_);
            push @src_data, [ $bnr->exminicategshash, $bnr->phl ];
            last if $. > $opt{max_count};
        }
        close $fh;
    }

    $timer->time('match');
    my ($matched, $total, $new_phrs) = (0, 0, 0);
    for my $d (@src_data) {
        my ($context, $phl) = @$d;
        my $ext = $csyn->extend_phl($phl, $context);
        $total++;
        if ($ext->count) {
            printf "match %d (seen: %d): %s => %s\n", ++$matched, $total, "$phl", "$ext";
            $new_phrs += $ext->count;
        }
    }

    printf "=====\n";
    printf "mem_usage: %d Mb\n", int($dict_mem / 2**20);
    printf "load time: %.1f sec\n", $timer->report->{load};
    printf "matched: %d phls of %d (%.2f %%), new phrases: %d\n", $matched, $total, 100 * $matched/$total, $new_phrs;
    my $time = $timer->report->{match};
    printf "extend time: %.1f sec (rps: %d)\n", $time, int($total / $time);

    if ($opt{timelog}) {
        print $proj->timelogreport;
    }

    #print Dumper(\%BM::ContextSyns::ContextLinks::time);
    #print Dumper(\%BM::ContextSyns::ContextLinks::count);
}


# template
sub rand_banners {
    my $proj = Project->new({load_dicts=>1,load_minicategs_light=>0,projsrv=>0});
    my %opt;
    GetOptions(\%opt, 'help', 'count=i', 'bids=s');
    if ($opt{help}) {
        printf "Options:\n";
        printf "  --count N    test N active banners (default: 10)\n";
        printf "  --bids L     comma-separated list of bids instead of random\n";
        return;
    }
    my $cnt = $opt{count} // 10;
    print "\n";

    my $tm = $proj->get_new_timer;
    $tm->time('random_banners');
    my $bid_str;
    my $bnl;
    if ($opt{bids}) {
        $bid_str = $opt{bids};
        my @bids = split /,/, $bid_str;
        $bnl = $proj->bf->ids2bnl(\@bids);
    } else {
        $bnl = $proj->random_banners_client->k_random_bnl($cnt, {active_flag=>[1]}); 
    }

    for my $bnr (@$bnl) {
        next if !$bnr;
        next if !$bnr->phl->count;
        for my $phr ($bnr->phl->phrases) {
            my $q1 = sum(0, map { $_->get_search_query_count } $phr->get_matched_queries->phrases);
            my $q2 = sum(0, map { $_->get_search_query_count } $phr->get_matched_queries(max_phrases => 20)->phrases);
            my $q3 = sum(0, map { $_->get_search_query_count } $phr->get_matched_queries(max_phrases => 10)->phrases);
            print join("\t", $phr->get_search_count, $q1, $q2, $q3), "\n";
        }
    }
}

sub rand_phrases {
    my %opt;
    GetOptions(\%opt, 'help', 'max_count=i');
    if ($opt{help}) {
        printf "Options for rand_phrases test:\n";
        printf "  --max_count=C    read C random phrases (default: read all)\n";
        exit(0);
    }

    my $proj = Project->new({load_dicts => 1});
    my $open_str = "zcat $data_dir/phrases-100k.gz |";
    if (defined $opt{max_count}) {
        $open_str .= "shuf -n $opt{max_count} |";
    }
    open(my $fh, $open_str);
    while (<$fh>) {
        chomp;
        my $phr = $proj->phrase($_);
        my $phl = $proj->phrase_list([$phr]);
        next unless $phr->look_like_a_model;
        my $phl1 = $phr->get_modellike_modifications;
        my $phl2 = $phr->get_modellike_modifications_lite;
        my %old = map { $_->norm_phr => 1 } @$phl1;
        my %new = map { $_->norm_phr => 1 } @$phl2;
        printf "phrase: %s\n", $phr->text;
        printf "common: %s\n", join(', ', grep { $new{$_->norm_phr} } @$phl1);
        printf "gone: %s\n", join(', ', grep { !$new{$_->norm_phr} } @$phl1);
        printf "came: %s\n", join(', ', grep { !$old{$_->norm_phr} } @$phl2);
        printf "\n";
    }
    close $fh;
}

sub hosts {
    my %par;

    $par{role} = 'catmedia-dev';

    print 'params: ', to_json(\%par), "\n";
    print "hosts:\n";
    for my $host (Utils::Hosts::get_hosts(%par)) {
        print "  ", $host, " => ", to_json(Utils::Hosts::get_host_info($host)), "\n";
    }
}

sub clickhouse {
    my $proj = Project->new({projsrv => 1});
}

sub _get_timelogpackages {
    return qw(
        BM::ContextSyns::ContextLinks
        );
    return qw(
        Utils::SubsetIndex
        BM::PhraseParser BM::PhraseCategs BM::PhraseModif BM::Phrase BM::PhraseList
        BM::ContextSyns::ContextLinks
        BM::Banners::LBannerDirect BM::Banners::LBannerBM BM::Banners::LBannerAnalysis
        BM::Matching::BannerMatcherFilters BM::Matching::BannerMatcherSources
    );
}

sub bmtasks {
    my $proj = Project->new;
    my $bmt = $proj->bmtasks;

    for my $h (
        ['task', $bmt->task_dbt],
        ['state', $bmt->states->dbt],
        ['state_log', $bmt->states->log_dbt],
    ) {
        my ($type, $dbt) = @$h;
        print "$type: ", $dbt->db_table, "\n";
        print "connect: ", to_json($bmt->task_dbt->dbh->get_connect_params), "\n\n";
    }
}
