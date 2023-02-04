#!/usr/bin/perl -w


for ( 1 .. 100000 ) {
    @values = ();
    for $g ( 1 .. 10 ) {
        push @values, int((10 + $g * 10)*rand());
    }
    print join("\t", @values)."\n";
}
