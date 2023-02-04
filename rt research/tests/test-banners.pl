#!/usr/bin/perl -w

use strict;

use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");
use lib '../lib';

use Utils::Common;
use List::Util qw(first max maxstr min minstr reduce shuffle sum);
use Project;
use BM::Phrase;

my $proj = Project->new({load_dicts => 1, load_minicategs=>1});

$proj->categs_tree->never_read_categs_cache(1);
$proj->categs_tree->never_write_categs_cache(1);

my $data = {};
my $count = 0;
my $bad_count = 0;

open F, $Utils::Common::options->{dirs}{dicts}."/test_banners";
while(<F>) {
    chomp;
    my ($name, $value) = /(\w+):\s*(.*)/;
    
    if($name) {
        $data->{$name} = $value;
    } else {
        next if !%$data;

        my $errors = 0;
        my @lines;
        my $bnr = BM::Banners::Banner->new({
            proj    => $proj,
            id      => ($data->{id} || 0),
            title   => $data->{title},
            body    => $data->{body},
            phrases => $data->{phrases}
        });

        $proj->log("checking banner #".(++$count)."...");

        push(@lines, "title: ".$bnr->title);
        push(@lines, "body: ".$bnr->body);
        push(@lines, "categs: ".join("/", $bnr->get_minicategs));
        push(@lines, "exminicategs: ".join("/", sort keys %{$bnr->exminicategshash}));
        push(@lines, "cities: ".join(",", $bnr->citieswords));

        if($data->{flags}) {
            my %h = map{$_=>1} $bnr->get_catalogia_flags;
            my @not_found = grep{!$h{$_}} split ",", $data->{flags};

            if(@not_found) {
                push(@lines, "ERROR: missing flags ".join(",", @not_found));
                $errors++;
            } 
        }

        if($data->{good}) {
            for my $ph(map{$proj->phrase($_)} split ",", $data->{good}) {
                my $r = $bnr->phrsfilterreason($ph);

                if($r) {
                    if($r =~ /badmini/) {
                        my @cts = $ph->get_minicategs;
                        @cts = $ph->get_uncertain_minicategs if !@cts;
                        $r .= " ".join("/", @cts);
                    }
                    if($r =~ /badhom/) {
                        my @context_words;
                        for my $w (keys %{$bnr->bannernormwordshash}) {
                            push @context_words, "$w:$_" for $proj->homonyms->get_context_words($w, keys %{$bnr->bannerwordshash});
                        }
                        $r .= " ".join(",", sort @context_words)." / ".join(",", sort keys %{$bnr->homonymy_minuswordshash});
                    }
                    $r .= " ".join(",", sort keys %{$bnr->vendorshash})."/".join(",", sort $ph->vendors) if $r =~ /vendor/;
                    push(@lines, "ERROR: good phrase '".$ph->text."' is filtered ($r)");
                    $errors++;
                }
            }
        }

        if($data->{bad}) {
            for my $ph(map{$proj->phrase($_)} split ",", $data->{bad}) {
                my $r = $bnr->phrsfilterreason($ph);

                if(!$r) {
                    push (@lines, "ERROR: bad phrase '".$ph->text."' is not filtered (categs: ".join("/", $ph->get_minicategs).")");
                    $errors++;
                }
            }
        }

        if($errors) {
            $proj->log($_) for @lines;
        }
        $proj->log("done with $errors errors");

        $bad_count++ if $errors;
        $data = {};
    }
}
close F;

$proj->log("RESULTS: $count banners have been processed, $bad_count are bad");

