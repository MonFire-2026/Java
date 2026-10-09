package school.sptech;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

public class ConexaoBanco {
    private final JdbcTemplate jdbcTemplate;

    public ConexaoBanco() {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/MonFire?serverTimezone=America/Sao_Paulo");
        dataSource.setUsername("aluno");
        dataSource.setPassword("sptech");

        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}
