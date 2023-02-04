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

my $feed_data = ReadFeed('/opt/broadmatching/temp/dyn_banners/ozonsport_ru/tskv/20151018_10_sitetskv_pkd');

my $feed = $proj->feed( { 
    data => $feed_data, 
    datatype => 'offers_tskv',
    filters => { 1111 => {
                      'url normordertitlecontains' => [
                                                        'беговая'
                                                      ]

               }},
});

for my $product (@{$feed->ptl}) {
    print $product->url . "\n";
} 

exit(0);

sub ReadFeed {
    my ( $fname, $lines ) = @_;
    local $/ = undef;
    open (fF,"<$fname") or die $!;
    my $result = <fF>;
    close fF;
    if ( defined($lines) && int($lines) > 0 ) {
        $lines = int($lines);
        $result = join("\n",
            (
            sort { rand() <=> rand() }
            split /[\r\n]/, $result
            )[0 .. $lines - 1]
        );
    }
    return $result;
}


1;

