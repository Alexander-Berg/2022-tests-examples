#include "db.h"

#include <solomon/libs/cpp/conf_db/model/agent_config.h>
#include <solomon/libs/cpp/conf_db/model/cluster_config.h>
#include <solomon/libs/cpp/conf_db/model/service_config.h>
#include <solomon/libs/cpp/conf_db/model/project_config.h>
#include <solomon/libs/cpp/conf_db/model/provider_config.h>
#include <solomon/libs/cpp/conf_db/model/shard_config.h>

#include <library/cpp/threading/future/future.h>

#include <util/generic/algorithm.h>
#include <util/generic/hash.h>
#include <util/system/spinlock.h>

#include <utility>

using namespace NSolomon::NDb;
using namespace NSolomon::NDb::NModel;
using namespace NThreading;

namespace {
    struct TCompoundKey {
        TCompoundKey() = default;
        TCompoundKey(TString id, TString pid)
            : ProjectId{std::move(pid)}
            , Id{std::move(id)}
        {
        }

        TString ProjectId;
        TString Id;

        bool operator==(const TCompoundKey& other) const noexcept{
            return other.ProjectId == ProjectId && other.Id == Id;
        }
    };

    template <typename TConf>
    TCompoundKey GetKey(const TConf& c) {
        return {c.Id, c.ProjectId};
    }

    TString GetKey(const TProjectConfig& c) {
        return c.Id;
    }

    TString GetKey(const TProviderConfig& c) {
        return c.Id;
    }
} // namespace

template <>
struct THash<TCompoundKey> {
    size_t operator()(const TCompoundKey& key) const noexcept {
        THash<TString> s;
        return s(key.Id) ^ s(key.ProjectId);
    }
};

namespace NSolomon::NTesting {
namespace {
    template <typename T, typename... TArgs>
    auto MakeError(TArgs&&... args) {
        return MakeErrorFuture<T>(
            std::make_exception_ptr(
                ((yexception() << ... << args))
            )
        );
    }

    template <typename TKey, typename TValue>
    class TMockDaoBase {
    public:
        TAsyncVoid InsertImpl(TValue val) {
            auto k = GetKey(val);
            auto g = Guard(Lock_);
            if (Values_.find(k) != Values_.end()) {
                return MakeError<void>("PK violation");
            }

            Values_[k] = val;
            return MakeFuture();
        }

        template <typename... TArgs>
        TAsyncVoid DeleteImpl(TArgs&&... args) {
            TKey k{args...};
            auto g = Guard(Lock_);
            Values_.erase(k);
            return MakeFuture();
        }

        TFuture<TVector<TValue>> GetAllImpl() {
            auto g = Guard(Lock_);
            TVector<TValue> result;
            for (auto&& [k, v]: Values_) {
                result.push_back(v);
            }

            return MakeFuture(result);
        }

        template <typename... TArgs>
        TFuture<std::optional<TValue>> GetOneImpl(TArgs&&... args) {
            TKey k{args...};
            return MakeFuture(Get(k));
        }

        std::optional<TValue> Get(TKey k) {
            auto g = Guard(Lock_);
            if (auto* v = Values_.FindPtr(k)) {
                return std::optional<TValue>(*v);
            }

            return std::nullopt;
        }

        template <typename TFunc>
        std::optional<TValue> GetBy(TFunc&& pred) {
            auto g = Guard(Lock_);
            auto it = FindIf(Values_.begin(), Values_.end(), pred);

            if (it == Values_.end()) {
                return std::nullopt;
            }

            return it->second;
        }

    protected:
        TAdaptiveLock Lock_;
        THashMap<TKey, TValue> Values_;
    };

    class TMockClusterDao: public IClusterConfigDao, private TMockDaoBase<TCompoundKey, TClusterConfig> {
    public:
        TAsyncVoid CreateTable() override { return MakeFuture(); }
        TAsyncVoid DropTable() override { return MakeFuture(); }

        TAsyncVoid Insert(const NModel::TClusterConfig& model) override {
            return InsertImpl(model);
        }

        TAsyncVoid Delete(const TString& clusterId, const TString& projectId) override {
            return DeleteImpl(clusterId, projectId);
        }

        TAsyncClusterConfig GetByProject(const TString& clusterId, const TString& projectId) override {
            return GetOneImpl(clusterId, projectId);
        }

        TAsyncClusterConfig GetByName(const TString& clusterName, const TString& projectId) override {
            return MakeFuture(GetBy([&] (auto&& model) {
                return model.second.Name == clusterName && model.second.ProjectId == projectId;
            }));
        }

