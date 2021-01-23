package org.iatoki.judgels.sandalphon.problem.bundle.item;

import static judgels.service.ServiceUtils.checkFound;

import java.io.IOException;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import judgels.persistence.api.Page;
import judgels.sandalphon.api.problem.Problem;
import org.apache.commons.lang3.EnumUtils;
import org.iatoki.judgels.play.actor.ActorChecker;
import org.iatoki.judgels.play.template.HtmlTemplate;
import org.iatoki.judgels.sandalphon.problem.base.ProblemControllerUtils;
import org.iatoki.judgels.sandalphon.problem.base.ProblemService;
import org.iatoki.judgels.sandalphon.problem.bundle.AbstractBundleProblemController;
import org.iatoki.judgels.sandalphon.problem.bundle.BundleProblemControllerUtils;
import org.iatoki.judgels.sandalphon.problem.bundle.item.html.listCreateItemsView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;

@Singleton
public final class BundleItemController extends AbstractBundleProblemController {

    private static final long PAGE_SIZE = 1000;

    private final ActorChecker actorChecker;
    private final BundleItemService bundleItemService;
    private final ProblemService problemService;

    @Inject
    public BundleItemController(
            ActorChecker actorChecker,
            BundleItemService bundleItemService,
            ProblemService problemService) {

        this.actorChecker = actorChecker;
        this.bundleItemService = bundleItemService;
        this.problemService = problemService;
    }

    @Transactional(readOnly = true)
    public Result viewItems(Http.Request req, long problemId)  {
        return listCreateItems(req, problemId, 0, "id", "desc", "");
    }

