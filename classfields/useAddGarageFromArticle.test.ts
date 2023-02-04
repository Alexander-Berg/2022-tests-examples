import { renderHook, act } from '@testing-library/react-hooks';

import { ResponseStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/response_model';

import gateApi from 'auto-core/react/lib/gateApi';

import useAddGarageFromArticle from './useAddGarageFromArticle';

const getReourceMock = gateApi.getResourcePublicApi as jest.MockedFunction<typeof gateApi.getResource>;

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResourcePublicApi: jest.fn(),
}));

jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

const ARTICLE_ID = 'id_of_the_article';
const CARD = { data: 123 };

describe('useAddGarageFromArticle', () => {
    beforeEach(() => {
        getReourceMock.mockReset();
    });
    it('выполняет запрос за данными машины для создания карточки гаража', async() => {
        getReourceMock.mockImplementation(jest.fn(() => Promise.resolve({ status: ResponseStatus.SUCCESS, card: CARD })));
        const { result, waitForNextUpdate } = renderHook(() => useAddGarageFromArticle({ articleId: ARTICLE_ID }));
        expect(getReourceMock).toHaveBeenCalledWith('garageBuildCardByArticleId', { article_id: ARTICLE_ID });
        expect(getReourceMock).toHaveBeenCalledTimes(1);

        await waitForNextUpdate();

        act(() => {
            expect(result.current.cardDraft).toEqual(CARD);
        });
    });

    it('меняет видимость попапа', async() => {
        getReourceMock.mockImplementation(jest.fn(() => Promise.resolve({})));
        const { result } = renderHook(() => useAddGarageFromArticle({ articleId: ARTICLE_ID }));

        expect(result.current.isOpen).toBe(false);
        act(() => {
            result.current.togglePopup();
        });

        expect(result.current.isOpen).toBe(true);

        act(() => {
            result.current.closePopup();
        });

        expect(result.current.isOpen).toBe(false);
    });

    it('выполняет запрос создания карточки гаража, запрос успешен', async() => {
        const GARAGE_CARD = { card: { id: 456 } };
        getReourceMock.mockImplementationOnce(jest.fn(() => Promise.resolve({ status: ResponseStatus.SUCCESS, card: CARD })));
        const { result, waitForNextUpdate } = renderHook(() => useAddGarageFromArticle({ articleId: ARTICLE_ID }));

        await waitForNextUpdate();

        const createGarageMock = jest.fn(() => Promise.resolve(GARAGE_CARD));

        getReourceMock.mockImplementationOnce(createGarageMock);

        expect(result.current.garage.status).toBe('Idle');

        act(() => {
            result.current.onAddCar();
        });

        await waitForNextUpdate();

        expect(createGarageMock).toHaveBeenLastCalledWith('garageAddCard', { payload: { added_manually: true, card: CARD } });
        expect(result.current.garage.status).toBe('Success');
        expect(result.current.garage.card).toBe(GARAGE_CARD.card);

    });

    it('выполняет запрос создания карточки гаража, в ответе нет карточки', async() => {
        const GARAGE_CARD = { id: 456 };
        getReourceMock.mockImplementationOnce(jest.fn(() => Promise.resolve({ status: ResponseStatus.SUCCESS, card: CARD })));
        const { result, waitForNextUpdate } = renderHook(() => useAddGarageFromArticle({ articleId: ARTICLE_ID }));

        await waitForNextUpdate();

        const createGarageMock = jest.fn(() => Promise.resolve(GARAGE_CARD));

        getReourceMock.mockImplementationOnce(createGarageMock);

        expect(result.current.garage.status).toBe('Idle');

        act(() => {
            result.current.onAddCar();
        });

        await waitForNextUpdate();

        expect(createGarageMock).toHaveBeenLastCalledWith('garageAddCard', { payload: { added_manually: true, card: CARD } });
        expect(result.current.garage.status).toBe('Error');
        expect(result.current.garage.card).toBe(null);

    });

    it('выполняет запрос создания карточки гаража, ошибка', async() => {
        getReourceMock.mockImplementationOnce(jest.fn(() => Promise.resolve({ status: ResponseStatus.SUCCESS, card: CARD })));
        const { result, waitForNextUpdate } = renderHook(() => useAddGarageFromArticle({ articleId: ARTICLE_ID }));

        await waitForNextUpdate();

        const createGarageMock = jest.fn(() => Promise.reject({}));

        getReourceMock.mockImplementationOnce(createGarageMock);

        expect(result.current.garage.status).toBe('Idle');

        act(() => {
            result.current.onAddCar();
        });

        await waitForNextUpdate();

        expect(createGarageMock).toHaveBeenLastCalledWith('garageAddCard', { payload: { added_manually: true, card: CARD } });
        expect(result.current.garage.status).toBe('Error');
        expect(result.current.garage.card).toBe(null);
    });
});
