#!/usr/bin/env perl
use strict;
use warnings;
use v5.10;
use Data::Dumper;
use Fcntl qw(:DEFAULT :flock);
use POSIX ":sys_wait_h";
use File::Find;
use Cwd;
use Getopt::Long;
use Digest::MD5;
use utf8;

my %opt;
GetOptions(
    \%opt,
    'html', 'svn', 'no-warn', 'threads=i', 'silent', 'no-check-cwd',
    'cwd=s', 'shard-pos=i', 'shards-total=i', 'perl=s', 'report-file=s',
);

my %use = (
    './scripts/lib/BM/Xmlse.pm'           => '-MXML::Simple',
    './scripts/lib/StaticMap.pm'          => '-MUtils::Sys',
    './scripts/monitors/check-logs.pl'    => '-MBM::Monitor::Logs',
    './scripts/wlib/Cmds/Categs.pm'       => '-MWebCommon',
    './scripts/wlib/Cmds/Crons.pm'        => '-MCmds::Feeds',
);

my %force_ignore = (
    './scripts/tests/perlcritic.pl' => 1,
    './scripts/cpan/Sereal/Decoder.pm' => 1,
    './scripts/cpan/Sereal/Encoder.pm' => 1,
    './scripts/cpan/Sereal/Performance.pm' => 1,
    './scripts/cpan/IO/HTML.pm' => 1,
    './scripts/cpan/Tie/Cache.pm' => 1,
    './scripts/cpan/URI/Escape.pm' => 1,
    './scripts/tests/judy_test.pl' => 1,
    './scripts/utils/log_rotate.pl' => 1,
    './scripts/lib/DSSM/Applier.pm' => 1,

    #ToDo: remove after fixing actions in begin block
    './scripts/bootstrap.pl' => 1,
    './scripts/publish_catalogia_to_sandbox.pl' => 1,

    #ToDo: remove after fixes
    './scripts/dyn-smart-banners/remap_dict_dynbanners_categs_mapping.pl' => 1,
    './scripts/lib/BM/MinusWords/Extractor/QtailWordsExtractor.pm' => 1,
    './scripts/lib/BM/MinusWords/Extractor/myLib/group12_sum1.pl' => 1,
    './scripts/lib/BM/MinusWords/Extractor/QtailExtractor.pm' => 1,
    './scripts/lib/BM/PrefProjSrv.pm' => 1,
    './scripts/lib/BM/Quality/Graphs.pm' => 1,
    './scripts/lib/DataSource/Query.pm' => 1,
    './scripts/lib/Utils/CommonDev.pm' => 1,
    './scripts/lib/Utils/StaticMap/test.pl' => 1,
    './scripts/lib/Utils/StaticZmap/test.pl' => 1,
    './scripts/tagging_benchmark_utils/find_banners_benchmark_categs_diff.pl' => 1,
    './scripts/tests/kyoto-test.pl' => 1,
    './scripts/tests/test-categs.pl' => 1,
    './scripts/tests/test-remotecache.pl' => 1,
    './scripts/utils/audit-bm-off.pl' => 1,
    './scripts/utils/deploy.pl' => 1,
);

my %tree = (
    ''        => 0,
    'scripts' => {
        ''      => 1,
        'users' => 0,
        'junk'  => 0,
    },
);

sub my_log_time_fmt {
    return POSIX::strftime("%Y-%m-%d %H:%M:%S", localtime);
}

sub my_log_msg_fmt {
    my $time = my_log_time_fmt();
    return join("\t", $time, "[$$]", shift);
}

sub loggify {
    my $str = shift;
    my @lines = split "\n", $str;
    return join "\n", map {my_log_msg_fmt($_)} @lines;
}

sub in_tree {
    my ($tree, $components) = @_;
    my $comp = shift @$components;
    if ('HASH' eq ref $tree->{$comp}) {
        return in_tree($tree->{$comp}, [ '' ]) unless scalar @$components;
        return in_tree($tree->{$comp}, $components);
    } else {
        return $tree->{$comp} if exists $tree->{$comp};
        return $tree->{''};
    }
}

sub linkify {
    my ($text, $revision) = @_;
    my @lines = split "\n", $text;
    @lines = map{
        if (m! at \./([\.\w/\-]+) line (\d+)!) {
            my $href = "https://a.yandex-team.ru/arc/trunk/arcadia/rt-research/broadmatching/$1?rev=$revision&blame=true#L$2";
            my $link = " at <a href=\"$href\">./$1 line $2</a>";
            s! at \./[\.\w/\-]+ line \d+!$link!;
        }
        $_
    } @lines;
    return join "\n", @lines;
}

