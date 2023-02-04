/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});
jest.mock('auto-core/lib/util/getDataTransferFiles');

import fetchMock from 'jest-fetch-mock';
import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';

import type { InteriorPanorama } from '@vertis/schema-registry/ts-types-snake/auto/panoramas/interior_model';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import getDataTransferFiles from 'auto-core/lib/util/getDataTransferFiles';
import { SECOND, MB } from 'auto-core/lib/consts';

import { VIEW } from 'auto-core/react/dataDomain/notifier/actions/notifier';
import gateApi from 'auto-core/react/lib/gateApi';

import { STATUSES, PANORAMA_INTERIOR_MAX_FILE_SIZE } from '../consts';

import type { Props } from './PanoramaUploadInterior';
import PanoramaUploadInterior from './PanoramaUploadInterior';

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;
const uploadUrlMock = 'http://upl.co/?sign=foobar';

const getDataTransferFilesMock = getDataTransferFiles as jest.MockedFunction<typeof getDataTransferFiles>;
getDataTransferFilesMock.mockReturnValue([]);

let props: Props;
let instance: any;

const validFileMock = {
    name: 'spyaschii_kotik.jpeg',
    type: 'image/jpeg',
    size: 42 * MB,
} as File;

const alertMock = jest.fn();
const confirmMock = jest.fn();

let getUploadUrlPromise: Promise<{upload_url: string}>;
beforeEach(() => {
    fetchMock.mockResponse(JSON.stringify({ id: 'panorama-id' }));
    getUploadUrlPromise = Promise.resolve({ upload_url: uploadUrlMock });
    getResource.mockImplementation(() => getUploadUrlPromise);

    props = {
        setPanoramaData: jest.fn(),
        showMessage: jest.fn(),
        data: null,
        isEdit: false,
        className: 'my-cool-class',
        category: 'cars',
    };

    //@see https://github.com/facebook/jest/issues/11551
    jest.useFakeTimers('legacy');

    jest.spyOn(global, 'alert').mockImplementation(alertMock);
    jest.spyOn(global, 'confirm').mockImplementation(confirmMock);
});

afterEach(() => {
    fetchMock.resetMocks();
    getResource.mockClear();
    jest.restoreAllMocks();
});

describe('при первом рендере покажет соответствующий статус', () => {
    it('если панорама обработана', () => {
        props.data = { status: 'COMPLETED' } as InteriorPanorama;
        const page = shallowRenderComponent({ props });

        expect(page.find('PanoramaUploadInteriorDumb').prop('status')).toBe(STATUSES.PROCESSED);
    });

    it('если панорама в процессе обработки', () => {
        props.data = { status: 'PROCESSING' } as InteriorPanorama;
        const page = shallowRenderComponent({ props });

        expect(page.find('PanoramaUploadInteriorDumb').prop('status')).toBe(STATUSES.IN_PROCESSING);
    });

    it('если обработка панорамы упала', () => {
        props.data = { status: 'FAILED' } as InteriorPanorama;
        const page = shallowRenderComponent({ props });

        expect(page.find('PanoramaUploadInteriorDumb').prop('status')).toBe(STATUSES.PROCESSING_ERROR);
    });

    it('если у панорамы еще нет статуса', () => {
        const page = shallowRenderComponent({ props });

        expect(page.find('PanoramaUploadInteriorDumb').prop('status')).toBe(STATUSES.INIT);
    });
});

describe('поллинг статуса при маунте', () => {
    it('начнется, если панорама находится в статусе PROCESSING', () => {
        props.data = { status: 'PROCESSING', id: 'panorama-id' } as InteriorPanorama;

        const fetchStatusMock1 = Promise.resolve({ status: 'SUCCESS', panorama: { status: 'PROCESSING' } });
        const fetchStatusMock2 = Promise.resolve({ status: 'SUCCESS', panorama: { status: 'COMPLETED' } });
        getResource.mockImplementationOnce(() => fetchStatusMock1);
        getResource.mockImplementationOnce(() => fetchStatusMock2);

        shallowRenderComponent({ props });

        expect(getResource).toHaveBeenCalledTimes(1);
        expect(getResource.mock.calls[0]).toMatchSnapshot();
    });

    it('не начнется, если панорама находится в статусе COMPLETED', () => {
        props.data = { status: 'COMPLETED' } as InteriorPanorama;

        shallowRenderComponent({ props });

        expect(getResource).toHaveBeenCalledTimes(0);
    });
});

