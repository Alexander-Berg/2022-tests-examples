#!/usr/bin/perl -w

use strict;

use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;

my $proj = Project->new({
    load_dicts => 1,
    load_minicategs => 1,
    load_languages => ["tr", "en"]
});

for my $lang ($proj->get_languages_list) {
    my $layer = $lang->layer_categs;

    for my $text ($layer->get_phrases) {
        my $data = $layer->get_text_data($text);
        my %categs = map{$_=>1} split "/", $data;

        if(keys(%categs) > 1) {
            $proj->log("WARNING: duplicate ".$lang->name." phrases '$text' in '".join("/", sort map{$lang->category_from_ru($_)} keys %categs)."'");
        }
    }
}

