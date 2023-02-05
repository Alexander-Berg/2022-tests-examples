
#import <Foundation/Foundation.h>

@class YXTReporter;

@interface YXTCommandCallbackReporter : NSObject

- (instancetype)initWithReporter:(YXTReporter *)reporter
                   commandsQueue:(NSOperationQueue *)commandsQueue;

- (void)reportResult:(NSDictionary *)result
         forObjectId:(NSString *)objectId
            callback:(NSString *)callback
          completion:(void(^)(NSDictionary *))completion;

@end
