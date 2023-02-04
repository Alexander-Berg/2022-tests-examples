export const foundOffendersByUriXML = `<?xml version="1.0"?>
<stoplist>
    <stop>
        <blocked>0</blocked>
        <description>haha</description>
        <modified>2020-01-24T17:18:48.834Z</modified>
        <uri>alyniekka.com</uri>
    </stop>
    <stop>
        <blocked>0</blocked>
        <description>—</description>
        <modified>2020-01-18T16:49:50.095Z</modified>
        <uri>sadsfdsad</uri>
    </stop>
</stoplist>`;

export const foundOffendersByAppXML = `<?xml version="1.0"?>
<stoplist>
    <stop>
        <blocked>0</blocked>
        <description>—</description>
        <modified>2020-01-16T16:34:00.663Z</modified>
        <appid>asdfsdbgdfsdsfbgdfsdsa</appid>
    </stop>
    <stop>
        <blocked>1</blocked>
        <description>test</description>
        <modified>2015-09-02T13:42:47.317Z</modified>
        <appid>test3</appid>
    </stop>
</stoplist>`;

export const foundOffendersByIpXML = `<?xml version="1.0"?>
<stoplist>
    <stop>
        <blocked>0</blocked>
        <description>Отдуши</description>
        <modified>2020-01-24T17:48:02.349Z</modified>
        <subnet>1.1.1.1</subnet>
    </stop>
    <stop>
        <blocked>0</blocked>
        <description>something should be happend out there</description>
        <modified>2020-01-20T08:28:31.972Z</modified>
        <subnet>100.100.100.101</subnet>
    </stop>
</stoplist>`;

export const notFoundOffendersByIpXML = `<?xml version="1.0"?>
<stoplist>
</stoplist>`;

export const offendersHistoryXML = `<?xml version="1.0"?>
<changelog>
    <entry>
        <blocked>0</blocked>
        <reason>haha</reason>
        <user>728119531574847900</user>
        <time>2020-01-24T17:18:48.834Z</time>
    </entry>
    <entry>
        <blocked>1</blocked>
        <reason>—</reason>
        <user>4475619391450774500</user>
        <time>2019-05-22T16:13:13.776Z</time>
    </entry>
</changelog>`;

export const noOffendersHistoryXML = `<?xml version="1.0"?>
<changelog>
</changelog>`;

export const offendersHistoryExpectedResult = [{
    blocked: false,
    reason: 'haha',
    time: '2020-01-24T17:18:48.834Z',
    user: '728119531574847900',
    login: 'user1@yandex-team.ru'
}, {
    blocked: true,
    reason: '—',
    time: '2019-05-22T16:13:13.776Z',
    user: '4475619391450774500',
    login: 'user2@yandex-team.ru'
}];

export const foundOffendersByUriExpectedResult = [{
    blocked: false,
    description: 'haha',
    modified: '2020-01-24T17:18:48.834Z',
    uri: 'alyniekka.com'
}, {
    blocked: false,
    description: '—',
    modified: '2020-01-18T16:49:50.095Z',
    uri: 'sadsfdsad'
}];

export const foundOffendersByAppExpectedResult = [{
    blocked: false,
    description: '—',
    modified: '2020-01-16T16:34:00.663Z',
    appid: 'asdfsdbgdfsdsfbgdfsdsa'
}, {
    blocked: true,
    description: 'test',
    modified: '2015-09-02T13:42:47.317Z',
    appid: 'test3'
}];

export const foundOffendersByIpExpectedResult = [{
    blocked: false,
    description: 'Отдуши',
    modified: '2020-01-24T17:48:02.349Z',
    subnet: '1.1.1.1'
}, {
    blocked: false,
    description: 'something should be happend out there',
    modified: '2020-01-20T08:28:31.972Z',
    subnet: '100.100.100.101'
}];
