/**
 * User: agoryunova
 * Date: 20.08.14
 * Time: 20:24
 */
var regionDataSource = function(ymaps, myMap, that) {

that.russia = [
        {
            "type": "Feature",
            "id": 0,
            "geometry": {
                "type": "Point",
                "coordinates": [55.753676,37.619899]
            },
            "properties": {
                "balloonContent": "Москва",
                "clusterCaption": "Москва",
                "hintContent": "Москва"
            }
        },
        {
            "type": "Feature",
            "id": 1,
            "geometry": {
                "type": "Point",
                "coordinates": [59.939095,30.315868]
            },
            "properties": {
                "balloonContent": "Санкт-Петербург",
                "clusterCaption": "Санкт-Петербург",
                "hintContent": "Санкт-Петербург"
            }
        },
        {
            "type": "Feature",
            "id": 2,
            "geometry": {
                "type": "Point",
                "coordinates": [51.661535,39.200287]
            },
            "properties": {
                "balloonContent": "Воронеж",
                "clusterCaption": "Воронеж",
                "hintContent": "Воронеж"
            }
        },
        {
            "type": "Feature",
            "id": 3,
            "geometry": {
                "type": "Point",
                "coordinates": [56.838607,60.605514]
            },
            "properties": {
                "balloonContent": "Екатеринбург",
                "clusterCaption": "Екатеринбург",
                "hintContent": "Екатеринбург"
            }
        },
        {
            "type": "Feature",
            "id": 4,
            "geometry": {
                "type": "Point",
                "coordinates": [43.116391,131.882421]
            },
            "properties": {
                "balloonContent": "Владивосток",
                "clusterCaption": "Владивосток",
                "hintContent": "Владивосток"
            }
        },
        {
            "type": "Feature",
            "id": 5,
            "geometry": {
                "type": "Point",
                "coordinates": [56.010569,92.852545]
            },
            "properties": {
                "balloonContent": "Красноярск",
                "clusterCaption": "Красноярск",
                "hintContent": "Красноярск"
            }
        },
        {
            "type": "Feature",
            "id": 6,
            "geometry": {
                "type": "Point",
                "coordinates": [53.036996,158.655954]
            },
            "properties": {
                "balloonContent": "Петропавловск-Камчатский",
                "clusterCaption": "Петропавловск-Камчатский",
                "hintContent": "Петропавловск-Камчатский"
            }
        },
        {
            "type": "Feature",
            "id": 7,
            "geometry": {
                "type": "Point",
                "coordinates": [68.969563,33.07454]
            },
            "properties": {
                "balloonContent": "Мурманск",
                "clusterCaption": "Мурманск",
                "hintContent": "Мурманск"
            }
        }
    ];

that.ukraine = [
        {
            "type": "Feature",
            "id": 0,
            "geometry": {
                "type": "Point",
                "coordinates": [50.450097,30.523397]
            },
            "properties": {
                "balloonContent": "Киев",
                "clusterCaption": "Киев",
                "hintContent": "Киев"
            }
        },
        {
            "type": "Feature",
            "id": 1,
            "geometry": {
                "type": "Point",
                "coordinates": [49.993499,36.230376]
            },
            "properties": {
                "balloonContent": "Харьков",
                "clusterCaption": "Харьков",
                "hintContent": "Харьков"
            }
        },
        {
            "type": "Feature",
            "id": 2,
            "geometry": {
                "type": "Point",
                "coordinates": [46.635413,32.616867]
            },
            "properties": {
                "balloonContent": "Херсон",
                "clusterCaption": "Херсон",
                "hintContent": "Херсон"
            }
        },
        {
            "type": "Feature",
            "id": 3,
            "geometry": {
                "type": "Point",
                "coordinates": [49.839678,24.029709]
            },
            "properties": {
                "balloonContent": "Львов",
                "clusterCaption": "Львов",
                "hintContent": "Львов"
            }
        },
        {
            "type": "Feature",
            "id": 4,
            "geometry": {
                "type": "Point",
                "coordinates": [48.464717,35.046181]
            },
            "properties": {
                "balloonContent": "Днепропетровск",
                "clusterCaption": "Днепропетровск",
                "hintContent": "Днепропетровск"
            }
        }
    ];

that.turkey = [
        {
            "type": "Feature",
            "id": 0,
            "geometry": {
                "type": "Point",
                "coordinates": [39.920874,32.853923]
            },
            "properties": {
                "balloonContent": "Анкара",
                "clusterCaption": "Анкара",
                "hintContent": "Анкара"
            }
        },
        {
            "type": "Feature",
            "id": 1,
            "geometry": {
                "type": "Point",
                "coordinates": [41.008925,28.967111]
            },
            "properties": {
                "balloonContent": "Стамбул",
                "clusterCaption": "Стамбул",
                "hintContent": "Стамбул"
            }
        },
        {
            "type": "Feature",
            "id": 2,
            "geometry": {
                "type": "Point",
                "coordinates": [36.888023,30.703698]
            },
            "properties": {
                "balloonContent": "Анталья",
                "clusterCaption": "Анталья",
                "hintContent": "Анталья"
            }
        }
    ];

that.usa = [
        {
            "type": "Feature",
            "id": 0,
            "geometry": {
                "type": "Point",
                "coordinates": [40.714545,-74.007112]
            },
            "properties": {
                "balloonContent": "Нью-Йорк",
                "clusterCaption": "Нью-Йорк",
                "hintContent": "Нью-Йорк"
            }
        },
        {
            "type": "Feature",
            "id": 1,
            "geometry": {
                "type": "Point",
                "coordinates": [38.890366,-77.031955]
            },
            "properties": {
                "balloonContent": "Вашингтон",
                "clusterCaption": "Вашингтон",
                "hintContent": "Вашингтон"
            }
        },
        {
            "type": "Feature",
            "id": 2,
            "geometry": {
                "type": "Point",
                "coordinates": [47.60357,-122.329449]
            },
            "properties": {
                "balloonContent": "Сиэтл",
                "clusterCaption": "Сиэтл",
                "hintContent": "Сиэтл"
            }
        },
        {
            "type": "Feature",
            "id": 3,
            "geometry": {
                "type": "Point",
                "coordinates": [41.884244,-87.632444]
            },
            "properties": {
                "balloonContent": "Чикаго",
                "clusterCaption": "Чикаго",
                "hintContent": "Чикаго"
            }
        },
        {
            "type": "Feature",
            "id": 4,
            "geometry": {
                "type": "Point",
                "coordinates": [29.760448,-95.369777]
            },
            "properties": {
                "balloonContent": "Хьюстон",
                "clusterCaption": "Хьюстон",
                "hintContent": "Хьюстон"
            }
        },
        {
            "type": "Feature",
            "id": 5,
            "geometry": {
                "type": "Point",
                "coordinates": [14.875787,-88.069627]
            },
            "properties": {
                "balloonContent": "Лас-Вегас",
                "clusterCaption": "Лас-Вегас",
                "hintContent": "Лас-Вегас"
            }
        },
        {
            "type": "Feature",
            "id": 6,
            "geometry": {
                "type": "Point",
                "coordinates": [34.053485,-118.245313]
            },
            "properties": {
                "balloonContent": "Лос-Анджелес",
                "clusterCaption": "Лос-Анджелес",
                "hintContent": "Лос-Анджелес"
            }
        },
        {
            "type": "Feature",
            "id": 7,
            "geometry": {
                "type": "Point",
                "coordinates": [25.774806,-80.197726]
            },
            "properties": {
                "balloonContent": "Майами",
                "clusterCaption": "Майами",
                "hintContent": "Майами"
            }
        }
    ]
}