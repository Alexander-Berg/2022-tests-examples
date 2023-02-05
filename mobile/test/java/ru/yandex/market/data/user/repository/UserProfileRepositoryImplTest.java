package ru.yandex.market.data.user.repository;

import com.annimon.stream.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import ru.yandex.market.data.user.network.UserFapiClient;
import ru.yandex.market.data.user.mapper.UserProfileMapper;
import ru.yandex.market.domain.auth.repository.AuthRepository;
import ru.yandex.market.domain.user.model.UserProfile;
import ru.yandex.market.domain.user.model.UserProfileTestFactory;
import ru.yandex.market.common.schedulers.TimerScheduler;
import ru.yandex.market.rx.SimpleSingleObserver;
import ru.yandex.market.utils.Observables;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserProfileRepositoryImplTest {

    @Mock
    CurrentUserPreferenceDao currentUserPreferenceDao;

    @Mock
    UserProfileMapper userProfileMapper;

    @Mock
    AuthRepository authenticationRepository;

    @Mock
    UserProfileAffectedDataStore userProfileAffectedDataStore;

    @Mock
    UserProfileJobExecutor userProfileJobExecutor;

    private UserProfileRepositoryImpl repository;

    @Before
    public void setUp() {
        repository = new UserProfileRepositoryImpl(
                userProfileJobExecutor,
                currentUserPreferenceDao,
                userProfileMapper,
                authenticationRepository,
                userProfileAffectedDataStore,
                new TimerScheduler(Schedulers.trampoline())
        );

        when(userProfileAffectedDataStore.getAffectedEventsStream()).thenReturn(Observable.never());
    }

    @Test
    public void getCurrentUserProfileStream_returnsEmptyOptionalUserIsNotLoggedIn() {
        when(authenticationRepository.getCurrentAccountStream())
                .thenReturn(Observables.stream(Optional.empty()));
        when(currentUserPreferenceDao.setCompletable(any())).thenReturn(Completable.complete());

        repository.getCurrentUserProfileStream()
                .test()
                .assertValue(ru.yandex.market.optional.Optional.Companion.empty());
    }

    @Test
    public void getCurrentUserProfileStream_clearsUserProfileCacheWhenUserIsNotLoggedIn() {
        when(authenticationRepository.getCurrentAccountStream())
                .thenReturn(Observables.stream(Optional.empty()));
        when(currentUserPreferenceDao.setCompletable(any())).thenReturn(Completable.complete());

        repository.getCurrentUserProfileStream().subscribe();

        verify(currentUserPreferenceDao).setCompletable(null);
    }

    @Test
    public void synchronizeCurrentUserProfile_clearsCacheIfThereIsNoProfile() {
        when(authenticationRepository.getCurrentAccountStream()).thenReturn(Observables.stream(Optional.empty()));
        when(currentUserPreferenceDao.setCompletable(any())).thenReturn(Completable.complete());

        repository.synchronizeCurrentUserProfile().subscribe(new SimpleSingleObserver<>());

        verify(currentUserPreferenceDao).setCompletable(null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getCachedCurrentUserProfileStream_returnsUserProfilesStreamFromCache() {
        final UserProfile firstProfile = UserProfileTestFactory.create(1);
        final UserProfile secondProfile = UserProfileTestFactory.create(2);
        when(currentUserPreferenceDao.observeValues()).thenReturn(
                Observables.stream(
                        ru.yandex.market.optional.Optional.Companion.of(firstProfile),
                        ru.yandex.market.optional.Optional.Companion.of(secondProfile)
                )
        );

        repository.getCachedCurrentUserProfileStream()
                .test()
                .assertValues(
                        ru.yandex.market.optional.Optional.Companion.of(firstProfile),
                        ru.yandex.market.optional.Optional.Companion.of(secondProfile)
                );
    }

    @Test
    public void getCachedCurrentUserProfileStream_distinctItemsUntilChanged() {
        final UserProfile firstProfile = UserProfileTestFactory.create();
        final UserProfile secondProfile = UserProfileTestFactory.create();
        //noinspection unchecked
        when(currentUserPreferenceDao.observeValues()).thenReturn(
                Observables.stream(
                        ru.yandex.market.optional.Optional.Companion.of(firstProfile),
                        ru.yandex.market.optional.Optional.Companion.of(secondProfile)
                )
        );

        repository.getCachedCurrentUserProfileStream()
                .test()
                .assertValue(ru.yandex.market.optional.Optional.Companion.of(firstProfile));
    }
}