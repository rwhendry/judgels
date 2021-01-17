package org.iatoki.judgels.sandalphon.controllers.api.internal;

import static judgels.service.ServiceUtils.checkFound;

import javax.inject.Inject;
import javax.inject.Singleton;
import judgels.sandalphon.api.problem.Problem;
import org.iatoki.judgels.jophiel.controllers.Secured;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.problem.base.ProblemService;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;

@Singleton
@Security.Authenticated(Secured.class)
public final class InternalProblemStatementAPIController extends AbstractJudgelsAPIController {

    private final ProblemService problemService;

    @Inject
    public InternalProblemStatementAPIController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @Transactional(readOnly = true)
    public Result renderMediaById(long problemId, String mediaFilename) {
        Problem problem = checkFound(problemService.findProblemById(problemId));
        String mediaUrl = problemService.getStatementMediaFileURL(IdentityUtils.getUserJid(), problem.getJid(), mediaFilename);

        return okAsImage(mediaUrl);
    }

    @Transactional(readOnly = true)
    public Result downloadStatementMediaFile(long id, String filename) {
        Problem problem = checkFound(problemService.findProblemById(id));
        String mediaUrl = problemService.getStatementMediaFileURL(IdentityUtils.getUserJid(), problem.getJid(), filename);

        return okAsDownload(mediaUrl);
    }
}
