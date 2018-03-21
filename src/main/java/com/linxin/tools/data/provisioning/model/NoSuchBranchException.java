package com.linxin.tools.data.provisioning.model;

/**
 * Created by Sam Lukes on 03/05/2017.
 */
public class NoSuchBranchException extends RuntimeException {
    private final int id;

    public NoSuchBranchException(final int id) {
        super(String.format("Branch with id [%s] was not found.", id));
        this.id = id;
    }

    public NoSuchBranchException(final String id) {
        super(String.format("Branch with id [SAP-%s] was not found.", id));
        this.id = Integer.parseInt(id);
    }

    public int getId() {
        return id;
    }
}
