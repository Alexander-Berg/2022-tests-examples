package ru.yandex.payments.fnsreg;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.hubspot.jinjava.Jinjava;
import lombok.Builder;
import lombok.val;

import ru.yandex.payments.fnsreg.dto.ApplicationVersion;
import ru.yandex.payments.fnsreg.dto.Kkt;
import ru.yandex.payments.fnsreg.dto.ReregistrationInfo.Change;
import ru.yandex.payments.fnsreg.types.AutomatedSystemNumber;
import ru.yandex.payments.fnsreg.types.FiasCode;
import ru.yandex.payments.fnsreg.types.Inn;
import ru.yandex.payments.fnsreg.types.ModelName;
import ru.yandex.payments.fnsreg.types.Ogrn;
import ru.yandex.payments.fnsreg.types.RegistrationNumber;
import ru.yandex.payments.fnsreg.types.SerialNumber;
import ru.yandex.payments.fnsreg.types.Sono;
import ru.yandex.payments.fnsreg.types.Withdraw;

import static java.util.Collections.unmodifiableMap;
import static java.util.Map.entry;

@Builder
class TestAppGenerator {
    private static final Jinjava JINJAVA = new Jinjava();

    // CHECKSTYLE:OFF
    // language=XML
    private static final String REG_TEMPLATE =
            """
            {%- set YES = 1 -%}
            {%- set NO = 2 -%}
            <?xml version='1.0' encoding='utf-8'?>
            <Файл ВерсПрог="1.0" ВерсФорм="{{ version }}" ИдФайл="KO_ZVLREGKKT_{{ firm_sono_initial }}_{{ firm_sono_target }}_{{ firm_inn }}{{ firm_kpp }}_19700101_{{ kkt_sn_pure }}">
                <Документ ДатаДок="01.01.1970" КНД="1110061" КодНО="{{ firm_sono_target }}">
                    {% if is_invalid_schema %} <!-- {% endif %}<СвНП>{% if is_invalid_schema %} --> {% endif %}
                        <НПЮЛ ИННЮЛ="{{ firm_inn }}"
                              КПП="{{ firm_kpp }}"
                              НаимОрг="{{ firm_name | e }}"
                              ОГРН="{{ firm_ogrn }}"/>
                    {% if is_invalid_schema %} <!-- {% endif %}</СвНП>{% if is_invalid_schema %} --> {% endif %}
                    <Подписант ПрПодп="{%if signer_document is defined %}2{% else %}1{% endif %}">
                        <ФИО Имя="{{ signer_first_name }}"
                             {% if signer_middle_name is defined %}
                                Отчество="{{ signer_middle_name }}"
                             {% endif %}
                             Фамилия="{{ signer_last_name }}"/>
                             {% if signer_document is defined %}
                                <СвПред НаимДок="{{ signer_document }}"/>
                             {% endif %}
                    </Подписант>
                    <!-- КодНОМУст="{{ address_sono }}" -->
                    <ЗаявРегККТ ВидДок="{% if is_reregistration %}2{% else %}1{% endif %}"
                                {% if is_reregistration %}
                                    ПрАвтономРежим="{{ change_autonomous_mode }}"
                                    ПрЗамФН="{{ change_fn }}"
                                    ПрИзмАвтУстр="{{ change_auto_sys }}"
                                    ПрИзмАдрМУ="{{ change_address }}"
                                    ПрИзмНаимНП="{{ change_org_name }}"
                                    ПрИныеПричины="{{ change_other }}"
                                    ПрСменОФД="{{ change_ofd }}"
                                    ПрЭлектрРежим="{{ change_std_mode }}"
                                    РегНомерККТ="{{ prev_reg_number }}"
                                {%- endif -%}
                    >
                        <СведРегККТ ЗаводНомерККТ="{{ kkt_sn }}"
                                    ЗаводНомерФН="{{ fn_sn }}"
                                    МоделККТ="{{ kkt_model_name | e }}"
                                    МоделФН="{{ fn_model_name | e }}"
                                    ПрАвтоматУстр="{% if kkt_automated_sys_number is defined %}{{ YES }}{% else %}{{ NO }}{% endif %}"
                                    {% if version == '5.05' and (!is_reregistration or marked_goods_usage is defined) %}
                                        ПрРасчМарк="{% if marked_goods_usage %}{{ YES }}{% else %}{{ NO }}{% endif %}"
                                        {% if ffd_version is defined %}
                                            НомерВерсФФД="{{ ffd_version }}"
                                        {% endif %}
                                    {% endif %}
                                    ПрАвтоном="{{ kkt_autonomous_mode }}"
                                    ПрАзарт="{{ kkt_gambling_mode }}"
                                    ПрИгорнЗавед="2"
                                    ПрАкцизТовар="{{ kkt_excise_mode }}"
                                    ПрБанкПлат="2"
                                    ПрБланк="{{ kkt_blank_mode }}"
                                    ПрИнтернет="{{ kkt_internet_mode }}"
                                    ПрЛотерея="{{ kkt_lottery_mode }}"
                                    ПрПлатАгент="{{ kkt_agent_mode }}"
                                    ПрРазвозРазнос="{{ kkt_transport_mode }}">
                            <СведОФД ИННЮЛ="{{ ofd_inn }}" НаимОрг="{{ ofd_name | e }}"/>
                            <СведАдрМУст НаимМУст="{{ firm_url }}">
                                <АдрМУстККТ>
                                    <АдрФИАС ИдНом="{{ address_fias }}" Индекс="{{ address_post_code }}">
                                        <Регион>{{ address_region_code }}</Регион>
                                        <МуниципРайон ВидКод="{{ address_municipality_region_code }}" Наим="{{ address_municipality_region_name }}"/>
                                        {% if address_city_name is defined %}
                                            <НаселенПункт Наим="{{ address_city_name }}" Вид="город"/>
                                        {%- endif -%}
                                        {% if address_street_name is defined %}
                                            <ЭлУлДорСети Наим="{{ address_street_name }}" Тип="улица"/>
                                        {%- endif -%}
                                        {% if address_home is defined %}
                                            <Здание Номер="{{ address_home }}" Тип="дом"/>
                                        {%- endif -%}
                                    </АдрФИАС>
                                </АдрМУстККТ>
                            </СведАдрМУст>
                            {% if kkt_automated_sys_number is defined %}
                                <СведАвтУстр НомерАвтоматУстр="{{ kkt_automated_sys_number }}" НаимМУст="{{ firm_url }}">
                                    <АдрМУстАвтУстр>
                                        <АдрФИАС ИдНом="{{ address_fias }}" Индекс="{{ address_post_code }}">
                                            <Регион>{{ address_region_code }}</Регион>
                                            <МуниципРайон ВидКод="{{ address_municipality_region_code }}" Наим="{{ address_municipality_region_name }}"/>
                                            {% if address_city_name is defined %}
                                                <НаселенПункт Наим="{{ address_city_name }}" Вид="город"/>
                                            {%- endif -%}
                                            {% if address_street_name is defined %}
                                                <ЭлУлДорСети Наим="{{ address_street_name }}" Тип="улица"/>
                                            {%- endif -%}
                                            {% if address_home is defined %}
                                                <Здание Номер="{{ address_home }}" Тип="дом"/>
                                            {%- endif -%}
                                        </АдрФИАС>
                                    </АдрМУстАвтУстр>
                                </СведАвтУстр>
                            {% endif %}
                        </СведРегККТ>
                        {% if is_reregistration  and change_fn == YES %}
                            <СведФискДок ПрЗаменПолом="2">
                                <СведОтчРег ВремяОтчРег="{{ rereg_report_time }}" ДатаОтчРег="{{ rereg_report_date }}" НомерОтчРег="{{ rereg_report_number }}" ФПД="{{ rereg_report_fn_sign }}"/>
                                <СведОтчЗакрФН ВремяОтчЗакрФН="{{ close_fiscal_report_time }}" ДатаОтчЗакрФН="{{ close_fiscal_report_date }}" НомерОтчЗакрФН="{{ close_fiscal_report_number }}" ФПД="{{ close_fiscal_report_fn_sign }}"/>
                            </СведФискДок>
                        {% endif %}
                    </ЗаявРегККТ>
                </Документ>
            </Файл>
            """;
    // CHECKSTYLE:ON