    @Transactional(readOnly = true)
    public Result listCreateItems(Http.Request req, long problemId, long pageIndex, String orderBy, String orderDir, String filterString) {
        String actorJid = actorChecker.check(req);

        Problem problem = checkFound(problemService.findProblemById(problemId));

        if (!BundleProblemControllerUtils.isAllowedToManageItems(problemService, problem)) {
            return notFound();
        }

        try {
            Page<BundleItem> pageOfBundleItems = bundleItemService.getPageOfBundleItemsInProblemWithClone(problem.getJid(), actorJid, pageIndex, PAGE_SIZE, orderBy, orderDir, filterString);
            Form<ItemCreateForm> itemCreateForm = formFactory.form(ItemCreateForm.class);

            return showListCreateItems(problem, pageOfBundleItems, orderBy, orderDir, filterString, itemCreateForm);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result createItem(Http.Request req, long problemId, String itemType, long page, String orderBy, String orderDir, String filterString) {
        String actorJid = actorChecker.check(req);

        Problem problem = checkFound(problemService.findProblemById(problemId));

        if (!BundleProblemControllerUtils.isAllowedToManageItems(problemService, problem)) {
            return notFound();
        }

        if (!EnumUtils.isValidEnum(BundleItemType.class, itemType)) {
            Form<ItemCreateForm> itemCreateForm = formFactory.form(ItemCreateForm.class);

            Page<BundleItem> pageOfBundleItems;
            try {
                pageOfBundleItems = bundleItemService.getPageOfBundleItemsInProblemWithClone(problem.getJid(), actorJid, page, PAGE_SIZE, orderBy, orderDir, filterString);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return showListCreateItems(problem, pageOfBundleItems, orderBy, orderDir, filterString, itemCreateForm.withGlobalError("Item undefined."));
        }

        BundleItemConfAdapter adapter = BundleItemConfAdapters.fromItemType(BundleItemType.valueOf(itemType));
        if (adapter == null) {
            Form<ItemCreateForm> itemCreateForm = formFactory.form(ItemCreateForm.class).withGlobalError("Item undefined.");

            Page<BundleItem> pageOfBundleItems;
            try {
                pageOfBundleItems = bundleItemService.getPageOfBundleItemsInProblemWithClone(problem.getJid(), actorJid, page, PAGE_SIZE, orderBy, orderDir, filterString);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return showListCreateItems(problem, pageOfBundleItems, orderBy, orderDir, filterString, itemCreateForm);
        }

        return showCreateItem(problem, itemType, adapter.getConfHtml(adapter.generateForm(formFactory), routes.BundleItemController.postCreateItem(problem.getId(), itemType, page, orderBy, orderDir, filterString), "Create"), page, orderBy, orderDir, filterString);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postCreateItem(Http.Request req, long problemId, String itemType, long page, String orderBy, String orderDir, String filterString) {
        String actorJid = actorChecker.check(req);

        Problem problem = checkFound(problemService.findProblemById(problemId));

        if (!BundleProblemControllerUtils.isAllowedToManageItems(problemService, problem)) {
            return notFound();
        }

        if (!EnumUtils.isValidEnum(BundleItemType.class, itemType)) {
            Form<ItemCreateForm> itemCreateForm = formFactory.form(ItemCreateForm.class).withGlobalError("Item undefined.");

            Page<BundleItem> pageOfBundleItems;
            try {
                pageOfBundleItems = bundleItemService.getPageOfBundleItemsInProblemWithClone(problem.getJid(), actorJid, page, PAGE_SIZE, orderBy, orderDir, filterString);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return showListCreateItems(problem, pageOfBundleItems, orderBy, orderDir, filterString, itemCreateForm);
        }

        try {
            ProblemControllerUtils.establishStatementLanguage(problemService, problem);
        } catch (IOException e) {
            return notFound();
        }

        BundleItemConfAdapter bundleItemConfAdapter = BundleItemConfAdapters.fromItemType(BundleItemType.valueOf(itemType));
        if (bundleItemConfAdapter == null) {
            Form<ItemCreateForm> itemCreateForm = formFactory.form(ItemCreateForm.class).withGlobalError("Item undefined");

            Page<BundleItem> pageOfBundleItems;
            try {
                pageOfBundleItems = bundleItemService.getPageOfBundleItemsInProblemWithClone(problem.getJid(), actorJid, page, PAGE_SIZE, orderBy, orderDir, filterString);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return showListCreateItems(problem, pageOfBundleItems, orderBy, orderDir, filterString, itemCreateForm);
        }

        Form bundleItemConfForm = bundleItemConfAdapter.bindFormFromRequest(formFactory, req);
        if (formHasErrors(bundleItemConfForm)) {
            return showCreateItem(problem, itemType, bundleItemConfAdapter.getConfHtml(bundleItemConfForm, routes.BundleItemController.postCreateItem(problem.getId(), itemType, page, orderBy, orderDir, filterString), "Create"), page, orderBy, orderDir, filterString);
        }

        problemService.createUserCloneIfNotExists(actorJid, problem.getJid());

        try {
            if (bundleItemService.bundleItemExistsInProblemWithCloneByMeta(problem.getJid(), actorJid, bundleItemConfAdapter.getMetaFromForm(bundleItemConfForm))) {
                Page<BundleItem> items = bundleItemService.getPageOfBundleItemsInProblemWithClone(problem.getJid(), actorJid, page, PAGE_SIZE, orderBy, orderDir, filterString);

                return showListCreateItems(problem, items, orderBy, orderDir, filterString, bundleItemConfForm.withGlobalError("Duplicate meta on item."));
            }

            bundleItemService.createBundleItem(problem.getJid(), actorJid, BundleItemType.valueOf(itemType), bundleItemConfAdapter.getMetaFromForm(bundleItemConfForm), bundleItemConfAdapter.processRequestForm(bundleItemConfForm), ProblemControllerUtils.getDefaultStatementLanguage(problemService, problem));

            return redirect(routes.BundleItemController.viewItems(problem.getId()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result editItem(Http.Request req, long problemId, String itemJid) {
        String actorJid = actorChecker.check(req);

        Problem problem = checkFound(problemService.findProblemById(problemId));

        if (!BundleProblemControllerUtils.isAllowedToUpdateItemInLanguage(problemService, problem)) {
            return notFound();
        }

        try {
            if (!bundleItemService.bundleItemExistsInProblemWithCloneByJid(problem.getJid(), actorJid, itemJid)) {
                return notFound();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            ProblemControllerUtils.establishStatementLanguage(problemService, problem);
        } catch (IOException e) {
            return notFound();
        }

        BundleItem bundleItem;
        try {
            bundleItem = bundleItemService.findInProblemWithCloneByItemJid(problem.getJid(), actorJid, itemJid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BundleItemConfAdapter bundleItemConfAdapter = BundleItemConfAdapters.fromItemType(bundleItem.getType());
        Set<String> allowedLanguages;
        try {
            allowedLanguages = ProblemControllerUtils.getAllowedLanguagesToUpdate(problemService, problem);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (bundleItemConfAdapter == null) {
            return notFound();
        }

        Form bundleItemConfForm;
        try {
            bundleItemConfForm = bundleItemConfAdapter.generateForm(formFactory, bundleItemService.getItemConfInProblemWithCloneByJid(problem.getJid(), actorJid, itemJid, ProblemControllerUtils.getCurrentStatementLanguage()), bundleItem.getMeta());
        } catch (IOException e) {
            try {
                bundleItemConfForm = bundleItemConfAdapter.generateForm(formFactory, bundleItemService.getItemConfInProblemWithCloneByJid(problem.getJid(), actorJid, itemJid, ProblemControllerUtils.getDefaultStatementLanguage(problemService, problem)), bundleItem.getMeta());
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        }

        return showEditItem(problem, bundleItem, bundleItemConfAdapter.getConfHtml(bundleItemConfForm, routes.BundleItemController.postEditItem(problem.getId(), itemJid), "Update"), allowedLanguages);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postEditItem(Http.Request req, long problemId, String itemJid) {
        String actorJid = actorChecker.check(req);

        Problem problem = checkFound(problemService.findProblemById(problemId));

        if (!BundleProblemControllerUtils.isAllowedToUpdateItemInLanguage(problemService, problem)) {
            return notFound();
        }

        try {
            if (!bundleItemService.bundleItemExistsInProblemWithCloneByJid(problem.getJid(), actorJid, itemJid)) {
                return notFound();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BundleItem bundleItem;
        try {
            bundleItem = bundleItemService.findInProblemWithCloneByItemJid(problem.getJid(), actorJid, itemJid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            ProblemControllerUtils.establishStatementLanguage(problemService, problem);
        } catch (IOException e) {
            return notFound();
        }

        BundleItemConfAdapter bundleItemConfAdapter = BundleItemConfAdapters.fromItemType(bundleItem.getType());
        Set<String> allowedLanguages;
        try {
            allowedLanguages = ProblemControllerUtils.getAllowedLanguagesToUpdate(problemService, problem);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (bundleItemConfAdapter == null) {
            return notFound();
        }

        Form bundleItemConfForm = bundleItemConfAdapter.bindFormFromRequest(formFactory, req);
        if (formHasErrors(bundleItemConfForm)) {
            return showEditItem(problem, bundleItem, bundleItemConfAdapter.getConfHtml(bundleItemConfForm, routes.BundleItemController.postEditItem(problem.getId(), itemJid), "Update"), allowedLanguages);
        }

        problemService.createUserCloneIfNotExists(actorJid, problem.getJid());
        try {
            bundleItemService.updateBundleItem(problem.getJid(), actorJid, itemJid, bundleItemConfAdapter.getMetaFromForm(bundleItemConfForm), bundleItemConfAdapter.processRequestForm(bundleItemConfForm), ProblemControllerUtils.getCurrentStatementLanguage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return redirect(routes.BundleItemController.viewItems(problem.getId()));
    }

    @Transactional(readOnly = true)
    public Result moveItemUp(Http.Request req, long problemId, String itemJid) {
        String actorJid = actorChecker.check(req);

        Problem problem = checkFound(problemService.findProblemById(problemId));

        if (!BundleProblemControllerUtils.isAllowedToManageItems(problemService, problem)) {
            return notFound();
        }

        problemService.createUserCloneIfNotExists(actorJid, problem.getJid());

        try {
            if (!bundleItemService.bundleItemExistsInProblemWithCloneByJid(problem.getJid(), actorJid, itemJid)) {
                return notFound();
            }

            bundleItemService.moveBundleItemUp(problem.getJid(), actorJid, itemJid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return redirect(routes.BundleItemController.viewItems(problem.getId()));
    }

    @Transactional(readOnly = true)
    public Result moveItemDown(Http.Request req, long problemId, String itemJid) {
        String actorJid = actorChecker.check(req);

        Problem problem = checkFound(problemService.findProblemById(problemId));

        if (!BundleProblemControllerUtils.isAllowedToManageItems(problemService, problem)) {
            return notFound();
        }

        problemService.createUserCloneIfNotExists(actorJid, problem.getJid());

        try {
            if (!bundleItemService.bundleItemExistsInProblemWithCloneByJid(problem.getJid(), actorJid, itemJid)) {
                return notFound();
            }

            bundleItemService.moveBundleItemDown(problem.getJid(), actorJid, itemJid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return redirect(routes.BundleItemController.viewItems(problem.getId()));
    }

    @Transactional
    public Result removeItem(Http.Request req, long problemId, String itemJid) {
        String actorJid = actorChecker.check(req);

        Problem problem = checkFound(problemService.findProblemById(problemId));

        if (!BundleProblemControllerUtils.isAllowedToManageItems(problemService, problem)) {
            return notFound();
        }

        problemService.createUserCloneIfNotExists(actorJid, problem.getJid());

        try {
            if (!bundleItemService.bundleItemExistsInProblemWithCloneByJid(problem.getJid(), actorJid, itemJid)) {
                return notFound();
            }
            bundleItemService.removeBundleItem(problem.getJid(), actorJid, itemJid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return redirect(routes.BundleItemController.viewItems(problem.getId()));
    }

    private Result showListCreateItems(Problem problem, Page<BundleItem> pageOfBundleItems, String orderBy, String orderDir, String filterString, Form<ItemCreateForm> itemCreateForm) {
        HtmlTemplate template = getBaseHtmlTemplate();
        template.setContent(listCreateItemsView.render(pageOfBundleItems, problem.getId(), pageOfBundleItems.getPageIndex(), orderBy, orderDir, filterString, itemCreateForm));

        template.setPageTitle("Problem - Bundle - Items");

        return renderTemplate(template, problemService, problem);
    }

    private Result showCreateItem(Problem problem, String itemType, Html html, long page, String orderBy, String orderDir, String filterString) {
        HtmlTemplate template = getBaseHtmlTemplate();
        template.setContent(html);
        template.markBreadcrumbLocation("Create item", routes.BundleItemController.createItem(problem.getId(), itemType, page, orderBy, orderDir, filterString));
        template.setPageTitle("Problem - Bundle - Items - Create");

        return renderTemplate(template, problemService, problem);
    }

    private Result showEditItem(Problem problem, BundleItem bundleItem, Html html, Set<String> allowedLanguages) {
        HtmlTemplate template = getBaseHtmlTemplate();
        template.setContent(html);
        appendStatementLanguageSelection(template, ProblemControllerUtils.getCurrentStatementLanguage(), allowedLanguages, org.iatoki.judgels.sandalphon.problem.base.routes.ProblemController.switchLanguage(problem.getId()));
        template.markBreadcrumbLocation("Update item", routes.BundleItemController.editItem(problem.getId(), bundleItem.getJid()));
        template.setPageTitle("Problem - Bundle - Item - Update");

        return renderTemplate(template, problemService, problem);
    }

    protected Result renderTemplate(HtmlTemplate template, ProblemService problemService, Problem problem) {
        template.markBreadcrumbLocation("Items", org.iatoki.judgels.sandalphon.problem.bundle.routes.BundleProblemController.jumpToItems(problem.getId()));

        return super.renderTemplate(template, problemService, problem);
    }
}
