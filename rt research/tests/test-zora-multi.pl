#!/usr/bin/perl -w
use strict;
use utf8;
no warnings 'utf8';

use Getopt::Long;
use FindBin;
use lib "$FindBin::Bin/../lib";
use Utils::Common;
use Project;
use Data::Dumper;
use Time::HiRes qw/gettimeofday tv_interval/;

select STDERR; $| = 1;
select STDOUT; $| = 1;

my $proj = Project->new({load_dicts   => 0});

my @ulmart_urls = qw~
http://ulmart.ru/catalog/15000120
http://ulmart.ru/goods/3500288
http://ulmart.ru/goods/473714
http://ulmart.ru/goods/269186
http://ulmart.ru/goods/873976
http://ulmart.ru/goods/156232
http://ulmart.ru/goods/3417297
http://ulmart.ru/goods/92632
http://www.ulmart.ru/goods/570256
http://ulmart.ru/goods/886096
http://ulmart.ru/goods/924675
http://ulmart.ru/goods/270976
http://www.ulmart.ru/goods/3376331
http://ulmart.ru/goods/744240
http://ulmart.ru/goods/3425405
http://www.ulmart.ru/goods/3400894
http://ulmart.ru/goods/577106
http://ulmart.ru/goods/3473149
http://ulmart.ru/goods/3504769
http://www.ulmart.ru/goods/3504719
http://ulmart.ru/goods/3415338
http://ulmart.ru/goods/286905
http://ulmart.ru/goods/811009
http://ulmart.ru/goods/1030548
http://ulmart.ru/goods/876713
http://ulmart.ru/goods/3464334
http://ulmart.ru/goods/3458583
http://ulmart.ru/goods/586004
http://www.ulmart.ru/goods/3484982
http://ulmart.ru/goods/710882
http://ulmart.ru/goods/807251
http://www.ulmart.ru/goods/3470389
http://ulmart.ru/catalog/10191985
http://ulmart.ru/goods/685242
http://ulmart.ru/goods/3451705
http://ulmart.ru/goods/1006974
http://ulmart.ru/goods/231783
http://ulmart.ru/goods/3393358
http://ulmart.ru/goods/3475244
http://ulmart.ru/goods/686208
http://ulmart.ru/goods/945133
http://ulmart.ru/goods/874804
http://ulmart.ru/goods/898436
http://www.ulmart.ru/goods/324669
http://ulmart.ru/catalog/94867_432_95764
http://ulmart.ru/goods/852052
http://ulmart.ru/goods/3479094
http://ulmart.ru/goods/619104
http://ulmart.ru/goods/606696
http://ulmart.ru/goods/3442237
~;

my @eldorado_urls = qw~
http://eldorado.ru/cat/detail/71123077/?category=1120823
http://eldorado.ru/cat/detail/71091310/?category=1120264
http://eldorado.ru/cat/detail/71110130/?category=178401001
http://eldorado.ru/cat/detail/71118195/?category=232549279
http://eldorado.ru/cat/detail/71118324/?category=1674535
http://eldorado.ru/cat/detail/71083482/?category=1120756
http://eldorado.ru/cat/detail/71089131/?category=1120435
http://eldorado.ru/cat/detail/71113887/?category=1119922
http://eldorado.ru/cat/detail/71097123/?category=172601215
http://eldorado.ru/cat/detail/71117929/?category=1119850
http://eldorado.ru/cat/detail/71091507/?category=1120261
http://eldorado.ru/cat/detail/71086264/?category=1793393
http://eldorado.ru/cat/detail/71091194/?category=1121199
http://eldorado.ru/cat/detail/71101409/?category=2908528
http://eldorado.ru/cat/detail/71072542/?category=1120117
http://eldorado.ru/cat/detail/71078232/?category=18790795
http://eldorado.ru/cat/detail/71089430/?category=1120243
http://eldorado.ru/cat/detail/71120479/?category=173741390
http://eldorado.ru/cat/detail/71087849/?category=1613059
http://eldorado.ru/cat/detail/71086213/?category=1828056
http://eldorado.ru/cat/detail/71090570/?category=1946955
http://eldorado.ru/cat/detail/71121968/?category=217247880
http://eldorado.ru/cat/detail/71121389/?category=155619391
http://eldorado.ru/cat/detail/71092127/?category=1120036
http://eldorado.ru/cat/detail/71113384/?category=2960678
http://eldorado.ru/cat/detail/71096508/?category=171921445
http://eldorado.ru/cat/detail/71089315/?category=2908536
http://eldorado.ru/cat/172261844/
http://eldorado.ru/cat/detail/71100190/?category=1793393
http://eldorado.ru/cat/detail/71113065/?category=1120633
http://eldorado.ru/cat/detail/71093439/?category=1927234
http://eldorado.ru/cat/detail/71050424/?category=2558753
http://eldorado.ru/cat/detail/71121035/?category=256965368
http://eldorado.ru/cat/detail/71074737/?category=1121316
http://eldorado.ru/cat/detail/80000622/?category=1119850
http://eldorado.ru/cat/detail/71081994/?category=1613059
http://eldorado.ru/cat/detail/71114440/?category=186527553
http://eldorado.ru/cat/detail/71088207/?category=7135340
http://eldorado.ru/cat/detail/71101987/?category=1613059
http://eldorado.ru/cat/detail/71091509/?category=1119913
http://eldorado.ru/cat/detail/71100091/?category=167192917
http://eldorado.ru/cat/detail/71039300/?category=1120066
http://eldorado.ru/cat/detail/71103559/?category=4341217
http://eldorado.ru/cat/detail/71120928/?category=224734568
http://eldorado.ru/cat/detail/71091283/?category=1120264
http://eldorado.ru/cat/detail/71085543/?category=1120820
http://eldorado.ru/cat/detail/71096564/?category=175064030
http://eldorado.ru/cat/detail/71119543/?category=184005788
http://eldorado.ru/cat/detail/71110705/?category=177367550
http://eldorado.ru/cat/detail/71070383/?category=5101
~;

