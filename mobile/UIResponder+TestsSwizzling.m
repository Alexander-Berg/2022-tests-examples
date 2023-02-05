//
//  NSLocale+Swizzling.m
//  YandexMapsTests
//
//  Created by Nikolay Lihogrud on 19.01.18.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <objc/runtime.h>

@interface UIResponder (Keyboard)
@end

@implementation UIResponder (Keyboard)

+ (void)load
{
    Method originalTextInputMode = class_getInstanceMethod(self, @selector(textInputMode));
    Method swizzledTextInputMode = class_getInstanceMethod(self, @selector(swizzled_textInputMode));
    method_exchangeImplementations(originalTextInputMode, swizzledTextInputMode);

}

- (UITextInputMode *)swizzled_textInputMode {
    UITextInputMode *res;
    for (UITextInputMode *mode in [UITextInputMode activeInputModes]) {
        if ([[mode primaryLanguage] containsString:@"en"]) {
            res = mode;
        }
    }
    return res;
}


@end


