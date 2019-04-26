package cu.cupet.cubalub.impl;

import cu.cupet.cubalub.interfaces.TIAppInitializer;
import cu.cupet.cubalub.utiles.Utiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import java.io.Serializable;
import java.util.Properties;

/**
 * Creado a las 19:00 del día 3/02/17.
 *
 * @author Eduardo Noel Núñez <enoel.corebsd@gmail.com>
 */
@Order(5)
public class TAppInitializer implements Serializable, TIAppInitializer {

    @Autowired
    @Override
    public void loadProperties() throws Exception {

        final Logger logger = LoggerFactory.getLogger(TAppInitializer.class);

        Properties prop = new Properties();

        try {

            prop.load(getClass().getClassLoader().getResourceAsStream("config.properties"));

            Utiles.Singleton().setBiblioteca_solr(prop.getProperty("biblioteca_solr"));
            Utiles.Singleton().setSolr(prop.getProperty("solr"));
            Utiles.Singleton().setRestful_server(prop.getProperty("restful_server"));
            Utiles.Singleton().setAlmacenamiento(prop.getProperty("almacenamiento"));
            Utiles.Singleton().setCrawlStorageFolder(prop.getProperty("crawlStorageFolder"));

        } catch (Exception ex) {

            logger.error(ex.getMessage());

        }

    }
}
