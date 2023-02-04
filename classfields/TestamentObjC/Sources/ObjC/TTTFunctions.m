//
//  Created by Alexey Aleshkov on 16.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

#import "TTTFunctions.h"
#import "TTTDisposition.h"
#import "TestamentObjC/TestamentObjC-Swift.h"

void TTTAssertImpl(BOOL condition, TTTDisposition disposition, NSString *_Nonnull message, const char *file, const char *function, int line)
{
    if (condition) {
        return;
    }

    [TTTAssertion assertWithDisposition:disposition message:message file:file function:function line:line];
}
