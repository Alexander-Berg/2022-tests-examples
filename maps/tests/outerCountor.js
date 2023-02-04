function getOuterCountor (ymaps, countors) {

	//need util.math.vector,graphics.CSG,graphics.Path

	var Vector = ymaps.util.math.vector,
		CGSSieve = ymaps.graphics.csg.sieve,
		Path = ymaps.graphics.Path,
		buffers = [],
		indexes = [],
		sieveIndexes = [],
		paths = [],
		startIndex = 0;


	for (var i = 0, l = countors.length; i < l; ++i) {
		buffers.push.apply(buffers, countors[i]);
		paths.push(Path.fromArray(countors[i]));
		var si = [];
		for (var j = 0, jl = countors[i].length; j < jl; ++j) {
			indexes.push(startIndex);
			si.push(startIndex);
			startIndex++;
		}

		indexes.push(false);
		//indexes.push(false);

		sieveIndexes.push(si);
	}

	var VBO = {
			buffer: {
				vertexes: buffers
			},
			indexes: indexes
		},
		sVBO = [];
	for (var i = 0, l = sieveIndexes.length; i < l; ++i) {
		sVBO.push({
			buffer: {
				vertexes: buffers
			},
			indexes: sieveIndexes[i]
		});
	}


	var edgeData = CGSSieve.meta(sVBO);

	var points = [],
		leafs = {},
		leafNodes = {},
		pointBuffer = VBO.buffer.vertexes,
		indexBuffer = VBO.indexes,
		indexesLen = indexBuffer.length,
		vboIndex = VBO.buffer._index;

	function findIntersections (edge) {
		var point11 = pointBuffer[edge[0]],
			point12 = pointBuffer[edge[1]],
			crosses = edgeData.indexLineForPoints(point11, point12, [edge[0], vboIndex]),
			result = [];

		if (crosses.length) {
			for (var i = 0, l = crosses.length; i < l; ++i) {
				//проверка что это не наше продолжение
				if (crosses[i][0] != edge[1] && crosses[i][1] != edge[0]) {
					var point21 = pointBuffer[crosses[i][0]],
						point22 = pointBuffer[crosses[i][1]];
					var cross = Vector.lineIntersectionPoint(point11, point12, point21, point22)
					if (cross) {
						//проверяем что находимся не концах граней
						var e = 1e-8, e1 = 1 - e;
						if (cross.a > e && cross.a < e1 && cross.b > e && cross.b < e1) {
							result.push([Vector.length2([cross.point[0] - point11[0], cross.point[1] - point11[1]]), cross.point, crosses[i][0]]);
						}
					}
				}
			}

		}
		return [point11, point12, result];
	}

	//генерация ИД листа основаная на ребрах
	function leafId (a, b) {
		var index = Math.min(a, b) + "-" + Math.max(a, b);
		if (!(index in leafNodes)) {
			leafNodes[index] = [];
		}
		return leafNodes[index];
	}


	var hasMiddlePoint = 0;
	var lastPoint = 0;

	for (var i = 0; i < indexesLen - 1; i++) {
		if (indexBuffer[i] !== false && indexBuffer[i + 1] != false) {

			var edge = [indexBuffer[i], indexBuffer[i + 1]],
				middlePoints = findIntersections(edge);

			lastPoint = middlePoints[1];

			points.push(middlePoints[0]);
			if (middlePoints[2] && middlePoints[2].length) {
				var middle = middlePoints[2],
					pindex = points.length;
				middle.sort(function (a, b) {
					return a[0] - b[0];
				});
				for (var j = 0, lj = middle.length; j < lj; j++) {
					//создаем лист пересечения двух ребер и кладем туда номера вершин
					var leaf = leafId(indexBuffer[i], middle[j][2]);
					leaf.push(pindex);
					leafs[pindex] = leaf;
					pindex++;
					points.push(middle[j][1]);
					hasMiddlePoint++;
				}
			}

		} else if (lastPoint) {

			points.push(lastPoint);
			points.push(false);
			lastPoint = 0;
		}

	}

	if (lastPoint) {
		points.push(lastPoint);
	}

	console.log(points);

	// Если не было самопересечений - функция не имеет смысла
	if (!hasMiddlePoint) {
		return {};
	}

	var trails = {},
		normals = {},
		trailLength = points.length;

	//создаем отрезки и определяем нормали
	var startId=-1;
	for (var i = 0, l = points.length - 1; i < l; i++) {
		var trail = [points[i], points[i + 1]];
		if (trail[0] !== false && trail[1] !== false) {
			if(startId<0)startId=i;
			var vector = Vector.norm(Vector.sub(trail[1], trail[0]));
			trails[i]=(trail);
			normals[i]=(vector);
		}else{
			if(startId>=0){
				var leaf = leafId(i-2, i-1);
				leaf.push(startId);
				leafs[i] = leaf;
			}
			startId=-1;
		}
	}

	console.log('>', trails);


	function glue (index, sign) {
		var outerCountor = [],
			lastIndex = index,
			start = trails[index][0];


		while (true) {
			var lastTrail = trails[index],
				ways = [];

			if (lastTrail == 1) {
			}

			if (!lastTrail) {
				console.log('err');
				break;
			//	throw new Error('graphics.csg.internalShapes: возможно полигон не замкнут');
			}else{

			delete trails[index];
			outerCountor.push(lastTrail[0]);

			if (Vector.equals(lastTrail[1], start)) {
				break;
			}
			}

			//если нет записи листа - значит это не точка пересечения
			if (!leafs[index + 1]) {
				ways = [index + 1];
			} else {
				var ldata = leafs[index + 1];
				//вершина обозначеная в листе может входить в три отрезка -1,0,+1
				for (var j = 0, jl = ldata.length; j < jl; j++) {
					for (var lind = ldata[j] - 1; lind < ldata[j] + 1; lind++) {
						if (index != lind && trails[lind]) {
							if (Vector.equals(lastTrail[1], trails[lind][0])) {
								ways.push(lind);
							}
						}
					}
				}
			}

			//путей нет, выход
			if (ways.length == 0) {
				outerCountor.push(lastTrail[1]);
				break;
			} else if (ways.length == 1) {
				//путь только один
				index = ways[0];
			} else {
				//в зависимости от знака ищем идеальный путь продолжения обхода

				var min = [-1, sign > 0 ? 2 : -2];
				for (var j = 0, lj = ways.length; j < lj; ++j) {
					var cos = Vector.dot(normals[index], normals[ways[j]]);
					if ((sign > 0 && cos < min[1]) || (sign < 0 && cos > min[1] && cos < 1)) {
						min = [j, cos];
					}
				}
				if (1 || min[1] >= 0) {
					index = ways[min[0]];
				} else {
					outerCountor.push(lastTrail[1]);
					break;
				}
			}
			lastIndex = index;
		}
		return outerCountor;
	}

	return glue(0, 1);

}