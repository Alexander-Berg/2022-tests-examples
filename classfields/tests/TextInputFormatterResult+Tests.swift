//
//  Created by Alexey Aleshkov on 05.10.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import struct YRENumberTextFormatter.CarriageText
import struct YRENumberTextFormatter.TextInputFormatterResult

extension TextInputFormatterResult {
    init(text: String, carriagePosition: Int, unformattedText: String) {
        let carriageText = CarriageText(text: text, carriagePosition: carriagePosition)
        self.init(formatted: carriageText, unformattedText: unformattedText)
    }
}
