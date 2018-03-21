package com.linxin.tools.data.provisioning;

import com.linxin.tools.data.provisioning.model.BranchPair;
import com.linxin.tools.data.provisioning.model.NoSuchBranchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProvisioningDAO {

    final static Logger logger = LoggerFactory.getLogger(ProvisioningDAO.class);

    // Get Z branches (b.NAMESPACE = 'SAP') which have no adverts (a.id is null) that are
    // duplicates (gn.GHOST_NAT_ID is not null) of non Z branches (gn.GOLDEN_NAT_ID < 900000000)
    // and there is only Z branch for its corresponding BF branch
    // (select count(1) from ghost_nats ghost2 where ghost2.golden_nat_id = ghost.golden_nat_id) = 1
    static final String ExpiredZBranchesThatAreDuplicatesOfABFBranchSQL =
            "select ghost.GOLDEN_NAT_ID as target_branch, golden.EYP_BUS_TYPE_CODE as target_classification, " +
                    "ghost.GHOST_NAT_ID as source_branch, ghost.EYP_BUS_TYPE_CODE as source_classification from branches b\n" +
                    "left join adverts a on a.branch_id = b.id\n" +
                    "left join GHOST_NATS ghost on ghost.GHOST_NAT_ID = b.id\n" +
                    "left join GOLDEN_NATS golden on golden.GOLDEN_NAT_ID = ghost.GOLDEN_NAT_ID\n" +
                    "where a.id is null\n" +
                    "and ghost.GHOST_NAT_ID is not null\n" +
                    "and (select namespace from branches where id = golden.GOLDEN_NAT_ID) != 'SAP'\n" +
                    "and (select count(1) from ghost_nats ghost2 where ghost2.golden_nat_id = ghost.golden_nat_id) = 1\n" +
                    "and b.NAMESPACE = 'SAP' ";// +
    //"and rownum < 50";

    static final String NonExpiredZBranchesSQL =
            "select distinct(b.id) from branches b\n" +
                    "left join adverts a on a.branch_id = b.id\n" +
                    "where a.id is not null\n" +
                    "and b.NAMESPACE = 'SAP'";

    // Get Z branches (b.NAMESPACE = 'SAP') which are expired i.e. have no adverts (a.id is null) that are
    // golden (golden.GOLDEN_NAT_ID is not null) and do not have a ghost (ghost.GHOST_NAT_ID is null)
    static final String ExpiredZBranchesThatAreGoldAndHaveNoGhostSQL =
            "select b.id from branches b\n" +
                    "left join adverts a on a.branch_id = b.id\n" +
                    "left join GOLDEN_NATS golden on golden.GOLDEN_NAT_ID = b.id\n" +
                    "left join GHOST_NATS ghost on ghost.GOLDEN_NAT_ID = golden.GOLDEN_NAT_ID\n" +
                    "where a.id is null\n" +
                    "and b.NAMESPACE = 'SAP'\n" +
                    "and golden.GOLDEN_NAT_ID is not null\n" +
                    "and ghost.GHOST_NAT_ID is null";//" and rownum < 50";

    static final String ExpiredZBranchesThatAreGoldAndHaveASingleNonSAPGhostSQL =
            "select golden.GOLDEN_NAT_ID as source_branch, golden.EYP_BUS_TYPE_CODE as source_classification," +
                    " ghost.GHOST_NAT_ID as target_branch, ghost.EYP_BUS_TYPE_CODE as target_classification from branches b\n" +
                    "left join adverts a on a.branch_id = b.id\n" +
                    "left join GOLDEN_NATS golden on golden.GOLDEN_NAT_ID = b.id\n" +
                    "left join GHOST_NATS ghost on ghost.GOLDEN_NAT_ID = golden.GOLDEN_NAT_ID\n" +
                    //"-- there are no adverts on the branch\n" +
                    "where a.id is null\n" +
                    //"-- the branch is a z branch\n" +
                    "and b.NAMESPACE = 'SAP'\n" +
                    //"-- the branch is gold\n" +
                    "and golden.GOLDEN_NAT_ID is not null\n" +
                    //"-- there is a ghost for the branch\n" +
                    "and ghost.GHOST_NAT_ID is not null\n" +
                    //"-- the ghost branch is not a z branch\n" +
                    "and (select namespace from branches where id = ghost.GHOST_NAT_ID) != 'SAP'\n" +
                    //"-- there is only one ghost\n" +
                    "and (select count(1) from ghost_nats ghost2 where ghost2.golden_nat_id = ghost.golden_nat_id) = 1";

    static final String ExpiredZBranchesThatAreGhostOfANonSAPGoldSQL =
            "select golden.GOLDEN_NAT_ID as target_branch, golden.EYP_BUS_TYPE_CODE as target_classification," +
                    " ghost.GHOST_NAT_ID as source_branch, ghost.EYP_BUS_TYPE_CODE as source_classification from branches b\n" +
                    //"-- the branch is a ghost\n" +
                    "left join GHOST_NATS ghost on ghost.GHOST_NAT_ID = b.id\n" +
                    //"-- join to get the golden for the ghost\n" +
                    "left join GOLDEN_NATS golden on golden.GOLDEN_NAT_ID = ghost.GOLDEN_NAT_ID\n" +
                    //"-- join to get the adverts for the branch\n" +
                    "left join adverts a on a.branch_id = b.id\n" +
                    //"-- there are no adverts on the branch\n" +
                    "where a.id is null\n" +
                    //"-- the branch is a z branch\n" +
                    "and b.NAMESPACE = 'SAP'\n" +
                    //"-- there is a ghost for the branch\n" +
                    "and ghost.GHOST_NAT_ID is not null\n" +
                    //"-- the gold branch is not a z branch\n" +
                    "and (select namespace from branches where id = golden.GOLDEN_NAT_ID) != 'SAP'";

    static final String GetGhostNatsForGoldenNatSQL =
            "select * from GHOST_NATS where GOLDEN_NAT_ID = ?";

    static final String GetProfileUrlsSQL =
            "select PROFILE_URL from tablename where BRANCH_ID = ?";

    public static final String GetCountOfRecordsFromHistoryByURLSQL =
            "select count(*) from branch_profile_url_history where PROFILE_URL = ?";

    public static final String UpdateProfileUrlHistorySQL =
            "update branch_profile_url_history set branch_id = ? where PROFILE_URL = ?";

    public static final String InsertIntoProfileUrlHistorySQL =
            "insert into branch_profile_url_history values (?, ?)";

    static final String DeleteProfileUrlsSQL =
            "delete from branch_profile_url where BRANCH_ID = ?";

    static final String ReassignProfileUrlHistorySQL =
            "update branch_profile_url_history set branch_id = ? where branch_id = ?";

    static final String GetClassificationIdSQL = "select CLASSIFICATION_CODE from branches where id = ?";

    static final String ZBranchExistsSQL =
            "select count(*) from branches where namespace = 'SAP' and namespace_id = ?";

    static final String GetCountOfAdvertsForZBranchSQL =
            "select count(*) from adverts where branch_id = (select id from branches where namespace = 'SAP' and namespace_id = ?)";

    static final String GetZBranchIdSQL = "select id from branches where namespace = 'SAP' and namespace_id = ?";

    static final String GetCountOfGoldUsingBranchIdZBranchSQL =
            "select count(*) from golden_nats where golden_nat_id = ?";

    static final String GetGhostsForGoldSQL =
            "select * from ghost_nats where golden_nat_id = ?";

    static final String GetGoldForGhost =
            "select * from golden_nats where ghost_nat_id = ?";

    static final String AdvertCountForBranchSQL =
            "select count(*) from adverts where branch_id = ?";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ProvisioningDAO(@Qualifier("prov") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<BranchPair> getExpiredZBranchesThatAreDuplicatesOfABFBranch() {
        return jdbcTemplate.query(ExpiredZBranchesThatAreDuplicatesOfABFBranchSQL,
                new RowMapper<BranchPair>() {
                    @Override
                    public BranchPair mapRow(ResultSet resultSet, int i) throws SQLException {
                        return new BranchPair(resultSet.getInt("source_branch"),
                                new String(resultSet.getBytes("source_classification")),
                                resultSet.getInt("target_branch"),
                                new String(resultSet.getBytes("target_classification")));
                    }
                });
    }

    public Optional<String> getCurrentProfileUrl(int branchId) {
        List<String> profileUrls = getProfileUrls("branch_profile_url", branchId);
        if (profileUrls.size() > 0)
            return Optional.of(getProfileUrls("branch_profile_url", branchId).get(0));
        else
            return Optional.empty();
    }

    public List<String> getProfileUrlHistory(int branchId) {
        return getProfileUrls("branch_profile_url_history", branchId);
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    private List<String> getProfileUrls(String table, int branchId) {
        return jdbcTemplate.query(GetProfileUrlsSQL.replace("tablename", table),
                new Integer[]{branchId},
                new RowMapper<String>() {
                    @Override
                    public String mapRow(ResultSet resultSet, int i) throws SQLException {
                        return new String(resultSet.getBytes("PROFILE_URL"));
                    }
                });
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void upsertProfileUrlToHistory(int branchId, String profileUrl) {

        String statement = InsertIntoProfileUrlHistorySQL;
        if (profileURLExistsInHistory(profileUrl)) {
            statement = UpdateProfileUrlHistorySQL;
        }

        jdbcTemplate.update(statement,
                new Object[]{branchId, profileUrl},
                new int[]{Types.INTEGER, Types.VARCHAR});

    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public boolean profileURLExistsInHistory(String profileUrl) {
        Integer count = jdbcTemplate.queryForObject(GetCountOfRecordsFromHistoryByURLSQL, Integer.class, profileUrl);
        if (count != null && count > 0)
            return true;
        else
            return false;
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void deleteCurrentProfileUrl(int branchId) {
        jdbcTemplate.update(DeleteProfileUrlsSQL, branchId);
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void reassignProfileUrlHistory(int oldBranchId, int newBranchId) {
        jdbcTemplate.update(ReassignProfileUrlHistorySQL, newBranchId, oldBranchId);
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<Integer> getGhostNatsForGoldenNat(int branchId) {
        return jdbcTemplate.query(GetGhostNatsForGoldenNatSQL, new Integer[]{branchId},
                new RowMapper<Integer>() {
                    @Override
                    public Integer mapRow(ResultSet resultSet, int i) throws SQLException {
                        return resultSet.getInt("ghost_nat_id");
                    }
                });
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public String getClassificationIdForBranch(int branchId) {
        try {
            return jdbcTemplate.query(GetClassificationIdSQL, new Integer[]{branchId},
                    new ResultSetExtractor<String>() {
                        @Override
                        public String extractData(ResultSet resultSet) throws SQLException, DataAccessException {
                            resultSet.next();
                            return new String(resultSet.getBytes("CLASSIFICATION_CODE"));
                        }
                    });

        } catch (IncorrectResultSizeDataAccessException e) {
            throw new NoSuchBranchException(branchId);
        } catch (UncategorizedSQLException e) {
            throw new NoSuchBranchException(branchId);
        }
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<Integer> getExpiredZBranchesThatAreGoldAndHaveNoGhost() {
        return jdbcTemplate.query(ExpiredZBranchesThatAreGoldAndHaveNoGhostSQL,
                new RowMapper<Integer>() {
                    @Override
                    public Integer mapRow(ResultSet resultSet, int i) throws SQLException {
                        return resultSet.getInt("ID");
                    }
                });
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<BranchPair> getExpiredZBranchesThatAreGoldAndHaveASingleNonSAPGhost() {
        return jdbcTemplate.query(ExpiredZBranchesThatAreDuplicatesOfABFBranchSQL,
                new RowMapper<BranchPair>() {
                    @Override
                    public BranchPair mapRow(ResultSet resultSet, int i) throws SQLException {
                        return new BranchPair(resultSet.getInt("source_branch"),
                                new String(resultSet.getBytes("source_classification")),
                                resultSet.getInt("target_branch"),
                                new String(resultSet.getBytes("target_classification")));
                    }
                });
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<BranchPair> getExpiredZBranchesThatAreGhostOfANonSAPGold() {
        return jdbcTemplate.query(ExpiredZBranchesThatAreGhostOfANonSAPGoldSQL,
                new RowMapper<BranchPair>() {
                    @Override
                    public BranchPair mapRow(ResultSet resultSet, int i) throws SQLException {
                        return new BranchPair(resultSet.getInt("source_branch"),
                                new String(resultSet.getBytes("source_classification")),
                                resultSet.getInt("target_branch"),
                                new String(resultSet.getBytes("target_classification")));
                    }
                });
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public boolean zBranchExists(String namespaceId) {
        Integer count = jdbcTemplate.queryForObject(ZBranchExistsSQL, Integer.class, namespaceId);
        if (count != null && count > 0)
            return false;
        else
            return true;
    }

    public boolean isZBranchExpired(String namespaceId) {
        Integer count = jdbcTemplate.queryForObject(GetCountOfAdvertsForZBranchSQL, Integer.class, namespaceId);
        if (count != null && count > 0)
            return false;
        else
            return true;
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public int getZBranchId(String namespaceId) {
        try {
            return jdbcTemplate.queryForObject(GetZBranchIdSQL, Integer.class, namespaceId);
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new NoSuchBranchException(namespaceId);
        }
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public boolean isZBranchGold(int branchId) {
        Integer count = jdbcTemplate.queryForObject(GetCountOfGoldUsingBranchIdZBranchSQL, Integer.class, branchId);
        if (count != null && count > 0)
            return true;
        else
            return false;
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<Integer> getGhostsForGold(int branchId) {
        return jdbcTemplate.query(GetGhostsForGoldSQL,
                new RowMapper<Integer>() {
                    @Override
                    public Integer mapRow(ResultSet resultSet, int i) throws SQLException {
                        return resultSet.getInt("ghost_nat_id");
                    }
                });
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public int getGoldForGhost(int branchId) {
        return jdbcTemplate.queryForObject(GetGoldForGhost, Integer.class, branchId);
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public boolean advertsExistForBranch(int branchId) {
        Integer count = jdbcTemplate.queryForObject(AdvertCountForBranchSQL, Integer.class, branchId);
        if (count != null && count > 0)
            return true;
        else
            return false;
    }

    public void testRetryable() {
        throw new RuntimeException("test");
    }

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<Integer> getNonExpiredZBranches() {
        return jdbcTemplate.query(NonExpiredZBranchesSQL,
                new RowMapper<Integer>() {
                    @Override
                    public Integer mapRow(ResultSet resultSet, int i) throws SQLException {
                        return resultSet.getInt("ID");
                    }
                });
    }


    //////////////////// logging result


    static final String AdvertOrdersForBranchSQL =
            "select * from adverts where branch_id = ?";

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<String> getFieldValueByBranchId(int branchId, final String field) {
        return jdbcTemplate.query(AdvertOrdersForBranchSQL,
                new Integer[]{branchId},
                new RowMapper<String>() {
                    @Override
                    public String mapRow(ResultSet resultSet, int i) throws SQLException {
                        return new String(resultSet.getBytes(field));
                    }
                });
    }

    static final String AdvertOrderNumberForBranchSQL =
            "select adverts.order_number, adverts.order_line_number, branches.namespace_id from adverts left join branches on adverts.branch_id = branches.id where branch_id = ?";


    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<Map<String, String>> getFieldValuesByBranchId(int branchId) {
        return jdbcTemplate.query(AdvertOrderNumberForBranchSQL,
                new Integer[]{branchId},

                new RowMapper<Map<String, String>>() {
                    @Override
                    public Map<String, String> mapRow(ResultSet rs, int rowNum) throws SQLException {
                        Map<String, String> map = new HashMap<>();
                        map.put("order_number", new String(rs.getBytes("order_number")));
                        map.put("order_line_number", new String(rs.getBytes("order_line_number")));
                        map.put("namespace_id", new String(rs.getBytes("namespace_id")));
                        return map;
                    }

                });

    }

    public static final int pageSize = 1000;

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<Map<String, String>> getBranches(int pageNumber) {

        int fromRowNum = pageNumber * pageSize;
        int toRowNum = fromRowNum + pageSize;

        String sql = " SELECT * FROM (SELECT ROWNUM AS rowno, b.* FROM BRANCHES b WHERE ROWNUM <= "+ toRowNum + ") table_alias WHERE table_alias.rowno >=  " + fromRowNum;

        return jdbcTemplate.query(sql,
                new RowMapper<Map<String, String>>() {
                    @Override
                    public Map<String, String> mapRow(ResultSet rs, int rowNum) throws SQLException {
                        Map<String, String> map = new HashMap<>();
                        map.put("id", new String(rs.getBytes("id")));
                        map.put("namespace", new String(rs.getBytes("namespace")));
                        map.put("namespace_id", new String(rs.getBytes("namespace_id")));
                        return map;
                    }

                });

    }

    //////////////////// DO Not commit


}