import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { initializeMobileRegistry, initializeDesktopRegistry } from 'common/__tests__/registry';
import { PersonsDesktopSubpage } from './subpage.desktop';
import { PersonsMobileSubpage } from './subpage.mobile';

import { mocks } from './subpage.data';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('подстраница списка плательщиков', () => {
    describePersonsSubpageTests(
        'десктопная версия',
        PersonsDesktopSubpage,
        initializeDesktopRegistry
    );
    describePersonsSubpageTests('мобильная версия', PersonsMobileSubpage, initializeMobileRegistry);
});

function describePersonsSubpageTests(
    title: string,
    PersonsSubpage: typeof PersonsDesktopSubpage | typeof PersonsMobileSubpage,
    initializeRegistry: jest.ProvidesCallback
) {
    return describe(title, () => {
        afterEach(() => {
            jest.clearAllMocks();
        });

        beforeAll(initializeRegistry);

        it('запрашивает клиента при загрузке', async () => {
            expect.assertions(1);

            const subpage = new PersonsSubpage({
                mocks: {
                    requestGet: [mocks.clientWithoutPersons]
                }
            });
            await subpage.initialize();

            expect(subpage.request.get).nthCalledWith(1, mocks.clientWithoutPersons.request);
        });

        it('у клиента отсутствуют плательщики', async () => {
            expect.assertions(1);

            const subpage = new PersonsSubpage({
                mocks: {
                    requestGet: [mocks.clientWithoutPersons]
                }
            });
            await subpage.initialize();

            expect(subpage.hasEmptyPersons()).toBeTruthy();
        });

        it('загрузка списка плательщиков', async () => {
            expect.assertions(2);

            const subpage = new PersonsSubpage({
                mocks: {
                    requestGet: [mocks.clientWithPersons, mocks.persons]
                }
            });
            await subpage.initialize();
            await subpage.waitForPersons();

            expect(subpage.request.get).nthCalledWith(2, mocks.persons.request);
            expect(subpage.getPersons().length).toBe(1);
        });

        it('переключение фильтра плательщиков', async () => {
            expect.assertions(2);

            const subpage = new PersonsSubpage({
                mocks: {
                    requestGet: [mocks.clientWithPersons, mocks.persons, mocks.archivedPersons]
                }
            });
            await subpage.initialize();
            await subpage.waitForPersons();

            subpage.chooseArchivedPersons();
            await subpage.waitForPersons();

            expect(subpage.request.get).nthCalledWith(3, mocks.archivedPersons.request);
            expect(subpage.getPersons().length).toBe(1);
        });

        it('пустой список плательщиков (после фильтрации)', async () => {
            expect.assertions(2);

            const subpage = new PersonsSubpage({
                mocks: {
                    requestGet: [mocks.clientWithPersons, mocks.emptyPersons]
                }
            });
            await subpage.initialize();
            await subpage.waitForPersons();

            expect(subpage.request.get).nthCalledWith(2, mocks.emptyPersons.request);
            expect(subpage.hasEmptyFilteredPersons()).toBeTruthy();
        });

        it('восстановление фильтра из строки поиска', async () => {
            expect.assertions(2);

            const subpage = new PersonsSubpage({
                windowLocationHash: '#/?archive=archived&partnership=regular&type=all&sort=',
                mocks: {
                    requestGet: [mocks.clientWithPersons, mocks.archivedPersons]
                }
            });
            await subpage.initialize();
            await subpage.waitForPersons();

            expect(subpage.request.get).nthCalledWith(2, mocks.archivedPersons.request);
            expect(subpage.getPersons().length).toBe(1);
        });

        it('пагинация', async () => {
            expect.assertions(2);

            const subpage = new PersonsSubpage({
                mockIntersectionObserver: true,
                mocks: {
                    requestGet: [mocks.clientWithPersons, mocks.persons, mocks.nextPersons]
                }
            });
            await subpage.initialize();
            await subpage.waitForPersons();

            subpage.emulateScrolling();
            await subpage.waitForPersons();

            expect(subpage.request.get).nthCalledWith(3, mocks.nextPersons.request);
            expect(subpage.getPersons().length).toBe(2);
        });
    });
}
