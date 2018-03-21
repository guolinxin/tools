package com.linxin.tools.data.provisioning.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class BranchPair {

    final int sourceBranch;
    final int targetBranch;
    final String targetBranchClassification;
    final String sourceBranchClassification;

    public BranchPair(int sourceBranch, String sourceBranchClassification, int targetBranch, String targetBranchClassification) {
        this.sourceBranch = sourceBranch;
        this.sourceBranchClassification = sourceBranchClassification;

        this.targetBranch = targetBranch;
        this.targetBranchClassification = targetBranchClassification;
    }

    public String getTargetBranchClassification() {
        return targetBranchClassification;
    }

    public String getSourceBranchClassification() {
        return sourceBranchClassification;
    }

    public int getSourceBranch() {
        return sourceBranch;
    }

    public int getTargetBranch() {
        return targetBranch;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        BranchPair that = (BranchPair) o;

        return new EqualsBuilder()
                .append(getSourceBranch(), that.getSourceBranch())
                .append(getTargetBranch(), that.getTargetBranch())
                .append(getTargetBranchClassification(), that.getTargetBranchClassification())
                .append(getSourceBranchClassification(), that.getSourceBranchClassification())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getSourceBranch())
                .append(getTargetBranch())
                .append(getTargetBranchClassification())
                .append(getSourceBranchClassification())
                .toHashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BranchPair{");
        sb.append("sourceBranch=").append(sourceBranch);
        sb.append(", targetBranch=").append(targetBranch);
        sb.append(", targetBranchClassification='").append(targetBranchClassification).append('\'');
        sb.append(", sourceBranchClassification='").append(sourceBranchClassification).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
