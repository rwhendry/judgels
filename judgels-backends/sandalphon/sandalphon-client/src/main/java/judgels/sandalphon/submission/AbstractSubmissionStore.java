package judgels.sandalphon.submission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import judgels.gabriel.api.GradingResultDetails;
import judgels.persistence.api.OrderDir;
import judgels.persistence.api.Page;
import judgels.persistence.api.SelectionOptions;
import judgels.sandalphon.api.submission.Grading;
import judgels.sandalphon.api.submission.Submission;
import judgels.sandalphon.api.submission.SubmissionData;
import judgels.sandalphon.persistence.AbstractGradingModel;
import judgels.sandalphon.persistence.AbstractSubmissionModel;
import judgels.sandalphon.persistence.BaseGradingDao;
import judgels.sandalphon.persistence.BaseSubmissionDao;

public abstract class AbstractSubmissionStore<SM extends AbstractSubmissionModel, GM extends AbstractGradingModel> {
    private static final int MAX_DOWNLOAD_SUBMISSIONS_LIMIT = 100;

    private final BaseSubmissionDao<SM> submissionDao;
    private final BaseGradingDao<GM> gradingDao;
    private final ObjectMapper mapper;

    public AbstractSubmissionStore(
            BaseSubmissionDao<SM> submissionDao,
            BaseGradingDao<GM> gradingDao,
            ObjectMapper mapper) {

        this.submissionDao = submissionDao;
        this.gradingDao = gradingDao;
        this.mapper = mapper;
    }

    public Optional<Submission> getSubmissionById(long submissionId) {
        return submissionDao.select(submissionId).map(model -> {
            Optional<GM> gradingModel = gradingDao.selectLatestBySubmissionJid(model.jid);
            return submissionFromModels(model, gradingModel.orElse(null));
        });
    }

    public Page<Submission> getSubmissionsForDownload(
            String containerJid,
            Optional<String> userJid,
            Optional<String> problemJid,
            Optional<Long> lastSubmissionId,
            Optional<Integer> limit) {

        SelectionOptions options = new SelectionOptions.Builder()
                .from(SelectionOptions.DEFAULT_PAGED)
                .pageSize(Math.min(MAX_DOWNLOAD_SUBMISSIONS_LIMIT, limit.orElse(MAX_DOWNLOAD_SUBMISSIONS_LIMIT)))
                .orderDir(OrderDir.ASC)
                .build();

        return getSubmissions(containerJid, userJid, problemJid, lastSubmissionId, options);
    }

    public Page<Submission> getSubmissions(
            String containerJid,
            Optional<String> userJid,
            Optional<String> problemJid,
            Optional<Integer> page) {

        SelectionOptions.Builder options = new SelectionOptions.Builder().from(SelectionOptions.DEFAULT_PAGED);
        page.ifPresent(options::page);

        return getSubmissions(containerJid, userJid, problemJid, Optional.empty(), options.build());
    }

    private Page<Submission> getSubmissions(
            String containerJid,
            Optional<String> userJid,
            Optional<String> problemJid,
            Optional<Long> lastSubmissionId,
            SelectionOptions options) {

        Page<SM> submissionModels =
                submissionDao.selectPaged(containerJid, userJid, problemJid, lastSubmissionId, options);
        Set<String> submissionJids = submissionModels.getPage().stream().map(m -> m.jid).collect(Collectors.toSet());
        Map<String, GM> gradingModels = gradingDao.selectAllLatestBySubmissionJids(submissionJids);

        return submissionModels.mapPage(p ->
                Lists.transform(p, sm -> submissionFromModels(sm, gradingModels.get(sm.jid))));
    }

    public Submission createSubmission(SubmissionData data, String gradingEngine) {
        SM model = submissionDao.createSubmissionModel();
        toModel(data, gradingEngine, model);
        return submissionFromModels(submissionDao.insert(model), null);
    }

    public String createGrading(Submission submission) {
        GM model = gradingDao.createGradingModel();
        model.submissionJid = submission.getJid();
        model.verdictCode = "?";
        model.verdictName = "Pending";
        model.score = 0;

        return gradingDao.insert(model).jid;
    }

    private Submission submissionFromModels(SM model, GM gradingModel) {
        return new Submission.Builder()
                .id(model.id)
                .jid(model.jid)
                .userJid(model.createdBy)
                .problemJid(model.problemJid)
                .containerJid(model.containerJid)
                .gradingEngine(model.gradingEngine)
                .gradingLanguage(model.gradingLanguage)
                .time(model.createdAt)
                .latestGrading(gradingFromModel(gradingModel))
                .build();
    }

    private void toModel(SubmissionData data, String gradingEngine, SM model) {
        model.problemJid = data.getProblemJid();
        model.containerJid = data.getContainerJid();
        model.gradingEngine = gradingEngine;
        model.gradingLanguage = data.getGradingLanguage();
    }

    private Optional<Grading> gradingFromModel(@Nullable GM model) {
        if (model == null) {
            return Optional.empty();
        }

        Grading.Builder grading = new Grading.Builder()
                .id(model.id)
                .jid(model.jid)
                .verdict(parseVerdictCode(model.verdictCode, model.verdictName))
                .score(model.score);

        if (model.details != null) {
            RawGradingResultDetails raw;
            try {
                raw = mapper.readValue(model.details, RawGradingResultDetails.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Map<String, byte[]> compilationOutputs = raw.getCompilationOutputs().entrySet()
                    .stream()
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getBytes()));

            grading.details(new GradingResultDetails.Builder()
                    .compilationOutputs(compilationOutputs)
                    .testDataResults(raw.getTestDataResults())
                    .subtaskResults(raw.getSubtaskResults())
                    .build());
        }

        return Optional.of(grading.build());
    }

    // TODO(fushar): this is a workaround to parse the worst subtask verdict in a subtasked problem.
    // We should refactor this, by updating Gabriel.
    private static String parseVerdictCode(String code, String name) {
        Pattern pattern = Pattern.compile("^.*\\(worst: (.*)\\)$");
        Matcher matcher = pattern.matcher(name);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return code;
    }
}