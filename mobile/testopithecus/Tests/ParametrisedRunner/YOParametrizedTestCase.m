//
//  YOParametrizedTestCase.m
//  YandexMobileMailAutoTests
//
//  Created by Artem Zoshchuk on 17/01/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//


#import "YOParametrizedTestCase.h"

@interface YOQuickSelectorWrapper ()
@property(nonatomic) SEL selector;
@end

@implementation YOQuickSelectorWrapper

- (instancetype)initWithSelector:(SEL)selector {
    self = [super init];
    _selector = selector;
    return self;
}

@end

@implementation YOParametrizedTestCase
+ (NSArray<NSInvocation *> *)testInvocations {
    // Получаем список тестовых методов из подкласса
    NSArray<YOQuickSelectorWrapper *> *wrappers = [self yo_testMethodSelectors];
    NSMutableArray<NSInvocation *> *invocations = [NSMutableArray arrayWithCapacity:wrappers.count];

    // Оборачиваем список тестовых методов в NSInvocation, как того требует api XCTest
    for (YOQuickSelectorWrapper *wrapper in wrappers) {
        SEL selector = wrapper.selector;
        NSMethodSignature *signature = [self instanceMethodSignatureForSelector:selector];
        NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:signature];
        invocation.selector = selector;

        [invocations addObject:invocation];
    }

    return invocations;
}

+ (NSArray<YOQuickSelectorWrapper *> *)yo_testMethodSelectors {
    return @[];
}
@end
