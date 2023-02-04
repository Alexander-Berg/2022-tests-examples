#pragma once

#include "svn.h"
#include <saas/util/logging/exception_process.h>
#include <saas/util/transaction.h>
#include <library/cpp/json/json_value.h>
#include <library/cpp/charset/ci_string.h>
#include <util/generic/ptr.h>
#include <util/generic/map.h>
#include <util/generic/vector.h>
#include <util/generic/set.h>
#include <util/string/cast.h>

template <class E, class S>
class TEnumToString {
public:
    E Get(const S& s) const {
        typename TStrToE::const_iterator i = StrToE.find(s);
        return (i == StrToE.end()) ? DefaultEnumValue() : i->second;
    }

    S Get(const E& e) const {
        typename TEToStr::const_iterator i = EToStr.find(e);
        VERIFY_WITH_LOG(i != EToStr.end(), "Invalid usage");
        return i->second;
    }
    virtual ~TEnumToString() {}
protected:
    TEnumToString() {};
    void Register(const E& e, const S& s) {
        StrToE[s] = e;
        EToStr[e] = s;
    }
    virtual E DefaultEnumValue() const = 0;
private:
    typedef TMap<S, E> TStrToE;
    typedef TMap<E, S> TEToStr;
    TStrToE StrToE;
    TEToStr EToStr;
};

template <class T, class G, class L>
class TGuardedValue {
public:
    TGuardedValue(T& value, const L& lock)
        : Value(value), Guards(1, new G(lock)) {}
    T& Get() {return Value;}
    const T& Get() const {return Value;}
    void AddLock(const L& lock) const {Guards.push_back(new G(lock));}
private:
    T& Value;
    mutable TVector<TSimpleSharedPtr<G> > Guards;
};

template <class T, class G, class L>
class TGuardedTMPtr: public TAtomicSharedPtr<TGuardedValue<T, G, L>> {
public:
    typedef TAtomicSharedPtr<TGuardedValue<T, G, L>> TBase;
    TGuardedTMPtr(T& value, const L& lock)
        : TBase(new TGuardedValue<T, G, L>(value, lock)) {}

    inline T* operator-> () const {
        return &(TBase::Get()->Get());
    }

    inline T& operator* () const {
        return *(TBase::Get()->Get());
    }

};

template <class T>
class TReaderPtr: public TGuardedTMPtr<T, TGuardIncompatibleAction, ITransaction> {
public:
    TReaderPtr(T& value, const ITransaction& transaction)
        : TGuardedTMPtr<T, TGuardIncompatibleAction, ITransaction> (value, transaction) {}
};

template <class DataType, class ChildType, class ChildKeyType>
class TDataHolder : public TNonCopyable {
public:
    typedef DataType TData;
    typedef ChildType TChild;
    typedef ChildKeyType TChildKey;
    typedef TAtomicSharedPtr<ChildType> TChildPtr;
    typedef TMap<ChildKeyType, TChildPtr> TChildren;

public:
    virtual TReaderPtr<TChildren> GetChildren() {
        return TReaderPtr<TChildren>(Children, Transaction);
    }

    virtual TReaderPtr<const TChildren> GetChildren() const {
        return TReaderPtr<const TChildren>(Children, Transaction);
    }

    TChildPtr UpdateChild(const typename TChild::TData& childData) {
        TGuardTransaction g(Transaction);
        return UpdateChildSafe(childData);
    }

    TVector<TChildPtr> UpdateChildren(const TVector<typename TChild::TData>& children, bool erase) {
        TVector<TChildPtr> result;
        if (children.empty())
            return result;
        TGuardTransaction g(Transaction);
        TSet<TChildKey> keys;
        for (typename TVector<typename TChild::TData>::const_iterator i = children.begin(), e = children.end(); i != e; ++i)
            keys.insert(GetChildKey(*i));
        for (typename TChildren::iterator i = Children.begin(); erase && i != Children.end();) {
            if (keys.find(i->first) == keys.end()) {
                Children.erase(i);
                i = Children.begin();
            } else
                ++i;
        }

        for (typename TVector<typename TChild::TData>::const_iterator i = children.begin(), e = children.end(); i != e; ++i)
            result.push_back(UpdateChildSafe(*i));
        AfterUpdateChildren();
        return result;
    }

    void Invalidate() {
        TGuardTransaction g(Transaction);
        AfterUpdateChildren();
    }

    TData& GetData() {return Data;}
    const TData& GetData() const {return Data;}

    virtual ~TDataHolder() {}

protected:
    TChildren Children;

private:
    virtual ChildKeyType GetChildKey(const typename TChild::TData& childData) = 0;
    virtual void AfterUpdateChildren() {}

