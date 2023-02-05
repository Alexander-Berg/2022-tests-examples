import {numberToString, formatCurrency, NumberFormatter, FractionOptions} from './i18n-utils';
import {CurrencyCode, Language} from 'types/locale';

const ruNumberCaseSet = (formatter: NumberFormatter): void => {
    expect(formatter(123)).toEqual('123');
    expect(formatter(123.1)).toEqual('123,1');
    expect(formatter(123.123123)).toEqual('123,12');
    expect(formatter(0.1)).toEqual('0,1');
    expect(formatter(12312312)).toEqual('12 312 312');
    expect(formatter(12312312.123123123)).toEqual('12 312 312,12');
};

const enNumberCaseSet = (formatter: NumberFormatter): void => {
    expect(formatter(123)).toEqual('123');
    expect(formatter(123.1)).toEqual('123.1');
    expect(formatter(123.123123)).toEqual('123.12');
    expect(formatter(0.1)).toEqual('0.1');
    expect(formatter(12312312)).toEqual('12,312,312');
    expect(formatter(12312312.123123123)).toEqual('12,312,312.12');
};

const trNumberCaseSet = (formatter: NumberFormatter): void => {
    expect(formatter(123)).toEqual('123');
    expect(formatter(123.1)).toEqual('123,1');
    expect(formatter(123.123123)).toEqual('123,12');
    expect(formatter(0.1)).toEqual('0,1');
    expect(formatter(12312312)).toEqual('12.312.312');
    expect(formatter(12312312.123123123)).toEqual('12.312.312,12');
};

const NUMBER_TO_STRING_CASES: Record<Language, (formatter: NumberFormatter) => void> = {
    ru: ruNumberCaseSet,
    en: enNumberCaseSet,
    uk: ruNumberCaseSet,
    tr: trNumberCaseSet,
    uz: ruNumberCaseSet,
    kk: ruNumberCaseSet
};

function generateCurrencyCase(table: Record<CurrencyCode, string>): (lang: Language) => void {
    return (lang) =>
        Object.entries(table).forEach(([currency, value]) => {
            expect(
                formatCurrency(
                    {
                        value: 123.23,
                        currency,
                        fractionDigits: 2
                    },
                    lang
                )
            ).toEqual(value);
        });
}

const ruTable = {
    RUB: '123,23 ₽',
    EUR: '123,23 €',
    USD: '123,23 $',
    GBP: '123,23 £',
    BYN: '123,23 BYN',
    UAH: '123,23 грн.',
    KZT: '123,23 ₸',
    ILS: '123,23 ₪',
    AED: '123,23 AED',
    AMD: '123,23 AMD',
    AZN: '123,23 AZN',
    GEL: '123,23 GEL',
    GHC: '123,23 GHC',
    GHS: '123,23 GHS',
    XOF: '123 CFA',
    KGS: '123,23 KGS',
    LVL: '123,23 LVL',
    LTL: '123,23 LTL',
    MDL: '123,23 MDL',
    RON: '123,23 RON',
    RSD: '123 RSD',
    UZS: '123,23 UZS'
} as const;

const ukTable = {
    RUB: '123,23 RUB',
    EUR: '123,23 EUR',
    USD: '123,23 USD',
    GBP: '123,23 GBP',
    BYN: '123,23 BYN',
    UAH: '123,23 грн.',
    KZT: '123,23 KZT',
    ILS: '123,23 ILS',
    AED: '123,23 AED',
    AMD: '123,23 AMD',
    AZN: '123,23 AZN',
    GEL: '123,23 GEL',
    GHC: '123,23 GHC',
    GHS: '123,23 GHS',
    XOF: '123 CFA',
    KGS: '123,23 KGS',
    LVL: '123,23 LVL',
    LTL: '123,23 LTL',
    MDL: '123,23 MDL',
    RON: '123,23 RON',
    RSD: '123 RSD',
    UZS: '123,23 UZS'
} as const;

const enTable = {
    RUB: 'RUB 123.23',
    EUR: '€123.23',
    USD: '$123.23',
    GBP: '£123.23',
    BYN: 'BYN 123.23',
    UAH: 'UAH 123.23',
    KZT: 'KZT 123.23',
    ILS: '₪123.23',
    AED: 'AED 123.23',
    AMD: 'AMD 123.23',
    AZN: 'AZN 123.23',
    GEL: 'GEL 123.23',
    GHC: 'GHC 123.23',
    GHS: 'GHS 123.23',
    XOF: 'CFA 123',
    KGS: 'KGS 123.23',
    LVL: 'LVL 123.23',
    LTL: 'LTL 123.23',
    MDL: 'MDL 123.23',
    RON: 'RON 123.23',
    RSD: 'RSD 123',
    UZS: 'UZS 123.23'
} as const;

const trTable = {
    RUB: 'RUB 123,23',
    EUR: '€123,23',
    USD: '$123,23',
    GBP: '£123,23',
    BYN: 'BYN 123,23',
    UAH: 'UAH 123,23',
    KZT: 'KZT 123,23',
    ILS: '₪123,23',
    AED: 'AED 123,23',
    AMD: 'AMD 123,23',
    AZN: 'AZN 123,23',
    GEL: 'GEL 123,23',
    GHC: 'GHC 123,23',
    GHS: 'GHS 123,23',
    XOF: 'CFA 123',
    KGS: 'KGS 123,23',
    LVL: 'LVL 123,23',
    LTL: 'LTL 123,23',
    MDL: 'MDL 123,23',
    RON: 'RON 123,23',
    RSD: 'RSD 123',
    UZS: 'UZS 123,23'
} as const;

