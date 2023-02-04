#!/usr/bin/perl -w
use strict;
use FindBin;
use lib '/opt/broadmatching/scripts/lib';
use Project;
use Data::Dumper;
use BM::Filter;

# utf8
use utf8;
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");
no warnings 'utf8';
# /utf8

my $proj = Project->new({load_dicts => 1, load_minicategs_light => 1});

my @phrases = map { $proj->phrase($_) } 
(
"купить валенки в москве и санкт-петербурге",
"продать телевизор в Нижнем Новгороде"
);

$proj->log("started");
for my $phrase ( @phrases ) {
    $proj->log("phrase:" . $phrase->{text} . ", geobase regions:[" . join(',', $phrase->get_geobase_region_ids()) . "]");
}
$proj->log("finished");