    // CHECKSTYLE:OFF
    // language=XML
    private static final String REG_REPORT_TEMPLATE =
            """
            {%- set NO = 2 -%}
            <?xml version='1.0' encoding='utf-8'?>
            <Файл ВерсПрог="1.0" ВерсФорм="5.02" ИдФайл="KO_OTCHREGKKT_{{ firm_sono_initial }}_{{ firm_sono_target }}_{{ firm_inn }}{{ firm_kpp }}_19700101_{{ fn_serial_number }}">
               <Документ ДатаДок="01.01.1970" КНД="1110201" КодНО="{{ firm_sono_target | e }}">
                   <СвНП>
                       <НПЮЛ ИННЮЛ="{{ firm_inn | e }}"
                           КПП="{{ firm_kpp | e }}"
                           НаимОрг="{{ firm_name | e }}"
                           ОГРН="{{ firm_ogrn | e }}" />
                   </СвНП>
                   <Подписант ПрПодп="{%if signer_document is defined %}2{% else %}1{% endif %}">
                        <ФИО Имя="{{ signer_first_name }}"
                             {% if signer_middle_name is defined %}
                                Отчество="{{ signer_middle_name }}"
                             {% endif %}
                             Фамилия="{{ signer_last_name }}"/>
                             {% if signer_document is defined %}
                                <СвПред НаимДок="{{ signer_document }}"/>
                             {% endif %}
                   </Подписант>
                   <ОтчРегККТ РегНомерККТ="{{ kkt_reg_number | e }}" ЗаводНомерФН="{{ fn_serial_number | e }}" ИННОФД="{{ ofd_inn }}"
                       ВремяОтчРег="{{ reg_fiscal_report_time | e }}" ДатаОтчРег="{{ reg_fiscal_report_date | e }}"
                       НомерОтчРег="{{ reg_fiscal_report_number | e }}" ФПД="{{ reg_fiscal_report_fn_sign | e }}"/>
               </Документ>
            </Файл>
            """;
    // CHECKSTYLE:ON

