import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { DevchatComplaintVerdict, IDevchat } from 'types/devchat';

import { DevchatList } from '../';

const renderOptions = { viewport: { width: 1250, height: 400 } };

const devchats: IDevchat[] = [
    { roomId: '1', itemId: '1', itemCreationTime: '2024-9-24', buyerPhone: '+79991160920', canComplain: true },
    { roomId: '2', itemId: '2', itemCreationTime: '2024-9-24', canComplain: false },
    {
        roomId: '3',
        itemId: '3',
        itemCreationTime: '2024-9-24',
        buyerPhone: '+7999116****',
        complaint: { verdict: DevchatComplaintVerdict.CHAT_NOT_OK, creationTime: '2021-9-21', comment: 'obama' },
        canComplain: true,
    },
    {
        roomId: '4',
        itemId: '4',
        itemCreationTime: '2024-9-24',
        buyerPhone: '+7999116****',
        complaint: { verdict: DevchatComplaintVerdict.CHAT_OK, creationTime: '2021-9-21', comment: 'trump' },
        canComplain: true,
    },
    {
        roomId: '5',
        itemId: '5',
        itemCreationTime: '2024-9-24',
        buyerPhone: '+79991160920',
        complaint: { creationTime: '2021-9-21', comment: 'bush' },
        canComplain: true,
    },
];

describe('DevchatList', () => {
    it('Рендерится с несколькими чатами', async () => {
        await render(
            <DevchatList
                chats={devchats}
                onOpenChatMessages={noop}
                onComplain={() => Promise.resolve()}
                hasComplainRight
            />,
            renderOptions
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится с несколькими чатами без прав на создание жалобы', async () => {
        await render(
            <DevchatList
                chats={devchats}
                onOpenChatMessages={noop}
                onComplain={() => Promise.resolve()}
                hasComplainRight={false}
            />,
            renderOptions
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
