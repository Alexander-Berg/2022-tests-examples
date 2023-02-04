package kafka

import (
	"context"
	"os"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/common/config"
	pb "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pkg/mdb"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/require"
	"github.com/yandex-cloud/go-genproto/yandex/cloud/mdb/kafka/v1"
	"github.com/yandex-cloud/go-genproto/yandex/cloud/operation"
	ycsdk "github.com/yandex-cloud/go-sdk"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/wrapperspb"
)

var (
	testTopicName = "shiva-for-test"
	testUserName  = "shiva-for-test"
	testPassword  = "shiva-for-test"
)

func skipNotCI(t *testing.T) {
	if os.Getenv("CI") == "" {
		t.Skip("Only for testing in CI")
	}
}

// TODO: May be make this test run in parallel?
func TestService_All(t *testing.T) {
	skipNotCI(t)
	test.InitTestEnv()

	clusterID := config.Str("TEST_KAFKA_CLUSTER")
	s := NewService(mdb.NewConf(), test.NewLogger(t))

	cleanup(t, s.cli, clusterID)

	t.Run("create topic", func(t *testing.T) {
		testCreateTopic(t, s, clusterID)
	})

	t.Run("update topic", func(t *testing.T) {
		testUpdateTopic(t, s, clusterID)
	})

	t.Run("create user", func(t *testing.T) {
		testCreateUser(t, s, clusterID)
	})

	t.Run("grant permission", func(t *testing.T) {
		testGrantPermission(t, s, clusterID)
	})

	t.Run("revoke permission", func(t *testing.T) {
		testRevokePermission(t, s, clusterID)
	})

	t.Run("delete user", func(t *testing.T) {
		testDeleteUser(t, s, clusterID)
	})

	t.Run("delete topic", func(t *testing.T) {
		testDeleteTopic(t, s, clusterID)
	})
}

func testCreateTopic(t *testing.T, s *Service, clusterID string) {
	err := s.CreateTopic(context.Background(), clusterID, testTopicName, &pb.KafkaOptions{
		Partitions:  4,
		RetentionMs: 86400000,
	})
	require.NoError(t, err)

	topic, err := s.cli.MDB().Kafka().Topic().Get(context.Background(), &kafka.GetTopicRequest{
		ClusterId: clusterID,
		TopicName: testTopicName,
	})
	require.NoError(t, err)
	require.Equal(t, wrapperspb.Int64(4), topic.Partitions)
	require.Equal(t, wrapperspb.Int64(2), topic.ReplicationFactor)
	require.Equal(t, wrapperspb.Int64(86400000), topic.GetTopicConfig_2_8().RetentionMs)

	//check double creation
	err = s.CreateTopic(context.Background(), clusterID, testTopicName, &pb.KafkaOptions{
		Partitions:  4,
		RetentionMs: 86400000,
	})
	require.ErrorIs(t, err, ErrAlreadyExists)
}

func testUpdateTopic(t *testing.T, s *Service, clusterID string) {
	err := s.UpdateTopic(context.Background(), clusterID, testTopicName, &pb.KafkaOptions{
		Partitions:  6,
		RetentionMs: 43200000,
	})
	require.NoError(t, err)

	topic, err := s.cli.MDB().Kafka().Topic().Get(context.Background(), &kafka.GetTopicRequest{
		ClusterId: clusterID,
		TopicName: testTopicName,
	})
	require.NoError(t, err)
	require.Equal(t, wrapperspb.Int64(6), topic.Partitions)
	require.Equal(t, wrapperspb.Int64(2), topic.ReplicationFactor)
	require.Equal(t, wrapperspb.Int64(43200000), topic.GetTopicConfig_2_8().RetentionMs)

	//check update not existing topic
	err = s.UpdateTopic(context.Background(), clusterID, testTopicName+"-not-existing", &pb.KafkaOptions{
		Partitions:  6,
		RetentionMs: 43200000,
	})
	require.ErrorIs(t, err, ErrNotFound)
}

func testDeleteTopic(t *testing.T, s *Service, clusterID string) {
	err := s.DeleteTopic(context.Background(), clusterID, testTopicName)
	require.NoError(t, err)

	_, err = s.cli.MDB().Kafka().Topic().Get(context.Background(), &kafka.GetTopicRequest{
		ClusterId: clusterID,
		TopicName: testTopicName,
	})
	require.Error(t, err)
	require.Equal(t, codes.NotFound, status.Code(err))

	//check double delete
	err = s.DeleteTopic(context.Background(), clusterID, testTopicName)
	require.ErrorIs(t, err, ErrNotFound)
}

