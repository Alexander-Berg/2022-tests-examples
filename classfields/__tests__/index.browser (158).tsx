import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyFunction } from 'realty-core/types/utils';

import {
    communicationChannels,
    FormAllowedCommunicationChannelsField,
    IFormAllowedCommunicationChannelsFieldProps,
} from '../index';

const defaultOnchange: AnyFunction = () => {
    /** */
};

const renderComponent = (
    value: communicationChannels[] | null,
    props: Partial<IFormAllowedCommunicationChannelsFieldProps> = {}
) =>
    render(
        <FormAllowedCommunicationChannelsField
            onChange={defaultOnchange}
            field={{
                id: 'id',
                value,
            }}
            id="id"
            {...props}
        />,
        { viewport: { width: 700, height: 70 } }
    );

describe('FormAllowedCommunicationChannelsField', () => {
    it('рендерится с пустым значением', async () => {
        await renderComponent(null);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
    it('рендерится задизейбленный', async () => {
        await renderComponent(null, { disabled: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
    it('рендерится с одним значением', async () => {
        await renderComponent([communicationChannels.COM_CALLS]);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
    it('рендерится с двумя значениями', async () => {
        await renderComponent([communicationChannels.COM_CALLS, communicationChannels.COM_CHATS]);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
