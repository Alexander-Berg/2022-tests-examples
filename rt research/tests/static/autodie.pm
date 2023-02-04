package autodie;
use strict;
use warnings;

use Exporter;
our @ISA = qw(Exporter);
our @EXPORT = qw(open close binmode);

sub open(*;@) {}

sub close(*) {}

sub binmode(*;@) {}

1;