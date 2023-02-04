/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

jest.mock('auto-core/lib/util/getDataTransferFiles', () => ({
    'default': jest.fn(),
}));

const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const PanoramaUploadExterior = require('./PanoramaUploadExterior');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const getDataTransferFiles = require('auto-core/lib/util/getDataTransferFiles').default;

const getResource = require('auto-core/react/lib/gateApi').getResource;

const consts = require('../consts');
const { MB, SECOND } = require('auto-core/lib/consts');
const fetchMock = require('jest-fetch-mock');

const uploaderUrlsMock = {
    create_url: 'create_url',
    upload_url: 'upload_url',
    confirm_url: 'confirm_url',
};
const getUploadInfoPromise = Promise.resolve({ multi_part_upload_urls: uploaderUrlsMock });
getResource.mockImplementation(() => getUploadInfoPromise);

let props;
let context;
let instance;

const validFileMock = {
    name: 'spyaschii_kotik',
    type: 'video/mp4',
    size: 42 * MB,
    slice: (start, end) => `file part from ${ start } to ${ end } bytes`,
};
const formDataAppendMock = jest.fn();

beforeEach(() => {
    props = {
        setPanoramaData: jest.fn(),
        showMessage: jest.fn(),
        data: {
            id: 'panorama-exterior-id',
        },
        isDealer: true,
    };
    context = _.cloneDeep(contextMock);

    context.metrika.sendParams.mockClear();
    getResource.mockClear();
    getDataTransferFiles.mockReturnValue([]);

    jest.spyOn(global, 'alert').mockImplementation(() => {});
    jest.spyOn(global, 'confirm').mockImplementation(() => {});
    jest.spyOn(global, 'FormData').mockReturnValue(({
        append: formDataAppendMock,
    }));
});

afterEach(() => {
    fetchMock.resetMocks();
    jest.restoreAllMocks();
});

describe('при первом рендере покажет соответствующий статус', () => {
    it('если панорама обработана', () => {
        props.data.status = 'COMPLETED';
        const page = shallowRenderComponent({ props, context });

        expect(page.find('PanoramaUploadExteriorDumb').prop('status')).toBe(consts.STATUSES.PROCESSED);
    });

    it('если панорама в процессе обработки', () => {
        props.data.status = 'PROCESSING';
        const page = shallowRenderComponent({ props, context });

        expect(page.find('PanoramaUploadExteriorDumb').prop('status')).toBe(consts.STATUSES.IN_PROCESSING);
    });

    it('если обработка панорамы упала', () => {
        props.data.status = 'FAILED';
        const page = shallowRenderComponent({ props, context });

        expect(page.find('PanoramaUploadExteriorDumb').prop('status')).toBe(consts.STATUSES.PROCESSING_ERROR);
    });

    it('если у панорамы еще нет статуса', () => {
        props.data.status = '';
        const page = shallowRenderComponent({ props, context });

        expect(page.find('PanoramaUploadExteriorDumb').prop('status')).toBe(consts.STATUSES.INIT);

    });
});

describe('поллинг статуса при маунте', () => {
    it('начнется, если панорама находится в статусе PROCESSING', () => {
        props.data.status = 'PROCESSING';

        shallowRenderComponent({ props, context });

        expect(getResource).toHaveBeenCalledTimes(1);
        expect(getResource.mock.calls[0]).toMatchSnapshot();
    });

    it('не начнется, если панорама находится в статусе COMPLETED', () => {
        props.data.status = 'COMPLETED';

        shallowRenderComponent({ props, context });

        expect(getResource).toHaveBeenCalledTimes(0);
    });
});

describe('при клике на область загрузки', () => {
    it('если текущий статус init кликнет на инпуте', () => {
        const page = shallowRenderComponent({ props, context });
        const component = page.find('PanoramaUploadExteriorDumb');
        component.simulate('uploadAreaClick');

        expect(instance.fileInput.click).toHaveBeenCalledTimes(1);
    });

    it('если текущий статус не init ничего не будет делать', () => {
        props.data.status = 'FAILED';
        const page = shallowRenderComponent({ props, context });
        const component = page.find('PanoramaUploadExteriorDumb');
        component.simulate('uploadAreaClick');

        expect(instance.fileInput.click).toHaveBeenCalledTimes(0);
    });
});

