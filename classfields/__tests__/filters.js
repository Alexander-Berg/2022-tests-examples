/* eslint-disable max-len */
const desktopRoutes = require('../desktop');
const mobileRoutes = require('../touch-phone');

describe('desktop urls', () => {
    describe('краевые случаи', () => {
        const route = desktopRoutes.getRouteByName('search');

        it('Учитывать разновидность категории Дом', () => {
            const params1 = {
                rgid: '587795',
                type: 'SELL',
                category: 'HOUSE',
                includeTag: 1794478
            };

            expect(route.build(params1)).toEqual('/moskva/kupit/dom/v-sovremennom-style/');

            const params2 = {
                rgid: '587795',
                type: 'SELL',
                category: 'HOUSE',
                houseType: 'DUPLEX',
                includeTag: 1794478
            };

            expect(route.build(params2)).toEqual('/moskva/kupit/dom/duplex/?includeTag=1794478');
        });

        it('Один экземляр классифицирующих параметров', () => {
            const params1 = {
                rgid: '587795',
                type: 'SELL',
                balcony: 'BALCONY',
                metroGeoId: [ 20444 ]
            };

            expect(route.build(params1))
                .toEqual('/moskva/kupit/kvartira/metro-universitet/s-balkonom/');

            const params2 = {
                rgid: '587795',
                type: 'SELL',
                balcony: 'BALCONY',
                metroGeoId: [ 20444, 20482 ]
            };

            expect(route.build(params2))
                .toEqual('/moskva/kupit/kvartira/?balcony=BALCONY&metroGeoId=20444&metroGeoId=20482');
        });
    });

    // SELL
    describe('sell urls', () => {
        const route = desktopRoutes.getRouteByName('search');

        it('should build url with one filter', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                balcony: 'BALCONY'
            };

            expect(route.build(params)).toEqual('/moskva/kupit/kvartira/s-balkonom/');
        });

        it('should build url with two filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                balcony: 'BALCONY',
                renovation: [ 'EURO', 'COSMETIC_DONE' ]
            };

            expect(route.build(params)).toEqual('/moskva/kupit/kvartira/s-remontom-i-s-balkonom/');
        });

        it('should build url with two filters and other params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                balcony: 'BALCONY',
                renovation: [ 'EURO', 'COSMETIC_DONE' ],
                someParam: 'someValue'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/s-remontom-i-s-balkonom/?someParam=someValue');
        });

        it('should ignore RENT filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                withPets: 'YES',
                withChildren: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/?withPets=YES&withChildren=YES');
        });

        it('should ignore COMMERCIAL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                officeClass: 'A',
                commercialBuildingType: 'BUSINESS_CENTER'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/?officeClass=A&commercialBuildingType=BUSINESS_CENTER');
        });

        it('should ignore NEWBUILDINGS filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                dealType: 'FZ_214',
                hasInstallment: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/?dealType=FZ_214&hasInstallment=YES');
        });

        it('should ignore VILLAGES filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                landType: 'SNT',
                wallsType: 'WOOD'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/?landType=SNT&wallsType=WOOD');
        });

        it('should build simple SELL url with street', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/st-ulica-hachaturyana-123/');
        });

        it('should build simple SELL url with street and filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                balcony: 'BALCONY'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/st-ulica-hachaturyana-123/s-balkonom/');
        });

        it('should build simple SELL url with street and metro', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/metro-universitet/?streetId=123');
        });

        it('should build simple SELL url with street and metro and filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444,
                balcony: 'BALCONY'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/metro-universitet/?streetId=123&balcony=BALCONY');
        });

        it('should build simple SELL url with street and house', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                buildingIds: 12312312312312,
                houseNumber: '24'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/st-ulica-hachaturyana-123/dom-24-12312312312312/');
        });

        it('should build simple SELL url with street, house and roomsTotal', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                buildingIds: 12312312312312,
                houseNumber: '24',
                roomsTotal: 2
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/dvuhkomnatnaya/st-ulica-hachaturyana-123/dom-24-12312312312312/');
        });

        it('should build simple SELL url with street, house and any filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                buildingIds: 12312312312312,
                houseNumber: '24',
                areaMin: 12,
                balcony: 'BALCONY'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/st-ulica-hachaturyana-123/' +
                    'dom-24-12312312312312/?areaMin=12&balcony=BALCONY');
        });

        it('should build simple SELL with get params when hasManyGeo', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                buildingIds: 12312312312312,
                houseNumber: '24',
                metroGeoId: 12,
                balcony: 'BALCONY'
            };

            expect(route.build(params)).toEqual(
                '/moskva/kupit/kvartira/?streetId=123&buildingIds=12312312312312&metroGeoId=12&balcony=BALCONY'
            );
        });

        it('should build url with filter and site params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                balcony: 'BALCONY',
                siteName: 'nazvanie-777',
                siteId: '777'
            };

            expect(route.build(params)).toEqual(
                '/moskva/kupit/kvartira/zhk-nazvanie-777/s-balkonom/'
            );
        });
    });

    // RENT
    describe('rent urls', () => {
        const route = desktopRoutes.getRouteByName('search');

        it('should build url with one filter', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                withPets: 'YES'
            };

            expect(route.build(params)).toEqual('/moskva/snyat/kvartira/s-zhivotnymi/');
        });

        it('should build url with two filters', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                withPets: 'YES',
                withChildren: 'YES'
            };

            expect(route.build(params)).toEqual('/moskva/snyat/kvartira/s-zhivotnymi-i-s-detmi/');
        });

        it('should build url with two filters and other params', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                withPets: 'YES',
                withChildren: 'YES',
                someParam: 'someValue'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/s-zhivotnymi-i-s-detmi/?someParam=someValue');
        });

        it('should ignore SELL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                hasWaterSupply: 'YES',
                hasHeatingSupply: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/?hasWaterSupply=YES&hasHeatingSupply=YES');
        });

        it('should ignore COMMERCIAL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                officeClass: 'A',
                commercialBuildingType: 'BUSINESS_CENTER'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/?officeClass=A&commercialBuildingType=BUSINESS_CENTER');
        });

        it('should ignore NEWBUILDINGS filter params', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                dealType: 'FZ_214',
                hasInstallment: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/?dealType=FZ_214&hasInstallment=YES');
        });

        it('should ignore VILLAGES filter params', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                landType: 'SNT',
                wallsType: 'WOOD'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/?landType=SNT&wallsType=WOOD');
        });

        it('should build simple RENT url with street', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                streetId: 123,
                streetName: 'улица Хачатуряна'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/st-ulica-hachaturyana-123/');
        });

        it('should build simple RENT url with street and filters', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                balcony: 'BALCONY'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/st-ulica-hachaturyana-123/s-balkonom/');
        });

        it('should build simple RENT url with street and metro', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/metro-universitet/?streetId=123');
        });

        it('should build simple RENT url with street and metro and filters', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444,
                balcony: 'BALCONY'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/metro-universitet/?streetId=123&balcony=BALCONY');
        });
        it('should build simple RENT url with street and house', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                buildingIds: 12312312312312,
                houseNumber: '24'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/st-ulica-hachaturyana-123/dom-24-12312312312312/');
        });

        it('should build simple RENT url with street, house and roomsTotal', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                buildingIds: 12312312312312,
                houseNumber: '24',
                roomsTotal: 2
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/dvuhkomnatnaya/st-ulica-hachaturyana-123/dom-24-12312312312312/');
        });

        it('should build simple RENT url with street, house and any filters', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                buildingIds: 12312312312312,
                houseNumber: '24',
                areaMin: 12,
                balcony: 'BALCONY'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/st-ulica-hachaturyana-123/' +
                    'dom-24-12312312312312/?areaMin=12&balcony=BALCONY');
        });

        it('should build simple RENT with get params when hasManyGeo', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                buildingIds: 12312312312312,
                houseNumber: '24',
                metroGeoId: 12,
                balcony: 'BALCONY'
            };

            expect(route.build(params)).toEqual(
                '/moskva/snyat/kvartira/?streetId=123&buildingIds=12312312312312&metroGeoId=12&balcony=BALCONY'
            );
        });

        it('should build url with filter and site params if not NEW_BUILDING_2004', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                balcony: 'BALCONY',
                siteName: 'nazvanie-777',
                siteId: '777'
            };

            expect(route.build(params)).toEqual(
                '/moskva/snyat/kvartira/zhk-nazvanie-777/s-balkonom/'
            );
        });

        it('should ignore NEW_BUILDING_2004 filter if site params', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                builtYearMin: '2004',
                siteName: 'nazvanie-777',
                siteId: '777'
            };

            expect(route.build(params)).toEqual(
                '/moskva/snyat/kvartira/zhk-nazvanie-777/?builtYearMin=2004'
            );
        });
    });

    // COMMERCIAL
    describe('commercial urls', () => {
        const route = desktopRoutes.getRouteByName('search');

        it('should build url with one filter', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                officeClass: 'A'
            };

            expect(route.build(params)).toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/class-a/');
        });

        it('should build url with two filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                officeClass: 'A',
                commercialBuildingType: 'BUSINESS_CENTER'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/biznes-center-i-class-a/');
        });

        it('should build url with two filters and other params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                officeClass: 'A',
                commercialBuildingType: 'BUSINESS_CENTER',
                someParam: 'someValue'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis' +
                    '/biznes-center-i-class-a/?someParam=someValue');
        });

        it('should ignore SELL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                hasWaterSupply: 'YES',
                hasHeatingSupply: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kommercheskaya-nedvizhimost/ofis/?hasWaterSupply=YES&hasHeatingSupply=YES');
        });

        it('should ignore RENT filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                withPets: 'YES',
                withChildren: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/?withPets=YES&withChildren=YES');
        });

        it('should ignore NEWBUILDINGS filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                dealType: 'FZ_214',
                hasInstallment: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/?dealType=FZ_214&hasInstallment=YES');
        });

        it('should ignore VILLAGES filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                landType: 'SNT',
                wallsType: 'WOOD'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/?landType=SNT&wallsType=WOOD');
        });

        it('should build simple COMMERCIAL url with street', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                streetId: 123,
                streetName: 'улица Хачатуряна'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/st-ulica-hachaturyana-123/');
        });

        it('should build simple COMMERCIAL url with street and filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                officeClass: 'A'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/st-ulica-hachaturyana-123/class-a/');
        });

        it('should build simple COMMERCIAL url with street and metro', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/metro-universitet/?streetId=123');
        });

        it('should build simple COMMERCIAL url with street and metro and filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444,
                officeClass: 'A'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/metro-universitet/?streetId=123&officeClass=A');
        });
    });

    // NEWBUILDINGS
    describe('newbuildings urls', () => {
        const route = desktopRoutes.getRouteByName('newbuilding-search');

        describe('фильтр отделка', () => {
            it('черновая', () => {
                const params = {
                    rgid: '587795',
                    type: 'SELL',
                    decoration: 'ROUGH'
                };

                expect(route.build(params)).toEqual('/moskva/kupit/novostrojka/chernovaya-otdelka/');
            });

            it('чистовая', () => {
                const params = {
                    rgid: '587795',
                    type: 'SELL',
                    decoration: 'CLEAN'
                };

                expect(route.build(params)).toEqual('/moskva/kupit/novostrojka/chistovaya-otdelka/');
            });

            it('под ключ', () => {
                const params = {
                    rgid: '587795',
                    type: 'SELL',
                    decoration: 'TURNKEY'
                };

                expect(route.build(params)).toEqual('/moskva/kupit/novostrojka/pod-kluch/');
            });

            it('без отделки', () => {
                const params = {
                    rgid: '587795',
                    type: 'SELL',
                    decoration: 'NO_DECORATION'
                };

                expect(route.build(params)).toEqual('/moskva/kupit/novostrojka/bez-otdelky/');
            });

            it('предчистовая', () => {
                const params = {
                    rgid: '587795',
                    type: 'SELL',
                    decoration: 'PRE_CLEAN'
                };

                expect(route.build(params)).toEqual('/moskva/kupit/novostrojka/predchistovya-otdelka/');
            });

            it('white-box', () => {
                const params = {
                    rgid: '587795',
                    type: 'SELL',
                    decoration: 'WHITE_BOX'
                };

                expect(route.build(params)).toEqual('/moskva/kupit/novostrojka/white-box/');
            });
        });

        it('should build url with two filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                decoration: 'ROUGH',
                hasSiteMortgage: 'YES'
            };

            expect(route.build(params)).toEqual('/moskva/kupit/novostrojka/chernovaya-otdelka-i-s-ipotekoy/');
        });

        it('should build url with two filters and other params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                decoration: 'ROUGH',
                hasSiteMortgage: 'YES',
                someParam: 'someValue'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/chernovaya-otdelka-i-s-ipotekoy/?someParam=someValue');
        });

        it('should ignore SELL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                hasWaterSupply: 'YES',
                hasHeatingSupply: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/?hasWaterSupply=YES&hasHeatingSupply=YES');
        });

        it('should ignore RENT filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                withPets: 'YES',
                withChildren: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/?withPets=YES&withChildren=YES');
        });

        it('should ignore COMMERCIAL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                officeClass: 'A',
                commercialBuildingType: 'BUSINESS_CENTER'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/?officeClass=A&commercialBuildingType=BUSINESS_CENTER');
        });

        it('should ignore VILLAGES filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                landType: 'SNT',
                wallsType: 'WOOD'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/?landType=SNT&wallsType=WOOD');
        });

        it('should build simple NEWBUILDING url with street', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/st-ulica-hachaturyana-123/');
        });

        it('should build simple NEWBUILDING url with street and filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                decoration: 'ROUGH'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/st-ulica-hachaturyana-123/chernovaya-otdelka/');
        });

        it('should build simple NEWBUILDING url with street and metro', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/metro-universitet/?streetId=123');
        });

        it('should build simple NEWBUILDING url with street and metro and filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444,
                decoration: 'ROUGH'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/metro-universitet/?streetId=123&decoration=ROUGH');
        });
    });

    // VILLAGES
    describe('villages urls', () => {
        const route = desktopRoutes.getRouteByName('village-search');

        it('should build url with one filter', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                landType: 'SNT'
            };

            expect(route.build(params)).toEqual('/moskva/kupit/kottedzhnye-poselki/v-snt/');
        });

        it('should build url with two filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                landType: 'SNT',
                wallsType: 'WOOD'
            };

            expect(route.build(params)).toEqual('/moskva/kupit/kottedzhnye-poselki/derevo-i-v-snt/');
        });

        it('should build url with two filters and other params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                landType: 'SNT',
                wallsType: 'WOOD',
                someParam: 'someValue'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kottedzhnye-poselki/derevo-i-v-snt/?someParam=someValue');
        });

        it('should build url with developer', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                developerName: 'test',
                developerId: '123123'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kottedzhnye-poselki/z-test-123123/');
        });

        it('should build url with developer with query params if developerName is not provided', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                developerId: '123123'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kottedzhnye-poselki/?developerId=123123');
        });

        it('should ignore SELL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                hasWaterSupply: 'YES',
                hasHeatingSupply: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kottedzhnye-poselki/?hasWaterSupply=YES&hasHeatingSupply=YES');
        });

        it('should ignore RENT filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                withPets: 'YES',
                withChildren: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kottedzhnye-poselki/?withPets=YES&withChildren=YES');
        });

        it('should ignore COMMERCIAL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                officeClass: 'A',
                commercialBuildingType: 'BUSINESS_CENTER'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kottedzhnye-poselki/?officeClass=A&commercialBuildingType=BUSINESS_CENTER');
        });

        it('should ignore NEWBUILDINGS filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                dealType: 'FZ_214',
                hasInstallment: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kottedzhnye-poselki/?dealType=FZ_214&hasInstallment=YES');
        });
    });
});

