/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('auto-core/react/lib/uploadImage');

import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import uploadImage from 'auto-core/react/lib/uploadImage';
import type { ImageFile } from 'auto-core/react/components/common/PhotosAdd/PhotosAdd';

import garageCardMock from 'auto-core/models/garageCard/mocks/mockChain';

import type { AbstractProps, AbstractState } from './GarageImageGalleryBase';
import GarageImageGalleryBase from './GarageImageGalleryBase';

const uploadImageMock = uploadImage as jest.MockedFunction<typeof uploadImage>;

class ComponentMock extends GarageImageGalleryBase<AbstractProps, AbstractState> {
    render() {
        return <div>gallery</div>;
    }
}

const photos = [
    { name: 'spyaschii_kotik.jpeg', preview: 'cat-image', type: 'image/jpeg' },
] as Array<ImageFile>;

let props: AbstractProps;
let originalCreateObjectUrl: any;
const confirmMock = jest.fn();

beforeEach(() => {
    props = {
        garageCard: garageCardMock.value(),
        mode: 'VIEW',
        uploadUrl: 'upload-url',
        changeFormWithImages: jest.fn(),
        updateCardWithPayload: jest.fn(),
    };

    jest.spyOn(global, 'confirm').mockImplementation(confirmMock);

    originalCreateObjectUrl = global.URL.createObjectURL;
    global.URL.createObjectURL = jest.fn(() => 'image-preview');
});

afterEach(() => {
    jest.restoreAllMocks();
    global.URL.createObjectURL = originalCreateObjectUrl;
});

describe('при маунте', () => {
    it('правильно формирует список слайдов, если есть панорама', () => {
        props.garageCard = garageCardMock.withPanoramaExterior().value();
        const { instance } = shallowRenderComponent({ props });
        const items = instance.state.items;

        expect(items).toHaveLength(3);
        expect(items[0].type).toBe('PANORAMA_EXTERIOR');
    });

    it('правильно формирует список слайдов, если есть только фотки', () => {
        props.garageCard = garageCardMock.value();
        const { instance } = shallowRenderComponent({ props });
        const items = instance.state.items;

        expect(items).toHaveLength(2);
        expect(items[0].type).toBe('IMAGE');
        expect(items[1].type).toBe('IMAGE');
    });

    it('правильно формирует список слайдов, если нет ничего', () => {
        props.garageCard = garageCardMock.withImages([]).value();
        const { instance } = shallowRenderComponent({ props });
        const items = instance.state.items;

        expect(items).toHaveLength(1);
        expect(items[0].type).toBe('IMAGE');
        expect(items[0].id).toBe('catalog-photo');
    });

    it('правильно формирует список слайдов, при редактировании если нет ничего', () => {
        props.garageCard = garageCardMock.withImages([]).value();
        props.mode = 'EDIT';
        const { instance } = shallowRenderComponent({ props });
        const items = instance.state.items;

        expect(items).toHaveLength(0);
    });
});

describe('правильно считает лимит фоток', () => {
    it('если есть фотка из каталога', () => {
        props.garageCard = garageCardMock.withImages([]).value();
        const { instance } = shallowRenderComponent({ props });

        expect(instance.getMaxPhotosNum()).toBe(instance.MAX_PHOTOS_IN_GALLERY + 1);
    });

    it('если есть загруженные фотки', () => {
        props.garageCard = garageCardMock.value();
        const { instance } = shallowRenderComponent({ props });

        expect(instance.getMaxPhotosNum()).toBe(instance.MAX_PHOTOS_IN_GALLERY);
    });
});