describe('при клике на область загрузки', () => {
    it('если текущий статус init кликнет на инпуте', () => {
        const page = shallowRenderComponent({ props });
        const component = page.find('PanoramaUploadInteriorDumb');
        component.simulate('uploadAreaClick');

        expect(instance.fileInput.current.click).toHaveBeenCalledTimes(1);
    });

    it('если текущий статус не init ничего не будет делать', () => {
        props.data = { status: 'FAILED' } as InteriorPanorama;
        const page = shallowRenderComponent({ props });
        const component = page.find('PanoramaUploadInteriorDumb');
        component.simulate('uploadAreaClick');

        expect(instance.fileInput.current.click).toHaveBeenCalledTimes(0);
    });
});

describe('при выборе файла покажет алерт если', () => {
    it('файлов больше чем один', () => {
        simulateFilesUpload([ { name: 'foo' } as File, { name: 'bar' } as File ]);

        expect(alertMock).toHaveBeenCalledTimes(1);
        expect(alertMock.mock.calls[0][0]).toMatchSnapshot();
    });

    it('тип файла не изображение', () => {
        simulateFilesUpload([ { name: 'foo', type: 'video/mp4' } as File ]);

        expect(alertMock).toHaveBeenCalledTimes(1);
        expect(alertMock.mock.calls[0][0]).toMatchSnapshot();
    });

    it('размер файла больше допустимого', () => {
        simulateFilesUpload([ { name: 'foo', type: 'image/jpeg', size: PANORAMA_INTERIOR_MAX_FILE_SIZE + MB } as File ]);

        expect(alertMock).toHaveBeenCalledTimes(1);
        expect(alertMock.mock.calls[0][0]).toMatchSnapshot();
    });
});

it('если файл оказался не валидным очистит значение инпута', () => {
    simulateFilesUpload([ { name: 'foo', type: 'video/mp4' } as File ]);
    expect(instance.fileInput.current.value).toBe('');
});

describe('при загрузке валидного файла', () => {
    let page: ShallowWrapper;

    it('дернет правильную ручку для получения урлов аплоадера', () => {
        page = simulateFilesUpload([ validFileMock ]);
        expect(getResource).toHaveBeenCalledTimes(1);
        expect(getResource).toHaveBeenCalledWith('getInteriorPanoramaUploadInfo');
    });

    describe('в начале загрузки', () => {
        beforeEach(() => {
            // переходим в состояние LOADING
            getResource.mockImplementation(() => new Promise(() => {}));
            page = simulateFilesUpload([ validFileMock ]);
        });

        it('поменяет статус блока', () => {
            return Promise.resolve()
                .then(() => {
                    const component = page.find('PanoramaUploadInteriorDumb');
                    expect(component.prop('status')).toBe(STATUSES.LOADING);
                });
        });

        it('сохранит статус в сторе', () => {
            return Promise.resolve()
                .then(() => {
                    expect(props.setPanoramaData).toHaveBeenCalledTimes(1);
                    expect(props.setPanoramaData).toHaveBeenCalledWith({ isLoading: true });
                });
        });
    });

    it('будет загружать файлы по пришедшему урлу аплоадера', () => {
        page = simulateFilesUpload([ validFileMock ]);
        return getUploadUrlPromise
            .then(() => {})
            .then(() => {
                expect(fetchMock).toHaveBeenCalledTimes(1);
                expect(fetchMock.mock.calls[0][0]).toBe(uploadUrlMock);
            });
    });

    describe('по окончанию', () => {
        beforeEach(() => {
            page = simulateFilesUpload([ validFileMock ]);
        });

        it('поменяет статус блока', () => {
            return getUploadUrlPromise
                .then(() => {})
                .then(() => {})
                .then(() => {
                    const component = page.find('PanoramaUploadInteriorDumb');
                    expect(component.prop('status')).toBe(STATUSES.IN_PROCESSING);
                });
        });

        it('сохранит полученный id панорамы в стор', () => {
            return getUploadUrlPromise
                .then(() => { })
                .then(() => { })
                .then(() => {
                    expect(props.setPanoramaData).toHaveBeenCalledTimes(2);
                    expect(props.setPanoramaData).toHaveBeenLastCalledWith({ id: 'panorama-id' });
                });
        });

        it('покажет нотифайку', () => {
            return getUploadUrlPromise
                .then(() => { })
                .then(() => { })
                .then(() => {
                    expect(props.showMessage).toHaveBeenCalledTimes(1);
                    expect(props.showMessage).toHaveBeenLastCalledWith({
                        message: 'Изображение загружено. В течение нескольких минут панорама появится в объявлении',
                        view: VIEW.SUCCESS,
                    });
                });
        });
    });
});

