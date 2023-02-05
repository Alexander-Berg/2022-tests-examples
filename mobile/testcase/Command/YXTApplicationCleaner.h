
#import <Foundation/Foundation.h>

extern void yxt_cleanCache(void);
extern void yxt_cleanKeychain(void);

@interface YXTApplicationCleaner : NSObject

+ (void)cleanCache;
+ (void)cleanKeychain;

@end
