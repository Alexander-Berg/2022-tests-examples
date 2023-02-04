#!/usr/bin/perl -w
use strict;

use utf8;
use open ":utf8";

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';

use Data::Dumper;
use Getopt::Long;

use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;

use lib "$FindBin::Bin/../wlib";
use CatalogiaMediaProject;

use Utils::Sys qw(
    handle_errors
    print_log print_err
);

handle_errors();

my %proj_par;
my $help;
my $subphr;
my $bm;
GetOptions('help|h' => \$help, 'bm' => \$bm, 'subphraser' => \$subphr, 'o=s' => \%proj_par);
if ($help) {
    printf "Usage: proj.pl [Options]\n";
    printf "Options:\n";
    printf "  -o flag=val [-o flag=val ...]    flags for project\n";
    printf "  --subphraser                     test subphraser\n";
    printf "  --bm                             broadmatch main tests\n";
    exit(0);
}

if ($subphr) {
    $proj_par{load_dicts} = 1;
    $proj_par{load_minicategs_light} = 1;
}

if (keys %proj_par) {
    print_log( "Project flags:");
    print Dumper(\%proj_par);
}

print_log( "Creating Project ...");
my $proj = Project->new(\%proj_par)
    or die "Could not create Project";
print_log( "Creating Project done");
if ($subphr) {
    print_log("Categorize:");
    my $text = 'купить ноутбук dell за '.int(rand(100000)).' рублей '.int(rand(100)).' коп';
    print_log($text.' => '.join('/', $proj->phrase($text)->get_minicategs));
}

print_log( "Creating CatalogiaMediaProject...");
my $proj_catmedia = CatalogiaMediaProject->new({ indcmd => 1, 
    no_auth => 1,
    no_form => 1,
    timelogpackages => [ qw[ 
        BM::PhraseNrmSrv BM::Phrase BM::PhraseList BM::PhraseListNrmSrv 
        BM::PhraseParser
    ] ] 
})
    or die "Could not create Project";
print_log( "Creating CatalogiaMediaProject done");

if ($bm) {
    print_log("nothing special to test");
}

print_log( "OK");
