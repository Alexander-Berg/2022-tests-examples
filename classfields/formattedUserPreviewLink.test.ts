import formattedUserPreviewLink from './formattedUserPreviewLink';

const links = [
    'https://www.youtube.com',
    'http://www.youtube.com',
    '//www.youtube.com',
    'www.youtube.com',
];

it('ссылки приводятся к нужному виду', () => {
    links.forEach((link) => {
        expect(formattedUserPreviewLink(link)).toBe('www.youtube.com');
    });
});
