const { CLEANUP_LONGITUDE_MIN, CLEANUP_LONGITUDE_MAX, CLEANUP_LATITUDE_STEP } = require('./constants'),
    sessionNumber = require('./get-session-counter')(),
    cleanup = require('./cleanup'),
    isFast = !!process.env.FAST;

function cleanUpAll() {
    if(isFast) {
        return;
    }
    const lat = CLEANUP_LATITUDE_STEP * sessionNumber * -1,
        coords = [
            [CLEANUP_LONGITUDE_MIN, lat],
            [CLEANUP_LONGITUDE_MAX, lat],
            [CLEANUP_LONGITUDE_MAX, lat * 2],
            [CLEANUP_LONGITUDE_MIN, lat * 2]
        ];

    return cleanup(coords);
}

cleanUpAll().catch((e) => console.log(e));
