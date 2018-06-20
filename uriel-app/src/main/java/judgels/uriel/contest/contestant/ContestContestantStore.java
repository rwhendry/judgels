package judgels.uriel.contest.contestant;

import static judgels.uriel.UrielCacheUtils.SEPARATOR;
import static judgels.uriel.UrielCacheUtils.getShortDuration;
import static judgels.uriel.api.contest.contestant.ContestContestantStatus.APPROVED;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import judgels.uriel.api.contest.contestant.ContestContestant;
import judgels.uriel.persistence.ContestContestantDao;
import judgels.uriel.persistence.ContestContestantModel;

@Singleton
public class ContestContestantStore {
    private final ContestContestantDao contestantDao;
    private final Clock clock;

    private final Cache<String, ContestContestant> contestantCache;

    @Inject
    public ContestContestantStore(ContestContestantDao contestantDao, Clock clock) {
        this.contestantDao = contestantDao;
        this.clock = clock;

        this.contestantCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(getShortDuration())
                .build();
    }

    public void upsertContestant(String contestJid, String userJid) {
        Optional<ContestContestantModel> maybeModel = contestantDao.selectByContestJidAndUserJid(contestJid, userJid);
        if (maybeModel.isPresent()) {
            ContestContestantModel model = maybeModel.get();
            toModel(contestJid, userJid, model);
            contestantDao.update(model);
        } else {
            ContestContestantModel model = new ContestContestantModel();
            toModel(contestJid, userJid, model);
            contestantDao.insert(model);
        }
    }

    public void removeContestant(String contestJid, String userJid) {
        contestantDao.selectByContestJidAndUserJid(contestJid, userJid).ifPresent(contestantDao::delete);
    }

    public Optional<ContestContestant> findContestant(String contestJid, String userJid) {
        return Optional.ofNullable(contestantCache.get(
                contestJid + SEPARATOR + userJid,
                $ -> findContestantUncached(contestJid, userJid)));
    }

    public ContestContestant findContestantUncached(String contestJid, String userJid) {
        return contestantDao.selectByContestJidAndUserJid(contestJid, userJid)
                .map(ContestContestantStore::fromModel)
                .orElse(null);
    }

    public void startVirtualContest(String contestJid, String userJid) {
        contestantDao.selectByContestJidAndUserJid(contestJid, userJid).ifPresent(model -> {
            model.contestStartTime = clock.instant();
            contestantDao.update(model);

            contestantCache.invalidate(contestJid + SEPARATOR + userJid);
        });
    }

    public long getContestantsCount(String contestJid) {
        return contestantDao.selectCountByContestJid(contestJid);
    }

    public Set<String> getContestants(String contestJid) {
        return contestantDao.selectAllByContestJid(contestJid)
                .stream()
                .map(model -> model.userJid)
                .collect(Collectors.toSet());
    }

    private static void toModel(String contestJid, String userJid, ContestContestantModel model) {
        model.contestJid = contestJid;
        model.userJid = userJid;
        model.status = APPROVED.name();
    }

    private static ContestContestant fromModel(ContestContestantModel model) {
        return new ContestContestant.Builder()
                .userJid(model.userJid)
                .contestStartTime(Optional.ofNullable(model.contestStartTime))
                .build();
    }
}
