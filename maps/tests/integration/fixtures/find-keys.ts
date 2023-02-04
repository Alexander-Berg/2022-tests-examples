export const findKeysByCountXML = `<?xml version="1.0"?>
<keylistsize count="1"/>`;

export const wrongFindKeysByCountXML = `<?xml version="1.0"?>
<keylistsize count="odin"/>`;

export const keyHistoryXML = `<?xml version="1.0"?>
<keylog>
    <link type="text/css" id="dark-mode" rel="stylesheet" href=""/>
    <style type="text/css" id="dark-mode-custom-style"/>
    <keyrecord reason="2" description="Something is there!" aid="1120000000190553" modified="1579468842"/>
    <keyrecord reason="0" description="Here will be something " aid="1120000000190553" modified="1579468870"/>
</keylog>`;

export const emptyKeyHistoryXML = `<?xml version="1.0"?>
<keylog>
</keylog>`;

export const wrongKeyHistoryXML = `<?xml version="1.0"?>
<keylog>
    <link type="text/css" id="dark-mode" rel="stylesheet" href=""/>
    <style type="text/css" id="dark-mode-custom-style"/>
    <keyrecord reason="2" description="Something is there!"/>
    <keyrecord reason="0" description="Here will be something"/>
</keylog>`;

export const keyInfoXML = `<?xml version="1.0"?>
<keylist uid="47654665">
    <keystate>
        <key>AGbdN0kBAAAAznK4cAIA7g-V6VCHWmUtH1sONLeu09A4gLYAAAAAAAAAAAC73G8iwP62CESm2e4fdmdnX94tJg==</key>
        <valid>true</valid>
        <broken>false</broken>
        <issued>1228408726</issued>
        <uri>somehost.ru</uri>
        <note>1234</note>
        <stoplist>
            <stop>
                <blocked>0</blocked>
                <description>Custom</description>
                <modified>2020-01-31 14:42:51.613779</modified>
            </stop>
        </stoplist>
        <restrictions>
            <allowCopyrightDisabling>1</allowCopyrightDisabling>
            <allowTraffic>0</allowTraffic>
            <maxSize>1</maxSize>
        </restrictions>
    </keystate>
</keylist>`;

export const keyInfoWithoutStoplistXML = `<?xml version="1.0"?>
<keylist uid="47654665">
    <keystate>
        <key>AGbdN0kBAAAAznK4cAIA7g-V6VCHWmUtH1sONLeu09A4gLYAAAAAAAAAAAC73G8iwP62CESm2e4fdmdnX94tJg==</key>
        <valid>true</valid>
        <broken>false</broken>
        <issued>1228408726</issued>
        <uri>somehost.ru</uri>
        <note>1234</note>
        <restrictions>
            <allowCopyrightDisabling>1</allowCopyrightDisabling>
            <allowTraffic>0</allowTraffic>
            <maxSize>1</maxSize>
        </restrictions>
    </keystate>
</keylist>`;

export const keyInfoRequiredParamsXML = `<?xml version="1.0"?>
<keylist uid="47654665">
    <keystate>
        <key>AGbdN0kBAAAAznK4cAIA7g-V6VCHWmUtH1sONLeu09A4gLYAAAAAAAAAAAC73G8iwP62CESm2e4fdmdnX94tJg==</key>
        <valid>true</valid>
        <broken>false</broken>
        <issued>1228408726</issued>
        <uri>somehost.ru</uri>
        <note>1234</note>
    </keystate>
</keylist>`;

export const wrongKeyInfoXML = `<?xml version="1.0"?>
<keylist userid="47654665">
    <keystate>
        <klyuch>AGbdN0kBAAAAznK4cAIA7g-V6VCHWmUtH1sONLeu09A4gLYAAAAAAAAAAAC73G8iwP62CESm2e4fdmdnX94tJg==</klyuch>
        <valid>true</valid>
        <slomlen>false</slomlen>
        <issued>1228408726</issued>
        <uri value="somehost.ru></uri>
        <zametka>1234</zametka>
        <stoplist>
            <stop>
                <blocked>0</blocked>
                <description>Custom</description>
                <modified>2020-01-31 14:42:51.613779</modified>
            </stop>
        </stoplist>
        <restrictions>
            <allowCopyrightDisabling>1</allowCopyrightDisabling>
            <allowTraffic>0</allowTraffic>
            <maxSize>1</maxSize>
        </restrictions>
    </keystate>
</keylist>`;

