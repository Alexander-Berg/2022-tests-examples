#!/usr/bin/perl -w
#ТЕСТ: категоризация баннеров с помощью landing page и ближайших соседей

use strict;
use utf8;
use open ':utf8';
use Data::Dumper;

no warnings 'utf8';

binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

#use FindBin;
#use lib "$FindBin::Bin/../lib";
#use lib "/home/yuryz/arcadia/rt-research/broadmatching/scripts/wlib";
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

=z
use CatalogiaMediaProject;
my $proj = CatalogiaMediaProject->new({
    indcmd => 1,
    load_dicts => 1,
    load_minicategs_light => 1,
});
=cut

#отключение кэша
#$proj->categs_tree->never_read_categs_cache(1);
#$proj->categs_tree->never_write_categs_cache(1);

my $FLAG = 1;

if ($FLAG == 0) { # тестиование баннеров из файла
    my $count = 0;
    while (<STDIN>) { #/home/yuryz/scripts/data/uncat_bnrs_1kk.active
        chomp;
        my $bnr = $proj->bf->text2banner($_);
        #print $proj->dump_lite($bnr);
        categ($bnr); #категоризация баннера
        last if ++$count == 25;
    }
} else { # тестирование единичного баннера по заданному ID
    my $bid = '732200963';
    my $bnr = $proj->bf->get_banner_by_id($bid);
    categ($bnr); #категоризация баннера
}


# --- категоризация баннера ---
sub categ {
    my ($bnr) = @_;
    my $bnr_info = join("\t", $bnr->id, $bnr->banner_text_phrase->text, $bnr->url);
    my @bnr_categs = $bnr->get_categs_neighbors;
    print "$bnr_info\t", join("\t", sort @bnr_categs), "\n";
}
