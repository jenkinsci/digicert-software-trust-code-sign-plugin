//The MIT License
//
//Copyright 2023
//
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package io.jenkins.plugins.digicert;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import java.util.ArrayList;

public class AgentInfo extends MasterToSlaveCallable<Boolean, Throwable> {
    private final TaskListener listener;
    private final EnvVars env;
    private String SM_HOST;
    // lgtm[jenkins/plaintext-storage]
    private String SM_API_KEY;
    private String SM_CLIENT_CERT_FILE;
    // lgtm[jenkins/plaintext-storage]
    private String SM_CLIENT_CERT_PASSWORD;
    private String path;
    private Integer result;

    public AgentInfo(TaskListener listener, EnvVars env) {
        this.listener = listener;
        this.env = env;
    }

    public StandardCredentials getCredential(String credentialID) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider
                        .lookupCredentials(StandardCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, new ArrayList<DomainRequirement>()),
                CredentialsMatchers.withId(credentialID)
        );
    }

    public String getValue(String credentialID) {
        StandardCredentials credentials = getCredential(credentialID);
        if (credentials != null) {
            if (credentials instanceof StringCredentials) {
                return ((StringCredentials) credentials).getSecret().getPlainText();
            }
        }
        return env.get(credentialID) != null ? env.get(credentialID) : System.getenv(credentialID);
    }

    public Boolean call() throws Throwable {

        String os = System.getProperty("os.name");

        SM_HOST = getValue("SM_HOST");
        SM_API_KEY = getValue("SM_API_KEY");
        SM_CLIENT_CERT_FILE = getValue("SM_CLIENT_CERT_FILE");
        SM_CLIENT_CERT_PASSWORD = getValue("SM_CLIENT_CERT_PASSWORD");
//        SM_HOST = env.get("SM_HOST")!=null?env.get("SM_HOST"):System.getenv("SM_HOST");
//        SM_API_KEY = env.get("SM_API_KEY")!=null?env.get("SM_API_KEY"):System.getenv("SM_API_KEY");
//        SM_CLIENT_CERT_FILE = env.get("SM_CLIENT_CERT_FILE")!=null?env.get("SM_CLIENT_CERT_FILE"):System.getenv("SM_CLIENT_CERT_FILE");
//        SM_CLIENT_CERT_PASSWORD = env.get("SM_CLIENT_CERT_PASSWORD")!=null?env.get("SM_CLIENT_CERT_PASSWORD"):System.getenv("SM_CLIENT_CERT_PASSWORD");
        path = env.get("path") != null ? env.get("path") : System.getenv("path");


        if (os.toLowerCase().contains("windows")) {
            Windows w = new Windows(this.listener, this.SM_HOST, this.SM_API_KEY, this.SM_CLIENT_CERT_FILE, this.SM_CLIENT_CERT_PASSWORD, this.path);
            result = w.call(os);
        } else {
            Linux l = new Linux(this.listener, this.SM_HOST, this.SM_API_KEY, this.SM_CLIENT_CERT_FILE, this.SM_CLIENT_CERT_PASSWORD, this.path);
            result = l.call(os);
        }
        if (result == 1)
            return false;
        return true;
    }
}
  /*
      public void check(String val, String name) {
        try{
            if (val == null) throw new Exception(name + " cannot be null");
        }
        catch (Exception e){
            e.printStackTrace(this.listener.error(e.getMessage()));
        }
    }
        check(this.SM_HOST,"host name");
        check(this.SM_API_KEY,"api key");
        check(this.SM_CLIENT_CERT_PASSWORD ,"certificate password");
        check(this.SM_CLIENT_CERT_FILE , "path to client certificate");
  */