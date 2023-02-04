TestTree = function (node, pages) {
    // json на основе которого строим дерево.
    var json = {"Все тесты": {}};

    for (var i = 0, l = pages.length; i < l; i++) {
        var parent = json["Все тесты"],
            units = pages[i].split("/");
        for (var j = 0, k = units.length; j < k; j++) {
            parent.childs = parent.childs || {};
            if (!parent.childs[units[j]]) {
                parent.childs[units[j]] = {};
                if (j == k - 1) {
                    parent.childs[units[j]].test = pages[i];
                }
            }
            parent = parent.childs[units[j]];
        }
    }

    var id = 1,
        createChilds = function (json) {
            var result = [];
            for (var name in json) {
                if (json.hasOwnProperty(name)) {
                    result.push({
                        data: name,
                        attributes : {
                            id: id++,
                            test : json[name].test
                        },
                        children: createChilds(json[name].childs)
                    });
                }
            }
            return result;
        },
        tree = createChilds(json);

    node.tree({
        rules : {
            multiple: "on"
        },
        ui : {
            theme_name : "checkbox",
            selected_parent_close: false
        },
        plugins : {
            checkbox : { },
            cookie : {
                prefix : "jstree_",
                types : {
                    selected    : true,
                    open        : false
                }
            }
        },
        data : {
            type : "json",
            opts : {
                "static" : tree
            }
        }
    });
};

TestTree.prototype = {
    getCheckedPages: function () {
        var checkedNodes = $.tree.plugins.checkbox.get_checked(),
            result = [];
        $.each(checkedNodes, function () {
            var testURL = $(this).attr('test');
            if(testURL) { result.push(testURL); }
        });

        return result;
    }
};