describe('при выборе файла покажет алерт если', () => {
    it('файлов больше чем один', () => {
        simulateFilesUpload([ { name: 'foo' }, { name: 'bar' } ]);

        expect(global.alert).toHaveBeenCalledTimes(1);
        expect(global.alert.mock.calls[0][0]).toMatchSnapshot();
    });

    it('тип файла не видео', () => {
        simulateFilesUpload([ { name: 'foo', type: 'image/jpeg' } ]);

        expect(global.alert).toHaveBeenCalledTimes(1);
        expect(global.alert.mock.calls[0][0]).toMatchSnapshot();
    });

    it('размер файла меньше допустимого', () => {
        simulateFilesUpload([ { name: 'foo', type: 'video/mp4', size: consts.PANORAMA_EXTERIOR_MIN_FILE_SIZE - MB } ]);

        expect(global.alert).toHaveBeenCalledTimes(1);
        expect(global.alert.mock.calls[0][0]).toMatchSnapshot();
    });

    it('размер файла больше допустимого', () => {
        simulateFilesUpload([ { name: 'foo', type: 'video/mp4', size: consts.PANORAMA_EXTERIOR_MAX_FILE_SIZE + MB } ]);

        expect(global.alert).toHaveBeenCalledTimes(1);
        expect(global.alert.mock.calls[0][0]).toMatchSnapshot();
    });
});

it('если файл оказался не валидным очистит значение инпута', () => {
    simulateFilesUpload([ { name: 'foo', type: 'image/jpeg' } ]);
    expect(instance.fileInput.value).toBe('');
});

