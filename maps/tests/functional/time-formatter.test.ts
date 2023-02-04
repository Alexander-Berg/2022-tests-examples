import {expect} from 'chai';
import {yandex} from '@yandex-int/maps-proto-schemas/types';
import timeFormatter from '../../src/lib/time-formatter';

const makeTimeInfoObj = (time: number, tzOffset: number): yandex.maps.proto.common2.i18n.ITime => {
    return {
        value: time,
        tzOffset,
        text: ''
    };
};

describe('Форматирование времени', () => {
    describe('Базовые проверки на обычную конвертацию секунд в строку c разными tz', () => {
        it('tzOffset = 0', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1422788400, 0)
            }, 'ru_RU');
            expect(time.begin).to.be.equal('01 февраля 2015 11:00 UTC');
        });

        it('tzOffset = 2 часа', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1422788400, 7200)
            }, 'ru_RU');
            expect(time.begin).to.be.equal('01 февраля 2015 13:00');
        });

        it('tzOffset = 3 часа', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1422788400, 10800)
            }, 'ru_RU');
            expect(time.begin).to.be.equal('01 февраля 2015 14:00');
        });

        it('Время окончание должно быть пустым', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1422788400, 10800)
            }, 'ru_RU');
            expect(time.end).to.be.equal('');
        });
    });

    describe('Языковые проверки', () => {
        const locales = [
            'be_BY', 'cs_CZ', 'de_DE', 'en_AU', 'en_CA', 'en_GB',
            'en_IE', 'en_NZ', 'en_US', 'fr_FR', 'hy_AM', 'kk_KZ',
            'ru_RU', 'ru_UA', 'tr_TR', 'tt_RU', 'uk_RU', 'uk_UA'
        ];

        const expects = [
            '02 студзеня 2015 14:00', '02 ledna 2015 14:00', '02 Januar 2015 14:00',
            '02 January 2015 14:00', '02 January 2015 14:00', '02 January 2015 14:00',
            '02 January 2015 14:00', '02 January 2015 14:00', '02 January 2015 14:00',
            '02 janvier 2015 14:00', '02 Հունվար 2015 14:00', '02 қаңтар 2015 14:00',
            '02 января 2015 14:00', '02 января 2015 14:00', '02 Ocak 2015 14:00',
            '02 гыйнвар 2015 14:00', '02 січня 2015 14:00', '02 січня 2015 14:00'
        ];

        it('Все языки', async () => {
            locales.forEach((locale, i) => {
                const time = timeFormatter({
                    begin: makeTimeInfoObj(1420196400, 10800)
                }, locale);
                expect(time.begin).to.be.equal(expects[i]);
            });
        });
    });

    describe('Проверяем форматирование с датами начала и окончания события', () => {
        it('Один и тот же год', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1425218400, 10800),
                end: makeTimeInfoObj(1425304800, 10800)
            }, 'ru_RU');

            expect(time.begin).to.be.equal('01 марта 17:00');
            expect(time.end).to.be.equal('02 марта 17:00');
        });

        it('Неподходящее время', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1422738000, 10800),
                end: makeTimeInfoObj(1422910799, 10800)
            }, 'ru_RU');

            expect(time.begin).to.be.equal('01 февраля');
            expect(time.end).to.be.equal('02 февраля');
        });

        it('Неподходящее время начала события', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1422738000, 10800),
                end: makeTimeInfoObj(1422869640, 10800)
            }, 'ru_RU');

            expect(time.begin).to.be.equal('01 февраля 00:00');
            expect(time.end).to.be.equal('02 февраля 12:34');
        });

        it('Неподходящее время окончания события', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1422752400, 10800),
                end: makeTimeInfoObj(1422910799, 10800)
            }, 'ru_RU');

            expect(time.begin).to.be.equal('01 февраля 04:00');
            expect(time.end).to.be.equal('02 февраля 23:59');
        });

        it('Неподходящее время начала события в один день с окончанием события', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1422738000, 10800),
                end: makeTimeInfoObj(1422783240, 10800)
            }, 'ru_RU');

            expect(time.begin).to.be.equal('01 февраля 00:00');
            expect(time.end).to.be.equal('12:34');
        });

        it('Неподходящее время окончания события в один день с началом события', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1422752400, 10800),
                end: makeTimeInfoObj(1422824399, 10800)
            }, 'ru_RU');

            expect(time.begin).to.be.equal('01 февраля 04:00');
            expect(time.end).to.be.equal('23:59');
        });

        it('Однодневное событие', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1422738000, 10800),
                end: makeTimeInfoObj(1422824340, 10800)
            }, 'ru_RU');

            expect(time.begin).to.be.equal('01 февраля');
            expect(time.end).to.be.equal('');
        });

        it('Год со временем', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1422781200, 10800),
                end: makeTimeInfoObj(1489158000, 10800)
            }, 'ru_RU');

            expect(time.begin).to.be.equal('01 февраля 2015 12:00');
            expect(time.end).to.be.equal('10 марта 2017 18:00');
        });

        it('Год без времени', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1422738000, 10800),
                end: makeTimeInfoObj(1489179540, 10800)
            }, 'ru_RU');

            expect(time.begin).to.be.equal('01 февраля 2015');
            expect(time.end).to.be.equal('10 марта 2017');
        });

        it('Тот же день, разные месяцы', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1420146000, 10800),
                end: makeTimeInfoObj(1425329940, 10800)
            }, 'ru_RU');

            expect(time.begin).to.be.equal('02 января');
            expect(time.end).to.be.equal('02 марта');
        });

        it('Тот же день, тот же месяц, разные годы', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1420146000, 10800),
                end: makeTimeInfoObj(1483304400, 10800)
            }, 'ru_RU');

            expect(time.begin).to.be.equal('02 января 2015');
            expect(time.end).to.be.equal('02 января 2017');
        });

        it('Разные годы, но он не должен присутствовать в форматированном результате', async () => {
            const time = timeFormatter({
                begin: makeTimeInfoObj(1447966800, 10800),
                end: makeTimeInfoObj(1457643540, 10800)
            }, 'ru_RU');

            expect(time.begin).to.be.equal('20 ноября');
            expect(time.end).to.be.equal('10 марта');
        });
    });
});
