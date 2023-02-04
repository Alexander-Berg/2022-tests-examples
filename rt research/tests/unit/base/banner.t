#!/usr/bin/env perl
use strict;
use warnings;

use utf8;
use open ':utf8';

use Test::More;

use FindBin;
use lib "$FindBin::Bin/../../../lib";
use BaseProject;

my $proj = BaseProject->new({
    load_dicts                              => 1,
    load_minicategs_light                   => 1,
    use_comptrie_subphraser                 => 1,
    use_sandbox_categories_suppression_dict => 1,
});

my @sfw_banners = (
    { title => 'купить квартиру в Москве', body => 'лучшие квартиры только у нас', url => 'http://incom.ru/', 'купить квартиру' },
    [ 'купить айфон', 'лучшие айфоны только у нас! доставка по России', 'http://iphone.ru/','купить айфон' ],
);

my @nsfw_banners = (
    { title => 'купить проститутку в Москве', body => 'лучшие проститутки только у нас', url => 'https://putana.ru/'},
    { title => 'купить наркотики бесплатно без смс', body => 'лучшие наркотики только у нас', url => 'https://ololo.ru/'},
);


sub test_get_minicategs {
    for my $case (@sfw_banners, @nsfw_banners) {
        my $bnr = $proj->bf->lbanner($case);
        my @categs = $bnr->get_minicategs;
        like(join('/', @categs), qr/\w+/, 'get_minicategs');
    }
}

sub test_get_catalogia_flags {
    for my $case (@nsfw_banners) { 
        my $bnr = $proj->bf->lbanner($case);
        my @categs = $bnr->get_catalogia_flags;
        like(join('/', @categs), qr/\w+/, 'get_catalogia_flags');
    }
}

test_get_minicategs();
test_get_catalogia_flags();
done_testing();