my @ozon_urls = qw~
http://ozon.ru/context/detail/id/24122256/
http://ozon.ru/context/detail/id/29352729/
http://ozon.ru/context/detail/id/32511372/
http://ozon.ru/context/detail/id/26685795/
http://ozon.ru/context/detail/id/26058401/
http://ozon.ru/context/detail/id/29034878/
http://ozon.ru/context/detail/id/28226286/
http://ozon.ru/context/detail/id/26464768/
http://ozon.ru/context/detail/id/32532566/
http://ozon.ru/context/detail/id/24720038/
http://ozon.ru/context/detail/id/32499901/
http://ozon.ru/context/detail/id/24931157/
http://ozon.ru/context/detail/id/8170979/
http://ozon.ru/context/detail/id/8166052/
http://ozon.ru/catalog/1146141/
http://ozon.ru/context/detail/id/32013493/
http://ozon.ru/context/detail/id/32434001/
http://ozon.ru/context/detail/id/26619247/
http://ozon.ru/context/detail/id/30599488/
http://ozon.ru/context/detail/id/31783644/
http://ozon.ru/context/detail/id/31709431/
http://ozon.ru/context/detail/id/30486380/
http://ozon.ru/context/detail/id/26247957/
http://ozon.ru/context/detail/id/31477958/
http://ozon.ru/context/detail/id/31453347/
http://ozon.ru/context/detail/id/31698642/
http://ozon.ru/context/detail/id/29575497/
http://ozon.ru/context/detail/id/28137338/
http://ozon.ru/context/detail/id/31677683/
http://ozon.ru/context/detail/id/31594067/
http://ozon.ru/context/detail/id/31738845/
http://ozon.ru/context/detail/id/32641024/
http://ozon.ru/context/detail/id/18573340/
http://ozon.ru/context/detail/id/31316445/
http://ozon.ru/context/detail/id/7244102/
http://ozon.ru/context/detail/id/32202758/
http://ozon.ru/context/detail/id/28875253/
http://ozon.ru/context/detail/id/18580643/
http://ozon.ru/context/detail/id/27539318/
http://ozon.ru/context/detail/id/19126402/
http://ozon.ru/context/detail/id/25804841/
http://ozon.ru/context/detail/id/24899127/
http://ozon.ru/context/detail/id/32388352/
http://ozon.ru/context/detail/id/32033439/
http://ozon.ru/context/detail/id/32404388/
http://ozon.ru/context/detail/id/26464994/
http://ozon.ru/context/detail/id/24090540/
http://ozon.ru/context/detail/id/32412728/
http://ozon.ru/context/detail/id/7165244/
http://ozon.ru/context/detail/id/25618720/
http://ozon.ru/context/detail/id/31480496/
http://ozon.ru/context/detail/id/8167655/
http://ozon.ru/context/detail/id/19943651/
http://ozon.ru/context/detail/id/27540443/
http://ozon.ru/context/detail/id/31646093/
http://ozon.ru/context/detail/id/26396932/
http://ozon.ru/context/detail/id/30746517/
http://ozon.ru/context/detail/id/6374518/
http://ozon.ru/context/detail/id/31914488/
http://ozon.ru/context/detail/id/32406354/
http://ozon.ru/context/detail/id/32129640/
http://ozon.ru/context/detail/id/31597389/
http://ozon.ru/context/detail/id/32704883/
http://ozon.ru/context/detail/id/31954366/
http://ozon.ru/context/detail/id/32683565/
~;

