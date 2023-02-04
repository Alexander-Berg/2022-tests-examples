import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import MessagePanelBanReasons from '../index';

const Component = props => <MessagePanelBanReasons {...props} />;

describe('MessagePanelBanReasons', () => {
    it('Без дополнительной информации', async() => {
        const banReasons = [
            {
                errorCode: 'AD_ON_PHOTO',
                editable: true,
                title: 'Фотографии не соответствуют описанию'
            }
        ];

        await render(
            <Component banReasons={banReasons} />,
            { viewport: { width: 1000, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Без дополнительной информации, юрик', async() => {
        const banReasons = [
            {
                errorCode: 'AD_ON_PHOTO',
                editable: true,
                title: 'Фотографии не соответствуют описанию'
            }
        ];

        await render(
            <Component banReasons={banReasons} extendedUserType='AGECY' />,
            { viewport: { width: 1000, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('С дополнительной информацией', async() => {
        const banReasons = [
            {
                errorCode: 'AD_ON_PHOTO',
                editable: true,
                title: 'Фотографии не соответствуют описанию',
                messageText: 'Фотографии не соответствуют или противоречат описанию.'
            }
        ];

        await render(
            <Component banReasons={banReasons} />,
            { viewport: { width: 1000, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Несколько причин с доп. информацией', async() => {
        const banReasons = [
            {
                errorCode: 'AD_ON_PHOTO',
                editable: true,
                title: 'Фотографии не соответствуют описанию',
                messageText: 'Фотографии не соответствуют или противоречат описанию.'
            },
            {
                errorCode: 'LOCATION_NOT_FOUND',
                editable: true,
                title: 'Неполный адрес или метка',
                messageText: 'Неполный адрес или неточная метка на карте.'
            },
            {
                errorCode: 'NOT_FOUND',
                editable: true,
                title: 'Не удалось получить цены на опции'
            }
        ];

        await render(
            <Component banReasons={banReasons} />,
            { viewport: { width: 1000, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