    TChildPtr UpdateChildSafe(const typename TChild::TData& childData) {
        TChildKey key = GetChildKey(childData);
        typename TChildren::iterator i = Children.find(key);
        if (i == Children.end())
            i = Children.insert(typename TChildren::value_type(key, new TChild)).first;
        i->second->GetData() = childData;
        return i->second;
    }

    ITransaction Transaction;
    TData Data;
};

struct TTestExecutionData {
public:
    enum TStatus {OK, FAILD, COREDUMP, RUN, WAIT, UNKNOWN, NOT_BUILD};
    TTestExecutionData ()
        : TaskId(0)
        , Status(UNKNOWN)
        , TaskInfo(NJson::JSON_MAP)
        , Result(0)
        , FailInfo("")
        , DataCache("")
    {}

public:
    TRevisionInfo Revision;
    ui64 TaskId;
    TStatus Status;
    NJson::TJsonValue TaskInfo;
    i64 Result;
    TString FailInfo;
    TString DataCache;
    TSimpleSharedPtr<ITransaction> Transaction;
};

struct TTestData {
public:
    TCiString Name;
    TCiString HistoryPattern;
    ui64 HistoryLastRevision;
    bool IsOn;
    bool IsExecuted;
    ui64 MaxWeatherCount;
};

struct TDbData {
    enum TType {TEST_ENVIRONMENT, LUNAPARK, AQUA, ROBOT};
    enum TStatus {NOT_STARTED, STARTED, UNKNOWN};
    TDbData()
        : Status(UNKNOWN)
        , Type(TEST_ENVIRONMENT)
    {}

    TCiString Name;
    TStatus Status;
    TType Type;
};

template<>
inline TString ToString<TDbData::TStatus>(const TDbData::TStatus& t) {
    switch(t) {
        case TDbData::NOT_STARTED: return "not_started";
        case TDbData::STARTED: return "started";
        case TDbData::UNKNOWN: return "unknown";
        default:
            VERIFY_WITH_LOG(false, "invalid usage");
    }
    return TString();
}

struct TModelData {

};

struct TTestExecution {
public:
    typedef TTestExecutionData TData;
    TData& GetData() {return Data;}
    const TData& GetData() const {return Data;}
    const ITransaction& GetTransaction() const {return Transaction;}

private:
    TData Data;
    ITransaction Transaction;
};

class TTest : public TDataHolder <TTestData, TTestExecution, ui64> {
public:
    enum TStatus {OK, FAILED, UNKNOWN};
    TTest()
        : LastUnstableRevision(0)
        , WeatherCountStarts(0)
        , WeatherCountOks(0)
        , Status(UNKNOWN)
        , Result(0)
        , TaskId(0)
    {}
    virtual TReaderPtr<TChildren> GetChildren() {
        TReaderPtr<TChildren> result = TDataHolder <TTestData, TTestExecution, ui64>::GetChildren();
        if (!!BrokenIn)
            result.Get()->AddLock(BrokenIn->GetTransaction());
        if (!!LastFinished)
            result.Get()->AddLock(LastFinished->GetTransaction());
        return result;
    }

    virtual TReaderPtr<const TChildren> GetChildren() const {
        TReaderPtr<const TChildren> result = TDataHolder <TTestData, TTestExecution, ui64>::GetChildren();
        if (!!BrokenIn)
            result.Get()->AddLock(BrokenIn->GetTransaction());
        if (!!LastFinished)
            result.Get()->AddLock(LastFinished->GetTransaction());
        return result;
    }
    ui64 LastUnstableRevision;
    TChildPtr BrokenIn;
    TChildPtr LastFinished;
    ui64 WeatherCountStarts;
    ui64 WeatherCountOks;
    TStatus Status;
    i64 Result;
    ui64 TaskId;
    bool NotFound() const;
    TAtomicSharedPtr<TSet<TTest*> > Builds;

private:
    virtual void AfterUpdateChildren();
    virtual ui64 GetChildKey(const TChild::TData& childData) {
        return 1000000 * childData.Revision.Revision + childData.TaskId;
    }
};

class TDataBase : public TDataHolder <TDbData, TTest, TCiString> {
    virtual TCiString GetChildKey(const TChild::TData& childData) {
        return childData.Name;
    }
    virtual void AfterUpdateChildren();
public:
    TDataBase()
        : BuildStatus(TTest::UNKNOWN)
    {}

    TTest::TChildPtr LastFinished;
    TTest::TChildPtr LastBuild;
    TTest::TChildPtr BuildBrokenIn;
    TTest::TStatus BuildStatus;
};

class TModel : public TDataHolder <TModelData, TDataBase, TCiString> {
    virtual TCiString GetChildKey(const TChild::TData& childData) {
        return childData.Name;
    }
};
