package cu.cupet.cubalub.impl;

import cu.cupet.cubalub.crawler.Crawler;
import cu.cupet.cubalub.interfaces.TICrawlerLaunch;
import cu.cupet.cubalub.utiles.Utiles;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Order(20)
public class TCrawlerLaunch implements TICrawlerLaunch {

    final Logger logger = LoggerFactory.getLogger(Crawler.class);

    @Override
    public void start() throws Exception {

        final int numberOfCrawlers = 5;

        CrawlConfig config;

        String crawlstoragefolder;

        PageFetcher pageFetcher;
        RobotstxtConfig robotstxtConfig;
        RobotstxtServer robotstxtServer;

        config = new CrawlConfig();

        crawlstoragefolder = Utiles.Singleton().getCrawlStorageFolder() + File.separator + UUID.randomUUID().toString();

        config.setCrawlStorageFolder(crawlstoragefolder);
        config.setMaxDownloadSize(1048576000);
        config.setIncludeBinaryContentInCrawling(true);
        config.setFollowRedirects(false);
        config.setMaxDepthOfCrawling(1000);
        config.setIncludeHttpsPages(true);

        pageFetcher = new PageFetcher(config);

        robotstxtConfig = new RobotstxtConfig();

        robotstxtConfig.setUserAgentName("DataCrawler - Sypder Web ");

        robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);

        File crawlstorefolder = new File(crawlstoragefolder);

        if (!crawlstorefolder.exists()) {

            crawlstorefolder.mkdir();
        }

        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        List<String> lista_indexados = restTemplate.getForObject(Utiles.Singleton().getRestful_server() + "/api/lista_indexados", List.class);

        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        lista_indexados.forEach(biblioteca -> {

            controller.setCustomData(biblioteca);

            controller.addSeed(biblioteca);

            controller.start(Crawler.class, numberOfCrawlers);

            controller.shutdown();

        });

        Utiles.Singleton().eliminaDirectorio(Utiles.Singleton().getCrawlStorageFolder());

    }

    @Override
    public void maintenance() throws Exception {

        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

        List<String> lista_indexados = restTemplate.getForObject(Utiles.Singleton().getRestful_server() + "/api/lista_indexados", List.class);

        lista_indexados.forEach(biblioteca -> {

            Long id_biblioteca = restTemplate.postForObject(Utiles.Singleton().getRestful_server() + "/api/obtener_id_biblioteca", biblioteca, Long.class);

            Map<String, Long> map = new HashMap<>();

            map.put("id", id_biblioteca);

            List<String> lista_urls = restTemplate.getForObject(Utiles.Singleton().getRestful_server() + "/api/urls_dado_biblio/{id}", List.class, map);

            int increment = lista_urls.size() / 5;

            for (int i = 0; i < 5; i++) {

                // Lanzo los 4 hilos de ejecucion, cada hilo con los indices min y max que van a crear la sublista a analizar
                new Thread(new Worker(i * increment, (i + 1) * increment, lista_urls, map)).start();
            }

        });

    }


    private class Worker implements Runnable {

        final private int minIndex; // first index, inclusive
        final private int maxIndex; // last index, exclusive
        final private List<String> urls;
        final private Map<String, Long> map;


        public Worker(int minIndex, int maxIndex, List<String> urls, Map<String, Long> map) {
            this.minIndex = minIndex;
            this.maxIndex = maxIndex;
            this.urls = urls;
            this.map = map;
        }

        @Override
        public void run() {

            try {

                List<String> sublista = urls.subList(minIndex, maxIndex);

                String biblio_solr_url = Utiles.Singleton().getSolr() + Utiles.Singleton().getBiblioteca_solr();

                HttpSolrClient.Builder builder = new HttpSolrClient.Builder(biblio_solr_url);
                SolrClient cliente = builder.build();

                RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

                sublista.forEach(valor -> {

                    try {

                        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
                        HttpGet httpGet = new HttpGet(valor);

                        CloseableHttpResponse response = closeableHttpClient.execute(httpGet);

                        // Si no se encuentra el objeto en el repositorio lo elimino del buscador
                        if (response.getStatusLine().getStatusCode() == 404) {

                            logger.info("Se ha borrado la siguiente URL de la base de datos y de SOLR: " + valor);

                            restTemplate.postForLocation(Utiles.Singleton().getRestful_server() + "/api/elimina_doc_indexado/{id}", valor, map);

                            cliente.deleteById(valor);
                            cliente.commit();

                        }

                        response.close();
                        httpGet.releaseConnection();

                    } catch (Exception ex) {

                        logger.error(ex.getMessage());

                    }


                });


            } catch (Exception ex) {

                logger.error(ex.getMessage());

            }

        }

    }

}
