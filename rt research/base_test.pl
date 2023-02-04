#!/usr/bin/perl -w

use strict;

use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use Time::HiRes qw(gettimeofday tv_interval);
use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;
use Data::Dumper;

my $proj = Project->new({ 
    load_dicts => 1, 
    load_minicategs_light => 1,
    load_languages => [qw(ru tr)],
});

my $phl = $proj->phrase_list([
    $proj->phrase("купить холодильник"), 
    $proj->phrase("окна пвх"), 
    $proj->phrase("her"), 
    $proj->phrase("москва спб"),
    $proj->phrase("iphone"),
    $proj->get_language("tr")->phrase("bir buzdolabı satın"),
    $proj->get_language("tr")->phrase("iphone"),
]);
$phl->cache_cdict_tail_categs;
$phl->cache_cdict_categs_atoms;
$phl->cache_cdict_regions_phrases;
$phl->cache_is_good_phrase;

for my $phr(@$phl) {
    print "*** TEXT: '$phr' LANG: '".$phr->lang."' ***\n";
    print "  get_search_count: " . $phr->get_search_count . "\n";
    print "  cdict_minicategs: " . join("/", @{$phr->{cdict_minicategs} || []}) . "\n";
    print "  tail2count: " . join(",", keys %{$phr->{tail2count}}) . "\n";
    print "  get_regions: " . Dumper([$phr->get_regions]);
    print "  get_search_syns: " . join(",", map{$_->text} @{$phr->get_search_syns}) . "\n";
    print "  cdict_is_good_phrase: " . $phr->{cdict_is_good_phrase} . "\n";
}

