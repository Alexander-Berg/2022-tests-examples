import breadcrumbsPublicApi from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';
import video from 'auto-core/react/dataDomain/video/mocks/video';

import videoSeo from './video';

it('генерирует сео при пустом фильтре', () => {
    expect(videoSeo({ breadcrumbsPublicApi, video: video.video }, {})).toMatchSnapshot();
});

it('генерирует сео при выбранной марке', () => {
    const mmm = { mark: 'FORD' };
    expect(videoSeo({ breadcrumbsPublicApi, video: {
        ...video.video,
        searchParams: {
            catalog_filter: [ mmm ],
        },
    } }, mmm)).toMatchSnapshot();
});

it('генерирует сео при выбранной марке и модели', () => {
    const mmm = { mark: 'FORD', model: 'ECOSPORT' };
    expect(videoSeo({ breadcrumbsPublicApi, video: {
        ...video.video,
        searchParams: {
            catalog_filter: [ mmm ],
        },
    } }, mmm)).toMatchSnapshot();
});

it('генерирует сео при выбранной марке, модели и неймплейте', () => {
    const mmm = { mark: 'FORD', model: 'C_MAX', nameplate_name: 'grand' };
    expect(videoSeo({ breadcrumbsPublicApi, video: {
        ...video.video,
        searchParams: {
            catalog_filter: [ mmm ],
        },
    } }, mmm)).toMatchSnapshot();
});

it('генерирует сео при выбранной марке, модели и поколении', () => {
    const mmm = { mark: 'FORD', model: 'ECOSPORT' };
    expect(videoSeo({ breadcrumbsPublicApi, video: {
        ...video.video,
        searchParams: {
            catalog_filter: [ { ...mmm, generation: '20104320' } ],
        },
    } }, { ...mmm, super_gen: '20104320' })).toMatchSnapshot();
});
