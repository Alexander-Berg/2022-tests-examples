import * as React from 'react';
import { render } from 'jest-puppeteer-react';

import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IModelChat } from 'view/models';

// eslint-disable-next-line @realty-front/no-relative-imports
import DateMock from '../../../../mocks/components/DateMock';

import ChatListItem from '../ChatListItem';

import mocks from './ChatListItem.mocks';

const mockStore = configureStore([thunk]);

interface IProps {
    chat: IModelChat;
    selected: boolean;
}

function renderComponent(props: IProps) {
    const store = mockStore({});

    return render(
        <Provider store={store}>
            <DateMock date="02-03-2020 13:00">
                <ChatListItem {...props} />
            </DateMock>
        </Provider>,
        { viewport: { width: 384, height: 152 } }
    );
}

it('с тех поддержкой', async () => {
    await renderComponent(mocks.tech_support);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('выделенный', async () => {
    await renderComponent(mocks.selected);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с коротким сообщением', async () => {
    await renderComponent(mocks.short_message);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с вчерашним сообщением', async () => {
    await renderComponent(mocks.yesterday_message);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с позавчерашним сообщением', async () => {
    await renderComponent(mocks.day_before_yesterday_message);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с прошлогодним сообщением', async () => {
    await renderComponent(mocks.last_year_message);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('непрочитанный', async () => {
    await renderComponent(mocks.unread);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('замьюченный', async () => {
    await renderComponent(mocks.muted);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('замьюченный с прошлогодним сообщением', async () => {
    await renderComponent(mocks.muted_last_year_message);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с аттачем', async () => {
    await renderComponent(mocks.attachment);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с продавцом', async () => {
    await renderComponent(mocks.seller);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с покупателем', async () => {
    await renderComponent(mocks.buyer);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с проданным оффером', async () => {
    await renderComponent(mocks.sold);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('заблокированный', async () => {
    await renderComponent(mocks.blocked);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('без аватарки недвижимость', async () => {
    await renderComponent(mocks.without_avatar);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с застройщиком недвижимость', async () => {
    await renderComponent(mocks.developer);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с аватарки', async () => {
    await renderComponent(mocks.with_avatar);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('без аватарки но с именем', async () => {
    await renderComponent(mocks.with_nickname);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('центр нотификаций', async () => {
    await renderComponent(mocks.notification_center);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});
