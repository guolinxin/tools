package com.linxin.tools.data.xcc;

import com.marklogic.xcc.exceptions.RequestException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class BaseXccService {

    private static final Logger LOGGER = Logger.getLogger(BaseXccService.class);

    @Autowired
    private BaseXccDao baseXccDao;

    public List<String> getMirListing(int from, int to) throws RequestException {
        return baseXccDao.getMirListing(from, to);
    }

    /**
     * Check MIR server connection
     *
     * @throws MlExportException
     */
    public boolean checkMlServerConnection() {

        try {
            List<String> listing = baseXccDao.getMirListing(1, 1);
            if (listing.isEmpty()) {
                LOGGER.error("No listing found in database");
                return false;
            }
        } catch (RequestException re) {
            throw new MlExportException(re.getMessage(), re);
        } finally {
            baseXccDao.closeSession();
        }

        return true;
    }

    public void closeSession() {
        baseXccDao.closeSession();
    }

}
