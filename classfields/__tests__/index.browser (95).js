import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SiteSnippetSearchSamolet } from '../index';

import { getItem } from './mocks';

const Wrapper = ({ children }) => {
    return (
        <div
            style={{ 'font-size': '14px',
                'line-height': '18px',
                position: 'relative',
                display: 'flex',
                overflow: 'hidden',
                '-webkit-box-orient': 'vertical',
                '-webkit-box-direction': 'normal',
                'flex-direction': 'column',
                margin: '0 -16px',
                padding: '16px 16px 0',
                'border-radius': '16px',
                'background-color': '#fff'
            }}
        >{children}
        </div>
    );
};

const WrappedComponent = props => (
    <AppProvider initialState={{ user: { favoritesMap: {} } }}>
        {/* eslint-disable-next-line max-len */}
        <SiteSnippetSearchSamolet Component={Wrapper} {...props} createStatsSelector={() => {}} />
    </AppProvider>
);

describe('SiteSnippetSearchSamolet', () => {
    it('Рендерится со всеми тегами', async() => {
        await render(
            <WrappedComponent item={getItem({ withOffers: true, withBuldingClass: true, withProposals: true })} />,
            {
                viewport: { width: 360, height: 600 }
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится со всеми тегами без оферов', async() => {
        await render(
            <WrappedComponent item={getItem({ withOffers: false, withBuldingClass: true, withProposals: true })} />,
            {
                viewport: { width: 360, height: 600 }
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится со всеми тегами без класса', async() => {
        await render(
            <WrappedComponent item={getItem({ withOffers: true, withBuldingClass: false, withProposals: true })} />,
            {
                viewport: { width: 360, height: 600 }
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится со всеми тегами без акций', async() => {
        await render(
            <WrappedComponent item={getItem({ withOffers: true, withBuldingClass: true, withProposals: false })} />,
            {
                viewport: { width: 360, height: 600 }
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится без тегов', async() => {
        await render(
            <WrappedComponent item={getItem({ withOffers: false, withBuldingClass: false, withProposals: false })} />,
            {
                viewport: { width: 360, height: 600 }
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
