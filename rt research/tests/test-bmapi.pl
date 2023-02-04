#!/usr/bin/perl -w

use strict;

use Getopt::Long;

use utf8;
use open ':utf8';
no warnings 'utf8';
binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

use Data::Dumper;

use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;
use Utils::Sys qw[
    print_err
    handle_errors
];

handle_errors();

my %opt = (
    host => 'localhost',
);
GetOptions(
    \%opt,
    'help|h',
    'host=s',
);

if ($opt{help}) {
    print "$_\n" for (
        "scripts/tests/test-bmapi.pl  -  Test bmapi host",
        "  --host      Host to test. Default is localhost",
    );
    exit(0);
}

my $res = main();
exit($res ? 0 : 1);


sub main {

    my $host = $opt{host} || 'localhost';

    my @errors;

    my @data = (
        { request => 'act=alive', response => { code => 200, content => 'ALIVE', } },
        { request => '', response => { code => 500, } },
        { request => 'act=norm_phrase_list&data=куплю слона%23END', response => { code => 200, content => "купить слон\n#END", } },
        { request => 'cmd=test_sleep&duration=1', response => { code => 200, content => '', } },
        #{ request => 'cmd=test_sleep&duration=1&timeout=3', response => { code => 200, content => '', } },
        #{ request => 'cmd=test_sleep&duration=10&timeout=3', response => { code => 500, content => '', } },
        { request => 'cmd=test_not_existing_cmd', response => { code => 500, } },
        #{ request => 'cmd=test_not_existing_cmd&timeout=3', response => { code => 500, content => '', } },
    );
    for my $item (@data) {
        my $ua = LWP::UserAgent->new();
        #$ua->init;
        my $url = "http://$host/fcgi-bin/?" . $item->{request};
        print_err("url: $url");
        my $resp = $ua->get($url);
        my $content = Encode::decode_utf8($resp->content);
        if ($resp->code eq $item->{response}{code}
            and (not defined $item->{response}{content}  or  $content eq $item->{response}{content})
            and (not defined $item->{response}{message}  or  $resp->message eq $item->{response}{message})
        ) {
            print_err("**** (" . $resp->code . ")  ok");
        } else {
            my $msg = join(", ",
                "url => '$url'",
                (map { "$_ => '". $resp->$_ . "'" } qw[ code message ]),
                "content => '$content'",
            );
            print_err("**** (" . $resp->code . ")  Failed: $msg");
            push @errors, $msg;
        }
    }

    for my $cmd (
        $Utils::Common::options->{dirs}{scripts} . "tests/test-yml2directinf.pl $host",
    ) {
        my $res = Utils::Sys::do_sys_cmd($cmd, no_die => 1);
        if($res) {
            print_err("**** ok");
        } else {
            push @errors, $cmd;
            print_err("**** Failed");
        }
    };

    print_err("Done. " . (@errors ? scalar @errors . " errors" : "OK"));

    return (@errors ? 0 : 1);
}
