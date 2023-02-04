import getFilesFromField from './getFilesFromField';

it('должен сформировать массив файлов', () => {
    expect(getFilesFromField({
        name: '',
        value: {
            'new': '111.jpeg,222.jpeg,333.jpeg',
            origin: '444.jpeg',
            'delete': '222.jpeg',
        },
    })).toEqual([
        { name: '111.jpeg', title: '111.jpeg' },
        { name: '333.jpeg', title: '333.jpeg' },
        { name: '444.jpeg', title: '444.jpeg' },
    ]);
});
