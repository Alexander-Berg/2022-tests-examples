#!/usr/bin/perl 

use strict;

use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use CGI;
use CGI::Carp qw/fatalsToBrowser/;

use FindBin;
use lib "$FindBin::Bin/../../lib";
use lib "$FindBin::Bin/../../wlib";
use CatalogiaMediaProject;
use Data::Dumper;

my $proj = CatalogiaMediaProject->new({ 
    indcmd => 1, 
    no_auth => 1
});

print "Content-Type: text/plain\n\n";

my $data = $proj->form->{export_set};
my $message = "";

if(!$data) {
    print "ERROR no data\n";
    exit(0);
}

my $num_items = 0;
my @fields = qw(
    SetID
    BannerID
    Lang
    Title
    Body
    Phrases
    InitialCategories
    ManualCategory
    Mode
    PhrasesCategories 
);

for my $item (split "\n", $data) {
    next if !$item;
    
    my @values = split "\t", $item;
    next if scalar(@values) != scalar(@fields);

    my $h = {};
    $h->{$fields[$_]} = $values[$_] for 0..$#fields;

    $proj->test_banners->Add($h, { replace => 1 });
    $num_items++;
}

print "OK $num_items\n";