const ruCurrencyCaseSet = generateCurrencyCase(ruTable);
const ukCurrencyCaseSet = generateCurrencyCase(ukTable);
const enCurrencyCaseSet = generateCurrencyCase(enTable);
const trCurrencyCaseSet = generateCurrencyCase(trTable);

const CURRENCY_TO_STRING_CASES: Record<Language, (lang: Language) => void> = {
    ru: ruCurrencyCaseSet,
    en: enCurrencyCaseSet,
    uk: ukCurrencyCaseSet,
    tr: trCurrencyCaseSet,
    uz: ruCurrencyCaseSet,
    kk: ruCurrencyCaseSet
};

describe('i18n utils', () => {
    describe('number to string', () => {
        Object.entries(NUMBER_TO_STRING_CASES).forEach(([lang, caseSet]) => {
            it(lang, () => {
                caseSet((input) => numberToString(input, lang));
            });
        });

        describe('digits amount', () => {
            const genericFormatter = (input: number, fractionOptions?: FractionOptions): string =>
                numberToString(input, 'en', fractionOptions);

            it('default', () => {
                const formatter = genericFormatter;
                expect(formatter(1)).toEqual('1');
                expect(formatter(1.1)).toEqual('1.1');
                expect(formatter(1.12)).toEqual('1.12');
                expect(formatter(1.123)).toEqual('1.12');
                expect(formatter(1.123123)).toEqual('1.12');
            });

            it('only min', () => {
                const formatter = (input: number): string => genericFormatter(input, {min: 2});
                expect(formatter(1)).toEqual('1.00');
                expect(formatter(1.1)).toEqual('1.10');
                expect(formatter(1.12)).toEqual('1.12');
                expect(formatter(1.123)).toEqual('1.123');
                expect(formatter(1.123123)).toEqual('1.123123');
            });

            it('empty options', () => {
                const formatter = (input: number): string => genericFormatter(input, {});
                expect(formatter(1)).toEqual('1');
                expect(formatter(1.1)).toEqual('1.1');
                expect(formatter(1.12)).toEqual('1.12');
                expect(formatter(1.123)).toEqual('1.123');
                expect(formatter(1.123123)).toEqual('1.123123');
            });

            it('fixed on 0', () => {
                const formatter = (input: number): string => genericFormatter(input, {min: 0, max: 0});
                expect(formatter(1)).toEqual('1');
                expect(formatter(1.1)).toEqual('1');
                expect(formatter(1.12)).toEqual('1');
                expect(formatter(1.123)).toEqual('1');
                expect(formatter(1.123123)).toEqual('1');
            });

            it('fixed on 2', () => {
                const formatter = (input: number): string => genericFormatter(input, {min: 2, max: 2});
                expect(formatter(1)).toEqual('1.00');
                expect(formatter(1.1)).toEqual('1.10');
                expect(formatter(1.12)).toEqual('1.12');
                expect(formatter(1.123)).toEqual('1.12');
                expect(formatter(1.123123)).toEqual('1.12');
            });

            it('from 3 to 5', () => {
                const formatter = (input: number): string => genericFormatter(input, {min: 3, max: 5});
                expect(formatter(1)).toEqual('1.000');
                expect(formatter(1.1)).toEqual('1.100');
                expect(formatter(1.12)).toEqual('1.120');
                expect(formatter(1.123)).toEqual('1.123');
                expect(formatter(1.123123)).toEqual('1.12312');
            });

            it('round fractions to max digits', () => {
                const formatter = (input: number): string => genericFormatter(input, {min: 3, max: 3});
                expect(formatter(1.1233)).toEqual('1.123');
                expect(formatter(1.1237)).toEqual('1.124');
            });
        });
    });

    describe('currency', () => {
        Object.entries(CURRENCY_TO_STRING_CASES).forEach(([lang, caseGenerator]) => {
            it(`lang [${lang}]`, () => {
                caseGenerator(lang);
            });
        });

        describe('fraction digits', () => {
            const genericFormatter = (value: number, fractionDigits: number) =>
                formatCurrency(
                    {
                        value,
                        currency: 'RUB',
                        fractionDigits
                    },
                    'en'
                );

            it('zero', () => {
                const formatter = (value: number) => genericFormatter(value, 0);
                expect(formatter(123)).toEqual('RUB 123');
                expect(formatter(123.1)).toEqual('RUB 123');
                expect(formatter(123.12)).toEqual('RUB 123');
                expect(formatter(123.123)).toEqual('RUB 123');
            });

            it('two', () => {
                const formatter = (value: number) => genericFormatter(value, 2);
                expect(formatter(123)).toEqual('RUB 123.00');
                expect(formatter(123.1)).toEqual('RUB 123.10');
                expect(formatter(123.12)).toEqual('RUB 123.12');
                expect(formatter(123.123)).toEqual('RUB 123.12');
            });
        });
    });
});
