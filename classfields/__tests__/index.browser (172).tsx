import React from 'react';
import noop from 'lodash/noop';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MessagePanelBanReasonsModal, IMessagePanelBanReasonsModal } from '../index';

// eslint-disable-next-line
const Component: React.FunctionComponent<Partial<IMessagePanelBanReasonsModal>> = (props: any) => (
    <MessagePanelBanReasonsModal visible onModalClose={noop} {...props} />
);

const VIEWPORT = {
    width: 800,
    height: 800,
};

describe('MessagePanelBanReasonsModal', () => {
    it('Текстовая причина', async () => {
        const banReasons = [
            {
                errorCode: 'LOCATION_NOT_FOUND',
                editable: true,
                title: 'не указан номер квартиры или кадастровый номер',
                messageText:
                    // eslint-disable-next-line max-len
                    'Пропущено обязательное для вашего региона поле: кадастровый номер <cadastral-number> или номер квартиры <apartment>. Добавьте хотя бы одно из них, чтобы объявление вернулось на сайт.',
            },
        ];

        await render(<Component banReasons={banReasons} />, {
            viewport: VIEWPORT,
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('HTML причина', async () => {
        const banReasons = [
            {
                errorCode: 'LOCATION_NOT_FOUND',
                editable: true,
                title: 'не указан номер квартиры или кадастровый номер',
                messageHtml:
                    'Количество тегов &#60;room-space&#62;' +
                    'не соответствует &#60;rooms&#62; или &#60;rooms-offered&#62;.' +
                    'Скорее всего, вы пропустили элемент или добавили лишний.' +
                    'Сверьтесь с <a class="link" href="https://yandex.ru" target="_blank">«Требованиями к фидам»</a>' +
                    'и исправьте неточности.',
            },
        ];

        await render(<Component banReasons={banReasons} />, {
            viewport: VIEWPORT,
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Много причин', async () => {
        const banReasons = [
            {
                errorCode: 'LOCATION_NOT_FOUND',
                editable: true,
                title: 'не указан номер квартиры или кадастровый номер',
                messageText:
                    // eslint-disable-next-line max-len
                    'Пропущено обязательное для вашего региона поле: кадастровый номер <cadastral-number> или номер квартиры <apartment>. Добавьте хотя бы одно из них, чтобы объявление вернулось на сайт.',
            },
            {
                errorCode: 'AD_ON_PHOTO',
                editable: true,
                title: 'Фотографии не соответствуют описанию',
                messageText: 'Фотографии не соответствуют или противоречат описанию.',
            },
            {
                errorCode: 'LOCATION_NOT_FOUND',
                editable: true,
                title: 'не указан номер квартиры или кадастровый номер',
                messageHtml:
                    'Количество тегов &#60;room-space&#62;' +
                    'не соответствует &#60;rooms&#62; или &#60;rooms-offered&#62;.' +
                    'Скорее всего, вы пропустили элемент или добавили лишний.' +
                    'Сверьтесь с <a class="link" href="https://yandex.ru" target="_blank">«Требованиями к фидам»</a>' +
                    'и исправьте неточности.',
            },
            {
                errorCode: 'LOCATION_NOT_FOUND',
                editable: true,
                title: 'Неполный адрес или метка',
                messageText: 'Неполный адрес или неточная метка на карте.',
            },
        ];

        await render(<Component banReasons={banReasons} />, {
            viewport: VIEWPORT,
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
