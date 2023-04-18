package io.jenkins.plugins.sample;

import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.*;
import java.io.*;
import java.net.URI;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.net.URL;

import org.apache.commons.httpclient.HttpException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import hudson.model.TaskListener;
import jenkins.security.MasterToSlaveCallable;

public class Linux  {
    private final TaskListener listener;
    private final String SM_HOST;
    private final String SM_API_KEY;
    private final String SM_CLIENT_CERT_FILE;
    private final String SM_CLIENT_CERT_PASSWORD;
    private final String pathVar;
    private final String prompt = "bash";
    private final char c = '-';
    String dir = System.getProperty("user.dir");
    private Integer result;
    ProcessBuilder processBuilder = new ProcessBuilder();

    public Linux(TaskListener listener,  String SM_HOST, String SM_API_KEY, String SM_CLIENT_CERT_FILE, String SM_CLIENT_CERT_PASSWORD, String pathVar) {
        this.listener = listener;
        this.SM_HOST = SM_HOST;
        this.SM_API_KEY = SM_API_KEY;
        this.SM_CLIENT_CERT_FILE = SM_CLIENT_CERT_FILE;
        this.SM_CLIENT_CERT_PASSWORD = SM_CLIENT_CERT_PASSWORD;
        this.pathVar = pathVar;
    }

    public Integer install(String os) {
        this.listener.getLogger().println("\nAgent type: "+os);
        this.listener.getLogger().println("\nIstalling SMCTL\n");
        executeCommand("curl -X GET https://stage.one.digicert.com/signingmanager/api-ui/v1/releases/noauth/smtools-linux-x64.tar.gz/download/ -o smtools-linux-x64.tar.gz");
        result = executeCommand("tar xvf smtools-linux-x64.tar.gz > /dev/null");
        dir = dir+File.separator+"smtools-linux-x64";
        return result;
//        this.listener.getLogger().println("Verifying Installation\n");
//        executeCommand("./smctl keypair ls > /dev/null");
//        this.listener.getLogger().println("Installation Verification Complete\n");
    }

    public Integer createFile(String path, String str) {

        File file = new File(path); //initialize File object and passing path as argument
        boolean result;
        try {
            result = file.createNewFile();  //creates a new file
            try {
                String name = file.getCanonicalPath();
                FileOutputStream fos = new FileOutputStream(name, false);  // true for append mode
                byte[] b = str.getBytes();       //converts string into bytes
                fos.write(b);           //writes bytes into file
                fos.close();            //close the file
                return 0;
            } catch (Exception e) {
                e.printStackTrace(this.listener.error(e.getMessage()));
                return 1;
            }
        } catch (IOException e) {
            e.printStackTrace(this.listener.error(e.getMessage()));
            return 1;
        }
    }

    public void setEnvVar(String key, String value) {
        try {
            Jenkins instance = Jenkins.getInstance();

            DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = instance.getGlobalNodeProperties();
            List<EnvironmentVariablesNodeProperty> envVarsNodePropertyList = globalNodeProperties.getAll(EnvironmentVariablesNodeProperty.class);

            EnvironmentVariablesNodeProperty newEnvVarsNodeProperty = null;
            EnvVars envVars = null;

            if (envVarsNodePropertyList == null || envVarsNodePropertyList.size() == 0) {
                newEnvVarsNodeProperty = new hudson.slaves.EnvironmentVariablesNodeProperty();
                globalNodeProperties.add(newEnvVarsNodeProperty);
                envVars = newEnvVarsNodeProperty.getEnvVars();
            } else {
                //We do have a envVars List
                envVars = envVarsNodePropertyList.get(0).getEnvVars();
            }
            envVars.put(key, value);
            instance.save();
        }
        catch (IOException e) {
            e.printStackTrace(this.listener.error(e.getMessage()));
        }
    }

