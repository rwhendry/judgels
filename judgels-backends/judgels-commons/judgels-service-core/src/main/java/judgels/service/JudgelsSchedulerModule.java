package judgels.service;

import dagger.Module;
import dagger.Provides;
import io.dropwizard.setup.Environment;
import javax.inject.Singleton;

@Module
public class JudgelsSchedulerModule {
    private final Environment environment;

    public JudgelsSchedulerModule(Environment environment) {
        this.environment = environment;
    }

    @Provides
    @Singleton
    public JudgelsScheduler scheduler() {
        return new JudgelsScheduler(environment.lifecycle());
    }
}
