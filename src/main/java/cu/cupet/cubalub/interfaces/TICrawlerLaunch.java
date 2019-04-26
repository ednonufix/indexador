package cu.cupet.cubalub.interfaces;

import java.io.Serializable;

public interface TICrawlerLaunch extends Serializable {

    public void start() throws Exception;
    public void maintenance() throws Exception;
}
