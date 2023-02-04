const React = require('react');
const { shallow } = require('enzyme');
const { Provider } = require('react-redux');

const VinReportCommentForm = require('./VinReportCommentForm');

jest.mock('auto-core/react/lib/uploadImage');
jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

const upload = require('auto-core/react/lib/uploadImage');
const getResource = require('auto-core/react/lib/gateApi').getResource;

const mockStore = require('autoru-frontend/mocks/mockStore').default;

beforeEach(() => {
    upload.mockRejectedValue('no_mock_error');
});

afterEach(() => {
    upload.mockClear();
    getResource.mockClear();
});

it('должен начать загружать картинки и поменять стейт (состояние в процессе загрузки)', () => {
    const files = [
        { name: '1', preview: 'preview1' },
    ];
    const gateApiPromise = Promise.resolve({ upload_url: 'upload url' });
    getResource.mockImplementation(() => gateApiPromise);
    const pr = Promise.resolve({ photo: { mds_photo_info: { foo: 'bar' }, sizes: { '1200x900': 'url1' } } });
    upload.mockImplementation(() => pr);
    const component = shallow(
        <Provider store={ mockStore({}) }>
            <VinReportCommentForm/>
        </Provider>,
    ).dive().dive();
    component.find('PhotoAddButton').simulate('drop', {}, files);
    return gateApiPromise.then(() => {
        expect(upload).toHaveBeenCalledWith('upload url', { name: '1', preview: 'preview1' });
        expect(component.state().images).toEqual([
            {
                isUploading: true,
                needUpload: false,
                key: '1',
                src: 'preview1',
                thumbnail: 'preview1',
                file: { name: '1', preview: 'preview1' },
                isUploadRejected: false,
            },
        ]);
    });
});

it('не должен повторно загружать уже загруженный файл', () => {
    const files = [
        { name: '1', preview: 'preview1' },
    ];
    getResource.mockResolvedValue({});
    const component = shallow(
        <Provider store={ mockStore({}) }>
            <VinReportCommentForm
                uploadUrl="upload url"
            />
        </Provider>,
    ).dive().dive();
    component.find('PhotoAddButton').simulate('drop', {}, files);
    const firstState = component.state().photos;
    component.find('PhotoAddButton').simulate('drop', {}, files);
    const secondState = component.state().photos;
    expect(firstState).toEqual(secondState);
});

it('должен загрузить картинки и поменять стейт (успех)', () => {
    const files = [
        { name: '1', preview: 'preview1' },
    ];
    const gateApiPromise = Promise.resolve({ upload_url: 'upload url' });
    getResource.mockImplementation(() => gateApiPromise);
    const pr = Promise.resolve({ photo: { mds_photo_info: { foo: 'bar' }, sizes: { '1200x900': 'url1' } } });
    upload.mockImplementation(() => pr);
    const component = shallow(
        <Provider store={ mockStore({}) }>
            <VinReportCommentForm/>
        </Provider>,
    ).dive().dive();
    component.find('PhotoAddButton').simulate('drop', {}, files);
    return gateApiPromise.then(() => {
        return pr.then(() => {
            expect(upload).toHaveBeenCalledTimes(1);
            expect(component.state().images).toEqual([
                {
                    isUploading: false,
                    needUpload: false,
                    key: '1',
                    mds_photo_info: { foo: 'bar' },
                    src: 'preview1',
                    thumbnail: 'preview1',
                    file: { name: '1', preview: 'preview1' },
                    isUploadRejected: false,
                    widescreen: 'url1',
                },
            ]);
        });
    });
});

