#!/usr/bin/perl -w
use strict;

use utf8;

use open ':utf8';

use JSON;

use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';
binmode STDERR, ':utf8';

select STDERR; $|++;
select STDOUT; $|++;


my $proj = Project->new({load_dicts=>1, load_minicategs_light=>1});
my @test;

my @ru_texts = (
    'запчасти kia ceed', 'москва ресторан', 'кроссовки nike', 'фраза с порно',
);
push @test, [ \@ru_texts, 'ru' ];

my @tr_texts = (
    'Türkiye', 'İstanbul konumlar',
);
push @test, [ \@tr_texts, 'tr' ];

for my $h (@test) {
    my ($texts, $lang) = @$h;
    $proj->current_lang($lang);
    my $phl = $proj->phrase_list($texts);

    $phl->cache_search_count;
    $phl->cache_search_query_count;
    $phl->cache_regions_count;
    $phl->cache_search_tail;
    $phl->cache_cdict_regions_phrases;
    $phl->cache_cdict_tail_categs;
    $phl->cache_search_syns;
    $phl->cache_is_good_phrase;
    for my $phr (@$phl) {
        my $reg_cnt = $phr->get_regions_count;
        my @top_reg = sort { $reg_cnt->{$b} <=> $reg_cnt->{$a} } keys %$reg_cnt;
        @top_reg = splice(@top_reg, 0, 10);

        my $tails = $phr->get_search_tail;
        my @top_tail = sort { $tails->{$b} <=> $tails->{$a} } keys %$tails;
        @top_tail = splice(@top_tail, 0, 10);

        my @top_syns = sort map { $_->text } $phr->get_search_syns->phrases;
        @top_syns = splice(@top_syns, 0, 10);

        print $_."\n" for (
            'lang => '.$phr->lang,
            'text => '.$phr->text,
            'search_count => '.$phr->get_search_count,
            'search_query_count => '.$phr->get_search_query_count,
            'regions_count => '.join(',', map { "$_:$reg_cnt->{$_}" } @top_reg),  # fix order
            'tails => '.join(',', map { "$_:$tails->{$_}" } @top_tail),
            'regions => '.join(',', sort { $a cmp $b } map { $_->{name} } $phr->get_regions),
            'tail_categs => '.join('/', map { @{ $phr->{tail2categs}{$_} // [] } } @top_tail),
            'syns => '.join(',', @top_syns),
            'is_good => '.$phr->is_good_phrase,
        );
        print "\n";
    }
}