describe('при загрузке валидного файла', () => {
    let page;
    beforeEach(() => {
        getResource.mockImplementation((method) => {
            if (method === 'getExteriorPanoramaUploadInfo') {
                return getUploadInfoPromise;
            } else if (method === 'getExteriorPanorama') {
                return Promise.resolve({ status: 'SUCCESS', panorama: { status: 'PROCESSING' } });
            }

            throw new Error('Unknown gateApi.getResource method');
        });
    });

    describe('в начале загрузки', () => {
        let fetchCreateUrlPromise;
        beforeEach(() => {
            fetchCreateUrlPromise = Promise.resolve(JSON.stringify({ uploadId: 'upload-id-mock' }));
            fetchMock.mockResponse((request) => {
                if (request.url === 'create_url') {
                    // отвечаем на старт загрузки
                    return fetchCreateUrlPromise;
                }

                // подвешиваем запросы на загрузку чанков
                return new Promise(() => {});
            });

            page = simulateFilesUpload([ validFileMock ]);
        });

        it('дернет правильную ручку для получения урлов аплоадера', () => {
            expect(getResource).toHaveBeenCalledTimes(1);
            expect(getResource).toHaveBeenCalledWith('getExteriorPanoramaUploadInfo');
        });

        it('поменяет статус блока', () => {
            return fetchCreateUrlPromise
                // ждем начала загрузки чанков
                .then(() => new Promise((resolve) => setTimeout(resolve, 100)))
                .then(() => {
                    const component = page.find('PanoramaUploadExteriorDumb');
                    expect(component.prop('status')).toBe(consts.STATUSES.LOADING);
                    expect(component.prop('loadingProgress')).toBe(0);
                });
        });

        it('сохранит статус в сторе', () => {
            return fetchCreateUrlPromise
                // ждем начала загрузки чанков
                .then(() => new Promise((resolve) => setTimeout(resolve, 100)))
                .then(() => {
                    expect(props.setPanoramaData).toHaveBeenCalledTimes(1);
                    expect(props.setPanoramaData.mock.calls[0]).toMatchSnapshot();
                });
        });
    });

    describe('процесс загрузки', () => {
        beforeEach(() => {
            fetch.mockResponses(...getSuccessfulUploadResponses());
            page = simulateFilesUpload([ validFileMock ]);
        });

        it('правильно сформирует урлы для запросов в аплоадер', () => {
            return waitForAllResponsesFinish(() => {
                const formattedCalls = fetch.mock.calls.map(formatFetchCalls);
                expect(formattedCalls).toMatchSnapshot();
            });
        });

        it('правильно нарежет файл для загрузки', () => {
            return waitForAllResponsesFinish(() => {
                expect(formDataAppendMock.mock.calls).toMatchSnapshot();
            });
        });
    });

    describe('прогресс загрузки', () => {
        beforeEach(() => {
            // Проверяем статус не по окончанию всех запросов, а когда только первые 5 из них выполнятся
            fetch.mockResponses(...getSuccessfulUploadResponses().slice(0, 4));
            page = simulateFilesUpload([ validFileMock ]);
        });

        it('правильно рассчитает прогресс в ходе загрузки', () => {
            return getUploadInfoPromise
                .then(() => { })
                .then(() => { })
                .then(() => { })
                .then(() => { })
                .then(() => { })
                .then(() => {
                    const component = page.find('PanoramaUploadExteriorDumb');
                    expect(component.prop('loadingProgress')).toBe(60);
                });
        });
    });

    describe('по окончанию', () => {
        beforeEach(() => {
            getResource.mockImplementation((method) => {
                if (method === 'getExteriorPanoramaUploadInfo') {
                    return getUploadInfoPromise;
                } else if (method === 'getExteriorPanorama') {
                    return Promise.resolve({ status: 'SUCCESS', panorama: { status: 'PROCESSING' } });
                }

                throw new Error('Unknown gateApi.getResource method');
            });

            fetch.mockResponses(...getSuccessfulUploadResponses());
            page = simulateFilesUpload([ validFileMock ]);
        });

        it('поменяет статус блока', () => {
            return waitForAllResponsesFinish(() => {
                const component = page.find('PanoramaUploadExteriorDumb');
                expect(component.prop('status')).toBe(consts.STATUSES.IN_PROCESSING);
            });
        });

        it('сохранит полученный id панорамы в стор', () => {
            return waitForAllResponsesFinish(() => {
                expect(props.setPanoramaData).toHaveBeenCalledTimes(2);
                expect(props.setPanoramaData.mock.calls[1]).toMatchSnapshot();
            });
        });

        it('покажет нотифайку', () => {
            return waitForAllResponsesFinish(() => {
                expect(props.showMessage).toHaveBeenCalledTimes(1);
                expect(props.showMessage.mock.calls[0]).toMatchSnapshot();
            });
        });

        it('начнется поллинг статуса панорамы', () => {
            return waitForAllResponsesFinish(() => {
                expect(getResource).toHaveBeenCalledTimes(2);
                expect(getResource.mock.calls[1]).toMatchSnapshot();
            });
        });
    });
});

describe('при поллинг статуса панорамы', () => {
    it('если панорама окажется обработанной, поменяет статус блока', () => {
        const fetchStatusMock = Promise.resolve({ status: 'SUCCESS', panorama: { status: 'COMPLETED', foo: 'bar' } });
        getResource.mockImplementationOnce(() => getUploadInfoPromise);
        getResource.mockImplementationOnce(() => fetchStatusMock);

        fetch.mockResponses(...getSuccessfulUploadResponses());
        const page = simulateFilesUpload([ validFileMock ]);

        return waitForAllResponsesFinish(() => {
            const props = page.find('PanoramaUploadExteriorDumb').props();
            expect(props.status).toBe(consts.STATUSES.PROCESSED);
            expect(props.data.foo).toBe('bar');
        });
    });

    it('если панорама сфейлится, поменяет статус блока', () => {
        const fetchStatusMock = Promise.resolve({ status: 'SUCCESS', panorama: { status: 'FAILED', foo: 'bar' } });
        getResource.mockImplementationOnce(() => getUploadInfoPromise);
        getResource.mockImplementationOnce(() => fetchStatusMock);

        fetch.mockResponses(...getSuccessfulUploadResponses());
        const page = simulateFilesUpload([ validFileMock ]);

        return waitForAllResponsesFinish(() => {
            const props = page.find('PanoramaUploadExteriorDumb').props();
            expect(props.status).toBe(consts.STATUSES.PROCESSING_ERROR);
            expect(props.data.foo).toBe('bar');
        });
    });
});

