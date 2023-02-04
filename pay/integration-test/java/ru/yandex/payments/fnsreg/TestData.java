package ru.yandex.payments.fnsreg;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import lombok.experimental.UtilityClass;
import lombok.val;

import ru.yandex.payments.fnsreg.dto.Firm;
import ru.yandex.payments.fnsreg.dto.Fn;
import ru.yandex.payments.fnsreg.dto.Kkt;
import ru.yandex.payments.fnsreg.dto.Kkt.Mode;
import ru.yandex.payments.fnsreg.dto.Ofd;
import ru.yandex.payments.fnsreg.types.AutomatedSystemNumber;
import ru.yandex.payments.fnsreg.types.FiasCode;
import ru.yandex.payments.fnsreg.types.Inn;
import ru.yandex.payments.fnsreg.types.ModelName;
import ru.yandex.payments.fnsreg.types.Ogrn;
import ru.yandex.payments.fnsreg.types.SerialNumber;
import ru.yandex.payments.fnsreg.types.Sono;

@UtilityClass
class TestData {
    private static final Kkt.Address DEFAULT_ADDRESS = new Kkt.Address(
            new FiasCode("a820ef3e-f4f2-4646-8502-a7d9dd1d3719"),
            new Sono("6232"),
            "391434",
            "62",
            "Город Сасово",
            "2",
            Optional.of(new Kkt.AddressElement("Сасово", "город")),
            Optional.of(new Kkt.AddressElement("Пушкина", "улица")),
            Optional.of(new Kkt.AddressElement("21", "дом"))

    );
    private static final Set<Mode> DEFAULT_MODE = EnumSet.of(Mode.INTERNET);

    private static final ModelName KKT_MODEL = new ModelName("Терминал-ФА");
    private static final ModelName FN_MODEL = new ModelName("Шифровальное (криптографическое) средство защиты " +
            "фискальных данных фискальный накопитель «ФН-1.1» исполнение 5-15-2");
    private static final AutomatedSystemNumber AUTOMATED_SYSTEM_NUMBER = new AutomatedSystemNumber("test-num");

    private static Kkt kkt(String sn, String fnSn) {
        val fn = new Fn(new SerialNumber(fnSn), FN_MODEL);
        return new Kkt(new SerialNumber(sn), KKT_MODEL, DEFAULT_MODE, fn, DEFAULT_ADDRESS,
                Optional.of(AUTOMATED_SYSTEM_NUMBER), Optional.of(Kkt.FfdVersion.V120));
    }

    private static Fn fn(String sn) {
        return new Fn(new SerialNumber(sn), FN_MODEL);
    }

    private static final List<Kkt> KKT_LIST = List.of(
            kkt("550101010040", "9282440300561039"),
            kkt("550101010041", "9282440300561041"),
            kkt("550101010042", "9282440300560867"),
            kkt("550101010043", "9282440300560868"),
            kkt("550101010044", "9282440300560873"),
            kkt("550101010045", "9282440300560246"),
            kkt("550101010046", "9282440300560248"),
            kkt("550101010047", "9282440300560253"),
            kkt("550101010048", "9282440300560254"),
            kkt("550101010049", "9282440300560782"),
            kkt("550101010050", "9282440300560777"),
            kkt("550101010051", "9282440300560778"),
            kkt("550101010052", "9282440300560537"),
            kkt("550101010053", "9282440300560538"),
            kkt("550101010054", "9282440300560540"),
            kkt("550101010055", "9282440300560541"),
            kkt("550101010056", "9282440300560586"),
            kkt("550101010057", "9282440300560584"),
            kkt("550101010058", "9282440300560631"),
            kkt("550101010059", "9282440300560638"),
            kkt("550101010060", "9282440300560649"),
            kkt("550101010061", "9282440300560141"),
            kkt("550101010062", "9282440300560139"),
            kkt("550101010063", "9282440300559937"),
            kkt("550101010064", "9282440300559938"),
            kkt("550101010065", "9282440300560043")
    );

    private static final List<Fn> UNUSED_FN_LIST = List.of(
            fn("9282440300570644"),
            fn("9282440300581625"),
            fn("9282440300581519"),
            fn("9282440300581512"),
            fn("9282440300581516"),
            fn("9282440300581459"),
            fn("9282440300573688"),
            fn("9282440300573692"),
            fn("9282440300573656"),
            fn("9282440300571521"),
            fn("9282440300571523"),
            fn("9282440300571525"),
            fn("9282440300571526"),
            fn("9282440300571270"),
            fn("9282440300571271"),
            fn("9282440300571205"),
            fn("9282440300571291"),
            fn("9282440300571302"),
            fn("9282440300571303"),
            fn("9282440300571338"),
            fn("9282440300571339"),
            fn("9282440300571349"),
            fn("9282440300571351"),
            fn("9282440300571354"),
            fn("9282440300571356"),
            fn("9282440300571369"),
            fn("9282440300571399"),
            fn("9282440300571402"),
            fn("9282440300571409"),
            fn("9282440300571453"),
            fn("9282440300571405"),
            fn("9282440300571498"),
            fn("9282440300570579"),
            fn("9282440300570521"),
            fn("9282440300570522"),
            fn("9282440300570517"),
            fn("9282440300570576"),
            fn("9282440300570635"),
            fn("9282440300570639"),
            fn("9282440300570641")
    );

    static final Firm FIRM = new Firm(
            new Inn("5245023808"), "ООО Время", "time.ru", "524501001", new Ogrn("1105252001546"),
            new Firm.Signer("Алла", Optional.of("Романовна"), "Романова", Optional.empty()),
            new Sono("1234"), new Sono("1234")
    );

    static final Ofd OFD = new Ofd(new Inn("7704358518"), "ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ \"ЯНДЕКС.ОФД\"");

    static Kkt obtainKkt() {
        val index = ThreadLocalRandom.current().nextInt(KKT_LIST.size());
        return KKT_LIST.get(index);
    }

    static Fn obtainUnusedFn() {
        val index = ThreadLocalRandom.current().nextInt(UNUSED_FN_LIST.size());
        return UNUSED_FN_LIST.get(index);
    }
}
