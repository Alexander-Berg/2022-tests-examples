#!/usr/bin/perl -w

use strict;

use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use IO::Socket;
use IO::Handle;

use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;

my $proj = Project->new({});

while(defined(my $query = STDIN->getline)) {
    chomp $query;
    print "-> $query\n";
    
    my $resp = $proj->bender_client->exec_command_once($query);
    print "<- $resp\n" if $resp;

    last if !$proj->bender_client->is_connected || $resp =~ /^BYE/;
}
