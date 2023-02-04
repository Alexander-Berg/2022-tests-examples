#!/usr/bin/perl

use strict;
use utf8;
use v5.10;

use Data::Dumper;
use POSIX qw/strftime/;
use FindBin;

use lib "$FindBin::Bin/../../lib";
use Project;
use Utils::Sys qw(md5int);

# init kyoto client
my $proj = Project->new;
my $broad_kyoto = $proj->broad_kyoto();

if ($ENV{I_FULLY_UNDERSTAND_WHAT_I_AM_DOING}) {
    kill_cache();
}

# aux
sub gen_medium_string {
    my $base = md5int(rand);
    my $retval = "";
    for my $i (1..500_000) {
    #    my $base = md5int(rand);
        $retval .= $base . $i;
    }
    return $retval;
    #return md5int(rand) x 500_000;
    #return join('', md5int(rand)) for 1..500_000;
}

sub try_cache_big_values {
    # md5int string is ~ 20 bytes
    say "generate big value";
    my $medium_string = gen_medium_string();
    my $big_string = $medium_string x 10;

    for my $string (($medium_string, $big_string)) {
        say "number of bytes:";
        say do { use bytes; length $string};

        my $key = 'key_for_big_value'.int(1000 * rand);

        my $set_result = $broad_kyoto->set($key, $string, 60);
        say "set result: $set_result";

        my $get_result = $broad_kyoto->get($key);
        say "get result: " . int($get_result eq $string);
    }
}

sub kill_cache {
    # hard load testing -- use with care!
    say "YOU ARE GOING TO KILL BROAD KYOTO CACHE!";
    if (prompt_yn('Sure?')) {
        my $N_KEYS = 300_000;
        my @killer_keys = map {"killer key $_"} 1 .. $N_KEYS;
        say "cleanup";
        $broad_kyoto->delete_multi(@killer_keys);
        $proj->log("load $N_KEYS keys of 10 MB each to broad_kyoto");
        my %kv;
        for my $killer_key (@killer_keys) {
            my $i = pop [split /\s+/, $killer_key];
            $proj->log("key $i / $N_KEYS") unless $i % 100;
            my $val = gen_medium_string();
            if ($i == 1 || $i == $N_KEYS) {
                $kv{$killer_key} = $val;
            }
            $broad_kyoto->set($killer_key, $val, 14*24*60*60);
        }

        say "try to get first and last";
        for my $k (keys %kv) {
            say $k;
            my $get_value = $broad_kyoto->get($k);
            say "is get value defined: " . int(defined $get_value);
            say "is get value correct: " . int($get_value eq $kv{$k});
        }

        # cleanup
        say "cleanup";
        $broad_kyoto->delete_multi(@killer_keys);
    }
}

sub prompt {
    my ($query) = @_; # take a prompt string as argument
    local $| = 1; # activate autoflush to immediately show the prompt
    print $query;
    chomp(my $answer = <STDIN>);
    return $answer;
}
sub prompt_yn {
    my ($query) = @_;
    my $answer = prompt("$query (Y/N): ");
    return lc($answer) eq 'y';
}
