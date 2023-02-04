import { isValidYoutubeVideoUrl } from 'realty-core/app/lib/youtube';

describe('isValidYoutubeVideoUrl', () => {
    test.each([
        [ 'http://www.youtube.com/watch?v=iwGFalTRHDA&feature=related' ],
        [ 'http://youtu.be/iwGFalTRHDA' ],
        [ 'http://www.youtube.com/embed/watch?feature=player_embedded&v=iwGFalTRHDA' ],
        [ 'http://www.youtube.com/watch?v=iwGFalTRHDA' ],
        [ 'http://youtu.be/t-ZRX8984sc' ],
        [ 'https://www.youtube.com/watch?v=iwGFalTRHDA&list=PLDfKAXSi6kUZnATwAUfN6tg1dULU-7XcD' ]
    ])('should be valid for %s', url => {
        expect(isValidYoutubeVideoUrl(url)).toBe(true);
    });

    test.each([
        [ 'https://www.youtube.com/iwGFalTRHDA' ],
        [ 'https://www.youtube.com/?f=iwGFalTRHDA' ],
        [ 'https://www.youtuber.com/iwGFalTRHDA' ],
        [ 'https://youtuber.com/iwGFalTRHDA' ],
        [ 'https://youtube.ru/iwGFalTRHDA' ],
        [ 'http://www.youtuber.com/watch?v=iwGFalTRHDA' ],
        [ 'http://www.youtuber.com/watch?v=http://youtu.be/t-ZRX8984sc' ]
    ])('should be invalid for %s', url => {
        expect(isValidYoutubeVideoUrl(url)).toBe(false);
    });
});