    // CHECKSTYLE:OFF
    // language=XML
    private static final String WITHDRAW_TEMPLATE =
            """
            {%- set NO = 2 -%}
            <?xml version='1.0' encoding='utf-8'?>
            <Файл ВерсПрог="1.0" ВерсФорм="5.02" ИдФайл="KO_ZVLSNUCHKKT_{{ firm_sono_initial }}_{{ firm_sono_target }}_{{ firm_inn }}{{ firm_kpp }}_19700101_{{ kkt_sn_pure }}">
                <Документ ДатаДок="01.01.1970" КНД="1110062" КодНО="{{ firm_sono_target | e }}">
                    {% if is_invalid_schema %} <!-- {% endif %}<СвНП>{% if is_invalid_schema %} --> {% endif %}
                        <НПЮЛ ИННЮЛ="{{ firm_inn | e }}"
                                КПП="{{ firm_kpp | e }}"
                                НаимОрг="{{ firm_name | e }}"
                                ОГРН="{{ firm_ogrn | e }}" />
                    {% if is_invalid_schema %} <!-- {% endif %}</СвНП>{% if is_invalid_schema %} --> {% endif %}
                    <Подписант ПрПодп="{%if signer_document is defined %}2{% else %}1{% endif %}">
                        <ФИО Имя="{{ signer_first_name }}"
                             {% if signer_middle_name is defined %}
                                Отчество="{{ signer_middle_name }}"
                             {% endif %}
                             Фамилия="{{ signer_last_name }}"/>
                             {% if signer_document is defined %}
                                <СвПред НаимДок="{{ signer_document }}"/>
                             {% endif %}
                    </Подписант>
                    <ЗаявСнРегККТ РегНомерККТ="{{ prev_reg_number | e }}"
                            МоделККТ="{{ kkt_model_name | e }}"
                            ЗаводНомерККТ="{{ kkt_sn | e }}"
                            ПрХищен="{{ is_kkt_stolen }}"
                            ПрПотер="{{ is_kkt_missing }}"
                            ПрЗаменПолом="{{ is_fn_broken }}">
                        {%- if is_kkt_stolen == NO and is_kkt_missing == NO and is_fn_broken == NO -%}
                            <СведОтчЗакрФН ВремяОтчЗакрФН="{{ close_fiscal_report_time | e }}"
                                    ДатаОтчЗакрФН="{{ close_fiscal_report_date | e }}"
                                    НомерОтчЗакрФН="{{ close_fiscal_report_number | e }}"
                                    ФПД="{{ close_fiscal_report_fn_sign | e }}" />
                        {%- endif -%}
                    </ЗаявСнРегККТ>
                </Документ>
            </Файл>
            """;
    // CHECKSTYLE:ON

