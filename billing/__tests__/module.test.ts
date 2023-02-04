import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { initializeDesktopRegistry } from 'common/__tests__/registry';

import { ChangePersonModule } from './module';
import { mocks } from './module.data';

jest.mock('common/utils/old-fetch');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('модуль формы плательщиков', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('загрузка данных для формы плательщика', async () => {
        expect.assertions(3);

        const module = new ChangePersonModule({
            clientId: '123',
            personType: 'ph',
            mocks: {
                fetchGet: [mocks.personForms, mocks.personDetails],
                requestGet: [mocks.countries]
            }
        });
        await module.initializeModule();

        expect(module.fetchGet).nthCalledWith(1, ...mocks.personForms.request);
        expect(module.fetchGet).nthCalledWith(2, ...mocks.personDetails.request);
        expect(module.request.get).nthCalledWith(1, mocks.countries.request);
    });

    it('загрузка формы нового плательщика', async () => {
        expect.assertions(1);

        const module = new ChangePersonModule({
            clientId: '123',
            personType: 'ph',
            mocks: {
                fetchGet: [mocks.personForms, mocks.personDetails],
                requestGet: [mocks.countries]
            }
        });
        await module.initializeModule();

        expect(module.isFormLoaded()).toBeTruthy();
    });

    it('загрузка формы редактирования плательщика', async () => {
        expect.assertions(2);

        const module = new ChangePersonModule({
            clientId: '123',
            personId: '234',
            mocks: {
                fetchGet: [mocks.personForms, mocks.personDetails],
                requestGet: [mocks.person, mocks.countries]
            }
        });
        await module.initializeModule();

        expect(module.isFormLoaded()).toBeTruthy();
        expect(module.request.get).nthCalledWith(1, mocks.person.request);
    });

    it('сохранение формы плательщика', async () => {
        expect.assertions(2);

        const onSave = jest.fn();
        const module = new ChangePersonModule({
            clientId: '123',
            personId: '234',
            onSave,
            mocks: {
                fetchGet: [mocks.personForms, mocks.personDetails],
                requestGet: [mocks.person, mocks.countries],
                requestPost: [mocks.setPerson]
            }
        });
        await module.initializeModule();

        await module.saveForm();

        expect(onSave).toHaveBeenCalledWith({ person: mocks.setPerson.response, isNew: false });
        expect(module.request.post).nthCalledWith(1, mocks.setPerson.request);
    });

    it('отмена формы плательщика', async () => {
        expect.assertions(1);

        const onCancel = jest.fn();
        const module = new ChangePersonModule({
            clientId: '123',
            personType: 'ph',
            onCancel,
            mocks: {
                fetchGet: [mocks.personForms, mocks.personDetails],
                requestGet: [mocks.countries]
            }
        });
        await module.initializeModule();

        await module.cancelForm();

        expect(onCancel).toHaveBeenCalled();
    });
});
