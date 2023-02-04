const path = require('path');
const viewerPackageJSON = path.join(__dirname, '..', '..', 'toloka-test-viewer', 'package.json');
const viewerVersion = require(viewerPackageJSON).version;
const viewerUrl = `https://yastatic.net/s3/front-maps-static/toloka-test-viewer/${viewerVersion}/index.html`;

function buildViewerUrl(test) {
    const url = new URL(viewerUrl);
    const zoom = test.result.metaInfo.zoom;
    const center = test.result.metaInfo.center.replace(', ', ',');
    const layer = test.result.metaInfo.layer;
    const night = test.result.metaInfo.night

    url.searchParams.append('z', zoom);
    url.searchParams.append('ll', center);

    if (layer) {
        url.searchParams.append('type', layer);
    }

    if (night) {
        url.searchParams.append('mode', 'night');
    }

    return url.toString();
}

module.exports = buildViewerUrl;