    private static final String YES = "1";
    private static final String NO = "2";

    private static final DateTimeFormatter REPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH.mm");
    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    static final String DEFAULT_SIGNER_FIRST_NAME = "Зиц";
    static final String DEFAULT_SIGNER_MIDDLE_NAME = "Председатель";
    static final String DEFAULT_SIGNER_LAST_NAME = "Фунт";

    static final FiasCode DEFAULT_ADDRESS_FIAS = new FiasCode("1b533431-3f0a-45b8-addc-69c3e1b19a8b");
    static final Sono DEFAULT_ADDRESS_SONO = new Sono("8899");
    static final String DEFAULT_ADDRESS_POST_CODE = "141281";
    static final String DEFAULT_ADDRESS_REGION_CODE = "50";
    static final String DEFAULT_ADDRESS_MUNICIPALITY_REGION_NAME = "Город Н.";
    static final String DEFAULT_ADDRESS_MUNICIPALITY_REGION_CODE = "2";
    static final String DEFAULT_ADDRESS_CITY_NAME = "Н";
    static final String DEFAULT_ADDRESS_STREET_NAME = "Советская";
    static final String DEFAULT_ADDRESS_HOME = "42";

    static final Inn DEFAULT_FIRM_INN = new Inn("7704448842");
    static final String DEFAULT_FIRM_NAME = "ООО \"Рога и копыта\"";
    static final String DEFAULT_FIRM_URL = "hh.com";
    static final String DEFAULT_FIRM_KPP = "770401001";
    static final Ogrn DEFAULT_FIRM_OGRN = new Ogrn("5177746308394");
    static final Sono DEFAULT_FIRM_SONO_INITIAL = new Sono("0123");
    static final Sono DEFAULT_FIRM_SONO_TARGET = new Sono("4567");

    static final SerialNumber DEFAULT_FN_SN = new SerialNumber("9282440300675177");
    static final ModelName DEFAULT_FN_MODEL_NAME = new ModelName("«ФН-1.1» исполнение 5-15-2");

    static final SerialNumber DEFAULT_KKT_SN = new SerialNumber("00000000381004050054");
    static final ModelName DEFAULT_KKT_MODEL_NAME = new ModelName("РП Система 1ФА");
    static final Set<Kkt.Mode> DEFAULT_KKT_MODE = EnumSet.of(Kkt.Mode.INTERNET);
    static final AutomatedSystemNumber DEFAULT_KKT_AUTOMATED_SYS_NUMBER = new AutomatedSystemNumber("test-as-num");

    static final Inn DEFAULT_OFD_INN = new Inn("7704358518");
    static final String DEFAULT_OFD_NAME = "ООО \"Лучший.ОФД\"";

    static final Set<Change> DEFAULT_CHANGE_SET = EnumSet.of(Change.FN, Change.OTHER);
    static final RegistrationNumber DEFAULT_PREV_REG_NUMBER = new RegistrationNumber("0001815835007088");
    static final RegistrationNumber DEFAULT_REG_NUMBER = new RegistrationNumber("0001815835007089");

    static final LocalDateTime DEFAULT_REREG_REPORT_TIME = LocalDateTime.of(2020, 2, 3, 4, 5, 6);
    static final long DEFAULT_REREG_REPORT_NUMBER = 1;
    static final String DEFAULT_REREG_REPORT_FN_SIGN = "2617967714";

    static final LocalDateTime DEFAULT_REG_REPORT_TIME = LocalDateTime.of(2020, 2, 3, 4, 5, 6);
    static final long DEFAULT_REG_REPORT_NUMBER = 1;
    static final String DEFAULT_REG_REPORT_FN_SIGN = "2517967714";

    static final LocalDateTime DEFAULT_CLOSE_FISCAL_REPORT_TIME = LocalDateTime.of(2020, 3, 4, 21, 6, 7);
    static final long DEFAULT_CLOSE_FISCAL_REPORT_NUMBER = 100500;
    static final String DEFAULT_CLOSE_FISCAL_REPORT_FN_SIGN = "3743166067";

    static final boolean DEFAULT_INVALID_SCHEMA_FLAG = false;
    static final boolean DEFAULT_MARKED_GOODS_USAGE_FLAG = false;


