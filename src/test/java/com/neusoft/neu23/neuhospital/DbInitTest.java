package com.neusoft.neu23.neuhospital;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootTest
public class DbInitTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void initDb() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("E:/Smart-Medical/Backend/infra/postgres/init/001-init.sql"));
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("E:/Smart-Medical/Backend/infra/postgres/init/002-auth-core-schema.sql"));
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("E:/Smart-Medical/Backend/src/main/resources/db/schema/phase1-minimal-business.sql"));
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("E:/Smart-Medical/Backend/src/main/resources/db/schema/phase2-inspection-pharmacy.sql"));
        }
    }
}
