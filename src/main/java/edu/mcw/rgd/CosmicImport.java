package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
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

    Logger log = LogManager.getLogger("status");

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
        log.info("   "+dao.getConnectionInfo());

        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("   started at "+sdt.format(new Date()));

        // QC
        log.debug("QC: get Cosmic Ids in RGD");
        List<XdbId> cosmicIdsInRgd = dao.getCosmicXdbIds();
        int initialCosmicIdCount = cosmicIdsInRgd.size();
        log.debug("QC: get incoming Cosmic Ids");
        List<XdbId> cosmicIdsIncoming = getIncomingCosmicIds();

        // determine to-be-inserted cosmic ids
        log.debug("QC: determine to-be-inserted Cosmic Ids");
        List<XdbId> cosmicIdsToBeInserted = new ArrayList<XdbId>(cosmicIdsIncoming);
        cosmicIdsToBeInserted.removeAll(cosmicIdsInRgd);

        // determine matching cosmic ids
        log.debug("QC: determine matching Cosmic Ids");
        List<XdbId> cosmicIdsMatching = new ArrayList<XdbId>(cosmicIdsIncoming);
        cosmicIdsMatching.retainAll(cosmicIdsInRgd);

        // determine to-be-deleted cosmic ids
        log.debug("QC: determine to-be-deleted Cosmic Ids");
        cosmicIdsInRgd.removeAll(cosmicIdsIncoming);
        List<XdbId> cosmicIdsToBeDeleted = cosmicIdsInRgd;


        int cosmicIdCountDiff = 0;
        // loading
        if( !cosmicIdsToBeInserted.isEmpty() ) {
            log.info("  COSMIC xdb ids inserted: "+Utils.formatThousands(cosmicIdsToBeInserted.size()));
            dao.insertXdbs(cosmicIdsToBeInserted);
            cosmicIdCountDiff += cosmicIdsToBeInserted.size();
        }

        if( !cosmicIdsToBeDeleted.isEmpty() ) {
            log.info("  COSMIC xdb ids deleted: "+Utils.formatThousands(cosmicIdsToBeDeleted.size()));
            dao.deleteXdbIds(cosmicIdsToBeDeleted);
            cosmicIdCountDiff -= cosmicIdsToBeDeleted.size();
        }

        if( !cosmicIdsMatching.isEmpty() ) {
            log.info("  COSMIC xdb ids matching: "+Utils.formatThousands(cosmicIdsMatching.size()));
            dao.updateModificationDate(cosmicIdsMatching);
        }

        NumberFormat plusMinusNF = new DecimalFormat(" +###,###,###; -###,###,###");
        String diffCountStr = cosmicIdCountDiff!=0 ? "     difference: "+ plusMinusNF.format(cosmicIdCountDiff) : "     no changes";
        log.info("final COSMIC ID count: "+Utils.formatThousands(initialCosmicIdCount+cosmicIdCountDiff)+diffCountStr);

        log.info("processing complete -- elapsed time "+Utils.formatElapsedTime(time0, System.currentTimeMillis()));
        log.info("===");
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

