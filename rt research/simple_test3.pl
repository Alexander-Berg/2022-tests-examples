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

use Getopt::Long;
use CatalogiaMediaProject;
use Cmds::Mediaplanners;
use Data::Dumper;

my $proj = Project->new({
});

my $url = 'http://halava-555@mail.ru';

my $fixed_url = $proj->page($proj->page($url)->unicode_url)->fixed_url;
print $fixed_url . "\n";
my $page = $proj->page($fixed_url);
print $page->domain . "\n";
print $page->uri . "|\n";
print $page->domain_path . "\n";
exit;
