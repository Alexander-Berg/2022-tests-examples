//
//  UIImage+MainBundle.m
//  YandexGeoToolboxTestApp
//
//  Created by Konstantin Kiselev on 04/05/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

#import "UIImage+MainBundle.h"
#import "DummyTestTargetClass.h"

#import <objc/runtime.h>

@implementation UIImage (MainBundle)

+ (void)load {
    [self swizzleClassMethod];
}

+ (void)swizzleClassMethod {
    SEL originalSelector = @selector(imageNamed:);
    SEL newSelector = @selector(swizzled_imageNamed:);
    Method origMethod = class_getClassMethod([UIImage class], originalSelector);
    Method newMethod = class_getClassMethod([UIImage class], newSelector);
    Class class = object_getClass([UIImage class]);
    if (class_addMethod(class, originalSelector, method_getImplementation(newMethod), method_getTypeEncoding(newMethod))) {
        class_replaceMethod(class, newSelector, method_getImplementation(origMethod), method_getTypeEncoding(origMethod));
    } else {
        method_exchangeImplementations(origMethod, newMethod);
    }
}

+ (UIImage *)swizzled_imageNamed:(NSString *)imageName {
    return [self imageNamed:imageName extension:@"png"];
}

+ (UIImage *)imageNamed:(NSString *)imageName extension:(NSString *)extension {
    NSBundle *bundle = [NSBundle bundleForClass:[DummyTestTargetClass class]];//Any class from test bundle can be used. NOTE: Do not use UIImage, as it is from different bundle
    NSString *imagePath = [bundle pathForResource:imageName ofType:extension];
    return [UIImage imageWithContentsOfFile:imagePath];
}

@end
