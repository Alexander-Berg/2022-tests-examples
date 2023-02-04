import maps.analyzer.pylibs.envkit as envkit
import maps.analyzer.toolkit.lib as tk

from maps.analyzer.sandbox.update_historic_jams.lib.update import update_historic_jams


def test_update(ytc):
    conf = tk.config.read_json_config('/conf/update_historic_jams.json')

    update_historic_jams(
        ytc,
        **conf
    )
    assert envkit.yt.svn_revision_attribute(ytc, tk.paths.Common.HISTORIC_JAMS.value), "should set svn revision attribute"
    assert ytc.exists(tk.paths.Common.HISTORIC_JAMS.value), "Should create historic jams"