it('покажет ошибку если ручка получения урлов упала', () => {
    const getUploadInfoFailedPromise = Promise.reject();
    getResource.mockImplementationOnce(() => getUploadInfoFailedPromise);
    const page = simulateFilesUpload([ validFileMock ]);

    return getUploadInfoFailedPromise
        .catch(() => { })
        .catch(() => {
            const component = page.find('PanoramaUploadInteriorDumb');
            expect(component.prop('status')).toBe(STATUSES.LOADING_ERROR);
            expect(props.showMessage).toHaveBeenCalledTimes(1);
            expect(props.showMessage).toHaveBeenLastCalledWith({
                message: 'Произошла ошибка. Попробуйте еще раз',
                view: VIEW.ERROR,
            });
        });
});

it('покажет ошибку если загрузка файла упала', () => {
    getUploadUrlPromise = Promise.reject();
    fetchMock.mockRejectOnce();
    const page = simulateFilesUpload([ validFileMock ]);

    return getUploadUrlPromise.then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        async() => {
            await new Promise((resolve) => process.nextTick(resolve));
            const component = page.find('PanoramaUploadInteriorDumb');
            expect(component.prop('status')).toBe(STATUSES.LOADING_ERROR);
            expect(props.showMessage).toHaveBeenCalledTimes(1);
            expect(props.showMessage).toHaveBeenLastCalledWith({
                message: 'Произошла ошибка. Попробуйте еще раз',
                view: 'error',
            });
        },
    );
});

it('при повторной попытке загрузки, окончательно загрузить файл', () => {
    getUploadUrlPromise = Promise.reject();
    fetchMock
        .mockRejectOnce()
        .once(JSON.stringify({ id: 'panorama-id' }));

    const page = simulateFilesUpload([ validFileMock ]);
    instance.fileInput.current.files = [ validFileMock ];

    return getUploadUrlPromise
        .catch(() => Promise.resolve())
        .then(() => {
            const component = page.find('PanoramaUploadInteriorDumb');
            component.simulate('repeatLinkClick');
            const updatedComponent = page.find('PanoramaUploadInteriorDumb');
            expect(updatedComponent.prop('status')).toBe(STATUSES.LOADING);
        })
        .catch(() => {
            const component = page.find('PanoramaUploadInteriorDumb');
            expect(component.prop('status')).toBe(STATUSES.IN_PROCESSING);
            expect(props.setPanoramaData).toHaveBeenCalledTimes(2);
            expect(props.setPanoramaData).toHaveBeenLastCalledWith({ id: 'panorama-id' });
        });
});

