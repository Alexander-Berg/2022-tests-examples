package ObjLib::Obj;

use std;

use Data::Dumper;

use Class::Accessor::Fast;
use base qw(Class::Accessor::Fast); #Класс, позволяющие определять accessors
use Digest::MD5 qw(md5_hex);
use File::Path qw(make_path);
use Time::HiRes;


use utf8;
use open ':utf8';

use Attribute::Handlers;

our $cache_stats = {};

sub new{
    my $proto  = shift;
    my $class  = ref($proto) || $proto; #Класс, к которому приводим
    my $self = shift() || {};
    bless($self,$class);
    $self->init if $self->can('init');#Если есть функция инициализации, инициализируем
    $self->after_init if $self->can('after_init');
    return $self;
}

#Без проверки init - сокращает время создания новых объектов
sub new_lite{
    my $proto  = shift;
    my $class  = ref($proto) || $proto; #Класс, к которому приводим
    my $self = shift() || {};
    bless($self,$class);
    return $self;
}

sub log {
    my ($self, $error) = @_;
    print STDERR $error."\n";
};


our $dtdmp_fixed = 0;

sub _dtdmp_fix {
    print STDERR "_dtdmp_fix\n";
    $Data::Dumper::Useqq = 1;
    {
        no warnings 'redefine';
        sub Data::Dumper::qqoute {
            my $s = shift;
            return "'$s'";
        }
    }
    $dtdmp_fixed = 1;
    $Data::Dumper::Terse = 1;
    $Data::Dumper::Indent = 1;
#    $Data::Dumper::Toaster = 'dumper_text';
    $Data::Dumper::Freezer = 'dumper_text';
}

sub _dtdmp_fix_lite {
    print STDERR "_dtdmp_fix_lite\n";
    $Data::Dumper::Useqq = 1;
    {
        no warnings 'redefine';
        sub Data::Dumper::qqoute {
            my $s = shift;
            return "'$s'";
        }
    }
    $dtdmp_fixed = 2;
    $Data::Dumper::Terse = 1;
    $Data::Dumper::Indent = 1;
#    $Data::Dumper::Toaster = 'dumper_text';
    $Data::Dumper::Freezer = 'dumper_text_lite';
}

sub dump {
    my $self = shift;
    _dtdmp_fix unless $dtdmp_fixed == 1;
    return Dumper(@_);
}

sub dump_lite {
    my $self = shift;
    _dtdmp_fix_lite unless $dtdmp_fixed == 2;
    return Dumper(@_);
}

sub ___error {
    my ($self, $error) = @_;
    
    if (defined $error) {
        $self->{'__ERROR__'} = $error;
        return;
    };
    
    return $self->{'__ERROR__'};
};


sub sys_error {
    my $self = shift;
    my $text = shift;
    my ($package, $filename, $line, $subroutine, $hasargs, $wantarray) = caller(0);
    print STDERR '['.localtime().'] '."[error] $package, $filename, $line : $text\n";
    my $i = 0;
    while(++$i){
        my ($package, $filename, $line, $subroutine,$hasargs, $wantarray) = caller($i);
        last if $subroutine eq 'partner::handler';
        last if $subroutine eq '';
#        print STDERR "    -- $package, $filename, $line : $subroutine\n";
        print STDERR "    -- $package, $line - $subroutine\n";
    }
}

sub ___sys_error {
    my ($self, $text, $exit) = @_;
#    SysError($self, $text, $exit);
}

sub _memory_id {
    my $self = shift;
    return scalar(\$self);    
}

sub _stack_trace {
    my ($self) = @_;

    my $i = 1;
    my @st = ();
    while(++$i) {
        my ($package, $filename, $line, $subroutine, $hasargs,
            $wantarray, $evaltext, $is_require, $hints, $bitmask) = caller($i);

        last if !defined($package);
        
        push(@st, {
            package     => $package,
            filename    => $filename,
            line        => $line,
            subroutine  => $subroutine,
        });
    };
    return \@st;
};

sub stack_trace {
    my ($self, $pref) = @_;
    $pref = '    ' unless $pref;
    my $arr = $self->_stack_trace;
    #my $text = join "", map { $pref.$_->{package}." ".$_->{line}." => ".$_->{subroutine}."\n" } reverse @$arr;
    my $text = join "", map { $pref.$_->{subroutine}." <= ".$_->{package}." ".$_->{line}."\n" } reverse @$arr;
    return $text;
}

#Очистка кэшей
sub _delete_cached_inf {
    my ($self) = @_;
    delete($self->{$_}) for grep {/^_cached_/} keys %$self;
}
sub _set_cache {
    my $self = shift;
    my $method = shift;
    my $value = shift;
    
    $self->{"_cached_$method"} = [$value];
}


sub STATIC :ATTR(CODE) {
}
sub CACHE : ATTR(CODE) {
}

#Более быстрый вариант кэша, так как не предполагает возврата массива
sub SCACHE : ATTR(CODE) {
}

sub GLOBALCACHE : ATTR(CODE) {
}

sub GLOBALTIMECACHE : ATTR(CODE) {
}

sub TABLECACHE : ATTR(CODE) {
}

sub FILECACHE : ATTR(CODE) {
}

sub RPC_CACHE : ATTR(CODE) {
}
sub RUN_ONCE : ATTR(CODE) {
}

1;