describe('при загрузке фоток', () => {
    const uploadImagePromise = Promise.resolve({
        response_status: 'SUCCESS',
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
    const updateCardWithPayloadPromise = Promise.resolve({});

    beforeEach(() => {
        uploadImageMock.mockReturnValueOnce(uploadImagePromise);
        (props.updateCardWithPayload as jest.MockedFunction<typeof props.updateCardWithPayload>).mockReturnValueOnce(updateCardWithPayloadPromise);
    });

    it('добавит превью в стейт', () => {
        const { instance } = shallowRenderComponent({ props });
        instance.handleDropPhotos(photos);

        expect(instance.state.items).toHaveLength(3);
        expect(instance.state.items[2]).toMatchObject({
            type: 'IMAGE',
            uploadStatus: 'LOADING',
            original: 'image-preview',
            thumbnail: 'cat-image',
            file: photos[0],
        });
    });

    it('вызовет утилиту загрузки', () => {
        const { instance } = shallowRenderComponent({ props });
        instance.handleDropPhotos(photos);

        expect(uploadImageMock).toHaveBeenCalledTimes(1);
        expect(uploadImageMock).toHaveBeenCalledWith('upload-url', photos[0]);
    });

    it('после загрузки проапдейтит карточку', () => {
        const { instance } = shallowRenderComponent({ props });
        instance.handleDropPhotos(photos);

        return uploadImagePromise
            .then(() => {
                expect(props.updateCardWithPayload).toHaveBeenCalledTimes(1);
                const payload = (props.updateCardWithPayload as jest.MockedFunction<typeof props.updateCardWithPayload>)
                    .mock.calls[0][0];
                expect(payload.exterior_panorama).toBeUndefined();
                expect(payload.images?.[2]).toMatchObject({
                    mds_photo_info: {
                        group_id: 1397950,
                        name: 'c0c5ed562113f99d609066ee262deb9b',
                        namespace: 'autoru-carfax',
                    },
                    sizes: {
                        '1200x900': 'big-image',
                        '320x240': 'small-image',
                    },
                });
            });
    });

    it('после загрузки проапдейтит стейт', () => {
        const { instance } = shallowRenderComponent({ props });
        instance.handleDropPhotos(photos);

        return uploadImagePromise
            .then(() => {
                expect(instance.state.items).toHaveLength(3);
                expect(instance.state.items[2]).toMatchObject({
                    type: 'IMAGE',
                    uploadStatus: 'SUCCESS',
                    original: 'big-image',
                    thumbnail: 'small-image',
                });
            });
    });
});

it('при неудачной загрузке добавит статус REJECTED фотке', () => {
    const uploadImagePromise = Promise.resolve({
        response_status: 'ERROR',
    });
    uploadImageMock.mockReturnValueOnce(uploadImagePromise);

    const { instance } = shallowRenderComponent({ props });
    instance.handleDropPhotos(photos);

    return uploadImagePromise
        .then(() => {
            expect(instance.state.items).toHaveLength(3);
            expect(instance.state.items[2]).toMatchObject({
                uploadStatus: 'REJECTED',
            });
        });
});

describe('при удалении слайда', () => {
    beforeEach(() => {
        const updateCardWithPayloadPromise = Promise.resolve({});
        (props.updateCardWithPayload as jest.MockedFunction<typeof props.updateCardWithPayload>).mockReturnValue(updateCardWithPayloadPromise);
    });

    it('удалит его из стейта', () => {
        const { instance } = shallowRenderComponent({ props });
        instance.handleRemoveItem(1);
        expect(instance.state.items).toHaveLength(1);
    });

    it('обновит карточку', () => {
        const { instance } = shallowRenderComponent({ props });
        instance.handleRemoveItem(1);

        expect(props.updateCardWithPayload).toHaveBeenCalledTimes(1);
        const payload = (props.updateCardWithPayload as jest.MockedFunction<typeof props.updateCardWithPayload>)
            .mock.calls[0][0];
        expect(payload.exterior_panorama).toBeUndefined();
        expect(payload.images?.[0]).toMatchObject({
            mds_photo_info: {
                group_id: 1397950,
                name: 'c0c5ed562113f99d609066ee262deb9b',
                namespace: 'autoru-carfax',
            },
            sizes: {
                '1200x900': 'picture-of-cat',
                '320x240': 'picture-of-cat',
            },
        });
    });

    it('если айтем был один до добавит на его место каталожный', () => {
        const { instance } = shallowRenderComponent({ props });
        instance.handleRemoveItem(0);
        instance.handleRemoveItem(0);

        expect(instance.state.items).toHaveLength(1);
        expect(instance.state.items[0].id).toBe('catalog-photo');
    });
});

it('при ретрае загрузки уже загруженного изображения просто еще раз попробует сохранить карточку', () => {
    const uploadImagePromise = Promise.resolve({
        response_status: 'SUCCESS',
        photo: {
            mds_photo_info: {
                group_id: 1397950,
                name: 'new-item',
                namespace: 'autoru-carfax',
            },
            sizes: {
                '1200x900': 'big-image',
                '320x240': 'small-image',
            },
        },
    });
    const updateCardWithPayloadPromise1 = Promise.resolve({
        error: 'foo',
    });
    const updateCardWithPayloadPromise2 = Promise.resolve({});
    uploadImageMock.mockReturnValueOnce(uploadImagePromise);
    (props.updateCardWithPayload as jest.MockedFunction<typeof props.updateCardWithPayload>).mockReturnValueOnce(updateCardWithPayloadPromise1);
    (props.updateCardWithPayload as jest.MockedFunction<typeof props.updateCardWithPayload>).mockReturnValueOnce(updateCardWithPayloadPromise2);

    const { instance } = shallowRenderComponent({ props });
    instance.handleDropPhotos(photos);

    return uploadImagePromise
        // тут нам нужно дождаться когда обновится стейт после загрузки, и только после делать ретрай
        .then(() => {})
        .then(() => {
            instance.handleRetryImageUpload(2);

            return updateCardWithPayloadPromise2
                .then(() => {
                    expect(uploadImageMock).toHaveBeenCalledTimes(1);
                    expect(props.updateCardWithPayload).toHaveBeenCalledTimes(2);
                    const payload = (props.updateCardWithPayload as jest.MockedFunction<typeof props.updateCardWithPayload>)
                        .mock.calls[1][0];
                    expect(payload.exterior_panorama).toBeUndefined();
                    expect(payload.images?.[2]).toMatchObject({
                        mds_photo_info: {
                            group_id: 1397950,
                            name: 'new-item',
                            namespace: 'autoru-carfax',
                        },
                        sizes: {
                            '1200x900': 'big-image',
                            '320x240': 'small-image',
                        },
                    });
                });
        });
});

it('при ретрае незагруженного изображения попробует загрузить его снова', () => {
    const uploadImagePromise1 = Promise.reject();
    const uploadImagePromise2 = Promise.resolve({
        response_status: 'SUCCESS',
        photo: {
            mds_photo_info: {
                group_id: 1397950,
                name: 'new-item',
                namespace: 'autoru-carfax',
            },
            sizes: {
                '1200x900': 'big-image',
                '320x240': 'small-image',
            },
        },
    });
    const updateCardWithPayloadPromise = Promise.resolve({});
    uploadImageMock.mockReturnValueOnce(uploadImagePromise1);
    uploadImageMock.mockReturnValueOnce(uploadImagePromise2);
    (props.updateCardWithPayload as jest.MockedFunction<typeof props.updateCardWithPayload>).mockReturnValueOnce(updateCardWithPayloadPromise);

    const { instance } = shallowRenderComponent({ props });
    instance.handleDropPhotos(photos);

    return uploadImagePromise1
        // тут нам нужно дождаться когда обновится стейт после загрузки, и только после делать ретрай
        .catch(() => {})
        .then(() => {
            instance.handleRetryImageUpload(2);

            return uploadImagePromise2
                .then(() => {
                    expect(uploadImageMock).toHaveBeenCalledTimes(2);
                    expect(props.updateCardWithPayload).toHaveBeenCalledTimes(1);
                });
        });
});

function shallowRenderComponent({ props }: { props: AbstractProps }) {
    const page = shallow(
        <ComponentMock { ...props }/>
        ,
        { context: contextMock },
    );

    const instance = page.instance() as ComponentMock;

    return { page, instance };
}
