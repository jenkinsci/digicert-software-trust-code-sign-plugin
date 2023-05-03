package io.jenkins.plugins.digicert;

import hudson.EnvVars;

import hudson.model.TaskListener;
import jenkins.security.MasterToSlaveCallable;

public class AgentInfo extends MasterToSlaveCallable<Boolean, Throwable> {
    private final TaskListener listener;
    private final EnvVars env;
    private String SM_HOST;
    private String SM_API_KEY;
    private String SM_CLIENT_CERT_FILE;
    private String SM_CLIENT_CERT_PASSWORD;
    private String path;
    private Integer result;

    public AgentInfo(TaskListener listener, EnvVars env) {
        this.listener = listener;
        this.env = env;
    }

    public Boolean call() throws Throwable {

        String os = System.getProperty("os.name");

        SM_HOST = env.get("SM_HOST")!=null?env.get("SM_HOST"):System.getenv("SM_HOST");
        SM_API_KEY = env.get("SM_API_KEY")!=null?env.get("SM_API_KEY"):System.getenv("SM_API_KEY");
        SM_CLIENT_CERT_FILE = env.get("SM_CLIENT_CERT_FILE")!=null?env.get("SM_CLIENT_CERT_FILE"):System.getenv("SM_CLIENT_CERT_FILE");
        SM_CLIENT_CERT_PASSWORD = env.get("SM_CLIENT_CERT_PASSWORD")!=null?env.get("SM_CLIENT_CERT_PASSWORD"):System.getenv("SM_CLIENT_CERT_PASSWORD");
        path = env.get("path")!=null?env.get("path"):System.getenv("path");


        if (os.toLowerCase().contains("windows")) {
            Windows w = new Windows(this.listener, this.SM_HOST, this.SM_API_KEY, this.SM_CLIENT_CERT_FILE, this.SM_CLIENT_CERT_PASSWORD, this.path);
            result = w.call(os);
        } else {
            Linux l = new Linux(this.listener, this.SM_HOST, this.SM_API_KEY, this.SM_CLIENT_CERT_FILE, this.SM_CLIENT_CERT_PASSWORD, this.path);
            result = l.call(os);
        }
        if(result == 1)
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