        TAsyncClusterConfigs GetAll() override {
            return GetAllImpl();
        }
    };

    class TMockShardDao: public IShardConfigDao, private TMockDaoBase<TCompoundKey, TShardConfig> {
    public:
        TAsyncVoid CreateTable() override { return MakeFuture(); }
        TAsyncVoid DropTable()  override { return MakeFuture(); }

        TAsyncVoid Insert(const NModel::TShardConfig& model) override {
            return InsertImpl(model);
        }

        TAsyncVoid Delete(const TString& shardId, const TString& projectId) override {
            return DeleteImpl(shardId, projectId);
        }

        TAsyncShardConfigs GetAll() override {
            return GetAllImpl();
        }

        TAsyncShardConfig GetById(const TString& shardId, const TString& projectId) override {
            return GetOneImpl(shardId, projectId);
        }
    };

    class TMockServiceDao: public IServiceConfigDao, private TMockDaoBase<TCompoundKey, TServiceConfig> {
    public:
        TAsyncVoid CreateTable() override { return MakeFuture(); }
        TAsyncVoid DropTable()  override { return MakeFuture(); }

        TAsyncVoid Insert(const NModel::TServiceConfig& model) override {
            return InsertImpl(model);
        }

        TAsyncVoid Delete(const TString& serviceId, const TString& projectId) override {
            return DeleteImpl(serviceId, projectId);
        }

        TAsyncServiceConfig GetByProject(const TString& serviceId, const TString& projectId) override {
            return GetOneImpl(serviceId, projectId);
        }

        TAsyncServiceConfig GetByName(const TString& serviceName, const TString& projectId) override {
            return MakeFuture(GetBy([&] (auto&& model) {
                return model.second.Name == serviceName && model.second.ProjectId == projectId;
            }));
        }

        TAsyncServiceConfigs GetAll() override {
            return GetAllImpl();
        }
    };

    class TMockProjectDao: public IProjectConfigDao, private TMockDaoBase<TString, TProjectConfig> {
    public:
        TAsyncVoid CreateTable() override { return MakeFuture(); }
        TAsyncVoid DropTable()  override { return MakeFuture(); }


        TAsyncBool Exists(const TString& projectId) override {
            return MakeFuture(Get(projectId).has_value());
        }

        TAsyncVoid Insert(const NModel::TProjectConfig& model) override {
            return InsertImpl(model);
        }

        TAsyncVoid Delete(const TString& projectId) override {
            return DeleteImpl(projectId);
        }

        TAsyncProjectConfig GetById(const TString& projectId) override {
            return GetOneImpl(projectId);
        }

        TAsyncProjectConfigs GetAll() override {
            return GetAllImpl();
        }
    };

    class TMockProviderDao: public IProviderConfigDao, private TMockDaoBase<TString, TProviderConfig> {
    public:
        TAsyncVoid CreateTable() override { return MakeFuture(); }
        TAsyncVoid DropTable()  override { return MakeFuture(); }


        TAsyncBool Exists(const TString& providerId) override {
            return MakeFuture(Get(providerId).has_value());
        }

        TAsyncVoid Insert(const NModel::TProviderConfig& model) override {
            return InsertImpl(model);
        }

        TAsyncVoid Delete(const TString& providerId) override {
            return DeleteImpl(providerId);
        }

        TAsyncProviderConfig GetById(const TString& providerId) override {
            return GetOneImpl(providerId);
        }

        TAsyncProviderConfigs GetAll() override {
            return GetAllImpl();
        }
    };

    class TMockConnection: public IDbConnection {
    public:
        IClusterConfigDaoPtr CreateClusterDao(const TString&) override {
            return new TMockClusterDao;
        }

        IShardConfigDaoPtr CreateShardDao(const TShardTables&) override {
            return new TMockShardDao;
        }

        IServiceConfigDaoPtr CreateServiceDao(const TString&) override {
            return new TMockServiceDao;
        }

        IAgentConfigDaoPtr CreateAgentDao(const TString&) override {
            Y_FAIL("not implemented");
        }

        IProjectConfigDaoPtr CreateProjectDao(const TString&) override {
            return new TMockProjectDao;
        }

        IProviderConfigDaoPtr CreateProviderDao(const TString&) override {
            return new TMockProviderDao;
        }
    };
} // namespace
    IDbConnectionPtr CreateMockConnection() {
        return new TMockConnection;
    }
} // namespace NSolomon::NTesting
