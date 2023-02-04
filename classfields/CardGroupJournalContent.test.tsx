import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import imageMock from 'auto-core/react/dataDomain/mag/imageMock';
import type { MagListingArticles } from 'auto-core/react/dataDomain/mag/StateMag';

import CardGroupJournalContent from './CardGroupJournalContent';

const mockData = { data: {
    articles: [
        {
            categories: [ { urlPart: 'news', title: 'Новости' } ],
            title: 'BMW X3 М и X4 М получили полный привод от M5',
            mainImage: imageMock.value(),
        },
    ] as MagListingArticles,
} };

const pageParams = { mark: 'BMW', model: 'X4', super_gen: 21203948 };

it('Рендерит CardGroupJournalContent, когда пришли данные', () => {
    const wrapper = shallow(
        <CardGroupJournalContent pageParams={ pageParams } utmCampaign="" data={ mockData }/>,
        { context: contextMock },
    );

    expect(wrapper).not.toBeEmptyRender();
});

it('Рендерит CardGroupJournalContent, когда данные не пришли', () => {
    const wrapper = shallow(
        <CardGroupJournalContent pageParams={ pageParams } utmCampaign="" data={ null }/>,
        { context: contextMock },
    );

    expect(wrapper).toBeEmptyRender();
});

it('Рендерит CardGroupJournalContent, когда пришел пустой массив', () => {
    const wrapper = shallow(
        <CardGroupJournalContent pageParams={ pageParams } utmCampaign="" data={{ data: { articles: [] } }}/>,
        { context: contextMock },
    );

    expect(wrapper).toBeEmptyRender();
});
