//
//  Created by Alexey Aleshkov on 16.12.2020.
//  Copyright © 2021 Yandex. All rights reserved.
//

@import Foundation;
#import "TTTObjcAssertionHandler.h"

@implementation TTTObjcAssertionHandler

- (void)handleFailureWithDisposition:(TTTDisposition)handleDisposition message:(NSString *_Nonnull)message file:(char const *_Nonnull)handleFile function:(char const *_Nonnull)handleFunction line:(NSUInteger)line
{
    const __auto_type file = [[NSString alloc] initWithUTF8String:handleFile];
    const __auto_type function = [[NSString alloc] initWithUTF8String:handleFunction];
    [NSAssertionHandler.currentHandler handleFailureInFunction:function file:file lineNumber:line description:@"%@", message];
}

@end
