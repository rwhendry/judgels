package org.iatoki.judgels.sandalphon.controllers.api.internal;

import static judgels.service.ServiceUtils.checkFound;

import javax.inject.Inject;
import javax.inject.Singleton;
import judgels.sandalphon.api.lesson.Lesson;
import org.iatoki.judgels.jophiel.controllers.Secured;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.lesson.LessonService;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;

@Singleton
@Security.Authenticated(Secured.class)
public final class InternalLessonStatementAPIController extends AbstractJudgelsAPIController {

    private final LessonService lessonService;

    @Inject
    public InternalLessonStatementAPIController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @Transactional(readOnly = true)
    public Result renderMediaById(long lessonId, String mediaFilename) {
        Lesson lesson = checkFound(lessonService.findLessonById(lessonId));
        String mediaUrl = lessonService.getStatementMediaFileURL(IdentityUtils.getUserJid(), lesson.getJid(), mediaFilename);

        return okAsImage(mediaUrl);
    }

    @Transactional(readOnly = true)
    public Result downloadStatementMediaFile(long id, String filename) {
        Lesson lesson = checkFound(lessonService.findLessonById(id));
        String mediaUrl = lessonService.getStatementMediaFileURL(IdentityUtils.getUserJid(), lesson.getJid(), filename);

        return okAsDownload(mediaUrl);
    }
}
