//
//  YOParametrizedTestCase.h
//  YandexMobileMailAutoTests
//
//  Created by Artem Zoshchuk on 17/01/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

#import <XCTest/XCTest.h>

/// SEL это указатель на C структуру поэтому мы не можем положить его в NSArray.
/// Вместо этого мы используем этот класс как враппер.
@interface YOQuickSelectorWrapper : NSObject
- (instancetype)initWithSelector:(SEL)selector;
@end

@interface YOParametrizedTestCase : XCTestCase
// Список тестовых методов для вызова.
+ (NSArray<YOQuickSelectorWrapper *> *)yo_testMethodSelectors;
@end
