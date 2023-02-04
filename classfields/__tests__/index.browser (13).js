import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import Notification from '../index';

const DefaultNotification = props => (
    <Notification {...props}>
        Скоро истекает бабло. Сделай что-нибудь, а то в конце платёжного периода переведём на тариф
        «Минимальный» и ты потеряешь клиентов.
    </Notification>
);

describe('Notification', () => {
    it('default with close 1000', async() => {
        await render(
            <DefaultNotification onClose={() => {}} />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('default with button 1000', async() => {
        await render(
            <DefaultNotification buttonText={'Понятно'} />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('default with close and button 1400', async() => {
        await render(
            <DefaultNotification onClose={() => {}} buttonText={'Понятно'} />,
            { viewport: { width: 1400, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('warning 1000', async() => {
        await render(
            <DefaultNotification type='warning' />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('critical 1000', async() => {
        await render(
            <DefaultNotification type='critical' />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('warning without info icon 1000', async() => {
        await render(
            <DefaultNotification type='warning' withIcon={false} />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
