import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import MessagePanelCustomMessages from '../index';

describe('MessagePanelCustomMessages', () => {
    it('info message', async() => {
        const messages = [
            {
                id: 'no-photo',
                type: 'info',
                node: 'Добавьте фотографии, без них объявления смотрят реже.'
            }
        ];

        await render(
            <MessagePanelCustomMessages messages={messages} />,
            { viewport: { width: 1000, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('info messages', async() => {
        const messages = [
            {
                id: 'no-photo',
                type: 'info',
                node: 'Добавьте фотографии, без них объявления смотрят реже.'
            },
            {
                id: 'draft',
                type: 'info',
                node: 'Черновик объявления.'
            }
        ];

        await render(
            <MessagePanelCustomMessages messages={messages} />,
            { viewport: { width: 1000, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('warning message', async() => {
        const messages = [
            {
                id: 'no-photo',
                type: 'warning',
                node: 'Добавьте фотографии, без них объявления смотрят реже.'
            }
        ];

        await render(
            <MessagePanelCustomMessages messages={messages} />,
            { viewport: { width: 1000, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('warning messages', async() => {
        const messages = [
            {
                id: 'no-photo',
                type: 'warning',
                node: 'Добавьте фотографии, без них объявления смотрят реже.'
            },
            {
                id: 'draft',
                type: 'warning',
                node: 'Черновик объявления.'
            }
        ];

        await render(
            <MessagePanelCustomMessages messages={messages} />,
            { viewport: { width: 1000, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('error message', async() => {
        const messages = [
            {
                id: 'no-photo',
                type: 'error',
                node: 'Добавьте фотографии, без них объявления смотрят реже.'
            }
        ];

        await render(
            <MessagePanelCustomMessages messages={messages} />,
            { viewport: { width: 1000, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('error messages', async() => {
        const messages = [
            {
                id: 'no-photo',
                type: 'error',
                node: 'Добавьте фотографии, без них объявления смотрят реже.'
            },
            {
                id: 'draft',
                type: 'error',
                node: 'Черновик объявления.'
            }
        ];

        await render(
            <MessagePanelCustomMessages messages={messages} />,
            { viewport: { width: 1000, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
