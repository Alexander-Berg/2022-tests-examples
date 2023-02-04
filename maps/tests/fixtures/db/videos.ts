import {Table, VideoStatus, VideoTranscoderStatus} from 'app/types/consts';
import {Schema} from 'app/types/db/videos';

const rows: Schema[] = [
    {
        id: '11a1e679-e3be-4aad-a3bb-591eada9cc44',
        data: {
            thumbnail: 'https://avatars.mds.yandex.net/get-vh/2114733/2a00000171122a688d4353ba9ff930eaf8c0/orig',
            player_url: 'https://frontend.vh.yandex.ru/player/vendO5CcEom8',
            stream_url: 'https://strm.yandex.ru/vh-maps-converted/vod-content/b811f356e693a83e0f32188cbbf29168.m3u8',
            duration_ms: 6037,
            screenshots: [
                'https://avatars.mds.yandex.net/get-vh/1454382/2a00000171122a88021c9591550cac471170/orig',
                'https://avatars.mds.yandex.net/get-vh/2415574/2a00000171122a881267231614df4a3e0aa9/orig',
                'https://avatars.mds.yandex.net/get-vh/1534896/2a00000171122a881fb7358f228456cd482b/orig'
            ],
            transcoder_status_str: VideoTranscoderStatus.ETS_DONE
        },
        status: VideoStatus.READY
    },
    {
        id: 'c2bc6a4d-d6bd-4f37-8a08-abb7a63a91e1',
        data: {
            thumbnail: 'https://avatars.mds.yandex.net/get-vh/2383281/2a00000171122afae63090bd2e4113bcfde3/orig',
            player_url: 'https://frontend.vh.yandex.ru/player/vl4XCH0S54r0',
            stream_url: 'https://strm.yandex.ru/vh-maps-converted/vod-content/86c2ca9a2d4580906bac21fe96dbce0e.m3u8',
            duration_ms: 6037,
            screenshots: [
                'https://avatars.mds.yandex.net/get-vh/1535404/2a00000171122b10fbb0bb545e85f4abb0bc/orig',
                'https://avatars.mds.yandex.net/get-vh/2364959/2a00000171122b113b6f52064c91cd30dbfc/orig',
                'https://avatars.mds.yandex.net/get-vh/2351157/2a00000171122b111915452dac05f525eab6/orig'
            ],
            transcoder_status_str: VideoTranscoderStatus.ETS_DONE
        },
        status: VideoStatus.READY
    },
    {
        id: 'ad5937ae-52ef-46bf-b688-936faa13598e',
        data: {
            thumbnail: 'https://avatars.mds.yandex.net/get-vh/1604051/2a000001711200e0d8af2607f3177763f260/orig',
            player_url: 'https://frontend.vh.yandex.ru/player/vNz4pfDkxmUE',
            stream_url: 'https://strm.yandex.ru/vh-maps-converted/vod-content/971cb3cbfffc97c6b7adaa3204ee2836.m3u8',
            duration_ms: 6037,
            screenshots: [
                'https://avatars.mds.yandex.net/get-vh/2415574/2a000001711200fd64c7ca58047de045f125/orig',
                'https://avatars.mds.yandex.net/get-vh/1078136/2a000001711200fd5ac0705393aa77cda319/orig',
                'https://avatars.mds.yandex.net/get-vh/2114716/2a000001711200fd633d834910801d614ac7/orig'
            ],
            transcoder_status_str: VideoTranscoderStatus.ETS_DONE
        },
        status: VideoStatus.MODERATING
    },
    {
        id: '25290259-8653-497a-8de2-5bf77bae8ccc',
        data: {
            thumbnail: 'https://avatars.mds.yandex.net/get-vh/1616035/2a000001711200ced04e3089a00adc25fcd8/orig',
            player_url: 'https://frontend.vh.yandex.ru/player/vaUd86kqtr0I',
            stream_url: 'https://strm.yandex.ru/vh-maps-converted/vod-content/4572bd291d8c360c1f9f876aa7f17288.m3u8',
            duration_ms: 6037,
            screenshots: [
                'https://avatars.mds.yandex.net/get-vh/2390301/2a000001711200dc27a458392b3fd81de6dd/orig',
                'https://avatars.mds.yandex.net/get-vh/2415574/2a000001711200dc4b92111273deeea1faab/orig',
                'https://avatars.mds.yandex.net/get-vh/2390301/2a000001711200dc4a28507223a53bf7e305/orig'
            ],
            transcoder_status_str: VideoTranscoderStatus.ETS_DONE
        },
        status: VideoStatus.READY
    }
];

const videos = {
    table: Table.VIDEOS,
    rows: rows.map((row) => ({
        ...row,
        data: JSON.stringify(row.data)
    }))
};
export {videos};
