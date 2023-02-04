import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { userReducer } from 'view/entries/user/reducer';

import { UserMessage, UserMessageType } from '../';

const renderOptions = [{ viewport: { width: 1000, height: 300 } }, { viewport: { width: 580, height: 300 } }];

const Component: React.FunctionComponent<{ type: UserMessageType }> = (props) => {
    const { type } = props;

    return (
        <AppProvider initialState={{ config: { isMobile: false } }} rootReducer={userReducer}>
            <UserMessage description={'Добавьте все счётчики, внесите их значения и сделайте фотографию'} type={type} />
        </AppProvider>
    );
};

describe('UserMessage', () => {
    Object.values(UserMessageType).forEach((type) => {
        describe(`${type}`, () => {
            describe(`Внешний вид`, () => {
                renderOptions.forEach((renderOption) => {
                    it(`${renderOption.viewport.width}px`, async () => {
                        await render(<Component type={type} />, renderOption);

                        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                    });
                });
            });
        });
    });
});
