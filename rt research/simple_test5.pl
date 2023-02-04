#!/usr/bin/perl -w
use strict;
use warnings;
use utf8;
use open ":utf8";

use Encode;

binmode(STDIN,':utf8');
binmode(STDOUT,':utf8');

use FindBin;
use lib "$FindBin::Bin/../../lib";
use lib "$FindBin::Bin/../../wlib";

use CatalogiaMediaProject;

use Getopt::Long;
use Data::Dumper;

my $proj = CatalogiaMediaProject->new({
    load_dicts                              => 1,
    load_minicategs_light                   => 1,
    no_auth => 1,
    no_form => 1,
    nrmsrv => 0,
});

    $proj->categs_tree->never_write_categs_cache(1);
    $proj->categs_tree->never_read_categs_cache(1);
print "-----------Ready----------\n";
while (<STDIN>)
{
    chomp;
    my $phr = $proj->phrase($_);
    $proj->dd($phr->text(), $phr->text_add_pluses_to_stop_words(), $phr->text_delete_pluses_except_stop_words() );
}
