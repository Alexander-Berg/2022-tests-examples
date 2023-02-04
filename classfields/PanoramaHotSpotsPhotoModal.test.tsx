/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/lib/util/getDataTransferFiles');
jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(() => new Promise(() => {})),
    };
});

import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';

import getDataTransferFiles from 'auto-core/lib/util/getDataTransferFiles';
import { MB } from 'auto-core/lib/consts';

import { panoramaHotSpotMock } from 'auto-core/react/dataDomain/panoramaHotSpots/mocks';
import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import getImageUrlsRaw from 'auto-core/react/lib/offer/getImageUrlsRaw';

import PanoramaHotSpotsPhotoModal, { MAX_FILE_SIZE } from './PanoramaHotSpotsPhotoModal';
import type { Props } from './PanoramaHotSpotsPhotoModal';

const getDataTransferFilesMock = getDataTransferFiles as jest.MockedFunction<typeof getDataTransferFiles>;
getDataTransferFilesMock.mockReturnValue([]);

const validFileMock = {
    name: 'spyaschii_kotik.jpeg',
    type: 'image/jpeg',
    size: 7 * MB,
} as File;

let props: Props;
const alertMock = jest.fn();
const confirmMock = jest.fn();
const inputClickMock = jest.fn();
let originalCreateObjectUrl: any;

beforeEach(() => {
    props = {
        isOpened: true,
        onUploadError: jest.fn(),
        onRequestHide: jest.fn(),
        onSave: jest.fn(),
        onSpotRemove: jest.fn(),
        spot: panoramaHotSpotMock.value(),
        zIndexLevel: 1,
    };

    jest.spyOn(global, 'alert').mockImplementation(alertMock);
    jest.spyOn(global, 'confirm').mockImplementation(confirmMock);

    originalCreateObjectUrl = global.URL.createObjectURL;
    global.URL.createObjectURL = jest.fn(() => 'image-preview');
});

afterEach(() => {
    jest.restoreAllMocks();
    global.URL.createObjectURL = originalCreateObjectUrl;
});

describe('при маунте если у точки нет фото', () => {
    it('если в оффере нет фото, кликнет на инпут', () => {
        shallowRenderComponent({ props });

        expect(inputClickMock).toHaveBeenCalledTimes(1);
    });

    it('если в оффере есть фото, откроет шторку', () => {
        props.relatedImages = getImageUrlsRaw(cardMock);
        const page = shallowRenderComponent({ props });

        expect(inputClickMock).toHaveBeenCalledTimes(0);

        const curtain = page.find('PanoramaHotSpotsPhotoCurtain');

        expect(curtain.prop('isOpened')).toBe(true);
    });
});

describe('при клике мимо', () => {
    it('не спросит подтверждение если ничего не менялось', () => {
        const page = shallowRenderComponent({ props });
        page.find('Modal').simulate('requestHide');

        expect(props.onRequestHide).toHaveBeenCalledTimes(1);
    });

    it('спросит подтверждение если изменился текст', () => {
        const page = shallowRenderComponent({ props });
        const textArea = page.find('.PanoramaHotSpotsPhotoModal__textArea');

        textArea.simulate('change', 'i am new text');
        page.find('Modal').simulate('requestHide');

        expect(props.onRequestHide).toHaveBeenCalledTimes(0);
        expect(confirmMock).toHaveBeenCalledTimes(1);
    });

    it('спросит подтверждение если загружается картинка', () => {
        const page = simulateFilesUpload([ validFileMock ]);
        page.find('Modal').simulate('requestHide');

        expect(props.onRequestHide).toHaveBeenCalledTimes(0);
        expect(confirmMock).toHaveBeenCalledTimes(1);
    });
});

describe('при выборе файла покажет алерт если', () => {
    it('файлов больше чем один', () => {
        simulateFilesUpload([ { name: 'foo' } as File, { name: 'bar' } as File ]);

        expect(alertMock).toHaveBeenCalledTimes(1);
        expect(alertMock).toHaveBeenCalledWith('Вы пытаетесь загрузить больше, чем один файл. Пожалуйста, выберете только один');
    });

    it('тип файла не изображение', () => {
        simulateFilesUpload([ { name: 'foo', type: 'video/mp4' } as File ]);

        expect(alertMock).toHaveBeenCalledTimes(1);
        expect(alertMock).toHaveBeenCalledWith('Вы пытаетесь загрузить файл некорректного типа. Допустимы только файлы изображений');
    });

    it('размер файла больше допустимого', () => {
        simulateFilesUpload([ { name: 'foo', type: 'image/jpeg', size: MAX_FILE_SIZE + MB } as File ]);

        expect(alertMock).toHaveBeenCalledTimes(1);
        expect(alertMock).toHaveBeenCalledWith('Вы пытаетесь загрузить файл размером 11 Мб. Допустимый размер файла - до 10 Мб.');
    });
});