// MOBILE
describe('mobile urls', () => {
    // SELL
    describe('sell urls', () => {
        const route = mobileRoutes.getRouteByName('search');

        it('should build url with one filter', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                balcony: 'BALCONY'
            };

            expect(route.build(params)).toEqual('/moskva/kupit/kvartira/s-balkonom/');
        });

        it('should build url with two filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                balcony: 'BALCONY',
                renovation: [ 'EURO', 'COSMETIC_DONE' ]
            };

            expect(route.build(params)).toEqual('/moskva/kupit/kvartira/s-remontom-i-s-balkonom/');
        });

        it('should build url with two filters and other params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                balcony: 'BALCONY',
                renovation: [ 'EURO', 'COSMETIC_DONE' ],
                someParam: 'someValue'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/s-remontom-i-s-balkonom/?someParam=someValue');
        });

        it('should ignore RENT filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                withPets: 'YES',
                withChildren: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/?withPets=YES&withChildren=YES');
        });

        it('should ignore COMMERCIAL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                officeClass: 'A',
                commercialBuildingType: 'BUSINESS_CENTER'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/?officeClass=A&commercialBuildingType=BUSINESS_CENTER');
        });

        it('should ignore NEWBUILDINGS filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                dealType: 'FZ_214',
                hasInstallment: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/?dealType=FZ_214&hasInstallment=YES');
        });

        it('should ignore VILLAGES filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                landType: 'SNT',
                wallsType: 'WOOD'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/?landType=SNT&wallsType=WOOD');
        });

        it('should build simple SELL url with street', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/st-ulica-hachaturyana-123/');
        });

        it('should build simple SELL url with street and filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                balcony: 'BALCONY'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/st-ulica-hachaturyana-123/s-balkonom/');
        });

        it('should build simple SELL url with street and metro', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/metro-universitet/?streetId=123');
        });

        it('should build simple SELL url with street and metro and filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444,
                balcony: 'BALCONY'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kvartira/metro-universitet/?streetId=123&balcony=BALCONY');
        });
    });

    // RENT
    describe('rent urls', () => {
        const route = mobileRoutes.getRouteByName('search');

        it('should build url with one filter', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                withPets: 'YES'
            };

            expect(route.build(params)).toEqual('/moskva/snyat/kvartira/s-zhivotnymi/');
        });

        it('should build url with two filters', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                withPets: 'YES',
                withChildren: 'YES'
            };

            expect(route.build(params)).toEqual('/moskva/snyat/kvartira/s-zhivotnymi-i-s-detmi/');
        });

        it('should build url with two filters and other params', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                withPets: 'YES',
                withChildren: 'YES',
                someParam: 'someValue'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/s-zhivotnymi-i-s-detmi/?someParam=someValue');
        });

        it('should ignore SELL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                hasWaterSupply: 'YES',
                hasHeatingSupply: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/?hasWaterSupply=YES&hasHeatingSupply=YES');
        });

        it('should ignore COMMERCIAL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                officeClass: 'A',
                commercialBuildingType: 'BUSINESS_CENTER'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/?officeClass=A&commercialBuildingType=BUSINESS_CENTER');
        });

        it('should ignore NEWBUILDINGS filter params', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                dealType: 'FZ_214',
                hasInstallment: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/?dealType=FZ_214&hasInstallment=YES');
        });

        it('should ignore VILLAGES filter params', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                landType: 'SNT',
                wallsType: 'WOOD'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/?landType=SNT&wallsType=WOOD');
        });

        it('should build simple RENT url with street', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                streetId: 123,
                streetName: 'улица Хачатуряна'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/st-ulica-hachaturyana-123/');
        });

        it('should build simple RENT url with street and filters', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                balcony: 'BALCONY'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/st-ulica-hachaturyana-123/s-balkonom/');
        });

        it('should build simple RENT url with street and metro', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/metro-universitet/?streetId=123');
        });

        it('should build simple RENT url with street and metro and filters', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444,
                balcony: 'BALCONY'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kvartira/metro-universitet/?streetId=123&balcony=BALCONY');
        });
    });

    // COMMERCIAL
    describe('commercial urls', () => {
        const route = mobileRoutes.getRouteByName('search');

        it('should build url with one filter', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                officeClass: 'A'
            };

            expect(route.build(params)).toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/class-a/');
        });

        it('should build url with two filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                officeClass: 'A',
                commercialBuildingType: 'BUSINESS_CENTER'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/biznes-center-i-class-a/');
        });

        it('should build url with two filters and other params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                officeClass: 'A',
                commercialBuildingType: 'BUSINESS_CENTER',
                someParam: 'someValue'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis' +
                    '/biznes-center-i-class-a/?someParam=someValue');
        });

        it('should ignore SELL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'RENT',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                hasWaterSupply: 'YES',
                hasHeatingSupply: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/snyat/kommercheskaya-nedvizhimost/ofis/?hasWaterSupply=YES&hasHeatingSupply=YES');
        });

        it('should ignore RENT filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                withPets: 'YES',
                withChildren: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/?withPets=YES&withChildren=YES');
        });

        it('should ignore NEWBUILDINGS filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                dealType: 'FZ_214',
                hasInstallment: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/?dealType=FZ_214&hasInstallment=YES');
        });

        it('should ignore VILLAGES filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                landType: 'SNT',
                wallsType: 'WOOD'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/?landType=SNT&wallsType=WOOD');
        });

        it('should build simple COMMERCIAL url with street', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                streetId: 123,
                streetName: 'улица Хачатуряна'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/st-ulica-hachaturyana-123/');
        });

        it('should build simple COMMERCIAL url with street and filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                officeClass: 'A'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/st-ulica-hachaturyana-123/class-a/');
        });

        it('should build simple COMMERCIAL url with street and metro', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/metro-universitet/?streetId=123');
        });

        it('should build simple COMMERCIAL url with street and metro and filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444,
                officeClass: 'A'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/metro-universitet/?streetId=123&officeClass=A');
        });
    });

    // NEWBUILDINGS
    describe('newbuildings urls', () => {
        const route = mobileRoutes.getRouteByName('sites-search');

        it('should build url with one filter', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                decoration: 'ROUGH'
            };

            expect(route.build(params)).toEqual('/moskva/kupit/novostrojka/chernovaya-otdelka/');
        });

        it('should build url with two filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                decoration: 'ROUGH',
                hasSiteMortgage: 'YES'
            };

            expect(route.build(params)).toEqual('/moskva/kupit/novostrojka/chernovaya-otdelka-i-s-ipotekoy/');
        });

        it('should build url with two filters and other params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                decoration: 'ROUGH',
                hasSiteMortgage: 'YES',
                someParam: 'someValue'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/chernovaya-otdelka-i-s-ipotekoy/?someParam=someValue');
        });

        it('should ignore SELL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                hasWaterSupply: 'YES',
                hasHeatingSupply: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/?hasWaterSupply=YES&hasHeatingSupply=YES');
        });

        it('should ignore RENT filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                withPets: 'YES',
                withChildren: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/?withPets=YES&withChildren=YES');
        });

        it('should ignore COMMERCIAL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                officeClass: 'A',
                commercialBuildingType: 'BUSINESS_CENTER'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/?officeClass=A&commercialBuildingType=BUSINESS_CENTER');
        });

        it('should ignore VILLAGES filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                landType: 'SNT',
                wallsType: 'WOOD'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/?landType=SNT&wallsType=WOOD');
        });

        it('should build simple NEWBUILDING url with street', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/st-ulica-hachaturyana-123/');
        });

        it('should build simple NEWBUILDING url with street and filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                decoration: 'ROUGH'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/st-ulica-hachaturyana-123/chernovaya-otdelka/');
        });

        it('should build simple NEWBUILDING url with street and metro', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/metro-universitet/?streetId=123');
        });

        it('should build simple NEWBUILDING url with street and metro and filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                streetId: 123,
                streetName: 'улица Хачатуряна',
                metroGeoId: 20444,
                decoration: 'ROUGH'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/novostrojka/metro-universitet/?streetId=123&decoration=ROUGH');
        });
    });

    // VILLAGES
    describe('villages urls', () => {
        const route = mobileRoutes.getRouteByName('villages-search');

        it('should build url with one filter', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                landType: 'SNT'
            };

            expect(route.build(params)).toEqual('/moskva/kupit/kottedzhnye-poselki/v-snt/');
        });

        it('should build url with two filters', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                landType: 'SNT',
                wallsType: 'WOOD'
            };

            expect(route.build(params)).toEqual('/moskva/kupit/kottedzhnye-poselki/derevo-i-v-snt/');
        });

        it('should build url with two filters and other params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                landType: 'SNT',
                wallsType: 'WOOD',
                someParam: 'someValue'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kottedzhnye-poselki/derevo-i-v-snt/?someParam=someValue');
        });

        it('should ignore SELL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                hasWaterSupply: 'YES',
                hasHeatingSupply: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kottedzhnye-poselki/?hasWaterSupply=YES&hasHeatingSupply=YES');
        });

        it('should ignore RENT filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                withPets: 'YES',
                withChildren: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kottedzhnye-poselki/?withPets=YES&withChildren=YES');
        });

        it('should ignore COMMERCIAL filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                officeClass: 'A',
                commercialBuildingType: 'BUSINESS_CENTER'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kottedzhnye-poselki/?officeClass=A&commercialBuildingType=BUSINESS_CENTER');
        });

        it('should ignore NEWBUILDINGS filter params', () => {
            const params = {
                rgid: '587795',
                type: 'SELL',
                dealType: 'FZ_214',
                hasInstallment: 'YES'
            };

            expect(route.build(params))
                .toEqual('/moskva/kupit/kottedzhnye-poselki/?dealType=FZ_214&hasInstallment=YES');
        });
    });
});
