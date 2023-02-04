import getVideoDataForIframe from './getVideoDataForIframe';

it('должен вернуть урл для iframe: Youtube', () => {
    const initialUrl = 'www.youtube.com/watch?v=Dexq34fr5Wo&t=1s';
    const videoData = getVideoDataForIframe(initialUrl);

    expect(videoData?.platform).toEqual('youtube');
    expect(videoData?.url.toString()).toEqual('https://www.youtube.com/embed/Dexq34fr5Wo&t=1s');
});

it('должен вернуть урл для iframe: Vimeo', () => {
    const initialUrl = 'https://vimeo.com/185775778';
    const videoData = getVideoDataForIframe(initialUrl);

    expect(videoData?.platform).toEqual('vimeo');
    expect(videoData?.url.toString()).toEqual('https://player.vimeo.com/video/185775778');
});

it('должен вернуть урл для iframe: Coub', () => {
    const initialUrl = 'coub.com/embed/2omwr1?muted=false&autostart=true&originalSize=false&startWithHD=true';
    const videoData = getVideoDataForIframe(initialUrl);

    expect(videoData?.platform).toEqual('coub');
    expect(videoData?.url.toString()).toEqual('https://coub.com/embed/2omwr1?muted=false&autostart=true&originalSize=false&startWithHD=true');
});