export const emptyKeyInfoXML = `<?xml version="1.0"?>
<keylist uid="47654665">
</keylist>`;

export const findKeysXML = `<?xml version="1.0"?>
<keylist>
    <keystate>
        <key>AGbdN0kBAAAAznK4cAIA7g-V6VCHWmUtH1sONLeu09A4gLYAAAAAAAAAAAC73G8iwP62CESm2e4fdmdnX94tJg==</key>
        <valid>true</valid>
        <broken>false</broken>
        <issued>1228408726</issued>
        <uri>somehost.ru</uri>
        <uid>47654665</uid>
        <note>1234</note>
        <stoplist>
            <stop>
                <blocked>0</blocked>
                <description>Custom</description>
                <modified>2020-01-31 14:42:51.613779</modified>
            </stop>
        </stoplist>
        <restrictions>
            <allowCopyrightDisabling>1</allowCopyrightDisabling>
            <allowTraffic>0</allowTraffic>
            <maxSize>1</maxSize>
        </restrictions>
    </keystate>
</keylist>`;

export const notFoundKeysXML = `<?xml version="1.0"?>
<keylist>
</keylist>`;

export const restrictionsListXML = `<?xml version="1.0"?>
<restrictions>
    <restriction>allowCopyrightDisabling</restriction>
    <restriction>allowLogoDisabling</restriction>
    <restriction>allowTraffic</restriction>
    <restriction>allowTrafficAdvertDisabling</restriction>
    <restriction>allowTrafficForecast</restriction>
    <restriction>checkReferer</restriction>
    <restriction>maxDrawArea</restriction>
    <restriction>maxPoints</restriction>
    <restriction>maxPolylines</restriction>
    <restriction>maxPolylinesCoords</restriction>
    <restriction>maxSize</restriction>
</restrictions>`;

export const correctSettedKeyXML = `<?xml version="1.0"?>
<ok/>`;

export const wrongSettedKeyXML = `<?xml version="1.0"?>
<keystate>
    <error>Key is broken.</error>
</keystate>`;

export const restrictionsListExpectedResult = [
    'allowCopyrightDisabling',
    'allowLogoDisabling',
    'allowTraffic',
    'allowTrafficAdvertDisabling',
    'allowTrafficForecast',
    'checkReferer',
    'maxDrawArea',
    'maxPoints',
    'maxPolylines',
    'maxPolylinesCoords',
    'maxSize'
];

export const wrongSettedKeyExpectedResult = {
    error: 'Key is broken.'
};

export const findKeysByCountExpectedResult = {
    count: 1
};

export const findKeysExpectedResult = [{
    key: 'AGbdN0kBAAAAznK4cAIA7g-V6VCHWmUtH1sONLeu09A4gLYAAAAAAAAAAAC73G8iwP62CESm2e4fdmdnX94tJg==',
    valid: true,
    uri: 'somehost.ru',
    issued: '1228408726'
}];

export const keyInfoExpectedResult = {
    key: 'AGbdN0kBAAAAznK4cAIA7g-V6VCHWmUtH1sONLeu09A4gLYAAAAAAAAAAAC73G8iwP62CESm2e4fdmdnX94tJg==',
    valid: true,
    broken: true,
    note: '1234',
    uri: 'somehost.ru',
    uid: '47654665',
    issued: '1228408726',
    stoplist: [
        {
            stop: {
                blocked: false,
                description: 'Custom',
                modified: new Date('2020-01-31 14:42:51.613779').toISOString()
            }
        }
    ],
    restrictions: {
        allowCopyrightDisabling: '1',
        allowTraffic: '0',
        maxSize: '1'
    },
    login: 'bankwerker'
};

export const keyHistoryExpectedResult = [
    {
        reason: '2',
        description: 'Something is there!',
        aid: '1120000000190553',
        modified: '1579468842',
        login: 'user@yandex-team.ru'
    },
    {
        reason: '0',
        description: 'Here will be something ',
        aid: '1120000000190553',
        modified: '1579468870',
        login: 'user@yandex-team.ru'
    }
];
