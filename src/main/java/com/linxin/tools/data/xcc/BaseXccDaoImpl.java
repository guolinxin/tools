package com.linxin.tools.data.xcc;

import com.linxin.tools.config.MarkLogicConfiguration;
import com.marklogic.xcc.*;
import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.types.XName;
import com.marklogic.xcc.types.XdmVariable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Component
public class BaseXccDaoImpl implements BaseXccDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseXccDaoImpl.class);

    public static final String FROM_PAGE_PARAM = "FROM_PAGE";
    public static final String TO_PAGE_PARAM = "TO_PAGE";

    @Autowired
    private MarkLogicConfiguration markLogicConfiguration;

    @Autowired
    private ContentSource contentSource;

    private Session session;

    public List<String> getMirListing(int from, int to) throws RequestException {
        try {
            XdmVariable fromVariable = ValueFactory.newVariable(new XName(FROM_PAGE_PARAM), ValueFactory.newXSInteger(from));
            XdmVariable toVariable = ValueFactory.newVariable(new XName(TO_PAGE_PARAM), ValueFactory.newXSInteger(to));
            ResultSequence resultSequence = executeMainModule(markLogicConfiguration.getMlModule(), null, fromVariable, toVariable);
            ResultItem[] resultItemArray = resultSequence.toResultItemArray();
            return Arrays.stream(resultItemArray)
                    .map(ResultItem::asString)
                    .collect(Collectors.toList());
        } catch (RequestException re) {
            throw re;
        }
    }

    /**
     * Execute ML module
     *
     * @param moduleName
     * @param options
     * @param variables
     * @return
     * @throws RequestException
     * @throws MlExportException
     */
    protected ResultSequence executeMainModule(String moduleName, RequestOptions options, XdmVariable... variables) throws
            RequestException, MlExportException {

        LOGGER.info("*** start execute main module");

        try {
            Request req = getSession().newModuleInvoke(moduleName);

            if (options != null) {
                req.setOptions(options);
            }

            if (variables != null) {
                for (XdmVariable v : variables) {
                    req.setVariable(v);
                }
            }

            return getSession().submitRequest(req);

        } catch (RequestException re) {
            LOGGER.error(re.getMessage(), re);
            throw re;
        } catch (Exception e) {
            LOGGER.error("Exception: ", e);
            throw new MlExportException(String.format("ErrorResponseItem while executing main module", e.getMessage()));
        }
    }

    /**
     * Execute database query
     *
     * @param query
     * @param options
     * @param variables
     * @return
     * @throws RequestException
     */
    protected ResultSequence executeQuery(String query, RequestOptions options, XdmVariable... variables) throws RequestException {
        try {
            Request req = getSession().newAdhocQuery(query, options);

            if (variables != null) {
                for (XdmVariable v : variables) {
                    if (v != null) {
                        req.setVariable(v);
                    }
                }
            }
            return getSession().submitRequest(req);

        } catch (RequestException rs) {
            throw rs;
        }
    }

    @Override
    public Session getSession() {
        if (this.session == null || session.isClosed()) {
            session = contentSource.newSession();
        }
        return session;
    }

    @Override
    public void closeSession() {
        if (session != null && !session.isClosed()) {
            session.close();
        }
    }
}
