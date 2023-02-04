/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import panoramaExteriorMock from 'auto-core/models/panoramaExterior/mocks';

import panoramaLoader, { clearStore } from './loader';

declare var global: {
    Ya: Record<string, any>;
    performance?: Performance;
    Image: any;
};

let originalWindowYa: any;
let originalWindowPerformance: Performance | undefined;

beforeEach(() => {
    originalWindowYa = global.Ya;
    global.Ya = {
        Rum: {
            time: jest.fn(),
            timeEnd: jest.fn(),
            _deltaMarks: {},
        },
    };
    global.Image = class {
        constructor() {
            // симулируем успех
            setTimeout(() => {
                this.onload();
            }, 10);
        }

        onload = () => {}
    };

    originalWindowPerformance = global.performance;
    delete global.performance;
    global.performance = {
        clearResourceTimings: jest.fn(),
        getEntriesByName: jest.fn(() => ([])),
    } as unknown as Performance;
});

afterEach(() => {
    jest.restoreAllMocks();
    clearStore();

    global.Ya = originalWindowYa;
    global.performance = originalWindowPerformance;
});

describe('качество:', () => {
    it('если параметр передан, будет грузить картинки в hd-качестве', async() => {
        const loader = panoramaLoader.createOrGetLoader(
            panoramaExteriorMock.withWidescreen().value(),
            {
                isWidescreen: true,
                quality: 'hd',
            });

        const imagePromises = loader.load();
        const images = await imagePromises;

        expect(images[0]?.src).toMatch(/1920_0000\.jpeg$/);
    });

    it('если параметр не передан, будет грузить картинки в обычном качестве', async() => {
        const loader = panoramaLoader.createOrGetLoader(
            panoramaExteriorMock.withWidescreen().value(),
            {
                isWidescreen: true,
            });

        const imagePromises = loader.load();
        const images = await imagePromises;

        expect(images[0]?.src).toMatch(/1280_0000\.jpeg$/);
    });

    it('если параметр передан, но в модели таких урлов нет, будет грузить картинки в обычном качестве', async() => {
        const loader = panoramaLoader.createOrGetLoader(
            panoramaExteriorMock.value(),
            {
                quality: 'hd',
            });

        const imagePromises = loader.load();
        const images = await imagePromises;

        expect(images[0]?.src).toMatch(/1200_0000\.jpeg$/);
    });
});
