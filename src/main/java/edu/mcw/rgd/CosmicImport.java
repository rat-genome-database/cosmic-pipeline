package edu.mcw.rgd;

import edu.mcw.rgd.dao.impl.ProteinDAO;
import edu.mcw.rgd.dao.impl.SequenceDAO;
import edu.mcw.rgd.dao.impl.XdbIdDAO;
import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

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
            t();
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

    static void t() throws Exception {

        ProteinDAO pdao = new ProteinDAO();
        SequenceDAO sdao = new SequenceDAO();
        XdbIdDAO xdao = new XdbIdDAO();
        Logger log = LogManager.getLogger("status");

        int proteinsSkipped = 0;
        int linesF1 = 0;
        int linesGood = 0;

        String dir = "/tmp/ws/human";
        int psKey = 70000;

        File fdir = new File(dir);
        File[] files = fdir.listFiles();
        for( File f: files ) {
            if( f.isFile() && f.getName().endsWith(".pdb.gz") ) {
                // protein acc is between dashes
                String fname = f.getName();
                String pureName = fname.substring(0, fname.length()-7);
                int dashPos1 = fname.indexOf('-');
                int dashPos2 = fname.indexOf('-', dashPos1+1);
                int dashPos3 = fname.indexOf('-', dashPos2+1);
                int fragmentNr = Integer.parseInt(fname.substring(dashPos2+2, dashPos3));

                if( dashPos1>0 && dashPos2>dashPos1 ) {
                    String proteinAcc = fname.substring(dashPos1+1, dashPos2);
                    List<Gene> genes = xdao.getActiveGenesByXdbId(XdbId.XDB_KEY_UNIPROT, proteinAcc);

                    String proteinAaRange;
                    Protein p = pdao.getProteinByUniProtId(proteinAcc);
                    if( p==null ) {
                        proteinsSkipped++;
                        continue;
                    }
                    List<Sequence> seqs = sdao.getObjectSequences(p.getRgdId(), "uniprot_seq");
                    if( seqs.isEmpty() ) {
                        proteinsSkipped++;
                        continue;
                    }
                    Sequence seq = seqs.get(0);
                    String seqData = seq.getSeqData();
                    int proteinLen = seqData.length();
                    if( fragmentNr==1 ) {
                        if( proteinLen>2700 ) {
                            proteinAaRange = "1-1400";
                        } else {
                            proteinAaRange = "1-" + proteinLen;
                        }
                        linesF1++;
                    } else {
                        // extract fragment nr
                        int start = 200*(fragmentNr-1) + 1;
                        int end = start + 1399;
                        if( end>proteinLen ) {
                            end = proteinLen;
                        }
                        proteinAaRange = start+"-"+end;
                    }
                    linesGood++;

                    for( Gene g: genes ) {
                        log.debug(psKey + " " + g.getSymbol() + " RGD:" + g.getRgdId() + " " + proteinAcc);
                        String sql = "INSERT INTO protein_structures (ps_key,name,modeller) VALUES(?,?,'AlphaFold')";
                        xdao.update(sql, psKey, pureName);
                        String sql2 = "INSERT INTO protein_structure_genes (rgd_id,ps_key,protein_acc_id,protein_aa_range) VALUES(?,?,?,?)";
                        xdao.update(sql2, g.getRgdId(), psKey, proteinAcc, proteinAaRange);
                        psKey++;
                    }

                }
            }
        }

        System.out.println("proteins skipped "+proteinsSkipped);
        System.out.println("proteins good    "+linesGood);
        System.out.println("proteins F1      "+linesF1);

        System.exit(0);
    }
}

