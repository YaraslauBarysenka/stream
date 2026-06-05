package net.red.demo;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@ContextConfiguration
@JdbcTest(includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = Repository.class))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public interface DatabaseIntegrationTestSupport {
    @TestConfiguration
    class DatabaseTestConfiguration {
        @Bean
        @Profile("!jenkins")
        public DataSource dataSource(PostgresContainer container) {
            var config = new HikariConfig();
            config.setJdbcUrl(container.getJdbcUrl());
            config.setUsername(container.getUsername());
            config.setPassword(container.getPassword());
            return new HikariDataSource(config);
        }

        @Bean(initMethod = "start", destroyMethod = "stop")
        @Profile("!jenkins")
        public PostgresContainer postgresContainer() {
            return new PostgresContainer();
        }

    }

    class PostgresContainer extends GenericContainer<PostgresContainer>  {
        private static final String USER = "postgres";
        private static final String PASS = "postgres";

        public PostgresContainer() {
            super("harbor.red.net/commonimg/postgresql-14");
            setWaitStrategy(new LogMessageWaitStrategy().withRegEx(".*ready to accept connection.*"));
            setExposedPorts(List.of(5432));
        }

        String getJdbcUrl() {
            return "jdbc:p6spy:postgresql://" + getHost() + ":"
                    + getMappedPort(5432) + "/postgres";
        }

        String getUsername() {
            return USER;
        }

        String getPassword() {
            return PASS;
        }
    }
}
