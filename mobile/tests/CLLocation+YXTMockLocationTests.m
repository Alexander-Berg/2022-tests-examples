
#import <CLLocationManager+YXTMockLocation.h>

SPEC_BEGIN(YXTMockLocationTests)

describe(@"YXTMockLocationTests", ^{

    CLLocation *const location = [[CLLocation alloc] initWithLatitude:23 longitude:42];
    CLLocationManager *__block locationManager = nil;

    beforeEach(^{
        [CLLocationManager yxt_exchangeImplementations];
        locationManager = [[CLLocationManager alloc] init];
    });

    afterEach(^{
        [locationManager stopUpdatingLocation];
        [CLLocationManager yxt_exchangeImplementations];
    });

    it(@"Should store location", ^{
        [CLLocationManager yxt_mockLocation:location];
        [[locationManager.location should] equal:location];
    });
    it(@"Should reset location", ^{
        [CLLocationManager yxt_mockLocation:location];
        [CLLocationManager yxt_mockLocation:nil];
        [[locationManager.location should] beNil];
    });
    it(@"Should enable location services", ^{
        [[theValue([CLLocationManager locationServicesEnabled]) should] beYes];
    });
    it(@"Should be authorized", ^{
        [[theValue([CLLocationManager authorizationStatus]) should] equal:theValue(kCLAuthorizationStatusAuthorizedAlways)];
    });

    context(@"Location updates", ^{
        NSObject<CLLocationManagerDelegate> *__block delegate = nil;
        beforeEach(^{
            delegate = [KWMock nullMockForProtocol:@protocol(CLLocationManagerDelegate)];
            locationManager.delegate = delegate;
        });
        it(@"Should receive location update on change", ^{
            [locationManager startUpdatingLocation];
            [[delegate should] receive:@selector(locationManager:didUpdateLocations:)
                         withArguments:locationManager, @[ location ]];
            [CLLocationManager yxt_mockLocation:location];
        });
        it(@"Should receive location update on locations update start", ^{
            [CLLocationManager yxt_mockLocation:location];
            [[delegate should] receive:@selector(locationManager:didUpdateLocations:)
                         withArguments:locationManager, @[ location ]];
            [locationManager startUpdatingLocation];
        });
        it(@"Should not be active before start", ^{
            [[theValue([CLLocationManager yxt_isLocationUpdatesActive]) should] beNo];
        });
        it(@"Should be active after start", ^{
            [locationManager startUpdatingLocation];
            [[theValue([CLLocationManager yxt_isLocationUpdatesActive]) should] beYes];
        });
        it(@"Should not be active after stop", ^{
            [locationManager startUpdatingLocation];
            [locationManager stopUpdatingLocation];
            [[theValue([CLLocationManager yxt_isLocationUpdatesActive]) should] beNo];
        });
    });

});

SPEC_END