it('на форме редактирования после загрузки файла привяжет панораму к офферу', () => {
    props.isEdit = true;
    props.category = 'cars';
    props.offerId = 'offer-id';
    const fetchStatusMock = Promise.resolve({ status: 'SUCCESS', panorama: { status: 'PROCESSING' } });
    getResource.mockImplementationOnce(() => getUploadInfoPromise);
    getResource.mockImplementationOnce(() => fetchStatusMock);

    fetch.mockResponses(...getSuccessfulUploadResponses());
    simulateFilesUpload([ validFileMock ]);
    return waitForAllResponsesFinish(() => {
        expect(getResource).toHaveBeenCalledTimes(3);
        expect(getResource.mock.calls[1]).toMatchSnapshot();
    });
});

it('покажет ошибку если ручка получения урлов упала', () => {
    const getUploadInfoFailedPromise = Promise.reject();
    getResource.mockImplementationOnce(() => getUploadInfoFailedPromise);
    const page = simulateFilesUpload([ validFileMock ]);

    return getUploadInfoFailedPromise
        .catch(() => {})
        .then(() => {})
        .then(() => {
            const component = page.find('PanoramaUploadExteriorDumb');
            expect(component.prop('status')).toBe(consts.STATUSES.LOADING_ERROR);
            expect(props.showMessage).toHaveBeenCalledTimes(1);
            expect(props.showMessage.mock.calls[0]).toMatchSnapshot();
        });
});

it('покажет ошибку если запрос на открытие сессии упал', () => {
    fetch.mockRejectOnce({ error: 'some error' });
    const page = simulateFilesUpload([ validFileMock ]);

    return waitForAllResponsesFinish(() => {
        const component = page.find('PanoramaUploadExteriorDumb');
        expect(component.prop('status')).toBe(consts.STATUSES.LOADING_ERROR);
        expect(props.showMessage).toHaveBeenCalledTimes(1);
        expect(props.showMessage.mock.calls[0]).toMatchSnapshot();
    }, 1);
});

it('если загрузка одного чанка зафейлилась 1 раз, все равно загрузит файл', () => {
    fetch
        .once(JSON.stringify({ uploadId: 'upload-id-mock' }))
        .mockRejectOnce(JSON.stringify({ error: 'some error' }))
        .once(JSON.stringify({ etag: 'etag-2' }))
        .once(JSON.stringify({ etag: 'etag-3' }))
        .once(JSON.stringify({ etag: 'etag-4' }))
        .once(JSON.stringify({ etag: 'etag-5' }))
        .once(JSON.stringify({ etag: 'etag-1' }))
        .once(JSON.stringify({ id: 'panorama-id-mock' }));

    const page = simulateFilesUpload([ validFileMock ]);

    return waitForAllResponsesFinish(() => {
        const formattedCalls = fetch.mock.calls.map(formatFetchCalls);
        expect(formattedCalls).toMatchSnapshot();

        const component = page.find('PanoramaUploadExteriorDumb');
        expect(component.prop('status')).toBe(consts.STATUSES.IN_PROCESSING);
    }, 8);
});

