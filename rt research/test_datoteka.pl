#!/usr/bin/perl
# use strict;
use warnings FATAL => 'all';
use Data::Dumper;
use utf8;

use Project;
my $proj = Project->new({load_dicts => 0});
my $dirs = $proj->options->{dirs};

my $old_cl = $proj->{datoteka_client};
my $new_cl = BM::BMClient::CdictClient->new({
        proj => $proj,
        port                => 11330,
        work_dir            => $dirs->{work} . "/cdict",
        temp_dir            => $dirs->{temp} . "/cdict",
        server_dir          => $dirs->{scripts} . "/cpp-source/cdict",
        norm_config_file    => $dirs->{dicts} . "/cpp_norm_config",
        host                => "bmbender-gen01i.yandex.ru",
        dict_file           => $dirs->{work} . "/cdict/datoteka_dict",
        index_file          => $dirs->{work} . "/cdict/datoteka_index",
        single_file         => $dirs->{work} . "/cdict/datoteka_cdict",
        raw_files_dir       => $dirs->{work} . "/cdict/raw_files",
    });

my $is_bad = undef;
my @phrases;
push @phrases, '14 185 75 зимний';
push @phrases, '14 185 725';

my @resp_old = $new_cl->exec_cmds([map { [ 'get', 'bnr_count', $_ ] } @phrases]);
my @resp_new = $old_cl->exec_cmds([map { [ 'get', 'bnr_count', $_ ] } @phrases]);
for my $i (0..$#resp_old) {
    # print "i=$i $resp_old[$i][1] $resp_new[$i][1]\n";
    if ($resp_old[$i][1] and $resp_new[$i][1] and $resp_old[$i][1] != $resp_new[$i][1]) {
        print "BAD ph='$phrases[$i]'; ns='bnr_count': old: $resp_old[$i][1] new $resp_new[$i][1] =(\n";
        $is_bad = 1;
        last;
    }
}

print "OK\n" if !$is_bad;
