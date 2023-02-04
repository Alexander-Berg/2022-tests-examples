package ObjLib::ProjPart;

# Абстрактный класс, от которого должны наследовать модули проекта
# Реализует доступ к объекту проекта

use std;
use base qw(ObjLib::Obj);
use Encode qw{_utf8_on};

use Utils::Sys; # для md5
use Utils::Common;

use Storable;

__PACKAGE__->mk_accessors(qw(
    work_dir
));

########################################################
#Конструктор
########################################################

#Переопределяем, добавляя фичу
sub new {
    my $self = ObjLib::Obj::new(@_);
    # переставляем ссылку на proxy_ref
    my $proj = $self->{proj};
    $self->{proj_proxy_ref} = $proj->proxy_ref;
    delete $self->{proj};

    # default для метода lang
    $self->{proj_current_lang} = $proj->current_lang;

    return $self;
}

sub new_lite {
    my $proto = shift;
    my $self = shift;
    my $class  = ref($proto) || $proto; #Класс, к которому приводим
    bless($self, $class);
    return $self;
}

########################################################
#Доступ к полям
########################################################

sub proj {
    my $self = shift;
    if ($self->{proj}) {  # в ObjLib::Obj::new вызывается init, там ещё proj, а не proxy_ref
        return $self->{proj};
    } else {
        my $ref = $self->{proj_proxy_ref};
        return $$ref;
    }
}

sub log {
    my ($self, $error) = @_;
    $self->proj->log($error);
};

sub lang {
    my $self = shift;
    if (@_) {
        $self->{lang} = $_[0];
    }
    return $self->{lang} // $self->{proj_current_lang};
}

sub language {
    my $self = shift;
    return $self->{language} //= $self->proj->get_language($self->lang);
}

sub do_sys_cmd {
    my $self = shift;
    $self->proj->do_sys_cmd(@_);
}

sub do_sys_cmd_bash {
    my $self = shift;
    $self->proj->do_sys_cmd_bash(@_);
}

sub read_sys_cmd {
    my $self = shift;
    return $self->proj->read_sys_cmd(@_);
}

sub temp_dir {
    my $self = shift;
    return $self->{temp_dir} || $self->proj->temp_dir;
}

sub get_tempfile {
    my $self = shift;
    my $name = shift || 'projpart_tmp';
    my %par  = (
        DIR => $self->temp_dir,
        @_,
    );
    return $self->proj->get_tempfile($name, %par);
}

sub histinf {
    my $self = shift;
    return $self->{'histinf'};    
}

sub set_histinf {
    my ($self, $dt) = @_;
    $self->{'histinf'} = $dt;    
}

sub add_histinf {
    my ($self, $dt) = @_;
    $self->{'histinf'} .= $self->{'histinf'} ? "->$dt" : $dt;
}

#Причина фильтрации
sub fltrsn {
    my $self = shift;
    return $self->{'fltrsn'};    
}

sub add_fltrsn {
    my ($self, $dt) = @_;
    $self->{'fltrsn'} .= $self->{'fltrsn'} ? ",$dt" : $dt;
}

sub TIMELOG : ATTR(CODE) {
}

sub KYOTOCACHE : ATTR(CODE) {
}

# органичения на импользования этого атрибута
# 1. ПЕРВЫЙ параметр - ссылка на массив скаляров
# 2. РЕЗУЛЬТАТ функции - ссылка на хэш, в котором ключи - это скалярные значения из первого параметра
sub KYOTOCACHEHASHREF : ATTR(CODE) {
}


sub REMOTECACHE : ATTR(CODE) {
}

sub REMOTECACHELIST : ATTR(CODE) {
}

sub LANG : ATTR(CODE) {
}


sub FLTR : ATTR(CODE) {
}

sub PACKETIZE : ATTR(CODE) {
}

sub EXTERNALLY_USED : ATTR(CODE) {
}

sub can_be_externally_used {
    ...;
}

sub LRUCACHE : ATTR(CODE) {
}

sub LRUCACHELIST : ATTR(CODE) {
}

1;
