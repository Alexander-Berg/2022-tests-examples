import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';
import { userReducer } from 'view/entries/user/reducer';

import { UserActionMessage, UserActionMessageIconType } from '../';

const renderOptions = [{ viewport: { width: 1000, height: 300 } }, { viewport: { width: 580, height: 300 } }];

const Component: React.FunctionComponent<{ iconType: UserActionMessageIconType }> = (props) => {
    const { iconType } = props;

    return (
        <AppProvider
            initialState={{ config: { isMobile: '' } }}
            rootReducer={userReducer}
            bodyBackgroundColor={AppProvider.PageColor.USER_LK}
        >
            <UserActionMessage
                text={'Комментарий менеджера'}
                iconType={iconType}
                onClick={() => {
                    return;
                }}
            />
        </AppProvider>
    );
};

describe('UserActionMessage', () => {
    Object.values(UserActionMessageIconType).forEach((iconType) => {
        describe(`${iconType}`, () => {
            describe(`Внешний вид`, () => {
                renderOptions.forEach((renderOption) => {
                    it(`${renderOption.viewport.width}px`, async () => {
                        await render(<Component iconType={iconType} />, renderOption);

                        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
                    });
                });
            });
        });
    });
});