describe('если загрузка одного чанка зафейлилась 2 и более раз', () => {
    let page;

    beforeEach(() => {
        fetch
            .once(JSON.stringify({ uploadId: 'upload-id-mock' }))
            .mockRejectOnce(JSON.stringify({ error: 'some error' }))
            .once(JSON.stringify({ etag: 'etag-2' }))
            .once(JSON.stringify({ etag: 'etag-3' }))
            .once(JSON.stringify({ etag: 'etag-4' }))
            .once(JSON.stringify({ etag: 'etag-5' }))
            .mockRejectOnce(JSON.stringify({ error: 'some error' }));

        page = simulateFilesUpload([ validFileMock ]);
    });

    it('не закроет сессию аплоадера', () => {
        return waitForAllResponsesFinish(() => {
            const lastFetchCallUrl = _.last(fetch.mock.calls)[0];
            expect(lastFetchCallUrl).not.toContain(uploaderUrlsMock.confirm_url);
        }, 7);
    });

    it('покажет ошибку загрузки', () => {
        return waitForAllResponsesFinish(() => {
            const component = page.find('PanoramaUploadExteriorDumb');
            expect(component.prop('status')).toBe(consts.STATUSES.LOADING_ERROR);
        }, 7);
    });
});

it('при повторной попытке загрузки, окончательно загрузить файл', () => {

    fetch
        .once(JSON.stringify({ uploadId: 'upload-id-mock' }))
        .mockRejectOnce(JSON.stringify({ error: 'some error' }))
        .once(JSON.stringify({ etag: 'etag-2' }))
        .once(JSON.stringify({ etag: 'etag-3' }))
        .once(JSON.stringify({ etag: 'etag-4' }))
        .once(JSON.stringify({ etag: 'etag-5' }))
        .mockRejectOnce(JSON.stringify({ error: 'some error' }))
        .once(JSON.stringify({ etag: 'etag-1' }))
        .once(JSON.stringify({ id: 'panorama-id-mock' }));

    const page = simulateFilesUpload([ validFileMock ]);

    return new Promise((done) => {
        const int = setInterval(() => {
            /* eslint-disable jest/no-conditional-expect */
            if (fetch.mock.calls.length === 7) {
                const component = page.find('PanoramaUploadExteriorDumb');
                expect(component.prop('status')).toBe(consts.STATUSES.LOADING_ERROR);

                component.simulate('repeatLinkClick');

                const updatedComponent = page.find('PanoramaUploadExteriorDumb');
                expect(updatedComponent.prop('status')).toBe(consts.STATUSES.LOADING);
                expect(props.setPanoramaData).toHaveBeenCalledTimes(2);
                expect(props.setPanoramaData.mock.calls[1]).toMatchSnapshot();
            }

            if (fetch.mock.calls.length === 9) {
                const component = page.find('PanoramaUploadExteriorDumb');
                expect(component.prop('status')).toBe(consts.STATUSES.IN_PROCESSING);
                expect(props.setPanoramaData).toHaveBeenCalledTimes(3);
                expect(props.setPanoramaData.mock.calls[2]).toMatchSnapshot();

                clearInterval(int);
                done();
            }
        }, 200);
    });
});