describe('при загрузке валидного файла', () => {
    let page: ShallowWrapper;

    beforeEach(() => {
        // uploadImageMock.mockImplementation(() => new Promise(() => {}));
        page = simulateFilesUpload([ validFileMock ]);
    });

    it('покажет лоадер', () => {
        const loader = page.find('Loader');

        expect(loader.isEmptyRender()).toBe(false);
    });

    it('покажет первью картинки', () => {
        const preview = page.find('.PanoramaHotSpotsPhotoModal__photoPreview');

        expect(preview.prop('src')).toBe('image-preview');
    });
});

describe('в шторке', () => {

    beforeEach(() => {
        props.relatedImages = getImageUrlsRaw(cardMock);
    });

    it('правильно прокинет индекс активной фотки', () => {
        const image = props.relatedImages ? props.relatedImages[0].sizes['1200x900'] : '';
        props.spot = panoramaHotSpotMock.withImage(`https:${ image }`).value();
        const page = shallowRenderComponent({ props });
        const curtain = page.find('PanoramaHotSpotsPhotoCurtain');

        expect(curtain.prop('selectedImageIndex')).toBe(0);
    });

    it('при клике на тайл с существующим фото, покажет его в модале', () => {
        const page = shallowRenderComponent({ props });
        const curtain = page.find('PanoramaHotSpotsPhotoCurtain');

        curtain.simulate('itemClick', 'offer-image');

        const photoPreview = page.find('.PanoramaHotSpotsPhotoModal__photoPreview');
        expect(photoPreview.prop('src')).toBe('offer-image');

        const updatedCurtain = page.find('PanoramaHotSpotsPhotoCurtain');
        expect(updatedCurtain.prop('isOpened')).toBe(false);
    });

    it('при клике на тайл "добавить фото", кликнет на инпут', () => {
        const page = shallowRenderComponent({ props });
        const curtain = page.find('PanoramaHotSpotsPhotoCurtain');

        curtain.simulate('itemClick', undefined);

        expect(inputClickMock).toHaveBeenCalledTimes(1);

        const updatedCurtain = page.find('PanoramaHotSpotsPhotoCurtain');
        expect(updatedCurtain.prop('isOpened')).toBe(false);
    });
});

describe('при клике на "заменить фото"', () => {
    it('если в оффере нет фото, кликнет на инпут и опчистит превью', () => {
        const page = simulateFilesUpload([ validFileMock ]);
        const replaceLink = page.find('.PanoramaHotSpotsPhotoModal__sideBar').find('Link').at(0);
        replaceLink.simulate('click');

        const photoPreview = page.find('.PanoramaHotSpotsPhotoModal__photoPreview');
        expect(photoPreview.prop('src')).toBe('');

        expect(inputClickMock).toHaveBeenCalledTimes(2);
    });

    it('если в оффере есть фото, откроет шторку', () => {
        props.relatedImages = getImageUrlsRaw(cardMock);
        const page = shallowRenderComponent({ props });
        const replaceLink = page.find('.PanoramaHotSpotsPhotoModal__sideBar').find('Link').at(0);
        replaceLink.simulate('click');

        const curtain = page.find('PanoramaHotSpotsPhotoCurtain');
        expect(curtain.prop('isOpened')).toBe(true);
    });
});

it('покажет LazyImage с блюр-превью для точки с добавленной фоткой', () => {
    props.spot = panoramaHotSpotMock.withImage('my-cat').value();
    const page = shallowRenderComponent({ props });
    const image = page.find('.PanoramaHotSpotsPhotoModal__photoPlaceholder').find('LazyImage');

    expect(image.prop('preview')).toBe('image-preview');
    expect(image.prop('src')).toBe('my-cat');
});

function simulateFilesUpload(files: Array<File>) {
    getDataTransferFilesMock.mockReturnValue(files);

    const page = shallowRenderComponent({ props });
    const input = page.find('input');
    input.simulate('change');

    return page;
}

function shallowRenderComponent({ props }: { props: Props }) {

    const page = shallow(
        <PanoramaHotSpotsPhotoModal { ...props }/>,
        { disableLifecycleMethods: true },
    );

    const componentInstance = page.instance() as PanoramaHotSpotsPhotoModal;

    componentInstance.inputRef = {
        current: {
            click: inputClickMock,
        },
    } as unknown as React.RefObject<HTMLInputElement>;

    if (typeof componentInstance.componentDidMount === 'function') {
        componentInstance.componentDidMount();
    }

    return page;
}
