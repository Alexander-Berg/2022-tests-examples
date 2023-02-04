import 'jest-enzyme';
import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import type { RawVinReport } from 'auto-core/react/dataDomain/vinReport/types';
import myReportsMock from 'auto-core/react/dataDomain/myReports/mocks/myReports.mock';

import MyVinReportDesktopItem from './MyVinReportDesktopItem';

const ContextProvider = createContextProvider(contextMock);

it('должен отрендерить кнопку "Объявление"', () => {
    const page = shallow(
        <ContextProvider>
            <MyVinReportDesktopItem
                vinReport={ myReportsMock.reports[0] }
            />
        </ContextProvider>,
    ).dive();

    const offerLink = page.find('Link').at(1);
    expect(offerLink.dive().text()).toBe('Объявление');
});

it('не должен отрендерить кнопку "Объявление"', () => {
    const page = shallow(
        <ContextProvider>
            <MyVinReportDesktopItem
                vinReport={ myReportsMock.reports[1] }
            />
        </ContextProvider>,
    ).dive();

    const offerLink = page.find('Link').at(1);
    expect(offerLink.isEmptyRender()).toBe(true);
});

it('должен отрендерить кнопку "В избранное"', () => {
    const page = shallow(
        <ContextProvider>
            <MyVinReportDesktopItem
                vinReport={ myReportsMock.reports[0] }
            />
        </ContextProvider>,
    ).dive();

    const favLink = page.find('Link').last();
    expect(favLink.dive().text()).toBe('В избранное');
});

it('должен отрендерить кнопку "В избранном"', () => {
    const mock = _.cloneDeep(myReportsMock.reports[0]);
    mock.report_offer_info!.is_favorite = true;
    const page = shallow(
        <ContextProvider>
            <MyVinReportDesktopItem
                vinReport={ mock }
            />
        </ContextProvider>,
    ).dive();

    const favLink = page.find('Link').last();
    expect(favLink.dive().text()).toBe('В избранном');
});

it('не должен отрендерить кнопку про избранное', () => {
    const page = shallow(
        <ContextProvider>
            <MyVinReportDesktopItem
                vinReport={ myReportsMock.reports[1] }
            />
        </ContextProvider>,
    ).dive();

    const favLink = page.find('Link').at(1);
    expect(favLink.isEmptyRender()).toBe(true);
});

// тупой тест, нельзя так писать
// но прикрывать костылями хуевые данные тоже не очень красиво
// так что какой кейс, такой и тест
it('не должен падать, если в отчете нет каких-то блоков', () => {
    const page = shallow(
        <ContextProvider>
            <MyVinReportDesktopItem
                vinReport={{} as RawVinReport}
            />
        </ContextProvider>,
    ).dive();

    expect(page.isEmptyRender()).toBe(false);
});
