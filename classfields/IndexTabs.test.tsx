/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import 'jest-enzyme';
import { shallow } from 'enzyme';
import React from 'react';

import contextMock from 'autoru-frontend/mocks/contextMock';

import IndexTabs from './IndexTabs';

describe('Гараж', () => {
    it('должен добавить ссылку "Гараж" если пользователь НЕ дилер', () => {
        const wrapper = shallow(
            <IndexTabs pageId="pageId"/>,
            { context: contextMock },
        );
        const headerTabs = wrapper.find('Header2Tabs').prop('tabs');
        const headerTabsList = (headerTabs as unknown as Array<{ name: string; url: string }>).map(item => item.name);

        expect(headerTabsList).toEqual([
            'Транспорт',
            'ПроАвто',
            'Кредиты',
            'Электромобили',
            'ОСАГО',
            'Гараж',
            'Отзывы',
            'Журнал',
            'Видео',
        ]);
    });

    it('не должен добавлять ссылку "Гараж" если пользователь дилер', () => {
        const wrapper = shallow(
            <IndexTabs pageId="pageId" isDealerUser/>,
            { context: contextMock },
        );
        const headerTabs = wrapper.find('Header2Tabs').prop('tabs');
        const headerTabsList = (headerTabs as unknown as Array<{ name: string; url: string }>).map(item => item.name);

        expect(headerTabsList).toEqual([
            'Транспорт',
            'ПроАвто',
            'Кредиты',
            'Электромобили',
            'ОСАГО',
            'Отзывы',
            'Журнал',
            'Видео',
        ]);
    });
});

describe('Ссылка на Выкуп', () => {
    it('Рендерится, если передан флаг', () => {
        const wrapper = shallow(
            <IndexTabs pageId="pageId" isBuyoutLinkVisible={ true }/>,
            { context: contextMock },
        );

        const headerTabs = wrapper.find('Header2Tabs').prop('tabs');
        const headerTabsList = (headerTabs as unknown as Array<{ name: string; url: string }>).map(item => item.name);

        expect(headerTabsList).toEqual([
            'Транспорт',
            'ПроАвто',
            'Кредиты',
            'Электромобили',
            'ОСАГО',
            'Гараж',
            'Выкуп',
            'Отзывы',
            'Журнал',
            'Видео',
        ]);
    });

    it('Не рендерится, если не передан соответствующий флаг', () => {
        const wrapper = shallow(
            <IndexTabs pageId="pageId"/>,
            { context: contextMock },
        );

        const headerTabs = wrapper.find('Header2Tabs').prop('tabs');
        const headerTabsList = (headerTabs as unknown as Array<{ name: string; url: string }>).map(item => item.name);

        expect(headerTabsList).toEqual([
            'Транспорт',
            'ПроАвто',
            'Кредиты',
            'Электромобили',
            'ОСАГО',
            'Гараж',
            'Отзывы',
            'Журнал',
            'Видео',
        ]);
    });
});

describe('ссылка на отчёты с метриками', () => {
    it('рендерится как ПроАвто', () => {
        const page = shallow(
            <IndexTabs pageId="pageId"/>,
            { context: { ...contextMock } },
        ).dive();

        const link = page.find('Link[url="link/proauto-landing/?from=shapka"]').dive().find('a');
        expect(link.text()).toEqual('ПроАвто');

        link.simulate('click');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'from_top_menu_secondline' ]);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'header', 'history', 'click' ]);
    });

    it('в экспе рендерится как Отчёты', () => {
        const page = shallow(
            <IndexTabs pageId="pageId"/>,
            {
                context: {
                    ...contextMock,
                    hasExperiment: (flag: string) => flag === 'AUTORUFRONT-21544_rename_proauto',
                },
            },
        ).dive();

        const link = page.find('Link[url="link/proauto-landing/?from=shapka"]').dive().find('a');
        expect(link.text()).toEqual('Отчёты');

        link.simulate('click');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'from_top_menu_secondline' ]);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'header', 'history', 'click', 'reports' ]);
    });
});
