
#import <Foundation/Foundation.h>

@class YXTCommandInput;

@interface YXTTestCommandCompleteHelper : NSObject

+ (void(^)(NSError *error))onFailureBlockForInput:(YXTCommandInput *)input;
+ (NSDictionary *)resultWithValue:(id)value;
+ (NSDictionary *)resultSuccess;
+ (NSDictionary *)resultFailureWithError:(NSError *)error;

@end
