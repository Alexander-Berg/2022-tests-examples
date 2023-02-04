import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { generateImageAliases } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import Gate from 'realty-core/view/react/libs/gate';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { IProfileCard } from 'realty-core/types/profileCard';
import { UID, UserTypes } from 'realty-core/types/common';
import { WeekDays } from 'realty-core/types/datetime';

import { IProfilesPageStore, profilesReducer } from 'view/react/deskpad/reducers/roots/profiles';

import styles from '../styles.module.css';

import { ProfileSerpItem } from '../index';

advanceTo(new Date('2020-10-23 04:20'));

const VIEWPORTS = [
    { width: 1000, height: 200 },
    { width: 1400, height: 200 },
] as const;

const profileCardStub: Partial<IProfileCard> = {
    name: 'Уникальный agency',
    logo: generateImageAliases({ height: 72, width: 72 }),
    foundationDate: '2010-02-28T21:00:00Z',
    address: {
        unifiedAddress: 'Иркутск, улица Баррикад, 32',
        rgid: '3077',
        point: { latitude: 52.29173, longitude: 52.29173 },
    },
    workSchedule: [{ day: WeekDays.FRIDAY, minutesFrom: 510, minutesTo: 1200 }],
    description: 'kek',
    creationDate: '2020-05-26T12:57:50Z',
    userType: UserTypes.AGENCY,
    profileUid: '4043632751' as UID,
};

const MockGate = {
    get: (path: string) => {
        switch (path) {
            case 'profiles.get_redirect_phones': {
                return Promise.resolve({
                    phones: [{ wholePhoneNumber: '+79992134916' }, { wholePhoneNumber: '+79992134916' }],
                });
            }
        }
    },
} as typeof Gate;

const Component: React.FunctionComponent<{
    store?: DeepPartial<IProfilesPageStore>;
    profile: Partial<IProfileCard>;
    Gate: typeof Gate;
}> = ({ store = { profiles: { redirectPhones: {} } }, profile, Gate }) => (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    <AppProvider rootReducer={profilesReducer as any} initialState={store} context={{}} Gate={Gate}>
        <ProfileSerpItem profile={profile} />
    </AppProvider>
);

describe('ProfileSerpItem', () => {
    VIEWPORTS.forEach((viewport) => {
        it(`отрисовка карточки профиля ${viewport.width}px`, async () => {
            const component = <Component profile={profileCardStub} Gate={MockGate} />;

            await render(component, { viewport });
            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.hover(`.${styles.container}`);
            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

            await page.click('.Button');
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
