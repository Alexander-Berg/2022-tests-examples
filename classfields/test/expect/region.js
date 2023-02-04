module.exports = {
    2 : {
        id : 2,
        name : 'Санкт-Петербург',
        country : 225,
        linguistics : {
            nominative : 'Санкт-Петербург',
            genitive : 'Санкт-Петербурга',
            dative : 'Санкт-Петербургу',
            prepositional : 'Санкт-Петербурге',
            preposition : 'в',
            locative : '',
            directional : '',
            ablative : '',
            accusative : 'Санкт-Петербург',
            instrumental : 'Санкт-Петербургом'
        },
        data : {
            /* jshint maxlen: 1000 */
            id : 2,
            parent : 10174,
            chief_region : 0,
            main : true,
            name : 'Санкт-Петербург',
            timezone : 'Europe/Moscow',
            type : 6,
            position : 72832295,
            phone_code : '812',
            zip_code : '',
            lat : 59.938531,
            lon : 30.313497,
            spn_lat : 0.629672,
            spn_lon : 1.369409,
            zoom : 11
        }
    },
    213 : {
        id : 213,
        name : 'Москва',
        country : 225,
        linguistics : {
            nominative : 'Москва',
            genitive : 'Москвы',
            dative : 'Москве',
            prepositional : 'Москве',
            preposition : 'в',
            locative : '',
            directional : '',
            ablative : '',
            accusative : 'Москву',
            instrumental : 'Москвой'
        },
        data : {
            id : 213,
            parent : 1,
            chief_region : 0,
            main : true,
            name : 'Москва',
            timezone : 'Europe/Moscow',
            type : 6,
            position : 1000000000,
            phone_code : '495 499',
            zip_code : '',
            lat : 55.755768,
            lon : 37.617671,
            spn_lat : 0.520073,
            spn_lon : 0.782226,
            zoom : 10
        }
    },
    timezone : {
        51 : {
            abbr : 'EET',
            dst : Boolean,  // не учитываем зимнее / летнее время в тестах
            name : 'Europe/Samara',
            offset : 14400
        },
        213 : {
            abbr : 'MSK',
            dst : Boolean, // не учитываем зимнее / летнее время в тестах
            name : 'Europe/Moscow',
            offset : 10800
        }
    }
};