func testCreateUser(t *testing.T, s *Service, clusterID string) {
	err := s.CreateUser(context.Background(), clusterID, testUserName, testPassword)
	require.NoError(t, err)

	user, err := s.cli.MDB().Kafka().User().Get(context.Background(), &kafka.GetUserRequest{
		ClusterId: clusterID,
		UserName:  testUserName,
	})
	require.NoError(t, err)
	require.Equal(t, []*kafka.Permission(nil), user.Permissions)

	//check double create
	err = s.CreateUser(context.Background(), clusterID, testUserName, testPassword)
	require.ErrorIs(t, err, ErrAlreadyExists)
}

func testDeleteUser(t *testing.T, s *Service, clusterID string) {
	err := s.DeleteUser(context.Background(), clusterID, testUserName)
	require.NoError(t, err)

	_, err = s.cli.MDB().Kafka().User().Get(context.Background(), &kafka.GetUserRequest{
		ClusterId: clusterID,
		UserName:  testUserName,
	})
	require.Error(t, err)
	require.Equal(t, codes.NotFound, status.Code(err))

	//check double delete
	err = s.DeleteUser(context.Background(), clusterID, testUserName)
	require.ErrorIs(t, err, ErrNotFound)
}

func testGrantPermission(t *testing.T, s *Service, clusterID string) {
	err := s.GrantPermission(context.Background(), clusterID, testTopicName, testUserName, kafka.Permission_ACCESS_ROLE_CONSUMER)
	require.NoError(t, err)

	user, err := s.cli.MDB().Kafka().User().Get(context.Background(), &kafka.GetUserRequest{
		ClusterId: clusterID,
		UserName:  testUserName,
	})
	require.NoError(t, err)
	require.EqualValues(t, []*kafka.Permission{{TopicName: testTopicName, Role: kafka.Permission_ACCESS_ROLE_CONSUMER}}, user.Permissions)

	//check double grant
	err = s.GrantPermission(context.Background(), clusterID, testTopicName, testUserName, kafka.Permission_ACCESS_ROLE_CONSUMER)
	require.ErrorIs(t, err, ErrAlreadyExists)

	//check grant for non-existing user
	err = s.GrantPermission(context.Background(), clusterID, testTopicName, testUserName+"-non-existing", kafka.Permission_ACCESS_ROLE_CONSUMER)
	require.ErrorIs(t, err, ErrNotFound)
}

func testRevokePermission(t *testing.T, s *Service, clusterID string) {
	err := s.RevokePermission(context.Background(), clusterID, testTopicName, testUserName, kafka.Permission_ACCESS_ROLE_CONSUMER)
	require.NoError(t, err)

	user, err := s.cli.MDB().Kafka().User().Get(context.Background(), &kafka.GetUserRequest{
		ClusterId: clusterID,
		UserName:  testUserName,
	})
	require.NoError(t, err)
	require.Equal(t, []*kafka.Permission(nil), user.Permissions)

	//check double revoke
	err = s.RevokePermission(context.Background(), clusterID, testTopicName, testUserName, kafka.Permission_ACCESS_ROLE_CONSUMER)
	require.ErrorIs(t, err, ErrNotFound)

	//check revoke for non-existing user
	err = s.GrantPermission(context.Background(), clusterID, testTopicName, testUserName+"-non-existing", kafka.Permission_ACCESS_ROLE_CONSUMER)
	require.ErrorIs(t, err, ErrNotFound)
}

func cleanup(t *testing.T, sdk *ycsdk.SDK, clusterID string) {
	t.Cleanup(func() {
		op, err := sdk.MDB().Kafka().Topic().Delete(context.Background(), &kafka.DeleteTopicRequest{
			ClusterId: clusterID,
			TopicName: testTopicName,
		})
		if err != nil {
			if status.Code(err) == codes.NotFound {
				return
			}
			t.Fatalf("fail delete topic when cleanup: %v", err)
		}

		wait(t, sdk, op.Id)
	})

	op, err := sdk.MDB().Kafka().Topic().Delete(context.Background(), &kafka.DeleteTopicRequest{
		ClusterId: clusterID,
		TopicName: testTopicName,
	})
	if err != nil {
		if status.Code(err) == codes.NotFound {
			return
		}
		t.Fatalf("fail delete topic when cleanup: %v", err)
	}

	wait(t, sdk, op.Id)
}

func wait(t *testing.T, sdk *ycsdk.SDK, opID string) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Minute)
	defer cancel()

	for {
		op, err := sdk.Operation().Get(ctx, &operation.GetOperationRequest{
			OperationId: opID,
		})
		if err != nil {
			t.Fatalf("fail get operation when cleanup: %v", err)
		}

		if op.Done {
			if op.GetError() != nil {
				t.Fatalf("fail delete topic when cleanup: %v", op.GetError())
			}
			return
		}

		time.Sleep(10 * time.Second)
	}
}
