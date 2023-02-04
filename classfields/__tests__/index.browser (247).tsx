import React, { useEffect, useState } from 'react';
import noop from 'lodash/noop';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { INewbuildingCardGenPlanPopupProps, NewbuildingCardGenPlanPopup } from '..';

import { house, houseInfo } from './mocks';

const OpenModal: React.FC<Omit<INewbuildingCardGenPlanPopupProps, 'visible'>> = (props) => {
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        setVisible(true);
    }, []);

    return <NewbuildingCardGenPlanPopup {...props} visible={visible} />;
};

describe('NewbuildingCardGenPlanPopup', () => {
    it('Рендерится корректно', async () => {
        await render(
            <AppProvider>
                <OpenModal house={house} houseInfo={houseInfo} onClose={noop} />
            </AppProvider>,
            {
                viewport: { width: 320, height: 400 },
            }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('Рендерится в состоянии загрузки', async () => {
        await render(
            <AppProvider>
                <OpenModal house={{ ...house, loading: true }} houseInfo={houseInfo} onClose={noop} />
            </AppProvider>,
            {
                viewport: { width: 320, height: 400 },
            }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
