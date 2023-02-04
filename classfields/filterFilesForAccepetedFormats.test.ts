import filterFilesForAccepetedFormats from './filterFilesForAccepetedFormats';

const FILE_1 = {
    type: 'image/jpeg',
};

const FILE_2 = {
    type: 'image/png',
};

const FILE_3 = {
    type: 'application/pdf',
};

const FILES = [ FILE_1, FILE_2, FILE_3 ];

it('filterFilesForAccepetedFormats должен оставить только картинки', () => {
    const result = filterFilesForAccepetedFormats(FILES, 'image/*');
    expect(result).toEqual([ FILE_1, FILE_2 ]);
});
