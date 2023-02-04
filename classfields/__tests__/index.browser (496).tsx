import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { YaDealValuationLinks } from '../index';

const LINK = [
    {
        id: '1',
        name: 'Оценка стоимости продажи 1-комнатной квартиры',
        url: '/calculator-stoimosti/kvartira/odnokomnatnaya/',
    },
];

const LINKS = [
    {
        id: '1',
        name: 'Студии',
        url: '/calculator-stoimosti/kvartira/odnokomnatnaya/',
        offersCount: 12345,
    },
    {
        id: '2',
        name: '1-комнатные',
        url: '/calculator-stoimosti/kvartira/odnokomnatnaya/',
        offersCount: 54321,
    },
    {
        id: '3',
        name: '2-комнатные',
        url: '/calculator-stoimosti/kvartira/odnokomnatnaya/',
        offersCount: 67890,
    },
    {
        id: '4',
        name: '3-комнатные',
        url: '/calculator-stoimosti/kvartira/odnokomnatnaya/',
        offersCount: 12345,
    },
    {
        id: '5',
        name: 'Без посредников',
        url: '/calculator-stoimosti/kvartira/odnokomnatnaya/',
        offersCount: 54321,
    },
    {
        id: '6',
        name: 'Комнаты в квартире',
        url: '/calculator-stoimosti/kvartira/odnokomnatnaya/',
        offersCount: 67890,
    },
];

interface ILinksProps {
    links: Array<ILinkProps>;
    width?: string;
}

interface ILinkProps {
    id: string;
    name: string;
    url: string;
    offersCount?: number;
}

const Component: React.FunctionComponent<ILinksProps> = ({ links, width }) => (
    <AppProvider>
        <YaDealValuationLinks links={links} width={width} />
    </AppProvider>
);

const renderOptions = { viewport: { width: 1000, height: 150 } };

const render = async (component: React.ReactElement) => {
    await _render(component, renderOptions);

    expect(await takeScreenshot()).toMatchImageSnapshot();
};

it('рендерится 1 ссылка', async () => {
    await render(<Component links={LINK} />);
});

it('рендерится 3 ссылки с количеством офферов в три колонки', async () => {
    await render(<Component links={LINKS} width="full" />);
});
