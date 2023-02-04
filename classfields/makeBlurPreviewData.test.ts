import makeBlurPreviewData from './makeBlurPreviewData';

it('правильно формирует превью для первой версии', () => {
    const result = makeBlurPreviewData({
        version: 1,
        data: new Uint8Array(42),
        width: 4,
        height: 3,
    });

    expect(result).toMatchSnapshot();
});

it('правильно формирует превью для второй версии', () => {
    const result = makeBlurPreviewData({
        version: 2,
        data: new Uint8Array(42),
        width: 4,
        height: 3,
    });

    expect(result).toMatchSnapshot();
});
