import getOfferDetailsFeatures, { stringifyFeature } from '..';
import stubs from 'realty-core/app/test-utils/stubs';
import * as offers from './tests-data/offers';

describe('Stringify feature util', () => {
    describe('parse boolean feature with', () => {
        it('positive value', () => {
            const result = stringifyFeature({ name: 'phone', value: true });

            expect(result).toBe('Телефон');
        });

        it('negative value', () => {
            const result = stringifyFeature({ name: 'phone', value: false });

            expect(result).toBe('Телефона нет');
        });
    });

    describe('parse simple feature with', () => {
        it('number value', () => {
            const result = stringifyFeature({
                name: 'parking_places',
                value: 5
            });

            expect(result).toBe('5 мест на парковке');
        });

        it('should not format years', () => {
            const result = stringifyFeature({
                name: 'building_year',
                value: 1998
            });

            expect(result).toBe('Год постройки здания 1998');
        });

        it('string value', () => {
            const result = stringifyFeature({
                name: 'garage_name',
                value: 'ЖСК'
            });

            expect(result).toBe('ГСК «ЖСК»');
        });
    });

    describe('parse complex feature with', () => {
        it('common rules', () => {
            const result = stringifyFeature({
                name: 'building_material',
                value: 'FERROCONCRETE'
            });

            expect(result).toBe('Железобетонный');
        });

        it('special rules', () => {
            const result = stringifyFeature({
                name: 'balcony_type',
                value: 'TWO_BALCONY'
            });

            expect(result).toBe('Два балкона');
        });
    });

    describe('parse realty type', () => {
        it('APARTMENT', () => {
            const result = getOfferDetailsFeatures(offers.apartment).map(
                stringifyFeature
            );

            expect(result).toEqual([
                'Санузел совмещённый',
                'Балкон и две лоджии',
                'Вид из окон во двор',
                'Телефон',
                'Интернет',
                'Мебель',
                'Мебель на кухне',
                'Телевизор',
                'Стиральная машина',
                'Холодильник',
                'Кондиционер',
                'Встроенная техника',
                'Посудомойка',
                'Сигнализация'
            ]);
        });

        it('ROOMS', () => {
            const result = getOfferDetailsFeatures(offers.room).map(
                stringifyFeature
            );

            expect(result).toEqual([
                'Санузел раздельный',
                'Лоджия',
                'Телефон',
                'Интернета нет',
                'Мебель',
                'Мебель на кухне',
                'Телевизор',
                'Стиральной машины нет',
                'Холодильник',
                'Кондиционер',
                'Встроенной техники нет',
                'Посудомойка'
            ]);
        });

        it('HOUSE', () => {
            const result = getOfferDetailsFeatures(offers.house).map(
                stringifyFeature
            );

            expect(result).toEqual([
                'Санузел совмещённый',
                'Балкон и две лоджии',
                'Отделка — косметический ремонт',
                'Возможность прописки',
                'Туалет на улице',
                'Душ на улице',
                'Отопление',
                'Водопровод',
                'Канализация',
                'Электроснабжение',
                'Газ',
                'Кухня',
                'Бассейн',
                'Бильярд',
                'Сауна',
                'Тип здания — кирпичное'
            ]);
        });

        it('HOUSE_PART', () => {
            const result = getOfferDetailsFeatures(offers.housePart).map(
                stringifyFeature
            );

            expect(result).toEqual([
                'Санузел раздельный',
                'Два балкона',
                'Отделка — косметический ремонт',
                'Возможности прописки нет',
                'Туалет в доме',
                'Душ в доме',
                'Отопления нет',
                'Водопровода нет',
                'Канализации нет',
                'Электроснабжения нет',
                'Газа нет',
                'Кухни нет',
                'Бассейна нет',
                'Бильярда нет',
                'Сауны нет',
                'Тип здания — деревянное'
            ]);
        });

        it('LOT', () => {
            const result = getOfferDetailsFeatures(offers.lot).map(
                stringifyFeature
            );

            expect(result).toEqual([
                'Возможность прописки',
                'Туалет на улице',
                'Душ на улице',
                'Отопление',
                'Водопровод',
                'Канализация',
                'Электроснабжение',
                'Газ'
            ]);
        });

        it('GARAGE', () => {
            const result = getOfferDetailsFeatures(offers.garage).map(
                stringifyFeature
            );

            expect(result).toEqual([
                'ГСК «БСК»',
                'Собственность',
                'Парковка рядом',
                'Металлический',
                'Водопровод',
                'Отопление',
                'Электроснабжение',
                'Автоматические ворота',
                'Видеонаблюдение',
                'Доступ на объект 24/7',
                'Охрана',
                'Пожарная сигнализация',
                'Пропускная система',
                'Смотровая яма',
                'Подвал-погреб',
                'Автомойка',
                'Автосервис'
            ]);
        });

        it('COMMERCIAL', () => {
            const result = getOfferDetailsFeatures(offers.commercial).map(
                stringifyFeature
            );

            expect(result).toEqual([
                'Класс бизнес-центра — A+',
                'Класс недвижимости — элитный',
                'Состояние хорошее',
                'Отделка — хороший ремонт',
                'Интернет',
                'Кондиционер',
                'Охрана',
                'Пропускная система',
                'Доступ на объект 24/7',
                'Охраняемая парковка',
                '5 мест на парковке',
                '10 мест на гостевой парковке',
                'Машиноместо 800 р/мес в месяц',
                'Заведение питания в здании',
                'Сигнализация',
                'Пожарная сигнализация',
                'Мебель',
                'Вид из окон на улицу',
                'Тип окон витринные',
                'Лифт',
                'Вход отдельный',
                'Парковки для гостей',
                'Возможность выбрать оператора связи',
                'Возможность добавить телефонные линии',
                'Электроснабжения нет',
                'Выделенной электрической мощности 600 кВт',
                'Вентиляция',
                'Канализации нет',
                'Отопления нет',
                'Водопровода нет',
                'Газа нет',
                'Мусоропровод',
                'Логистические услуги',
                'Ответственное хранение',
                'Грузовой лифт',
                'Офис на складе',
                'Подъезд для грузового транспорта',
                'Открытая площадка',
                'Рядом ветка ж/д',
                'Температурный режим на складе 25 градусов',
                'Стоимость палета-места 400 р/мес',
                'Пандус',
                'Тип здания — деревянное',
                'Серия здания 25 ЖС'
            ]);
        });
    });

    describe('real offers', () => {
        stubs.everySearcherOfferMatchSnapshot(offer => {
            return getOfferDetailsFeatures(offer).map(stringifyFeature);
        });
    });
});
