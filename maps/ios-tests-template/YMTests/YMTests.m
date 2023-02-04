#import <XCTest/XCTest.h>
#import "unit_test_wrapper.h"

NSTimeInterval TESTS_TIMEOUT = 900.0;

@interface UnitTests : XCTestCase

@end

@implementation UnitTests

- (void)setUp {
    [super setUp];
}

- (void)tearDown {
    [super tearDown];
}

- (void)testRunBoostTests {
    __block int result = 1;
    XCTestExpectation *expectation = [self expectationWithDescription:@"Unit tests out of time"];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        result = do_unit_tests();
        [expectation fulfill];
    });

    [self waitForExpectationsWithTimeout:TESTS_TIMEOUT handler: ^(NSError* error) {
        XCTAssert(result == 0 || result == 200);
    }];
}

@end
