package net.red.demo;

import java.util.List;
import java.util.function.Predicate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionTemplate;

import net.red.distributedjobs.spring.DistributedJobsAutoConfiguration;
import net.red.demo.test.config.CacheTestConfig;

@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude={DistributedJobsAutoConfiguration.class})
@Import({DatabaseIntegrationTestSupport.DatabaseTestConfiguration.class, CacheTestConfig.class})
@SpringBootTest(properties = {"scheduling.enabled=false", "logging.config="})
public class AbstractIT {
    private static final List<String> PRE_INITIALIZED_TABLE_NAMES = List.of("databasechangeloglock", "databasechangelog");
    private static final String PRE_INITIALIZED_TABLE_BACK_UP_SUFFIX = "_test_back_up";
    @Autowired
    protected TransactionTemplate transactionTemplate;
    @Autowired
    protected EntityManager entityManager;

    @BeforeEach
    public void init() {
        transactionTemplate.execute(s -> {
            PRE_INITIALIZED_TABLE_NAMES.forEach(t ->
                    entityManager.createNativeQuery("CREATE TABLE " + t + PRE_INITIALIZED_TABLE_BACK_UP_SUFFIX
                            + " AS SELECT * FROM " + t).executeUpdate());
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @AfterEach
    public void cleanup() {
        transactionTemplate.execute(s -> {
            String query = "SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'";
            List<String> tableNames = (List<String>) entityManager.createNativeQuery(query).getResultList();
            tableNames.stream()
                    .filter(Predicate.not(n -> n.endsWith(PRE_INITIALIZED_TABLE_BACK_UP_SUFFIX)))
                    .filter(n -> ((Long) entityManager.createNativeQuery("SELECT COUNT(*) FROM " + n).getSingleResult()).intValue() > 0)
                    .forEach(n -> entityManager.createNativeQuery("TRUNCATE TABLE " + n + " CASCADE").executeUpdate());
            PRE_INITIALIZED_TABLE_NAMES.forEach(t -> {
                entityManager.createNativeQuery("INSERT INTO " + t
                        + " SELECT * FROM " + t + PRE_INITIALIZED_TABLE_BACK_UP_SUFFIX).executeUpdate();
                entityManager.createNativeQuery("DROP TABLE " + t + PRE_INITIALIZED_TABLE_BACK_UP_SUFFIX).executeUpdate();

            });
            return null;
        });
    }

}
