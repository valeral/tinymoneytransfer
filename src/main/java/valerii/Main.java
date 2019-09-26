package valerii;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valerii.db.DbProvider;
import valerii.db.H2Provider;

import java.sql.SQLException;

import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

/**
 * @author vliutyi
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        Server server = initServer(8080);

        DbProvider.setProvider(new H2Provider());

        try {
            DbProvider.createDBTables();
        } catch (SQLException e) {
            logger.error("Error occurred while creating DB tables: " + e.getMessage());
            System.exit(2);
        }

        try {
            server.start();
            server.join();
        } catch (Exception ex) {
            logger.error("Error occurred while starting Jetty", ex);
            System.exit(1);
        } finally {
            server.destroy();
        }
    }

    static Server initServer(int port) {
        Server server = new Server(port);

        ServletContextHandler servletContextHandler = new ServletContextHandler(NO_SESSIONS);

        servletContextHandler.setContextPath("/");
        server.setHandler(servletContextHandler);

        ServletHolder servletHolder = servletContextHandler.addServlet(ServletContainer.class, "/api/*");
        servletHolder.setInitOrder(0);
        servletHolder.setInitParameter(
                "jersey.config.server.provider.packages",
                "valerii.resources"
        );

        return server;
    }
}
