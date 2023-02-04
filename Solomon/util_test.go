package main

import (
	"strconv"
	"strings"
	"testing"
)

func assertEquals(expected, actual string, t *testing.T) {
	if expected != actual {
		t.Error("Expected \n" + expected + "\n but got\n" + actual + "\n")
	}
}

func arrayGet(model interface{}, idx int) interface{} {
	return model.([]interface{})[idx]
}

func dictGet(model interface{}, key string) interface{} {
	return model.(map[string]interface{})[key]
}

func getByPath(model interface{}, path string) interface{} {
	if path == "" {
		return model
	}
	if path[0] == '.' {
		next := strings.IndexAny(path[1:], ".[")
		if next == -1 {
			return dictGet(model, path[1:])
		} else {
			return getByPath(dictGet(model, path[1:next+1]), path[next+1:])
		}
	}
	if path[0] == '[' {
		end := strings.IndexByte(path, ']')
		idx, _ := strconv.Atoi(path[1:end])
		return getByPath(arrayGet(model, idx), path[end+1:])
	}
	panic("malformed path: " + path)
}

func TestJsonTraverse(t *testing.T) {
	json := `{"firstName": "Иван", "lastName": "Иванов", "address": {"streetAddress": "Московское ш., 101, кв.101",
       "city": "Ленинград", "postalCode": 101101}, "phoneNumbers": ["812 123-1234", "916 123-4567"]}`
	model, _ := deserialize([]byte(json))
	expected := map[string]string{
		".firstName":             "Иван",
		".lastName":              "Иванов",
		".address.city":          "Ленинград",
		".address.streetAddress": "Московское ш., 101, кв.101",
		".phoneNumbers[0]":       "812 123-1234",
		".phoneNumbers[1]":       "916 123-4567",
	}

	visited := map[string]bool{}

	applyMap(model, "", func(path string, value string) string {
		assertEquals(expected[path], value, t)
		visited[path] = true
		return value
	})

	for k := range expected {
		if _, has := visited[k]; !has {
			t.Errorf("Key " + k + " was not visited!")
		}
	}
}
