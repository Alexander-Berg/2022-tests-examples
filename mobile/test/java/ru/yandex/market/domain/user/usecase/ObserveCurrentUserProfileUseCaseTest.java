package ru.yandex.market.domain.user.usecase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.domain.user.health.CurrentUserProfileHealthFacade;
import ru.yandex.market.domain.user.model.UserProfile;
import ru.yandex.market.domain.user.model.UserProfileTestFactory;
import ru.yandex.market.domain.user.repository.UserProfileRepository;
import ru.yandex.market.optional.Optional;
import ru.yandex.market.utils.Observables;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ObserveCurrentUserProfileUseCaseTest {

    @Mock
    UserProfileRepository userProfileRepository;

    @Mock
    CurrentUserProfileHealthFacade currentUserProfileHealthFacade;

    @InjectMocks
    ObserveCurrentUserProfileUseCase observeCurrentUserProfileUseCase;

    @Test
    public void testGetUserInfo_Ok() {
        final UserProfile userProfile = UserProfileTestFactory.create();
        when(userProfileRepository.getCurrentUserProfileStream())
                .thenReturn(Observables.stream(Optional.Companion.of(userProfile)));

        observeCurrentUserProfileUseCase.execute()
                .test()
                .assertValue(Optional.Companion.of(userProfile));
    }
}
