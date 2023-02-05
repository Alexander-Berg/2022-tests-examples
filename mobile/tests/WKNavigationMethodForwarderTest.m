//
//  WKNavigationMethodForwarderTest.m
//  UtilsTests
//
//  Created by Timur Turaev on 12.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

#import <XCTest/XCTest.h>
@import Utils;

@interface YOTestForwarderObject : NSObject
- (instancetype)initWithForwarder:(YOWKNavigationMethodForwarder *)forwarder;
@property (nonatomic, readonly) BOOL forwarderDidCrashWebProcessCalled;
@end


@interface WKNavigationMethodForwarderTest : XCTestCase
@property (nonatomic) YOWKNavigationMethodForwarder *forwarder;
@property (nonatomic) SEL didCrashSelector;
@end


@implementation WKNavigationMethodForwarderTest

- (void)setUp {
    [super setUp];
    self.forwarder = [[YOWKNavigationMethodForwarder alloc] init];
    EXECUTE_WITHOUT_SELECTOR_CHECK(self.didCrashSelector = @selector(_webViewWebProcessDidCrash:));
}

- (void)testRespondsToSelector {
    XCTAssertTrue([self.forwarder respondsToSelector:self.didCrashSelector]);
}

- (void)testMethodSignature {
    XCTAssertNotNil([self.forwarder methodSignatureForSelector:self.didCrashSelector]);
}

- (void)testDelegate {
    YOTestForwarderObject *testObject = [[YOTestForwarderObject alloc] initWithForwarder:self.forwarder];
    EXECUTE_WITHOUT_SELECTOR_CHECK([testObject performSelector:self.didCrashSelector withObject:nil]);
    XCTAssertTrue(testObject.forwarderDidCrashWebProcessCalled);
}

@end


@interface YOTestForwarderObject () <YOWKNavigationMethodForwarderDelegate>
@property (nonatomic, readonly) YOWKNavigationMethodForwarder *forwarder;
@end

@implementation YOTestForwarderObject

- (instancetype)initWithForwarder:(YOWKNavigationMethodForwarder *)forwarder {
    self = [super init];
    if (self) {
        _forwarder = forwarder;
        _forwarder.delegate = self;
    }
    return self;
}

- (void)forwarderDidCrashWebProcess:(YOWKNavigationMethodForwarder *)forwarder {
    _forwarderDidCrashWebProcessCalled = YES;
}

- (BOOL)respondsToSelector:(SEL)selector {
    return [super respondsToSelector:selector] || [self.forwarder respondsToSelector:selector];
}

- (NSMethodSignature *)methodSignatureForSelector:(SEL)selector {
    return [super methodSignatureForSelector:selector] ?: [self.forwarder methodSignatureForSelector:selector];
}

- (void)forwardInvocation:(NSInvocation *)invocation {
    if ([super respondsToSelector:invocation.selector]) {
        [super forwardInvocation:invocation];
    } else if ([self.forwarder respondsToSelector:invocation.selector]) {
        [invocation invokeWithTarget:self.forwarder];
    }
}

@end
