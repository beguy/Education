package main;

import accountServer.AccountServer;
import accountServer.AccountServerController;
import accountServer.AccountServerControllerMBean;
import accountServer.AccountServerI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import resources.IResourceServer;
import resources.ResourceServerController;
import resources.ResourceServerControllerMBean;
import resources.TestResource;
import servlets.AdminPageServlet;
import servlets.HomePageServlet;
import servlets.ResourcePageServlet;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;

/**
 * @author a.akbashev
 * @author v.chibrikov
 *         <p>
 *         Пример кода для курса на https://stepic.org/
 *         <p>
 *         Описание курса и лицензия: https://github.com/vitaly-chibrikov/stepic_java_webserver
 */
public class Main {
    static final Logger logger = LogManager.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {

//        if (args.length != 1) {
//            logger.error("Use port as the first argument");
//            System.exit(1);
//        }

        // указываем порт при старте приложения
//        String portString = args[0];

        Thread.currentThread().wait();

        String portString = "5050";
        int port = Integer.valueOf(portString);

        logger.info("Starting at http://127.0.0.1:" + portString);

        AccountServerI accountServer = new AccountServer(1);

        AccountServerControllerMBean serverStatistics = new AccountServerController(accountServer);
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("ServerManager:type=AccountServerController");
        mbs.registerMBean(serverStatistics, name);

        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(new HomePageServlet(accountServer)), HomePageServlet.PAGE_URL);

        // JMX для admin
        AccountServerI accountServerAdmin = new AccountServer(10);
        AccountServerControllerMBean serverStatisticsAdmin = new AccountServerController(accountServerAdmin);
        ObjectName nameAdmin = new ObjectName("Admin:type=AccountServerController.userLimit");
        mbs.registerMBean(serverStatisticsAdmin, nameAdmin);

        // Регистрация сервлета Admin
        context.addServlet(new ServletHolder(new AdminPageServlet(accountServerAdmin)), AdminPageServlet.PAGE_URL);

        // JMX для resources
        IResourceServer iResourceServer = new TestResource();
        ResourceServerControllerMBean resourceServerControllerMBean = new ResourceServerController(iResourceServer);
        ObjectName objectName = new ObjectName("Admin:type=ResourceServerController");
        mbs.registerMBean(resourceServerControllerMBean, objectName);

        // Регистрация сервлета resources
        context.addServlet(new ServletHolder(new ResourcePageServlet(iResourceServer)), ResourcePageServlet.PAGE_URL);

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(true);
        resource_handler.setResourceBase("static");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{resource_handler, context});
        server.setHandler(handlers);

        // запускаем сервер
        server.start();
        java.util.logging.Logger.getGlobal().log(Level.INFO, "Server started");
        logger.info("Server started");
        server.join();
    }
}
