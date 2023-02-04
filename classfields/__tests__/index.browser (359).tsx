import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferCardActions } from '../index';

import { getProps, getInitialState, context } from './mocks';

const renderComponent = ({ initialState = getInitialState(), props = getProps() } = {}) =>
    render(
        <AppProvider initialState={initialState} context={context}>
            {/*eslint-disable-next-line @typescript-eslint/ban-ts-comment */}
            {/* @ts-ignore */}
            <OfferCardActions {...props} visible={true} placement="offer_base_info" />
        </AppProvider>,
        { viewport: { width: 350, height: 700 } }
    );

describe('OfferCardActions', () => {
    it('рисует блок (owner)', async () => {
        const props = { ...getProps({ isEditable: true }), isOwner: true };

        await renderComponent({ props });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует полный блок с ховером', async () => {
        await renderComponent();

        await page.click('[class*="item"]');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует полный блок с избранным, заметкой, сравнением и с возможностью скрыть', async () => {
        await renderComponent({
            initialState: getInitialState({ favoritesMap: { 1: true } }),
            props: getProps({
                comparison: ['1'],
                userNote: '1',
                deletedByUser: true,
            }),
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
