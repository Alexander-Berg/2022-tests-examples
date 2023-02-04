import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { PdfsendPage } from './page';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('yb-frontend-utils');
jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin - pdfsend', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    async function fillPage(page: PdfsendPage) {
        page.fillSelect('Договор/доп. соглашение', 'contract');
        page.fillDateField('Дата создания (с)', '2020-06-02T00:00:00');
        page.fillDateField('Дата создания (по)', '2020-06-03T00:00:00');
        page.fillSelect('Сервисы', '102');
        page.fillTextField('Номер договора', '1234');
        page.fillSelect('Вид договора', 'GENERAL');
        page.fillSelect('Фирма', '4');
        page.fillSelect('Тип оплаты', 'POSTPAY');
        page.fillSelect('Подписан по факсу', 'YES');
        page.fillSelect('Email в очереди', 'NO');
        page.fillSelect('Подписан в оригинале', 'YES');
        page.fillSelect('Отправлен оригинал', 'NO');
        page.fillSelect('Нетиповые условия', 'YES');
        page.fillSelect('Бронь подписи', 'NO');
    }

    function fillSendForm(page: PdfsendPage) {
        page.fillTextField('Кому', 'asd@asd.asd');
        page.fillCheckboxField('Клиенту', true);
        page.fillCheckboxField('Менеджеру', true);
        page.fillSelect('От кого', 'comission@yandex-team.ru');
        page.fillTextareaField('Тема письма', 'email subject');
        page.fillTextareaField('Тело письма', 'email body');
    }

    test('заполнение фильтра и получение списка контрактов для отправки', async () => {
        const data = await import('./data');
        const { services, firms, contracts } = data;

        const page = new PdfsendPage({
            mocks: {
                requestGet: [firms, services, contracts]
            }
        });

        await page.initializePage();
        await fillPage(page);
        await page.submitFilter();

        expect(page.getListItems().length).toBe(contracts.response.items.length);

        expect(page.request.get).toHaveBeenCalledTimes(3);
        expect(page.request.get).nthCalledWith(1, firms.request);
        expect(page.request.get).nthCalledWith(2, services.request);
        expect(page.request.get).nthCalledWith(3, contracts.request);
    });

    test('успешная отправка контракта', async () => {
        const data = await import('./data');
        const { services, firms, contracts, firmsEmails, sendEmailsSuccess } = data;

        const page = new PdfsendPage({
            mocks: {
                requestGet: [firms, services, contracts, firmsEmails],
                requestPost: [sendEmailsSuccess]
            }
        });
        await page.initializePage();
        await page.submitFilter();

        page.checkItem();
        await page.openSendModal();
        fillSendForm(page);
        await page.submitSendFormWithSuccess();

        expect(page.getSendFormMessage()).toBe('Отправка писем успешно проставлена в очередь');

        expect(page.request.get).toHaveBeenCalledTimes(4);
        expect(page.request.get).nthCalledWith(4, firmsEmails.request);

        expect(page.request.post).toHaveBeenCalledTimes(1);
        expect(page.request.post).nthCalledWith(1, sendEmailsSuccess.request);
    });

    test('неуспешная отправка контракта', async () => {
        const data = await import('./data');
        const { services, firms, contracts, firmsEmails, sendEmailsFailure } = data;

        const page = new PdfsendPage({
            mocks: {
                requestGet: [firms, services, contracts, firmsEmails],
                requestPost: [sendEmailsFailure]
            }
        });
        await page.initializePage();
        await page.submitFilter();

        page.checkItem();
        await page.openSendModal();
        fillSendForm(page);
        await page.submitSendFormWithFailure();

        expect(page.getSendFormMessage()).toBe('Ошибка при простановке отправки писем в очередь');

        expect(page.request.get).toHaveBeenCalledTimes(4);
        expect(page.request.get).nthCalledWith(4, firmsEmails.request);

        expect(page.request.post).toHaveBeenCalledTimes(1);
        expect(page.request.post).nthCalledWith(1, sendEmailsFailure.request);
    });
});
