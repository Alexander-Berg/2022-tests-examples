import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { YaDealValuationLinks } from '../index';

const LINK = [
    {
        id: '1',
        name: 'Стоимость продажи 1-комнатной квартиры',
        url: '/calculator-stoimosti/kvartira/odnokomnatnaya/',
    },
];

const LINKS = [
    {
        id: '1',
        name: 'Стоимость продажи 1-комнатной квартиры',
        url: '/calculator-stoimosti/kvartira/odnokomnatnaya/',
        offersCount: 12345,
    },
    {
        id: '2',
        name: 'Стоимость продажи 1-комнатной квартиры',
        url: '/calculator-stoimosti/kvartira/odnokomnatnaya/',
        offersCount: 54321,
    },
    {
        id: '3',
        name: 'Стоимость продажи 1-комнатной квартиры',
        url: '/calculator-stoimosti/kvartira/odnokomnatnaya/',
        offersCount: 67890,
    },
];

interface ILinksProps {
    links: Array<ILinkProps>;
}

interface ILinkProps {
    id: string;
    name: string;
    url: string;
    offersCount?: number;
}

const Component: React.FunctionComponent<ILinksProps> = ({ links }) => (
    <AppProvider>
        <YaDealValuationLinks links={links} />
    </AppProvider>
);

const renderOptions = { viewport: { width: 320, height: 200 } };

const render = async (component: React.ReactElement) => {
    await _render(component, renderOptions);

    expect(await takeScreenshot()).toMatchImageSnapshot();
};

it('рендерится 1 ссылка', async () => {
    await render(<Component links={LINK} />);
});

it('рендерится 3 ссылки с количеством офферов', async () => {
    await render(<Component links={LINKS} />);
});
