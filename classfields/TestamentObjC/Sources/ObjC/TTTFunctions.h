//
//  Created by Alexey Aleshkov on 16.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef NS_CLOSED_ENUM(NSUInteger, TTTDisposition);

void TTTAssertImpl(BOOL condition, TTTDisposition disposition, NSString *_Nonnull message, const char *_Nonnull file, const char *_Nonnull function, int line);

#define _TTTAssert(condition, level, message) TTTAssertImpl((condition), (level), (message), __FILE__, __FUNCTION__, __LINE__)

#define TTTAssert(condition, format, ...) _TTTAssert((condition), TTTDispositionBody, ([[NSString alloc] initWithFormat:format, ##__VA_ARGS__]))
#define TTTParameterAssert(condition) _TTTAssert((condition), TTTDispositionParameter, ([[NSString alloc] initWithFormat:@"Invalid parameter not satisfying: %@", @#condition]))

NS_ASSUME_NONNULL_END
