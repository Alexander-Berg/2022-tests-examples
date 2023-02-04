import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

jest.mock('auto-core/react/lib/uploadImage');
import uploadImage from 'auto-core/react/lib/uploadImage';

import type { TOfferImage } from 'auto-core/types/proto/auto/api/api_offer_model';

import MdsPhotos from './MdsPhotos';

const mockPhotos = [
    { name: 'photo1' },
    { name: 'photo2' },
] as Array<TOfferImage>;

const uploadImageMock = uploadImage as jest.MockedFunction<typeof uploadImage>;

const handleChange = jest.fn();

let originalCreateObjectUrl: any;

beforeEach(() => {
    originalCreateObjectUrl = global.URL.createObjectURL;
    global.URL.createObjectURL = jest.fn(() => 'image-preview');
});

afterEach(() => {
    jest.restoreAllMocks();
    global.URL.createObjectURL = originalCreateObjectUrl;
});

it('если есть загруженные фото и мы загрузили невалидную, отдаем только валидные фото в проп onChange', async() => {
    const uploadUrl = 'upload-url';
    const file = new File([ 'hello' ], 'hello.png', { type: 'image/jpg' });

    const store = mockStore({});
    const Context = createContextProvider(contextMock);

    const uploadImagePromise = Promise.reject({
        response_status: 'ERROR',
        photo: {
            mds_photo_info: {
                group_id: 1397950,
                name: 'c0c5ed562113f99d609066ee262deb9b',
                namespace: 'autoru-carfax',
            },
            sizes: {
                '1200x900': 'big-image',
                '320x240': 'small-image',
            },
        },
    });

    render(
        <Provider store={ store }>
            <Context>
                <MdsPhotos
                    uploadUrl={ uploadUrl }
                    limit={ 5 }
                    initialPhotos={ mockPhotos }
                    onChange={ handleChange }
                    category="cars"
                    offerId="1"
                />)
            </Context>
        </Provider>,
    );

    const addInput = screen.getByRole('file-input');

    uploadImageMock.mockReturnValueOnce(uploadImagePromise);

    await userEvent.upload(addInput, file);

    expect(handleChange).toHaveBeenCalledTimes(1);
    expect(handleChange.mock.calls[0][0]).toHaveLength(2);
    expect(uploadImage).toHaveBeenCalledTimes(1);
});

it('если нет загруженных фото и мы загрузили невалидную в проп onChange отдается пустой массив', async() => {
    const uploadUrl = 'upload-url';
    const file = new File([ 'hello' ], 'hello.png', { type: 'image/jpg' });

    const store = mockStore({});
    const Context = createContextProvider(contextMock);

    const uploadImagePromise = Promise.reject({
        response_status: 'ERROR',
        photo: {
            mds_photo_info: {
                group_id: 1397950,
                name: 'c0c5ed562113f99d609066ee262deb9b',
                namespace: 'autoru-carfax',
            },
            sizes: {
                '1200x900': 'big-image',
                '320x240': 'small-image',
            },
        },
    });

    render(
        <Provider store={ store }>
            <Context>
                <MdsPhotos
                    uploadUrl={ uploadUrl }
                    limit={ 5 }
                    initialPhotos={ [] }
                    onChange={ handleChange }
                    category="cars"
                    offerId="1"
                />)
            </Context>
        </Provider>,
    );

    const addInput = screen.getByRole('file-input');

    uploadImageMock.mockReturnValueOnce(uploadImagePromise);

    await userEvent.upload(addInput, file);

    expect(handleChange).toHaveBeenCalledTimes(1);
    expect(handleChange.mock.calls[0][0]).toHaveLength(0);
    expect(uploadImage).toHaveBeenCalledTimes(1);
});
