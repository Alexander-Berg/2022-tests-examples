import type { Offer } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import { disk, nbsp } from 'auto-core/react/lib/html-entities';

import {
    getCarTechInfoFromOffer,
    getGearTypeText,
    getEngineInfoText,
    getEngineTypeText,
    getBodyTypeText,
} from './getCarTechInfoFromOffer';

describe('getCarTechInfoFromOffer - функция-маппер для черновика с ограниченными полями', () => {
    it('возвращает строку со всеми значениями, если передан валидный объект со всем полями', () => {
        const mockOffer = createMockOffer({
            drive: 'FORWARD_CONTROL',
            engine_type: 'GASOLINE',
            transmission: 'AUTOMATIC',
            tech_param: { displacement: 2997 },
            horse_power: 150,
            body_type: 'SEDAN',
        });

        expect(getCarTechInfoFromOffer(mockOffer)).toBe(
            // eslint-disable-next-line max-len
            `10${ nbsp }000${ nbsp }км${ nbsp }${ disk } Передний${ nbsp }${ disk } Бензин${ nbsp }${ disk } 3.0${ nbsp }AT${ nbsp }(150 л.с.)${ nbsp }${ disk } Седан${ nbsp }${ disk } AAAAAAAAAAAAAGGG`,
            // eslint-enable-next-line max-len
        );
    });

    it('возвращает строку с несколькими значениями, если передан валидный объект с некоторыми полями', () => {
        const mockOffer = createMockOffer({
            drive: 'FORWARD_CONTROL',
            transmission: 'AUTOMATIC',
            tech_param: { displacement: 2997 },
            body_type: 'SEDAN',
        });

        expect(getCarTechInfoFromOffer(mockOffer)).toBe(
            `10${ nbsp }000${ nbsp }км${ nbsp }${ disk } Передний${ nbsp }${ disk } 3.0${ nbsp }AT${ nbsp }${ disk } Седан${ nbsp }${ disk } AAAAAAAAAAAAAGGG`,
        );
    });

    it('возвращает строку с пробегом и вином, если отсутствует car_info', () => {
        const mockOffer = createMockOffer({});

        expect(getCarTechInfoFromOffer(mockOffer)).toBe(
            `10${ nbsp }000${ nbsp }км${ nbsp }${ disk } AAAAAAAAAAAAAGGG`,
        );
    });

    it('возвращает строку "пробег не указан", если передан объект с другими полями', () => {
        const mockOffer = {
            vehicle_info: {
                drive: 'FORWARD_CONTROL',
                engine_type: 'GASOLINE',
                transmission: 'AUTOMATIC',
                tech_param: { displacement: 2997 },
                horse_power: 150,
                body_type: 'SEDAN',
            },
        } as unknown as Offer;

        expect(getCarTechInfoFromOffer(mockOffer)).toBe('пробег не указан');
    });

    it('возвращает строку с правильными разделителями, если были переданы параметры', () => {
        const mockOffer = createMockOffer({
            drive: 'FORWARD_CONTROL',
            engine_type: 'GASOLINE',
            transmission: 'AUTOMATIC',
            tech_param: { displacement: 2997 },
            horse_power: 150,
            body_type: 'SEDAN',
        });

        const separator = ',';

        expect(getCarTechInfoFromOffer(mockOffer, separator)).toBe(
            `10${ nbsp }000${ nbsp }км, Передний, Бензин, 3.0${ nbsp }AT${ nbsp }(150 л.с.), Седан, AAAAAAAAAAAAAGGG`,
        );
    });
});

describe('getGearTypeText - функция для получения информации о типе передачи', () => {
    it('возвращает корректный текст, если передан объект с типом передачи', () => {
        const mockOffer = createMockOffer({
            drive: 'FORWARD_CONTROL',
        });

        expect(getGearTypeText(mockOffer)).toBe('Передний');
    });

    it('возвращает пустую строку, если в поле с типом передачи невалидное значение', () => {
        const mockOffer = createMockOffer({
            drive: 'SUPER_CONTROL',
        });

        expect(getGearTypeText(mockOffer)).toBe('');
    });

    it('возвращает пустую строку, если передан объект, где нет передачи', () => {
        const mockOffer = createMockOffer({});

        expect(getGearTypeText(mockOffer)).toBe('');
    });
});

describe('getEngineInfoText - функция для получения информации о двигателе', () => {
    it('возвращает корректный текст, если передан объект со всеми данными', () => {
        const mockOffer = createMockOffer({
            engine_type: 'GASOLINE',
            transmission: 'AUTOMATIC',
            tech_param: { displacement: 2997 },
            horse_power: 249,
        });

        expect(getEngineInfoText(mockOffer)).toBe(`3.0${ nbsp }AT${ nbsp }(249 л.с.)`);
    });

    it('возвращает неполный текст, если передан объект, где нет части данных', () => {
        const mockOffer = createMockOffer({
            engine_type: 'GASOLINE',
            transmission: 'AUTOMATIC',
            horse_power: 249,
        });

        expect(getEngineInfoText(mockOffer)).toBe(`AT${ nbsp }(249 л.с.)`);
    });

    it('возвращает неполный текст, если передан объект, где есть только одно поле', () => {
        const mockOffer = createMockOffer({
            horse_power: 249,
        });

        expect(getEngineInfoText(mockOffer)).toBe(`(249 л.с.)`);
    });

    it('возвращает пустую строку, если передан объект с пустыми полями', () => {
        const mockOffer = createMockOffer({
            engine_type: '',
            transmission: '',
            tech_param: {},
            horse_power: 0,
        });

        expect(getEngineInfoText(mockOffer)).toBe('');
    });

    it('возвращает пустую строку, если передан объект, где нет необходимых полей', () => {
        const mockOffer = createMockOffer({});

        expect(getEngineInfoText(mockOffer)).toBe('');
    });
});

describe('getEngineTypeText - функция для получения информации о типе двигателя', () => {
    it('возвращает корректный текст, если передан объект с типом двигателя', () => {
        const mockOffer = createMockOffer({
            engine_type: 'GASOLINE',
        });

        expect(getEngineTypeText(mockOffer)).toBe('Бензин');
    });

    it('возвращает пустую строку, если в поле с типом передачи невалидное значение', () => {
        const mockOffer = createMockOffer({
            drive: 'WATER_BASED',
        });

        expect(getEngineTypeText(mockOffer)).toBe('');
    });

    it('возвращает пустую строку, если передан объект, где нет передачи', () => {
        const mockOffer = createMockOffer({});

        expect(getEngineTypeText(mockOffer)).toBe('');
    });
});

describe('getBodyTypeText - функция для получения информации о типе кузова', () => {
    it('возвращает корректный текст, если передан объект с типом кузова', () => {
        const mockOffer = createMockOffer({
            body_type: 'SEDAN',
        });

        expect(getBodyTypeText(mockOffer)).toBe('Седан');
    });

    it('возвращает пустую строку, если в поле с типом передачи невалидное значение', () => {
        const mockOffer = createMockOffer({
            body_type: 'BANANA',
        });

        expect(getBodyTypeText(mockOffer)).toBe('');
    });

    it('возвращает пустую строку, если передан объект, где нет передачи', () => {
        const mockOffer = createMockOffer({});

        expect(getBodyTypeText(mockOffer)).toBe('');
    });
});

function createMockOffer(props: Record<string, unknown>) {
    return {
        car_info: {
            ...props,
        },
        state: { mileage: 10000 },
        documents: {
            vin: 'AAAAAAAAAAAAAGGG',
            year: 2017,
        },
    } as unknown as Offer;
}
