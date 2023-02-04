import _ from 'lodash';

import versusMock from 'auto-core/react/dataDomain/versus/mock';

import type { MobileCompareAppState } from '../MobileCompareAppState';

import getSeo from './getSeo';

const stateMock = {
    config: {
        data: {
            pageType: '',
            pageParams: {
                category: 'cars',
            },
        },
    },
};

let state: MobileCompareAppState;

describe('должен корректно сформировать seo-данные для сравнения', () => {
    beforeEach(() => {
        state = _.cloneDeep(stateMock) as unknown as MobileCompareAppState;
        state.config.data.pageType = 'compare';
    });

    it('моделей', () => {
        state.config.data.pageParams.content = 'models';

        expect(getSeo(state)).toEqual({
            title: 'Сравнение моделей авто',
            canonical: 'https://autoru_frontend.base_domain/compare-models/',
        });
    });

    it('офферов', () => {
        state.config.data.pageParams.content = 'offers';

        expect(getSeo(state)).toEqual({
            title: 'Сравнение автомобилей',
            canonical: 'https://autoru_frontend.base_domain/compare-offers/',
        });
    });

    it('моделей, не должен добавлять в canonical query-параметры', () => {
        state.config.data.pageParams.content = 'models';
        state.config.data.pageParams.my_param = 'bla_bla_bla';

        expect(getSeo(state)).toEqual({
            title: 'Сравнение моделей авто',
            canonical: 'https://autoru_frontend.base_domain/compare-models/',
        });
    });
});

describe('должен корректно сформировать seo-данные для версусов', () => {
    beforeEach(() => {
        state = _.cloneDeep(stateMock) as unknown as MobileCompareAppState;
        state.config.data.pageType = 'versus';
        state.config.data.pageParams = {
            first_mark: 'ford',
            first_model: 'ecosport',
            second_mark: 'kia',
            second_model: 'rio',
        };
    });

    it('все, без query-параметров', () => {
        state.versus = versusMock.value();

        expect(getSeo(state)).toEqual({
            title: 'Сравнение Ford EcoSport и Kia Rio по характеристикам, ' +
                'стоимости покупки и обслуживания. Что лучше - Форд ЭкоСпорт или Киа Рио',
            description: 'Что лучше - Ford EcoSport и Kia Rio - ' +
                'все плюсы и минусы моделей. Как выбрать между Форд ЭкоСпорт и ' +
                'Киа Рио по характеристикам, стоимости покупки и обслуживания',
            canonical: 'https://autoru_frontend.base_domain/compare-cars/ford-ecosport-vs-kia-rio/',
        });
    });

    it('canonical, с query-параметров', () => {
        state.config.data.pageParams.catalog_filter = [ { foo: 'bar' } ];

        expect(getSeo(state).canonical).toEqual('https://autoru_frontend.base_domain/compare-cars/ford-ecosport-vs-kia-rio/');
    });
});
