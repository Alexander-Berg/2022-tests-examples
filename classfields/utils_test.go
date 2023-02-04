package utils

import (
	"testing"
)

func TestClusterName(t *testing.T) {
	testClusterName := "autoparts.indexer-test-int.slb.vertis.yandex.net"
	expectedClusterName := "autoparts_indexer"

	gotClusterName := ClusterName(testClusterName)
	if expectedClusterName != gotClusterName {
		t.Errorf("Failed to get expected ClusterName '%s' from '%s'. Result is '%s'", expectedClusterName, testClusterName, gotClusterName)
	}
}

func TestRouteName(t *testing.T) {
	testRouteName := "autoparts.indexer-test-int.slb.vertis.yandex.net"
	expectedRouteName := "autoparts.indexer"

	gotRouteName := RouteName(testRouteName)
	if expectedRouteName != gotRouteName {
		t.Errorf("Failed to get expected ClusterName '%s' from '%s'. Result is '%s'", expectedRouteName, testRouteName, gotRouteName)
	}
}
