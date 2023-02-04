#!/usr/bin/perl -w

use strict;
use utf8;

my $feedfile    = '/opt/broadmatching/temp/dyn_banners/eldorado_ru/tskv/20151020_15_sitetskv_pkd.100';

use lib '/opt/broadmatching/scripts/lib';
use lib '/opt/broadmatching/scripts/wlib';

use Project;
use CatalogiaMediaProject;
my $proj = CatalogiaMediaProject->new({ 
    load_dicts =>1,
    load_minicategs_light => 1,
    no_auth => 1,
    no_form => 1,
    nrmsrv => 0, 
});

$proj->log("started");

my $feed_data = GetDataFromFeed($feedfile);

my $filters = GetFiltersFromFile();

$proj->log("feed data length=" . length($feed_data));

my $feed = $proj->feed( { data => $feed_data, datatype => 'offers_tskv', filters => $filters } );
#my $feed = $proj->feed( { data => $feed_data, datatype => 'offers_tskv', filters => {} } );
$proj->log("feed ptl count:" . $feed->ptl->count);

$proj->log("finished");

exit(0);

# subs
sub GetDataFromFeed {
    my ( $file ) = @_;
    my $result = "";
    open (fF,"<$file") || die $!;
    while (<fF>) {
        $result .= $_;
    }
    close fF;
    return $result;
}

sub GetFiltersFromFileaaa {
    return {
      99 => { 'url like' => ['7112'] },
    };
}

sub GetFiltersFromFile {
    return


          {
            63 => {
                    'url NOT like' => [
                                        'http://www.eldorado.ru/help/'
                                      ]
                  },
            75 => {
                    'url NOT contains' => [
                                            'вакансии'
                                          ],
                    'url NOT like' => [
                                        'credit',
                                        'tenders',
                                        'help',
                                        'company',
                                        'for_buyers',
                                        'corp.eldorado.ru'
                                      ]
                  },
            64 => {
                    'url NOT like' => [
                                        'http://www.eldorado.ru/company/tenders/'
                                      ]
                  },
            66 => {
                    'url NOT like' => [
                                        'http://www.eldorado.ru/company/'
                                      ]
                  },
            61 => {
                    'url NOT normordercontains' => [
                                                     'вакансии'
                                                   ]
                  },
            76 => {
                    'url like' => [
                                    'eldorado.ru/cat/'
                                  ]
                  },
            65 => {
                    'url NOT like' => [
                                        'http://www.eldorado.ru/for_buyers/'
                                      ]
                  }
          };
}

1;
