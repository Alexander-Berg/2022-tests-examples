#!/usr/bin/perl -w
#подготовка тестового массива

use strict;

use utf8;
use open ":utf8";
use Data::Dumper;

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';
binmode STDERR, ':utf8';

#use FindBin;
#use lib "$FindBin::Bin/../lib";
use lib "/home/yuryz/arcadia/rt-research/broadmatching/scripts/lib";
use Project;

my $proj = Project->new({
    load_dicts   => 1,
    load_minicategs_light => 1, 
});


my $worker = Utils::Worker->new;
$worker->{verbose}    = 1;
$worker->{num_processes}    = 12;

$worker->{file_input}       = "/home/yuryz/scripts/homonym/bayes/bayes.test";
$worker->{file_output}      = "/home/yuryz/scripts/homonym/bayes/bayes.test.tmp";

$worker->{process_line}     = sub {
    my ($line, $fh) = @_;
    chomp $line;

    my ($ctgs, $title, $body, $id) = split /\t/, $line;
    my $phr = "$title $body";

    my $phr_pref = $proj->phrase($phr)->get_banner_prefiltered_phrase->text; #очистка
    my $phr_norm = $proj->phrase($phr_pref)->norm_phr; #нормализация
    my $phr_snorm = $proj->phrase($phr_norm)->snorm_phr; #снормализация

    my %wrds;
    $wrds{$_} = 1 for split / /, $phr_snorm; #удаление дублей
    my $wrds = join " ", sort keys %wrds;
    print $fh "$ctgs\t$id\t$title\t$body\t$wrds\n";
};

$worker->process_data;