describe('при отмене загрузки файла', () => {
    it('покажет сообщение о подтверждении', () => {
        // переходим в состояние LOADING
        getResource.mockImplementation(() => new Promise(() => {}));
        const page = simulateFilesUpload([ validFileMock ]);

        return Promise.resolve()
            .then(() => {
                const component = page.find('PanoramaUploadInteriorDumb');
                component.simulate('removeLinkClick');

                expect(confirmMock).toHaveBeenCalledTimes(1);
                expect(confirmMock.mock.calls[0]).toMatchSnapshot();
            });
    });

    describe('если пользователь подтвердил отмену', () => {
        let page: ShallowWrapper;
        beforeEach(() => {
            // переходим в состояние LOADING
            getResource.mockImplementation(() => new Promise(() => {}));

            confirmMock.mockImplementationOnce(() => true);
            page = simulateFilesUpload([ validFileMock ]);

            jest.useFakeTimers();
        });

        afterEach(() => {
            jest.useRealTimers();
        });

        it('покажет статус отмены', () => {
            return Promise.resolve()
                .then(() => {
                    const component = page.find('PanoramaUploadInteriorDumb');
                    component.simulate('removeLinkClick');

                    const updatedComponent = page.find('PanoramaUploadInteriorDumb');
                    expect(updatedComponent.prop('status')).toBe(STATUSES.CANCELED);
                });
        });

        it('сбросит статус в сторе', () => {
            return Promise.resolve()
                .then(() => {
                    const component = page.find('PanoramaUploadInteriorDumb');
                    component.simulate('removeLinkClick');

                    jest.advanceTimersByTime(SECOND);

                    expect(props.setPanoramaData).toHaveBeenCalledTimes(2);
                    expect(props.setPanoramaData).toHaveBeenLastCalledWith({ });
                });
        });

        it('по окончанию покажет начальное состояние и очистит значение инпута', () => {
            return Promise.resolve()
                .then(() => {
                    const component = page.find('PanoramaUploadInteriorDumb');
                    component.simulate('removeLinkClick');
                })
                .then(() => {
                    jest.advanceTimersByTime(SECOND);
                    const component = page.find('PanoramaUploadInteriorDumb');
                    expect(component.prop('status')).toBe(STATUSES.INIT);
                    expect(instance.fileInput.current.value).toBe('');
                });
        });
    });
});

describe('при дропе файла', () => {
    it('загрузит его если статус блока был init', () => {
        simulateFilesDrop([ validFileMock ]);

        expect(getResource).toHaveBeenCalledTimes(1);
        expect(getResource).toHaveBeenCalledWith('getInteriorPanoramaUploadInfo');
    });

    it('покажет алерт если статус у блока не init', () => {
        props.data = { status: 'COMPLETED' } as InteriorPanorama;
        simulateFilesDrop([ validFileMock ]);

        expect(getResource).toHaveBeenCalledTimes(0);
        expect(alertMock).toHaveBeenCalledTimes(1);
        expect(alertMock.mock.calls[0][0]).toMatchSnapshot();
    });
});

describe('при клике на крестик', () => {
    it('если блок в статусе loading_error сбросит состояние на первоначальное', () => {
        const page = shallowRenderComponent({ props });
        instance.setState({ status: STATUSES.LOADING_ERROR });

        const component = page.find('PanoramaUploadInteriorDumb');
        component.simulate('removeLinkClick');

        const updatedComponent = page.find('PanoramaUploadInteriorDumb');
        expect(updatedComponent.prop('status')).toBe(STATUSES.INIT);
    });

    it('если блок в статусе processing_error сбросит состояние на первоначальное', () => {
        const page = shallowRenderComponent({ props });
        instance.setState({ status: STATUSES.PROCESSING_ERROR });

        const component = page.find('PanoramaUploadInteriorDumb');
        component.simulate('removeLinkClick');

        const updatedComponent = page.find('PanoramaUploadInteriorDumb');
        expect(updatedComponent.prop('status')).toBe(STATUSES.INIT);
    });

    it('если блок в статусе in_processing покажет окно подтверждения', () => {
        const page = shallowRenderComponent({ props });
        instance.setState({ status: STATUSES.IN_PROCESSING });

        const component = page.find('PanoramaUploadInteriorDumb');
        component.simulate('removeLinkClick');

        const updatedComponent = page.find('PanoramaUploadInteriorDumb');
        expect(updatedComponent.prop('isConfirmModalVisible')).toBe(true);
    });

    it('если блок в статусе processed покажет окно подтверждения', () => {
        const page = shallowRenderComponent({ props });
        instance.setState({ status: STATUSES.PROCESSED });

        const component = page.find('PanoramaUploadInteriorDumb');
        component.simulate('removeLinkClick');

        const updatedComponent = page.find('PanoramaUploadInteriorDumb');
        expect(updatedComponent.prop('isConfirmModalVisible')).toBe(true);
    });
});

describe('при отвязке панорамы', () => {
    let page: ShallowWrapper;

    beforeEach(() => {
        props.data = { status: 'COMPLETED', id: 'panorama-id-mock' } as InteriorPanorama;

        page = shallowRenderComponent({ props });

        const component = page.find('PanoramaUploadInteriorDumb');
        component.simulate('panoramaRemove', 'remove-reason-mock');
    });

    it('сохранит в сторе пустой id', () => {
        expect(props.setPanoramaData).toHaveBeenCalledTimes(1);
        expect(props.setPanoramaData).toHaveBeenCalledWith({ });
    });

    it('отправит метрику', () => {
        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams.mock.calls[0]).toMatchSnapshot();
    });

    it('изменит состояние блока на init', () => {
        const component = page.find('PanoramaUploadInteriorDumb');
        expect(component.prop('status')).toBe(STATUSES.INIT);
    });
});

