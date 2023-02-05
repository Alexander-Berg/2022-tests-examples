
#import <Foundation/Foundation.h>

@class YXTCommandInput;

@interface YXTCommandArgumentsProvider : NSObject

+ (NSString *)objectIdForParameters:(NSDictionary *)parameters;
+ (NSString *)objectIdForInput:(YXTCommandInput *)input;
+ (NSString *)listenerIdForInput:(YXTCommandInput *)input;
+ (NSString *)logStringForInput:(YXTCommandInput *)input;

@end
