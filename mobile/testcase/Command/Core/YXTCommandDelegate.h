
#import <Foundation/Foundation.h>

@class YXTCommandInput;

@interface YXTCommandDelegate : NSObject

+ (instancetype)delegateWithCommandInput:(YXTCommandInput *)commandInput;

- (void)completeWithCallback:(NSString *)callback;
- (void)completeWithCallback:(NSString *)callback
                  completion:(void(^)(NSDictionary *))completion;
- (void)completeWithCallback:(NSString *)callback
                      result:(NSDictionary *)result;
- (void)completeWithCallback:(NSString *)callback
                      result:(NSDictionary *)result
                  completion:(void(^)(NSDictionary *))completion;

@end
