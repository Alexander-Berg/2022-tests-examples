#!/usr/bin/perl -w
use strict;
use FindBin;
use lib '/opt/broadmatching/scripts/lib';
use lib '/opt/broadmatching/scripts/wlib';
use lib '/opt/broadmatching/scripts/cpan';
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

use CatalogiaMediaProject;
my $proj = CatalogiaMediaProject->new({ indcmd => 1, 
    load_dicts =>1,
    load_minicategs_light => 1,
    no_auth => 1,
    no_form => 1,
    nrmsrv => 0, 
});


my $feed_data = <<'END_FEED'
categpath=/Женские	url=http://www.ozonsport.ru/product_3594.html	name=Велосипед Maxim MC STEEL 0.4.3 SUPERB (красный)
categpath=/Грузоблочные тренажеры	url=http://www.ozonsport.ru/product_4147.html	name=Верхняя-Нижняя тяга (2в1) MARBO SPORT MP-U211
categpath=/беговые дорожки	url=http://www.ozonsport.ru/product_2088.html	name=Беговая дорожка TRUE M30
categpath=/Педальные машинки	url=http://www.ozonsport.ru/product_1293.html	name=Детская педальная машина KETTLER КЕТТКАР BARCELONA AIR T01050-0010
categpath=/велотренажеры	url=http://www.ozonsport.ru/product_3934.html	name=Велотренажер AEROFIT 9900R
categpath=/Грифы и замки для штанги	url=http://www.ozonsport.ru/product_3684.html	name=Гриф тяжелоатлетический для соревнований GYM WAY OBMNB220
categpath=/беговые дорожки	url=http://www.ozonsport.ru/product_2580.html	name=Беговая дорожка DIADORA EDGE 1.6 DARK
categpath=/беговые дорожки	url=http://www.ozonsport.ru/product_2236.html	name=Беговая дорожка NORDICTRACK PRO 3000
categpath=/Городские	url=http://www.ozonsport.ru/product_3247.html	name=Велосипед Giant Roam 3 (2015)
categpath=/беговые дорожки	url=http://www.ozonsport.ru/product_3082.html	name=Беговая дорожка BODY SCULPTURE TM1556-01
categpath=/Скамьи и стойки	url=http://www.ozonsport.ru/product_2240.html	name=Скамья Скотта BODY SOLID GPCB-329
END_FEED
;

my $feed = $proj->feed( { 
    data => $feed_data, 
    datatype => 'offers_tskv',
    filters => { 1111 => {
                      'url normordertitlecontains' => [
                                                        'БЕГОвая'
                                                      ]

               }},
});

for my $product (@{$feed->ptl}) {
    print $product->url . "\n";
} 

exit(0);

sub ReadFeed {
    my ( $fname ) = @_;
    local $/ = undef;
    open (fF,"<$fname") or die $!;
    my $result = <fF>;
    close fF;
    return $result;
}


1;

