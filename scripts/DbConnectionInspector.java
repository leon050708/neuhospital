import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class DbConnectionInspector {
    private static final String[] QUERIES = new String[] {
        "show max_connections",
        "select count(*) as total_connections from pg_stat_activity",
        """
        select
          application_name,
          usename,
          coalesce(client_addr::text, 'local') as client_addr,
          state,
          count(*) as conn_count,
          min(backend_start) as oldest_backend_start,
          max(state_change) as latest_state_change
        from pg_stat_activity
        where pid <> pg_backend_pid()
        group by application_name, usename, coalesce(client_addr::text, 'local'), state
        order by conn_count desc, application_name, state
        """,
        """
        select
          pid,
          application_name,
          usename,
          coalesce(client_addr::text, 'local') as client_addr,
          state,
          wait_event_type,
          wait_event,
          coalesce(now() - xact_start, interval '0 second') as tx_age,
          coalesce(now() - query_start, interval '0 second') as query_age,
          left(regexp_replace(query, '\\s+', ' ', 'g'), 220) as query_sample
        from pg_stat_activity
        where pid <> pg_backend_pid()
          and state <> 'idle'
        order by query_age desc
        limit 30
        """,
        """
        select
          left(regexp_replace(query, '\\s+', ' ', 'g'), 220) as query_sample,
          state,
          count(*) as conn_count
        from pg_stat_activity
        where pid <> pg_backend_pid()
          and query is not null
        group by left(regexp_replace(query, '\\s+', ' ', 'g'), 220), state
        order by conn_count desc
        limit 30
        """,
        "select exists (select 1 from pg_extension where extname = 'pg_stat_statements') as has_pg_stat_statements",
        """
        select
          calls,
          round(total_exec_time::numeric, 2) as total_exec_ms,
          round(mean_exec_time::numeric, 2) as mean_exec_ms,
          rows,
          left(regexp_replace(query, '\\s+', ' ', 'g'), 220) as query_sample
        from pg_stat_statements
        order by total_exec_time desc
        limit 20
        """
    };

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: java DbConnectionInspector <jdbc-url> <username> <password>");
            System.exit(2);
        }

        String jdbcUrl = args[0];
        if (!jdbcUrl.contains("ApplicationName=")) {
            jdbcUrl = jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "ApplicationName=db-connection-inspector";
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl, args[1], args[2])) {
            for (String query : QUERIES) {
                runQuery(connection, query);
            }
        }
    }

    private static void runQuery(Connection connection, String query) throws Exception {
        System.out.println("\n=== QUERY ===");
        System.out.println(query);
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            printResultSet(resultSet);
        } catch (Exception exception) {
            System.out.println("QUERY FAILED: " + exception.getMessage());
        }
    }

    private static void printResultSet(ResultSet resultSet) throws Exception {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                System.out.print(" | ");
            }
            System.out.print(metaData.getColumnLabel(i));
        }
        System.out.println();

        int rowCount = 0;
        while (resultSet.next()) {
            rowCount++;
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    System.out.print(" | ");
                }
                Object value = resultSet.getObject(i);
                System.out.print(value == null ? "null" : value.toString());
            }
            System.out.println();
        }

        if (rowCount == 0) {
            System.out.println("(no rows)");
        }
    }
}