    private ApplicationVersion appVersion;

    @Builder.Default
    private String signerFirstName = DEFAULT_SIGNER_FIRST_NAME;

    @Builder.Default
    private String signerLastName = DEFAULT_SIGNER_LAST_NAME;

    @Builder.Default
    private Optional<String> signerMiddleName = Optional.of(DEFAULT_SIGNER_MIDDLE_NAME);

    @Builder.Default
    private Optional<String> signerDocument = Optional.empty();

    @Builder.Default
    private FiasCode addressFias = DEFAULT_ADDRESS_FIAS;

    @Builder.Default
    private Sono addressSono = DEFAULT_ADDRESS_SONO;

    @Builder.Default
    private String addressPostCode = DEFAULT_ADDRESS_POST_CODE;

    @Builder.Default
    private String addressRegionCode = DEFAULT_ADDRESS_REGION_CODE;

    @Builder.Default
    private String addressMunicipalityRegionName = DEFAULT_ADDRESS_MUNICIPALITY_REGION_NAME;

    @Builder.Default
    private String addressMunicipalityRegionCode = DEFAULT_ADDRESS_MUNICIPALITY_REGION_CODE;

    @Builder.Default
    private Optional<String> addressCityName = Optional.of(DEFAULT_ADDRESS_CITY_NAME);

    @Builder.Default
    private Optional<String> addressStreetName = Optional.of(DEFAULT_ADDRESS_STREET_NAME);

    @Builder.Default
    private Optional<String> addressHome = Optional.of(DEFAULT_ADDRESS_HOME);

    @Builder.Default
    private Inn firmInn = DEFAULT_FIRM_INN;

    @Builder.Default
    private String firmName = DEFAULT_FIRM_NAME;

    @Builder.Default
    private String firmUrl = DEFAULT_FIRM_URL;

    @Builder.Default
    private String firmKpp = DEFAULT_FIRM_KPP;

    @Builder.Default
    private Ogrn firmOgrn = DEFAULT_FIRM_OGRN;

    @Builder.Default
    private Sono firmSonoInitial = DEFAULT_FIRM_SONO_INITIAL;

    @Builder.Default
    private Sono firmSonoTarget = DEFAULT_FIRM_SONO_TARGET;

    @Builder.Default
    private SerialNumber fnSn = DEFAULT_FN_SN;

    @Builder.Default
    private ModelName fnModelName = DEFAULT_FN_MODEL_NAME;

    @Builder.Default
    private SerialNumber kktSn = DEFAULT_KKT_SN;

    @Builder.Default
    private ModelName kktModelName = DEFAULT_KKT_MODEL_NAME;

    @Builder.Default
    private Set<Kkt.Mode> kktMode = DEFAULT_KKT_MODE;

    @Builder.Default
    private Optional<AutomatedSystemNumber> kktAutomatedSysNumber = Optional.of(DEFAULT_KKT_AUTOMATED_SYS_NUMBER);

    @Builder.Default
    private Inn ofdInn = DEFAULT_OFD_INN;

    @Builder.Default
    private String ofdName = DEFAULT_OFD_NAME;

    @Builder.Default
    private Set<Change> changeSet = DEFAULT_CHANGE_SET;

    @Builder.Default
    private RegistrationNumber prevRegNumber = DEFAULT_PREV_REG_NUMBER;

    @Builder.Default
    private RegistrationNumber regNumber = DEFAULT_REG_NUMBER;

    @Builder.Default
    private LocalDateTime reregReportTime = DEFAULT_REREG_REPORT_TIME;

    @Builder.Default
    private long reregReportNumber = DEFAULT_REREG_REPORT_NUMBER;

    @Builder.Default
    private String reregReportFnSign = DEFAULT_REREG_REPORT_FN_SIGN;

    @Builder.Default
    private LocalDateTime regReportTime = DEFAULT_REG_REPORT_TIME;

    @Builder.Default
    private long regReportNumber = DEFAULT_REG_REPORT_NUMBER;

    @Builder.Default
    private String regReportFnSign = DEFAULT_REG_REPORT_FN_SIGN;

    @Builder.Default
    private LocalDateTime closeFiscalReportTime = DEFAULT_CLOSE_FISCAL_REPORT_TIME;

