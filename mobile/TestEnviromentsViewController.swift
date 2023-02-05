//
//  TestEnviromentsViewController.swift
//  20/12/2019
//

#if INHOUSE
import UIKit
import UIList
import YXMail360Components

protocol TestEnviromentsDelegate: AnyObject {
    func environmentHasChaged()
}

final class TestEnviromentsViewController: DiskListViewController {
    weak var delegate: TestEnviromentsDelegate?

    private var webdavHost: String = RemoteHostsConfiguration.webdavHost.absoluteString
    private var restHost: String = RemoteHostsConfiguration.restApiHost.absoluteString
    private var docviewerOverrideHost: String? = RemoteHostsConfiguration.docviewerOverrideHost
    private var onlyOfficeOverrideHost: String? = RemoteHostsConfiguration.onlyOfficeOverrideHost
    private var serviceSheetOverrideHost: String? = RemoteHostsConfiguration.serviceSheetOverrideHost

    private static let prodWebDavHost = "https://webdav.yandex.ru/disk/"
    private static let prodRestHost = "https://cloud-api.yandex.net"

    private let stableRestHost = "https://api-stable.dst.yandex.net"
    private let currentRestHost = "https://api-current.dst.yandex.net"
    private let testWebDavHost = "https://webdav.dst.yandex.ru/disk/"

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Test environment"
        reloadSections()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        newWebdavHostValue.flatMap {
            SharedSettings.main.testWebDavServerUrl = $0
        }
        newRestHostValue.flatMap {
            SharedSettings.main.testRESTAPIServerUrl = $0
        }

        docviewerOverrideHost.flatMap {
            SharedSettings.main.testDocviewerOverrideHost = $0.isEmpty ? nil : $0
        }

        onlyOfficeOverrideHost.flatMap {
            SharedSettings.main.testOnlyOfficeOverrideHost = $0.isEmpty ? nil : $0
        }

        serviceSheetOverrideHost.flatMap {
            SharedSettings.main.testServiceSheetOverrideHost = $0.isEmpty ? nil : $0
        }

