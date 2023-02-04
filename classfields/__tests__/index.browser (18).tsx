import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';
import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import {
    IImageUploaderImage,
    ImageUploaderImageId,
    ImageUploaderEntityId,
    ImageUploaderImageErrorType,
    ImageUploaderImageStatus,
    IUploaderUploadedImage,
    IImageUploaderImageFailed,
} from 'types/imageUploader';

import { AppProvider } from 'view/libs/test-helpers';

import { ImageUploaderImagePreview as ImageUploaderImagePreviewBase } from '../index';
import styles from '../styles.module.css';

const previewStub = generateImageUrl({ width: 160, height: 140, size: 10 });

const renderOptions = { viewport: { width: 160, height: 100 } };

const Component: React.FunctionComponent<React.ComponentProps<typeof ImageUploaderImagePreviewBase>> = (props) => {
    return (
        <AppProvider>
            <ImageUploaderImagePreviewBase {...props} />
        </AppProvider>
    );
};

const uploaderImages: IImageUploaderImage[] = [
    {
        entityId: '' as ImageUploaderEntityId,
        imageId: '1' as ImageUploaderImageId,
        previewUrl: previewStub,
        largeUrl: '',
        status: ImageUploaderImageStatus.LOADED,
        uploaderData: {} as IUploaderUploadedImage,
    },
    {
        entityId: '' as ImageUploaderEntityId,
        imageId: '2' as ImageUploaderImageId,
        previewUrl: previewStub,
        largeUrl: '',
        status: ImageUploaderImageStatus.PENDING,
    },
    {
        entityId: '' as ImageUploaderEntityId,
        imageId: '3' as ImageUploaderImageId,
        previewUrl: previewStub,
        largeUrl: '',
        status: ImageUploaderImageStatus.FAILED,
        error: ImageUploaderImageErrorType.NETWORK_ERROR,
    },
    {
        entityId: '' as ImageUploaderEntityId,
        imageId: '4' as ImageUploaderImageId,
        previewUrl: previewStub,
        largeUrl: '',
        status: ImageUploaderImageStatus.SAVED,
        uploaderData: {} as IUploaderUploadedImage,
    },
];

describe('ImageUploaderImagePreview', () => {
    describe('Внешний вид desktop', () => {
        uploaderImages.forEach((image) => {
            it(`состояние ${image.status}`, async () => {
                await render(
                    <Component
                        image={image}
                        isMobile={false}
                        imageUploader={{
                            uploaderImages: [],
                            handleRepeatUploadingImage: noop,
                            handleDeleteUploadedImage: noop,
                            handleOpenFileDialog: noop,
                            handleMoveImage: noop,
                            isImagesChanged: false,
                            isImagesUploading: false,
                            isDragActive: false,
                            images: [],
                            handleOpenFS: noop,
                            handleCloseFS: noop,
                        }}
                    />,
                    renderOptions
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });

        it('Ошибка превышен размер файла', async () => {
            await render(
                <Component
                    image={{
                        ...(uploaderImages[2] as IImageUploaderImageFailed),
                        error: ImageUploaderImageErrorType.MAX_IMAGE_SIZE,
                    }}
                    isMobile={false}
                    imageUploader={{
                        uploaderImages: [],
                        handleRepeatUploadingImage: noop,
                        handleDeleteUploadedImage: noop,
                        handleOpenFileDialog: noop,
                        handleMoveImage: noop,
                        isImagesChanged: false,
                        isImagesUploading: false,
                        isDragActive: false,
                        images: [],
                        handleOpenFS: noop,
                        handleCloseFS: noop,
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('По ховеру появляется кнопка удаления', async () => {
            await render(
                <Component
                    image={uploaderImages[0]}
                    isMobile={false}
                    imageUploader={{
                        uploaderImages: [],
                        handleRepeatUploadingImage: noop,
                        handleDeleteUploadedImage: noop,
                        handleOpenFS: noop,
                        handleCloseFS: noop,
                        handleOpenFileDialog: noop,
                        handleMoveImage: noop,
                        isImagesChanged: false,
                        isImagesUploading: false,
                        isDragActive: false,
                        images: [],
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.hover(`.${styles.imageFSButton}`);

            expect(await takeScreenshot({ fullPage: true, keepCursor: true })).toMatchImageSnapshot();
        });
    });

    describe('Внешний вид touch', () => {
        uploaderImages.forEach((photoData) => {
            it(`состояние ${photoData.status}`, async () => {
                await render(
                    <Component
                        image={photoData}
                        isMobile={true}
                        imageUploader={{
                            uploaderImages: [],
                            handleRepeatUploadingImage: noop,
                            handleDeleteUploadedImage: noop,
                            handleOpenFS: noop,
                            handleCloseFS: noop,
                            handleOpenFileDialog: noop,
                            handleMoveImage: noop,
                            isImagesChanged: false,
                            isImagesUploading: false,
                            isDragActive: false,
                            images: [],
                        }}
                    />,
                    renderOptions
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