describe('при отмене загрузки файла', () => {
    it('покажет сообщение о подтверждении', () => {
        fetch.mockResponses(...getSuccessfulUploadResponses());
        const page = simulateFilesUpload([ validFileMock ]);

        return getUploadInfoPromise
            .then(() => {})
            .then(() => {
                instance.setState({ status: consts.STATUSES.LOADING });
                const component = page.find('PanoramaUploadExteriorDumb');
                component.simulate('removeLinkClick');

                expect(global.confirm).toHaveBeenCalledTimes(1);
                expect(global.confirm.mock.calls[0]).toMatchSnapshot();
            });
    });

    describe('если пользователь подтвердил отмену', () => {
        let page;
        beforeEach(() => {
            global.confirm.mockImplementationOnce(() => true);
            fetch.mockResponse((request) => {
                if (request.url === 'create_url') {
                    return Promise.resolve(JSON.stringify({ uploadId: 'upload-id-mock' }));
                }

                return new Promise(() => {});
            });
            page = simulateFilesUpload([ validFileMock ]);
        });

        afterEach(() => {
            jest.useRealTimers();
        });

        it('покажет статус отмены', () => {
            return getUploadInfoPromise
                // ждем начала загрузки чанков
                .then(() => new Promise((resolve) => setTimeout(resolve, 100)))
                .then(() => {
                    const component = page.find('PanoramaUploadExteriorDumb');
                    component.simulate('removeLinkClick');

                    const updatedComponent = page.find('PanoramaUploadExteriorDumb');
                    expect(updatedComponent.prop('status')).toBe(consts.STATUSES.CANCELED);
                });
        });

        it('сбросит статус в сторе', () => {
            return getUploadInfoPromise
                .then(() => new Promise((resolve) => setTimeout(resolve, 100)))
                .then(() => {
                    jest.useFakeTimers();
                    const component = page.find('PanoramaUploadExteriorDumb');
                    component.simulate('removeLinkClick');

                    jest.advanceTimersByTime(SECOND);

                    expect(props.setPanoramaData).toHaveBeenCalledTimes(2);
                    expect(props.setPanoramaData.mock.calls[1]).toMatchSnapshot();
                });
        });

        it('не будет запрашивать последующие чанки', () => {
            return getUploadInfoPromise
                .then(() => new Promise((resolve) => setTimeout(resolve, 100)))
                .then(() => {
                    const component = page.find('PanoramaUploadExteriorDumb');
                    component.simulate('removeLinkClick');
                })
                .then(() => { })
                .then(() => { })
                .then(() => { })
                .then(() => {
                    expect(fetch).toHaveBeenCalledTimes(4);
                });
        });

        it('по окончанию покажет начальное состояние и очистит значение инпута', () => {
            return getUploadInfoPromise
                .then(() => new Promise((resolve) => setTimeout(resolve, 100)))
                .then(() => {
                    jest.useFakeTimers();
                    const component = page.find('PanoramaUploadExteriorDumb');
                    component.simulate('removeLinkClick');
                })
                .then(() => { })
                .then(() => { })
                .then(() => { })
                .then(() => {
                    jest.advanceTimersByTime(SECOND);
                    const component = page.find('PanoramaUploadExteriorDumb');
                    expect(component.prop('status')).toBe(consts.STATUSES.INIT);
                    expect(instance.fileInput.value).toBe('');
                });
        });
    });
});

describe('при дропе файла', () => {
    beforeEach(() => {
        fetch.mockResponses(...getSuccessfulUploadResponses());
    });

    it('загрузит его если статус блока был init', () => {
        props.data = null; // если привязанной панорамы нет, то и data тоже нет
        const page = simulateFilesDrop([ validFileMock ]);

        expect(getResource).toHaveBeenCalledTimes(1);

        return waitForAllResponsesFinish(() => {
            const component = page.find('PanoramaUploadExteriorDumb');
            expect(component.prop('status')).toBe(consts.STATUSES.IN_PROCESSING);
        });
    });

    it('ничего не будет делать если статус блока не init', () => {
        getDataTransferFiles.mockReturnValue([ validFileMock ]);

        const page = shallowRenderComponent({ props, context });
        instance.setState({ status: consts.STATUSES.LOADING });

        const component = page.find('PanoramaUploadExteriorDumb');
        component.simulate('drop');

        expect(getResource).toHaveBeenCalledTimes(0);
    });

    it('покажет алерт если уже есть какая-то панорама', () => {
        props.data.status = 'COMPLETED';
        simulateFilesDrop([ validFileMock ]);

        expect(global.alert).toHaveBeenCalledTimes(1);
        expect(global.alert.mock.calls[0][0]).toMatchSnapshot();
    });
});

