import * as React from 'react';
import { render } from 'jest-puppeteer-react';

import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IModelChat, IModelChatMessage } from 'view/models';

// eslint-disable-next-line @realty-front/no-relative-imports
import DateMock from '../../../../mocks/components/DateMock';

const mockStore = configureStore([thunk]);

import ChatMessage from '../ChatMessage';

import mocks from './ChatMessage.mocks';

interface IProps {
    message: IModelChatMessage;
    chat: IModelChat;
    last_of_group: boolean;
}

interface IViewport {
    width?: number;
    height?: number;
}

function renderComponent(props: IProps, { width = 500, height = 200 }: IViewport = {}) {
    const store = mockStore({ config: {} });
    return render(
        <Provider store={store}>
            <DateMock date="01-01-2020 13:00">
                <ChatMessage {...props} />
            </DateMock>
        </Provider>,
        { viewport: { width, height } }
    );
}

it('отправленное', async () => {
    await renderComponent(mocks.sent);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('отправленное 320px', async () => {
    await renderComponent(mocks.sent, { width: 360 });
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('отправленное с хвостиком', async () => {
    await renderComponent(mocks.sent_with_tail);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('полученное', async () => {
    await renderComponent(mocks.received);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('полученное с хвостиком', async () => {
    await renderComponent(mocks.received_with_tail);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('короткое', async () => {
    await renderComponent(mocks.brief);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('отправляемое', async () => {
    await renderComponent(mocks.queued);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('непрочитанное', async () => {
    await renderComponent(mocks.unread);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('прочитанное', async () => {
    await renderComponent(mocks.read);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('неотправленное', async () => {
    await renderComponent(mocks.failed);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('неотправленное с хвостиком', async () => {
    await renderComponent(mocks.failed_with_tail);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с несколькими пресетами', async () => {
    await renderComponent(mocks.presets, { width: 500, height: 340 });
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с одним пресетом', async () => {
    await renderComponent(mocks.with_one_preset, { width: 500, height: 340 });
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с несколькими командами бота', async () => {
    await renderComponent(mocks.bot_commands, { width: 500, height: 340 });
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с одной командой бота', async () => {
    await renderComponent(mocks.with_one_bot_commands, { width: 500, height: 340 });
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с опросом', async () => {
    await renderComponent(mocks.poll, { width: 500, height: 340 });
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с пройденным опросом', async () => {
    await renderComponent(mocks.poll_selected, { width: 500, height: 340 });
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('с html', async () => {
    await renderComponent(mocks.html);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('аттач', async () => {
    await renderComponent(mocks.attachment, { width: 500, height: 340 });
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('аттач с хвостиком', async () => {
    await renderComponent(mocks.attachment_with_tail, { width: 500, height: 340 });
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

// TODO: Придумать способ протестировать File
// eslint-disable-next-line jest/no-commented-out-tests
/*
it('отправляемый аттач', async() => {

});
*/

it('непрочитанный аттач', async () => {
    await renderComponent(mocks.attachment_unread, { width: 500, height: 340 });
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('прочитанный аттач', async () => {
    await renderComponent(mocks.attachment_read, { width: 500, height: 340 });
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('входящий звонок', async () => {
    await renderComponent(mocks.call_incoming);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('входящий пропущенный звонок', async () => {
    await renderComponent(mocks.call_incoming_missed);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('исходящий звонок', async () => {
    await renderComponent(mocks.call_outcoming);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});

it('исходящий пропущенный звонок', async () => {
    await renderComponent(mocks.call_outcoming_missed);
    const screenshot = await takeScreenshot();
    expect(screenshot).toMatchImageSnapshot();
});
