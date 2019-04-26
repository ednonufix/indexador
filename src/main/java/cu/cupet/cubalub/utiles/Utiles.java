package cu.cupet.cubalub.utiles;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Creado a las 19:40 del día 16/02/17.
 *
 * @author Eduardo Noel Núñez <enoel.corebsd@gmail.com>
 */
public class Utiles implements Serializable {

    private String biblioteca;
    private String biblioteca_solr;
    private String solr;
    private String restful_server;

    private String almacenamiento;
    private String crawlStorageFolder;


    public static Utiles Singleton() {

        return CargaPropertiesHolder.INSTANCE;
    }

    private static class CargaPropertiesHolder {

        private static final Utiles INSTANCE = new Utiles();
    }


    public String getBiblioteca() {
        return biblioteca;
    }

    public void setBiblioteca(String biblioteca) {
        this.biblioteca = biblioteca;
    }

    public String getBiblioteca_solr() {
        return biblioteca_solr;
    }

    public void setBiblioteca_solr(String biblioteca_solr) {
        this.biblioteca_solr = biblioteca_solr;
    }

    public String getSolr() {
        return solr;
    }

    public void setSolr(String solr) {
        this.solr = solr;
    }

    public String getRestful_server() {
        return restful_server;
    }

    public void setRestful_server(String restful_server) {
        this.restful_server = restful_server;
    }

    public String getAlmacenamiento() {
        return almacenamiento;
    }

    public void setAlmacenamiento(String almacenamiento) {
        this.almacenamiento = almacenamiento;
    }

    public String getCrawlStorageFolder() {
        return crawlStorageFolder;
    }

    public void setCrawlStorageFolder(String crawlStorageFolder) {
        this.crawlStorageFolder = crawlStorageFolder;
    }

    public void eliminaDirectorio(String directorio) throws IOException {

        File fichero = new File(directorio);

        if(fichero.isDirectory()) {

            File[] valor = fichero.listFiles();

            for(File temp : valor) {

                if(temp.isDirectory()) {

                    eliminaDirectorio(temp.getAbsolutePath());
                    temp.delete();

                } else {

                    temp.delete();

                }

            }

        }

    }
}