my @vseinstrumenty = qw~
http://vseinstrumenti.ru/sadovaya_tehnika/systemy_poliva_i_orosheniya/shlangi/armirovannye/fitt/sadovyi_pvh_idro_mat_1_2_50_m_7023350/
http://karcher.vseinstrumenti.ru/uborka/pylesosy/professionalnye/dlya_chistki_kovrov/kommercheskiy_pilesos_karcher_cv_301/
http://vseinstrumenti.ru/avtogarazhnoe_oborudovanie/avtomobilnye_aksessuary/kovriki/bmw/novline/3_f30_akpp_2012_sed_4_sht_bezhevye_nlt.05.35.12.112kh/
http://vseinstrumenti.ru/ruchnoy_instrument/sadoviy_instrument_i_inventar/obrabotka_pochvy/tyapki/sibrteh/radiusnaya_motyga_190_h_150_mm._matrix_62365/
http://vseinstrumenti.ru/ruchnoy_instrument/otvertki/ploskie/wera/otvertka_335_0.8x4.0x200_mm_wera_we-110006/
http://vseinstrumenti.ru/rashodnie_materialy/instrument/dlya_dreley/patrony/ataka/ataka_patron_9_0_8-10mm_1_2_bzp_standart_7088690/
http://vseinstrumenti.ru/electrika_i_svet/rozetki_i_vykljuchateli/volsten/dvuhklavishnyi_vyklyuchatel_volsten_v01-32-v22-s._9853/
http://makita.vseinstrumenti.ru/rashodnie_materialy/instrument/dlya_shlifmashin/uglovyh_bolgarok/diski_i_krugi/shlifovalnye/disk_lepestkovyi.f180h22mm.k80.drovn_pov.dder.met.plast_makita_d-27355/
http://www.vseinstrumenti.ru/ruchnoy_instrument/izmeritelnyj/urovni_stroitelnye/truper/stroitelnyj_uroven_truper_np-18_17034/
http://dewalt.vseinstrumenti.ru/rashodnie_materialy/k_stankam/k_lentochnopilnym_stankam/polotna/polotno_pilnoe_dewalt_dt8476/
http://vseinstrumenti.ru/ruchnoy_instrument/malyarnyj/kisti/stayer/ploskaya_kist_stayer_aqua-kanekaron_0106-075/
http://vseinstrumenti.ru/silovaya_tehnika/stabilizatory_napryazheniya/odnofaznye/do_10_kvt/elitech/stabilizator_elitech_asn_2000rn/
http://vseinstrumenti.ru/ruchnoy_instrument/stolyarno-slesarnyi/rezbonareznoy/plashki/sibrteh/matrix_plashka_m5_h_0.8_mm___sibrteh_77013/
http://vseinstrumenti.ru/avtogarazhnoe_oborudovanie/smazochnoe_i_zapravochnoe/toplivnye_nasosy/komplektuyuschie/groz/shlang_300_mm_400_atm_1_8_dlya_smazochnyh_shpritsev_groz_gr43700/
http://vseinstrumenti.ru/spetsodezhda/siz/organov_zreniya/ochki_zaschitnye/rosomz/rosomz_ochki_zaschitnye_zakrytye_s_pryamoj_ventilyatsiej_zp8_etalon_super_5_sa_30838/
http://vseinstrumenti.ru/ruchnoy_instrument/klyuchi/treschotki_golovki_i_vorotki/golovki_i_nabory_golovok/delo_tehniki/delo_tehniki_golovka_so_vstavkoy_1_2_spline_m8_l55mm_dt_200_10_625408/
http://vseinstrumenti.ru/rashodnie_materialy/instrument/dlya_perforatorov_i_otboynyh_molotkov/bury/sds-max/prorab/bur_s_posadkoy_sds_max_12x260_prorab_3402/
http://vseinstrumenti.ru/klimat/obogrevateli/konvektory/elektricheskie/thermor/evidence_2_elec_2500/
http://vseinstrumenti.ru/rashodnie_materialy/dlya_silovogo_oborudovaniya/dlya_svarochnyh_rabot/prochie_aksessuary/duga/rukav_gazovyi_chernyi_iii_klass_9.0_mm_buhta_40_m_/
http://vseinstrumenti.ru/rashodnie_materialy/klimaticheskoe_oborudovanie/vodoprovod/fitingi/polipropilenovie/troyniki/firat/firat_troynik_perehodnoy_25_h_20_h_25_7b42252025/
http://vseinstrumenti.ru/silovaya_tehnika/skladskoe_oborudovanie/shtabelery/ruchnye_gidravlicheskie_s_elektropodemom/lema/samohodnyi_shtabeler_lema_lm-el_1530_1041530/
http://vseinstrumenti.ru/ruchnoy_instrument/klyuchi/kombinirovannye/matrix/kombinirovanyi_fosfatirovannyi_klyuch_17_mm_matrix_sibrteh_14911/
http://vseinstrumenti.ru/rashodnie_materialy/k_stankam/frezernym/frezy/stayer/frezy_stayer_po_derevu_hvostovik_6_mm_._v_nabore_12sht_2992-h12/
http://www.vseinstrumenti.ru/silovaya_tehnika/generatory_elektrostantsii/dizelnye/mobilnye/spets/dizelnyi_generator_spets_sd-2000e/
http://dewalt.vseinstrumenti.ru/instrument/akkumulyatorniy_instrument/akkumulyatori/akkumulyatornye_bloki/dewalt_akkumulyator._14.4v.3.0achli-ion._0.53kg._xr-seriya_dcb_140/
http://vseinstrumenti.ru/sadovaya_tehnika/snegouborochnaya_tehnika/benzinovye_snegouborschiki/mega/
http://vseinstrumenti.ru/electrika_i_svet/el_mont_prod/shkaf_schit_bok/boksy/schneider_electric/boks_2_ryada_24_modulya_ip_65_schneider_electric_kaedra_13983/
http://makita.vseinstrumenti.ru/rashodnie_materialy/k_stankam/frezernym/frezy/po_derevu/makita_freza_pazovaya.1lezvie._hv-8mm._f3mm._dlina-8mm_d-10001/
http://bosch.vseinstrumenti.ru/rashodnie_materialy/instrument/dlya_shlifmashin/uglovyh_bolgarok/diski_i_krugi/shlifovalnye/krug_lepestk_115mm_k60_best_for_metal_bosch_2608607335/
http://vseinstrumenti.ru/stanki/gibochnye/listogibochnye/proma/uo-120_25100101/
http://vseinstrumenti.ru/instrument/pnevmoinstrument/pnevmosteplery/novus/stepler_novus_j328ec_032-0034/
http://vseinstrumenti.ru/electrika_i_svet/byt_osv/lampy/energosberegajuschie/gauss/spiral_26w_2700k_e27_212126/
http://vseinstrumenti.ru/rashodnie_materialy/instrument/dlya_shlifmashin/uglovyh_bolgarok/diski_i_krugi/otreznye/stayer/stayer_krug_otreznoj_abrazivnyj_po_metallu_200mmh2_5mmh22_2mm_36220-200-2.5_z01/
http://vseinstrumenti.ru/rashodnie_materialy/instrument/dlya_shlifmashin/uglovyh_bolgarok/diski_i_krugi/shlifovalnye/fit/fit_it_disk_nazhdachnyi_lepestkovyi_._115_mm__r80___39544/
http://www.vseinstrumenti.ru/instrument/dreli/udarnye/interskol/du-1000er/
http://vseinstrumenti.ru/ruchnoy_instrument/nozhovki/po_derevu/zubr/nozhovka_po_derevu_zubr_master_meteor_1537-40/
http://vseinstrumenti.ru/ruchnoy_instrument/klyuchi/treschotki_golovki_i_vorotki/golovki_i_nabory_golovok/jonnesway/jonnesway_s04h4925_tortsevaya_golovka_1_2dr_12-gr._25_mm/
http://vseinstrumenti.ru/electrika_i_svet/el_mont_prod/din_reyki/
http://vseinstrumenti.ru/avtogarazhnoe_oborudovanie/smazochnoe_i_zapravochnoe/toplivnye_nasosy/dlya_dizelnogo_topliva/piusi/nasos_piusi_carry_3000_inline_12_v_f00223260/
http://vseinstrumenti.ru/electrika_i_svet/rozetki_i_vykljuchateli/volsten/dvuhklavishnyi_vyklyuchatel_volsten_v01-15-v21-m__magenta_argento._10028/
http://vseinstrumenti.ru/rashodnie_materialy/instrument/dlya_gaykovertov_vintovertov_i_shurupovertov/nasadki/nabory_bit/interskol/interskol_otvertka-nasadka_krestoobraznaya_phillips_rn1_50mm_5sht__2041405000010/
http://makita.vseinstrumenti.ru/rashodnie_materialy/instrument/dlya_dreley/udarn/sverla/po_derevu/sverlo_ddereva._24x450mm.1sht.hv-tsilindr.spiralnoe_makita_d-07630/
http://vseinstrumenti.ru/sadovaya_tehnika/opryskivateli/rancevye/grinda/clever_spray_8-425155/
http://vseinstrumenti.ru/spetsodezhda/sumki_kejsy/chehly_organajzery_dlya_instrumenta/energomash/sumka_dlya_instrumentov_energomash_tb21201e/
http://bosch.vseinstrumenti.ru/instrument/shlifmashiny/bolgarka_ushm/bosch_gws_15-125_cie_0.601.796.002/
http://vseinstrumenti.ru/klimat/obogrevateli/konvektory/elektricheskie/polaris/pmh_2081/
http://vseinstrumenti.ru/ruchnoy_instrument/klyuchi/treschotki_golovki_i_vorotki/golovki_i_nabory_golovok/jonnesway/jonnesway_s04hd4108_tortsevaya_golovka_glubokaya_1_2dr._8_mm/
http://vseinstrumenti.ru/krepezh/dlya_gvozdezabivateley_steplerov/zaklepki/fit/zaklepka_vytyazhnaya_alyuminievaya_50_sht_4.0h10_mm_fit_it_23740/
http://vseinstrumenti.ru/krepezh/dyubeli/raspornye/sormat/dyubel_50sht_10_sor_75010/
http://lestnicy.vseinstrumenti.ru/trehsektsionnye/obschego_naznacheniya/krause/corda_3h10_013408/
~;

