var wgs84MercatorSource = function (ymaps, myMap, that) {

    /**
     * geoobjects
     */

    that.geoObjectPlacemark = new ymaps.GeoObject({
        geometry: { type: "Point", coordinates: [55.87047708644851, 37.48430023193276] }, properties: {custom: true}
    }, {id: 'geoObjectPlacemark'});
    //that.geoObjectPlacemark.id = 'geoObjectPlacemark';

    that.geoObjectPolyline = new ymaps.GeoObject({
        geometry: { type: "LineString", coordinates: [
            [55.93821795096054, 37.643322753905174],
            [55.85797267987236, 37.67191162109292],
            [55.93890536025055, 37.70374755859263]
        ] }, properties: {custom: false}
    }, { strokeWidth: 5, id: 'geoObjectPolyline'});

    that.geoObjectCircle = new ymaps.GeoObject({
        geometry: { type: "Circle", coordinates: [55.89836062228889, 37.399499511718034], radius: 2500 }, properties: {custom: true}
    }, {id: 'geoObjectCircle'});

    that.geoObjectPolygon = new ymaps.GeoObject({
        geometry: { type: "Polygon", coordinates: [
            [
                [55.944435274746034, 37.29840545654244],
                [55.86483659083991, 37.29864569770129],
                [55.86483659084632, 37.35220404731071],
                /*[55.90497293823593,37.29967566596304],*/[55.94314094275842, 37.34911414252554],
                [55.944435274746034, 37.29840545654244]
            ]
        ] }, properties: {hintContent: 'some property', custom: false}
    }, {id: 'geoObjectPolygon'});

    that.geoObjectRectangle = new ymaps.GeoObject({
        geometry: { type: "Rectangle", coordinates: [
            [55.85533572601149, 37.54789062499949],
            [55.944837989401236, 37.617890624999305]
        ] }, properties: {custom: true}
    }, {id: 'geoObjectRectangle'});

    /**
     * array of geoobjects (contains 3 sets of each geo object)
     */
    that.geoObjectArray = [
        // 1st set
        new ymaps.GeoObject({
            geometry: { type: "Point", coordinates: [55.77151667968799, 37.47606048583902] }, properties: {custom: false}}, {id: 'geoObjectArrayPlacemark1'}),
        new ymaps.GeoObject({
            geometry: { type: "LineString", coordinates: [
                [55.84252178204625, 37.643322753905174],
                [55.762077634623175, 37.67191162109292],
                [55.84321089577644, 37.703747558592575]
            ] }, properties: {custom: true}
        }, { strokeWidth: 5, id: 'geoObjectArrayPolyline1' }),
        new ymaps.GeoObject({
            geometry: { type: "Circle", coordinates: [55.80256564967056, 37.399499511718034], radius: 2500 }, properties: {custom: false}}, {id: 'geoObjectArrayCircle1'}),
        new ymaps.GeoObject({
            geometry: { type: "Polygon", coordinates: [
                [
                    [55.83948251538787, 37.2929122924799],
                    [55.759667435911304, 37.293152533638796],
                    [55.75966743591769, 37.346710883248214],
                    /*[55.79991287241701,37.29418250190056],*/[55.83818466306252, 37.34362097846305],
                    [55.83948251538787, 37.2929122924799]
                ]
            ] }, properties: {custom: true}}, {id: 'geoObjectArrayPolygon1'}),
        new ymaps.GeoObject({
            geometry: { type: "Rectangle", coordinates: [
                [55.75633659801334, 37.54514404296816],
                [55.84606784561549, 37.615144042967984]
            ] }, properties: {custom: false}}, {id: 'geoObjectArrayRectangle1'}),

        // 2nd set
        new ymaps.GeoObject({
            geometry: { type: "Point", coordinates: [55.669198825209556, 37.46782073974513] }, properties: {custom: true}}, {id: 'geoObjectArrayPlacemark2'}),
        new ymaps.GeoObject({
            geometry: { type: "LineString", coordinates: [
                [55.73109311810124, 37.63508300781142],
                [55.65041767605592, 37.663671874999],
                [55.731784214116445, 37.69550781249869]
            ] }, properties: {custom: false}
        }, { strokeWidth: 5, id: 'geoObjectArrayPolyline2' }),
        new ymaps.GeoObject({
            geometry: { type: "Circle", coordinates: [55.69722734495064, 37.38851318359288], radius: 2500 }, properties: {custom: true}}, {id: 'geoObjectArrayCircle2'}),
        new ymaps.GeoObject({
            geometry: { type: "Polygon", coordinates: [
                [
                    [55.73269472632186, 37.29565887451115],
                    [55.6526597370297, 37.295899115670046],
                    [55.65265973703625, 37.34945746527946],
                    /*[55.69301603400168,37.2969290839318],*/[55.73139329647317, 37.34636756049429],
                    [55.73269472632186, 37.29565887451115]
                ]
            ] }, properties: {custom: false}}, {id: 'geoObjectArrayPolygon2'}),
        new ymaps.GeoObject({
            geometry: { type: "Rectangle", coordinates: [
                [55.64621339785425, 37.53965087890565],
                [55.73619905320758, 37.60965087890544]
            ] }, properties: {hintContent: 'some property', custom: true}}, {id: 'geoObjectArrayRectangle2'}),

        // 3rd set
        new ymaps.GeoObject({
            geometry: { type: "Point", coordinates: [55.57283665890869, 37.47331390380759] }, properties: {custom: false}}, {id: 'geoObjectArrayPlacemark3'}),
        new ymaps.GeoObject({
            geometry: { type: "LineString", coordinates: [
                [55.63022268628381, 37.6405761718739],
                [55.54933812333297, 37.66916503906146],
                [55.630915574554734, 37.701000976561104]
            ] }, properties: {custom: true}
        }, { strokeWidth: 5, id: 'geoObjectArrayPolyline3' }),
        new ymaps.GeoObject({
            geometry: { type: "Circle", coordinates: [55.59315855153306, 37.40224609374908], radius: 2500 }, properties: {custom: false}}, {id: 'geoObjectArrayCircle3'}),
        new ymaps.GeoObject({
            geometry: { type: "Polygon", coordinates: [
                [
                    [55.63493592217409, 37.29565887451115],
                    [55.554699855402895, 37.295899115670046],
                    [55.554699855409254, 37.34945746527946],
                    /*[55.59515751891029,37.2969290839318],*/[55.63363122115564, 37.34636756049429],
                    [55.63493592217409, 37.29565887451115]
                ]
            ] }, properties: {custom: true}}, {id: 'geoObjectArrayPolygon3'}),
        new ymaps.GeoObject({
            geometry: { type: "Rectangle", coordinates: [
                [55.54668017163498, 37.54239746093687],
                [55.63689549131277, 37.612397460936634]
            ] }, properties: {custom: false}}, {id: 'geoObjectArrayRectangle3'})
    ];

    /**
     * collection of geoobjects (contains 3 sets of each geo object)
     */
    that.geoObjectCollection = new ymaps.GeoObjectCollection({}, {
        strokeColor: '#00FF00',
        preset: "twirl#greenIcon"
    });
    that.geoObjectCollection.add(
            new ymaps.GeoObject({
                geometry: { type: "Point", coordinates: [55.4746764188949, 37.48430023193255] }, properties: {custom: true}
            }, {id: 'geoObjectCollectionPlacemark1'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "LineString", coordinates: [
                    [55.5337642009911, 37.643322753905075],
                    [55.45267989358332, 37.67191162109268],
                    [55.53445880116444, 37.70374755859215]
                ] }, properties: {custom: false}
            }, { strokeWidth: 5, id: 'geoObjectCollectionPolyline1' })
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Circle", coordinates: [55.4981676617161, 37.40773925781134], radius: 2500 }, properties: {custom: true}
            }, {id: 'geoObjectCollectionCircle1'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Polygon", coordinates: [
                    [
                        [55.5369315371614, 37.2929122924799],
                        [55.456494117516456, 37.293152533638796],
                        [55.45649411752285, 37.346710883248214],
                        /*[55.49705328618504,37.29418250190056],*/[55.535623560479806, 37.34362097846305],
                        [55.5369315371614, 37.2929122924799]
                    ]
                ] }, properties: {custom: false}
            }, {id: 'geoObjectCollectionPolygon1'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Rectangle", coordinates: [
                    [55.44845431854879, 37.54789062499936],
                    [55.53889602607483, 37.61789062499915]
                ] }, properties: {custom: true}
            }, {id: 'geoObjectCollectionRectangle1'})
        );
    that.geoObjectCollection.add(
            new ymaps.GeoObject({
                geometry: { type: "Point", coordinates: [55.3684494685438, 37.476060485838815] }, properties: {custom: false}
            }, {id: 'geoObjectCollectionPlacemark2'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "LineString", coordinates: [
                    [55.42925902664112, 37.637829589842504],
                    [55.347958566023316, 37.6664184570302],
                    [55.429955479355854, 37.698254394529656]
                ] }, properties: {custom: true}
            }, { strokeWidth: 5, id: 'geoObjectCollectionPolyline2' })
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Circle", coordinates: [55.39669399359766, 37.394006347655065], radius: 2500 }, properties: {custom: false}
            }, {id: 'geoObjectCollectionCircle2'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Polygon", coordinates: [
                    [
                        [55.43399650954864, 37.28192596435479],
                        [55.35334785520828, 37.28216620551374],
                        [55.35334785521461, 37.33572455512305],
                        /*[55.394013510398324,37.28319617377542],*/[55.432685096429644, 37.332634650338015],
                        [55.43399650954864, 37.28192596435479]
                    ]
                ] }, properties: {hintContent: 'some property', custom: true}
            }, {id: 'geoObjectCollectionPolygon2'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Rectangle", coordinates: [
                    [55.34528695417901, 37.54239746093679],
                    [55.43596615994939, 37.612397460936606]
                ] }, properties: {custom: false}
            }, {id: 'geoObjectCollectionRectangle2'})
        );
    that.geoObjectCollection.add(
            new ymaps.GeoObject({
                geometry: { type: "Point", coordinates: [55.26350381873909, 37.473313903807444] }, properties: {custom: true}
            }, {id: 'geoObjectCollectionPlacemark3'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "LineString", coordinates: [
                    [55.32447530230717, 37.640576171873725],
                    [55.242958378477425, 37.66916503906131],
                    [55.32517361022816, 37.70100097656082]
                ] }, properties: {custom: false}
            }, { strokeWidth: 5, id: 'geoObjectCollectionPolyline3' })
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Circle", coordinates: [55.288688790838314, 37.39400634765494], radius: 2500 }, properties: {custom: true}
            }, {id: 'geoObjectCollectionCircle3'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Polygon", coordinates: [
                    [
                        [55.33079120248391, 37.27368621826095],
                        [55.2499310149089, 37.273926459419876],
                        [55.24993101491529, 37.327484809029215],
                        /*[55.29070330688941,37.27495642768161],*/[55.32947634805618, 37.32439490424418],
                        [55.33079120248391, 37.27368621826095]
                    ]
                ] }, properties: {custom: false}
            }, {id: 'geoObjectCollectionPolygon3'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Rectangle", coordinates: [
                    [55.24184898203012, 37.53965087890555],
                    [55.33276602162341, 37.60965087890536]
                ] }, properties: {custom: true}
            }, {id: 'geoObjectCollectionRectangle3'})
        );

    /**
     * array of collections of geo objects (contains 3 sets of each geo object)
     */
    that.geoObjectCollectionArray = [
        new ymaps.GeoObjectCollection({}, {
            strokeColor: '#000000',
            preset: "twirl#blackIcon"
        }),
        new ymaps.GeoObjectCollection({}, {
            strokeColor: '#000000',
            preset: "twirl#blackIcon"
        })
    ];

    that.geoObjectCollectionArray[0].add(
            new ymaps.GeoObject({
                geometry: { type: "Point", coordinates: [55.875109653310595, 37.937486267087856] }, properties: {custom: false}
            }, {id: 'geoObjectCollectionArrayPlacemark1'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "LineString", coordinates: [
                    [55.93821794893135, 38.09376220702914],
                    [55.857972677876255, 38.122351074216695],
                    [55.938905358221355, 38.15418701171583]
                ] }, properties: {custom: true}
            }, { strokeWidth: 5, id: 'geoObjectCollectionArrayPolyline1' })
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Circle", coordinates: [55.898360620548104, 37.85817871093515], radius: 2500 }, properties: {custom: false}
            }, {id: 'geoObjectCollectionArrayCircle1'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Polygon", coordinates: [
                    [
                        [55.93518619931843, 37.75433807372846],
                        [55.85556843483601, 37.75457831488774],
                        [55.855568434842276, 37.808136664497056],
                        /*[55.89571440112142,37.75560828314934],*/[55.93389155692764, 37.805046759711814],
                        [55.93518619931843, 37.75433807372846]
                    ]
                ] }, properties: {custom: true}
            }, {id: 'geoObjectCollectionArrayPolygon1'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Rectangle", coordinates: [
                    [55.85533572392072, 37.998330078122756],
                    [55.94483798727207, 38.06833007812283]
                ]}, properties: {custom: false}
            }, {id: 'geoObjectCollectionArrayRectangle1'})
        );
    that.geoObjectCollectionArray[0].add(
            new ymaps.GeoObject({
                geometry: { type: "Point", coordinates: [55.771516678045664, 37.937486267087856] }, properties: {custom: true}
            }, {id: 'geoObjectCollectionArrayPlacemark2'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "LineString", coordinates: [
                    [55.834794017761034, 38.09376220702906],
                    [55.75433382007732, 38.122351074216596],
                    [55.835483269048304, 38.15418701171583]
                ] }, properties: {custom: false}
            }, { strokeWidth: 5, id: 'geoObjectCollectionArrayPolyline2'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Circle", coordinates: [55.79792439148369, 37.863671874997614], radius: 2500 }, properties: {custom: true}
            }, {id: 'geoObjectCollectionArrayCircle2'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Polygon", coordinates: [
                    [
                        [55.83484567703003, 37.75433807372846],
                        [55.75502104322964, 37.754578314887716],
                        [55.75502104323584, 37.808136664497056],
                        /*[55.7952712962455,37.75560828314934],*/[55.83354766927437, 37.805046759711814],
                        [55.83484567703003, 37.75433807372846]
                    ]
                ] }, properties: {custom: false}
            }, {id: 'geoObjectCollectionArrayPolygon2'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Rectangle", coordinates: [
                    [55.75014075351265, 38.001076660154006],
                    [55.83988632334859, 38.071076660154]
                ] }, properties: {custom: true}
            }, {id: 'geoObjectCollectionArrayRectangle2'})
        );
    that.geoObjectCollectionArray[1].add(
            new ymaps.GeoObject({
                geometry: { type: "Point", coordinates: [55.66919882349415, 37.934739685056584] }, properties: {custom: false}
            }, {id: 'geoObjectCollectionArrayPlacemark3'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "LineString", coordinates: [
                    [55.731093116079705, 38.088269042966544],
                    [55.65041767406724, 38.1168579101541],
                    [55.73178421209497, 38.148693847653334]
                ] }, properties: {custom: true}
            }, { strokeWidth: 5, id: 'geoObjectCollectionArrayPolyline3' })
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Circle", coordinates: [55.70653339203807, 37.85543212890383], radius: 2500 }, properties: {hintContent: 'some property', custom: false}
            }, {id: 'geoObjectCollectionArrayCircle3'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Polygon", coordinates: [
                    [
                        [55.738893348934916, 37.75159149169721],
                        [55.65887111716739, 37.751831732856466],
                        [55.658871117173554, 37.805390082465706],
                        /*[55.69922098285009,37.7528617011181],*/[55.73759212662869, 37.80230017768057],
                        [55.738893348934916, 37.75159149169721]
                    ]
                ] }, properties: {custom: true}
            }, {id: 'geoObjectCollectionArrayPolygon3'})
        ).add(
            new ymaps.GeoObject({
                geometry: { type: "Rectangle", coordinates: [
                    [55.64931972419049, 37.99558349609151],
                    [55.73929820766435, 38.065583496091456]
                ] }, properties: {custom: false}
            }, {id: 'geoObjectCollectionArrayRectangle3'})
        );

    // object
    that.objectPlacemark = {
        type: "Feature",
        geometry: {
            type: "Point",
            coordinates: [55.56816793638622, 37.9292465209941]
        }, properties: {hintContent: 'some property', custom: true}, options: {
            id: 'objectPlacemark'
        }
    };

    that.objectPolyline = {
        type: "Feature",
        geometry: {
            type: "LineString",
            coordinates: [
                [55.6302226842884, 38.09101562499764],
                [55.54933812136998, 38.119604492185324],
                [55.63091557255936, 38.15144042968443]
            ]
        }, properties: {custom: false}, options: {
            strokeWidth: 5,
            id: 'objectPolyline'
        }
    };

    that.objectCircle = {
        type: "Feature",
        geometry: {
            type: "Circle",
            radius: 2500,
            coordinates: [55.59937940902106, 37.85543212890383]
        }, properties: {custom: true}, options: {
            id: 'objectCircle'
        }
    };

    that.objectPolygon = {
        type: "Feature",
        geometry: {
            type: "Polygon",
            coordinates: [
                [
                    [55.6364895642535, 37.74884490966597],
                    [55.55625669137928, 37.7490851508252],
                    [55.55625669138547, 37.802643500434435],
                    /*[55.59671274479137,37.75011511908685],*/[55.63518491519492, 37.7995535956493],
                    [55.6364895642535, 37.74884490966597]
                ]
            ]
        }, properties: {custom: false}, options: {
            id: 'objectPolygon'
        }
    };

    that.objectRectangle = {
        type: "Feature",
        geometry: {
            type: "Rectangle",
            coordinates: [
                [55.54512295034602, 37.99558349609151],
                [55.63534186102953, 38.065583496091456]
            ]
        }, properties: {custom: true}, options: {
            id: 'objectRectangle'
        }
    };

    //string
    that.stringPlacemark = '{"type": "Feature", "geometry": { "type": "Point", "coordinates": [55.46687538432664,37.9429794311503] },"properties": {"custom": "false"}, "options": {"id": "stringPlacemark"}}';

    that.stringPolyline = '{"type": "Feature", "geometry": { "type": "LineString", ' +
        '"coordinates": [[55.524416871923634,38.09376220702885],[55.44331322023102,38.12235107421657],[55.525111637887235,38.15418701171562]] }, "properties": {"hintContent": "some property", "custom": "true"}, "options": {"strokeWidth": "5", "id": "stringPolyline"}}';

    that.stringCircle = '{"type": "Feature", "geometry": { "type": "Circle", "radius" : 2500, ' +
        '"coordinates": [55.49193069360756,37.863671874997436] },"properties": {"custom": "false"}, "options": {"id": "stringCircle"}}';

    that.stringPolygon = '{"type": "Feature", "geometry": { "type": "Polygon", ' +
        '"coordinates": [[[55.53070073517871,37.74609832763472],[55.45025052200228,37.746338568793874],[55.45025052200833,37.799896918403164],[55.52939255036884,37.79680701361795],[55.53070073517871,37.74609832763472]]] },"properties": {"custom": "true"}, "options": {"id": "stringPolygon"}}';

    that.stringRectangle = '{"type": "Feature", "geometry": { "type": "Rectangle", ' +
        '"coordinates": [[55.43908663739027,37.998330078122756],[55.529549921706625,38.068330078122706]] },"properties": {"custom": "false"}, "options": {"id": "stringRectangle"}}';

    // json array            /*[55.49081614008529,37.74736853705561],*/

    that.geometryArray = [];
    that.geometryArray.push({
        type: "Point",
        coordinates: [55.366885164432915, 37.93473968505656],
        properties: {hintContent: 'some property'},
        options: {
            id: 'geometryArrayPlacemark1'
        }
    });
    that.geometryArray.push({
        type: "LineString",
        coordinates: [
            [55.42613518781212, 38.0937622070288],
            [55.34482827011363, 38.12235107421657],
            [55.426831695867556, 38.15418701171562]
        ],
        options: {
            strokeWidth: 5,
            id: 'geometryArrayPolyline1'
        }
    });
    that.geometryArray.push({
        type: "Circle",
        radius: 2500,
        coordinates: [55.39044089529226, 37.863671874997436],
        options: {
            id: 'geometryArrayCircle1'
        }
    });
    that.geometryArray.push({
        type: "Polygon",
        coordinates: [
            [
                [55.433996507187196, 37.743351745603455],
                [55.353347852885264, 37.74359198676263],
                [55.35334785289129, 37.79715033637192],
                /*[55.394013508056325,37.744621955024364],*/[55.43268509406957, 37.79406043158668],
                [55.433996507187196, 37.743351745603455]
            ]
        ],
        options: {
            id: 'geometryArrayPolygon1'
        }
    });
    that.geometryArray.push({
        type: "Rectangle",
        coordinates: [
            [55.339025691157595, 38.001076660154006],
            [55.429719301529765, 38.07107666015379]
        ],
        options: {
            id: 'geometryArrayRectangle1'
        }
    });
    that.geometryArray.push({
        type: "Point",
        coordinates: [55.27134521696249, 37.94023284911905],
        options: {
            id: 'geometryArrayPlacemark2'
        }
    });
    that.geometryArray.push({
        type: "LineString",
        coordinates: [
            [55.32760721010593, 38.09101562499756],
            [55.246096752361616, 38.119604492185324],
            [55.328305462608995, 38.15144042968438]
        ],
        options: {
            strokeWidth: 5,
            id: 'geometryArrayPolyline2'
        }
    });
    that.geometryArray.push({
        type: "Circle",
        radius: 2500,
        coordinates: [55.29495803831193, 37.86641845702866],
        options: {
            id: 'geometryArrayCircle2'
        }
    });
    that.geometryArray.push({
        type: "Polygon",
        coordinates: [
            [
                [55.33079120012741, 37.7460983276346],
                [55.24993101259075, 37.746338568793874],
                [55.249931012596875, 37.799896918403164],
                /*[55.29070330455232,37.74736853705561],*/[55.329476345701, 37.79680701361795],
                [55.33079120012741, 37.7460983276346]
            ]
        ],
        options: {
            id: 'geometryArrayPolygon2'
        }
    });
    that.geometryArray.push({
        type: "Rectangle",
        coordinates: [
            [55.24027965489736, 37.998330078122756],
            [55.3312003005655, 38.0683300781225]
        ],
        options: {
            id: 'geometryArrayRectangle2'
        }
    });
    that.geometryArray.push({
        type: "Point",
        coordinates: [55.87665371818429, 38.376939392087166],
        options: {
            id: 'geometryArrayPlacemark3'
        }
    });
    that.geometryArray.push({
        type: "LineString",
        coordinates: [
            [55.93821794751259, 38.53046874999692],
            [55.85797267648055, 38.559057617184756],
            [55.9389053568021, 38.59089355468379]
        ],
        properties: {hintContent: 'some property'},
        options: {
            strokeWidth: 5,
            id: 'geometryArrayPolyline3'
        }
    });
    that.geometryArray.push({
        type: "Circle",
        radius: 2500,
        coordinates: [55.90144683248903, 38.29763183593387],
        options: {
            id: 'geometryArrayCircle3'
        }
    });
    that.geometryArray.push({
        type: "Polygon",
        coordinates: [
            [
                [55.93826946763859, 38.191044616695734],
                [55.858658063683045, 38.19128485785458],
                [55.85865806368912, 38.244843207463944],
                /*[55.89880082350384,38.19231482611667],*/[55.9369749287213, 38.24175330267873],
                [55.93826946763859, 38.191044616695734]
            ]
        ],
        options: {
            id: 'geometryArrayPolygon3'
        }
    });
    that.geometryArray.push({
        type: "Rectangle",
        coordinates: [
            [55.855335722211564, 38.44327636718456],
            [55.94483798553092, 38.51327636718453]
        ],
        options: {
            id: 'geometryArrayRectangle3'
        }
    });


    //JSON коллекция объектов с вложенной коллекцией.  Розовый цвет.

    that.objectCollection = {
        type: 'FeatureCollection',
        features: [
            {
                type: "Feature",
                geometry: { type: "Point", coordinates: [55.77616109362325, 38.37419281005592] },
                properties: {custom: false},
                options: {
                    id: 'objectCollectionPlacemark1'
                }
            },
            {
                type: "Feature",
                geometry: { type: "LineString", coordinates: [
                    [55.83633969210228, 38.53321533202814],
                    [55.75588270464334, 38.56180419921588],
                    [55.837028915876594, 38.5936401367149]
                ] },
                properties: {custom: true},
                options: {
                    strokeWidth: 5,
                    id: 'objectCollectionPolyline1'
                }
            },
            {
                type: "Feature",
                geometry: { type: "Circle", radius: 2500, coordinates: [55.80256564650234, 38.29763183593387] },
                properties: {custom: false},
                options: {
                    id: 'objectCollectionCircle1'
                }
            },
            {
                type: "Feature",
                geometry: {
                    type: "Polygon",
                    coordinates: [
                        [
                            [55.83793696034517, 38.19379119872698],
                            [55.75811869622769, 38.194031439885826],
                            [55.75811869623376, 38.247589789495166],
                            /*[55.798365738169615,38.195061408147865],*/[55.83663905621212, 38.244499884709924],
                            [55.83793696034517, 38.19379119872698]
                        ]
                    ]
                },
                properties: {custom: true},
                options: {
                    id: 'objectCollectionPolygon1'
                }
            },
            {
                type: "Feature",
                geometry: { type: "Rectangle", coordinates: [
                    [55.750140751767525, 38.44052978515329],
                    [55.83988632157084, 38.51052978515328]
                ] },
                properties: {custom: false},
                options: {
                    id: 'objectCollectionRectangle1'
                }
            },
            {
                type: 'FeatureCollection',
                features: [
                    {
                        type: "Feature",
                        geometry: { type: "Point", coordinates: [55.66919882214023, 38.37419281005587] },
                        properties: {custom: true},
                        options: {
                            id: 'objectCollectionPlacemark2'
                        }
                    },
                    {
                        type: "Feature",
                        geometry: { type: "LineString", coordinates: [
                            [55.73109311449373, 38.53321533202814],
                            [55.65041767250691, 38.56180419921588],
                            [55.7317842105086, 38.5936401367149]
                        ] },
                        properties: {custom: false},
                        options: {
                            strokeWidth: 5,
                            id: 'objectCollectionPolyline2'
                        }
                    },
                    {
                        type: "Feature",
                        geometry: { type: "Circle", radius: 2500, coordinates: [55.6941248310675, 38.29763183593387] },
                        properties: {custom: true},
                        options: {
                            id: 'objectCollectionCircle2'
                        }
                    },
                    {
                        type: "Feature",
                        geometry: {
                            type: "Polygon",
                            coordinates: [
                                [
                                    [55.73579415762691, 38.188298034664335],
                                    [55.655765547294166, 38.188538275823184],
                                    [55.65576554730025, 38.2420966254326],
                                    /*[55.696118628522385,38.189568244085294],*/[55.73449283155276, 38.23900672064736],
                                    [55.73579415762691, 38.188298034664335]
                                ]
                            ]
                        },
                        properties: {custom: false},
                        options: {
                            id: 'objectCollectionPolygon2'
                        }
                    },
                    {
                        type: "Feature",
                        geometry: { type: "Rectangle", coordinates: [
                            [55.643106818255475, 38.44052978515329],
                            [55.73309964569464, 38.51052978515328]
                        ] },
                        properties: {custom: true},
                        options: {
                            id: 'objectCollectionRectangle2'
                        }
                    },
                    {
                        type: 'FeatureCollection',
                        features: [
                            {
                                type: "Feature",
                                geometry: { type: "Point", coordinates: [55.57128047758205, 38.371446228024624] },
                                properties: {custom: false},
                                options: {
                                    id: 'objectCollectionPlacemark3'
                                }
                            },
                            {
                                type: "Feature",
                                geometry: { type: "LineString", coordinates: [
                                    [55.63022268274784, 38.53046874999687],
                                    [55.54933811985433, 38.55905761718463],
                                    [55.63091557101846, 38.59089355468366]
                                ] },
                                properties: {custom: true},
                                options: {
                                    strokeWidth: 5,
                                    id: 'objectCollectionPolyline3'
                                }
                            },
                            {
                                type: "Feature",
                                geometry: { type: "Circle", radius: 2500, coordinates: [55.599379407489764, 38.29488525390263] },
                                properties: {custom: false},
                                options: {
                                    id: 'objectCollectionCircle3'
                                }
                            },
                            {
                                type: "Feature",
                                geometry: {
                                    type: "Polygon",
                                    coordinates: [
                                        [
                                            [55.63493591744712, 38.18005828857042],
                                            [55.55469985075206, 38.18029852972929],
                                            [55.55469985075809, 38.23385687933861],
                                            /*[55.59515751422138,38.18132849799143],*/[55.633631216430494, 38.23076697455345],
                                            [55.63493591744712, 38.18005828857042]
                                        ]
                                    ]
                                },
                                properties: {custom: true},
                                options: {
                                    id: 'objectCollectionPolygon3'
                                }
                            },
                            {
                                type: "Feature",
                                geometry: { type: "Rectangle", coordinates: [
                                    [55.54356566766042, 38.435036621090795],
                                    [55.63378816943728, 38.50503662109077]
                                ] },
                                properties: {hintContent: "some property", custom: false},
                                options: {
                                    id: 'objectCollectionRectangle3'
                                }
                            }
                        ]
                    }
                ]
            }
        ]
    };

    // util.Promise
    // geoCoder
    that.promisePoints = ymaps.geocode("петербург");
    that.promisePolyline = ymaps.route([
        'Королев',
        'Химки'
    ], {
        mapStateAutoApply: false,
        id: 'promisePolyline'
    });
    that.geoXMLObject = ymaps.geoXml.load('https://maps.yandex.ru/export/usermaps/6lXyMJReL87z0LJ0Y3eF2M59iZ1Dbv9D/');

    /*ymaps.geocode("петербург").then(function (res) {
        promisePoints = res.geoObjects.get(0);
        that.geoQueryResult.add(promisePoints)
    }, function (err) {
        console.log(err.message);
    });

    // router
    ymaps.route([
        'Королев',
        'Химки'
    ], {
        mapStateAutoApply: false,
        id: 'promisePolyline'
    }).then(function (route) {
            promisePolyline = route;
            that.geoQueryResult.add(promisePolyline)
        }, function (err) {
            console.log(err.message);
        });

    // geoXML
    ymaps.geoXml.load('http://maps.yandex.ru/export/usermaps/6lXyMJReL87z0LJ0Y3eF2M59iZ1Dbv9D/').then(function (res) {
        geoXMLObject = res.geoObjects;
        that.geoQueryResult.add(geoXMLObject)
    }, function (err) {
        console.log(err.message);
    });*/

    that.geoQueryResult = ymaps.geoQuery(that.geoObjectPlacemark).add(that.geoObjectPolyline)
        .add(that.geoObjectCircle).add(that.geoObjectPolygon).add(that.geoObjectRectangle).add(that.geoObjectArray)
        .add(that.geoObjectCollection).add(that.geoObjectCollectionArray)
        .add(that.objectPlacemark).add(that.objectPolyline).add(that.objectCircle).add(that.objectPolygon)
        .add(that.objectRectangle).add(that.stringPlacemark).add(that.stringPolyline).add(that.stringCircle)
        .add(that.stringPolygon).add(that.stringRectangle).add(that.geometryArray).add(that.objectCollection)
        .add(that.promisePoints).add(that.promisePolyline).add(that.geoXMLObject);



    // GeoQueryResult + sourceResult


};

