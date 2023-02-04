from time import monotonic


class UserRepo:
    """
    Facade for TUS client and local users cache, that handles all interactions between them.
    """

    def __init__(self, tus_client, user_cache):
        self.tus_client = tus_client
        self.user_cache = user_cache

    def getUser(self, tags, rental_duration=200, tus_lock_duration=2000):
        """
        Get user with specific tags. Lookup is performed in cache, and if no suitable cached user
        was found, a new user will be requested from TUS.

        Method is raising RuntimeError if TUS request fails.

        Arguments:
        tags - returned client will satisfy all given tags, but will not match them exactly
        rental_duration - returned client will not be unlocked by TUS during this time period
                          (even when cached client was returned)
        tus_lock_duration - during that time period client will be locked in TUS for other
        """
        if rental_duration > tus_lock_duration:
            raise ValueError(
                f"rental_duration should be less or equal to tus_lock_duration: rental_duration={rental_duration}, tus_lock_duration={tus_lock_duration}"
            )

        if not isinstance(tags, set):
            raise ValueError(f"Expected tags to be a set, but got tags={tags}")

        cached_user = self.user_cache.getUser(tags, rental_duration)
        if cached_user is not None:
            return cached_user

        tags_str = ",".join(map(str, tags)) if len(tags) > 0 else None

        tus_lock_start_ts = monotonic()
        tus_user = self.tus_client.getUser(tus_lock_duration, tags_str)
        if tus_user is None:
            if "plus" in tags:
                raise RuntimeError("Unable to get user with paid subscription from TUS")
            else:
                self.tus_client.createUser(tags_str)
                tus_lock_start_ts = monotonic()
                tus_user = self.tus_client.getUser(tus_lock_duration, tags_str)
                if tus_user is None:
                    raise RuntimeError("Unable to get user from TUS")

        self.user_cache.putUser(tus_user, tags, tus_lock_start_ts, tus_lock_duration)
        return tus_user

    def unlockCachedUsers(self):
        """
        Unlock all cached users in TUS. Should be called on teardown.
        """
        for cached_user in self.user_cache:
            if cached_user.lock_start_ts + cached_user.lock_duration > monotonic():
                self.tus_client.unlockUser(cached_user.user)


class UserCacheEntry:
    """
    Hashable data class, that holds TUS user and lock info.
    Users are compared by UID.
    """

    def __init__(self, user, tags, lock_start_ts, lock_duration):
        self.user = user
        self.tags = tags
        self.lock_start_ts = lock_start_ts
        self.lock_duration = lock_duration

    def uid(self):
        return self.user["account"]["uid"]

    def __hash__(self):
        return hash(self.uid())

    def __eq__(self, other):
        if isinstance(other, UserCacheEntry):
            return self.uid() == other.uid()
        return False


class UserCache:
    """
    Maintains cache structure and lets to acquire cached users by tags and lock duration.
    """

    def __init__(self):
        self.all_users = set()
        self.tags_to_users = dict()

    def getUser(self, tags, rental_duration):
        """
        Get cached user with specific tags that is guaranteed to be locked in TUS during rental_duration.
        If several cached users satisfy tags, the most recently requested will be returned.
        """

        suitable_users = self.tags_to_users.get(frozenset(tags))

        if suitable_users is None or len(suitable_users) == 0:
            return None

        most_recent = max(suitable_users, key=lambda u: u.lock_start_ts)

        lock_duration_left = most_recent.lock_duration - (monotonic() - most_recent.lock_start_ts)
        if lock_duration_left < rental_duration:
            return None

        return most_recent.user

    def putUser(self, user, tags, lock_start_ts, lock_duration):
        """
        Put user into cache.
        """
        frozen_tags = frozenset(tags)
        cache_entry = UserCacheEntry(user, frozen_tags, lock_start_ts, lock_duration)
        self.tags_to_users.setdefault(frozen_tags, set()).add(cache_entry)
        self.all_users.add(cache_entry)

    def __iter__(self):
        return self.all_users.__iter__()
