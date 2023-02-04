#!/usr/bin/perl -w
#тестирование фраз баннера на предмет "подарков"

use strict;
use utf8;
use open ':utf8';
no warnings 'utf8';

binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

#use FindBin;
#use lib "$FindBin::Bin/../lib";
use lib "/home/yuryz/arcadia/rt-research/broadmatching/scripts/lib";

use Utils::Common;
#use Utils::Graph3 qw(patch convert_to_graph1 mvgraph cpgraph rmgraph newgraph);
#use List::Util qw(first max maxstr min minstr reduce shuffle sum);
use Project;
use BM::Phrase;
use BM::PhraseList;
use Time::HiRes qw(tv_interval gettimeofday);

my $proj = Project->new({
    load_dicts   => 1,
    load_minicategs_light => 1, 
});

while (<STDIN>) { #./data/bnrs_10kk.camp
    chomp;

    my @f = split /\t/;
    my $id = $f[0];
    my $camp = $f[1];
    my $title = $f[3];
    my $body = $f[4];
    my $url = $f[8];
    my $ctg = substr($f[20], 6); #mctgs=
    $ctg = "NO_CATEGS" unless $ctg;

    my $lang = $f[21];
    next unless $lang eq "lang=ru";

    my $text = "$title\t$body";
    next unless $text =~ /подаро?к/i;
    #next unless $title =~ /подаро?к/i;
    print "$text\t$ctg\n";

    my $title_prf = $proj->phrase($title)->get_banner_prefiltered_phrase->text;
    my $body_prf = $proj->phrase($body)->get_banner_prefiltered_phrase->text;
    print "PRF:$title_prf\t$body_prf\n";

    my $bnr = $proj->bf->text2banner($_);
    my $phrase_list = $bnr->phl; #список фраз баннера
    for my $phrase (@$phrase_list) {
        print "\t$phrase\n" if $phrase =~ /подаро?к/i;
    }
    print "\n";
}
