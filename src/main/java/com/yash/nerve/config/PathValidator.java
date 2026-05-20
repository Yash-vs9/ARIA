package com.yash.nerve.config;

import java.nio.file.Path;

public class PathValidator {
    private SandboxConfig sandboxConfig;
    public PathValidator(SandboxConfig sandboxConfig){
        this.sandboxConfig=sandboxConfig;
    }
    public void validate(String path){
        Path requested=Path.of(path).normalize().toAbsolutePath();
        Path sandbox=Path.of(sandboxConfig.getPath()).normalize().toAbsolutePath();
        if(!requested.startsWith(sandbox)){
            throw new SecurityException(
                    "Access denied: '" + path + "' is outside the sandbox. " +
                            "NERVE can only access: " + sandbox
            );
        }
    }
}
