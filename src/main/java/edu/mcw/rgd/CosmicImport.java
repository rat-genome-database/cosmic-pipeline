package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author jdepons
 * @since 4/26/12
 */
public class CosmicImport {

    private CosmicDAO dao = new CosmicDAO();
    private String version;

    Logger log = Logger.getLogger("status");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        CosmicImport manager = (CosmicImport) (bf.getBean("manager"));

        try {
            manager.run();
        }catch (Exception e) {
            Utils.printStackTrace(e, manager.log);
            throw e;
        }
    }

    public void run() throws Exception {

        long time0 = System.currentTimeMillis();

        log.info(getVersion());
        log.info(dao.getConnectionInfo());

        // QC
        log.info("QC: get Cosmic Ids in RGD");
        List<XdbId> cosmicIdsInRgd = dao.getCosmicXdbIds();
        log.info("QC: get incoming Cosmic Ids");
        List<XdbId> cosmicIdsIncoming = getIncomingCosmicIds();

        // determine to-be-inserted cosmic ids
        log.info("QC: determine to-be-inserted Cosmic Ids");
        List<XdbId> cosmicIdsToBeInserted = new ArrayList<XdbId>(cosmicIdsIncoming);
        cosmicIdsToBeInserted.removeAll(cosmicIdsInRgd);

        // determine matching cosmic ids
        log.info("QC: determine matching Cosmic Ids");
        List<XdbId> cosmicIdsMatching = new ArrayList<XdbId>(cosmicIdsIncoming);
        cosmicIdsMatching.retainAll(cosmicIdsInRgd);

        // determine to-be-deleted cosmic ids
        log.info("QC: determine to-be-deleted Cosmic Ids");
        cosmicIdsInRgd.removeAll(cosmicIdsIncoming);
        List<XdbId> cosmicIdsToBeDeleted = cosmicIdsInRgd;


        // loading
        if( !cosmicIdsToBeInserted.isEmpty() ) {
            log.info("inserting xdb ids for COSMIC (Human): "+cosmicIdsToBeInserted.size());
            dao.insertXdbs(cosmicIdsToBeInserted);
        }

        if( !cosmicIdsToBeDeleted.isEmpty() ) {
            log.info("Deleting xdb ids for COSMIC (Human): "+cosmicIdsToBeDeleted.size());
            dao.deleteXdbIds(cosmicIdsToBeDeleted);
        }

        if( !cosmicIdsMatching.isEmpty() ) {
            log.info("matching xdb ids for COSMIC (Human): "+cosmicIdsMatching.size());
            dao.updateModificationDate(cosmicIdsMatching);
        }

        log.info("=== Cosmic ID generation complete -- elapsed time "+Utils.formatElapsedTime(time0, System.currentTimeMillis()));
    }

    List<XdbId> getIncomingCosmicIds() throws Exception {

        List<Gene> genes = dao.getActiveGenes(SpeciesType.HUMAN);
        List<XdbId> incomingCosmicIds = new ArrayList<XdbId>(genes.size());
        for (Gene g: genes) {
            XdbId x = new XdbId();
            x.setAccId(g.getSymbol());
            x.setSrcPipeline("COSMIC");
            x.setRgdId(g.getRgdId());
            x.setXdbKey(45);
            x.setCreationDate(new Date());
            x.setModificationDate(new Date());
            x.setLinkText(g.getSymbol());
            incomingCosmicIds.add(x);
        }
        return incomingCosmicIds;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}

