//  Created by Denis Malykh on 06.09.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation

final class MainTableViewController: UITableViewController {

    private lazy var demoComponentGraph: DemoComponentGraph = {
        DemoComponentGraph()
    }()

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if let cell = tableView.cellForRow(at: indexPath) {
            if cell.reuseIdentifier == "dc" {
                let vc = demoComponentGraph.makeViewController()
                navigationController?.pushViewController(vc, animated: true)
            }
        }
    }
}
