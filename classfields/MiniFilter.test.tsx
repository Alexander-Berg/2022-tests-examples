import { cloneDeep, noop } from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import type { Props as PseudoInputGroupProps } from 'auto-core/react/components/mobile/PseudoInputGroup/PseudoInputGroup';

import MiniFilter from './MiniFilter';

const info = {
    mark: {
        'big-logo': '',
        count: 706,
        reviews_count: 22,
        cyrillic_name: 'Ауди',
        id: 'AUDI',
        itemFilterParams: { mark: 'AUDI' },
        logo: '',
        name: 'Audi',
        numeric_id: 3139,
        popular: true,
        year_from: 1927,
        year_to: 2020,
    },
    models: [
        {
            count: 60,
            reviews_count: 22,
            cyrillic_name: 'А3',
            generations: [ {
                count: 0,
                reviews_count: 22,
                id: '21837610',
                itemFilterParams: { super_gen: '21837610' },
                super_gen: '21837610',
                mobilePhoto: '',
                name: 'IV (8Y)',
                photo: '',
                yearFrom: 2020,
                yearTo: 2020,

            } ],
            nameplates: [
                {
                    id: '9264587',
                    name: 'g-tron',
                    no_model: false,
                    semantic_url: 'g_tron',

                },
            ],
            id: 'А3',
            name: 'А3',
            itemFilterParams: { model: 'A3' },
            popular: false,
            year_from: 1996,
            year_to: 2020,
        },
        {
            count: 60,
            reviews_count: 22,
            cyrillic_name: 'А4',
            generations: [ {
                count: 0,
                reviews_count: 22,
                id: '21460328',
                itemFilterParams: { super_gen: '21460328' },
                super_gen: '21460328',
                mobilePhoto: '',
                name: 'V (B9) Рестайлинг',
                photo: '',
                yearFrom: 2020,
                yearTo: 2020,

            },
            {
                count: 0,
                reviews_count: 22,
                id: '214603280',
                itemFilterParams: { super_gen: '214603280' },
                super_gen: '214603280',
                mobilePhoto: '',
                name: 'V',
                photo: '',
                yearFrom: 2020,
                yearTo: 2020,

            } ],
            id: 'А4',
            name: 'А4',
            itemFilterParams: { model: 'A4' },
            popular: false,
            year_from: 1996,
            year_to: 2020,
        },
    ],
};

describe('правильный текст в свернутом фильтре', () => {
    it('если есть только марка', () => {
        const infoMark = {
            mark: info.mark,
        };

        const wrapper = shallow(
            <MiniFilter
                item={{}}
                index={ 0 }
                info={ infoMark }
                marks={ [] }
                mmmLength={ 1 }
                onMmmClear={ noop }
                onRequestOpen={ noop }
                withGenerations
            />,
        );
        expect((wrapper.find('PseudoInputGroup').props() as PseudoInputGroupProps).title).toBe('Audi');
        expect((wrapper.find('PseudoInputGroup').props() as PseudoInputGroupProps).subtitle).toBe('Все модели / Все поколения');
    });

    it('если есть только марка и модель, фильтр с withGenerations', () => {
        const infoMarkModel = {
            mark: info.mark,
            models: [ cloneDeep(info.models[0]) ],
        };

        infoMarkModel.models[0].generations = [];

        const wrapper = shallow(
            <MiniFilter
                item={{}}
                index={ 0 }
                info={ infoMarkModel }
                marks={ [] }
                mmmLength={ 1 }
                onMmmClear={ noop }
                onRequestOpen={ noop }
                withGenerations
            />,
        );
        expect((wrapper.find('PseudoInputGroup').at(0).props() as PseudoInputGroupProps).title).toBe('Audi');
        expect((wrapper.find('PseudoInputGroup').at(0).props() as PseudoInputGroupProps).subtitle).toBe('А3 / Все поколения');
    });

    it('если есть только марка и модель, фильтр без withGenerations', () => {
        const infoMarkModel = {
            mark: info.mark,
            models: [ cloneDeep(info.models[0]) ],
        };

        infoMarkModel.models[0].generations = [];

        const wrapper = shallow(
            <MiniFilter
                item={{}}
                index={ 0 }
                info={ infoMarkModel }
                marks={ [] }
                mmmLength={ 1 }
                onMmmClear={ noop }
                onRequestOpen={ noop }
                withGenerations={ false }
            />,
        );
        expect((wrapper.find('PseudoInputGroup').at(0).props() as PseudoInputGroupProps).title).toBe('Audi');
        expect((wrapper.find('PseudoInputGroup').at(0).props() as PseudoInputGroupProps).subtitle).toBe('А3');
    });

    it('если есть только марка, несколько моделей и поколений', () => {
        const wrapper = shallow(
            <MiniFilter
                item={{}}
                index={ 0 }
                info={ info }
                marks={ [] }
                mmmLength={ 1 }
                onMmmClear={ noop }
                onRequestOpen={ noop }
                withGenerations
            />,
        );
        expect((wrapper.find('PseudoInputGroup').props() as PseudoInputGroupProps).title).toBe('Audi');
        expect((wrapper.find('PseudoInputGroup').props() as PseudoInputGroupProps).subtitle)
            .toBe('А3 (IV (8Y)), А4 (V (B9) Рестайлинг, V)');
    });
});

// mmmLength может быть 0 или 1
// catalog_filter = [] или catalog_filter=[{}] по идее одно и то же
it('должен отрисовать пустой инпут, если это единственный фильтр (mmmLength=0)', () => {
    const wrapper = shallow(
        <MiniFilter
            item={{}}
            index={ 0 }
            info={{}}
            marks={ [] }
            mmmLength={ 0 }
            onMmmClear={ noop }
            onRequestOpen={ noop }
            withGenerations
        />,
    );

    expect(wrapper.find('PseudoInput')).toExist();
});

it('должен отрисовать пустой инпут, если это единственный фильтр (mmmLength=1)', () => {
    const wrapper = shallow(
        <MiniFilter
            item={{}}
            index={ 0 }
            info={{}}
            marks={ [] }
            mmmLength={ 1 }
            onMmmClear={ noop }
            onRequestOpen={ noop }
            withGenerations
        />,
    );

    expect(wrapper.find('PseudoInput')).toExist();
});

it('не должен отрисовать пустой инпут, если это не единственный фильтр', () => {
    const wrapper = shallow(
        <MiniFilter
            item={{}}
            index={ 1 }
            info={{}}
            marks={ [] }
            mmmLength={ 2 }
            onMmmClear={ noop }
            onRequestOpen={ noop }
            withGenerations
        />,
    );

    expect(wrapper).toBeEmptyRender();
});
