#!/usr/bin/perl
use strict;

use utf8;
use open ":utf8";

use FindBin;
use lib "$FindBin::Bin/../../lib";
use lib "$FindBin::Bin/../../wlib";
use Utils::Common;
use Utils::Sys qw/
    get_file_lock
    release_file_lock
    handle_errors
    dir_files
    modtime
    do_sys_cmd
/;
use Project;
use BM::Phrase;
use Data::Dumper;

handle_errors();

my $proj = Project->new(
{ 
no_auth => 1, no_form => 1,
#load_dicts => 1,
#load_minicategs_light => 1
}
);


my $x = q['perl -e '"'"'my ($cnt,$wrong, $key) = (0,0, "");my $threshold = 0.25; while (<STDIN>) {my ($flag, $text, $w) = split /\t/, $_; if ("$flag\t$text" ne $key) { if ($cnt) { my $res = $wrong/$cnt; print "$key\n" if $res < $threshold;}'"'"] ;
do_sys_cmd("echo $x");
