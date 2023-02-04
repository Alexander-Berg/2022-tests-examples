import React from 'react';
import FilterPresets from './FilterPresets';
import { render, screen } from '@testing-library/react';
import defaultPropsMock from './mocks/defaultProps.mock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

const Context = createContextProvider(contextMock);
describe('Клиент без мультипостинга', () => {
    it('Пресеты для табы Все объявления', () => {
        render(
            <Context>
                <FilterPresets
                    { ...defaultPropsMock }
                    routeParams={{
                        all: '1',
                    }}
                    isMultipostingEnabled={ false }
                />
            </Context>);
        expect(screen.getByText('Сняты с продажи')).toBeDefined();
        expect(screen.getByText('Причины блокировки')).toBeDefined();
        expect(screen.getByText('Недостаточно средств для размещения')).toBeDefined();
    });

    it('Пресеты для табы Ждут активации', () => {
        render(
            <Context>
                <FilterPresets
                    { ...defaultPropsMock }
                    routeParams={{
                        status: 'need_activation',
                    }}
                    isMultipostingEnabled={ false }
                />
            </Context>);

        expect(screen.getByText('Причины блокировки')).toBeDefined();
    });

    it('Пресеты для табы Активные', () => {
        render(
            <Context>
                <FilterPresets
                    { ...defaultPropsMock }
                    routeParams={{
                        status: 'active',
                    }}
                    isMultipostingEnabled={ false }
                />
            </Context>);

        expect(screen.getByText('Причины блокировки')).toBeDefined();
    });

    it('Пресеты для табы Неактивные', () => {
        render(
            <Context>
                <FilterPresets
                    { ...defaultPropsMock }
                    routeParams={{
                        status: 'notActive',
                    }}
                    isMultipostingEnabled={ false }
                />
            </Context>);

        expect(screen.getByText('Сняты с продажи')).toBeDefined();
        expect(screen.getByText('Причины блокировки')).toBeDefined();
        expect(screen.getByText('Недостаточно средств для размещения')).toBeDefined();
    });

    it('Пресеты для табы Заблокированные', () => {
        render(
            <Context>
                <FilterPresets
                    { ...defaultPropsMock }
                    routeParams={{
                        status: 'notActive',
                    }}
                    isMultipostingEnabled={ false }
                />
            </Context>,
        );
        expect(screen.getByText('Причины блокировки')).toBeDefined();
    });

    it('Пресеты для табы Архив', () => {
        render(
            <Context>
                <FilterPresets
                    { ...defaultPropsMock }
                    routeParams={{
                        status: 'removed',
                    }}
                    isMultipostingEnabled={ false }
                />
            </Context>,
        );
        expect(document.querySelectorAll('button')).toHaveLength(0);
    });
});

describe('клиент с мультипостингом', () => {
    it('Пресеты для табы Все объявления', () => {
        render(
            <Context>
                <FilterPresets
                    { ...defaultPropsMock }
                    routeParams={{
                        all: '1',
                    }}
                    isMultipostingEnabled={ true }
                />
            </Context>);
        expect(screen.getByText('Размещаются только на странице дилера Авто.ру')).toBeDefined();
        expect(screen.getByText('Причины блокировки')).toBeDefined();
        expect(screen.getByText('Недостаточно средств для размещения на Авто.ру')).toBeDefined();
    });

    it('Пресеты для табы В продаже', () => {
        render(
            <Context>
                <FilterPresets
                    { ...defaultPropsMock }
                    routeParams={{
                        multiposting_status: 'active',
                    }}
                    isMultipostingEnabled={ true }
                />
            </Context>);
        expect(screen.getByText('Размещаются только на странице дилера Авто.ру')).toBeDefined();
        expect(screen.getByText('Недостаточно средств для размещения на Авто.ру')).toBeDefined();
    });

    it('Пресеты для табы Снятые с продажи', () => {
        render(
            <Context>
                <FilterPresets
                    { ...defaultPropsMock }
                    routeParams={{
                        multiposting_status: 'inactive',
                    }}
                    isMultipostingEnabled={ true }
                />
            </Context>);
        expect(document.querySelectorAll('button')).toHaveLength(0);
    });

    it('Должен нарисовать заглушку для табы Заблокированные', () => {
        render(
            <Context>
                <FilterPresets
                    { ...defaultPropsMock }
                    routeParams={{
                        status: 'banned',
                    }}
                    isMultipostingEnabled={ true }
                />
            </Context>);
        expect(screen.getByText('Причины блокировки')).toBeDefined();
    });

    it('Должен нарисовать заглушку для табы Архив', () => {
        render(
            <Context>
                <FilterPresets
                    { ...defaultPropsMock }
                    routeParams={{
                        status: 'removed',
                    }}
                    isMultipostingEnabled={ true }
                />
            </Context>);
        expect(document.querySelectorAll('button')).toHaveLength(0);
    });
});
