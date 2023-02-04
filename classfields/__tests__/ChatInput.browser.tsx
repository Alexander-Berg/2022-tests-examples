import * as React from 'react';
import { render } from 'jest-puppeteer-react';

import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import * as _ from 'lodash';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IModelChat } from 'view/models';

import ChatInput from '../ChatInput';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
declare const page: any;

const mockStore = configureStore([thunk]);
const textareaSelector = '.ChatInput__textarea';

const mockChat = {} as IModelChat;

describe('desktop', () => {
    const store = mockStore({
        visible: true,
        config: { platform: 'desktop' },
    });

    const viewport = { width: 500, height: 150 };

    it('пустой инпут', async () => {
        await render(
            <Provider store={store}>
                <ChatInput chat={mockChat} chat_id="123" check_before_unload={false} on_input_resize={_.noop} />
            </Provider>,
            { viewport }
        );
        const screenshot = await takeScreenshot();

        expect(screenshot).toMatchImageSnapshot();
    });

    it('инпут с текстом', async () => {
        await render(
            <Provider store={store}>
                <ChatInput chat={mockChat} chat_id="123" check_before_unload={false} on_input_resize={_.noop} />
            </Provider>,
            { viewport }
        );
        await page.type(textareaSelector, 'Добрый день, ещё продаёте?');

        const screenshot = await takeScreenshot();

        expect(screenshot).toMatchImageSnapshot();
    });

    it('инпут с длинным текстом', async () => {
        await render(
            <Provider store={store}>
                <ChatInput chat={mockChat} chat_id="123" check_before_unload={false} on_input_resize={_.noop} />
            </Provider>,
            { viewport }
        );
        await page.type(
            textareaSelector,
            'Добрый день, ещё продаёте? А где можно посмотреть? А когда вам будет удобно?'
        );

        const screenshot = await takeScreenshot();

        expect(screenshot).toMatchImageSnapshot();
    });
});

describe('mobile', () => {
    const store = mockStore({
        visible: true,
        config: { platform: 'mobile' },
    });

    const viewport = { width: 360, height: 150 };

    it('пустой инпут', async () => {
        await render(
            <Provider store={store}>
                <ChatInput chat={mockChat} chat_id="123" check_before_unload={false} on_input_resize={_.noop} />
            </Provider>,
            { viewport }
        );
        const screenshot = await takeScreenshot();

        expect(screenshot).toMatchImageSnapshot();
    });

    it('инпут с текстом', async () => {
        await render(
            <Provider store={store}>
                <ChatInput chat={mockChat} chat_id="123" check_before_unload={false} on_input_resize={_.noop} />
            </Provider>,
            { viewport }
        );
        await page.type(textareaSelector, 'Добрый день, ещё продаёте?');

        const screenshot = await takeScreenshot();

        expect(screenshot).toMatchImageSnapshot();
    });

    it('инпут с длинным текстом', async () => {
        await render(
            <Provider store={store}>
                <ChatInput chat={mockChat} chat_id="123" check_before_unload={false} on_input_resize={_.noop} />
            </Provider>,
            { viewport: { width: 360, height: 200 } }
        );
        await page.type(
            textareaSelector,
            'Добрый день, ещё продаёте? А где можно посмотреть? А когда вам будет удобно?'
        );

        const screenshot = await takeScreenshot();

        expect(screenshot).toMatchImageSnapshot();
    });

    it('инпут с заданным стартовым текстом', async () => {
        await render(
            <Provider store={store}>
                <ChatInput
                    chat={mockChat}
                    chat_id="123"
                    check_before_unload={false}
                    on_input_resize={_.noop}
                    startMessage="Стартовое сообщение"
                />
            </Provider>,
            { viewport }
        );
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type(textareaSelector, '  и ещё немного текста');
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