sub render_res {
    my ($file, $res, $revision, $out, $prev_out) = @_;
    my $hidden = "style=\"display: none;\"";
    my $color_ok = "style=\"color: green;\"";
    my $color_warn = "style=\"color: orange; cursor: pointer;\"";
    my $color_fail = "style=\"color: red; cursor: pointer;\"";
    $prev_out = '' if $opt{'no-warn'};
    if ('ok' eq $res) {
        return loggify("$file ok") unless $opt{'html'};
        return "<div $color_ok>$file ok</div><br/>\n\n";
    }
    elsif ('ok with ObjLib::Obj' eq $res) {
        return loggify("$file ok with ObjLib::Obj\n$prev_out") unless $opt{'html'};
        my $html = linkify($prev_out, $revision);
        return "<div $color_warn onclick=\"toggle(this)\">$file ok with ObjLib::Obj</div>\n<pre $hidden>$html</pre><br/>\n\n";
    }
    elsif ('ok with Project' eq $res) {
        return loggify("$file ok with Project\n$prev_out") unless $opt{'html'};
        my $html = linkify($prev_out, $revision);
        return "<div $color_warn onclick=\"toggle(this)\">$file ok with Project</div>\n<pre $hidden>$html</pre><br/>\n\n";
    }
    else {
        return loggify("$file failed\n$out") unless $opt{'html'};
        my $html = linkify($out, $revision);
        return "<div $color_fail onclick=\"toggle(this)\">$file failed</div>\n<pre>$html</pre><br/>\n\n";
    }
}

sub kostyl_preprocessor {
    my ($str) = @_;
    my @lines = split "\n", $str;
    @lines = grep {
        length($_) && !(m!Nonexistent subroutine '[\w:]*open'! || m!Nonexistent subroutine '[\w:]*close'! ||
        m!Nonexistent subroutine 'BM::Matching::CampaignMatcher::total_size' called at ./scripts/lib/BM/Matching/CampaignMatcher.pm!)
    } @lines;
    return join "\n", @lines;
}

