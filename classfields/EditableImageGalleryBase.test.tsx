/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import type { ImageFile } from 'auto-core/react/components/common/PhotosAdd/PhotosAdd';

import EditableImageGalleryBase from './EditableImageGalleryBase';
import type { ImageGalleryItem } from './EditableImageGalleryBase';

//Mocks
let blob;
let imageFile: ImageFile;
let imageItem: ImageGalleryItem;
beforeEach(() => {
    blob = new Blob(undefined, { type: 'image/png' });

    imageFile = {
        name: 'somefile.png',
        preview: 'somefile.png',
        lastModified: 0,
        ...blob,
    } as ImageFile;

    imageItem = {
        type: 'IMAGE',
        uploadStatus: 'SUCCESS',
    };
});

it('должен вызвать метод загрузки фотографий, если фотографий меньше максимума', () => {
    const handleDropPhotosMock = jest.fn();

    const wrapper = shallow(
        <EditableImageGalleryBase
            modelId="foo"
            items={ [ imageItem ] }
            maxPhotosInGallery={ 10 }
            onDropPhotos={ handleDropPhotosMock }
            onRemoveItem={ jest.fn() }
            onRetryImageUpload={ jest.fn() }
        />,
    );

    const instance = (wrapper.instance() as EditableImageGalleryBase<any, any>);
    instance.handleDropPhotos(null, [ imageFile, imageFile ]);

    expect(handleDropPhotosMock).toHaveBeenCalledTimes(1);
    expect(handleDropPhotosMock).toHaveBeenCalledWith([ imageFile, imageFile ]);
});

it('не должен вызывать метод загрузки фотографий, если фотографий больше максимума', () => {
    const handleDropPhotosMock = jest.fn();

    const wrapper = shallow(
        <EditableImageGalleryBase
            modelId="foo"
            items={ [ imageItem, imageItem, imageItem ] }
            maxPhotosInGallery={ 3 }
            onDropPhotos={ handleDropPhotosMock }
            onRemoveItem={ jest.fn() }
            onRetryImageUpload={ jest.fn() }
        />,
    );

    const instance = (wrapper.instance() as EditableImageGalleryBase<any, any>);
    instance.handleDropPhotos(null, [ imageFile, imageFile ]);

    expect(handleDropPhotosMock).toHaveBeenCalledTimes(0);
});

it('должен вызывать метод загрузки фотографий для числа фоток, укладывающегося в ограничение на максимум', () => {
    const handleDropPhotosMock = jest.fn();

    const wrapper = shallow(
        <EditableImageGalleryBase
            modelId="foo"
            items={ [ imageItem, imageItem, imageItem ] }
            maxPhotosInGallery={ 4 }
            onDropPhotos={ handleDropPhotosMock }
            onRemoveItem={ jest.fn() }
            onRetryImageUpload={ jest.fn() }
        />,
    );

    const instance = (wrapper.instance() as EditableImageGalleryBase<any, any>);
    instance.handleDropPhotos(null, [ imageFile, imageFile ]);

    expect(handleDropPhotosMock).toHaveBeenCalledTimes(1);
    expect(handleDropPhotosMock).toHaveBeenCalledWith([ imageFile ]);
});
