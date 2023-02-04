import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { toggleAddressPurchaseSuggest } from 'realty-core/view/react/modules/egrn-address-purchase/actions';

import { EGRNAddressPurchaseSearchBtn } from '../.';
import { IEGRNAddressPurchaseSearchBtnProps } from '../types';

const Component = (props: Partial<IEGRNAddressPurchaseSearchBtnProps> = {}) => (
    <EGRNAddressPurchaseSearchBtn
        isLoading={false}
        showSearchBtn={false}
        toggleAddressPurchaseSuggest={toggleAddressPurchaseSuggest}
        {...props}
    />
);

describe('EGRNAddressPurchaseSearchBtn', () => {
    it('рендерится в дефолтном состоянии', async () => {
        await render(<Component />, {
            viewport: { width: 360, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('дизейблится при isLoading true', async () => {
        await render(<Component isLoading />, {
            viewport: { width: 360, height: 100 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('по умолчанию скрывается', async () => {
        await render(
            <div>
                <div style={{ height: 300 }} />
                <Component />
            </div>,
            {
                viewport: { width: 360, height: 300 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('прилипает к низу экрана', async () => {
        await render(
            <div>
                <div style={{ height: 300 }} />
                <Component showSearchBtn />
            </div>,
            {
                viewport: { width: 360, height: 300 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