describe('при клике на крестик', () => {
    it('если блок в статусе loading_error сбросит состояние на первоначальное', () => {
        const page = shallowRenderComponent({ props, context });
        instance.setState({ status: consts.STATUSES.LOADING_ERROR });

        const component = page.find('PanoramaUploadExteriorDumb');
        component.simulate('removeLinkClick');

        const updatedComponent = page.find('PanoramaUploadExteriorDumb');
        expect(updatedComponent.prop('status')).toBe(consts.STATUSES.INIT);
    });

    it('если блок в статусе processing_error сбросит состояние на первоначальное', () => {
        const page = shallowRenderComponent({ props, context });
        instance.setState({ status: consts.STATUSES.PROCESSING_ERROR });

        const component = page.find('PanoramaUploadExteriorDumb');
        component.simulate('removeLinkClick');

        const updatedComponent = page.find('PanoramaUploadExteriorDumb');
        expect(updatedComponent.prop('status')).toBe(consts.STATUSES.INIT);
    });

    it('если блок в статусе in_processing покажет окно подтверждения', () => {
        const page = shallowRenderComponent({ props, context });
        instance.setState({ status: consts.STATUSES.IN_PROCESSING });

        const component = page.find('PanoramaUploadExteriorDumb');
        component.simulate('removeLinkClick');

        const updatedComponent = page.find('PanoramaUploadExteriorDumb');
        expect(updatedComponent.prop('isConfirmModalVisible')).toBe(true);
    });

    it('если блок в статусе processed покажет окно подтверждения', () => {
        const page = shallowRenderComponent({ props, context });
        instance.setState({ status: consts.STATUSES.PROCESSED });

        const component = page.find('PanoramaUploadExteriorDumb');
        component.simulate('removeLinkClick');

        const updatedComponent = page.find('PanoramaUploadExteriorDumb');
        expect(updatedComponent.prop('isConfirmModalVisible')).toBe(true);
    });
});

describe('при отвязке панорамы', () => {
    let page;

    beforeEach(() => {
        props.data.status = 'COMPLETED';
        props.data.id = 'panorama-id-mock';
        page = shallowRenderComponent({ props, context });

        const component = page.find('PanoramaUploadExteriorDumb');
        component.simulate('panoramaRemove', 'remove-reason-mock');
    });

    it('сохранит в сторе пустой id', () => {
        expect(props.setPanoramaData).toHaveBeenCalledTimes(1);
        expect(props.setPanoramaData.mock.calls[0]).toMatchSnapshot();
    });

    it('отправит метрику', () => {
        expect(context.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendParams.mock.calls[0]).toMatchSnapshot();
    });

    it('изменит состояние блока на init', () => {
        const component = page.find('PanoramaUploadExteriorDumb');
        expect(component.prop('status')).toBe(consts.STATUSES.INIT);
    });
});

function simulateFilesUpload(files) {
    getDataTransferFiles.mockReturnValue(files);

    const page = shallowRenderComponent({ props, context });
    const input = page.find('input');
    input.simulate('change');

    return page;
}

function simulateFilesDrop(files) {
    getDataTransferFiles.mockReturnValue(files);

    const page = shallowRenderComponent({ props, context });
    const component = page.find('PanoramaUploadExteriorDumb');
    component.simulate('drop');

    return page;
}

function getSuccessfulUploadResponses() {
    return [
        [ JSON.stringify({ uploadId: 'upload-id-mock' }) ],
        [ JSON.stringify({ etag: 'etag-1' }) ],
        [ JSON.stringify({ etag: 'etag-2' }) ],
        [ JSON.stringify({ etag: 'etag-3' }) ],
        [ JSON.stringify({ etag: 'etag-4' }) ],
        [ JSON.stringify({ etag: 'etag-5' }) ],
        [ JSON.stringify({ id: 'panorama-id-mock' }) ],
    ];
}

function waitForAllResponsesFinish(callback, callsNum = 7) {
    return new Promise((done) => {
        const int = setInterval(() => {
            if (fetch.mock.calls.length === callsNum) {
                callback();

                clearInterval(int);
                done();
            }
        }, 200);
    });
}

function formatFetchCalls([ first, second ]) {
    // тут в body попадает и постоянно дублируется мок FormData.append
    // его вызовы мы проверим отдельно ниже, а тут просто заменяем чтоб не загромождать снэпшот
    if (second.body) {
        second.body = 'mock-file-data';
    }

    return [ first, second ];
}

function shallowRenderComponent({ context, props }) {
    const ContextProvider = createContextProvider(context);

    const page = shallow(
        <ContextProvider>
            <PanoramaUploadExterior { ...props }/>
        </ContextProvider>,
    ).dive();

    instance = page.instance();
    instance.fileInput = {
        click: jest.fn(),
        value: 'some file path',
    };

    return page;
}
