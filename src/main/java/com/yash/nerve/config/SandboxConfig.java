package com.yash.nerve.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nerve.sandbox")
public class SandboxConfig {
    private String path;
    public String getPath(){
        return this.path;
    }
    public void setPath(String path){
        this.path=path;
    }

}
