package client

import (
	"context"
	"math/rand"
	"strconv"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const (
	tvmType    = 47
	name       = "danevgeTest1"
	supplierID = 14
	serviceID  = 1265
	externalID = "2021147"
	resourceID = int64(6226691)
	consumerID = int64(20618051)
)

func TestClientImpl_GetResourceByExternalID(t *testing.T) {
	test.InitTestEnv()
	log := test.NewLogger(t)
	c := NewClient(log)
	result, err := c.ResourceByExternalID(tvmType, externalID)
	require.NoError(t, err)
	assert.Equal(t, name, result.Name)
	assert.Equal(t, externalID, result.ExternalID)
	assert.Equal(t, resourceID, result.ResourceID)
}

func TestClientImpl_GetResourceByName(t *testing.T) {
	test.InitTestEnv()
	log := test.NewLogger(t)
	c := NewClient(log)
	result, err := c.ResourceByName(tvmType, supplierID, 1265, name)
	require.NoError(t, err)
	require.NotNil(t, result)
	assert.Equal(t, name, result.Name)
	assert.Equal(t, externalID, result.ExternalID)
	assert.Equal(t, resourceID, result.ResourceID)
}

func TestMetaInfo(t *testing.T) {
	test.InitTestEnv()
	log := test.NewLogger(t)
	c := NewClient(log)
	result, err := c.MetaInfo(consumerID)
	require.NoError(t, err)
	assert.True(t, result["secret_uuid"] != "")
	assert.True(t, result["version_uuid"] != "")
	assert.True(t, result["vault_link"] != "")
}

func TestClient_ConsumerID(t *testing.T) {
	test.InitTestEnv()
	log := test.NewLogger(t)
	c := NewClient(log)
	result, err := c.ConsumerID(serviceID, externalID)
	require.NoError(t, err)
	assert.Equal(t, consumerID, result)
}

func TestClient_NewResourceDeleteResource(t *testing.T) {
	t.Skip("turn on after https://st.yandex-team.ru/PASSP-27423")

	test.InitTestEnv()
	log := test.NewLogger(t)
	c := NewClient(log)

	rand.Seed(time.Now().UnixNano())
	resourceName := name + "-" + strconv.Itoa(int(rand.Int31()))

	ctx, cancel := context.WithTimeout(context.Background(), time.Minute)
	defer cancel()

	extId, err := c.NewResource(ctx, serviceID, tvmType, map[string]string{"resource_name": resourceName})
	log.Infof("got external id: %s", extId)
	require.NoError(t, err)

	resource, err := c.ResourceByName(tvmType, supplierID, 1265, resourceName)
	require.NoError(t, err)

	consumerId, err := c.ConsumerID(serviceID, resource.ExternalID)
	require.NoError(t, err)

	//need cookie
	err = c.DeleteResource(consumerId)
	require.NoError(t, err)
}

func TestClient_DeleteOldSecret(t *testing.T) {
	t.Skip("turn on after https://st.yandex-team.ru/PASSP-27423")

	test.InitTestEnv()
	log := test.NewLogger(t)
	c := NewClient(log)

	c.RecreateSecret(consumerID)

	result, err := c.DeleteOldSecret(consumerID)
	require.NoError(t, err)

	require.NoError(t, err)
	assert.True(t, result["secret_uuid"] != "")
	assert.True(t, result["version_uuid"] != "")
	assert.True(t, result["vault_link"] != "")
	assert.True(t, result["old_client_secret"] == "")
}

func TestClientImpl_RecreateSecret(t *testing.T) {
	t.Skip("turn on after https://st.yandex-team.ru/PASSP-27423")

	test.InitTestEnv()
	log := test.NewLogger(t)
	c := NewClient(log)

	_, err := c.DeleteOldSecret(consumerID)
	require.NoError(t, err)

	result, err := c.RecreateSecret(consumerID)
	require.NoError(t, err)
	assert.True(t, result["secret_uuid"] != "")
	assert.True(t, result["version_uuid"] != "")
	assert.True(t, result["vault_link"] != "")
	assert.True(t, result["old_client_secret"] != "")
}