sub check_utf8 {
    my ($file) = @_;
    open my $fh, '<', $file;
    binmode($fh, ":utf8");
    my $ok = 1;
    local $SIG{__WARN__} = sub {
        $ok = 0;
    };
    my $pod = 0;
    while (my $ln = <$fh>) {
        $pod = 1 if $ln =~ m!^=!;
        $pod = 0 if $ln =~ m!^=cut!;
        next if $pod;
        last if $ln =~ m!^\s*use utf8;!;
        return 0 if $ln =~ m!^[^#]*[\x80-\x{10ffff}]!;
    }
    return $ok;
}

sub check {
    my ($file, $revision, $exit_code_ref) = @_;
    my $perl = $opt{perl} || 'perl';

    if (!eval{check_utf8($file)}) {
        $$exit_code_ref = 1;
        return render_res($file, 'failed', $revision, 'Wide character without utf8 pragma');
    }
    my $use = exists $use{$file} ? $use{$file} : '';
    my $out1 = kostyl_preprocessor(scalar qx($perl $use -MO=Lint,none,undefined-subs $file 2>&1));
    chomp $out1;
    if ($out1 =~ m!^[\w\./\-]+ syntax OK$!) {
        return '' if $opt{'silent'};
        return render_res($file, 'ok', $revision, $out1);
    }

    my $out2 = kostyl_preprocessor(scalar qx($perl -MObjLib::Obj $use -MO=Lint,none,undefined-subs $file 2>&1));
    chomp $out2;
    if ($out2 =~ m!^[\w\./\-]+ syntax OK$!) {
        return '' if $opt{'silent'};
        return render_res($file, 'ok with ObjLib::Obj', $revision, $out2, $out1);
    }

    my $out3 = kostyl_preprocessor(scalar qx($perl -MProject $use -MO=Lint,none,undefined-subs $file 2>&1));
    chomp $out3;
    if ($out3 =~ m!^[\w\./\-]+ syntax OK$!) {
        return '' if $opt{'silent'};
        return render_res($file, 'ok with Project', $revision, $out3, $out2);
    }

    $$exit_code_ref = 1;
    return render_res($file, 'failed', $revision, $out3);
}

sub get_revision() {
    my $out = qx(ya tool svn </dev/null info 2>/dev/null);
    for my $line (split "\n", $out) {
        if ($line =~ m!^Revision: (\d+)$!) {
            return $1;
        }
    }
    #die "Unknown revision" unless -e '../../.hg';
    return "999999999";
}

sub openlock($) {
    my ($pid) = @_;
    open my $fh, '>', ".$pid.lock";
    return $fh;
}

sub rmlock($) {
    my ($pid) = @_;
    unlink ".$pid.lock"
}

sub __output {
    my $lfh = shift;
    flock($lfh, LOCK_EX);
    say STDERR $_[0] if ($_[0] ne '');
    STDERR->flush();
    flock($lfh, LOCK_UN);
}

sub run {
    my ($ppid, $data_rd_fh, $signal_wr_fh) = @_;
    my $exit_code = 0;
    my $wlfh = openlock($ppid);
    $signal_wr_fh->autoflush();
    print $signal_wr_fh "\n";
    while (my $ln = <$data_rd_fh>) {
        chomp $ln;
        my ($file, $revision) = split "\t", $ln;
        __output($wlfh, check($file, $revision, \$exit_code));
        print $signal_wr_fh "\n";
    }
    return $exit_code;
}

sub md5int_simple {
    my ($str) = @_;
    my @a = unpack("N4", Digest::MD5::md5($str));
    return ($a[1] ^ $a[3]) << 32 | ($a[0] ^ $a[2]);
}


sub main(){
    if ($opt{cwd}) {
        chdir($opt{cwd});
    }
    die "Run from rt-research/broadmatching directory" unless (getcwd() =~ m!rt-research/broadmatching!) or $opt{'no-check-cwd'};
    $ENV{PERL5LIB} = "scripts/tests/static:scripts/cpan:scripts/reduce:".
        "scripts/lib/BM/MinusWords/Extractor/myLib:scripts/wlib:scripts/lib".($ENV{PERL5LIB} ? ":$ENV{PERL5LIB}" : "");
    my @to_check;
    if ($opt{svn}) {
        my @lines = qx(ya tool svn </dev/null status);
        chomp @lines;
        @to_check = map{s!^scripts!./scripts!; $_} map{m!^[MA]\s+(\S+)$! ? $1 : ()} @lines;
    }
    elsif (scalar @ARGV) {
        @to_check = map{s!^scripts!./scripts!; $_} @ARGV;
        for my $file (@to_check) {
            die "Use ./scripts/... or scripts/... path format" unless $file =~ m!^./scripts/!;
        }
    }
    else {
        finddepth({wanted => sub {
                if (m!\.pl$! || m!\.pm$!) {
                    my $path = $File::Find::name;
                    return if exists $force_ignore{$path};
                    $path =~ s!\./!!;
                    if ($opt{'shards-total'}) {
                        my $t = $opt{'shards-total'};
                        my $p = $opt{'shard-pos'};
                        my $h = md5int_simple($path);
                        push @to_check, $File::Find::name if in_tree(\%tree, [ split '/', $path ]) && ($p == $h % $t);
                    } else {
                        push @to_check, $File::Find::name if in_tree(\%tree, [ split '/', $path ]);
                    }
                }
            }, follow => 1}, ".");
        if ($opt{'report-file'}) {
            open my $fh, '>', $opt{'report-file'};
            my $to_check_cnt = scalar @to_check;
            print $fh '{"files_checked": '.$to_check_cnt.'}';
        }
    }
    @to_check = sort @to_check;
    print "<html><head><script>
    function toggle(el){
        el.nextElementSibling.style['display'] = el.nextElementSibling.style['display'] ? '' : 'none';
    }
    </script></head><body>\n" if $opt{'html'};
    my $revision = get_revision();

    my @workers;
    my $jobs = $opt{threads} || 24;
    for (1 .. $jobs) {
        pipe my $data_rd_fh, my $data_wr_fh;
        pipe my $signal_rd_fh, my $signal_wr_fh;
        my $ppid = $$;
        my $pid = fork;
        if ($pid) {
            close $data_rd_fh;
            close $signal_wr_fh;
            push @workers, { pid => $pid, data_wr_fh => $data_wr_fh, signal_rd_fh => $signal_rd_fh };
        }
        else {
            close $data_wr_fh;
            close $signal_rd_fh;
            exit run($ppid, $data_rd_fh, $signal_wr_fh);
        }
    }

    my @ready;
    for my $file (@to_check) {
        while (0 == scalar @ready) {
            my ($read_descrs_bitmap, $write_descrs_bitmap, $error_descrs_bitmap) = ('', undef, undef);
            vec($read_descrs_bitmap, fileno($_->{signal_rd_fh}), 1) = 1 for @workers; # set corresponding bit
            my $timeout = undef; # indefinitely
            select $read_descrs_bitmap, $write_descrs_bitmap, $error_descrs_bitmap, $timeout;
            @ready = grep{vec($read_descrs_bitmap, fileno($_->{signal_rd_fh}), 1)} @workers;
        }
        my $worker = shift @ready;
        sysread($worker->{signal_rd_fh}, my $unused, 1); # flush ready flag
        my $wfh = $worker->{data_wr_fh};
        say $wfh "$file\t$revision";
        $wfh->flush();
    }
    close $_->{data_wr_fh} for @workers;
    rmlock($$);

    my $kid;
    my $exit_code = 0;
    do {
        $kid = waitpid(-1, 0);
        $exit_code = $? >> 8 if ($kid > 0) && $?;
    } while $kid > 0;
    exit $exit_code;
    print "</body></html>" if $opt{'html'};
    return $exit_code;
}

main();
