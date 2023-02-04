#!/usr/bin/perl -w
use strict;
use warnings;
use utf8;
use open ":utf8";

binmode(STDIN,':utf8');
binmode(STDOUT,':utf8');


use FindBin;
use lib "$FindBin::Bin/../../lib";
use lib "$FindBin::Bin/../../wlib";
use Utils::Sys qw/md5int/;
use List::Util qw(min);

use Getopt::Long;
use CatalogiaMediaProject;
use Project;
use Cmds::Mediaplanners;
use Data::Dumper;
use BM::PhraseCategs;
use Utils::Urls qw(normalize_url);
use Utils::Words qw(stop4norm);


use BM::YQL::Helpers qw(get_k_random_banners);

my $proj = Project->new({
#    load_dicts                              => 1,
#    load_minicategs_light                   => 1,
#    allow_lazy_dicts                        => 1,
#    use_comptrie_subphraser                 => 1,
#    use_sandbox_categories_suppression_dict => 1,
});


$proj->dd($proj->banner_factory->get_string_fields);
