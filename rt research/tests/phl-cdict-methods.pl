#!/usr/bin/perl -w
use strict;

use utf8;
use open ':utf8';

use JSON;

use Project;

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';

my $proj = Project->new({load_dicts => 1, load_minicategs_light => 1, use_comptrie_subphraser => 1});

my @texts = (
    'adidas', 'робот пылесос', 'курсы немецкого', 'рф',
    '', 'курс $',
    '2011 марка угон',
    'сайт мебели спб',
    '2017',
    'в на украине',
    'купить авто', 'продать авто',
    '!что !где !когда',
);
my $phl = $proj->phrase_list(\@texts);
my @r = test_phl($phl);
for my $r (@r) {
    print join("\t", @$r), "\n";
}

sub test_phl {
    my $phl = shift;
    my $proj = $phl->proj;
    my @res;
    my $lim = 100;

    $phl->cache_search_count;
    push @res, map { [ 'cache_search_count', $_->text, $_->get_search_count ] } @$phl;

    $phl->cache_search_syns;
    for my $phr (@$phl) {
        my $syns = $phr->get_search_syns;
        my @syns = sort map { $_->text } @$syns;
        @syns = splice(@syns, 0, $lim);
        for my $i (0 .. $#syns) {
            push @res, [ 'cache_search_syns', $phr->text, "syn_$i:".$syns[$i] ];
        }
    }

    $phl->cache_regions_count;
    push @res, map {
        my $rc = $_->get_regions_count;
        (
            [ 'cache_search_countg', $_->text, 'msk:'.($rc->{213} // 0) ],
            [ 'cache_search_countg', $_->text, 'spb:'.($rc->{2} // 0) ],
        );
    } @$phl;

    $phl->cache_search_tail;
    for my $phr (@$phl) {
        my $tails = $phr->get_search_tail;
        my @top = sort { $tails->{$b} <=> $tails->{$a} } keys %$tails;
        @top = splice(@top, 0, $lim);
        for my $i (0 .. $#top) {
            push @res, [ 'cache_search_tail', $phr->text, "tail_$i:".$top[$i].':'.$tails->{$top[$i]} ];
        }
        push @res, [ 'cache_search_tail', $phr->text, 'tail_cnt:'.@top ];
    }

    $phl->cache_search_query_count;
    push @res, map { [ 'cache_search_query_count', $_->text, $_->get_search_query_count ] } @$phl;

    $phl->cache_cdict_regions_phrases;
    for my $phr (@$phl) {
        my $phr2regs = $phr->{cdict_regions_phrases} // {};
        my @phrs = sort keys %$phr2regs;
        @phrs = splice(@phrs, 0, $lim);
        for my $reg_phr (@phrs) {
            my @regs = sort keys %{$phr2regs->{$reg_phr}};
            push @res, [ 'cache_cdict_regions_phrases', $phr->text, "phr_$reg_phr:".join(',', @regs) ];
        }
        push @res, [ 'cache_cdict_regions_phrases', $phr->text, 'phr_cnt:'.(keys %$phr2regs) ];
    }

    $phl->cache_cdict_tail_categs;
    for my $phr (@$phl) {
        my $tail2count = $phr->{tail2count};
        my @tails = sort { $tail2count->{$b} <=> $tail2count->{$a} } keys %$tail2count;
        @tails = splice(@tails, 0, $lim);
        for my $tail (@tails) {
            push @res, [ 'cache_cdict_tail_categs', $phr->text, "tail_$tail:".join('/', sort @{$phr->{tail2categs}{$tail}}) ];
        }
    }

    $phl->cache_cdict_minicategs;
    push @res, map { [ 'cache_cdict_minicategs', $_->text, join('/', sort @{$_->{cdict_minicategs} // []}) ] } @$phl;

    return @res;
}