    @Builder.Default
    private long closeFiscalReportNumber = DEFAULT_CLOSE_FISCAL_REPORT_NUMBER;

    @Builder.Default
    private String closeFiscalReportFnSign = DEFAULT_CLOSE_FISCAL_REPORT_FN_SIGN;

    @Builder.Default
    private boolean invalidSchema = DEFAULT_INVALID_SCHEMA_FLAG;

    @Builder.Default
    private Optional<Boolean> markedGoodsUsage = Optional.empty();

    @Builder.Default
    private Optional<Kkt.FfdVersion> ffdVersion = Optional.empty();

    private Withdraw withdraw;

    private Map<String, Object> regBindings(boolean reregistration) {
        val map = new HashMap<>(Map.ofEntries(
                entry("is_reregistration", reregistration),
                entry("version", appVersion.getVersionString()),
                entry("signer_first_name", signerFirstName),
                entry("signer_last_name", signerLastName),
                entry("address_fias", addressFias),
                entry("address_sono", addressSono),
                entry("address_post_code", addressPostCode),
                entry("address_region_code", addressRegionCode),
                entry("address_municipality_region_name", addressMunicipalityRegionName),
                entry("address_municipality_region_code", addressMunicipalityRegionCode),
                entry("firm_inn", firmInn),
                entry("firm_name", firmName),
                entry("firm_url", firmUrl),
                entry("firm_kpp", firmKpp),
                entry("firm_ogrn", firmOgrn),
                entry("firm_sono_initial", firmSonoInitial),
                entry("firm_sono_target", firmSonoTarget),
                entry("fn_sn", fnSn),
                entry("fn_model_name", fnModelName),
                entry("kkt_sn", kktSn),
                entry("kkt_sn_pure", kktSn.decay()),
                entry("kkt_model_name", kktModelName),
                entry("kkt_autonomous_mode", kktMode.contains(Kkt.Mode.AUTONOMOUS) ? YES : NO),
                entry("kkt_gambling_mode", kktMode.contains(Kkt.Mode.GAMBLING) ? YES : NO),
                entry("kkt_internet_mode", kktMode.contains(Kkt.Mode.INTERNET) ? YES : NO),
                entry("kkt_lottery_mode", kktMode.contains(Kkt.Mode.LOTTERY) ? YES : NO),
                entry("kkt_transport_mode", kktMode.contains(Kkt.Mode.TRANSPORT) ? YES : NO),
                entry("kkt_blank_mode", kktMode.contains(Kkt.Mode.BLANK) ? YES : NO),
                entry("kkt_agent_mode", kktMode.contains(Kkt.Mode.AGENT) ? YES : NO),
                entry("kkt_excise_mode", kktMode.contains(Kkt.Mode.EXCISE) ? YES : NO),
                entry("ofd_inn", ofdInn),
                entry("ofd_name", ofdName),
                entry("change_address", changeSet.contains(Change.ADDRESS) ? YES : NO),
                entry("change_ofd", changeSet.contains(Change.OFD) ? YES : NO),
                entry("change_auto_sys", changeSet.contains(Change.AUTOMATED_SYSTEM) ? YES : NO),
                entry("change_fn", changeSet.contains(Change.FN) ? YES : NO),
                entry("change_autonomous_mode", changeSet.contains(Change.TO_AUTONOMOUS_MODE) ? YES : NO),
                entry("change_std_mode", changeSet.contains(Change.TO_STD_MODE) ? YES : NO),
                entry("change_org_name", changeSet.contains(Change.ORG_NAME) ? YES : NO),
                entry("change_other", changeSet.contains(Change.OTHER) ? YES : NO),
                entry("prev_reg_number", prevRegNumber),
                entry("rereg_report_time", reregReportTime.format(REPORT_TIME_FORMATTER)),
                entry("rereg_report_date", reregReportTime.format(REPORT_DATE_FORMATTER)),
                entry("rereg_report_number", reregReportNumber),
                entry("rereg_report_fn_sign", reregReportFnSign),
                entry("close_fiscal_report_time", closeFiscalReportTime.format(REPORT_TIME_FORMATTER)),
                entry("close_fiscal_report_date", closeFiscalReportTime.format(REPORT_DATE_FORMATTER)),
                entry("close_fiscal_report_number", closeFiscalReportNumber),
                entry("close_fiscal_report_fn_sign", closeFiscalReportFnSign),
                entry("is_invalid_schema", invalidSchema)
        ));

        signerMiddleName.ifPresent(middleName -> map.put("signer_middle_name", middleName));
        signerDocument.ifPresent(doc -> map.put("signer_document", doc));
        addressCityName.ifPresent(name -> map.put("address_city_name", name));
        addressStreetName.ifPresent(name -> map.put("address_street_name", name));
        addressHome.ifPresent(home -> map.put("address_home", home));
        kktAutomatedSysNumber.ifPresent(number -> map.put("kkt_automated_sys_number", number));
        markedGoodsUsage.ifPresent(usage -> map.put("marked_goods_usage", usage));
        ffdVersion.ifPresent(version -> map.put("ffd_version", version.getCode()));

        return unmodifiableMap(map);
    }

