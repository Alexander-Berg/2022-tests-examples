import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import FormGroupTypeLocation from '../';

const initialState = {
    offerForm: {
        category: 'APARTMENT',
        siteId: 1754609,
        location: {
            address: 'ЖК «Nobelius»',
            coords: [ 59.965275, 30.345936 ],
            rgid: 741965,
            hasSites: true,
            isKnown: true,
            country: 225,
            combinedAddress: 'Россия, Санкт-Петербург, Выборгская улица, 4к2'
        }
    },
    user: {},
    geo: {},
    page: {
        params: {}
    }
};

const activeControls = {
    apartment: { opts: { vos: { group: 'location' } }, type: 'text' },
    location: { opts: { isRequired: true }, type: 'location' }
};

describe('FormGroupTypeLocation', () => {
    it('common', async() => {
        const Gate = {
            create: () => Promise.resolve({})
        };

        await render(
            <AppProvider initialState={initialState} Gate={Gate}>
                <FormGroupTypeLocation
                    type='location'
                    activeControls={activeControls}
                    controlNames={[ 'apartment', 'location' ]}
                />
            </AppProvider>,
            { viewport: { width: 700, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
