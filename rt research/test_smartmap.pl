#! /usr/bin/perl -w 

use strict;
use utf8;
use 5.010;
use Data::Dumper;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN, ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

$Data::Dumper::Useqq = 1;
{ 
    no warnings 'redefine';
    sub Data::Dumper::qquote {
        my $s = shift;
        return "'$s'";
    }
}
use FindBin;
use lib "$FindBin::Bin/../lib";
use lib "$FindBin::Bin/../cpan";
use lib "$FindBin::Bin/../wlib";

use CatalogiaMediaProject;
my $proj = CatalogiaMediaProject->new({ indcmd => 1, 
    load_dicts =>1,
    load_minicategs_light => 1,
    no_auth => 1,
    no_form => 1,
    nrmsrv => 0, 
    timelogpackages => [ qw[ 
        BM::PhraseNrmSrv BM::Phrase BM::PhraseList BM::PhraseListNrmSrv 
        BM::PhraseParser
        BM::Pages::Page BM::Banners::LBannerAnalysis
        BM::Pages::PageHierarchy
        BM::PhraseCategs
    ] ] 
});

my $url = 'http://www.bluefish.ru/feed-yandex/';

my $page = $proj->page( $url );
my $text = $page->tt;

my $str_source_to_canonical = '
        minicategs => "Автомобили"
        product_type => "Автомобили"
        vin => OfferID
        unique_id => OfferID
        currency => currencyId
        folder_id => model
        mark_id => vendor
        images => _FUNC_GET_FIRST_PICTURE_ picture
    ';

my $str_source_to_yabs = '
        price => price => current
        currency => text => currency_iso_code
        images => _FUNC_GET_FIRST_PICTURE_ image
        folder_id => attribute => model
        folder_id => attribute => market => Model
        mark_id => attribute => market => Vendor
    ';

my $fd = $proj->fdm->test_smartmap($text, $str_source_to_canonical,  $str_source_to_yabs, 'car' );

print $fd->tskv_view_debug;

