#!/usr/bin/perl -w
#
# use blib;
use lib "../../";
use XMLParser;
use Data::Dumper;
print "start test \n";
my $grep_res = XMLParser::grep("test-optozorax.xml", "ololo.tskv", 'offer', 0);
print Dumper($grep_res);
print "grep_res->message: ".$grep_res->message()."\n";
print "grep_res->message_ru: ".$grep_res->message_ru()."\n";
print "grep_res->is_error: ".$grep_res->is_error()."\n";
print "grep_res->is_warning: ".$grep_res->is_warning()."\n";
print "end test \n";
