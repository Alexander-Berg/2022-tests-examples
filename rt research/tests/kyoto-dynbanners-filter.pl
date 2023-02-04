#!/usr/bin/perl -w

use strict;
use utf8;

use lib '/opt/broadmatching/scripts/lib';

use Project;

my $proj = Project->new({load_dicts=>1, load_minicategs_light =>1});

open(F, "</tmp/sample_feed_input.100") or die $!;
my @tskvarr = <F>;
my $data = join '', @tskvarr;
close(F);


my $filters =  {
            151 => {
#                     'url normordertitlecontains' => [
#                                                       'электроинструмент'
#                                                     ],
                     'url NOT normordercontains' => [
                                                      'Товар временно отсутствует в продаже'
                                                    ]
                   }
          };


$proj->log('beg111');
    my $fd = $proj->feed( { data => $data, datatype => 'offers_tskv',  filters => $filters, } );
    $fd->ptl;
$proj->log('end111');

