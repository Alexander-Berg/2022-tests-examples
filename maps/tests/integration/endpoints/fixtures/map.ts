export const mapFixture = {
    get properties() {
        return {
            name: 'map name',
            description: 'map description',
            access: 'private'
        };
    },

    get options() {
        return {};
    },

    get state() {
        return {
            type: 'yandex#map',
            size: [2, 3],
            zoom: 10,
            center: [37, 55],
            trafficControlEnabled: false
        };
    }
};

export const mapFixtureFull = {
    ...mapFixture,
    time_created: '2016-07-14 01:00:00',
    time_updated: '2016-07-14 01:00:00',
    revision: '1',
    deleted: false
};
