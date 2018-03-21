package com.linxin.tools.data.provisioning;

import com.yell.provisioning.bindings.branches.branch.Branch;
import com.yell.provisioning.bindings.branches.branchadverts.BranchAdverts;
import com.yell.provisioning.bindings.branches.content.BranchContent;
import com.yell.provisioning.bindings.branches.content.urls.BranchContentUrls;
import com.yell.provisioning.bindings.profileurls.ProfileUrls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.*;

import java.util.*;

/**
 * This is not a Spring bean so that in can be used in AWS Lambdas.
 */
public class PPAPIClient {

    private final static Logger LOGGER = LoggerFactory.getLogger(PPAPIClient.class);

    private final String baseUrl;
    private final RestTemplate restTemplate;

    /**
     * Constructs client, baking in basic authentication using the username and password provided.
     * The baseUrl is expected to be the whole URL up to but not including the "branches" segment.
     */
    public PPAPIClient(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        List<ClientHttpRequestInterceptor> interceptors = Collections
                .<ClientHttpRequestInterceptor>singletonList(new BasicAuthorizationInterceptor(
                        username, password));
        restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new InterceptingClientHttpRequestFactory(restTemplate.getRequestFactory(),
                interceptors));
    }


    /**
     * Constructs client, any necessary authentication must have already been baked into the RestTemplate provided.
     * The baseUrl is expected to be the whole URL up to but not including the "branches" segment.
     */
    public PPAPIClient(String baseUrl, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
    }


    /**
     * Returns null if the branch does not exist.
     */
    public String getBranchInternalId(String namespace, String namespaceId) throws RestClientException {
        String url = baseUrl + "/branches/{namespace}-{namespaceId}";
        Map<String, String> uriVariables = baseUriVariables(namespace, namespaceId);
        try {
            ResponseEntity<String> listing = restTemplate.getForEntity(url, String.class, uriVariables);
            String bipUrl = listing.getHeaders().get("BIP-URL").get(0);
            String[] bipParts = bipUrl.split("-");
            return bipParts[bipParts.length - 1];
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            } else {
                throw e;
            }
        }
    }


    /**
     * Returns null if the branch does not exist.
     */
    public Branch getBranch(String namespace, String namespaceId) throws RestClientException {
        String url = baseUrl + "/branches/{namespace}-{namespaceId}";
        Map<String, String> uriVariables = baseUriVariables(namespace, namespaceId);
        try {
            return restTemplate.getForObject(url, Branch.class, uriVariables);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            } else {
                throw e;
            }
        }
    }


    /**
     * Returns object with an empty list of adverts if the branch has none.
     */
    public BranchAdverts getBranchAdverts(String namespace, String namespaceId) throws RestClientException {
        String url = baseUrl + "/branches/{namespace}-{namespaceId}/adverts";
        Map<String, String> uriVariables = baseUriVariables(namespace, namespaceId);
        return restTemplate.getForObject(url, BranchAdverts.class, uriVariables);
    }


    public ProfileUrls getBranchProfileUrls(String namespace, String namespaceId) throws RestClientException {
        String url = baseUrl + "/branches/{namespace}-{namespaceId}/profileurls";
        Map<String, String> uriVariables = baseUriVariables(namespace, namespaceId);
        return restTemplate.getForObject(url, ProfileUrls.class, uriVariables);
    }


    public void addBranchHistoricalProfileUrl(String namespace, String namespaceId, String profileUrl) throws RestClientException {
        String url = baseUrl + "/branches/{namespace}-{namespaceId}/profileurls/historicalprofileurls/{profileUrl}";
        Map<String, String> uriVariables = baseUriVariables(namespace, namespaceId);
        uriVariables.put("profileUrl", profileUrl);
        restTemplate.put(url, null, uriVariables);
    }


    private Map<String, String> baseUriVariables(String namespace, String namespaceId) {
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("namespace", namespace);
        uriVariables.put("namespaceId", namespaceId);
        return uriVariables;
    }


    public BranchContentUrls getContentURLsForBranch(int branchId) {
        BranchContentUrls branchContentUrls = new BranchContentUrls();
        try {
            ResponseEntity<BranchContentUrls> response = restTemplate.getForEntity(baseUrl +
                    "/branches/prov-{branchid}/content", BranchContentUrls.class, branchId);
            branchContentUrls = response.getBody();
        } catch (HttpServerErrorException e) {
            if (e.getStatusCode().is5xxServerError()) {
                LOGGER.error("Error reading content for {}", branchId, e);
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.debug("No content for {}", branchId);
            }
        }
        return branchContentUrls;
    }


    public void writeBranchContent(BranchContent branchContent, int branchId, String source, String type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);

        HttpEntity<BranchContent> request = new HttpEntity<BranchContent>(branchContent, headers);
        try {
            restTemplate.put(baseUrl + "/branches/prov-{branchid}/content/{source}-{type}", request,
                    branchId, source, type);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                LOGGER.error("Ignoring 400 error copying content for : " + source);
            } else {
                LOGGER.error("Error copying content for : " + source);
                throw e;
            }
        }
    }


    public void writeBranchContent(BranchContent branchContent, String namespace, String namespaceId, String source, String type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);

        HttpEntity<BranchContent> request = new HttpEntity<>(branchContent, headers);
        restTemplate.put(baseUrl + "/branches/{namespace}-{namespaceId}/content/{source}-{type}", request,
                namespace, namespaceId, source, type);
    }


    public void deleteBranch(int branchId) {
        deleteBranch("prov", Integer.toString(branchId));
    }


    public void deleteBranch(String namespace, String namespaceId) {
        try {
            restTemplate.delete(baseUrl + "/branches/{namespace}-{namespaceId}", namespace, namespaceId);
        } catch (HttpClientErrorException e) {
            // An attempt to delete a branch which doesn't exist is not a problem
            if (!e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw e;
            }
        }
    }


    public Optional<BranchContent> getCanonicalBranchContent(String branchContentUrl) {

        String actualSource = branchContentUrl.substring(branchContentUrl.lastIndexOf("/") + 1,
                branchContentUrl.lastIndexOf("-"));

        // Replace the actual source with canonical so we get the canonical version
        String canonicalUrl = branchContentUrl.replace(actualSource, "canonical");

        // The canonical also includes /provisioning-x.y/ so remove that too as the baseurl already has it
        canonicalUrl = canonicalUrl.substring(canonicalUrl.indexOf("/branches"));

        try {
            ResponseEntity<BranchContent> response = restTemplate.getForEntity(baseUrl + canonicalUrl, BranchContent.class);

            return Optional.of(response.getBody());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.info("Could not find content for url {} despite {} being present. " +
                        "This may be because the entity group has a non active status i.e. 'pending'", canonicalUrl, branchContentUrl);

                return Optional.empty();
            } else {
                throw e;
            }
        }
    }


    public Optional<BranchContent> getCanonicalBranchContent(String namespace, String namespaceId, String contentType) {
        try {
            ResponseEntity<BranchContent> response =
                    restTemplate.getForEntity(baseUrl + "/branches/{namespace}-{namespaceid}/content/canonical-{contentType}",
                            BranchContent.class, namespace, namespaceId, contentType);

            return Optional.of(response.getBody());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }


    public String writeBranch(String namespace, String namespaceId, Branch branch) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);

        //blat the branch content or else we get a 400 error
        Branch clonedBranch = (Branch) branch.clone();
        clonedBranch.setBranchContents(null);

        try {
            HttpEntity<Branch> request = new HttpEntity<>(clonedBranch, headers);
            ResponseEntity<Branch> resp = restTemplate.exchange(baseUrl + "/branches/{namespace}-{namespaceId}", HttpMethod.PUT,
                    request, Branch.class, namespace, namespaceId);

            String[] splitBipUrl = resp.getHeaders().get("bip-url").get(0).split("-");
            return splitBipUrl[splitBipUrl.length - 1];
        } catch (HttpStatusCodeException e) {
            //Allows us to see the error response in the cucumber test
            String xml = e.getResponseBodyAsString();
            throw new RuntimeException(xml, e);
        }
    }


    public boolean branchExists(final String namespace, final String namespaceId) {
        try {
            ResponseEntity<Branch> responseEntity = restTemplate.getForEntity(baseUrl + "/branches/{namespace}-{namespaceId}",
                    Branch.class, namespace, namespaceId);

            return responseEntity.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                return false;
            } else {
                throw e;
            }
        }
    }


    // To re-ingest the branch read it then write it
    public void reingestBranch(int branchId) {
        try {
            String branchLocationUri = baseUrl + "/branches/prov-{namespaceId}";
            Branch branch = restTemplate.getForObject(branchLocationUri, Branch.class, branchId);


            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);

            HttpEntity<Branch> request = new HttpEntity<>(branch, headers);
            restTemplate.put(branchLocationUri, request, branchId);
        } catch (HttpClientErrorException hcee) {
            if (hcee.getStatusCode() != HttpStatus.NOT_FOUND)
                throw hcee;
        }
    }

}
