//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import func Darwin.sys.sysctl.sysctl
import var Darwin.sys.sysctl.CTL_KERN
import var Darwin.sys.sysctl.KERN_PROC
import var Darwin.sys.sysctl.KERN_PROC_PID
import struct Darwin.sys.sysctl.kinfo_proc
import var Darwin.sys.proc.P_TRACED
import func Darwin.POSIX.unistd.getpid
import func Darwin.C.signal.raise
import var Darwin.sys.signal.SIGINT

public final class BreakpointInterrupter: EventInterrupterProtocol {
    public init() {
    }

    // MARK: - EventInterrupterProtocol

    public func triggerInterrupt(_ event: Event) {
        if Self.isDebuggerAttached() {
            // triggers non-lock "breakpoint"
            raise(SIGINT)
        }
    }

    // MARK: - Private

    private static func isDebuggerAttached() -> Bool {
        var debuggerIsAttached = false

        var name: [Int32] = [CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid()]
        var info: kinfo_proc = .init()
        var infoSize = MemoryLayout<kinfo_proc>.size

        let success = name.withUnsafeMutableBytes({ nameBytePointer -> Bool in
            guard let nameBytes = nameBytePointer.bindMemory(to: Int32.self).baseAddress else { return false }
            let result = -1 != sysctl(nameBytes, 4, &info, &infoSize, nil, 0)
            return result
        })

        if !success {
            debuggerIsAttached = false
        }

        if !debuggerIsAttached && (info.kp_proc.p_flag & P_TRACED) != 0 {
            debuggerIsAttached = true
        }

        return debuggerIsAttached
    }
}
