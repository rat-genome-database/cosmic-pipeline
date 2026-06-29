package edu.mcw.rgd;

import edu.mcw.rgd.dao.impl.GeneDAO;
import edu.mcw.rgd.dao.impl.XdbIdDAO;
import edu.mcw.rgd.dao.spring.IntStringMapQuery;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mtutaj
 * @since 9/3/13
 * wrapper to handle all DAO code
 */
public class CosmicDAO {

    XdbIdDAO xdao = new XdbIdDAO();
    GeneDAO gdao = new GeneDAO();

    Logger logInserted = LogManager.getLogger("inserted");
    Logger logDeleted = LogManager.getLogger("deleted");

    public String getConnectionInfo() {
        return xdao.getConnectionInfo();
    }

    public List<XdbId> getCosmicXdbIds() throws Exception {

        XdbId filter = new XdbId();
        filter.setXdbKey(XdbId.XDB_KEY_COSMIC);
        filter.setSrcPipeline("COSMIC");
        return xdao.getXdbIds(filter, SpeciesType.HUMAN);
    }

    /**
     * Returns (gene RGD id, gene symbol) pairs for all active genes of given species.
     * Results do not contain splices or alleles.
     * @param speciesKey species type key
     * @return list of MapPair objects: gene RGD id (keyValue) and gene symbol (stringValue)
     * @throws Exception when unexpected error in spring framework occurs
     */
    public List<IntStringMapQuery.MapPair> getSymbolForActiveGenes(int speciesKey) throws Exception {
        return gdao.getSymbolForActiveGenes(speciesKey);
    }

    /**
     * insert a bunch of XdbIds; duplicate entries are not inserted (with same RGD_ID,XDB_KEY,ACC_ID,SRC_PIPELINE)
     * @param xdbs list of XdbIds objects to be inserted
     * @return number of actually inserted rows
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int insertXdbs(Collection<XdbId> xdbs) throws Exception {

        for( XdbId xdbId: xdbs ) {
            logInserted.debug(xdbId.dump("|"));
        }

        return xdao.insertXdbs(new ArrayList<>(xdbs));
    }

    /**
     * delete a list external ids (RGD_ACC_XDB rows);
     * if ACC_XDB_KEY is provided, it is used to delete the row;
     * else ACC_ID, RGD_ID, XDB_KEY and SRC_PIPELINE are used to locate and delete every row
     *
     * @param xdbIds list of external ids to be deleted
     * @return nr of rows deleted
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int deleteXdbIds( Collection<XdbId> xdbIds ) throws Exception {

        for( XdbId xdbId: xdbIds ) {
            logDeleted.debug(xdbId.dump("|"));
        }

        return xdao.deleteXdbIds(new ArrayList<>(xdbIds));
    }

    public int updateModificationDate(Collection<XdbId> xdbIds) throws Exception {

        List<Integer> xdbKeys = new ArrayList<>(xdbIds.size());
        for( XdbId xdbId: xdbIds ) {
            xdbKeys.add(xdbId.getKey());
        }
        return xdao.updateModificationDate(xdbKeys);
    }
}