    private String renderRegApp(boolean reregistration) {
        return JINJAVA.render(REG_TEMPLATE, regBindings(reregistration)).trim();
    }

    String toRegistrationApp() {
        return renderRegApp(false);
    }

    String toReregistrationApp() {
        return renderRegApp(true);
    }

    private Map<String, ?> withdrawBindings() {
        val map = new HashMap<>(Map.ofEntries(
                entry("signer_first_name", signerFirstName),
                entry("signer_last_name", signerLastName),
                entry("firm_inn", firmInn),
                entry("firm_name", firmName),
                entry("firm_kpp", firmKpp),
                entry("firm_ogrn", firmOgrn),
                entry("firm_sono_initial", firmSonoInitial),
                entry("firm_sono_target", firmSonoTarget),
                entry("kkt_sn", kktSn),
                entry("kkt_sn_pure", kktSn.decay()),
                entry("kkt_model_name", kktModelName),
                entry("prev_reg_number", prevRegNumber),
                entry("is_kkt_stolen", withdraw instanceof Withdraw.KktStolen ? YES : NO),
                entry("is_kkt_missing", withdraw instanceof Withdraw.KktMissing ? YES : NO),
                entry("is_fn_broken", withdraw instanceof Withdraw.FnBroken ? YES : NO),
                entry("is_invalid_schema", invalidSchema)
        ));

        if (withdraw instanceof Withdraw.FiscalClose fiscalClose) {
            val report = fiscalClose.report();
            map.putAll(Map.of(
                    "close_fiscal_report_time", report.time().format(REPORT_TIME_FORMATTER),
                    "close_fiscal_report_date", report.time().format(REPORT_DATE_FORMATTER),
                    "close_fiscal_report_number", report.number(),
                    "close_fiscal_report_fn_sign", report.fnSign()
            ));
        }

        signerMiddleName.ifPresent(middleName -> map.put("signer_middle_name", middleName));
        signerDocument.ifPresent(doc -> map.put("signer_document", doc));

        return unmodifiableMap(map);
    }

    private Map<String, ?> regReportBindings() {
        val map = new HashMap<>(Map.ofEntries(
                entry("signer_first_name", signerFirstName),
                entry("signer_last_name", signerLastName),
                entry("firm_inn", firmInn),
                entry("firm_name", firmName),
                entry("firm_kpp", firmKpp),
                entry("firm_ogrn", firmOgrn),
                entry("firm_sono_initial", firmSonoInitial),
                entry("firm_sono_target", firmSonoTarget),
                entry("kkt_reg_number", regNumber),
                entry("reg_fiscal_report_time", regReportTime.format(REPORT_TIME_FORMATTER)),
                entry("reg_fiscal_report_date", regReportTime.format(REPORT_DATE_FORMATTER)),
                entry("reg_fiscal_report_number", regReportNumber),
                entry("reg_fiscal_report_fn_sign", regReportFnSign),
                entry("fn_serial_number", fnSn),
                entry("ofd_inn", ofdInn)
        ));

        signerMiddleName.ifPresent(middleName -> map.put("signer_middle_name", middleName));
        signerDocument.ifPresent(doc -> map.put("signer_document", doc));

        return unmodifiableMap(map);
    }

    String toWithdrawApp() {
        return JINJAVA.render(WITHDRAW_TEMPLATE, withdrawBindings()).trim();
    }

    String toRegReportApp() {
        return JINJAVA.render(REG_REPORT_TEMPLATE, regReportBindings()).trim();
    }
}
