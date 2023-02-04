function logMemoryLeak (obj, objName) {
    var leak = ml(obj, objName);
    //console.log('event Objects', i)
    console.log('Number of objects in ' + objName, leak.numberObjs)
    console.log('Deleted objects in ' + objName, {deletedObjs: leak.deletedObjs})
    console.log('Added objects in ' + objName, {addedObjs: leak.addedObjs})
    console.log('Undeleted objs in ' + objName, { undeletedObjs: leak.undeletedObjs })
}

function ml (ownerObj, ownerObjName) {
    ml.prevObjs = ml.prevObjs || [];
    ml.prevObjsNames = ml.prevObjsNames || [];
    ownerObjName = ownerObjName || '';
    var i = 0, // количество объектов
        j = 0,
        objs = [ownerObj],
        objsNames = [ownerObjName],
        addedObjs = [], // Добавленные объекты
        deletedObjs = [], // Удаленные объекты
        addedObjsNames = [],
        deletedObjsNames = [];

    while (j < objs.length) {
        var obj = objs[j],
            name = objsNames[j++];
        for (var k in obj) {
            try {
                if (!obj[k].nodeType && typeof obj[k] == 'object' && obj[k] !== null) {
                    i++;
                    if (objs.indexOf(obj[k]) == -1) {
                        objs.push(obj[k]);
                        objsNames.push(name + '.' + k);
                    }
                    if (ml.prevObjs.indexOf(obj[k]) == -1) {
                        var o = {},
                            n = name + '.' + k;
                        o[n] = obj[k];
                        addedObjs.push(o);
                        addedObjsNames.push(n);
                    }
                }
            }
            catch (e) {

            }
        }
    }

    for (var z = 0, l = ml.prevObjs.length; z < l; z++) {
        var obj = ml.prevObjs[z];
        if (objs.indexOf(obj) == -1) {
            var o = {},
                n = ml.prevObjsNames[z];
            o[n] = obj;
            deletedObjs.push(o);
            deletedObjsNames.push(n);
        }
    }

    ml.prevObjs = objs;
    ml.prevObjsNames = objsNames;

    var undeletedObjs = []; // Неудаленные объекты (удаленные минус не удаленные по ключам)

    for (var z = 0, l = addedObjs.length; z < l; z++) {
        var name = addedObjsNames[z];
        if (deletedObjsNames.indexOf(name) == -1) {
            undeletedObjs.push(addedObjs[z]);
        }
    }

    return {
        numberObjs: i,
        addedObjs: addedObjs,
        deletedObjs: deletedObjs,
        undeletedObjs: undeletedObjs
    };
}