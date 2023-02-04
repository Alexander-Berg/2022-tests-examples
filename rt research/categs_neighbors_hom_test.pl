#!/usr/bin/perl -w
#ТЕСТ: снятие омонимии баннеров

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
use Project;
use BM::Phrase;
use BM::PhraseList;
use Time::HiRes qw(tv_interval gettimeofday);

my $proj = Project->new({ 
    load_dicts => 1,
    load_minicategs_light => 1,
});


my $FLAG = 0;

if ($FLAG == 0) { # тестиование баннеров из файла
    while (<STDIN>) { #/home/yuryz/scripts/data/bnrs_1kk_good
        chomp;
        my $bnr = $proj->bf->text2banner($_);
        my @bnr_categs = split("/", $bnr->{mctgs});
        disamb($bnr, \@bnr_categs); #снятие омонимии баннера
    }
} else { # тестирование единичного баннера по заданному ID
    my $bid = '1000000015';
    my $bnr = $proj->bf->get_banner_by_id($bid);
    my @bnr_categs = $proj->phrase($bnr->banner_text_phrase->text)->get_minicategs;
    disamb($bnr, \@bnr_categs); #снятие омонимии баннера
}


# --- снятие омонимии баннера ---
sub disamb {
    my ($bnr, $bnr_categs) = @_;

    my $bnr_info = join("\t", $bnr->id, $bnr->banner_text_phrase->text, $bnr->url);
    my $best_categ = $$bnr_categs[0];

    if (@$bnr_categs > 1) { #омонимия
print "$bnr_info\t", join("/", @$bnr_categs), "\n";
        my $campaign = $bnr->campaign_obj; #кампания
        #print "CID=", $campaign->campaign_id, "\n";
        my $campaign_bnl = $campaign->bnl; #баннеры кампании

        my %bctgs; #частотный словарь категорий кампании
        for my $ban (@$campaign_bnl) {
        #for my $ban (sort { $a->id cmp $b->id } @$campaign_bnl) {
            my @bctgs = $proj->phrase($ban->banner_text_phrase->text)->get_minicategs;
            #print "*", $ban->id, "\t", join("\t", sort @bctgs), "\n";
            for my $bctg (@bctgs) {
                $bctgs{$bctg}++; #частота категории
            }
        }

        #for (sort { $bctgs{$b} <=> $bctgs{$a} } keys %bctgs) {
        #    print "$_\t$bctgs{$_}\n";
        #}

        for my $i (1..$#{$bnr_categs}) {
            $best_categ = $$bnr_categs[$i] if $bctgs{$best_categ} && $bctgs{$$bnr_categs[$i]} && $bctgs{$best_categ} < $bctgs{$$bnr_categs[$i]};
        }
print "$bnr_info\t$best_categ\n--\n";
    }

    return $best_categ;
}
