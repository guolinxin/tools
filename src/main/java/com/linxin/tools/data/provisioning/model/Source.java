package com.linxin.tools.data.provisioning.model;

public enum Source {
    micapture("micapture", 1),
    custom("custom", 1),
    webcapture("webcapture", 1),
    webcaptureadmin("webcaptureadmin", 1),
    yext("yext", 1),
    cic("cic", 2),
    adworks("adworks", 3),
    apc("apc", 4),
    ipix_migration("ipix-migration", 4),
    nabulk("nabulk", 5),
    ocd("ocd", 6),
    lb("lb", 7),
    ah("ah", 8),
    vb("vb", 9),
    ldc("ldc", 10),
    mobius("mobius", 10),
    datoin("datoin", 10),
    apex("apex", 11),
    pa("pa", 12),
    tp("tp", 13),
    ldc_video("ldc-video", 14),
    postoffice("postoffice", 15),
    wca("wca", 16),
    ee("ee", 17),
    unknown("unknown", 100),
    // special for images
    generated("generated", 1);

    private final int reputation;
    private final String moniker;

    Source(String moniker, int reputation) {
        this.reputation = reputation;
        this.moniker = moniker;
    }

    public static Source getEnum(String value) {
        if (value == null)
            return unknown;

        for (Source source : values())
            if (source.getMoniker().equalsIgnoreCase(value)) return source;

        return unknown;
    }

    public boolean isMoreReputableThan(Source that) {
        return this.reputation < that.reputation;
    }

    public int getReputation() {
        return reputation;
    }

    public String getMoniker() {
        return moniker;
    }
}
