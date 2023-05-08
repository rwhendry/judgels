package judgels;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.GENERATE_STATISTICS;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import com.palantir.websecurity.WebSecurityConfiguration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.DropwizardTestSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import judgels.gabriel.api.GabrielClientConfiguration;
import judgels.jerahmeel.JerahmeelConfiguration;
import judgels.jerahmeel.stats.StatsConfiguration;
import judgels.jophiel.JophielConfiguration;
import judgels.jophiel.api.JophielClientConfiguration;
import judgels.jophiel.api.session.Credentials;
import judgels.jophiel.api.session.Session;
import judgels.jophiel.api.session.SessionService;
import judgels.jophiel.api.user.User;
import judgels.jophiel.api.user.UserData;
import judgels.jophiel.api.user.UserService;
import judgels.jophiel.mailer.MailerConfiguration;
import judgels.jophiel.session.SessionConfiguration;
import judgels.jophiel.user.account.UserRegistrationConfiguration;
import judgels.jophiel.user.account.UserResetPasswordConfiguration;
import judgels.jophiel.user.avatar.UserAvatarConfiguration;
import judgels.jophiel.user.superadmin.SuperadminCreatorConfiguration;
import judgels.jophiel.user.web.WebConfiguration;
import judgels.sandalphon.SandalphonConfiguration;
import judgels.sandalphon.api.SandalphonClientConfiguration;
import judgels.sandalphon.api.lesson.Lesson;
import judgels.sandalphon.api.problem.Problem;
import judgels.sandalphon.api.problem.ProblemType;
import judgels.sandalphon.api.problem.bundle.ItemType;
import judgels.service.api.actor.AuthHeader;
import judgels.service.jaxrs.JaxRsClients;
import judgels.uriel.UrielConfiguration;
import judgels.uriel.api.UrielClientConfiguration;
import judgels.uriel.file.FileConfiguration;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.h2.Driver;
import org.hibernate.dialect.H2Dialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseJudgelsServiceIntegrationTests {
    private static DropwizardTestSupport<JudgelsServerApplicationConfiguration> support;
    private static Path baseDataDir;

    protected static User admin;
    protected static User user;

    protected static AuthHeader adminHeader;
    protected static AuthHeader userHeader;

    protected static WebTarget webTarget;

    @BeforeAll
    static void startApp() throws Exception {
        DataSourceFactory dbConfig = new DataSourceFactory();
        dbConfig.setDriverClass(Driver.class.getName());
        dbConfig.setUrl("jdbc:h2:mem:./judgels");
        dbConfig.setProperties(ImmutableMap.<String, String>builder()
                .put(DIALECT, H2Dialect.class.getName())
                .put(HBM2DDL_AUTO, "create")
                .put(GENERATE_STATISTICS, "false")
                .build());

        baseDataDir = Files.createTempDirectory("judgels");

        JudgelsAppConfiguration judgelsAppConfig = new JudgelsAppConfiguration.Builder()
                .baseUrl("http://localhost:9101")
                .name("Judgels")
                .build();

        JudgelsServerConfiguration judgelsConfig = new JudgelsServerConfiguration.Builder()
                .baseDataDir(baseDataDir.toString())
                .appConfig(judgelsAppConfig)
                .build();

        JophielConfiguration jophielConfig = new JophielConfiguration.Builder()
                .baseDataDir(baseDataDir.toString())
                .mailerConfig(new MailerConfiguration.Builder()
                        .host("localhost")
                        .port(2500)
                        .useSsl(false)
                        .username("wiser")
                        .password("wiser")
                        .sender("noreply@wiser.com")
                        .build())
                .userRegistrationConfig(UserRegistrationConfiguration.DEFAULT)
                .userResetPasswordConfig(UserResetPasswordConfiguration.DEFAULT)
                .userAvatarConfig(UserAvatarConfiguration.DEFAULT)
                .superadminCreatorConfig(SuperadminCreatorConfiguration.DEFAULT)
                .sessionConfig(SessionConfiguration.DEFAULT)
                .webConfig(WebConfiguration.DEFAULT)
                .build();

        UrielConfiguration urielConfig = new UrielConfiguration.Builder()
                .baseDataDir(baseDataDir.toString())
                .sandalphonConfig(SandalphonClientConfiguration.DEFAULT)
                .gabrielConfig(GabrielClientConfiguration.DEFAULT)
                .submissionConfig(judgels.uriel.submission.programming.SubmissionConfiguration.DEFAULT)
                .fileConfig(FileConfiguration.DEFAULT)
                .build();

        SandalphonConfiguration sandalphonConfig = new SandalphonConfiguration.Builder()
                .baseDataDir(baseDataDir.toString())
                .jophielConfig(JophielClientConfiguration.DEFAULT)
                .gabrielConfig(GabrielClientConfiguration.DEFAULT)
                .build();

        JerahmeelConfiguration jerahmeelConfig = new JerahmeelConfiguration.Builder()
                .baseDataDir(baseDataDir.toString())
                .sandalphonConfig(SandalphonClientConfiguration.DEFAULT)
                .urielConfig(UrielClientConfiguration.DEFAULT)
                .gabrielConfig(GabrielClientConfiguration.DEFAULT)
                .submissionConfig(judgels.jerahmeel.submission.programming.SubmissionConfiguration.DEFAULT)
                .statsConfig(StatsConfiguration.DEFAULT)
                .build();

        JudgelsServerApplicationConfiguration config = new JudgelsServerApplicationConfiguration(
                dbConfig,
                WebSecurityConfiguration.DEFAULT,
                judgelsConfig,
                jophielConfig,
                sandalphonConfig,
                urielConfig,
                jerahmeelConfig);

        support = new DropwizardTestSupport<>(JudgelsServerApplication.class, config);
        support.before();

        Session adminSession = createService(SessionService.class).logIn(Credentials.of("superadmin", "superadmin"));
        adminHeader = AuthHeader.of(adminSession.getToken());
        admin = createService(UserService.class).getUser(adminHeader, adminSession.getUserJid());

        user = createUser("user");
        userHeader = getHeader(user);
    }

    @AfterAll
    static void stopApp() throws IOException {
        support.after();
        MoreFiles.deleteRecursively(baseDataDir, RecursiveDeleteOption.ALLOW_INSECURE);
    }

    protected static WebTarget createWebTarget() {
        return JerseyClientBuilder.createClient()
                .property("jersey.config.client.followRedirects", false)
                .register(MultiPartFeature.class)
                .target("http://localhost:" + support.getLocalPort());
    }

    protected static <T> T createService(Class<T> serviceClass) {
        return JaxRsClients.create(
                serviceClass,
                "http://localhost:" + support.getLocalPort());
    }

    protected static void assertPermitted(ThrowingCallable callable) {
        assertThatCode(callable).doesNotThrowAnyException();
    }

    protected static AbstractThrowableAssert<?, ? extends Throwable> assertBadRequest(ThrowingCallable callable) {
        return assertThatThrownBy(callable).hasFieldOrPropertyWithValue("code", 400);
    }

    protected static void assertUnauthorized(ThrowingCallable callable) {
        assertThatThrownBy(callable).hasFieldOrPropertyWithValue("code", 401);
    }

    protected static AbstractThrowableAssert<?, ? extends Throwable> assertForbidden(ThrowingCallable callable) {
        return assertThatThrownBy(callable).hasFieldOrPropertyWithValue("code", 403);
    }

    protected static void assertNotFound(ThrowingCallable callable) {
        assertThatThrownBy(callable).hasFieldOrPropertyWithValue("code", 404);
    }

    protected static User createUser(String username) {
        return createService(UserService.class).createUser(adminHeader, new UserData.Builder()
                .username(username)
                .password("pass")
                .email(username + "@domain.com")
                .build());
    }

    protected static AuthHeader getHeader(User user) {
        return AuthHeader.of(createService(SessionService.class)
                .logIn(Credentials.of(user.getUsername(), "pass"))
                .getToken());
    }

    protected static Problem createBundleProblem(AuthHeader authHeader, String slug) {
        return createProblem(authHeader, slug, "Bundle");
    }

    protected static Problem createProblem(AuthHeader authHeader, String slug) {
        return createProblem(authHeader, slug, "Batch");
    }

    protected static Problem createProblem(AuthHeader authHeader, String slug, String gradingEngine) {
        Form form = new Form();
        form.param("slug", slug);
        form.param("gradingEngine", gradingEngine);
        form.param("additionalNote", "");
        form.param("initialLanguage", "en-US");

        Response response = webTarget
                .path("/problems/new")
                .request()
                .cookie(new Cookie("JUDGELS_TOKEN", authHeader.getBearerToken()))
                .post(Entity.entity(form, APPLICATION_FORM_URLENCODED));

        String redirect = response.getLocation().toString();

        Pattern pattern = Pattern.compile("/(\\d+)/");
        Matcher matcher = pattern.matcher(redirect);
        matcher.find();

        long problemId = Long.valueOf(matcher.group(1));

        response = webTarget
                .path("/problems/" + problemId)
                .request()
                .cookie(new Cookie("JUDGELS_TOKEN", authHeader.getBearerToken()))
                .get();

        String html = response.readEntity(String.class);

        pattern = Pattern.compile("(JID(?:BUND|PROG)[a-zA-Z0-9]+)");
        matcher = pattern.matcher(html);
        matcher.find();

        String problemJid = matcher.group(1);

        return new Problem.Builder()
                .id(problemId)
                .jid(problemJid)
                .slug(slug)
                .authorJid("JIDUSERxxx")
                .additionalNote("")
                .lastUpdateTime(Instant.now())
                .type(gradingEngine.equals("Bundle") ? ProblemType.BUNDLE : ProblemType.PROGRAMMING)
                .build();
    }

    protected static void updateProblemStatement(AuthHeader authHeader, Problem problem, String title, String text) {
        Form form = new Form();
        form.param("title", title);
        form.param("text", text);

        webTarget
                .path("/problems/" + problem.getId() + "/statements/edit")
                .request()
                .cookie(new Cookie("JUDGELS_TOKEN", authHeader.getBearerToken()))
                .post(Entity.entity(form, APPLICATION_FORM_URLENCODED));

        form = new Form();
        form.param("title", "Update title");
        form.param("description", "");

        webTarget
                .path("/problems/" + problem.getId() + "/versions/local")
                .request()
                .cookie(new Cookie("JUDGELS_TOKEN", authHeader.getBearerToken()))
                .post(Entity.entity(form, APPLICATION_FORM_URLENCODED));
    }

    protected static String createBundleProblemItem(AuthHeader authHeader, Problem problem, ItemType type, Form config) {
        Form form = new Form();
        form.param("type", type.name());

        Response response = webTarget
                .path("/problems/bundle/" + problem.getId() + "/items")
                .request()
                .cookie(new Cookie("JUDGELS_TOKEN", authHeader.getBearerToken()))
                .post(Entity.entity(form, APPLICATION_FORM_URLENCODED));

        String redirect = response.getLocation().toString();
        Pattern pattern = Pattern.compile("(JIDITEM[a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(redirect);
        matcher.find();

        String itemJid = matcher.group(1);

        webTarget
                .path("/problems/bundle/" + problem.getId() + "/items/" + itemJid)
                .request()
                .cookie(new Cookie("JUDGELS_TOKEN", authHeader.getBearerToken()))
                .post(Entity.entity(config, APPLICATION_FORM_URLENCODED));

        form = new Form();
        form.param("title", "Add item");
        form.param("description", "");

        webTarget
                .path("/problems/" + problem.getId() + "/versions/local")
                .request()
                .cookie(new Cookie("JUDGELS_TOKEN", authHeader.getBearerToken()))
                .post(Entity.entity(form, APPLICATION_FORM_URLENCODED));

        return itemJid;
    }

    protected static void createProblemEditorial(AuthHeader authHeader, Problem problem) {
        Form form = new Form();
        form.param("initialLanguage", "en-US");

        webTarget
                .path("/problems/" + problem.getId() + "/editorials")
                .request()
                .cookie(new Cookie("JUDGELS_TOKEN", authHeader.getBearerToken()))
                .post(Entity.entity(form, APPLICATION_FORM_URLENCODED));

        form = new Form();
        form.param("title", "Add editorial");
        form.param("description", "");

        webTarget
                .path("/problems/" + problem.getId() + "/versions/local")
                .request()
                .cookie(new Cookie("JUDGELS_TOKEN", authHeader.getBearerToken()))
                .post(Entity.entity(form, APPLICATION_FORM_URLENCODED));
    }

    protected static Lesson createLesson(AuthHeader authHeader, String slug) {
        Form form = new Form();
        form.param("slug", slug);
        form.param("additionalNote", "");
        form.param("initialLanguage", "en-US");

        Response response = webTarget
                .path("/lessons/new")
                .request()
                .cookie(new Cookie("JUDGELS_TOKEN", authHeader.getBearerToken()))
                .post(Entity.entity(form, APPLICATION_FORM_URLENCODED));

        String redirect = response.getLocation().toString();

        Pattern pattern = Pattern.compile("/(\\d+)/");
        Matcher matcher = pattern.matcher(redirect);
        matcher.find();

        long lessonId = Long.valueOf(matcher.group(1));

        response = webTarget
                .path("/lessons/" + lessonId)
                .request()
                .cookie(new Cookie("JUDGELS_TOKEN", authHeader.getBearerToken()))
                .get();

        String html = response.readEntity(String.class);

        pattern = Pattern.compile("(JIDLESS[a-zA-Z0-9]+)");
        matcher = pattern.matcher(html);
        matcher.find();

        String lessonJid = matcher.group(1);

        return new Lesson.Builder()
                .id(lessonId)
                .jid(lessonJid)
                .slug(slug)
                .authorJid("JIDUSERxxx")
                .additionalNote("")
                .lastUpdateTime(Instant.now())
                .build();
    }

    protected static String randomString() {
        return "string" + (Math.random() * 1000000000);
    }

    protected static ThrowingCallable callAll(ThrowingCallable... callables) {
        return () -> {
            Throwable throwable = null;
            int throwables = 0;

            for (ThrowingCallable callable : callables) {
                try {
                    callable.call();
                } catch (Throwable t) {
                    throwables++;
                    throwable = t;
                }
            }

            if (throwables != 0 && throwables != callables.length) {
                throw new IllegalStateException();
            }

            if (throwable != null) {
                throw throwable;
            }
        };
    }
}