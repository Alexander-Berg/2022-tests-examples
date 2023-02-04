#!/usr/bin/perl -w

use strict;
use Data::Dumper;
use lib '/opt/broadmatching/scripts/lib';

use Project;

my $proj = Project->new();

$proj->log( Dumper($proj->st_client->get_tasks({
        queue   => 'SMARTBANNER',
        summary => 'DIVAN.RU',
})));
