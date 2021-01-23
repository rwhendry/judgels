package org.iatoki.judgels.sandalphon.problem.base;

import com.google.inject.ImplementedBy;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import judgels.fs.FileInfo;
import judgels.persistence.api.Page;
import judgels.sandalphon.api.problem.Problem;
import judgels.sandalphon.api.problem.ProblemStatement;
import judgels.sandalphon.api.problem.ProblemType;
import judgels.sandalphon.api.problem.partner.ProblemPartner;
import judgels.sandalphon.api.problem.partner.ProblemPartnerChildConfig;
import judgels.sandalphon.api.problem.partner.ProblemPartnerConfig;
import org.iatoki.judgels.GitCommit;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;

@ImplementedBy(ProblemServiceImpl.class)
public interface ProblemService {

    Problem createProblem(ProblemType type, String slug, String additionalNote, String initialLanguageCode) throws IOException;

    boolean problemExistsByJid(String problemJid);

    boolean problemExistsBySlug(String slug);

    Optional<Problem> findProblemById(long problemId);

    Problem findProblemByJid(String problemJid);

    Problem findProblemBySlug(String slug);

    boolean isUserPartnerForProblem(String problemJid, String userJid);

    void createProblemPartner(String problemJid, String userJid, ProblemPartnerConfig baseConfig, ProblemPartnerChildConfig childConfig);

    void updateProblemPartner(long problemPartnerId, ProblemPartnerConfig baseConfig, ProblemPartnerChildConfig childConfig);

    Page<ProblemPartner> getPageOfProblemPartners(String problemJid, long pageIndex, long pageSize, String orderBy, String orderDir);

    Optional<ProblemPartner> findProblemPartnerById(long problemPartnerId);

    ProblemPartner findProblemPartnerByProblemJidAndPartnerJid(String problemJid, String partnerJid);

    void updateProblem(String problemJid, String slug, String additionalNote);

    Page<Problem> getPageOfProblems(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString, String userJid, boolean isAdmin);

    Map<String, StatementLanguageStatus> getAvailableLanguages(String userJid, String problemJid) throws IOException;

    void addLanguage(String userJid, String problemJid, String languageCode) throws IOException;

    void enableLanguage(String userJid, String problemJid, String languageCode) throws IOException;

    void disableLanguage(String userJid, String problemJid, String languageCode) throws IOException;

    void makeDefaultLanguage(String userJid, String problemJid, String languageCode) throws IOException;

    String getDefaultLanguage(String userJid, String problemJid) throws IOException;

    ProblemStatement getStatement(String userJid, String problemJid, String languageCode) throws IOException;

    Map<String, String> getTitlesByLanguage(String userJid, String problemJid) throws IOException;

    void updateStatement(String userJid, String problemJid, String languageCode, ProblemStatement statement) throws IOException;

    void uploadStatementMediaFile(String userJid, String problemJid, File mediaFile, String filename) throws IOException;

    void uploadStatementMediaFileZipped(String userJid, String problemJid, File mediaFileZipped) throws IOException;

    List<FileInfo> getStatementMediaFiles(String userJid, String problemJid);

    String getStatementMediaFileURL(String userJid, String problemJid, String filename);

    List<GitCommit> getVersions(String userJid, String problemJid);

    void initRepository(String userJid, String problemJid);

    boolean userCloneExists(String userJid, String problemJid);

    void createUserCloneIfNotExists(String userJid, String problemJid);

    boolean commitThenMergeUserClone(String userJid, String problemJid, String title, String text);

    boolean updateUserClone(String userJid, String problemJid);

    boolean pushUserClone(String userJid, String problemJid);

    boolean fetchUserClone(String userJid, String problemJid);

    void discardUserClone(String userJid, String problemJid) throws IOException;

    void restore(String problemJid, String hash);
}
