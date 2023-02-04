#!/usr/bin/perl -w
#генерация разлиных данных для тестирования schachtel.pl

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
use Data::Dumper;

my $proj = Project->new({ 
    load_dicts => 1,
    load_minicategs_light => 1,
});

while (<STDIN>) { #/home/yuryz/scripts/tatr/uncat_bnrs_tatr.src
    chomp;

    my $bnr = $proj->bf->text2banner($_);
    #my $bnr_info = join("\t", $bnr->id, $bnr->banner_text_phrase->text);

=exp1
    #--- EXP1 ---
    s/#//g;
    my @f = split /\t/;
    print "$f[3] $f[4]\n"; #exp1: banner = title + body
=cut

=exp2
    #--- EXP2 ---
    s/#//g;
    my @f = split /\t/;
    print "$f[3]\n"; #exp2: title
=cut

=exp3
    #--- EXP3 ---
    my @a = $bnr->parse;
    my $brand = $a[0] ? $a[0] : "";
    $brand .= $brand && $a[1] ? " $a[1]" : $a[1] ? $a[1] : "";
    print $brand ? $brand : ".", "\n";  #exp3: brand + model

    #my $h = $bnr->parsed_banner_text; # ***уступает а полноте $bnr->parse
=cut

#=exp4
    #--- EXP4 ---
    my $h = $bnr->get_intent;
    print "$$h{intent}\n"; #exp4: intent
#=cut
}
