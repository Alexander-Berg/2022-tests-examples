/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

require('jest-enzyme');
const React = require('react');
const { mount, shallow } = require('enzyme');

const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock').data;
const linkDesktop = require('auto-core/router/auto.ru/react/link');
const link = require('auto-core/router/m.auto.ru/react/link');

const CatalogFilters = require('./CatalogFilters');

const params = {
    category: 'cars',
    mark: 'audi',
    model: 'a4',
    super_gen: '20637504',
    configuration_id: '20637561',
};

it('должен отрендерить фильтр выбора марки и строку с кнопкой "Все параметры"', () => {
    const wrapper = shallow(
        <CatalogFilters
            params={{ category: params.category }}
            breadcrumbsPublicApi={ breadcrumbsPublicApiMock }
        />,
        { context: { linkDesktop, link } },
    );

    expect(wrapper.find('.ShowAllButton')).toExist();
});

it('должен отрендерить фильтр выбора модели', () => {
    const wrapper = shallow(
        <CatalogFilters
            params={{ category: params.category, mark: params.mark }}
            breadcrumbsPublicApi={ breadcrumbsPublicApiMock }
        />,
        { context: { linkDesktop, link } },
    );

    expect(wrapper.find('.CatalogFilters__modelField')).toExist();
});

it('должен отрендерить фильтр выбора поколения', () => {
    const wrapper = mount(
        <CatalogFilters
            params={{ category: params.category, mark: params.mark, model: params.model }}
            breadcrumbsPublicApi={ breadcrumbsPublicApiMock }
        />,
        { context: { linkDesktop, link } },
    );

    expect(wrapper.find('.CatalogFilters__select')).toExist();
});

it('должен отрендерить фильтр выбора кузова', () => {
    const wrapper = shallow(
        <CatalogFilters
            params={{
                category: params.category,
                mark: params.mark,
                model: params.model,
                super_gen: params.super_gen,
            }}
            breadcrumbsPublicApi={ breadcrumbsPublicApiMock }
        />,
        { context: { linkDesktop, link } },
    );

    expect(wrapper.find('.CatalogFilters__bodyType')).toExist();
});

it('должен отрендерить фильтр выбора комплектации', () => {
    const wrapper = shallow(
        <CatalogFilters
            params={ params }
            breadcrumbsPublicApi={ breadcrumbsPublicApiMock }
            configurations={ [] }
        />,
        { context: { linkDesktop, link } },
    );

    expect(wrapper.find('.CatalogFilters__complectation')).toExist();
});
