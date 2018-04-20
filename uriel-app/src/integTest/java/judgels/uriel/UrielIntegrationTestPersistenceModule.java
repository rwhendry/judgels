package judgels.uriel;

import dagger.Module;
import dagger.Provides;
import java.time.Clock;
import javax.inject.Singleton;
import judgels.persistence.ActorProvider;
import judgels.persistence.FixedActorProvider;
import judgels.persistence.FixedClock;

@Module
public class UrielIntegrationTestPersistenceModule {
    private UrielIntegrationTestPersistenceModule() {}

    @Provides
    @Singleton
    static Clock clock() {
        return new FixedClock();
    }

    @Provides
    @Singleton
    static ActorProvider actorProvider() {
        return new FixedActorProvider();
    }
}
