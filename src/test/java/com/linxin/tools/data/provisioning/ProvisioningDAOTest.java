package com.linxin.tools.data.provisioning;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProvisioningDAOTest {

    @Autowired
    public ProvisioningDAO provisioningDAO;

    @Test
    public void getBranchTest(){

        List<Map<String, String>> branches =  provisioningDAO.getBranches(2);

        Assert.assertFalse(branches.isEmpty());
    }

}



