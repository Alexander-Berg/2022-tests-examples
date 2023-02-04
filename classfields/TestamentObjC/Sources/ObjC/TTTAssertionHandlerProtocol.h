//
//  Created by Alexey Aleshkov on 18.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_CLOSED_ENUM(NSUInteger, TTTDisposition);

@protocol TTTAssertionHandlerProtocol <NSObject>

- (void)handleFailureWithDisposition:(TTTDisposition)handleDisposition message:(NSString *_Nonnull)message file:(char const *_Nonnull)handleFile function:(char const *_Nonnull)handleFunction line:(NSUInteger)line NS_SWIFT_NAME(handleFailure(disposition:message:file:function:line:));

@end

NS_ASSUME_NONNULL_END
