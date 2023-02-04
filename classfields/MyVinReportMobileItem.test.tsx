import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import type { RawVinReport } from 'auto-core/react/dataDomain/vinReport/types';
import myReportsMock from 'auto-core/react/dataDomain/myReports/mocks/myReports.mock';

import MyVinReportMobileItem from './MyVinReportMobileItem';

const ContextProvider = createContextProvider(contextMock);

it('должен отрендерить кнопку "Объявление"', () => {
    const page = shallow(
        <ContextProvider>
            <MyVinReportMobileItem
                vinReport={ myReportsMock.reports[0] }
            />
        </ContextProvider>,
    ).dive();

    const offerLink = page.find('Button').at(1);
    expect(offerLink.dive().text()).toBe('Объявление');
});

it('не должен отрендерить кнопку "Объявление"', () => {
    const page = shallow(
        <ContextProvider>
            <MyVinReportMobileItem
                vinReport={ myReportsMock.reports[1] }
            />
        </ContextProvider>,
    ).dive();

    const offerLink = page.find('Button').at(1);
    expect(offerLink.isEmptyRender()).toBe(true);
});

it('должен отрендерить кнопку "В избранное"', () => {
    const page = shallow(
        <ContextProvider>
            <MyVinReportMobileItem
                vinReport={ myReportsMock.reports[0] }
            />
        </ContextProvider>,
    ).dive();

    const button = page.find('.MyVinReportMobileItem__favoriteButton');
    expect(button.isEmptyRender()).toBe(false);
});

it('не должен отрендерить кнопку про избранное', () => {
    const page = shallow(
        <ContextProvider>
            <MyVinReportMobileItem
                vinReport={ myReportsMock.reports[1] }
            />
        </ContextProvider>,
    ).dive();

    const button = page.find('.MyVinReportMobileItem__favoriteButton');
    expect(button.isEmptyRender()).toBe(true);
});

// тупой тест, нельзя так писать
// но прикрывать костылями хуевые данные тоже не очень красиво
// так что какой кейс, такой и тест
it('не должен падать, если в отчете нет каких-то блоков', () => {
    const page = shallow(
        <ContextProvider>
            <MyVinReportMobileItem
                vinReport={{} as RawVinReport}
            />
        </ContextProvider>,
    ).dive();

    expect(page.isEmptyRender()).toBe(false);
});
