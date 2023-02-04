import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { initializeDesktopRegistry } from 'common/__tests__/registry';
import { registry as commonRegistry } from 'common/components/registry';

import { ChangePersonSubpage } from './subpage';
import { mocks } from './subpage.data';
import { Messages } from '../../../components/Messages';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('подстраница формы плательщика', () => {
    beforeAll(() => {
        initializeDesktopRegistry();
        commonRegistry.set('Messages', Messages);
    });

    afterEach(jest.resetAllMocks);

    it('загрузка данных для формы плательщика', async () => {
        expect.assertions(2);

        const subpage = new ChangePersonSubpage({
            mocks: {
                requestGet: [mocks.client, mocks.creatableCategories]
            }
        });
        await subpage.initialize();

        expect(subpage.request.get).nthCalledWith(1, mocks.client.request);
        expect(subpage.request.get).nthCalledWith(2, mocks.creatableCategories.request);
    });

    it('открытие формы нового плательщика', async () => {
        expect.assertions(1);

        const subpage = new ChangePersonSubpage({
            mocks: {
                requestGet: [mocks.client, mocks.creatableCategories]
            }
        });
        await subpage.initialize();

        expect(subpage.isInitialized()).toBeTruthy();
    });

    it('загрузка формы плательщика при выборе категории', async () => {
        expect.assertions(1);

        const subpage = new ChangePersonSubpage({
            mocks: {
                fetchGet: [mocks.personForms, mocks.personDetails],
                requestGet: [mocks.client, mocks.creatableCategories, mocks.countries]
            }
        });
        await subpage.initialize();

        await subpage.choosePersonCategory('ph_0');
        await subpage.continueWithPersonCategory();
        await subpage.waitForChangePersonForm();

        expect(subpage.isChangePersonFormLoaded()).toBeTruthy();
    });

    it('предупреждение при смене категории после выбора', async () => {
        const subpage = new ChangePersonSubpage({
            mocks: {
                fetchGet: [mocks.personForms, mocks.personDetails],
                requestGet: [mocks.client, mocks.creatableCategories, mocks.countries]
            }
        });
        await subpage.initialize();

        await subpage.choosePersonCategory('ph_0');
        await subpage.continueWithPersonCategory();
        await subpage.waitForChangePersonForm();
        await subpage.changePersonCategory('ph_1');

        expect(subpage.getMessageBoxText()).toBe('ID_ChangePerson_confirm-person-category-change');
    });

    it('открытие формы редактирования плательщика', async () => {
        expect.assertions(2);

        const subpage = new ChangePersonSubpage({
            personId: '234',
            mocks: {
                fetchGet: [mocks.personForms, mocks.personDetails],
                requestGet: [mocks.client, mocks.personCategory, mocks.person, mocks.countries]
            }
        });
        await subpage.initialize();
        await subpage.waitForChangePersonForm();

        expect(subpage.isChangePersonFormLoaded()).toBeTruthy();
        expect(subpage.isCategorySelectDisabled()).toBeTruthy();
    });

    it('сохранение плательщика', async () => {
        expect.assertions(1);

        const subpage = new ChangePersonSubpage({
            personId: '234',
            mocks: {
                fetchGet: [mocks.personForms, mocks.personDetails],
                requestGet: [mocks.client, mocks.personCategory, mocks.person, mocks.countries],
                requestPost: [mocks.setPerson]
            }
        });
        await subpage.initialize();
        await subpage.waitForChangePersonForm();
        await subpage.saveChangePersonForm();

        expect(subpage.getMessageText()).toBe('ID_ChangePerson_updated_successfully');
    });

    it('попытка редактирования плательщика', async () => {
        expect.assertions(1);

        const subpage = new ChangePersonSubpage({
            personId: '234',
            mocks: {
                fetchGet: [mocks.personForms, mocks.personDetails],
                requestGet: [
                    mocks.client,
                    mocks.personCategory,
                    mocks.partnerPerson,
                    mocks.countries
                ]
            }
        });
        await subpage.waitForErrorMessage();

        expect(subpage.getMessageText()).toBe('ID_ChangePerson_forbidden-to-edit-partner-person');
    });
});