my @avito_urls = qw~
https://www.avito.ru/moskva?q=телевизор1
https://www.avito.ru/moskva?q=телевизор2
https://www.avito.ru/moskva?q=телевизор3
https://www.avito.ru/moskva?q=телевизор4
https://www.avito.ru/moskva?q=телевизор5
https://www.avito.ru/moskva?q=телевизор6
https://www.avito.ru/moskva?q=телевизор7
https://www.avito.ru/moskva?q=телевизор8
https://www.avito.ru/moskva?q=телевизор9
https://www.avito.ru/moskva?q=телевизор10
~;

my @ezhik_urls = qw~
http://eldorado.ru/cat/detail/71116447/?category=219154535
http://eldorado.ru/cat/detail/71116461/?category=219154535
http://eldorado.ru/cat/detail/71116448/?category=219154535
http://eldorado.ru/cat/detail/71116491/?category=219154535
~;


#my @urls = ( sort { rand() <=> rand() } @ulmart_urls )[ 0 .. 4 ];
#my @urls = @ezhik_urls;
my @urls = qw~
    http://www.mvideo.ru/online-credit
    http://www.mvideo.ru/pylesosy/avtomobilnye-pylesosy-2444?cityId=CityCZ_6267'
~;
my $start = [gettimeofday];

#my $result = $proj->zora_client->multi_get_extras([ @urls ]);
my $result = $proj->zora_client->multi_get_hashref([ @urls ], {timeout => 14400});

for my $url ( keys %$result ) {
    my $content = $result->{$url}->{content};
    print join("\t",
        "url:" . $url,
        "size:" . Dumper( length($content) ),
    ) . "\n";
}
$proj->log("rate:" . int(scalar(@urls)/tv_interval($start) + 0.5) . " urls per sec");
$proj->log("zora done");

exit(0);

1;
