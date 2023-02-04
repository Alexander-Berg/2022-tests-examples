#!/usr/bin/perl

use strict;
use warnings;
use autodie;

use utf8;
use open ":utf8";
no warnings "utf8";

use LWP::UserAgent;
use HTTP::Request;
use JSON::XS qw(encode_json decode_json);
use Time::HiRes;
use Encode qw(encode_utf8 decode_utf8);
use Data::Dumper;

use Project;

my $CHUNK_SIZE = 10;
my $COUNT = 100 * $CHUNK_SIZE;
my $FORKS = 30;

sub send_load {
    my $proj = shift // die;

    my $ua = LWP::UserAgent->new();   
    my @bids = split /,/, $proj->random_banners_client->k_random_banners($COUNT);

    $proj->log("Started");

    my $started = Time::HiRes::time();
    while (my @chunk = splice(@bids, 0, $CHUNK_SIZE)) {
        my $query = {
            action => "select",
            what => "bid, title, body",
            from => "//home/catalogia/dyntables/resources/banners_extended",
            where => "bid in (" . join(", ", @chunk) . ")"
        };
        
        my $req = HTTP::Request->new(POST => "http://127.0.0.1:10203/");
        $req->content(encode_json($query));

        my $resp = $ua->request($req)->decoded_content();
        my $data = decode_json($resp);
        if ($data->{errmsg}) {
            $proj->log($data->{errmsg} . "\n" . Dumper($query));
        }
    }
    my $elapsed = Time::HiRes::time() - $started;
    my $ms_per_chunk = $elapsed / ($COUNT / $CHUNK_SIZE) * 1000;
    my $ms_per_key = $ms_per_chunk / $CHUNK_SIZE;
    
    $proj->log("Done, $ms_per_chunk ms per chunk, $ms_per_key per key");
}

my $proj = Project->new();

for (1 .. $FORKS) {
    my $pid = fork();
    if ($pid == 0) {
        send_load($proj);
        exit(0);
    }
}

while (wait() != -1) { sleep 1 }


# perl -MLWP::UserAgent -MHTTP::Request -MJSON::XS=encode_json -lwe 'my $ua = LWP::UserAgent->new(); my $req = HTTP::Request->new(POST => "http://127.0.0.1:10203/"); $req->content(encode_json({ action => "select", what => "bid, title, body", from => "//home/catalogia/dyntables/resources/banners_extended", where => "bid = 4" })); print $ua->request($req)->decoded_content()'

# perl -MLWP::UserAgent -MHTTP::Request -MJSON::XS=encode_json -lwe 'my $ua = LWP::UserAgent->new(); my $req = HTTP::Request->new(POST => "http://127.0.0.1:10203/"); $req->content(encode_json({ action => "shutdown" })); print $ua->request($req)->decoded_content()'
