package ddctest;

import com.config.ClientConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

/**
 * 集成测试 Spring 上下文。
 *
 * 使用 H2 内存库（MODE=MySQL）替代真实 MySQL，无需任何外部依赖。
 * 导入 ClientConfiguration 以初始化 ddc-client 完整链路：
 *   EventNotifyAspect → MyDataSourceTransactionManager → DomainEventApplicationContext
 */
@Configuration
@EnableAspectJAutoProxy
@EnableTransactionManagement
@ComponentScan(basePackages = {"ddctest"})
@Import(ClientConfiguration.class)
public class DdcTestConfig {

    /**
     * H2 内存数据库，MODE=MySQL 保证 MOD()、LEFT()、CONCAT()、IFNULL() 等函数兼容。
     * DB_CLOSE_DELAY=-1 保证整个 JVM 生命周期内数据库不关闭。
     */
    @Bean(destroyMethod = "shutdown")
    public DataSource dataSource() {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder()
                .generateUniqueName(false)
                .setName("ddctest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;" +
                         "CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1")
                .setType(EmbeddedDatabaseType.H2)
                .setScriptEncoding("UTF-8")
                .addScript("classpath:schema.sql")
                .build();
        return db;
    }
}
