
#import <Foundation/Foundation.h>

@interface YXTTestCaseRequest : NSObject

+ (void)requestCaseWithUrl:(NSString *)requestUrl
                  callback:(void(^)(NSDictionary *))callback;

@end
