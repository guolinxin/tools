package com.linxin.tools.data.xcc;

import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.RequestException;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface BaseXccDao {

    /**
     * Load Listing document from MIR
     * specific execute module: "export/export-listings.xqy"
     *
     * @param from
     * @param to
     * @return
     * @throws RequestException
     */
    List<String> getMirListing(int from, int to) throws RequestException;

    /**
     * Get ML connection session
     *
     * @return
     */
    Session getSession();

    /**
     * Close ML connection session
     */
    void closeSession();
}
