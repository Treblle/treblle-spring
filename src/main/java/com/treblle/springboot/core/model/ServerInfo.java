package com.treblle.springboot.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The {@code data.server} object.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ServerInfo {

    private String ip = "bogon";
    private String timezone = "UTC";
    private String software;
    private String protocol;
    private Os os = new Os();

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getSoftware() {
        return software;
    }

    public void setSoftware(String software) {
        this.software = software;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Os getOs() {
        return os;
    }

    public void setOs(Os os) {
        this.os = os;
    }

    /**
     * The {@code data.server.os} object.
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class Os {
        private String name;
        private String release;
        private String architecture;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRelease() {
            return release;
        }

        public void setRelease(String release) {
            this.release = release;
        }

        public String getArchitecture() {
            return architecture;
        }

        public void setArchitecture(String architecture) {
            this.architecture = architecture;
        }
    }
}
