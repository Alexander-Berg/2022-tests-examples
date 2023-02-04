#!/usr/bin/perl -w
use strict;

use utf8;
use open ":utf8";

use Getopt::Long;

use FindBin;
use lib "$FindBin::Bin/../lib";
use Project;
use Utils::Hosts qw(
    get_hosts
    get_host_info
);
use Utils::Sys qw(
    uniq
    handle_errors
    print_log print_err
);

handle_errors();

my %opt;
GetOptions(\%opt, 'help|h', 'role=s', 'project=s');
if ($opt{help}) {
    printf "Usage: $0 [Options]\n";
    printf "Check perl syntax for some scripts\n";
    printf "Options:\n";
    printf "  --role=R1[,R2,...]     scripts for given roles\n";
    printf "  --project=P            scripts for all roles of given project\n";
    exit(0);
}

my $proj = Project->new;
my $sdir = $proj->options->{dirs}{scripts};
my @roles;
if ($opt{project}) {
    @roles = get_roles($opt{project});
} else {
    @roles = split /,/, $opt{role};
}

print "@roles\n";
my @scripts = uniq(map { get_scripts($_) } @roles);
for my $script (@scripts) {
    system "perl -c $sdir/$script";
}


sub get_roles {
    my $p = shift;
    my @roles = uniq map {
        get_host_info($_)->{role}
    } grep {
        my $inf = get_host_info($_); !$inf->{test} and $inf->{role} !~ /idle/;
    } get_hosts(project => $p);
    return @roles;
}

sub get_scripts {
    my $r = shift;
    my @res;
    my @crontabs = ("$sdir/crontabs/$r.cron.d");
    die "No main crontab file for role `$r'" if !-f $crontabs[0];
    push @crontabs, grep { -f $_ } "$sdir/crontabs/$r-master.cron.d";
    push @res, get_scripts_from_crontab($_) for @crontabs;
    return @res;
}

sub get_scripts_from_crontab {
    my $file = shift;
    my @res;
    open my $fh, '<', $file
        or die "Can't open crontab `$file': $!";
    while (<$fh>) {
        next if /^\s*#/;
        next unless $_ =~ m!/opt/broadmatching/scripts/([^ ]*\.pl)!;
        push @res, $1;
    }
    close $fh;
    return @res;
}