    public Integer signing() {
        try {
            this.listener.getLogger().println("\nInstalling and configuring signing tools - Jarsigner and Jsign\n");
            result = executeCommand("curl -fSslL https://github.com/ebourg/jsign/releases/download/3.1/jsign_3.1_all.deb -o jsign_3.1_all.deb && sudo dpkg --install jsign_3.1_all.deb > /dev/null");
            if (result==0)
                this.listener.getLogger().println("\nJsign successfully installed\n");
            else{
                this.listener.getLogger().println("\nJsign failed to install\n");
                return 1;
            }
            this.listener.getLogger().println("\nJarsigner successfully installed\n");
            result = executeCommand("sudo chmod -R +x "+dir);
            if (result==0)
                this.listener.getLogger().println("\nSigning tools installation and configuration complete\n");
            else{
                this.listener.getLogger().println("\nFailed to configure signing tools\n");
                return 1;
            }
            setEnvVar("PATH",this.pathVar+":/"+dir);
            return 0;
//            executeCommand("./smctl sign -k " + key + " -f " + fingerprint + " --config-file "+dir+"/pkcs11properties.cfg -v -i " + file);
        } catch (Exception e) {
            e.printStackTrace(this.listener.error(e.getMessage()));
            return 1;
        }
    }

    public Integer executeCommand(String command) {

        try {
            processBuilder.command(prompt,c+"c",command);
            Map<String, String> env = processBuilder.environment();
            // if(SM_API_KEY!=null)
            //     env.put("SM_API_KEY", SM_API_KEY);
            // if(SM_CLIENT_CERT_PASSWORD!=null)
            //     env.put("SM_CLIENT_CERT_PASSWORD", SM_CLIENT_CERT_PASSWORD);
            // if(SM_CLIENT_CERT_FILE!=null)
            //     env.put("SM_CLIENT_CERT_FILE", SM_CLIENT_CERT_FILE);
            // if(SM_HOST!=null)
            //     env.put("SM_HOST", SM_HOST);
            // env.put("PATH",System.getenv("PATH")+":/"+dir+"/smtools-linux-x64/");
            processBuilder.directory(new File(dir));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                this.listener.getLogger().println(line);
            }
            int exitCode = process.waitFor();
            return exitCode;
//            try {
//                if (exitCode != 0) throw new Exception("Command failed");
//            }
//            catch (Exception e) {
//                e.printStackTrace(this.listener.error(e.getMessage()));
//            }
        } catch (IOException e) {
            e.printStackTrace(this.listener.error(e.getMessage()));
            return 1;
        } catch (InterruptedException e) {
            e.printStackTrace(this.listener.error(e.getMessage()));
            return 1;
        }
        catch (Exception e) {
            e.printStackTrace(this.listener.error(e.getMessage()));
            return 1;
        }
    }

    public Integer call(String os) throws IOException {

        result = install(os);
        if (result==0)
            this.listener.getLogger().println("\nSMCTL Istallation Complete\n");
        else {
            this.listener.getLogger().println("\nSMCTL Istallation Failed\n");
            return result;
        }

        this.listener.getLogger().println("\nCreating PKCS11 Config File\n");
        String str = "name=signingmanager\n" +
                "library="+dir+"/smpkcs11.so\n" +
                "slotListIndex=0\n";
        String configPath = dir+File.separator+"pkcs11properties.cfg";

        result = createFile(configPath, str);

        if (result==0)
            this.listener.getLogger().println("\nPKCS11 config file successfully created at location: "+configPath+"\n");
        else {
            this.listener.getLogger().println("\nFailed to create PKCS11 config file\n");
            return result;
        }

        //signing
        result = signing();
        return result;
    }
}
/*
Boolean kp = keypairExists();

        if(kp && !fingerprint.equals("")) {
            ;
        }
        else if (kp && this.fingerprint.equals("")) {
            JSONArray items = getRequest("https://stage.one.digicert.com/signingmanager/api/v1/certificates?alias="+alias);
            JSONArray cert = search(items,alias);
            JSONObject obj = cert.getJSONObject(0);
            importCert(obj);
        }
        else {
            //check if certname exists then generate kp and cert
            JSONArray items = getRequest("https://stage.one.digicert.com/signingmanager/api/v1/certificates?alias="+alias);
            JSONArray cert = search(items,alias);
            if (cert.length() != 0) {
                this.alias = key+"_cert";   //since cert alias already exists
                this.listener.getLogger().println("Provided certificate alias is a duplicate value. Using new unique alias "+alias+"\n");
            }
            ID = generateKP();
            checkUserAccess(ID);
            //Find fingerprint, import cert chain of newly created cert
            items = getRequest("https://stage.one.digicert.com/signingmanager/api/v1/certificates?alias="+alias);
            cert = search(items,alias);
            JSONObject obj = cert.getJSONObject(0);
            importCert(obj);
        }
 */