describe('поллинг статуса при удачной загрузке файла', () => {

    it('начнется сразу после загрузки файла', () => {
        const fetchStatusMock = Promise.resolve({ status: 'SUCCESS', panorama: { status: 'PROCESSING' } });
        getResource.mockImplementationOnce(() => getUploadUrlPromise);
        getResource.mockImplementationOnce(() => fetchStatusMock);

        simulateFilesUpload([ validFileMock ]);

        return waitUntilFileIsLoaded()
            .then(() => {
                expect(getResource).toHaveBeenCalledTimes(2);
                expect(getResource.mock.calls[1]).toMatchSnapshot();
            });
    });

    it('если панорама обработана, обновит статус блока', () => {
        const fetchStatusMock = Promise.resolve({ status: 'SUCCESS', panorama: { status: 'COMPLETED', foo: 'bar' } });
        getResource.mockImplementationOnce(() => getUploadUrlPromise);
        getResource.mockImplementationOnce(() => fetchStatusMock);
        const page = simulateFilesUpload([ validFileMock ]);

        return waitUntilFileIsLoaded()
            .then(() => { })
            .then(() => {
                const props = page.find('PanoramaUploadInteriorDumb').props() as any;
                expect(props.status).toBe(STATUSES.PROCESSED);
                expect(props.data.foo).toBe('bar');
            });
    });

    it('если панорама сфейлилась, обновит статус блока', () => {
        const fetchStatusMock = Promise.resolve({ status: 'SUCCESS', panorama: { status: 'FAILED', foo: 'bar' } });
        getResource.mockImplementationOnce(() => getUploadUrlPromise);
        getResource.mockImplementationOnce(() => fetchStatusMock);
        const page = simulateFilesUpload([ validFileMock ]);

        return waitUntilFileIsLoaded()
            .then(() => { })
            .then(() => {
                const props = page.find('PanoramaUploadInteriorDumb').props() as any;
                expect(props.status).toBe(STATUSES.PROCESSING_ERROR);
                expect(props.data.foo).toBe('bar');
            });

    });
});

it('на форме редактирования после загрузки файла привяжет панораму к офферу', () => {
    props.isEdit = true;
    props.category = 'cars';
    props.offerId = 'offer-id';
    const fetchStatusMock = Promise.resolve({ status: 'SUCCESS', panorama: { status: 'PROCESSING' } });
    getResource.mockImplementationOnce(() => getUploadUrlPromise);
    getResource.mockImplementationOnce(() => fetchStatusMock);

    simulateFilesUpload([ validFileMock ]);
    return waitUntilFileIsLoaded()
        .then(() => {
            expect(getResource).toHaveBeenCalledTimes(3);
            expect(getResource.mock.calls[1]).toMatchSnapshot();
        });
});

function waitUntilFileIsLoaded() {
    return getUploadUrlPromise
        .then(() => { })
        .then(() => { })
        .then(() => { })
        .then(() => { })
        .then(() => { })
        .then(() => { });
}

function simulateFilesUpload(files: Array<File>) {
    getDataTransferFilesMock.mockReturnValue(files);

    const page = shallowRenderComponent({ props });
    const input = page.find('input');
    input.simulate('change');

    return page;
}

function simulateFilesDrop(files: Array<File>) {
    getDataTransferFilesMock.mockReturnValue(files);

    const page = shallowRenderComponent({ props });
    const component = page.find('PanoramaUploadInteriorDumb');
    component.simulate('drop');

    return page;
}

function shallowRenderComponent({ props }: { props: Props }) {
    const ContextProvider = createContextProvider(contextMock);

    const page = shallow(
        <ContextProvider>
            <PanoramaUploadInterior { ...props }/>
        </ContextProvider>,
    ).dive();

    instance = page.instance();
    instance.fileInput = {
        current: {
            click: jest.fn(),
            value: 'some file path',
        },
    };

    return page;
}
