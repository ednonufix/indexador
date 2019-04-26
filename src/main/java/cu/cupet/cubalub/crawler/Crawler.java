package cu.cupet.cubalub.crawler;

import com.google.common.io.Files;
import cu.cupet.cubalub.utiles.Utiles;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.url.WebURL;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Creado a las 18:25 del día 22/02/17.
 *
 * @author Eduardo Noel Núñez <enoel.corebsd@gmail.com>
 */
@Order(20)
public class Crawler extends WebCrawler implements Serializable {

    final Logger logger = LoggerFactory.getLogger(Crawler.class);

    private final Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg|png|mp3|mp4|zip|gz|git|rar))$");
    private final Pattern documentos = Pattern.compile(".*(\\.(xml|json|jsonl|csv|pdf|doc|docx|ppt|pptx|xls|xlsx|odt|odp|ods|ott|otp|ots|rtf|htm|html|txt|log|mhtml))$");

    private String biblioteca;

    @Override
    public void onStart() {

        biblioteca = (String) myController.getCustomData();

    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {

        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

        Long id_biblioteca = restTemplate.postForObject(Utiles.Singleton().getRestful_server() + "/api/obtener_id_biblioteca", biblioteca, Long.class);

        Map<String, Long> map = new HashMap<>();

        map.put("id", id_biblioteca);

        List<String> lista_urls = restTemplate.getForObject(Utiles.Singleton().getRestful_server() + "/api/urls_dado_biblio/{id}", List.class, map);

        String href = url.getURL();

        return !FILTERS.matcher(href).matches() && href.startsWith(biblioteca) && !lista_urls.contains(href);

    }

    @Override
    public void visit(Page page) {

        File almacen = new File(Utiles.Singleton().getAlmacenamiento());

        if (!almacen.exists()) {

            almacen.mkdir();

        }

        String url = page.getWebURL().getURL();

        String hash = UUID.randomUUID().toString();

        String documento = almacen.getAbsolutePath() + File.separator + hash;

        if (documentos.matcher(url).matches()) {

            try {

                Files.write(page.getContentData(), new File(documento));

                logger.info("Almacenado: {}", url);

            } catch (IOException iox) {

                logger.error("Fallo al escribir el fichero: " + documento, iox);
            }

            HttpSolrClient.Builder builder = new HttpSolrClient.Builder(Utiles.Singleton().getSolr());
            HttpSolrClient cliente = builder.build();

            ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest("/update/extract");

            try {

                ContentStream stream = new ContentStreamBase.FileStream(new File(documento));

                updateRequest.addContentStream(stream);
                updateRequest.setParam("literal.id", url);
                updateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
                updateRequest.setMethod(SolrRequest.METHOD.POST);

                cliente.request(updateRequest, Utiles.Singleton().getBiblioteca_solr());

                RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

                Long id_biblioteca = restTemplate.postForObject(Utiles.Singleton().getRestful_server() + "/api/obtener_id_biblioteca", biblioteca, Long.class);

                restTemplate.postForLocation(Utiles.Singleton().getRestful_server() + "/api/annade_doc_indexado/{id}", url, id_biblioteca);

                // Borro el documento descargado
                File documento_borrar = new File(documento);
                documento_borrar.delete();

            } catch (IOException | SolrServerException e) {

                logger.error(e.getMessage());

            }
        }

    }

}