/*
public List<Path> findByFileName(Path path, String fileName) throws IOException {

        List<Path> result;
        try (Stream<Path> pathStream = Files.find(path,
                Integer.MAX_VALUE,
                (p, basicFileAttributes) ->
                        p.getFileName().toString().equalsIgnoreCase(fileName))
        ) {
            result = pathStream.collect(Collectors.toList());
        }
        return result;
    }

    public JSONArray search(JSONArray array, String searchValue){

        JSONArray filtedArray = new JSONArray();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj= null;
            try {
                obj = array.getJSONObject(i);
                if(obj.getString("alias").equals(searchValue))
                {
                    filtedArray.put(obj);
                }
            } catch (JSONException e) {
                e.printStackTrace(this.listener.error(e.getMessage()));
            }
        }
        return filtedArray;
    }

    public String findCert(JSONArray array, String searchValue){

        for (int i = 0; i < array.length(); i++) {
            try {
                if(array.getJSONObject(i).getString("cert_type").equals(searchValue))
                {
                    return array.getJSONObject(i).getString("blob");
                }
            } catch (JSONException e) {
                e.printStackTrace(this.listener.error(e.getMessage()));
            }
        }
        return "";
    }

    public String findFingerprint(String certString)  {

        try {
            var cert = certString;
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(cert));
            var x509Certificate = (X509Certificate) certFactory.generateCertificate(in);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(x509Certificate.getEncoded());
            var hex = DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
            return hex;
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace(this.listener.error(e.getMessage()));
        }
        catch(CertificateException e){
            e.printStackTrace(this.listener.error(e.getMessage()));
        }
        return "";
    }

    public void createCert(String path, String str) {

        File file = new File(path); //initialize File object and passing path as argument
        boolean result;
        try {
            result = file.createNewFile();  //creates a new file
            if (result)      // test if successfully created a new file
            {
                try {
                    String name = file.getCanonicalPath();
                    FileOutputStream fos = new FileOutputStream(name, false);  // true for append mode
                    byte[] b = str.getBytes();       //converts string into bytes
                    fos.write("-----BEGIN CERTIFICATE-----\n".getBytes("US-ASCII"));
                    fos.write(b);           //writes bytes into file
                    fos.write("\n-----END CERTIFICATE-----\n".getBytes("US-ASCII"));
                    fos.close();      //close the file
                } catch (Exception e) {
                    e.printStackTrace(this.listener.error(e.getMessage()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace(this.listener.error(e.getMessage()));
        }
    }

    public void importCert(JSONObject obj) {

        this.listener.getLogger().println("\nImporting certificates into truststore\n");
        String root = findCert(obj.getJSONArray("chain"),"root");
        String ica = findCert(obj.getJSONArray("chain"),"intermediate");
        String ee = obj.getString("cert");
        createCert(dir+File.separator+"root.crt",root);
        createCert(dir+File.separator+"ica.crt",ica);
        createCert(dir+File.separator+obj.getString("alias")+".crt",ee);
        executeCommand("sudo cp root.crt /usr/local/share/ca-certificates/root.crt && sudo update-ca-certificates > /dev/null");
        executeCommand("sudo cp ica.crt /usr/local/share/ca-certificates/ica.crt && sudo update-ca-certificates > /dev/null");
        executeCommand("sudo cp "+obj.getString("alias")+".crt /usr/local/share/ca-certificates/"+obj.getString("alias")+".crt && sudo update-ca-certificates > /dev/null");
        this.fingerprint = findFingerprint(ee);
        this.listener.getLogger().println("\nCertificates imported\n");
    }

    public Boolean keypairExists() throws IOException, HttpException{

        JSONArray items = getRequest("https://stage.one.digicert.com/signingmanager/api/v1/keypairs?alias="+key);
        JSONArray item = search(items,key);
        if(item.length()==0){
            return false; //keypair does not exist
        }
        else {
            this.listener.getLogger().println("Keypair with the given alias already exists\n");
            items = getRequest("https://stage.one.digicert.com/signingmanager/api/v1/certificates?alias="+alias);
            ID = item.getJSONObject(0).getString("id");
            JSONArray cert = search(items,alias);
            checkUserAccess(ID);
            if(cert.length()==0) {
                // Cert does not exist. Generate cert using cert-alias
                this.listener.getLogger().println("Certificate with the given alias does NOT exist\n" +
                        "Generating certificate with alias "+alias+" for existing keypair...\n");
                executeCommand("./smctl keypair generate-cert "+ID+" --cert-alias="+alias+" --cert-profile-id="+certProfile+" --set-as-default-cert=true");
                return true;
            }
            //certificate exists
            JSONObject obj = cert.getJSONObject(0);
            if ( !(obj.getJSONObject("keypair").getString("id")).equals(ID) ) {
                alias = key + "_cert";
                this.listener.getLogger().println("Certificate with the given alias exists but not for the given keypair.\n" +
                        "Checking if certificate with a different alias "+alias+" exists\n");
                items = getRequest("https://stage.one.digicert.com/signingmanager/api/v1/certificates?alias="+alias);
                cert = search(items,alias);
                if(cert.length()==0){
                    this.listener.getLogger().println("Certificate does not exist.\nNew certificate is generated with alias "+alias+"\n");
                    executeCommand("./smctl keypair generate-cert "+ID+" --cert-alias="+alias+" --cert-profile-id="+certProfile+" --set-as-default-cert=true");
                }
                else {
                    this.listener.getLogger().println("Certificate already exists.\nUsing certificate "+alias+" to sign\n");
                }
                return true;
            }
            this.listener.getLogger().println("Certificate with the given alias already exists\n");
            importCert(obj);
            return true;
        }
    }

    public JSONArray getRequest(String req) throws IOException, HttpException{
        int requestExitCode;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response;

        HttpGet getRequest = new HttpGet(req);
        getRequest.setHeader("x-api-key", apikey);
        response = httpclient.execute(getRequest);
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);

        JSONObject json = new JSONObject(result);

        requestExitCode = response.getStatusLine().getStatusCode();

        if (requestExitCode != 200) {
            listener.getLogger().println("\nRequest Failed with Exit Code: " + requestExitCode);
            listener.getLogger().println("FAILURE REASON: " + response.getStatusLine().getReasonPhrase());
            throw new HttpException("Unexpected response to CONNECT request\n" + result);
        }
        return json.getJSONArray("items");
    }

    public Boolean findUser(JSONArray array, String searchValue) {

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj= null;
            try {
                obj = array.getJSONObject(i);
                if(obj.getString("id").equals(searchValue))
                {
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace(this.listener.error(e.getMessage()));
            }
        }
        return false;
    }

    public void checkUserAccess(String id) {
        try {
            JSONArray items = getRequest("https://stage.one.digicert.com/signingmanager/api/v1/keypairs?id=" + id);
            JSONObject obj = items.getJSONObject(0);
            if (obj.getBoolean("limit_by_users")) {
                JSONArray users = obj.getJSONArray("users");
                String user = obj.getString("created_by");
                if (users.length() == 0 || !findUser(users, obj.getString("created_by"))) {
                    executeCommand("./smctl keypair update-access "+ id +" --users "+user +" --operation add");
                }
            }
        }
        catch (IOException e){
            e.printStackTrace(this.listener.error(e.getMessage()));
        }
    }

    public String generateKP(){
        try{
            String keyType;
            if(prod)
                keyType = "PRODUCTION";
            else
                keyType = "TEST";

            this.listener.getLogger().println("Creating "+keyType+" keypair with alias "+key+" and certificate alias "+alias+"\n");

            processBuilder.command(prompt,c+"c","./smctl keypair generate rsa "+ key +" --cert-alias="+alias+
                    " --cert-profile-id="+certProfile+ " --generate-cert=true --key-type="+keyType);
            Map<String, String> env = processBuilder.environment();
            env.put("SM_API_KEY", apikey);
            env.put("SM_CLIENT_CERT_PASSWORD", password );
            env.put("SM_CLIENT_CERT_FILE", cert);
            env.put("SM_HOST", host);
            env.put("PATH",System.getenv("PATH")+":/"+dir+"/smtools-linux-x64/");
            processBuilder.directory(new File(dir));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                this.listener.getLogger().println(line);
                return line;
            }
            int exitCode = process.waitFor();
        } catch (IOException e) {
            e.printStackTrace(this.listener.error(e.getMessage()));
        } catch (InterruptedException e) {
            e.printStackTrace(this.listener.error(e.getMessage()));
        }
        return "";
    }
 */