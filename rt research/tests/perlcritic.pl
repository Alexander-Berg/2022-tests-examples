#!/usr/bin/env perl
use 5.006001;
use strict;
use warnings;
use Perl::Critic::Command qw(run);
open(STDOUT, ">&STDERR");
exit run();
