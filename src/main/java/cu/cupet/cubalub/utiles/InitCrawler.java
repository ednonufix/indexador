package cu.cupet.cubalub.utiles;

import cu.cupet.cubalub.interfaces.TICrawlerLaunch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Order(25)
public class InitCrawler {

    @Autowired
    TICrawlerLaunch crawler;

    @PostConstruct
    public void start() {

        Logger logger = LoggerFactory.getLogger(InitCrawler.class);

        try {

            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

            Runnable start = () -> {

                try {

                    System.out.println("Ejecutando spyder");
                    crawler.start();

                } catch (Exception ex) {

                    logger.error(ex.getMessage());

                }

            };


            Runnable maintenance = () -> {

                try {

                    System.out.println("Ejecutando mantenimiento");

                    crawler.maintenance();

                } catch (Exception ex) {

                    logger.error(ex.getMessage());

                }

            };

            executorService.scheduleAtFixedRate(start, 1, 75, TimeUnit.SECONDS);
            executorService.scheduleAtFixedRate(maintenance, 1, 75, TimeUnit.SECONDS);


        } catch (Exception ex) {

            logger.error(ex.getMessage());

        }

    }

}
