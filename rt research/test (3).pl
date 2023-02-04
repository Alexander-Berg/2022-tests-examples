#!/usr/bin/perl -w
#тестирование изменений в categs_atoms

use strict;

use utf8;
use open ":utf8";
use Data::Dumper;

binmode STDIN, ':utf8';
binmode STDOUT, ':utf8';
binmode STDERR, ':utf8';

open F1, "categs_atoms_src";
chomp(my @a = <F1>);

my @new; #новая версия словаря

for my $file ("categs_atoms_999", "categs_atoms_1004", "categs_atoms_1006", "categs_atoms_1007", "categs_atoms_1008", "categs_atoms_1009", "categs_atoms_1010", "categs_atoms_1012") {
    open F2, $file;
    chomp(my @b = <F2>);

    print("Не совпадает число записей $file:", @a+0, "!=", @b+0, "\n") if @a != @b;

    for my $i (0..$#a) {
        if ($a[$i] ne $b[$i]) {
            push @new, "$i\t$b[$i]";
            print STDERR "$file\t$i\t$b[$i]\n";
        }
    }
}
