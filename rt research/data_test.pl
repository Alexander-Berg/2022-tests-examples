#!/usr/bin/perl -w
#тестирование категорий на сложность входящих фраз

use strict;
use utf8;
use open ':utf8';
no warnings 'utf8';
use Data::Dumper;

binmode(STDIN,  ":utf8");
binmode(STDOUT, ":utf8");
binmode(STDERR, ":utf8");

my $ctg_count = 0; #общее число категорий
my $dup_total = 0; #в скольких категориях встречаются дубли
my $per_total = 0; #суммарный общий процент дублей среди $dup_total

while (<STDIN>) { #~/arcadia/rt-research/broadmatching/dicts/caddphr_web_ru (/opt/broadmatching/dicts/caddphr_web_ru)
    chomp;

    my @a = split /\t/; #последнее поле - список фраз (comma-delimited)
    $ctg_count++;

    if ($a[$#a] =~ /\[/) {
        my @b = split /,/, $a[$#a];
        my $ctg = join("\t", @a[0..$#a-1]);
        print join("\t", @a[0..$#a-1]), "\tNUM=", @b+0, "\n";
    }
    #print "$_\n" if $a[$#a] =~ /\[/; #есть сложные фразы
}


#--- проверка вхождения лексиконов фраз ---
sub lex_incl {
    my ($ctg, $a, $b, $phr_lst) = @_;

    my @phrs = ();
    @phrs = split /,/, $phr_lst if $phr_lst;
    print STDERR ">$ctg: ", @phrs+0, "\n";
    return () if @phrs == 0;
    for (@phrs) { #чистка
        s/^ +//;
        s/ +$//;
        s/  +/ /g;
    }

    my $dup_count = 0;
    for my $i (0..$#phrs) {
        my @wrdsI = phr_wrds($phrs[$i]); #получение списка "слов" фразы
        for my $j (0..$#phrs) {
            next if $i == $j;
            my @wrdsJ = phr_wrds($phrs[$j]);

            #-----------------------------------------------------
            next unless dic_incl(\@wrdsJ, \@wrdsI); #проверка 1-го и 4-го необходимого условия вхождения $phrs[$j] в $phrs[$i]
            #-----------------------------------------------------

            my ($minI, $maxI) = phr_degree(\@wrdsI); #вычисление степеней фразы
            my ($minJ, $maxJ) = phr_degree(\@wrdsJ);
            if ($minJ >= $minI && $maxJ <= $maxI) { #проверка 2-го и 3-го необходимых условий вхождения $phrs[$j] в $phrs[$i]
                $dup_count++;

                print "$ctg\t$a\t$b\t$i\t$phrs[$i]\n";
                print "$ctg\t$a\t$b\t$j\t$phrs[$j]\n";
                print "--\n";
            }
        }
    }

    my $phr_count = @phrs+0;
    my $frac = $dup_count / $phr_count;
    if ($frac > 0) {
        $dup_total++;
        $per_total += $frac;
        my $per_avg = sprintf("%.2f%%", 100 * $per_total / $dup_total); #средний % дублей

        $frac = sprintf("%.2f%%", 100 * $frac);
        print STDERR "КАТЕГОРИЯ: $ctg\tФРАЗ: ", $phr_count, "\tДУБЛЕЙ: $dup_count\tПРОЦЕНТ: $frac\tПРОЦ_СРЕД: $per_avg\n";
    }
}


# --- 1-е и 4-е необходимое условие вхождения одной фразы $a в другую $b ---
sub dic_incl {
    my ($a, $b) = @_; #списки "слов" (массивы) сравниваемых фраз

    my (%h, %m); #m - хеш минус-слов
    for my $i (0..$#{$b}) {
        for (get_wrds($$b[$i])) {
            next unless $_;
            if (!/^-/) {
                $h{lc($_)} = 1;
            } else {
                $m{lc($_)} = 1;
            }
        }
    }

    for my $i (0..$#{$a}) {
        for (get_wrds($$a[$i])) {
            next unless $_;
            if (!/^-/) {
                return 0 unless $h{lc($_)};
            } else {
                return 0 unless $m{lc($_)};
            }
        }
    }

    return 1;
}


# --- определение min и max степеней фразы ---
sub phr_degree {
    my ($a) = @_; #список "слов" фразы

    my ($min, $max) = (0, 0);
    for my $i (0..$#{$a}) {
        next if $$a[$i] =~ /^-/; #минус-слово
        $min++ unless $$a[$i] =~ m{/\]$}; #неоднородный анонимный атом
        $max++;
    }

    return ($min, $max);
}


#########################################

#--- оптимизациия фраз в категории ---
sub phr_opt {
    my ($ctg, $phr_lst) = @_;

    my @phrs = split /,/, $phr_lst;
    for (@phrs) { #чистка
        s/^ +//;
        s/ +$//;
        s/  +/ /g;
    }

    my $flag = 1;
    for my $i (0..$#phrs) {
        next if $phrs[$i] =~ /^\*/; #фраза $phrs[$i] уже включена в какую-то фразу
        my @wrdsI = phr_wrds($phrs[$i]); #получение списка "слов" фразы
        for my $j (0..$#phrs) {
            next if $i == $j;
            next if $phrs[$j] =~ /^\*/; #фраза $phrs[$j] уже включена в какую-то фразу
            my @wrdsJ = phr_wrds($phrs[$j]);

            #-----------------------------------------------------
            next unless nec_cond(\@wrdsJ, \@wrdsI); #проверка необходимого условия вхождения $phrs[$j] в $phrs[$i]
            #-----------------------------------------------------

            if ($flag == 1) {
                $flag = 0;
                print STDERR "\n*КАТЕГОРИЯ: '$ctg'\n\n"; #категория
            }
            print STDERR "$i\t$phrs[$i]\n";
            print STDERR "$j\t$phrs[$j]\n";
            print STDERR "--\n";

            $phrs[$j] = "*$phrs[$j]";
        }
    }

    my @tmp;
    for (@phrs) { #фильтр более мелких подфраз
        push @tmp, $_ unless /^\*/;
    }
 
    my @phrs_num = split(/,/, $phr_lst); #исходное число фраз
    if ($flag == 0) { #были удаления фраз
        print STDERR "ЧИСЛО ФРАЗ: ДО=", @phrs_num+0, ", ПОСЛЕ=", @tmp+0, "\n";
    }

    return @tmp;
}


# --- необходимое условие вхождения одной фразы в другую ---
sub nec_cond {
    my ($a, $b) = @_; #списки "слов" (массивы) сравниваемых фраз

    my @a = del_com($a, $b); #удаление из @$a элементов, общих с @$b
    my @b = del_com($b, $a);

    @a = ("") unless @a;
    @b = ("") unless @b;

    my %hash; #отображение "слов" фразы @a в "слова" фразы @b
    for my $i (0..$#a) {
        my $key = $i + 1;
        for my $j (0..$#b) {
            if (wrds_incl($a[$i], $b[$j])) { #"слово" $a[$i] входит в слово $b[$j]
                my $value = $j + 1;
                push @{$hash{$key}}, $value;
            }
        }
    }
    return 0 if scalar keys %hash < @a; #"слова" @b не покрывают @a

    my @num_comb;
    for (sort keys %hash) { #####
        @num_comb = cart_prod(\@num_comb, \@{$hash{$_}}); #номера слов @b, ВОЗМОЖНО покрывающие @a
    }

    my $hom = 0; #число однородных атомов
    my %n;
    for my $nums (@num_comb) {
        my @n = split / /, $nums;
        %n = ();
        $n{$_}++ for @n; #распознанные РАЗНЫЕ номера
        next if scalar keys %n < @a; #ДАННАЯ комбинация "слов" @b не покрывают @a

        $hom = 0; #число однородных атомов среди "слов" @b, не участвующих в покрытии
        for my $i (0..$#b) {
            my $key = $i + 1;
            next if $n{$key};
            $hom++ unless $b[$i] =~ m{\/\]};
        }
        last if $hom == 0;
    }
    return 0 if scalar keys %n < @a; #"слова" фразы @b не покрывают фразу @a
    return 0 if $hom; #однородные атомы @b не позволяют выполнить покрытие @a

    return 1;
}


#--- проверка вхождения "слова" $a в слово $b ---
sub wrds_incl {
    my ($a, $b) = @_; #"слова" из разных фраз

    my @a = get_wrds($a);
    my @b = get_wrds($b);

    @a = ("") unless @a;
    @b = ("") unless @b;

    my %h;
    $h{$_} = 1 for @b;
    for (@a) {
        return 0 unless $h{$_};
    }
    return 1;
}


#--- получение списка слов атома ---
sub get_wrds {
    my ($a) = @_;

    $a =~ s/^\[//;
    $a =~ s/\]$//;
    $a =~ s/^ +//;
    $a =~ s/ +$//;
    my @a = split m{/}, $a;
    push @a, "" if $a =~ m{/$};

    return @a;
}


#--- удаление из массива @a элементов, общих с @b (с учетом количества) ---
sub del_com {
    my ($a, $b) = @_;

    my %h;
    $h{$_}++ for @$b;

    my @a;
    for (@$a) {
        unless ($h{$_}) {
            push @a, $_;
        } else {
            $h{$_}--;
            delete $h{$_} if $h{$_} == 0;
        }
    }

    return @a;
}


#--- генерация полного списка простых фраз ---
sub wrds_comb {
    my ($wrds) = @_; #список "слов" фразы

    my @wrds_comb; # массив всевозможных комбинаций слов и словосочетаний фразы
    for my $wrd (@$wrds) {
        my @tmp;
        if ($wrd =~ /^\[/) { #атом
            $wrd =~ s/^\[//;
            $wrd =~ s/\]$//;
            $wrd =~ s/^ +//;
            $wrd =~ s/ +$//;
            @tmp = split m{/}, $wrd;
            push @tmp, "" if $wrd =~ m{/$};
        } else { #не атом
            push @tmp, $wrd;
        }
        @wrds_comb = cart_prod(\@wrds_comb, \@tmp);
    }

    for my $wrds_comb (@wrds_comb) {
        $wrds_comb =~ s/^ +//;
        $wrds_comb =~ s/ +$//;
        $wrds_comb =~ s/  +/ /g;

        $wrds_comb = join(" ", sort (phr_wrds($wrds_comb)));
    }

    return @wrds_comb;
}


#--- получение списка "слов" фразы ---
sub phr_wrds {
    my ($phr) = @_;

    #проверка и исправление синтаксиса
    if ($phr =~ /((?<=[^ ])([\[\{])|([\]\}])(?=[^ ]))/ || $phr =~ /((?<=[^ \/\[-])<|>(?=[^ \/\]]))/) { #синтаксическая ошибка - нет ' ' слева от '[{' или справа от ']}' ИЛИ нет ' /[-' слева от '<' или ' /]' справа от '>'
        $phr =~ s/(?<=[^ ])([\[\{])/ $1/g; #вставка пробела слева от '[{'
        $phr =~ s/([\]\}])(?=[^ ])/$1 /g; #вставка пробела справа от ']}'

        $phr =~ s/(?<=[^ \/\[-])</ </g; #вставка пробела слева от '<'
        $phr =~ s/>(?=[^ \/\]])/> /g; #вставка пробела справа от '>'
    }
    $phr =~ s{\\}{\/}g if $phr =~ m{\\};

    return phr_parse($phr); #список "слов" фразы
}


#--- парсинг строки с фразами ---
sub phr_parse {
    my ($phr) = @_;

    my @wrds; # массив слов и словосочетаний фразы
    while ($phr =~ m!(\[[^\]]+\]|<[^>]+>|\{[^\}]+\}|[^ ]+(?= |$))!g) { #[.Производители техники/Разработчики/] led-телевизор [samsung/loewe/philips/toshiba/hitachi/orion/grundig/lg] <плеер pioneer> dv {Супермаркеты электроники и бытовой техники} 610 av
        push @wrds, $1;
    }

    return @wrds;
}


#--- декартово произведение двух множеств ---
sub cart_prod {
    my ($a, $b, $DEL) = @_;
    $DEL = " " unless $DEL; #разделитель "слов"

    my @prod;
    unless (@$a) {
        @prod = @$b; 
    } else {
        for my $a (@$a) {
            push @prod, $a ? "$a$DEL$_" : $_ for @$b;
        }
    }

    return @prod;
}