it('должен загрузить картинки и поменять стейт (ошибка загрузки)', () => {
    const files = [
        { name: '1', preview: 'preview1' },
    ];
    const gateApiPromise = Promise.resolve({ upload_url: 'upload url' });
    getResource.mockImplementation(() => gateApiPromise);
    const pr = Promise.reject();
    upload.mockImplementation(() => pr);
    const component = shallow(
        <Provider store={ mockStore({}) }>
            <VinReportCommentForm/>
        </Provider>,
    ).dive().dive();
    component.find('PhotoAddButton').simulate('drop', {}, files);
    return gateApiPromise.then(() => {
        return pr.then(
            () => {},
            () => {
                expect(upload).toHaveBeenCalledTimes(1);
                expect(component.state().images).toEqual([
                    {
                        isUploading: false,
                        needUpload: false,
                        isUploadRejected: true,
                        key: '1',
                        src: 'preview1',
                        thumbnail: 'preview1',
                        file: { name: '1', preview: 'preview1' },
                    },
                ]);
            });
    });
});

it('должен загрузить картинки и поменять стейт (не пришел урл аплоадера)', () => {
    const files = [
        { name: '1', preview: 'preview1' },
    ];
    const gateApiPromise = Promise.reject();
    getResource.mockImplementation(() => gateApiPromise);
    const component = shallow(
        <Provider store={ mockStore({}) }>
            <VinReportCommentForm/>
        </Provider>,
    ).dive().dive();
    component.find('PhotoAddButton').simulate('drop', {}, files);
    return gateApiPromise.then(
        () => {},
        () => {
            expect(upload).toHaveBeenCalledTimes(0);
            expect(component.state().images).toEqual([
                {
                    isUploading: false,
                    needUpload: false,
                    isUploadRejected: true,
                    key: '1',
                    src: 'preview1',
                    thumbnail: 'preview1',
                    file: { name: '1', preview: 'preview1' },
                },
            ]);
        });
});

it('должен удалить фото', () => {
    const images = [
        { thumbnail: 'thumbnail1' },
    ];
    const component = shallow(
        <Provider store={ mockStore({}) }>
            <VinReportCommentForm
                images={ images }
            />
        </Provider>,
    ).dive().dive();
    expect(component.state().images).toEqual([ { thumbnail: 'thumbnail1' } ]);

    const gallery = component
        .find('VinReportCommentGallery')
        .dive()
        .find('Connect(ImageGalleryDesktop)')
        .dive()
        .dive();

    gallery.find('.VinReportCommentGallery__thumbDelete IconSvg').simulate('click', { stopPropagation: () => {} });
    expect(component.state().images).toEqual([]);
});

it('должен перезагрузить фото', () => {
    const images = [
        { thumbnail: 'thumbnail1', isUploadRejected: true },
    ];
    const gateApiPromise = Promise.resolve({ upload_url: 'upload url' });
    getResource.mockImplementation(() => gateApiPromise);
    const pr = Promise.resolve({ photo: { mds_photo_info: { foo: 'bar' }, sizes: { '1200x900': 'url1' } } });
    upload.mockImplementation(() => pr);
    const component = shallow(
        <Provider store={ mockStore({}) }>
            <VinReportCommentForm
                images={ images }
            />
        </Provider>,
    ).dive().dive();
    const gallery = component
        .find('VinReportCommentGallery')
        .dive()
        .find('Connect(ImageGalleryDesktop)')
        .dive()
        .dive();

    gallery.find('.VinReportCommentGallery__thumbReload IconSvg').simulate('click', { stopPropagation: () => {} });
    return gateApiPromise.then(() => {
        expect(upload).toHaveBeenCalledTimes(1);
    });
});

it('не должен показать ошибку, если ввели не слишком много букв', () => {
    const component = shallow(
        <Provider store={ mockStore({}) }>
            <VinReportCommentForm
                input="1234"
            />
        </Provider>,
    ).dive().dive();

    expect(component.find('.VinReportCommentForm__error').isEmptyRender()).toBe(true);
});

it('должен показать ошибку, если ввели слишком много букв', () => {
    let input = '';
    for (var i = 0; i <= 200; i++) {
        input += '0987654321';
    }
    const component = shallow(
        <Provider store={ mockStore({}) }>
            <VinReportCommentForm
                input={ input }
            />
        </Provider>,
    ).dive().dive();

    expect(component.find('.VinReportCommentForm__error').isEmptyRender()).toBe(false);
});