        delegate?.environmentHasChaged()
        DebugApi.onlyOffileHostOverride = SharedSettings.main.testOnlyOfficeOverrideHost
        DebugApi.docviewerHostOverride = SharedSettings.main.testDocviewerOverrideHost
        DebugApi.sheetServiceHostOverride = SharedSettings.main.testServiceSheetOverrideHost
    }

    private func reloadSections() {
        let customSection = ListSection(
            header: .string("Custom hosts"),
            footer: nil,
            items: [
                ListItemTextField(ctx: webdavHostItem),
                ListItemTextField(ctx: restHostItem),
                ListItemTextField(ctx: docviewerOverrideHostItem),
                ListItemTextField(ctx: onlyOfficeOverrideHostItem),
                ListItemTextField(ctx: serviceSheetOverrideHostItem),
            ]
        )

        let prodSection = ListSection(
            header: .string("Prod hosts"),
            footer: nil,
            items: [
                ListItemSwitcher(ctx: prodRESTSwitch),
                ListItemSwitcher(ctx: prodWebDavSwitch),
            ]
        )

        let testSection = ListSection(
            header: .string("Test hosts"),
            footer: nil,
            items: [
                ListItemSwitcher(ctx: stableRESTSwitch),
                ListItemSwitcher(ctx: currentRESTSwitch),
                ListItemSwitcher(ctx: testWebDavSwitch),
                ListItemSwitcher(ctx: testPassportSwitch),
            ]
        )

        let sections: [ListSection] = [
            customSection,
            prodSection,
            testSection,
        ]
        reload(sections: sections)
    }

    private var webdavHostItem: TextFieldItemContext {
        TextFieldItemContext(label: nil, configure: { textField in
            textField.placeholder = "WebDav url"
            textField.text = self.webdavHost
        }, didChange: { [unowned self] value in
            self.webdavHost = value
        })
    }

    private var restHostItem: TextFieldItemContext {
        TextFieldItemContext(label: nil, configure: { textField in
            textField.placeholder = "Rest url"
            textField.text = self.restHost
        }, didChange: { [unowned self] value in
            self.restHost = value
        })
    }

    private var onlyOfficeOverrideHostItem: TextFieldItemContext {
        TextFieldItemContext(label: nil, configure: { textField in
            textField.placeholder = "Host for replace disk.yandex.$tld"
            textField.text = self.onlyOfficeOverrideHost
        }, didChange: { [unowned self] value in
            self.onlyOfficeOverrideHost = value
        })
    }

    private var docviewerOverrideHostItem: TextFieldItemContext {
        TextFieldItemContext(label: nil, configure: { textField in
            textField.placeholder = "Host for replace docviewer.yandex.$tld"
            textField.text = self.docviewerOverrideHost
        }, didChange: { [unowned self] value in
            self.docviewerOverrideHost = value
        })
    }

    private var serviceSheetOverrideHostItem: TextFieldItemContext {
        TextFieldItemContext(label: nil, configure: { textField in
            textField.placeholder = "Host for replace any url in service sheet"
            textField.text = self.serviceSheetOverrideHost
        }, didChange: { [unowned self] value in
            self.serviceSheetOverrideHost = value
        })
    }

    private var prodRESTSwitch: SwitchListItemContext {
        SwitchListItemContext(
            title: "Prod REST",
            valueBlock: { [unowned self] () -> Bool in
                self.restHost == Self.prodRestHost
            },
            action: { value in
                if value {
                    self.restHost = Self.prodRestHost
                    if self.isProdEnv {
                        SharedSettings.main.testingEnvironmentEnabled = false
                    }
                    self.reloadSections()
                }
            }
        )
    }

    private var prodWebDavSwitch: SwitchListItemContext {
        SwitchListItemContext(
            title: "Prod WebDav",
            valueBlock: { [unowned self] () -> Bool in
                self.webdavHost == Self.prodWebDavHost
            },
            action: { value in
                if value {
                    self.webdavHost = Self.prodWebDavHost
                    if self.isProdEnv {
                        SharedSettings.main.testingEnvironmentEnabled = false
                    }
                    self.reloadSections()
                }
            }
        )
    }

    private var stableRESTSwitch: SwitchListItemContext {
        SwitchListItemContext(
            title: "Stable REST",
            valueBlock: { [unowned self] () -> Bool in
                self.restHost == self.stableRestHost
            },
            action: { value in
                if value {
                    self.restHost = self.stableRestHost
                    self.reloadSections()
                }
            }
        )
    }

    private var currentRESTSwitch: SwitchListItemContext {
        SwitchListItemContext(
            title: "Current REST",
            valueBlock: { [unowned self] () -> Bool in
                self.restHost == self.currentRestHost
            }, action: { value in
                if value {
                    self.restHost = self.currentRestHost
                    self.reloadSections()
                }
            }
        )
    }

    private var testWebDavSwitch: SwitchListItemContext {
        SwitchListItemContext(
            title: "Test WebDav",
            valueBlock: { [unowned self] () -> Bool in
                self.webdavHost == self.testWebDavHost
            },
            action: { value in
                if value {
                    self.webdavHost = self.testWebDavHost
                    self.reloadSections()
                }
            }
        )
    }

    private var testPassportSwitch: SwitchListItemContext {
        SwitchListItemContext(
            title: "Test passport",
            valueBlock: {
                SharedSettings.main.testingEnvironmentEnabled
            }, action: { value in
                SharedSettings.main.testingEnvironmentEnabled = value
            }
        )
    }

    private var newWebdavHostValue: URL? {
        guard !webdavHost.isEmpty
            && webdavHost != RemoteHostsConfiguration.webdavHost.absoluteString else {
            return nil
        }

        return URL(string: webdavHost)
    }

    private var newRestHostValue: URL? {
        guard !restHost.isEmpty
            && restHost != RemoteHostsConfiguration.restApiHost.absoluteString else {
            return nil
        }

        return URL(string: restHost)
    }

    private var isProdEnv: Bool {
        webdavHost == Self.prodWebDavHost && restHost == Self.prodRestHost
    }

    @objc static var prodEnvStatus: String {
        if RemoteHostsConfiguration.webdavHost.absoluteString == prodWebDavHost
            && RemoteHostsConfiguration.restApiHost.absoluteString == prodRestHost
            && !SharedSettings.main.testingEnvironmentEnabled
            && RemoteHostsConfiguration.onlyOfficeOverrideHost == nil
            && RemoteHostsConfiguration.docviewerOverrideHost == nil
            && RemoteHostsConfiguration.serviceSheetOverrideHost == nil {
            return "✅"
        }

        return "🙅🏼‍♀️"
    }
}

#endif
