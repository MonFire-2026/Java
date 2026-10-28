package school.sptech;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

public class ConexaoBanco {
    private final JdbcTemplate jdbcTemplate;

    public ConexaoBanco() {
        BasicDataSource dataSource = new BasicDataSource();

        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String porta = System.getenv().getOrDefault("DB_PORT", "3306");
        String banco = System.getenv().getOrDefault("DB_NAME", "MonFire");
        String usuario = System.getenv().getOrDefault("DB_USER", "aluno");
        String senha = System.getenv().getOrDefault("DB_PASS", "sptech");

        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://" + host + ":" + porta + "/" + banco
                + "?serverTimezone=America/Sao_Paulo");
        dataSource.setUsername(usuario);
        dataSource.setPassword(senha);

        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}