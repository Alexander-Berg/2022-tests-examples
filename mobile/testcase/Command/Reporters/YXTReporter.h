
#import <Foundation/Foundation.h>

@interface YXTReporter : NSObject

- (instancetype)initWithBaseURL:(NSString *)baseUrl
                   networkQueue:(NSOperationQueue *)networkQueue
                        timeout:(NSTimeInterval)timeout;

- (void)reportResult:(NSDictionary *)result
 withQueryParameters:(NSDictionary *)queryParameters
          completion:(void(^)(NSDictionary *))completion;

@end
